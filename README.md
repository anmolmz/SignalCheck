# SignalCheck
Native Android wireless measurement laboratory (Galaxy S9). Version 0.1 includes Wi-Fi RSSI and scan results.

## Build without Android Studio
Push to `main` and open **Actions → Build SignalCheck APK → latest successful run → Artifacts → SignalCheck-debug-apk**.
Unzip and install `app-debug.apk` on the Galaxy S9. Android will ask for permission to install from the browser/files app.

Grant Location permission and enable Android's Location switch (required for Wi-Fi scan results). Nearby Wi-Fi scans can be throttled or cached by Android.

**Do not treat this as a precise location tracker.** The device reads signal strength, not physical coordinates. Future room heatmaps will use manual position tagging.
