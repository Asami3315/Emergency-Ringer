# Emergency Ringer

An Android app that ensures you never miss critical calls from emergency contacts. When a whitelisted contact calls (via Phone or WhatsApp), the app overrides Silent and Do Not Disturb modes to play a loud alarm.

## Requirements

- Android 7.0 (API 24) or higher
- Kotlin, Jetpack Compose

## Setup

1. Clone and open in Android Studio
2. Build and run on a device or emulator
3. Grant all permissions (Notification Access, DND Access, Contacts)
4. Add emergency contacts via the + button
5. Enable the app in Settings > Notification Access

## Custom Alarm Sound

To use a custom emergency ring sound, add `emergency_ring.mp3` to `app/src/main/res/raw/`. If the file is not present, the app uses the system default alarm sound.

## Permissions

| Permission | Purpose |
|------------|---------|
| Notification Access | Detect incoming call notifications |
| Do Not Disturb Access | Bypass DND when emergency contact calls |
| MODIFY_AUDIO_SETTINGS | Change ringer mode and volume |
| READ_CONTACTS | Pick emergency contacts |
| WAKE_LOCK | Play alarm with screen off |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Reliable background monitoring |

## Monitored Apps

- WhatsApp (`com.whatsapp`)
- Phone / Dialer (`com.android.server.telecom`, `com.android.dialer`, `android`)

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
