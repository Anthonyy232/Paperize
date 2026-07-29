# Changelog

## 4.0.0

Paperize 4 is a ground-up Android 12+ rewrite focused on reliable local wallpaper
rotation, a modern Compose interface, and a hardware-accelerated live wallpaper mode.

### Highlights

- Added static and live wallpaper modes with independent home/lock configuration.
- Added scaling, blur, darken, vignette, grayscale, adaptive brightness, parallax,
  double-tap, shuffle, and interval controls.
- Added an in-app **Change wallpaper now** action and Quick Settings support.
- Added current-wallpaper previews that do not require restricted wallpaper access.
- Removed the obsolete all-files storage permission; selected images and folders use
  Android's scoped document access.
- Added live progress while importing folders or individual wallpapers.
- Reworked folder scanning around the Storage Access Framework and refreshes linked
  folders whenever Paperize starts.

### Fixes and reliability

- Sets matching home and lock wallpapers atomically when their presentation settings
  match, preventing one screen from diverging or reverting to the system default.
- Renders against the largest built-in panel on foldables, even when changed while
  the device is folded.
- Correctly handles EXIF-rotated images without applying orientation twice.
- Preserves distinct FIT, FILL, STRETCH, and NONE behavior on scrolling launchers.
- Reduces bitmap memory pressure and fixes several bitmap, texture, queue, scheduling,
  and transient-storage failure paths.
- Speeds up large folder imports and queue/album refresh operations.
- Repairs the release workflow so signed APKs are published with a versioned filename.

### Dependencies

- Updated the stable Jetpack Compose platform, Navigation, Kotlin serialization, and
  reorderable components.
- Updated the GitHub release action and made Renovate updates safer and less noisy.

### Upgrade note

Upgrading from Paperize 3 performs a one-time local database/settings reset because
Paperize 4 uses a new storage model. Albums and schedules must be configured again.
Users of the 4.0.0 alpha have already completed this migration and are not reset again.
