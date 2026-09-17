FCM integration notes

- Use Cloud Functions (or Cloud Run) triggered on `pairings/{pairingId}/logs/{logId}` document creation to send a push to other pairing members.
- Payload: light, playful copy (example): "{displayName} just went 💩"
- Include notification data key with pairingId/logId to deep-link into the app's shared feed.
- For MVP, serverless function can look up member tokens in users/{userId}/fcmTokens and send to all except the creator.
- Consider sending data-only messages for silent badge updates and notifications with lively copy for user-facing alerts.
- Respect user notification preferences (mute, do-not-disturb windows) persisted in users/{userId}/prefs.
