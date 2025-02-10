# Vision Lens Android

Vision Lens is an Android application that uses ML Kit to perform real-time text recognition and face detection using your device's camera.

## Features

- Real-time camera preview using CameraX
- Text recognition using ML Kit
- Face detection using ML Kit
- Modern Android architecture and components

## Tech Stack

- **Language**: Kotlin
- **Camera**: CameraX (Jetpack)
- **ML**: ML Kit (Text Recognition & Face Detection)
- **Architecture**: MVVM with modular design
- **Minimum SDK**: 24 (Android 7.0)

## Project Structure

```
vision-lens-android/
├── app/                # Main application module
├── camera/             # Camera functionality module
├── mlkit/              # ML Kit processing module
├── core/              # Core utilities and shared components
└── database/          # Local data storage module
```

## Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/vision-lens-android.git
```

2. Open the project in Android Studio

3. Build and run the project:
```bash
./gradlew build
```

## Permissions

The app requires the following permissions:
- `android.permission.CAMERA` - For camera access

## Usage

1. Launch the app
2. Grant camera permissions when prompted
3. Point your camera at:
   - Text to recognize written content
   - Faces to detect and count them
4. Use the buttons at the bottom to switch between text and face detection modes

## Development

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK 34
- Android device/emulator running Android 7.0 (API 24) or higher

### Building
```bash
# Clean build
./gradlew clean

# Build debug variant
./gradlew assembleDebug

# Run tests
./gradlew test
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [CameraX](https://developer.android.com/training/camerax)
- [ML Kit](https://developers.google.com/ml-kit)
- [Android Jetpack](https://developer.android.com/jetpack) 