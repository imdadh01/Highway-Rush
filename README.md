# Amber Launcher 3.0

A user-arranged Android home screen. There are no predefined apps or suggested folders.

## Home
- Drag an app from the drawer to any empty home cell or dock slot.
- Drop an app onto another app to create a folder. Drop onto a folder to add it.
- Open a folder and select Small (1×1), Big (2×2) or Vertical (1×2).
- Folder overflow stays accessible when opened. Edit apps, rename or remove through its menu. Removing a shortcut/folder never uninstalls its apps.
- Drag from an open folder to move an app onto home. Removing via Manage apps only changes membership.
- Hold a home item to drag it. The upper Options and Remove targets expose actions without a permanent editing toolbar.
- Hold a dragged icon at either screen edge to change page. Holding at the right edge adds pages. Add-to-home finds space and creates another page automatically; there is no special Your apps screen or 20-item limit.
- Settings can add a page or remove the current additional empty page. The first page has the clock/search area; subsequent pages have a larger icon grid. Empty dock slots are user assignable.
- Resizing a folder finds free space if necessary; it does not overwrite adjacent shortcuts.

## Drawer and appearance
- Full-screen wallpaper-backed native grid with compact header, no green controls.
- Tap the header search icon to replace the title with an inline search field; the field occupies that same header row.
- Alphabet rail supports tap/drag and a letter indicator. Favorites and sorting count launches through this launcher only.
- Hold an icon and drag it to home. For favorite/hide/info/uninstall actions use More → Manage apps, then tap the app. Android confirms uninstall.
- More → Background & opacity offers six presets, a custom hex color and continuous opacity controls. Drawer and folder overlays can be transparent; home wallpaper has an independent tint-opacity control.
- Menus use compact, neutral bottom sheets.

## Update behavior
Version 3 removes the v1/v2 fixed reference panels and prefilled dock. Wallpaper and appearance preferences remain. Only shortcuts/folders explicitly added by the user to the v2 extra page are migrated; fixed suggested apps are not migrated. A fresh installation has no apps or folders on home.

## Build and installation
GitHub Actions builds `Amber-Launcher.apk`. Java 17, SDK 35, Gradle 8.9, AGP 8.7.3:

```
gradle :app:assembleDebug -x test -x lint --no-daemon
```

CI restores the version-2 signing-key cache when available for in-place upgrades. If the cache was lost and Android rejects the signature, uninstall the previous build first (that clears launcher data).

Only compilation/packaging is performed. No tests, lint, emulator sessions or device runs are performed per the user's instruction. Runtime behavior is unverified.

The drawer lists launchable apps in the current Android user profile. Third-party widgets, password-protected hiding, notification badges, work-profile management, lock screen and system-control-center replacement are outside this version. Target SDK 28 is retained for the sideload build. The default kitten wallpaper is reconstructed from the supplied screenshot; a user-picked wallpaper persists across updates.
