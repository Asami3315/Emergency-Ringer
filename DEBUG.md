# Debugging Emergency Ringer

## 1. Test Ringer Button

1. Open the app and tap **"Test Ringer (plays alarm now)"**
2. If you hear the alarm: audio path works. Issue is notification detection.
3. If no sound: check DND access, volume, battery optimization.

## 2. View Logs via ADB

Connect your phone via USB (with USB debugging on) and run:

```bash
adb logcat -s EmergencyRinger:* | tee ringer_log.txt
```

Or on Windows PowerShell:

```powershell
adb logcat -s EmergencyRinger:* | Tee-Object -FilePath ringer_log.txt
```

Keep this running, then:
- Make a test call from an emergency contact (or have someone call)
- Or tap **Test Ringer** in the app
- Stop the log (Ctrl+C) and share `ringer_log.txt`

## 3. What the Logs Show

| Log message | Meaning |
|-------------|---------|
| `NotificationListenerService CONNECTED` | App is receiving notifications |
| `PKG=com.xxx` | Which app posted the notification |
| `Category=call` | Android marked it as a call notification |
| `Extras: [key]=value` | Raw notification data (helps debug) |
| `Call? cat=true text=true` | Incoming call detected |
| `Whitelist=[names] \| Matches=true` | Whitelist match found |
| `EMERGENCY CALL - triggering ringer` | RingerManager was called |
| `EMERGENCY RINGER TRIGGERED!` | Audio override started |

## 4. Common Issues

| Symptom | Check |
|---------|-------|
| No "CONNECTED" log | Re-enable Notification Access in Settings |
| No notification logs at all | Phone app package may differ - check PKG in logs and add to MONITORED_PACKAGES |
| `Matches=false` | Whitelist name doesn't match caller - try exact contact name |
| `Whitelist empty` | Add emergency contacts in the app |
| Test Ringer works but real calls don't | Notification structure differs - share the Extras log |
