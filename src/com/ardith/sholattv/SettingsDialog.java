package com.ardith.sholattv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SettingsDialog {

    public interface OnSettingsChangeListener {
        void onLocationChanged();
        void onPreferencesChanged();
        void onThemeChanged();
    }

    public static void show(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
        String[] options = new String[]{
                "📍 Ganti Lokasi Kota (" + prayerEngine.getCityName() + ")",
                "🖼 Pilihan Background Tema (" + getThemeName(alertManager.getTheme()) + ")",
                "🔊 Suara Waktu Sholat: " + getSoundLabel(alertManager.getSoundMode()),
                "⏱ Hitung Mundur 5 Menit: " + (alertManager.isCountdownEnabled() ? "ON" : "OFF"),
                "📺 Layar Selalu ON (Anti-Sleep): " + (alertManager.isKeepAwake() ? "ON" : "OFF"),
                "📖 Tampilkan Teks Arab: " + (alertManager.isShowArab() ? "ON" : "OFF"),
                "🔤 Tampilkan Teks Latin: " + (alertManager.isShowLatin() ? "ON" : "OFF"),
                "🇮🇩 Tampilkan Terjemahan: " + (alertManager.isShowTranslation() ? "ON" : "OFF"),
                "ℹ️ Dibuat oleh: ardith666 (Contributor)"
        };

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pengaturan Jadwal Sholat TV")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCityPicker(context, prayerEngine, listener);
                            break;
                        case 1:
                            showThemePicker(context, alertManager, listener);
                            break;
                        case 2:
                            showSoundPicker(context, alertManager, listener);
                            break;
                        case 3:
                            alertManager.setCountdownEnabled(!alertManager.isCountdownEnabled());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 4:
                            alertManager.setKeepAwake(!alertManager.isKeepAwake());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 5:
                            alertManager.setShowArab(!alertManager.isShowArab());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 6:
                            alertManager.setShowLatin(!alertManager.isShowLatin());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 7:
                            alertManager.setShowTranslation(!alertManager.isShowTranslation());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 8:
                            new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("Tentang Aplikasi")
                                    .setMessage("Jadwal Sholat TV v1.1\n\nDibuat oleh: ardith666\nGitHub: github.com/ardith666/jadwal-sholat-tv\n\nKhusus Android TV & Google TV.")
                                    .setPositiveButton("Tutup", null)
                                    .show();
                            break;
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private static String getSoundLabel(String mode) {
        if (AlertManager.MODE_BEEP.equals(mode)) return "Beep Pendek";
        if (AlertManager.MODE_SILENT.equals(mode)) return "Hening (Visual Saja)";
        return "Adzan Merdu";
    }

    public static final String[][] THEMES = new String[][]{
            // 5 Tema Gelap
            {"01_midnight_kaaba.jpg", "🌙 Midnight Ka'bah (Gelap)"},
            {"02_emerald_nabawi.jpg", "🕌 Emerald Nabawi (Gelap)"},
            {"03_sapphire_night.jpg", "🌌 Sapphire Night (Gelap)"},
            {"04_royal_purple.jpg", "🟣 Royal Islamic Purple (Gelap)"},
            {"05_charcoal_minimal.jpg", "🌑 Charcoal Minimal (Gelap)"},
            // 5 Tema Terang
            {"06_ivory_andalusia.jpg", "☀️ Ivory Andalusia (Terang)"},
            {"07_desert_gold.jpg", "🏜 Desert Gold (Terang)"},
            {"08_sage_garden.jpg", "🌿 Sage Green Garden (Terang)"},
            {"09_serene_sky.jpg", "⛅ Serene Sky Blue (Terang)"},
            {"10_marble_white.jpg", "🏛 Marble White Mosque (Terang)"}
    };

    private static String getThemeName(String file) {
        for (String[] t : THEMES) {
            if (t[0].equals(file)) return t[1];
        }
        return "Midnight Ka'bah (Gelap)";
    }

    private static void showThemePicker(Context context, AlertManager alertManager, OnSettingsChangeListener listener) {
        String[] names = new String[THEMES.length];
        int selected = 0;
        String cur = alertManager.getTheme();
        for (int i = 0; i < THEMES.length; i++) {
            names[i] = THEMES[i][1];
            if (THEMES[i][0].equals(cur)) selected = i;
        }

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilih Tema Background (10 Tema)")
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    alertManager.setTheme(THEMES[which][0]);
                    dialog.dismiss();
                    if (listener != null) listener.onThemeChanged();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private static void showSoundPicker(Context context, AlertManager alertManager, OnSettingsChangeListener listener) {
        String[] modes = new String[]{"Adzan Merdu", "Beep Pendek", "Hening (Visual Saja)"};
        final String[] keys = new String[]{AlertManager.MODE_ADZAN, AlertManager.MODE_BEEP, AlertManager.MODE_SILENT};

        int current = 0;
        String curKey = alertManager.getSoundMode();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(curKey)) { current = i; break; }
        }

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilih Suara Masuk Waktu Sholat")
                .setSingleChoiceItems(modes, current, (dialog, which) -> {
                    alertManager.setSoundMode(keys[which]);
                    dialog.dismiss();
                    if (listener != null) listener.onPreferencesChanged();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private static void showCityPicker(Context context, PrayerEngine prayerEngine, OnSettingsChangeListener listener) {
        List<PrayerEngine.City> allCities = prayerEngine.loadCities();
        if (allCities.isEmpty()) return;

        // Sort ASC by text
        Collections.sort(allCities, (a, b) -> a.name.compareToIgnoreCase(b.name));

        final List<PrayerEngine.City> filtered = new ArrayList<>(allCities);
        final List<String> displayList = new ArrayList<>();
        for (PrayerEngine.City c : filtered) displayList.add(c.name);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 20, 32, 10);

        EditText searchBox = new EditText(context);
        searchBox.setHint("🔍 Ketik nama kota / kabupaten...");
        searchBox.setSingleLine(true);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setHintTextColor(0x88FFFFFF);
        layout.addView(searchBox);

        ListView listView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);
        layout.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500));

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilih Kota / Kabupaten (Urut A-Z)")
                .setView(layout)
                .setNegativeButton("Batal", null)
                .create();

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                filtered.clear();
                displayList.clear();
                for (PrayerEngine.City c : allCities) {
                    if (query.isEmpty() || c.name.toLowerCase(Locale.ROOT).contains(query)) {
                        filtered.add(c);
                        displayList.add(c.name);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < filtered.size()) {
                PrayerEngine.City selected = filtered.get(position);
                prayerEngine.setCity(selected.id, selected.name);
                dialog.dismiss();
                if (listener != null) listener.onLocationChanged();
            }
        });

        dialog.show();
    }
}
