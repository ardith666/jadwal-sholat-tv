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
