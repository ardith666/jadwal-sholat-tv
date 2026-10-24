package com.ardith.sholattv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrayerEngine {
    private static final String TAG = "PrayerEngine";
    private static final String PREFS_NAME = "sholattv_prefs";
    private static final String KEY_CITY_ID = "city_id";
    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_MOSQUE_NAME = "mosque_name";
    private static final String DEFAULT_CITY_ID = "1624"; // Sidoarjo
    private static final String DEFAULT_CITY_NAME = "KAB. SIDOARJO";
    private static final String DEFAULT_MOSQUE_NAME = "";

    public static final class City {
        public final String id;
        public final String name;
        public final String province;
        public City(String id, String name) {
            this(id, name, "");
        }
        public City(String id, String name, String province) {
            this.id = id;
            this.name = name;
            this.province = province == null ? "" : province;
        }
        public String getDisplayName() {
            if (province == null || province.isEmpty()) return name;
            return name + " - " + province;
        }
        @Override
        public String toString() { return getDisplayName(); }
    }

    public static final class Slot {
        public final String name;
        public final String time;
        public final int minutes;
        public Slot(String name, String time, int minutes) {
            this.name = name;
            this.time = time;
            this.minutes = minutes;
        }
    }

    public static final class DaySchedule {
        public final String date;
        public final String tanggalText;
        public final String cityName;
        public final List<Slot> slots = new ArrayList<>();

        public DaySchedule(String date, String tanggalText, String cityName) {
            this.date = date;
            this.tanggalText = tanggalText;
            this.cityName = cityName;
        }
    }

    private final Context ctx;

    public PrayerEngine(Context context) {
        this.ctx = context.getApplicationContext();
    }

    public String getCityId() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CITY_ID, DEFAULT_CITY_ID);
    }

    public String getCityName() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CITY_NAME, DEFAULT_CITY_NAME);
    }

    public void setCity(String id, String name) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CITY_ID, id)
                .putString(KEY_CITY_NAME, name)
                .apply();
    }

    public String getMosqueName() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MOSQUE_NAME, DEFAULT_MOSQUE_NAME);
    }

    public void setMosqueName(String name) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MOSQUE_NAME, name == null ? "" : name)
                .apply();
    }

    public String getProvinceName() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("province_name", "");
    }

    public void setProvinceName(String name) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("province_name", name == null ? "" : name)
                .apply();
    }

    public List<City> loadCities() {
        List<City> list = new ArrayList<>();
        try (InputStream in = ctx.getAssets().open("data/cities.json");
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new City(o.getString("id"), o.getString("lokasi"), o.optString("daerah", "")));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cities", e);
        }
        return list;
    }

    public DaySchedule getTodaySchedule() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);
        String dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);

        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cacheKey = "sched_" + getCityId() + "_" + dateKey;
        String cachedJson = sp.getString(cacheKey, null);

        if (cachedJson != null) {
            DaySchedule s = parseSchedule(cachedJson);
            if (s != null) return s;
        }

        // Fetch online via MyQuran API v2
        String fetched = fetchMonth(getCityId(), year, month);
        if (fetched != null) {
            // Save cache for today
            sp.edit().putString(cacheKey, fetched).apply();
            DaySchedule s = parseSchedule(fetched);
            if (s != null) return s;
        }

        // Fallback default calculation / estimation
        return createFallback(dateKey);
    }

    private String fetchMonth(String cityId, int year, int month) {
        try {
            Calendar now = Calendar.getInstance();
            int day = now.get(Calendar.DAY_OF_MONTH);
            String urlStr = String.format(Locale.US,
                    "https://api.myquran.com/v2/sholat/jadwal/%s/%04d/%02d/%02d",
                    cityId, year, month, day);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "JadwalSholatTV/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                conn.disconnect();
                return sb.toString();
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Network fetch failed: " + e.getMessage());
        }
        return null;
    }

    private DaySchedule parseSchedule(String jsonStr) {
        try {
            JSONObject root = new JSONObject(jsonStr);
            if (!root.optBoolean("status", true)) return null;
            JSONObject data = root.optJSONObject("data");
            if (data == null) return null;

            String lokasi = data.optString("lokasi", getCityName());
            JSONObject j = data.optJSONObject("jadwal");
            if (j == null) return null;

            String date = j.optString("date", "");
            String tanggal = j.optString("tanggal", "");
            DaySchedule ds = new DaySchedule(date, tanggal, lokasi);

            addSlot(ds, "Subuh", j.optString("subuh"));
            addSlot(ds, "Dzuhur", j.optString("dzuhur"));
            addSlot(ds, "Ashar", j.optString("ashar"));
            addSlot(ds, "Maghrib", j.optString("maghrib"));
            addSlot(ds, "Isya", j.optString("isya"));

            return ds;
        } catch (Exception e) {
            Log.e(TAG, "Parse schedule error", e);
        }
        return null;
    }

    private void addSlot(DaySchedule ds, String name, String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return;
        int min = parseMinutes(timeStr);
        ds.slots.add(new Slot(name, timeStr, min));
    }

    public static int parseMinutes(String hhmm) {
        try {
            String[] p = hhmm.trim().split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private DaySchedule createFallback(String dateKey) {
        DaySchedule ds = new DaySchedule(dateKey, "Hari ini", getCityName());
        ds.slots.add(new Slot("Imsak", "04:10", parseMinutes("04:10")));
        ds.slots.add(new Slot("Subuh", "04:20", parseMinutes("04:20")));
        ds.slots.add(new Slot("Terbit", "05:35", parseMinutes("05:35")));
        ds.slots.add(new Slot("Dhuha", "06:00", parseMinutes("06:00")));
        ds.slots.add(new Slot("Dzuhur", "11:45", parseMinutes("11:45")));
        ds.slots.add(new Slot("Ashar", "14:55", parseMinutes("14:55")));
        ds.slots.add(new Slot("Maghrib", "17:50", parseMinutes("17:50")));
        ds.slots.add(new Slot("Isya", "19:00", parseMinutes("19:00")));
        return ds;
    }
}
