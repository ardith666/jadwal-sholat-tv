package com.ardith.sholattv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SlideManager {
    private static final String TAG = "SlideManager";
    private static final String FILE_NAME = "slides.json";
    private static final String PREFS_NAME = "sholattv_prefs";
    private static final String KEY_ADMIN_PIN = "admin_pin";
    public static final String DEFAULT_PIN = "1989";

    private final Context ctx;
    private final List<SlideModel> slides = new ArrayList<>();

    public SlideManager(Context context) {
        this.ctx = context.getApplicationContext();
        load();
    }

    public String getAdminPin() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ADMIN_PIN, DEFAULT_PIN);
    }

    public void setAdminPin(String pin) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ADMIN_PIN, pin).apply();
    }

    public synchronized List<SlideModel> getAllSlides() {
        return new ArrayList<>(slides);
    }

    /**
     * Menghasilkan daftar slide aktif untuk ditampilkan di carousel TV.
     * Jika ada slide ANNOUNCEMENT dengan multi-agenda (> 1 item), otomatis dipecah menjadi 2 slide:
     * 1. Slide Agenda Terdekat (Hero)
     * 2. Slide Daftar 6 Agenda Berikutnya
     */
    public synchronized List<SlideModel> getEnabledSlides() {
        List<SlideModel> res = new ArrayList<>();
        for (SlideModel s : slides) {
            if (s.enabled) {
                if (SlideModel.TYPE_ANNOUNCEMENT.equals(s.type) && s.agendaList != null && s.agendaList.size() > 1) {
                    // Slide A: Agenda Terdekat (Hero)
                    SlideModel hero = cloneSlide(s);
                    hero.agendaDisplayMode = 0;
                    res.add(hero);

                    // Slide B: Daftar 6 Agenda Berikutnya
                    SlideModel list = cloneSlide(s);
                    list.agendaDisplayMode = 1;
                    res.add(list);
                } else {
                    res.add(s);
                }
            }
        }
        if (res.isEmpty() && !slides.isEmpty()) {
            res.add(slides.get(0));
        }
        return res;
    }

    private SlideModel cloneSlide(SlideModel src) {
        JSONObject obj = src.toJsonObject();
        return SlideModel.fromJsonObject(obj);
    }

    public synchronized void saveSlides(List<SlideModel> newSlides) {
        slides.clear();
        slides.addAll(newSlides);
        persist();
    }

    public synchronized void addSlide(SlideModel slide) {
        slides.add(slide);
        persist();
    }

    public synchronized void deleteSlide(String id) {
        if ("slide_prayer_core".equals(id)) {
            return;
        }
        slides.removeIf(s -> s.id.equals(id));
        persist();
    }

    public synchronized SlideModel getSlideById(String id) {
        for (SlideModel s : slides) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    private synchronized void load() {
        slides.clear();
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                JSONArray arr = new JSONArray(sb.toString());
                for (int i = 0; i < arr.length(); i++) {
                    SlideModel m = SlideModel.fromJsonObject(arr.getJSONObject(i));
                    if (m != null) slides.add(m);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading slides.json", e);
            }
        }

        if (slides.isEmpty()) {
            initDefaultTemplates();
            persist();
        }
    }

    private void initDefaultTemplates() {
        // Slide 1: Jadwal Sholat & Quran (DEFAULT AKTIF)
        SlideModel s1 = new SlideModel("slide_prayer_core", SlideModel.TYPE_PRAYER, "Jadwal Sholat & Al-Qur'an", true, 45);

        // Slide 2: Template Kas Masjid (DEFAULT NON-AKTIF)
        SlideModel s2 = new SlideModel("slide_kas_masjid", SlideModel.TYPE_KAS, "Laporan Keuangan & Kas Masjid", false, 25);
        s2.saldoAwal = 0;
        s2.pemasukan = 0;
        s2.pengeluaran = 0;
        s2.catatanKas = "";

        // Slide 3: Template Agenda Kajian Multi-Item (DEFAULT NON-AKTIF)
        SlideModel s3 = new SlideModel("slide_agenda_kajian", SlideModel.TYPE_ANNOUNCEMENT, "Agenda Kajian & Pengumuman", false, 25);
        s3.agendaList.add(new SlideModel.AgendaItem("Kajian Akbar Akhir Pekan: Membedah Kitab Riyadhus Shalihin", "Ahad, 06.00 - 07.30 WIB", "Ust. Dr. H. Ahmad Fauzi", "Kajian terbuka untuk umum Ikhwan & Akhwat. Disediakan sarapan bersama."));
        s3.agendaList.add(new SlideModel.AgendaItem("Kajian Fiqih Ibadah", "Senin Ba'da Maghrib", "Ust. Abdullah", "Pembahasan tata cara sholat khusyuk sesuai sunnah."));
        s3.agendaList.add(new SlideModel.AgendaItem("Tahsin & Tahfidz Al-Qur'an", "Rabu, 18.30 WIB", "Ustadz Hamzah", "Bimbingan makharijul huruf & hafalan juz 30."));
        s3.agendaList.add(new SlideModel.AgendaItem("Kajian Khusus Muslimah / Akhwat", "Kamis, 09.00 - 11.00 WIB", "Ustadzah Fatimah, Lc", "Fiqih keluarga sakinah dan peran ibu dalam Islam."));
        s3.agendaList.add(new SlideModel.AgendaItem("Tabligh Akbar Menyambut Ramadhan", "Sabtu Malam Ahad", "Habib Muhammad Al-Haddad", "Persiapan ruhani & ilmu menyambut bulan suci Ramadhan."));
        s3.agendaList.add(new SlideModel.AgendaItem("Pemeriksaan Kesehatan Gratis", "Ahad Pekan Depan 07.30 WIB", "Tim Medis Rumah Sakit Islam", "Pengecekan tensi, gula darah, dan konsultasi dokter gratis."));
        s3.agendaList.add(new SlideModel.AgendaItem("Gerakan Bersih Masjid", "Jumat Pagi, 06.30 WIB", "Remaja Islam Masjid (RISMA)", "Kerja bakti bersama membersihkan karpet dan pelataran."));

        // Slide 4: Template Live Streaming Kajian / Khotbah (DEFAULT NON-AKTIF, SIAP DIAKTIFKAN)
        SlideModel s4 = new SlideModel("slide_live_khotbah", SlideModel.TYPE_LIVE_STREAM, "Live Siaran Khotbah & Kajian", false, 60);
        s4.liveRtspUrl = "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main";
        s4.isLocked = true; // Mode kunci saat live aktif
        s4.audioEnabled = false;

        // Slide 5: Template Multi-Stream CCTV (DEFAULT NON-AKTIF)
        SlideModel s5 = new SlideModel("slide_cctv_multi", SlideModel.TYPE_CCTV, "CCTV Pengawasan Masjid", false, 30);
        s5.cctvStreams.add(new SlideModel.CctvStream("Kamera Utama / Shaf Depan", "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main"));
        s5.cctvStreams.add(new SlideModel.CctvStream("Kamera Tempat Wudhu & Parkir", "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main"));

        // Slide 6: Template YouTube Live / Video (DEFAULT NON-AKTIF)
        SlideModel s6 = new SlideModel("slide_youtube_stream", SlideModel.TYPE_YOUTUBE, "Kajian YouTube Live / Video", false, 180);
        s6.youtubeUrl = "https://www.youtube.com/watch?v=live_stream";
        s6.isLocked = true;

        // Slide 7: Template Multi-Panel Dashboard Kiosk (DEFAULT NON-AKTIF, SIAP DIAKTIFKAN)
        SlideModel s7 = new SlideModel("slide_multi_panel", SlideModel.TYPE_MULTI_PANEL, "Dashboard Multi-Panel Terpadu", false, 60);
        s7.multiPanelPreset = "SPLIT_60_40";
        s7.slotLeftType = "LIVE_STREAM";
        s7.slotLeftUrl = "rtsp://admin:LNUMXL@192.168.1.114:554/ch1/main";
        s7.slotRightType = "PRAYER";
        s7.runningText = "Lurus dan rapatkan shaf saat sholat berjamaah. Matikan suara nada dering handphone selama berada di dalam masjid.";
        s7.runningTextSpeed = 2;
        s7.runningTextBg = "GREEN";

        slides.add(s1);
        slides.add(s2);
        slides.add(s3);
        slides.add(s4);
        slides.add(s5);
        slides.add(s6);
        slides.add(s7);
    }

    public synchronized void persist() {
        JSONArray arr = new JSONArray();
        for (SlideModel s : slides) {
            arr.put(s.toJsonObject());
        }
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Failed writing slides.json", e);
        }
    }

    public File getImageFolder() {
        File dir = new File(ctx.getFilesDir(), "custom_slides_img");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
}
