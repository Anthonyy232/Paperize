## v4.0.3

- Kept scheduled static changes in the device's natural orientation, even when a
  landscape game or app is in the foreground.
- Rendered FIT, FILL, STRETCH, and NONE against one physical display panel to fix
  black canvases, excessive stretching, zoom, and inconsistent home/lock alignment.
- Restored centered static FILL behavior by default and added an explicit horizontal
  scrolling option for users who want wide wallpapers to move across home pages.
- Added a persistent pause/resume control that keeps selected albums intact, while
  leaving manual static and live wallpaper changes available when paused.
- Removed the unnecessary network-state permission and unused network image loader.
- Updated Hilt and the coroutine test library and expanded rotation, scaling,
  scrolling, pause/resume, static-effect, live-renderer, and scheduler verification.

**Full Changelog**: https://github.com/Anthonyy232/Paperize/compare/v4.0.2...v4.0.3

## v4.0.2

- Reloaded the current live wallpaper at native resolution after fold, unfold,
  surface-size, and scaling changes without advancing the wallpaper queue.
- Made adaptive brightness update the wallpaper already on screen and removed
  the brightness dip that could occur halfway through live crossfades.
- Kept live vignette shading continuous across large images split into multiple
  GPU texture tiles.
- Prevented a delayed effect-slider save from overwriting a switch or other
  setting changed immediately afterward.
- Restored the launcher app shortcut for gesture apps and other launchers, with
  automatic routing to the configured static or live wallpaper engine.
- Added the missing daily album refresh to live schedules and hardened boot
  recovery so valid jobs are restored without duplicates and stale jobs are removed.
- Removed two unused legacy serialization and document-file dependencies from
  the release package.
- Expanded device coverage for every static effect and verified synchronized,
  independent, manual, live, and reboot scheduling paths.

**Full Changelog**: https://github.com/Anthonyy232/Paperize/compare/v4.0.1...v4.0.2

## v4.0.1

- Restored native static FILL scrolling so wide wallpapers move between their real
  left and right edges without synthetic launcher-sized overflow.
- Added independent brightness, blur, vignette, and grayscale controls for home
  and lock screens, including when both screens share one album and schedule.
- Refreshed folder-backed albums whenever Paperize returns to the foreground so
  added and removed files appear without restarting the app.
- Improved foldable sizing on Android 17 by including inactive built-in panels
  while excluding external displays.
- Made wallpaper changes commit queue and current-wallpaper state only after
  Android accepts the bitmap, with rejected changes restored for retry.
- Corrected live wallpaper parallax enablement, intensity, edge traversal, and
  launcher-offset clamping, and clarified that it responds to home-page swipes.
- Added device regressions for EXIF rotation, static FILL overflow, and folded
  display sizing, plus focused queue and live-renderer unit coverage.

**Full Changelog**: https://github.com/Anthonyy232/Paperize/compare/v4.0.0...v4.0.1

## v4.0.0

- Completely rewrote Paperize for Android 12 and newer with a modern Compose interface.
- Added static and live wallpaper modes with independent home and lock screen settings.
- Added scaling, blur, darken, vignette, grayscale, adaptive brightness, parallax,
  double-tap, shuffle, interval, and manual wallpaper controls.
- Added current-wallpaper previews, Quick Settings support, and progress indicators
  for large wallpaper and folder imports.
- Improved folder scanning, queue refreshes, bitmap memory usage, scheduling, and
  wallpaper rendering reliability.
- Fixed home and lock screen synchronization, foldable display sizing, EXIF rotation,
  and FIT, FILL, STRETCH, and NONE scaling behavior.
- Removed the obsolete all-files storage permission in favor of Android's scoped
  document access.
- Updated dependencies and repaired the release workflow for signed, versioned APKs.

**Upgrade note:** Updating from Paperize 3 resets local albums and settings once
because Paperize 4 uses a new storage model. Users of the 4.0.0 alpha are not reset
again.

## New Contributors

* @gpunto made their first contribution in https://github.com/Anthonyy232/Paperize/pull/415

**Full Changelog**: https://github.com/Anthonyy232/Paperize/compare/v3.2.1...v4.0.0
