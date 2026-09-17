Cloud Functions for Coupoop

This folder contains Firebase Cloud Functions used to send FCM notifications and compute sync-streaks when logs are created.

Setup
1. Install tools: npm install --save
2. Ensure firebase CLI is installed and logged in (`npm i -g firebase-tools` and `firebase login`).
3. Configure project: `firebase use --add` to select your Firebase project.
4. Deploy functions: `firebase deploy --only functions`

Notes
- The function reads tokens from users/{userId}.fcmTokens (array). The Android client should save FCM registration tokens to that path on sign-in/refresh.
- The sync-window is 30 minutes (configured in index.js). The streak increment heuristic is simple (resets after >48 hours) — tweak as product needs evolve.
- The function also writes a small document into pairings/{pairingId}/celebrations which the client can listen to for triggering animations.
