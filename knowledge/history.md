# History — jadwal-sholat-tv

- [2026-10-21 07:15] Inisialisasi Proyek:
  - Direktori: phpBro/www/lab/jadwal-sholat-tv
  - Status: ✅ Struktur direktori & git repository dibuat

- [2026-10-21 07:25] Penyusunan Aset Bundled:
  - 514 Kota & Kabupaten Indonesia (`assets/data/cities.json`)
  - 6.236 Ayat Al-Qur'an Arab + Latin + Terjemahan Kemenag (`assets/quran/quran_compact.json`, 3.04 MB)
  - Audio Adzan pendek CC-BY-SA (`assets/audio/adzan.ogg`) dan Beep (`assets/audio/beep.wav`)
  - Status: ✅ Selesai

- [2026-10-21 07:35] Implementasi Kode Inti & UI:
  - Engine jadwal sholat, rotasi ayat random, alert adzan, dialog setting lokasi & audio
  - Fitur Keep Screen On (anti-sleep)
  - Status: ✅ Selesai

- [2026-10-21 07:42] Build & Verifikasi TV:
  - APK `build/jadwal-sholat-tv.apk` (2.5 MB) berhasil dikompilasi
  - Terpasang dan diuji di TCL Google TV (192.168.3.11) via ADB
  - Status: ✅ Terverifikasi (Screenshots: backup/tcl-09-jadwal-sholat-fullscreen.png, backup/tcl-10-sholat-settings-dialog.png)

- [2026-10-21 07:45] Integrasi Widget tv-home:
  - Card widget di launcher diperbarui dengan opsi "Buka Layar Penuh" untuk meluncurkan `jadwal-sholat-tv`
  - Status: ✅ Terverifikasi (Screenshots: backup/tcl-11-prayer-options-dialog.png, backup/tcl-12-launched-from-widget.png)
