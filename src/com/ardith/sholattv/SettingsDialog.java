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

    public static final int REQUEST_CODE_PICK_IMAGE = 1001;

    public interface OnSettingsChangeListener {
        void onLocationChanged();
        void onPreferencesChanged();
        void onThemeChanged();
        void onPickImageFromGallery();
    }

    public static void show(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
        String currentBgLabel = alertManager.getCustomBgUri() != null
                ? "🖼 Foto Kustom dari Galeri"
                : "🖼 Tema Background: " + getThemeName(alertManager.getTheme());

        String[] options = new String[]{
                "📍 Ganti Lokasi Kota (" + prayerEngine.getCityName() + ")",
                currentBgLabel,
                "📂 Pilih Background dari Galeri TV",
                "🔊 Suara Waktu Sholat: " + getSoundLabel(alertManager.getSoundMode()),
                "⏱ Hitung Mundur 5 Menit: " + (alertManager.isCountdownEnabled() ? "ON" : "OFF"),
                "🔔 Suara Beep Countdown (30s, 20s, 10-0s): " + (alertManager.isCountdownAudioEnabled() ? "ON" : "OFF"),
                "📺 Layar Selalu ON (Anti-Sleep): " + (alertManager.isKeepAwake() ? "ON" : "OFF"),
                "📖 Tampilkan Teks Arab: " + (alertManager.isShowArab() ? "ON" : "OFF"),
                "🔤 Tampilkan Teks Latin: " + (alertManager.isShowLatin() ? "ON" : "OFF"),
                "🇮🇩 Tampilkan Terjemahan: " + (alertManager.isShowTranslation() ? "ON" : "OFF"),
                "ℹ️ Dibuat oleh: ardith666 (Contributor)"
        };

        AlertDialog[] dialogHolder = new AlertDialog[1];

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pengaturan Jadwal Sholat TV")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0:
                            showCityPicker(context, prayerEngine, alertManager, listener);
                            break;
                        case 1:
                            showThemePicker(context, prayerEngine, alertManager, listener);
                            break;
                        case 2:
                            if (listener != null) listener.onPickImageFromGallery();
                            break;
                        case 3:
                            showSoundPicker(context, prayerEngine, alertManager, listener);
                            break;
                        case 4:
                            alertManager.setCountdownEnabled(!alertManager.isCountdownEnabled());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 5:
                            alertManager.setCountdownAudioEnabled(!alertManager.isCountdownAudioEnabled());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 6:
                            alertManager.setKeepAwake(!alertManager.isKeepAwake());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 7:
                            alertManager.setShowArab(!alertManager.isShowArab());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 8:
                            alertManager.setShowLatin(!alertManager.isShowLatin());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 9:
                            alertManager.setShowTranslation(!alertManager.isShowTranslation());
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, listener);
                            break;
                        case 10:
                            new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("Tentang Aplikasi")
                                    .setMessage("Jadwal Sholat TV v1.2\n\nDibuat oleh: ardith666\nGitHub: github.com/ardith666/jadwal-sholat-tv\n\nFitur lengkap: Kiosk ambient, 30 background (20 foto masjid & 10 motif), audio countdown 30s/20s/10-0s, custom gallery, zero background RAM usage.")
                                    .setPositiveButton("Tutup", (diag, w) -> show(context, prayerEngine, alertManager, listener))
                                    .setOnCancelListener(diag -> show(context, prayerEngine, alertManager, listener))
                                    .show();
                            break;
                    }
                })
                .setNegativeButton("Tutup", null)
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }

    private static String getSoundLabel(String mode) {
        if (AlertManager.MODE_BEEP.equals(mode)) return "Beep Pendek";
        if (AlertManager.MODE_SILENT.equals(mode)) return "Hening (Visual Saja)";
        return "Adzan Merdu";
    }

    public static final String[][] THEMES = new String[][]{
            // --- 10 Foto Masjid Malam / Gelap ---
            {"photo_dark_01.jpg", "📸 [Foto Malam] Kauno Mosque Night"},
            {"photo_dark_02.jpg", "📸 [Foto Malam] Illuminated Minaret Night"},
            {"photo_dark_03.jpg", "📸 [Foto Malam] Blue Mosque Istanbul Night"},
            {"photo_dark_04.jpg", "📸 [Foto Malam] Golden Mosque Dome Night"},
            {"photo_dark_05.jpg", "📸 [Foto Malam] Grand Mosque Lights"},
            {"photo_dark_06.jpg", "📸 [Foto Malam] Twilight Minaret Silhouette"},
            {"photo_dark_07.jpg", "📸 [Foto Malam] Holy Ka'bah Makkah Night"},
            {"photo_dark_08.jpg", "📸 [Foto Malam] Islamic Arch & Moon"},
            {"photo_dark_09.jpg", "📸 [Foto Malam] Illuminated Grand Courtyard"},
            {"photo_dark_10.jpg", "📸 [Foto Malam] Crescent Sky over Mosque"},

            // --- 10 Foto Masjid Siang / Terang ---
            {"photo_light_01.jpg", "📸 [Foto Siang] Sultan Omar Ali Saifuddin"},
            {"photo_light_02.jpg", "📸 [Foto Siang] White Mosque Sunny Sky"},
            {"photo_light_03.jpg", "📸 [Foto Siang] Minarets under Clear Blue Sky"},
            {"photo_light_04.jpg", "📸 [Foto Siang] Sheikh Zayed Grand Mosque"},
            {"photo_light_05.jpg", "📸 [Foto Siang] Majestic Mosque Sunlight"},
            {"photo_light_06.jpg", "📸 [Foto Siang] Blue Mosque Daylight Courtyard"},
            {"photo_light_07.jpg", "📸 [Foto Siang] Golden Hour Mosque Dome"},
            {"photo_light_08.jpg", "📸 [Foto Siang] White Marble Columns"},
            {"photo_light_09.jpg", "📸 [Foto Siang] Skyward Minarets Sunlight"},
            {"photo_light_10.jpg", "📸 [Foto Siang] Grand Marble Architecture"},

            // --- 10 Motif Geometris Islamic ---
            {"01_midnight_kaaba.jpg", "🎨 [Motif Gelap] Midnight Ka'bah Minimal"},
            {"02_emerald_nabawi.jpg", "🎨 [Motif Gelap] Emerald Nabawi Green"},
            {"03_sapphire_night.jpg", "🎨 [Motif Gelap] Sapphire Night Blue"},
            {"04_royal_purple.jpg", "🎨 [Motif Gelap] Royal Islamic Purple"},
            {"05_charcoal_minimal.jpg", "🎨 [Motif Gelap] Charcoal Slate Minimal"},
            {"06_ivory_andalusia.jpg", "🎨 [Motif Terang] Ivory Andalusia Gold"},
            {"07_desert_gold.jpg", "🎨 [Motif Terang] Desert Sand Gold"},
            {"08_sage_garden.jpg", "🎨 [Motif Terang] Sage Green Garden"},
            {"09_serene_sky.jpg", "🎨 [Motif Terang] Serene Sky Blue"},
            {"10_marble_white.jpg", "🎨 [Motif Terang] Marble White Arch"}
    };

    private static String getThemeName(String file) {
        for (String[] t : THEMES) {
            if (t[0].equals(file)) return t[1];
        }
        return "Foto Malam Kauno Mosque";
    }

    private static void showThemePicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
        String[] names = new String[THEMES.length];
        int selected = 0;
        String cur = alertManager.getTheme();
        for (int i = 0; i < THEMES.length; i++) {
            names[i] = THEMES[i][1];
            if (THEMES[i][0].equals(cur)) selected = i;
        }

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilih Background (20 Foto + 10 Motif)")
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    alertManager.setTheme(THEMES[which][0]);
                    dialog.dismiss();
                    if (listener != null) listener.onThemeChanged();
                    show(context, prayerEngine, alertManager, listener);
                })
                .setNegativeButton("Kembali", (dialog, which) -> show(context, prayerEngine, alertManager, listener))
                .setOnCancelListener(dialog -> show(context, prayerEngine, alertManager, listener))
                .show();
    }

    private static void showSoundPicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
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
                    show(context, prayerEngine, alertManager, listener);
                })
                .setNegativeButton("Kembali", (dialog, which) -> show(context, prayerEngine, alertManager, listener))
                .setOnCancelListener(dialog -> show(context, prayerEngine, alertManager, listener))
                .show();
    }

    private static void showCityPicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
        List<PrayerEngine.City> allCities = prayerEngine.loadCities();
        if (allCities.isEmpty()) {
            show(context, prayerEngine, alertManager, listener);
            return;
        }

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
                .setNegativeButton("Kembali", (d, which) -> show(context, prayerEngine, alertManager, listener))
                .setOnCancelListener(d -> show(context, prayerEngine, alertManager, listener))
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
                show(context, prayerEngine, alertManager, listener);
            }
        });

        dialog.show();
    }
}
