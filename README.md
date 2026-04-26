# Jellyfin TV

An Android TV app that lets you connect to, browse, and play media from a Jellyfin media server.

## Features

- 🔌 **Server setup** – Enter your Jellyfin server URL and sign in with your username/password.
- 📺 **Media browsing** – Navigate your Libraries, see Latest Movies, Latest Shows, and
  Continue Watching rows using a TV-optimised Leanback UI.
- 🎬 **Video playback** – Full-screen playback via ExoPlayer (Media3) with direct-stream support,
  resume position, and automatic playback progress reporting to the server.
- 🔒 **Session persistence** – Credentials are stored securely in SharedPreferences so you stay
  signed in across restarts.

## Requirements

| Component | Version |
|---|---|
| Android TV device / emulator | API 21+ (Android 5.0) |
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Jellyfin server | 10.8+ recommended |

## Build Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/rpeters1430/Jellyfin-TV.git
   cd Jellyfin-TV
   ```

2. **Open in Android Studio** – File → Open → select the project root.

3. **Sync Gradle** – Android Studio will automatically download all dependencies.

4. **Run on a device or emulator**
   - Select an Android TV device / AVD target.
   - Click **Run** ▶ or press `Shift+F10`.

### Command-line build
```bash
./gradlew assembleDebug         # Build debug APK
./gradlew test                  # Run unit tests
./gradlew lint                  # Lint checks
```

The debug APK will be placed in `app/build/outputs/apk/debug/`.

## Project Structure

```
app/src/main/java/com/rpeters1430/jellyfintv/
├── App.kt                          # Application class (singletons)
├── LoginActivity.kt                # Server URL + sign-in screen
├── MainActivity.kt                 # Hosts BrowseFragment
├── DetailsActivity.kt              # Hosts DetailsFragment
├── PlaybackActivity.kt             # Full-screen ExoPlayer
├── api/
│   ├── JellyfinApiService.kt       # Retrofit interface for Jellyfin REST
│   ├── ApiClient.kt                # OkHttp + Retrofit factory
│   └── AuthInterceptor.kt          # Attaches MediaBrowser auth header
├── models/
│   ├── Auth.kt                     # Login request/response models
│   ├── MediaItem.kt                # BaseItemDto and related models
│   └── System.kt                   # Server info + playback info models
├── repository/
│   └── JellyfinRepository.kt       # Single source of truth for API calls
├── viewmodel/
│   ├── LoginViewModel.kt
│   ├── BrowseViewModel.kt
│   └── DetailsViewModel.kt
├── fragments/
│   ├── BrowseFragment.kt           # Leanback BrowseSupportFragment
│   └── DetailsFragment.kt          # Leanback DetailsSupportFragment
├── presenter/
│   ├── CardPresenter.kt            # ImageCardView presenter
│   └── DetailsDescriptionPresenter.kt
└── utils/
    ├── PreferenceManager.kt        # SharedPreferences wrapper
    └── ImageUrlBuilder.kt          # Jellyfin image/stream URL helpers
```

## Architecture

The app follows a simple **MVVM** pattern:

```
UI (Activities / Fragments)
    ↕ observe LiveData
ViewModels
    ↕ suspend functions → Result<T>
Repository  (JellyfinRepository)
    ↕ Retrofit suspend calls
Jellyfin REST API  (JellyfinApiService)
```

## License

[MIT](LICENSE)