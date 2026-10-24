package com.ardith.sholattv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SettingsDialog {

    public static final int REQUEST_CODE_PICK_IMAGE = 1001;

    public interface OnSettingsChangeListener {
        void onLocationChanged();
        void onPreferencesChanged();
        void onThemeChanged();
        void onPickImageFromGallery();
    }

    // 15 Tema Gelap (10 Foto + 5 Motif)
    public static final String[][] DARK_THEMES = new String[][]{
            {"photo_dark_01.jpg", "📸 Kauno Mosque Malam"},
            {"photo_dark_02.jpg", "📸 Menara Cahaya Malam"},
            {"photo_dark_03.jpg", "📸 Blue Mosque Istanbul"},
            {"photo_dark_04.jpg", "📸 Kubah Emas Malam"},
            {"photo_dark_05.jpg", "📸 Masjid Agung Malam"},
            {"photo_dark_06.jpg", "📸 Siluet Menara Senja"},
            {"photo_dark_07.jpg", "📸 Ka'bah Suci Makkah"},
            {"photo_dark_08.jpg", "📸 Lengkungan & Rembulan"},
            {"photo_dark_09.jpg", "📸 Pelataran Megah Malam"},
            {"photo_dark_10.jpg", "📸 Bulan Sabit di Langit"},
            {"01_midnight_kaaba.jpg", "🎨 Midnight Ka'bah Motif"},
            {"02_emerald_nabawi.jpg", "🎨 Emerald Nabawi Hijau"},
            {"03_sapphire_night.jpg", "🎨 Sapphire Night Biru"},
            {"04_royal_purple.jpg", "🎨 Royal Islamic Ungu"},
            {"05_charcoal_minimal.jpg", "🎨 Charcoal Slate Abu"}
    };

    // 15 Tema Terang (10 Foto + 5 Motif)
    public static final String[][] LIGHT_THEMES = new String[][]{
            {"photo_light_01.jpg", "📸 Sultan Omar Ali Saifuddin"},
            {"photo_light_02.jpg", "📸 Masjid Putih Cerah"},
            {"photo_light_03.jpg", "📸 Menara Langit Biru"},
            {"photo_light_04.jpg", "📸 Sheikh Zayed Grand Mosque"},
            {"photo_light_05.jpg", "📸 Cahaya Pagi Masjid"},
            {"photo_light_06.jpg", "📸 Pelataran Siang Megah"},
            {"photo_light_07.jpg", "📸 Kubah Golden Hour"},
            {"photo_light_08.jpg", "📸 Tiang Marmer Putih"},
            {"photo_light_09.jpg", "📸 Menara Menjulang Siang"},
            {"photo_light_10.jpg", "📸 Arsitektur Marmer Klasik"},
            {"06_ivory_andalusia.jpg", "🎨 Ivory Andalusia Emas"},
            {"07_desert_gold.jpg", "🎨 Desert Sand Gold"},
            {"08_sage_garden.jpg", "🎨 Sage Green Garden"},
            {"09_serene_sky.jpg", "🎨 Serene Sky Blue"},
            {"10_marble_white.jpg", "🎨 Marble White Kubah"}
    };

    public static String getThemeName(String file) {
        for (String[] t : DARK_THEMES) {
            if (t[0].equals(file)) return t[1];
        }
        for (String[] t : LIGHT_THEMES) {
            if (t[0].equals(file)) return t[1];
        }
        return "Kauno Mosque Malam";
    }

    public static void show(Context context, PrayerEngine prayerEngine, AlertManager alertManager, SlideManager slideManager, OnSettingsChangeListener listener) {
        ScrollView scroll = new ScrollView(context);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 28), dp(context, 16), dp(context, 28), dp(context, 20));

        // ==========================================
        // 1. HEADER CONTRIBUTOR & DUKUNGAN QRIS
        // ==========================================
        LinearLayout supportCard = new LinearLayout(context);
        supportCard.setOrientation(LinearLayout.HORIZONTAL);
        supportCard.setGravity(Gravity.CENTER_VERTICAL);
        supportCard.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
        supportCard.setFocusable(true);

        GradientDrawable supportBg = new GradientDrawable();
        supportBg.setCornerRadius(dp(context, 16));
        supportBg.setColor(0x332EA043);
        supportBg.setStroke(dp(context, 1), 0x667EE787);
        supportCard.setBackground(supportBg);

        supportCard.setOnFocusChangeListener((v, has) -> {
            supportBg.setStroke(dp(context, 2), has ? 0xFFFFD700 : 0x667EE787);
            supportBg.setColor(has ? 0x4D2EA043 : 0x332EA043);
            v.animate().scaleX(has ? 1.02f : 1f).scaleY(has ? 1.02f : 1f).setDuration(160).start();
        });

        // Gambar QRIS
        ImageView qrisView = new ImageView(context);
        qrisView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 110), dp(context, 110)));
        qrisView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrisView.setImageResource(R.drawable.qris);

        // Container putih untuk QRIS agar tajam dipindai kamera
        FrameLayout qrisFrame = new FrameLayout(context);
        qrisFrame.setBackgroundColor(0xFFFFFFFF);
        qrisFrame.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        qrisFrame.addView(qrisView);
        supportCard.addView(qrisFrame);

        // Pesan & Doa di sebelah kanan QRIS
        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(context, 18), 0, 0, 0);

        TextView headerTitle = new TextView(context);
        headerTitle.setText("Infaq & Dukungan Pengembangan");
        headerTitle.setTextColor(0xFFFFD700);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        headerTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textCol.addView(headerTitle);

        TextView supportMsg = new TextView(context);
        supportMsg.setText("Aplikasi ini dibuat tulus sebagai amal jariyah, gratis & open source. Jika Anda merasakan manfaatnya dan tergerak untuk menitipkan infaq operasional, silakan scan QRIS di samping. Doa tulus Anda pun adalah dukungan yang amat berharga bagi kami.");
        supportMsg.setTextColor(0xE6FFFFFF);
        supportMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        supportMsg.setPadding(0, dp(context, 3), 0, dp(context, 4));
        textCol.addView(supportMsg);

        TextView prayerMsg = new TextView(context);
        prayerMsg.setText("“Jazakumullahu khairan wa barakallahu fiikum”\n(Semoga Allah membalas kebaikan & memberkahi antum sekeluarga)");
        prayerMsg.setTextColor(0xFF7EE787);
        prayerMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        prayerMsg.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        textCol.addView(prayerMsg);

        supportCard.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(supportCard, new LinearLayout.LayoutParams(-1, -2));

        // Click QRIS card to view enlarged QRIS
        supportCard.setOnClickListener(v -> showEnlargedQris(context, prayerEngine, alertManager, slideManager, listener));

        // ==========================================
        // 1B. BANNER WEB ADMIN & IP PORT INFO
        // ==========================================
        String localIp = NetworkUtils.getLocalIpAddress(context);
        LinearLayout webAdminCard = new LinearLayout(context);
        webAdminCard.setOrientation(LinearLayout.VERTICAL);
        webAdminCard.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
        webAdminCard.setFocusable(true);

        GradientDrawable webBg = new GradientDrawable();
        webBg.setCornerRadius(dp(context, 14));
        webBg.setColor(0x331F6FEB);
        webBg.setStroke(dp(context, 1), 0x6658A6FF);
        webAdminCard.setBackground(webBg);

        webAdminCard.setOnFocusChangeListener((v, has) -> {
            webBg.setStroke(dp(context, 2), has ? 0xFFFFD700 : 0x6658A6FF);
            webBg.setColor(has ? 0x4D1F6FEB : 0x331F6FEB);
            v.animate().scaleX(has ? 1.02f : 1f).scaleY(has ? 1.02f : 1f).setDuration(160).start();
        });

        TextView webTitle = new TextView(context);
        webTitle.setText("🌐 Web Admin (Atur Slider, Kas & Gambar via HP/PC)");
        webTitle.setTextColor(0xFF58A6FF);
        webTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        webTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        webAdminCard.addView(webTitle);

        TextView webUrl = new TextView(context);
        webUrl.setText(String.format("Alamat: http://%s:%d\nPIN Default: %s (Koneksikan HP ke Wi-Fi yang sama)",
                localIp, WebServerManager.PORT, slideManager.getAdminPin()));
        webUrl.setTextColor(0xFFFFFFFF);
        webUrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        webUrl.setPadding(0, dp(context, 4), 0, 0);
        webAdminCard.addView(webUrl);

        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(-1, -2);
        webLp.setMargins(0, dp(context, 12), 0, 0);
        root.addView(webAdminCard, webLp);

        // Spacing divider
        View div = new View(context);
        div.setBackgroundColor(0x33FFFFFF);
        root.addView(div, new LinearLayout.LayoutParams(-1, dp(context, 1)));
        ((LinearLayout.LayoutParams) div.getLayoutParams()).setMargins(0, dp(context, 16), 0, dp(context, 14));

        // ==========================================
        // 2. DAFTAR PENGATURAN (SETTINGS BUTTONS)
        // ==========================================
        String currentBgLabel = alertManager.getCustomBgUri() != null
                ? "🖼 Foto Kustom dari Galeri"
                : "🖼 Background Aktif: " + getThemeName(alertManager.getTheme());

        String[] menuItems = new String[]{
                "📍 Ganti Lokasi Kota (" + prayerEngine.getCityName() + ")",
                "🌙 Pilih Background Tema GELAP (15 Pilihan)",
                "☀️ Pilih Background Tema TERANG (15 Pilihan)",
                "📂 Pilih Background dari Galeri TV",
                "🔄 Cek Pembaruan Versi Aplikasi",
                "🚀 Otomatis Cek Update saat Buka: " + (alertManager.isAutoUpdateCheckEnabled() ? "ON" : "OFF"),
                "🔊 Suara Waktu Sholat: " + getSoundLabel(alertManager.getSoundMode()),
                "⏱ Hitung Mundur 5 Menit: " + (alertManager.isCountdownEnabled() ? "ON" : "OFF"),
                "🔔 Suara Beep Countdown (30s, 20s, 10-0s): " + (alertManager.isCountdownAudioEnabled() ? "ON" : "OFF"),
                "📺 Layar Selalu ON (Anti-Sleep): " + (alertManager.isKeepAwake() ? "ON" : "OFF"),
                "📖 Tampilkan Teks Arab: " + (alertManager.isShowArab() ? "ON" : "OFF"),
                "🔤 Tampilkan Teks Latin: " + (alertManager.isShowLatin() ? "ON" : "OFF"),
                "🇮🇩 Tampilkan Terjemahan: " + (alertManager.isShowTranslation() ? "ON" : "OFF"),
                "🔒 Mode Kiosk (Auto-Start + Kunci Layar): " + (alertManager.isKioskEnabled() ? "ON" : "OFF")
        };

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (int i = 0; i < menuItems.length; i++) {
            final int index = i;
            TextView itemBtn = createMenuItem(context, menuItems[i]);
            itemBtn.setOnClickListener(v -> {
                dialogHolder[0].dismiss();
                switch (index) {
                    case 0:
                        showCityPicker(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 1:
                        showGalleryPicker(context, prayerEngine, alertManager, slideManager, listener, false); // Gelap
                        break;
                    case 2:
                        showGalleryPicker(context, prayerEngine, alertManager, slideManager, listener, true);  // Terang
                        break;
                    case 3:
                        if (listener != null) listener.onPickImageFromGallery();
                        break;
                    case 4:
                        UpdateChecker.check(context, true, (updateAvailable, latestTag, downloadUrl, notes) -> {
                            if (updateAvailable) {
                                UpdateChecker.showUpdateDialog(context, latestTag, downloadUrl, notes);
                            } else {
                                new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                        .setTitle("Pembaruan Aplikasi")
                                        .setMessage("Aplikasi Anda sudah menggunakan versi terbaru (v2.0).")
                                        .setPositiveButton("OK", (d, w) -> show(context, prayerEngine, alertManager, slideManager, listener))
                                        .setOnCancelListener(d -> show(context, prayerEngine, alertManager, slideManager, listener))
                                        .show();
                            }
                        });
                        break;
                    case 5:
                        alertManager.setAutoUpdateCheckEnabled(!alertManager.isAutoUpdateCheckEnabled());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 6:
                        showSoundPicker(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 7:
                        alertManager.setCountdownEnabled(!alertManager.isCountdownEnabled());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 8:
                        alertManager.setCountdownAudioEnabled(!alertManager.isCountdownAudioEnabled());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 9:
                        alertManager.setKeepAwake(!alertManager.isKeepAwake());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 10:
                        alertManager.setShowArab(!alertManager.isShowArab());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 11:
                        alertManager.setShowLatin(!alertManager.isShowLatin());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 12:
                        alertManager.setShowTranslation(!alertManager.isShowTranslation());
                        if (listener != null) listener.onPreferencesChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                        break;
                    case 13:
                        if (alertManager.isKioskEnabled()) {
                            promptPin(context, slideManager, () -> {
                                alertManager.setKioskEnabled(false);
                                if (listener != null) listener.onPreferencesChanged();
                                show(context, prayerEngine, alertManager, slideManager, listener);
                            });
                        } else {
                            alertManager.setKioskEnabled(true);
                            if (listener != null) listener.onPreferencesChanged();
                            show(context, prayerEngine, alertManager, slideManager, listener);
                        }
                        break;
                }
            });
            root.addView(itemBtn, new LinearLayout.LayoutParams(-1, -2));
            ((LinearLayout.LayoutParams) itemBtn.getLayoutParams()).setMargins(0, 0, 0, dp(context, 8));
        }

        scroll.addView(root);

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Pengaturan Jadwal Sholat TV")
                .setView(scroll)
                .setNegativeButton("Tutup", null)
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }

    private static TextView createMenuItem(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        tv.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 12));
        bg.setColor(0x40161B22);
        bg.setStroke(dp(context, 1), 0x26FFFFFF);
        tv.setBackground(bg);

        tv.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : 0x40161B22);
            tv.setTextColor(has ? 0xFF0D1117 : 0xFFFFFFFF);
            v.animate().scaleX(has ? 1.02f : 1f).scaleY(has ? 1.02f : 1f).setDuration(160).start();
        });
        return tv;
    }

    private static void showEnlargedQris(Context context, PrayerEngine pe, AlertManager am, SlideManager sm, OnSettingsChangeListener l) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(context, 24), dp(context, 16), dp(context, 24), dp(context, 16));

        ImageView iv = new ImageView(context);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 280), dp(context, 380)));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageResource(R.drawable.qris);

        FrameLayout whiteBox = new FrameLayout(context);
        whiteBox.setBackgroundColor(0xFFFFFFFF);
        whiteBox.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        whiteBox.addView(iv);
        layout.addView(whiteBox);

        TextView tv = new TextView(context);
        tv.setText("Infaq Operasional & Pengembangan Aplikasi\nScan QRIS dengan GoPay, OVO, Dana, ShopeePay, BCA, Livin, dll.\n\n“Jazakumullahu khairan wa barakallahu fiikum”");
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(context, 12), 0, 0);
        layout.addView(tv);

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Infaq & Dukungan Developer")
                .setView(layout)
                .setPositiveButton("Kembali", (dialog, which) -> show(context, pe, am, sm, l))
                .setOnCancelListener(dialog -> show(context, pe, am, sm, l))
                .show();
    }

    // ==========================================
    // 3. GALLERY THUMBNAIL PICKER (GELAP / TERANG)
    // ==========================================
    private static void showGalleryPicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, SlideManager slideManager, OnSettingsChangeListener listener, boolean isLight) {
        final String[][] themeSet = isLight ? LIGHT_THEMES : DARK_THEMES;
        String title = isLight ? "☀️ Koleksi Tema Terang (15 Pilihan)" : "🌙 Koleksi Tema Gelap (15 Pilihan)";

        ScrollView scroll = new ScrollView(context);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 16));

        final String activeTheme = alertManager.getTheme();
        AlertDialog[] dialogHolder = new AlertDialog[1];
        Map<String, Bitmap> thumbCache = new HashMap<>();

        int total = themeSet.length;
        for (int i = 0; i < total; i += 3) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);

            for (int j = 0; j < 3; j++) {
                int index = i + j;
                if (index < total) {
                    final String file = themeSet[index][0];
                    final String name = themeSet[index][1];
                    final boolean isSelected = file.equals(activeTheme);

                    LinearLayout card = new LinearLayout(context);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(Gravity.CENTER_HORIZONTAL);
                    card.setPadding(dp(context, 5), dp(context, 5), dp(context, 5), dp(context, 6));
                    card.setFocusable(true);

                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setCornerRadius(dp(context, 8));
                    cardBg.setColor(isSelected ? 0x4D2EA043 : 0x33161B22);
                    cardBg.setStroke(dp(context, isSelected ? 2 : 1), isSelected ? 0xFFFFD700 : 0x33FFFFFF);
                    card.setBackground(cardBg);

                    ImageView thumbView = new ImageView(context);
                    thumbView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 144), dp(context, 81))); // 16:9
                    thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    card.addView(thumbView);

                    TextView labelView = new TextView(context);
                    labelView.setText(name + (isSelected ? "\n✓ AKTIF" : ""));
                    labelView.setTextColor(isSelected ? 0xFFFFD700 : 0xFFFFFFFF);
                    labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    labelView.setGravity(Gravity.CENTER);
                    labelView.setPadding(0, dp(context, 3), 0, 0);
                    card.addView(labelView);

                    Bitmap thumb = thumbCache.get(file);
                    if (thumb == null) {
                        try (InputStream in = context.getAssets().open("backgrounds/" + file)) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = 8;
                            thumb = BitmapFactory.decodeStream(in, null, opts);
                            if (thumb != null) thumbCache.put(file, thumb);
                        } catch (Exception ignored) {}
                    }
                    if (thumb != null) {
                        thumbView.setImageBitmap(thumb);
                    }

                    card.setOnFocusChangeListener((v, has) -> {
                        cardBg.setColor(has ? 0x662EA043 : (file.equals(alertManager.getTheme()) ? 0x4D2EA043 : 0x33161B22));
                        cardBg.setStroke(dp(context, 2), has ? 0xFFFFFFFF : (file.equals(alertManager.getTheme()) ? 0xFFFFD700 : 0x33FFFFFF));
                        v.animate().scaleX(has ? 1.05f : 1f).scaleY(has ? 1.05f : 1f).setDuration(150).start();
                    });

                    card.setOnClickListener(v -> {
                        alertManager.setTheme(file);
                        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                        if (listener != null) listener.onThemeChanged();
                        show(context, prayerEngine, alertManager, slideManager, listener);
                    });

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(context, 154), -2);
                    cardLp.setMargins(dp(context, 4), 0, dp(context, 4), 0);
                    row.addView(card, cardLp);
                } else {
                    View spacer = new View(context);
                    LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(dp(context, 154), -2);
                    spacerLp.setMargins(dp(context, 4), 0, dp(context, 4), 0);
                    row.addView(spacer, spacerLp);
                }
            }

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.setMargins(0, 0, 0, dp(context, 10));
            layout.addView(row, rowLp);
        }

        scroll.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(scroll)
                .setNegativeButton("Kembali", (d, which) -> show(context, prayerEngine, alertManager, slideManager, listener))
                .setOnCancelListener(d -> show(context, prayerEngine, alertManager, slideManager, listener))
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }

    private static String getSoundLabel(String mode) {
        if (AlertManager.MODE_BEEP.equals(mode)) return "Beep Pendek";
        if (AlertManager.MODE_SILENT.equals(mode)) return "Hening (Visual Saja)";
        return "Adzan Merdu";
    }

    static void promptPin(Context context, SlideManager slideManager, Runnable onSuccess) {
        EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("PIN Web Admin");
        input.setSingleLine(true);

        FrameLayout wrap = new FrameLayout(context);
        int pad = dp(context, 20);
        wrap.setPadding(pad, dp(context, 6), pad, 0);
        wrap.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔒 Buka Kunci Kiosk")
                .setMessage("Masukkan PIN Web Admin untuk mematikan mode kiosk.")
                .setView(wrap)
                .setPositiveButton("Buka Kunci", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (slideManager.getAdminPin().equals(input.getText().toString().trim())) {
                dialog.dismiss();
                onSuccess.run();
            } else {
                input.setError("PIN salah");
                input.setText("");
            }
        }));
        dialog.show();
    }

    private static void showSoundPicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, SlideManager slideManager, OnSettingsChangeListener listener) {
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
                    show(context, prayerEngine, alertManager, slideManager, listener);
                })
                .setNegativeButton("Kembali", (dialog, which) -> show(context, prayerEngine, alertManager, slideManager, listener))
                .setOnCancelListener(dialog -> show(context, prayerEngine, alertManager, slideManager, listener))
                .show();
    }

    private static void showCityPicker(Context context, PrayerEngine prayerEngine, AlertManager alertManager, SlideManager slideManager, OnSettingsChangeListener listener) {
        List<PrayerEngine.City> allCities = prayerEngine.loadCities();
        if (allCities.isEmpty()) {
            show(context, prayerEngine, alertManager, slideManager, listener);
            return;
        }

        Collections.sort(allCities, (a, b) -> a.name.compareToIgnoreCase(b.name));

        final List<PrayerEngine.City> filtered = new ArrayList<>(allCities);
        final List<String> displayList = new ArrayList<>();
        for (PrayerEngine.City c : filtered) displayList.add(c.getDisplayName());

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
                .setNegativeButton("Kembali", (d, which) -> show(context, prayerEngine, alertManager, slideManager, listener))
                .setOnCancelListener(d -> show(context, prayerEngine, alertManager, slideManager, listener))
                .create();

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                filtered.clear();
                displayList.clear();
                for (PrayerEngine.City c : allCities) {
                    if (query.isEmpty()
                            || c.name.toLowerCase(Locale.ROOT).contains(query)
                            || c.province.toLowerCase(Locale.ROOT).contains(query)) {
                        filtered.add(c);
                        displayList.add(c.getDisplayName());
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
                show(context, prayerEngine, alertManager, slideManager, listener);
            }
        });

        dialog.show();
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
