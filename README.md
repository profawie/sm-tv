# SM TV

A custom home screen (launcher) for Google TV / Android TV, with a dark and gold theme. It has been tested on a Xiaomi Mi TV (MiTV-AFKR0, Android 11) and a Chromecast with Google TV (Android 14).

> **Disclaimer:** This project is for entertainment purposes only. If you build or install it yourself, you do so at your own risk. The author accepts no responsibility for any damage, data loss or other problems that may result.

## Features

- **Header**: TV name on the left. On the right: WiFi status and network name, date and time (`dd/MM/yyyy hh:mm a`), All apps, Settings, and a button back to Google TV (shown only while the Google TV launcher is enabled).
- **Prayer times** (full-width first row): from [api.waktusolat.app](https://api.waktusolat.app/docs) (JAKIM data). The zone is detected from the location's coordinates, and the next prayer is highlighted with a countdown.
- **Weather**: current conditions and a 3-day forecast from [Open-Meteo](https://open-meteo.com/) (free, no key). Selecting the widget opens a full screen with details, the next 24 hours and a rain radar map ([RainViewer](https://www.rainviewer.com/) over an Esri dark base map).
- **Solar (Growatt)**: current power, plus today, this month and total energy, from the official [Growatt OpenAPI](https://openapi.growatt.com/). It needs an API token, and the widget stays hidden until one is entered.
- **CCTV**: live view of one camera from a Dahua-compatible NVR over RTSP (home network only). Selecting the widget opens the camera full screen in HD.
- **Apps**: Netflix, YouTube and Disney+ shortcuts. If an app isn't installed, its Play Store page opens instead. An All apps screen lists every installed TV app.
- **Wallpaper**: a random photo from [Unsplash](https://unsplash.com/developers), with the theme chosen in Settings. Without an Unsplash key, a dark gradient is used.
- **Settings** (left menu, right content, Google TV style):
  - General: TV name (default *Syukri's Family*), language (Bahasa Melayu / English)
  - Location: search for a town (default *Puncak Alam, Kuala Selangor*)
  - Wallpaper: photo theme, change photo now
  - CCTV: recorder IP address, username, password, and camera 1–16 with a live preview
  - Solar: Growatt API token
  - About: app version
- Widgets reload automatically when the network comes back, and retry every 30 seconds after a failed load.

> **Note:** MSN Weather is a Power Automate connector that only works inside Microsoft Power Platform, so this app uses Open-Meteo instead.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```properties
# local.properties (not committed)
sdk.dir=/Users/<you>/Library/Android/sdk
UNSPLASH_ACCESS_KEY=<access key from https://unsplash.com/developers>
```

```bash
./gradlew assembleDebug
```

## Install on a TV

Turn on *Developer options* and *USB debugging* (or *Wireless debugging*) on the TV, then:

```bash
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.smc.tv/.MainActivity
```

On TVs that use *Wireless debugging* (for example a Chromecast on Android 14), pair first with the code shown on the TV:

```bash
adb pair <tv-ip>:<pairing-port> <pairing-code>
```

To show the WiFi network name without a permission prompt:

```bash
adb shell pm grant com.smc.tv android.permission.ACCESS_FINE_LOCATION
```

Passwords and tokens (CCTV, Growatt) are entered on each TV in Settings and stored only on that TV. App backup is turned off, so they are not copied to Google.
