# PPPlayer

مشغّل فيديو وموسيقى لأندرويد، مكتوب بـ Kotlin و Jetpack Compose فوق Media3 (ExoPlayer).
A local video and music player for Android, built with Kotlin, Jetpack Compose and Media3 (ExoPlayer).

## Features

**Library**
- Videos grouped by folder or as one list, in list or grid view
- Sort by name, date, size or duration; search across videos and music
- "Continue watching" card, watched-progress lines and a NEW badge
- Music tab and a Recent tab with history
- Share, rename, delete and file details
- Open a network stream (HTTP, HTTPS, HLS, DASH, RTSP)
- Opens video and audio files from other apps

**Player**
- Swipe left side for brightness, right side for volume (up to 200% with boost)
- Swipe sideways to seek, double-tap the edges to jump, hold for 2× speed, pinch to zoom
- Audio track selection, embedded subtitles, external subtitle files (SRT, VTT, ASS/SSA, TTML), subtitle size
- Playback speed 0.25×–4×, display modes (fit, crop, stretch, 16:9, 4:3)
- Screen lock, rotation, picture-in-picture, background playback with a media notification
- Sleep timer, repeat, shuffle, A–B repeat, playlist, resume from the last position
- Decoder modes: HW, HW+ and SW (bundled FFmpeg audio decoders)

**App**
- Arabic and English, full right-to-left layout
- Light and dark themes

## Build

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest lintDebug
```

Minimum Android version: 8.0 (API 26).

## Licences

- Readex Pro font — SIL Open Font License (see `licenses/ReadexPro-OFL.txt`)
- Jellyfin Media3 FFmpeg decoder — GPL-3.0; FFmpeg — LGPL/GPL. Shipping this app
  with the FFmpeg decoder means distributing it under compatible terms. Remove the
  `org.jellyfin.media3:media3-ffmpeg-decoder` dependency to drop that requirement.
