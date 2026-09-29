# Vision Lens

Real-time, on-device dog detection and breed recognition for Android, built with TensorFlow Lite (LiteRT), CameraX and Jetpack Compose.

- Finds dogs live from the camera and names their breed (120 breeds from the Stanford Dogs dataset)
- Two-stage pipeline: EfficientDet-Lite0 (COCO) locates the dogs, then a MobileNetV3 breed classifier labels each crop
- Runs fully offline, so no images leave the device
- Color-coded bounding boxes with breed + confidence labels, plus a list of the dogs in view
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

## Breed model

`app/src/main/assets/dog_breeds.tflite` is trained by `training/train_dog_breeds.py` on
[Stanford Dogs](http://vision.stanford.edu/aditya86/ImageNetDogs/) (85% top-1 on its test set).
Download and extract `images.tar`, `annotation.tar` and `lists.tar` into one folder, then:

```bash
pip install tensorflow pillow scipy
python training/train_dog_breeds.py --data <that folder> --out app/src/main/assets
```

The backbone is frozen, so this runs on a CPU in about 15 minutes. Crops with a breed
confidence below 30% are labelled plain "Dog".

On-device tests (held-out test photos) run with `./gradlew connectedDebugAndroidTest`.

## Project layout
```
app/src/main/assets/                 efficientdet_lite0.tflite, labelmap.txt,
                                     dog_breeds.tflite, dog_breeds_labels.txt
app/src/main/java/com/ishaq/visionlens/
  MainActivity.kt                    splash, edge-to-edge, camera permission flow
  detection/ObjectDetector.kt        EfficientDet (COCO) interpreter, filtered to dogs
  detection/DogBreedClassifier.kt    breed classifier on a dog crop
  detection/DogDetector.kt           detector + classifier pipeline used by the app
  ui/DetectorViewModel.kt            frame analysis, FPS, settings state
  ui/screens/                        PermissionScreen, CameraScreen
  ui/components/DetectionOverlay.kt  bounding-box drawing
  ui/theme/Theme.kt                  colors, typography, glass style
training/train_dog_breeds.py         trains and exports the breed model
```
