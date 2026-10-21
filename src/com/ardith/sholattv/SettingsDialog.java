package com.ardith.sholattv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import java.util.List;

public final class SettingsDialog {

    public interface OnSettingsChangeListener {
        void onLocationChanged();
        void onPreferencesChanged();
    }

    public static void show(Context context, PrayerEngine prayerEngine, AlertManager alertManager, OnSettingsChangeListener listener) {
        String[] options = new String[]{
                "📍 Ganti Lokasi Kota (" + prayerEngine.getCityName() + ")",
                "🔊 Suara Waktu Sholat: " + getSoundLabel(alertManager.getSoundMode()),
                "⏱ Hitung Mundur 5 Menit: " + (alertManager.isCountdownEnabled() ? "ON" : "OFF"),
                "📺 Layar Selalu ON (Anti-Sleep): " + (alertManager.isKeepAwake() ? "ON" : "OFF"),
                "📖 Tampilkan Teks Arab: " + (alertManager.isShowArab() ? "ON" : "OFF"),
                "🔤 Tampilkan Teks Latin: " + (alertManager.isShowLatin() ? "ON" : "OFF"),
                "🇮🇩 Tampilkan Terjemahan: " + (alertManager.isShowTranslation() ? "ON" : "OFF")
        };

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pengaturan Jadwal Sholat TV")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCityPicker(context, prayerEngine, listener);
                            break;
                        case 1:
                            showSoundPicker(context, alertManager, listener);
                            break;
                        case 2:
                            alertManager.setCountdownEnabled(!alertManager.isCountdownEnabled());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 3:
                            alertManager.setKeepAwake(!alertManager.isKeepAwake());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 4:
                            alertManager.setShowArab(!alertManager.isShowArab());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 5:
                            alertManager.setShowLatin(!alertManager.isShowLatin());
                            if (listener != null) listener.onPreferencesChanged();
                            break;
                        case 6:
                            alertManager.setShowTranslation(!alertManager.isShowTranslation());
                            if (listener != null) listener.onPreferencesChanged();
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
        List<PrayerEngine.City> cities = prayerEngine.loadCities();
        if (cities.isEmpty()) return;

        String[] cityNames = new String[cities.size()];
        for (int i = 0; i < cities.size(); i++) {
            cityNames[i] = cities.get(i).name;
        }

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pilih Kota / Kabupaten")
                .setItems(cityNames, (dialog, which) -> {
                    PrayerEngine.City selected = cities.get(which);
                    prayerEngine.setCity(selected.id, selected.name);
                    dialog.dismiss();
                    if (listener != null) listener.onLocationChanged();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
