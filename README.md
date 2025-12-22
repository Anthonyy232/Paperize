<div align="center">
  <img style="display: block" src="https://github.com/user-attachments/assets/e8fb14f5-ec8e-440e-a2ac-8065322b0e28" alt="">
  <h1>Paperize</h1>
  <p><strong>A dynamic wallpaper changer that keeps your device's aesthetic fresh and exciting</strong></p>

  [![GitHub Downloads](https://img.shields.io/github/downloads/Anthonyy232/Paperize/total?style=flat&logo=github&label=Downloads)](https://github.com/Anthonyy232/Paperize/releases)
  [![GitHub Release](https://img.shields.io/github/v/release/Anthonyy232/Paperize?style=flat&logo=github)](https://github.com/Anthonyy232/Paperize/releases/latest)
  [![License](https://img.shields.io/github/license/Anthonyy232/Paperize?style=flat)](LICENSE)
  [![F-Droid](https://img.shields.io/f-droid/v/com.anthonyla.paperize?style=flat&logo=fdroid)](https://f-droid.org/en/packages/com.anthonyla.paperize/)
  
  [![Crowdin](https://badges.crowdin.net/paperize/localized.svg)](https://crowdin.com/project/paperize)
</div>

---

## Features

- **Dynamic Wallpaper Changer** — Set your wallpaper to change at specific time intervals
- **Static & Live Wallpapers** — Choose between traditional static wallpapers or smooth live wallpaper transitions
- **Multiple Image Formats** — Supports JPG, PNG, WEBP, AVIF, HEIC/HEIF, BMP, GIF, TIFF, and SVG
- **Folder Support** — Organize wallpapers into folders for auto-updating
- **Dual Screen Support** — Choose the same or separate albums for home and lock screen
- **Wallpaper Effects** — Apply various effects including brightness, blur, scaling, vignette, and more
- **On-Device Storage** — All wallpapers and settings stored locally on your device

---

## Download

[<img src="https://github.com/Anthonyy232/Paperize/assets/60626873/1c034414-21cd-4a0a-838d-89fe7bd56910" alt="Download from GitHub" height="60">](https://github.com/Anthonyy232/Paperize/releases)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/en/packages/com.anthonyla.paperize/)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="60">](https://apt.izzysoft.de/fdroid/index/apk/com.anthonyla.paperize)

---

## Localization

Help translate Paperize into your language! Currently, most translations are provided using machine translation — contributions from native speakers are greatly appreciated.

**[Contribute on Crowdin →](https://crowdin.com/project/paperize/invite?h=d8d7a7513d2beb0c96ba9b2a5f85473e2084922)**

---

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/develop/ui/compose) |
| **Design System** | [Material 3](https://m3.material.io/) |
| **Dependency Injection** | [Dagger Hilt](https://dagger.dev/hilt/) |
| **Database** | [Room](https://developer.android.com/training/data-storage/room) |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) |
| **Animations** | [Lottie](https://github.com/airbnb/lottie) |

<details>
<summary><b>View all dependencies</b></summary>

- [Zoomable](https://github.com/usuiat/Zoomable) — Zoomable and pannable views
- [DocumentFileCompat](https://github.com/ItzNotABug/DocumentFileCompat) — Efficient DocumentFile wrapper
- [LazyColumnScrollbar](https://github.com/nanihadesuka/LazyColumnScrollbar) — Compose scrollbar library
- [compose-collapsing-toolbar](https://github.com/onebone/compose-collapsing-toolbar) — Collapsing toolbar for Compose

</details>

---

## Building from Source

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java | 17 |
| Android Gradle Plugin | 8.7.0+ |
| Gradle | 9.2.1 |
| Minimum SDK | 31 (Android 12) |
| Target SDK | 36 |

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Anthonyy232/Paperize.git
   cd Paperize
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select `File > Open` and navigate to the cloned repository

3. **Build and Run**
   - Click `▶ Run` to build and install on a connected device, or
   - Select `Build > Generate Signed Bundle / APK` to create a signed release

---

## Contributing

Contributions are welcome! Feel free to:

- Report bugs by opening an issue
- Suggest features or improvements
- Submit pull requests

---

## Support

If you find Paperize useful, consider supporting development through [GitHub Sponsors](https://github.com/sponsors/Anthonyy232) (one-time or monthly). Thank you!

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
