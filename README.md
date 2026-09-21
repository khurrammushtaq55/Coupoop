Sync — Playful Poop Tracker for Couples/Besties

Lightweight Android app (Kotlin + Jetpack Compose) that lets paired users log "moments" and see each other's activity in near real time. Backend uses Firebase Firestore + Auth + FCM. Includes a Jetpack Glance home-screen widget and local Compose rendering for weekly recap cards.

Getting started (dev)
1. Open project in Android Studio.
2. Configure local.properties with Android SDK path and add google-services.json to app/ for Firebase.
3. Build and run on a device/emulator (minSdk, package name, and Firebase project details are in the repo).

Design & tone: playful, emoji-forward, non-medical.

## Architecture
This app is organized around a Clean Architecture split:
- Domain: core app models and repository contracts (for example, theme settings).
- Data: implementations that talk to Android preferences and Firebase-backed services.
- Presentation: Compose screens and app-level navigation logic that react to domain/data state.

The UI code still lives under the existing Compose package tree, while the app entry point delegates through `com.mmushtaq04.coupoop.presentation.CoupoopApp` to keep the root activity thin and the responsibilities clearer.

See the project plan in the associated Copilot session for prioritized todos and phases.