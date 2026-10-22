# Project Knowledge — jadwal-sholat-tv

## Vision
Aplikasi jadwal sholat dan lantunan teks Al-Qur'an ambient mandiri untuk Android TV/Google TV. Ringan, bebas iklan, tanpa framework berat, ramah navigasi remote TV, dan dapat diinstalasi otomatis oleh agen AI via ADB.

## Architecture
- **Stack**: Java native AOSP bare tools (`aapt2`, `d8`, `javac --release 11`, `apksigner`, OpenJDK 17).
- **Core Modules**:
  - `MainActivity.java`: UI fullscreen ambient, clock ticker, prayer countdown, quran verse animator.
  - `PrayerEngine.java`: Integrasi jadwal sholat Kemenag/MyQuran v2 dengan cache offline.
  - `QuranEngine.java`: 6.236 ayat Al-Qur'an terkompresi lokal (~3 MB JSON).
  - `AlertManager.java`: Audio adzan (CC-BY-SA OGG) / beep alert + preferences.
  - `SettingsDialog.java`: Dialog pengaturan 514 kota se-Indonesia & opsi tampilan/audio ramah remote D-pad.

## Decisions
| Date | Decision | Reason | Rejected |
|------|----------|--------|----------|
| 2026-10-21 | Bundled offline Quran JSON (~3 MB) | TV langsung siap tanpa harus download 6000+ ayat di runtime | Mengunduh via internet saat first-run |
| 2026-10-21 | Bare SDK tools build (no Gradle) | Build instan dalam 3 detik, mudah dijalankan oleh coding agent AI | Proyek Gradle multi-gigabyte |
| 2026-10-21 | FLAG_KEEP_SCREEN_ON dengan toggle setting | Mencegah layar mati saat dipakai sebagai display musholla/rumah | Layar sleep otomatis memutus tampilan |
| 2026-10-21 | MyQuran API v2 untuk 514 Kota | Database lengkap dan terverifikasi sesuai hisab Kemenag RI | Scraping manual session Bimas Islam Kemenag |
| 2026-10-22 | SOP Pembuatan Release APK | Manifest versionCode & versionName WAJIB dinaikkan SEBELUM compile & gh release create/upload | Membuat release tag sebelum build APK diperbarui |
| 2026-10-22 | Native Origin pada WebView YouTube | Aturan referer YouTube (Sept 2025) mewajibkan origin: https://${pkg} untuk menghindari Error 150/101 | Menggunakan origin https://www.youtube.com |
| 2026-10-22 | In-App Package Installer via Cache Internal | Menyimpan APK update di context.getCacheDir() menghindari error permission Scoped Storage Android 14 | Menggunakan external storage tanpa izin WRITE |

## SOP Rilis & Prosedur Update (Anti-Bug Versi)
Agar tidak terjadi bug versi di mana release tag GitHub tidak cocok dengan versi APK di TV:
1. **Periksa & Naikkan Versi di 3 Titik Kunci**:
   - `AndroidManifest.xml`: `android:versionCode` (+1) dan `android:versionName` (misal "1.8").
   - `UpdateChecker.java`: `CURRENT_VERSION = "1.8"`.
   - `MainActivity.java` & `SettingsDialog.java`: Teks footer & dialog versi.
2. **Kompilasi APK Terlebih Dahulu**:
   - Jalankan `./build-and-install.sh --build`.
   - Script sekarang otomatis mencetak: `📦 Info Versi APK: versionCode=X versionName=Y`.
   - Verifikasi output badging aapt2 sebelum lanjut ke langkah GitHub.
3. **Upload ke GitHub Release**:
   - Jika tag sudah ada: `gh release upload <tag> build/jadwal-sholat-tv.apk --clobber`.
   - Jika tag baru: `gh release create <tag> build/jadwal-sholat-tv.apk --title "..." --notes "..."`.
4. **Verifikasi Unduhan**:
   - Lakukan uji unduh URL langsung (`curl -sL <browser_download_url>`) dan cek badging aapt2 bahwa binary di cloud sudah identik dengan versi baru.
