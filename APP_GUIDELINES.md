# Voice Cloner Application - Build Guidelines

This document outlines the architecture and implementation details for the Voice Cloner application to facilitate consistent development across different AI agent assistants.

## Overview
The Voice Cloner is an Android application built with Jetpack Compose (MVVM architecture) that allows users to record their voice, clone it using Google Gemini's audio capability, and generate synthetic voice samples based on the cloned profile.

## Core Components

### 1. Data Layer (Room Database)
- **Entities**:
  - `VoiceProfile`: Stores metadata about cloned voices (pitch, tone, vibe, pace, etc.).
  - `VoiceGenerationResult`: Stores links to generated audio files corresponding to a profile.
- **DAO**: `VoiceDao` handles standard CRUD operations using Coroutines and Flow.
- **Repository/Logic**: `VoiceClonerViewModel` interacts with `VoiceDao`.

### 2. Audio Processing (`AudioHelper`)
- **Recording**: Uses `MediaRecorder` to record audio samples from the microphone to the app's cache directory.
- **Playback**: Uses `MediaPlayer` for replaying raw audio files (recorded samples, cloned samples, or profile samples).
- **Utility**: `fileToBase64` / `saveBase64ToPersistentFile` for handling Gemini API integration (audio input/output).

### 3. ViewModel Logic (`VoiceClonerViewModel`)
- **State Management**: Uses `StateFlow` for UI states (recording status, playback, synthesis status, error messages).
- **Network**: Uses `OkHttpClient` and JSON construction to communicate with Gemini API (v1beta).
- **Audio Cloner Integration**: 
  - `cloneAndAnalyze`: Sends voice sample in base64 to Gemini for trait analysis (JSON response).
  - `synthesizeText`: Sends text to Gemini TTS API (audio generation response - base64).

## API Integration Notes
- **Gemini API**: Requires `GEMINI_API_KEY` in `BuildConfig` (injected via `secrets.gradle.plugin` and `.env` file).
- APIs used: 
  - Generative Language API (Analyze): `models/gemini-3.5-flash:generateContent`
  - Generative Language API (TTS): `models/gemini-2.5-flash-preview-tts:generateContent`

## UI/UX Patterns
- **Jetpack Compose**: Follows Material 3 design system.
- **State Observation**: `collectAsStateWithLifecycle` in composables.
- **Accessibility**: Explicit `contentDescription` for all interactive audio controls.
- **Data Visualization**: Uses a custom `Canvas` component (`AudioMetricsChart`) for rendering frequency analysis and pitch variance metrics for captured audio, optimized for performance without external dependencies.

## Critical Build Configurations
- **Namespace**: `com.example`
- **Application ID**: `com.aistudio.voicecloner.[random_suffix]`
- **SDK**: `minSdk = 24`, `targetSdk = 36`
- **Dependencies**: Uses Version Catalog (`libs.versions.toml`).
