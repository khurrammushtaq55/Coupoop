Privacy Policy (Draft) — Sync (Coupoop)

Last updated: 2026-09-17

Overview
- Sync is a light, playful app for paired users to log private "moments" and view each other's activity. This is not a medical app.

Data we collect
- Minimal account data: anonymous Firebase Auth UID, optional display name, and FCM tokens for push notifications.
- Pairing relationships and logs are stored in Firestore under pairings/{pairingId}/logs.
- Optional fields in logs: mood, bristol-type, short note. Photos are not collected in the MVP.

How data is used
- Data is private to the members of a pairing and is never published publicly by the app.
- FCM tokens are used only to send push notifications to pairing members.
- Shared recap cards are generated locally and shared by the user’s intent (client-side).

Retention & deletion
- Users can delete their account data by removing their user record; deleting an account does not automatically remove pairing logs created by others. Provide a settings option to request full deletion.

Third parties
- Firebase (Google) services (Auth, Firestore, FCM) are used; refer to Google’s privacy docs for their practices.

Security
- Firestore rules restrict access to pairing members. Tokens and data are stored securely using Firebase best practices.

Contact
- For privacy questions or data removal requests, contact: privacy@yourdomain.example (replace with valid contact before submission).

Notes
- This is a draft for development. Update with legal review before Play Store submission.
