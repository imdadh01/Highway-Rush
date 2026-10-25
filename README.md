# Amber Launcher

A completely new native Android home launcher inspired by the supplied first screenshot, using the standard Android sans-serif font. No code, assets, dependencies or workflows from Highway-Rush are used. This branch contains only the new launcher project; main is unchanged.

## Install
Download `Amber-Launcher-APK` from this branch's GitHub Actions run, unzip it and install `Amber-Launcher.apk`. Allow installation from your browser/file manager when Android asks. Open Amber Launcher and long-press an empty area → **Set as default home**.

## Features
- Reconstructed warm kitten wallpaper, live local clock and date.
- Search, voice-search intent and Google Lens web shortcut.
- Reference-positioned calculator, 2×2 suggestions panel, gallery, Google folder, vertical productivity panel and five-app glass dock.
- Launch installed apps; long-press a shortcut to assign a different installed app.
- Swipe up or sideways, or tap the page indicator, for a searchable app drawer.
- Long-press an app in the drawer for Android app info.
- Long-press home for default-launcher selection, a custom wallpaper picker, manual weather text or layout reset.
- Android supplies the real status/navigation bars. No fake unread counts.

## Design limits
Wallpaper is AI-reconstructed from the reference, not the exact original wallpaper. Installed app icons come from the device and can vary. Colored fallback icons are shown for missing apps; tapping one opens the app picker. Weather is explicitly manual (blank until configured); it is not a live feed. AI Suggestions is a fixed, editable four-shortcut panel, not an AI service. The app does not replace Android's lock screen or system control center. Target SDK 28 preserves the reference's system-bar layout on newer Android; this is a sideload build, not a Play Store submission.

## Build
Java 17, Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.3.

```
gradle :app:assembleDebug -x test -x lint --no-daemon
```

The GitHub workflow runs only APK assembly and artifact upload. No unit, instrumentation, UI, or lint tests are run, per request. The APK uses Android's debug signing for installation; subsequent CI builds may use a new debug key and require uninstalling the previous build.

## Asset provenance
`app/src/main/assets/wallpaper.jpg` was created using the built-in image-generation tool from the user-supplied screenshot. Prompt: remove all status, clock, date, weather, search, icons, folders, dock and navigation UI; reconstruct the underlying kitten wallpaper while preserving its warm palette, pose, composition and blurred room, with no text or symbols.
