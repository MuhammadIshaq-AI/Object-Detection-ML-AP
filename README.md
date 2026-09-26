# Vision Lens

Real-time, on-device object detection for Android, built with TensorFlow Lite (LiteRT), CameraX and Jetpack Compose.

- Detects 80 COCO object types (people, animals, vehicles, food, etc.) live from the camera
- Uses the EfficientDet-Lite0 model and runs fully offline, so no images leave the device
- Color-coded bounding boxes with confidence labels, plus a list of detected objects
- Shows FPS and inference time, and has an adjustable confidence threshold
- Pause/resume, flash toggle, and front/back camera switch

## Requirements
- Android 7.0+ (API 24), target API 36
- Build: JDK 21, Android SDK 36 (the Gradle wrapper is included)

## Build

```bash
./gradlew assembleDebug        # debug APK
./gradlew bundleRelease        # signed Play Store bundle (.aab)
```

The release build is signed only if `keystore.properties` exists in the project root:

```properties
storeFile=keystore/upload-keystore.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

`keystore/`, `keystore.properties` and `release/` are gitignored. Never commit them.

> **Windows with Avast/AVG HTTPS scanning:** if Gradle can't download dependencies, run
> `set JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` before building.

## Project layout
```
app/src/main/assets/                 efficientdet_lite0.tflite, labelmap.txt
app/src/main/java/com/ishaq/visionlens/
  MainActivity.kt                    splash, edge-to-edge, camera permission flow
  detection/ObjectDetector.kt        LiteRT interpreter: preprocessing + postprocessing
  ui/DetectorViewModel.kt            frame analysis, FPS, settings state
  ui/screens/                        PermissionScreen, CameraScreen
  ui/components/DetectionOverlay.kt  bounding-box drawing
  ui/theme/Theme.kt                  colors, typography, glass style
```
