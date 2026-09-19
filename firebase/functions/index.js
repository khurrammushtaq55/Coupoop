const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

// Configuration: sync window in minutes
const SYNC_WINDOW_MINUTES = 30;

exports.onLogCreate = functions.firestore
  .document('pairings/{pairingId}/logs/{logId}')
  .onCreate(async (snap, context) => {
    const { pairingId } = context.params;
    const log = snap.data();
    if (!log) return null;

    const userId = log.userId;
    const ts = log.timestamp ? log.timestamp.toDate() : new Date();

    // Fetch pairing doc to get members
    const pairingRef = db.collection('pairings').doc(pairingId);
    const pairingSnap = await pairingRef.get();
    if (!pairingSnap.exists) return null;
    const pairing = pairingSnap.data();
    const memberIds = pairing.memberIds || [];

    // Prepare notification payload
    const notification = {
      notification: {
        title: `${log.displayName || 'Partner'} just went 💩`,
        body: `${log.displayName || 'They'} logged a moment`,
      },
      data: {
        pairingId: pairingId,
        logId: snap.id
      }
    };

    // Collect tokens from users/{userId}/fcmTokens (array) - best-effort,
    // skipping any member who has muted this pairing (mutedPairings array on
    // their own users/{uid} doc — see NotificationPrefsManager on the client)
    const tokens = [];
    for (const member of memberIds) {
      if (member === userId) continue;
      try {
        const u = await db.collection('users').doc(member).get();
        const d = u.data();
        if (!d) continue;
        const mutedPairings = d.mutedPairings || [];
        if (mutedPairings.includes(pairingId)) continue;
        const t = d.fcmTokens || [];
        for (const tok of t) tokens.push(tok);
      } catch (e) {
        console.warn('error fetching user token', e);
      }
    }

    // Send notification to member tokens
    if (tokens.length > 0) {
      // chunk if needed; admin.messaging().sendMulticast supports up to 500 tokens
      const uniqueTokens = Array.from(new Set(tokens));
      try {
        await admin.messaging().sendMulticast({ tokens: uniqueTokens, ...notification });
      } catch (e) {
        console.error('Failed to send notifications', e);
      }
    }

    // Check for sync moment: see if any other member logged within SYNC_WINDOW_MINUTES
    const windowMs = SYNC_WINDOW_MINUTES * 60 * 1000;
    const logsSnap = await pairingRef.collection('logs')
      .orderBy('timestamp', 'desc')
      .limit(20)
      .get();

    let syncTriggered = false;
    let otherRecentLog = null;
    for (const doc of logsSnap.docs) {
      const data = doc.data();
      if (!data || data.userId === userId) continue;
      const otherTs = data.timestamp ? data.timestamp.toDate() : null;
      if (!otherTs) continue;
      const diff = Math.abs(ts.getTime() - otherTs.getTime());
      if (diff <= windowMs) {
        syncTriggered = true;
        otherRecentLog = { docId: doc.id, data };
        break;
      }
    }

    if (syncTriggered) {
      // Update streaks doc under pairings/{pairingId}/streaks/sync
      const streakRef = pairingRef.collection('streaks').doc('sync');
      await db.runTransaction(async (tx) => {
        const sSnap = await tx.get(streakRef);
        const now = admin.firestore.Timestamp.now();
        if (!sSnap.exists) {
          tx.set(streakRef, {
            currentStreak: 1,
            longestStreak: 1,
            lastSyncMomentAt: now
          });
        } else {
          const s = sSnap.data();
          // Simple heuristic: if lastSyncMomentAt exists and is within 48 hours, increment; else reset to 1
          const last = s.lastSyncMomentAt ? s.lastSyncMomentAt.toDate() : null;
          let current = s.currentStreak || 0;
          let longest = s.longestStreak || 0;
          if (last) {
            const hoursSince = Math.abs(now.toDate().getTime() - last.getTime()) / (1000 * 60 * 60);
            if (hoursSince <= 48) {
              current = current + 1;
            } else {
              current = 1;
            }
          } else {
            current = 1;
          }
          longest = Math.max(longest, current);
          tx.update(streakRef, {
            currentStreak: current,
            longestStreak: longest,
            lastSyncMomentAt: now
          });
        }
      });

      // Optionally write a small celebratory document for client to react to
      const celebrationRef = pairingRef.collection('celebrations').doc();
      await celebrationRef.set({
        type: 'sync_moment',
        at: admin.firestore.Timestamp.now(),
        participants: [userId, otherRecentLog.data.userId || null]
      });
    }

    return null;
  });
