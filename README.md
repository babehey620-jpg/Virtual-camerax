# Virtual Camera Studio (Android)

A legitimate in-app media preview and camera-testing utility for Android.

## Features in this starter build
- Pick an image or video using Android's system document picker.
- Preview images and videos inside the app.
- RTMP stream preview (`rtmp://` / `rtmps://`) using ExoPlayer's RTMP extension.
- Zoom, drag-to-position, and rotate/correction controls for still-image preview.
- Loop playback toggle.
- Restore/clear preview.
- In-app mini-preview mode toggle.

## Important scope
This app does **not** replace the Android system camera provider, inject media into other apps,
or bypass identity checks / liveness detection. The floating preview is an in-app mini-preview,
not a system-wide overlay.

## Build the APK using GitHub Actions (no Android Studio required)
1. Create a new GitHub repository.
2. Upload the contents of this folder to the repository root.
3. Open the repository's **Actions** tab.
4. Run **Build Android APK** (or push to `main`).
5. Open the completed workflow run and download the `VirtualCameraStudio-debug` artifact.
6. Extract the ZIP; install `app-debug.apk` on your Android phone. If Android asks, allow
   installation from the browser or file manager you used.

## Local build
Requires JDK 17, Gradle 8.9, Android SDK platform 35 and build-tools 35.0.0:
`gradle assembleDebug`

## Notes
- Debug APK is for testing. A release APK should be signed with your own private signing key.
- RTMP playback depends on the stream being reachable and compatible with ExoPlayer's RTMP extension.
