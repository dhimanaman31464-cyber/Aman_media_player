# Aman Media Player

Native Android media player project.

## Features
- Requests media-library permissions on first launch.
- Reads audio, video and image media from Android MediaStore.
- All, Music, Videos and Photos library filters.
- Dark, green-accented music-player interface.
- Audio playback in-app; videos and photos open with the device's compatible viewer.
- Flaming A launcher icon.

## Build APK
The GitHub Actions workflow builds a debug APK on every push to `main`, and can also be started manually.

1. Open the repository's **Actions** tab.
2. Select **Build Aman Media Player APK**.
3. Open the latest completed run.
4. Under **Artifacts**, download **Aman-Media-Player-APK**.
5. Extract the ZIP; the installable file is `app-debug.apk`.

The first installation may require allowing your browser or file manager to install apps.

## Local build
Requires JDK 17, Android SDK platform 35 and Gradle 8.9:
`gradle assembleDebug`
