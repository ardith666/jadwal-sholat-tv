package com.ardith.sholattv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SlideModel {
    public static final String TYPE_PRAYER = "PRAYER";
    public static final String TYPE_KAS = "KAS";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_CCTV = "CCTV";
    public static final String TYPE_LIVE_STREAM = "LIVE_STREAM";
    public static final String TYPE_YOUTUBE = "YOUTUBE";

    public static class AgendaItem {
        public String title;
        public String waktu;
        public String narasumber;
        public String desc;

        public AgendaItem(String title, String waktu, String narasumber, String desc) {
            this.title = title;
            this.waktu = waktu;
            this.narasumber = narasumber;
            this.desc = desc;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("title", title);
                o.put("waktu", waktu);
                o.put("narasumber", narasumber);
                o.put("desc", desc);
            } catch (Exception ignored) {}
            return o;
        }

        public static AgendaItem fromJson(JSONObject o) {
            if (o == null) return null;
            return new AgendaItem(
                    o.optString("title", ""),
                    o.optString("waktu", ""),
                    o.optString("narasumber", ""),
                    o.optString("desc", "")
            );
        }
    }

    public static class CctvStream {
        public String name;
        public String rtspUrl;

        public CctvStream(String name, String rtspUrl) {
            this.name = name;
            this.rtspUrl = rtspUrl;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("name", name);
                o.put("rtspUrl", rtspUrl);
            } catch (Exception ignored) {}
            return o;
        }

        public static CctvStream fromJson(JSONObject o) {
            if (o == null) return null;
            return new CctvStream(
                    o.optString("name", "Kamera"),
                    o.optString("rtspUrl", "")
            );
        }
    }

    public String id;
    public String type;
    public String title;
    public boolean enabled;
    public int durationSec;

    // Data Kas
    public long saldoAwal;
    public long pemasukan;
    public long pengeluaran;
    public String catatanKas;

    // Data Agenda / Pengumuman Multi-Item
    public List<AgendaItem> agendaList = new ArrayList<>();
    public int agendaDisplayMode = 0; // 0 = Agenda Utama/Terdekat (Hero), 1 = Daftar 6 Agenda Berikutnya

    // Data Gambar / Poster
    public String imageFileName;

    // Data RTSP (Live Stream & Multi-CCTV) & YouTube
    public String liveRtspUrl;
    public boolean isLocked; // Kunci layar TV saat live kajian / video youtube berlangsung
    public boolean audioEnabled;
    public List<CctvStream> cctvStreams = new ArrayList<>();
    public String youtubeUrl; // Link video atau live stream YouTube

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

            o.put("imageFileName", imageFileName != null ? imageFileName : "");

            // Agenda List
            JSONArray aArr = new JSONArray();
            if (agendaList != null) {
                for (AgendaItem a : agendaList) {
                    aArr.put(a.toJson());
                }
            }
            o.put("agendaList", aArr);
            o.put("agendaDisplayMode", agendaDisplayMode);

            // RTSP & CCTV & YouTube
            o.put("liveRtspUrl", liveRtspUrl != null ? liveRtspUrl : "");
            o.put("youtubeUrl", youtubeUrl != null ? youtubeUrl : "");
            o.put("isLocked", isLocked);
            o.put("audioEnabled", audioEnabled);

            JSONArray cArr = new JSONArray();
            if (cctvStreams != null) {
                for (CctvStream c : cctvStreams) {
                    cArr.put(c.toJson());
                }
            }
            o.put("cctvStreams", cArr);
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

        m.imageFileName = o.optString("imageFileName", "");

        // Agenda Items
        JSONArray aArr = o.optJSONArray("agendaList");
        if (aArr != null) {
            for (int i = 0; i < aArr.length(); i++) {
                AgendaItem item = AgendaItem.fromJson(aArr.optJSONObject(i));
                if (item != null) m.agendaList.add(item);
            }
        }
        m.agendaDisplayMode = o.optInt("agendaDisplayMode", 0);

        // Fallback backward-compat dari format v1.4 lama jika agendaList kosong tapi ada pengumumanTeks
        if (m.agendaList.isEmpty() && o.has("pengumumanTeks")) {
            String teks = o.optString("pengumumanTeks", "");
            String waktu = o.optString("tanggalAgenda", "");
            String narasumber = o.optString("narasumber", "");
            if (!teks.isEmpty() || !waktu.isEmpty()) {
                m.agendaList.add(new AgendaItem(title, waktu, narasumber, teks));
            }
        }

        // RTSP & YouTube
        m.liveRtspUrl = o.optString("liveRtspUrl", "");
        m.youtubeUrl = o.optString("youtubeUrl", "");
        m.isLocked = o.optBoolean("isLocked", false);
        m.audioEnabled = o.optBoolean("audioEnabled", false);

        JSONArray cArr = o.optJSONArray("cctvStreams");
        if (cArr != null) {
            for (int i = 0; i < cArr.length(); i++) {
                CctvStream stream = CctvStream.fromJson(cArr.optJSONObject(i));
                if (stream != null) m.cctvStreams.add(stream);
            }
        }

        return m;
    }
}
