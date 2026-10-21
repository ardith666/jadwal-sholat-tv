package com.ardith.sholattv;

import org.json.JSONObject;

public class SlideModel {
    public static final String TYPE_PRAYER = "PRAYER";
    public static final String TYPE_KAS = "KAS";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_IMAGE = "IMAGE";

    public String id;
    public String type; // PRAYER, KAS, ANNOUNCEMENT, IMAGE
    public String title;
    public boolean enabled;
    public int durationSec; // Durasi tampil (detik)

    // Data spesifik Kas Masjid
    public long saldoAwal;
    public long pemasukan;
    public long pengeluaran;
    public String catatanKas;

    // Data spesifik Pengumuman / Agenda
    public String pengumumanTeks;
    public String tanggalAgenda;
    public String narasumber;

    // Data spesifik Gambar / Poster
    public String imageFileName; // path di context.getFilesDir() / "slides_img"

    public SlideModel(String id, String type, String title, boolean enabled, int durationSec) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.enabled = enabled;
        this.durationSec = durationSec;
    }

    public JSONObject toJsonObject() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("type", type);
            o.put("title", title);
            o.put("enabled", enabled);
            o.put("durationSec", durationSec);

            o.put("saldoAwal", saldoAwal);
            o.put("pemasukan", pemasukan);
            o.put("pengeluaran", pengeluaran);
            o.put("catatanKas", catatanKas != null ? catatanKas : "");

            o.put("pengumumanTeks", pengumumanTeks != null ? pengumumanTeks : "");
            o.put("tanggalAgenda", tanggalAgenda != null ? tanggalAgenda : "");
            o.put("narasumber", narasumber != null ? narasumber : "");

            o.put("imageFileName", imageFileName != null ? imageFileName : "");
        } catch (Exception ignored) {}
        return o;
    }

    public static SlideModel fromJsonObject(JSONObject o) {
        if (o == null) return null;
        String id = o.optString("id", "slide_" + System.currentTimeMillis());
        String type = o.optString("type", TYPE_PRAYER);
        String title = o.optString("title", "Slide");
        boolean enabled = o.optBoolean("enabled", true);
        int duration = o.optInt("durationSec", 30);

        SlideModel m = new SlideModel(id, type, title, enabled, duration);
        m.saldoAwal = o.optLong("saldoAwal", 0);
        m.pemasukan = o.optLong("pemasukan", 0);
        m.pengeluaran = o.optLong("pengeluaran", 0);
        m.catatanKas = o.optString("catatanKas", "");

        m.pengumumanTeks = o.optString("pengumumanTeks", "");
        m.tanggalAgenda = o.optString("tanggalAgenda", "");
        m.narasumber = o.optString("narasumber", "");

        m.imageFileName = o.optString("imageFileName", "");
        return m;
    }
}
