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

- [2026-10-21 08:50] Rilis v1.3 - Header Contributor QRIS & Gallery Thumbnail (Terang / Gelap):
  - Header Contributor QRIS: Di bagian paling atas menu pengaturan disematkan kartu pendukung developer dengan barcode QRIS (`assets/qris.png`), pesan ajakan support/doa, dan doa “Jazakumullahu khairan” (bisa diklik untuk memperbesar QRIS)
  - Galeri Thumbnail Background: Background dikelompokkan ke dalam 2 menu visual terpisah:
    1. 🌙 Koleksi Tema Gelap (15 pilihan: 10 foto malam + 5 motif gelap)
    2. ☀️ Koleksi Tema Terang (15 pilihan: 10 foto siang + 5 motif terang)
  - Layout GridView 3 kolom dengan pratinjau thumbnail 16:9 yang ringan, border fokus D-pad menyala, dan penanda tema aktif
  - Status: ✅ Teruji di TV TCL (Screenshots: backup/tcl-20, tcl-21, tcl-22) & dipush ke GitHub (Commit 758dafd)

- [2026-10-21 09:10] Rilis GitHub v1.3 & Integrasi In-App Auto Update Checker:
  - Teks Amal Jariyah Diperhalus: Ditambahkan kata "gratis" ("Aplikasi ini dibuat tulus sebagai amal jariyah, gratis & open source...")
  - Rilis Resmi GitHub v1.3: Dibuat release tag `v1.3` di https://github.com/ardith666/jadwal-sholat-tv/releases/tag/v1.3 dengan asset `jadwal-sholat-tv.apk`
  - In-App Auto Update Checker:
    - Default: Otomatis memeriksa rilis terbaru ke GitHub Releases API saat aplikasi dibuka
    - Menampilkan popup dialog konfirmasi jika versi baru tersedia (beserta catatan rilis & tombol langsung unduh APK)
    - Opsi tombol manual "Cek Pembaruan Versi Aplikasi" dan toggle ON/OFF di menu pengaturan
  - Status: ✅ Teruji di TV TCL (Screenshots: backup/tcl-31-settings-v13-update-checker.png, backup/tcl-32-manual-update-dialog.png) & terpublikasi di GitHub (Commit b4c5208)

- [2026-10-21 09:25] Tampilan Versi & Tanggal Rilis di Footer Layar Utama:
  - Menambahkan informasi `v1.3 (Rilis: 21 Okt 2026)` di pojok kiri bawah layar utama, berdampingan dengan `Dibuat oleh : ardith666` di pojok kanan bawah
  - Status: ✅ Teruji di TV TCL (Screenshot: backup/tcl-33-version-release-date-footer.png) & dipush ke GitHub (Commit 806d677)

- [2026-10-21 09:45] Rilis v1.4 - Multi-Slide Kiosk Display & Embedded Web Server Admin:
  - Multi-Slide Carousel: Layar utama mendukung perputaran slide otomatis (Jadwal Sholat & Quran, Laporan Kas Masjid, Pengumuman Agenda, Poster Gambar)
  - Default: Hanya 1 slide aktif (Jadwal Sholat), template Kas & Pengumuman siap diaktifkan via Web Admin
  - Interupsi Cerdas: Saat countdown adzan aktif (< 5 menit), slider otomatis terkunci di slide jadwal sholat
  - Embedded Web Server: Port 8080 (aman dan tidak bentrok dengan Chromecast/ADB TV), melayani Single Page App Admin responsif
  - Keamanan PIN: Default PIN `1989` (terdokumentasi di README.md dan dapat diubah lewat web)
  - Banner Informasi Web Admin: Menampilkan URL `http://IP:8080` dan PIN default di menu pengaturan TV serta footer layar utama
  - Zero Background Footprint: Web Server dan Socket di-stop di `onPause()` saat aplikasi diminimalkan
  - Status: ✅ Teruji di TV TCL (Screenshots: backup/tcl-34, tcl-35, tcl-36, tcl-37) & di-push ke GitHub repo ardith666/jadwal-sholat-tv

- [2026-10-21 10:05] In-App Direct APK Downloader & Installer (Self-Update):
  - Mengatasi error "Anda saat ini tidak memiliki aplikasi yang dapat melakukan tindakan ini" akibat ketiadaan browser di Android TV
  - Ditambahkan downloader HTTP internal dengan ProgressDialog interaktif
  - Ditambahkan `AppFileProvider` dan permission `REQUEST_INSTALL_PACKAGES`
  - Begitu unduhan selesai, sistem langsung memicu intent installer bawaan Android (`application/vnd.android.package-archive`)
  - Status: ✅ Teruji di TV TCL (Screenshot: backup/tcl-38-in-app-update-checked.png) & di-push ke GitHub (Commit e8d4827)

- [2026-10-21 10:20] Fix Navigasi D-Pad Remote TV:
  - Masalah: Remote TV sebelumnya hanya merespons Kiri/Kanan untuk slider sehingga tombol Atas tidak bisa fokus ke Pengaturan
  - Solusi: Menambahkan explicit interceptor pada `onKeyDown(KEYCODE_DPAD_UP)` untuk langsung memberikan fokus ke `settingsBtn.requestFocus()`
  - Tombol Kiri/Kanan dibatasi hanya mengganti slide jika header Pengaturan sedang tidak dalam keadaan fokus
  - Status: ✅ Teruji di TV TCL (Screenshots: backup/tcl-39-settings-focused-dpad-up.png, backup/tcl-40-settings-opened-by-dpad.png) & di-push ke GitHub (Commit 86037ff)

- [2026-10-21 13:40] Perencanaan Fitur YouTube Live/Video & Integrasi Stream Kamera:
  - Slide YouTube: Mendukung link video atau live streaming YouTube (lock mode hingga video selesai atau ditekan tombol Back)
  - Parser URL RTSP: Perbaikan sanitasi query string dan kredensial kamera dengan karakter khusus (@)
  - Integrasi Multi-CCTV & Live Kajian: Struktur data model v1.5 siap
  - Status: 📝 Dicatat dalam Todo List untuk sesi pengerjaan berikutnya

- [2026-10-22 09:25] Penyelesaian Rilis v1.7 & Evaluasi Versioning Bug:
  - Evaluasi Bug Versi: Ditemukan bahwa release tag GitHub sebelumnya terbuat sebelum build APK diperbarui versinya di AndroidManifest.xml (sehingga TV mendownload APK v1.6 lama).
  - Tindakan Mitigasi:
    1. AndroidManifest dinaikkan ke versionCode=5 dan versionName="1.7".
    2. Script `build-and-install.sh` diperbarui dengan automated aapt2 version checker sebelum selesai build.
    3. Binary APK di GitHub Release v1.7 diperbarui via clobber upload.
    4. Prosedur rilis 4-langkah dibakukan di KNOWLEDGE.md dan Obsidian Vault.
  - Status: ✅ Terverifikasi di TV TCL (versionCode=5, versionName=1.7, screenshot backup/tcl-66-v17-running-confirmed.png)
