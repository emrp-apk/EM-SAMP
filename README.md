# EM-SAMP

Custom Android launcher UI for Empire Mallu Roleplay (EMRP).

- App name: EM-SAMP
- Server: play.emrp.online:2026
- Android: API 28+
- Kotlin + Jetpack Compose
- RP nickname: Firstname_Lastname
- Server status / player count / ping
- Game/S-MP folder picker using Android Storage Access Framework
- UCP / forum / store links
- Remote news feed
- Aim sensitivity and crosshair preferences

## Important
The launcher starts an installed compatible S-MP Android client. Different clients use different package IDs and connection intents, so exact one-tap server connection depends on the EMRP S-MP client. Add the exact package/activity and documented intent extras for the client used by EMRP.

This project does not modify game memory or inject an automatic targeting cheat.

## Build
Open this folder in Android Studio, let Gradle sync, then choose `app` -> `assembleRelease`. The APK will be under `app/build/outputs/apk/release/`.
