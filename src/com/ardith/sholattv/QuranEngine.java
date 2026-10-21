package com.ardith.sholattv;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public final class QuranEngine {
    private static final String TAG = "QuranEngine";

    public static final class Verse {
        public final int surahNumber;
        public final int ayahNumber;
        public final String surahName;
        public final String arab;
        public final String latin;
        public final String translation;

        public Verse(int surahNumber, int ayahNumber, String surahName, String arab, String latin, String translation) {
            this.surahNumber = surahNumber;
            this.ayahNumber = ayahNumber;
            this.surahName = surahName;
            this.arab = arab;
            this.latin = latin;
            this.translation = translation;
        }

        public String getReference() {
            return "QS. " + surahName + " [" + surahNumber + ":" + ayahNumber + "]";
        }
    }

    private final Context ctx;
    private JSONArray rawArray;
    private final Random random = new Random();

    public QuranEngine(Context context) {
        this.ctx = context.getApplicationContext();
        loadAsync();
    }

    private void loadAsync() {
        new Thread(() -> {
            try (InputStream in = ctx.getAssets().open("quran/quran_compact.json");
                 BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                rawArray = new JSONArray(sb.toString());
                Log.i(TAG, "Quran loaded: " + rawArray.length() + " verses");
            } catch (Exception e) {
                Log.e(TAG, "Failed loading quran_compact.json", e);
            }
        }).start();
    }

    public Verse getRandomVerse() {
        if (rawArray == null || rawArray.length() == 0) {
            // Default initial verse: Al-Fatihah 1
            return new Verse(1, 1, "Al-Fatihah",
                    "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                    "Bismillāhir-raḥmānir-raḥīm",
                    "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.");
        }
        try {
            int idx = random.nextInt(rawArray.length());
            JSONArray v = rawArray.getJSONArray(idx);
            int sNum = v.getInt(0);
            int aNum = v.getInt(1);
            String sName = v.getString(2);
            String ar = v.getString(3);
            String lt = v.getString(4);
            String tr = v.getString(5);
            return new Verse(sNum, aNum, sName, ar, lt, tr);
        } catch (Exception e) {
            return new Verse(1, 1, "Al-Fatihah",
                    "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                    "Bismillāhir-raḥmānir-raḥīm",
                    "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.");
        }
    }
}
