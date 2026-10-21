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

- [2026-10-21 08:00] Rilis v1.1 - Fitur Tambahan:
  - Search & Sort Kota: Dialog pemilihan kota kini diurutkan A-Z secara alfabetis dan dilengkapi kolom pencarian teks real-time
  - 10 Tema Background Islamic: 5 tema gelap (Midnight Ka'bah, Emerald Nabawi, Sapphire Night, Royal Purple, Charcoal) + 5 tema terang (Ivory Andalusia, Desert Gold, Sage Garden, Serene Sky, Marble White) dengan scrim overlay otomatis
  - Atribusi Contributor: Menampilkan "Dibuat oleh : ardith666" di sudut kanan bawah layar utama dan menu "Tentang Aplikasi" di dialog pengaturan
  - Status: ✅ Terverifikasi di TV & dipush ke GitHub (Commit 18b389f)

- [2026-10-21 08:20] Rilis v1.2 - Icon Kustom, 20 Foto Asli, Gallery Picker & Audio Countdown:
  - Custom App Icon: Mengonversi `jadwal-sholat-tv.jpeg` menjadi icon multi-resolusi (mdpi s/d xxxhdpi) dan TV Leanback banner (320x180)
  - 20 Foto Asli Arsitektur Masjid: Menambahkan 10 foto masjid malam (Kauno, Blue Mosque, Ka'bah Makkah, dll) dan 10 foto masjid siang (Sultan Omar, Sheikh Zayed, dll) sehingga total ada 30 pilihan background
  - Pilihan Background dari Galeri: Opsi memilih gambar kustom dari Galeri / File Manager TV via `ACTION_GET_CONTENT`
  - Audio Countdown Alert: Nada peringatan pada 30 detik & 20 detik, serta audio counting naik pitch bertahap dari detik 10 hingga 0
  - Zero-Background RAM Lifecycle: Menghubungkan lifecycle `onPause()` dan `onStop()` untuk memutus seluruh Handler callback timer & menghentikan MediaPlayer saat aplikasi diminimalkan/ditutup, menjaga RAM TV tetap nol beban background
  - Status: ✅ Teruji di TV TCL (Screenshots: backup/tcl-16 s/d tcl-18) & dipush ke GitHub (Commit d7b7dc6)

- [2026-10-21 08:35] Navigasi Hierarki Pengaturan (Back 1 Step):
  - Mengubah perilaku dialog Settings: Ketika mengubah lokasi, tema, suara, atau toggle ON/OFF, dialog tidak langsung keluar ke layar utama melainkan tetap bertahan di menu pengaturan dengan state ter-update
  - Tombol Batal pada sub-dialog otomatis kembali ke menu utama pengaturan
  - Status: ✅ Terverifikasi di TV TCL (Screenshot: backup/tcl-19-settings-keep-open.png) & dipush ke GitHub (Commit 3088143)
