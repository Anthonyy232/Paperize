# Paperize media

Generated images for [Paperize](https://github.com/Anthonyy232/Paperize), kept off `master` so it stays light: the README hero and feature panels, the website screenshots, the F-Droid and IzzyOnDroid listing art, and the release banners. Every script takes the path of a Paperize checkout of `master`, reads the logo from its `app/src/main/ic_launcher.svg`, and needs no network.

| Path | What |
| --- | --- |
| `lib.cjs` | Fonts and rendering shared by the scripts |
| `fonts/` | Instrument Serif, under the SIL Open Font License (`OFL.txt`) |
| `readme/wallpapers.cjs` | Writes the two demo albums, Paper and Dusk, into `readme/wallpapers/` (abstract shapes, six 1080x2400 JPEGs each) |
| `readme/inputs/` | The raw emulator screenshots, 1080x2400, light and dark |
| `readme/build.cjs` | Writes `.github/readme/*.png`, `site/img/*.webp` and `metadata/en-US/images/` (`icon.png`, `featureGraphic.png`, `phoneScreenshots/1-5.png`) in the checkout from `readme/inputs/` |
| `releases/banner.cjs` | Writes `releases/<tag>.png` from `releases/highlights.json` |

## Setup

From a Paperize checkout of `master`:

```bash
git worktree add ../paperize-media media
cd ../paperize-media
npm ci && npx playwright install chromium
```

## README, site and store images

```bash
node readme/build.cjs ../Paperize    # the path of the master checkout
```

Commit the changed images on `master`. Re-capture the inputs first when the screens they show have changed.

## Capturing the screenshots

The app can only be seen on a device, so the inputs are captured by hand on the `paperize_api36` emulator (Pixel-sized, 1080x2400, API 36), driven from the T3 Code device tools (`agent-device`); `adb` is used only to install the app and push the wallpapers.

1. Build and install the debug app from the checkout: `./gradlew assembleDebug`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Make the demo albums and push them: `node readme/wallpapers.cjs`, then `adb push readme/wallpapers/Paper /sdcard/Pictures/` and the same for `Dusk`.
3. Clear the app's data, set the system to light mode, and open the app. Agree to the privacy notice, choose **Static Mode**, and skip the notification permission.
4. Library: add an album **Dusk** and add `dusk-1.jpg` to `dusk-6.jpg` from `Pictures/Dusk` with **Add wallpapers** (select all six in one go). Then add an album **Paper** the same way from `Pictures/Paper`. Paper is created second so the library lists it first.
5. Open `dusk-5.jpg` in Dusk, **Set wallpaper**, **Home Screen**; open `paper-3.jpg` in Paper, **Set wallpaper**, **Lock Screen**. Leave **Continue rotation from here** unchecked.
6. Wallpaper tab: turn on **Lock** and **Home**, pick **Paper** as the lock album and **Dusk** as the home album, and turn on **Wallpaper changing** (this applies the first wallpaper of each album: repeat step 5 afterwards so lock shows `paper-3` and home `dusk-5`). Keep the interval at 1 hour, shuffle and individual scheduling off, scaling **Fill**. Under Visual Effects turn on **Blur Effect** for **Home** and set it to **43%**. Everything else stays at its default, including dynamic colour off.
7. Clear the notification shade (**Clear all**) so the status bar shows nothing but the clock.
8. Take each screenshot with `agent-device screenshot readme/inputs/<scheme>-<screen>.png`, positioning the Wallpaper tab by the label positions from `agent-device snapshot --raw` (y is the label's centre on the 2400 px screen):

   | Screen | What is on screen |
   | --- | --- |
   | `home` | Wallpaper tab, scrolled to the top |
   | `albums` | Wallpaper tab, the **Paper** album row label at y 619 (the album rows just under the tabs) |
   | `schedule` | Wallpaper tab, the **Scaling** heading at y 1672 (**Change wallpaper now** button top at 1482) |
   | `scaling` | Wallpaper tab, the **Scaling** heading at y 567 (**Visual Effects** at 1192) |
   | `effects` | Wallpaper tab, the **Visual Effects** heading at y 616 |
   | `library` | Library tab |
   | `album` | Library, the Dusk album open |

   Slow `agent-device gesture pan` drags scroll by close to their length (short ones lose about 20 px to touch slop), so reach each position with one long pan and a short correction.
9. Switch to dark mode with `agent-device settings appearance dark` and take the same seven screens.

The clock is whatever the emulator shows, so it differs between captures.

## Release banners

```bash
node releases/banner.cjs ../Paperize v4.3.0 "One short highlight"
```

That records the highlight in `releases/highlights.json` and writes `releases/v4.3.0.png` (1280x640). Without a tag it redraws every banner. Commit, push, and use the raw URL in the release notes:

`https://raw.githubusercontent.com/Anthonyy232/Paperize/media/releases/v4.3.0.png`
