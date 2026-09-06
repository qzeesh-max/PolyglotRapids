# Polyglot Rapids

Polyglot Rapids is an educational and fast-paced Android game where players steer a raft down a perilous river while simultaneously collecting letters to form words in one of 23 supported languages. Avoid rocks, logs, and shallows, hit ramps to jump over obstacles, and test your vocabulary skills in real-time!

## Origin
This project is an Android port of the original `JavaNativeLink` Polyglot Rapids demo application. The original application utilized a hybrid Java + C++ architecture compiled to the web using TeaVM. This repository completely ports the C++ game engine logic into a 100% native Kotlin implementation specifically designed for modern Android devices.

## Key Features
- **100% Native Android**: Re-written from scratch in pure Kotlin, removing the need for JNI overhead.
- **Modern Jetpack Compose UI**: Features a sleek, responsive, full-screen canvas-based game loop running at a smooth 60 FPS.
- **Dynamic Language Support**: Support for 23 languages including English, Spanish, Mandarin, Hindi, Arabic, and more, loaded dynamically via Android Asset dictionaries.
- **Interactive Gameplay**: Steer your raft to dodge obstacles and collect letters to build words and restore health.
- **Reactive Architecture**: Game state is completely decoupled from the UI, utilizing Compose state variables to drive rendering.

## Technologies Used
- **Kotlin**: Primary language used for game logic and Android development.
- **Jetpack Compose**: Used for the entire UI layer, HUD overlays, and Canvas rendering.
- **Android SDK**: Target platform utilizing modern features like `MediaPlayer` for lifecycle-aware audio playback and `pointerInput` for gesture detection.
- **Gradle**: Build system for dependency management and generating signed release App Bundles.
