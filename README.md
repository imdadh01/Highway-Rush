# Amber Launcher 2.0

A native Android launcher with the supplied kitten-home composition and a full-window, density-aware app drawer. This independent branch does not use the original repository application.

## What changed in 2.0
- Full-screen 4/5-column app grid, 54dp icons, two-line labels and search with empty states.
- Alphabet rail with tap/drag navigation, A–Z sorting, local launch-count sorting and favorites.
- Independent icon drawables for home and drawer: drawing a home icon no longer changes the drawer icon bounds.
- Launchable app discovery off the UI thread; refresh on app install, removal, changes and resume.
- Long-press app actions: add to home, favorite, folder, app info, Android-confirmed uninstall and hide from drawer.
- A second home page with up to 20 shortcuts/folders, drag-to-reorder, rename/edit folders and remove shortcuts.
- Home and dock shortcuts can be reassigned or removed; folders and settings persist across restarts.
- Navigation-bar inset handling keeps the dock above the system controls.
- Optional manual weather text, 12/24-hour clock, custom wallpaper and default-home selection.

## Use
Swipe up for all apps. Swipe left on home for Your apps, or right to return to the reference home. Long-press a drawer app to add it to home. On Your apps, hold then drag an icon to move it; hold and release to open its menu. Long-press an empty home area for settings. Folder membership is edited through its menu. Hidden apps can be restored through the drawer menu or home settings.

## Install and build
The GitHub Actions artifact contains Amber-Launcher.apk. If Android rejects an update over version 1 because the debug signing key differs, uninstall version 1 first; its launcher layout preferences will be cleared. From version 2, CI caches its debug signing key for subsequent builds while the cache exists.

Java 17, Gradle 8.9, Android SDK 35 and AGP 8.7.3:

```
gradle :app:assembleDebug -x test -x lint --no-daemon
```

Only APK compilation/packaging is performed. No unit, UI, instrumentation, emulator or lint tests are run, per the user's instruction. Runtime behavior on a device has not been verified.

## Scope
The drawer shows enabled launchable apps in the current Android user profile, not background-only packages. Most-used sorting counts launches through Amber only. Hidden apps remain installed and are not password-protected. System apps may not support uninstall. Weather is manual, not live. The four reference suggestions are editable shortcuts, not an AI service. Third-party widget hosting, notification badges, work-profile management, lock-screen replacement and system control-center replacement are not included.

The kitten wallpaper was reconstructed from the supplied screenshot using the built-in image-generation tool: remove UI and reconstruct the kitten/background while preserving pose, composition and warm palette. Installed icons vary by device. Target SDK 28 preserves the home system-bar layout for this sideload build; this is not a Play Store submission.
