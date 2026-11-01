# SM TV

Launcher tersuai untuk Google TV (diuji untuk Xiaomi Mi TV, model MiTV-AFKR0).

## Ciri

- **Header**: nama TV (kiri); status WiFi + nama rangkaian, tarikh/masa `dd/MM/yyyy hh:mm a`, Tetapan, dan butang kembali ke Google TV (kanan).
- **Widget tengah**:
  - Waktu solat dari [api.waktusolat.app](https://api.waktusolat.app/docs) (data JAKIM, zon dikesan dari koordinat lokasi). Waktu seterusnya diserlahkan bersama kiraan masa.
  - Cuaca semasa + ramalan 3 hari dari [Open-Meteo](https://open-meteo.com/) (percuma, tanpa kunci).
- **Bawah**: Netflix, YouTube, Disney+. Jika app belum dipasang, klik akan buka Play Store.
- **Latar**: gambar rawak dari Unsplash, tema boleh ditukar di Tetapan. Tanpa kunci API, latar gradien gelap digunakan.
- **Tetapan**: Nama TV (lalai: *Syukri's Family*), Lokasi (lalai: *Puncak Alam, Kuala Selangor*), Wallpaper.
- Tema gelap sahaja, warna emas.

> Nota: pautan MSN Weather ialah connector Power Automate yang hanya boleh digunakan dalam Microsoft Power Platform, jadi app ini guna Open-Meteo sebagai ganti.

## Bina

Perlukan JDK 17 dan Android SDK (platform 35).

```bash
# local.properties (tidak di-commit)
sdk.dir=/Users/<anda>/Library/Android/sdk
UNSPLASH_ACCESS_KEY=<access key dari https://unsplash.com/developers>
```

```bash
./gradlew assembleDebug
```

## Pasang ke TV

Hidupkan *Developer options* dan *USB/Network debugging* di TV, kemudian:

```bash
adb connect 192.168.2.202:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.smc.tv/.MainActivity
```

Untuk kebenaran nama WiFi tanpa pop-up:

```bash
adb shell pm grant com.smc.tv android.permission.ACCESS_FINE_LOCATION
```
