# Don't Disturb (Android)

Quick Settings tile that toggles the system **Do Not Disturb** mode, with an
auto-off timer. Native Android (Kotlin), no root required.

## Features

- **QS tile** – tap to turn Do Not Disturb on/off (same system mode, so the phone goes silent).
- **Timer in the notification** – once enabled, an ongoing notification offers
  *15 min* / *1 hour* / *Turn off*. When a timer is running it shows a live
  countdown and DND switches off automatically when it ends.
- **App screen** – more presets (5 min – 4 h), custom minutes, manual toggle.
- Survives reboot / app update (running timer is re-armed).
- English and Azerbaijani UI.

## First run

1. Open the app and tap **Allow Do Not Disturb access** (system settings, one-time).
2. Allow notifications (Android 13+) so the timer controls appear.
3. Tap **Add tile to Quick Settings** (or edit Quick Settings manually).

Optional: allow *exact alarms* for a precise auto-off time; otherwise Android may
delay it by a few minutes in Doze.

## Build

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds it on every push to `main` — download it from the run's
artifacts or from **Releases → Latest debug build**.

## Customising

`DndManager.FILTER_ON` decides what "on" means: `PRIORITY` (default, like the
system DND), `ALARMS` (alarms only) or `NONE` (total silence).
