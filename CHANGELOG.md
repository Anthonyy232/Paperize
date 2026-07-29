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
