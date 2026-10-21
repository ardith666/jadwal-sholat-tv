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

    public synchronized List<SlideModel> getEnabledSlides() {
        List<SlideModel> res = new ArrayList<>();
        for (SlideModel s : slides) {
            if (s.enabled) res.add(s);
        }
        if (res.isEmpty() && !slides.isEmpty()) {
            // Minimal slide 1 (Jadwal Sholat) selalu tampil jika semua dinonaktifkan
            res.add(slides.get(0));
        }
        return res;
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
            // Slide utama jadwal sholat tidak boleh dihapus, hanya bisa diatur
            return;
        }
        slides.removeIf(s -> s.id.equals(id));
        persist();
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

        // Slide 2: Template Kas Masjid (DEFAULT NON-AKTIF, SIAP DIAKTIFKAN VIA WEB ADMIN)
        SlideModel s2 = new SlideModel("slide_kas_masjid", SlideModel.TYPE_KAS, "Laporan Keuangan & Kas Masjid", false, 25);
        s2.saldoAwal = 12500000;
        s2.pemasukan = 4250000;
        s2.pengeluaran = 1850000;
        s2.catatanKas = "Pemasukan Infaq Kotak Jumat & Operasional Kebersihan Masjid";

        // Slide 3: Template Pengumuman Kajian (DEFAULT NON-AKTIF, SIAP DIAKTIFKAN VIA WEB ADMIN)
        SlideModel s3 = new SlideModel("slide_agenda_kajian", SlideModel.TYPE_ANNOUNCEMENT, "Agenda Kajian & Pengumuman", false, 25);
        s3.pengumumanTeks = "Kajian Rutin Ahad Pagi: Membedah Kitab Riyadhus Shalihin bersama Ust. Fulan bin Fulan. Terbuka untuk umum Ikhwan & Akhwat. Disediakan sarapan bersama.";
        s3.tanggalAgenda = "Ahad, Pukul 06.00 - 07.30 WIB";
        s3.narasumber = "Dewan Kemakmuran Masjid (DKM)";

        slides.add(s1);
        slides.add(s2);
        slides.add(s3);
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
