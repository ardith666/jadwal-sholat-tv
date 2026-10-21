# Jadwal Sholat TV (Android TV / Google TV)

**Aplikasi Jadwal Sholat & Al-Qur'an Ambient Kiosk untuk Android TV & Google TV.**  
Bebas iklan, ringan, 100% ramah navigasi remote D-pad, dengan rotasi ayat suci Al-Qur'an (Arab, Latin, Terjemahan Kemenag RI) dan audio adzan lokal.

---

## Fitur Utama

- **Layar Penuh (Ambient Kiosk Display)**: Jam digital format besar (`HH:mm:ss`), tanggal Masehi & Hijriah, dan nama Kota/Kabupaten.
- **Jadwal Sholat 5 Waktu + Imsak & Dhuha**: Menggunakan data resmi hisab Kemenag RI (MyQuran API v2) yang di-cache offline. Card waktu sholat yang mendekat otomatis disorot.
- **Hitung Mundur 5 Menit**: Otomatis memunculkan countdown dinamis (`⏳ Menuju Waktu Ashar: -04:59`) ketika sisa waktu adzan kurang dari 5 menit.
- **Audio & Visual Alert Masuk Waktu**:
  - Pilihan nada: **Adzan Merdu**, **Beep Pendek**, atau **Hening (Visual Saja)**.
- **Rotasi Ayat Al-Qur'an (Offline Bundled)**:
  - Mengambil acak dari 6.236 ayat Al-Qur'an lengkap (Arab Utsmani, Latin, dan Terjemahan Bahasa Indonesia resmi Kemenag).
  - Teks terkompresi langsung di dalam APK sehingga tidak membebani kuota internet.
- **Layar Selalu ON (Anti-Sleep / Keep Screen On)**:
  - Mencegah TV masuk ke mode screensaver atau sleep selama aplikasi dibuka (dapat diaktifkan/dinonaktifkan di pengaturan).
- **Pengaturan Ramah Remote TV**:
  - Ganti lokasi: Mendukung seluruh **514 Kota & Kabupaten se-Indonesia**.
  - Toggle tampilan: Teks Arab, Latin, dan Terjemahan.
  - Toggle audio alert & countdown timer.
- **Integrasi Launcher**:
  - Dapat dibuka langsung dari aplikasi atau diklik lewat card widget di home screen launcher (seperti `tv-home`).

---

## Cara Pasang Otomatis dengan Coding Agent (Paling Mudah)

1. Aktifkan **USB Debugging** di TV: *Settings → System → About → Tekan OK 7x pada Build → Developer Options → USB Debugging: ON*.
2. Buka folder repo ini di agen AI (Claude Code / Antigravity / Cursor) dan paste instruksi dari `PROMPT.md`.

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
- Lisensi Kode: MIT.
