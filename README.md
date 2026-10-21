# Jadwal Sholat TV (Android TV / Google TV)

**Aplikasi Jadwal Sholat, Al-Qur'an Ambient Kiosk, & Digital Signage Masjid untuk Android TV & Google TV.**  
Bebas iklan, ringan, 100% ramah navigasi remote D-pad, dengan kontrol panel **Web Administrator lokal (Port 8080)**, multi-slide otomatis (Jadwal Sholat, Laporan Kas Keuangan, Agenda Kajian, Poster Flyer), rotasi ayat Al-Qur'an (Arab, Latin, Terjemahan Kemenag RI) dan audio adzan lokal.

---

## Fitur Utama

- **Layar Penuh Multi-Slide Kiosk Display**:
  - **Slide 1: Jadwal Sholat & Al-Qur'an (Inti)**: Jam digital format besar (`HH:mm:ss`), tanggal Masehi & Hijriah, 5 waktu sholat (+ Imsak & Dhuha), countdown dinamis, dan rotasi 6.236 ayat Al-Qur'an terkompresi offline.
  - **Slide 2: Laporan Keuangan & Kas Masjid**: Menampilkan saldo awal, penerimaan infaq, pengeluaran, saldo akhir, dan catatan rincian kas.
  - **Slide 3: Agenda Kajian & Pengumuman**: Tanggal, narasumber, dan uraian pengumuman kegiatan masjid.
  - **Slide Custom Poster / Flyer**: Upload gambar flyer kegiatan langsung dari HP via Web Admin.
  - *Default: Hanya Slide 1 yang aktif. Template Kas dan Pengumuman siap diaktifkan dan dikustomisasi kapan saja via Web Admin.*
- **Interupsi Cerdas Waktu Sholat**:
  - Saat waktu sholat mendekat (< 5 menit) atau saat adzan, perputaran slider **otomatis dihentikan sementara dan layar terkunci di Slide Jadwal Sholat** agar jamaah fokus melihat waktu sholat. Slider kembali berputar setelahnya.
- **Embedded Web Server Administrator (Port 8080)**:
  - Cukup buka browser HP/Laptop di jaringan Wi-Fi yang sama:  
    `http://IP-TV:8080` (Contoh: `http://192.168.3.11:8080`)
  - **Keamanan PIN**: Dilindungi login PIN.
    > 🔑 **PIN Administrator Bawaan (Default): `1989`**  
    > *(Dapat diubah sendiri kapan saja melalui menu 'Ganti PIN' di Web Admin).*
  - **Kontrol Lengkap via Browser HP**: Atur slide (durasi, isi kas, teks pengumuman, upload poster), ganti kota sholat, pilih background, atur suara adzan, dan audio countdown tanpa repot mengetik dengan remote TV.
- **Hitung Mundur & Audio Alert Bertahap**:
  - Countdown dinamis `< 5 menit` dengan peringatan audio pada detik 30, 20, dan counting pips dari detik 10 hingga 0 sebelum adzan.
  - Pilihan nada: **Adzan Merdu (Lokal)**, **Beep Pendek**, atau **Hening (Visual Saja)**.
- **Pilihan 30 Background Islamic & Galeri**:
  - 15 Tema Gelap (10 foto masjid malam + 5 motif gelap).
  - 15 Tema Terang (10 foto masjid siang + 5 motif terang).
  - Menu pilih background dari Galeri / File Manager TV.
- **Layar Selalu ON (Anti-Sleep)**:
  - Mencegah TV masuk ke mode screensaver/sleep saat dipakai sebagai display musholla/rumah.
- **Zero Background RAM Lifecycle**:
  - Saat aplikasi tidak dibuka atau user kembali ke launcher home screen, seluruh timer, web server, dan audio player langsung dimatikan di `onPause()` sehingga RAM TV tetap bebas beban.

---

## Cara Pasang Otomatis dengan Coding Agent (Paling Mudah)

1. Aktifkan **USB Debugging** di TV: *Settings → System → About → Tekan OK 7x pada Build → Developer Options → USB Debugging: ON*.
2. Buka folder repo ini di agen AI (Claude Code / Antigravity / Cursor) dan paste instruksi dari `PROMPT.md`.

---

## Cara Akses Web Administrator dari HP / Laptop

1. Pastikan HP atau laptop terhubung ke **Wi-Fi yang sama** dengan TV.
2. Buka aplikasi di TV, lalu buka menu **Pengaturan** (`⚙ Pengaturan`). Di bagian atas terdapat kartu info alamat:
   `Alamat: http://<IP-TV>:8080` (Contoh: `http://192.168.3.11:8080`).
3. Buka alamat tersebut di browser Chrome/Safari/Firefox di HP Anda.
4. Masukkan PIN Administrator: **`1989`**.
5. Anda dapat langsung mengaktifkan Slide Kas, mengedit Pengumuman, mengunggah Poster, dan mengatur seluruh konfigurasi TV secara real-time.

---

## Cara Build & Pasang Manual (macOS / Linux)

Prasyarat: OpenJDK 17 dan Android Command Line Tools (`aapt2`, `d8`, `apksigner`).

```bash
# 1. Build APK saja
./build-and-install.sh --build

# 2. Build dan langsung install ke TV via ADB (setel IP TV Anda)
TV_IP=192.168.3.11 ./build-and-install.sh
```

---

## Lisensi & Sumber Data
- Data Jadwal Sholat: [Kemenag RI via MyQuran API](https://api.myquran.com).
- Data Al-Qur'an: [quran-json oleh Rio Astamal](https://github.com/rioastamal/quran-json) / [sutanlab](https://github.com/sutanlab/quran-api) (Kemenag RI).
- Audio Adzan: [Wikimedia Commons (CC-BY-SA 4.0)](https://commons.wikimedia.org/wiki/File:Azan.ogg).
- Lisensi Kode: MIT (Dibuat oleh: ardith666).
