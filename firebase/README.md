Firebase setup notes for Sync (Coupoop)

1) Create a Firebase project in the Firebase Console.
   - Record the project ID (use in CI/Play Console integration if needed).

2) Enable Authentication
   - Enable Anonymous sign-in for quick onboarding (recommended default).
   - Optionally enable Google sign-in for account recovery. Configure OAuth client IDs in Google Cloud if enabling.

3) Firestore
   - Create Cloud Firestore in production or test mode as desired.
   - Deploy firestore.rules (use firebase CLI: `firebase deploy --only firestore:rules`).
   - Recommended data model: users/{userId}, pairings/{pairingId}, pairings/{pairingId}/logs/{logId}, pairings/{pairingId}/streaks/{streakId}.

4) FCM (Cloud Messaging)
   - Configure FCM in the Firebase Console and add google-services.json to app/.
   - For server-side notifications consider using Cloud Functions triggers on document create to send a tailored push to other pairing members.

5) Security notes
   - Keep Firestore rules restrictive: only allow reads/writes to pairing members.
   - Avoid client-side logic that can escalate privileges (e.g., don't let client update memberIds)._ 

6) Local testing
   - Use the Firebase emulator suite for local testing of Firestore and Authentication.

7) Deployment
   - Add google-services.json to app/ before building release variants.

See firestore.rules in this folder for starter rules.
