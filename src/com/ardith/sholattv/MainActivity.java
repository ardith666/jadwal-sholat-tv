package com.ardith.sholattv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SettingsDialog.OnSettingsChangeListener, WebServerManager.OnWebUpdateListener, SlideViewBuilder.StreamHolder {
    private static final String TAG = "MainActivity";

    private PrayerEngine prayerEngine;
    private QuranEngine quranEngine;
    private AlertManager alertManager;
    private SlideManager slideManager;
    private WebServerManager webServerManager;

    private ImageView bgImageView;
    private View scrimOverlay;

    // Multi-Slide Container
    private FrameLayout sliderContainer;
    private LinearLayout prayerSlideView; // Slide 1 (Jadwal Sholat & Quran)
    private View customSlideView; // Slide aktif lainnya (Kas, Pengumuman, Image, CCTV, Live)
    private int currentSlideIndex = 0;
    private List<SlideModel> activeSlides = new ArrayList<>();
    private boolean isAdzanApproaching = false;

    // Active RTSP VideoViews to stop/release when changing slides
    private final List<android.widget.VideoView> activeVideoViews = new ArrayList<>();

    private TextView clockText;
    private TextView dateText;
    private TextView locationBadge;
    private TextView settingsBtn;
    private TextView countdownBanner;
    private TextView creditText;
    private TextView versionText;

    private LinearLayout prayerCardsRow;
    private FrameLayout quranCardContainer;
    private TextView quranRefText;
    private TextView quranArabText;
    private TextView quranLatinText;
    private TextView quranTransText;

    private PrayerEngine.DaySchedule currentSchedule;
    private int highlightedSlotIndex = -1;
    private int lastAlertTriggeredMinute = -1;
    private int lastPlayedPipSecond = -1;

    private boolean isActivityActive = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            if (!isActivityActive) return;
            updateClockAndSchedule();
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable quranRotation = new Runnable() {
        @Override
        public void run() {
            if (!isActivityActive) return;
            rotateQuranVerse();
            handler.postDelayed(this, 25000);
        }
    };

    private final Runnable slideNextRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActivityActive) return;
            advanceToNextSlide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prayerEngine = new PrayerEngine(this);
        quranEngine = new QuranEngine(this);
        alertManager = new AlertManager(this);
        slideManager = new SlideManager(this);
        webServerManager = new WebServerManager(this, slideManager, prayerEngine, alertManager, this);

        buildUI();
        refreshSlidesList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
        applyKeepAwake();
        applyTheme();

        // Jalankan Web Server lokal (port 8080)
        if (webServerManager != null) {
            webServerManager.start();
        }

        loadScheduleAsync();
        handler.removeCallbacks(timerTick);
        handler.removeCallbacks(quranRotation);
        handler.post(timerTick);
        handler.postDelayed(quranRotation, 3000);

        scheduleNextSlide();
        updateFooterIpText();

        // Cek update otomatis di background jika diaktifkan (default: ON)
        if (alertManager != null && alertManager.isAutoUpdateCheckEnabled()) {
            handler.postDelayed(() -> {
                if (!isActivityActive) return;
                UpdateChecker.check(this, false, (updateAvailable, latestTag, downloadUrl, notes) -> {
                    if (updateAvailable && isActivityActive) {
                        UpdateChecker.showUpdateDialog(this, latestTag, downloadUrl, notes);
                    }
                });
            }, 4000);
        }
    }

    @Override
    public void registerVideoView(android.widget.VideoView vv) {
        if (vv != null && !activeVideoViews.contains(vv)) {
            activeVideoViews.add(vv);
        }
    }

    private void stopAllVideoStreams() {
        for (android.widget.VideoView vv : activeVideoViews) {
            try {
                if (vv != null) {
                    vv.stopPlayback();
                }
            } catch (Exception ignored) {}
        }
        activeVideoViews.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
        // ZERO BACKGROUND RESOURCE FOOTPRINT
        handler.removeCallbacksAndMessages(null);
        stopAllVideoStreams();
        if (webServerManager != null) {
            webServerManager.stop();
        }
        if (alertManager != null) {
            alertManager.stopAudio();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityActive = false;
        handler.removeCallbacksAndMessages(null);
        if (webServerManager != null) {
            webServerManager.stop();
        }
        if (alertManager != null) {
            alertManager.stopAudio();
        }
    }

    private void applyKeepAwake() {
        if (alertManager.isKeepAwake()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void applyTheme() {
        String customUri = alertManager.getCustomBgUri();
        if (customUri != null) {
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(customUri))) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                if (bmp != null) {
                    bgImageView.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }

        String themeFile = alertManager.getTheme();
        try (InputStream in = getAssets().open("backgrounds/" + themeFile)) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp != null) {
                bgImageView.setImageBitmap(bmp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading background: " + themeFile, e);
        }
    }

    private void buildUI() {
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        // Background Image View
        bgImageView = new ImageView(this);
        bgImageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        bgImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootLayout.addView(bgImageView);

        // Scrim Overlay
        scrimOverlay = new View(this);
        scrimOverlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        scrimOverlay.setBackgroundColor(0x99000000);
        rootLayout.addView(scrimOverlay);

        // Main Foreground Content
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(36), dp(20), dp(36), dp(16));
        content.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        // 1. HEADER (Jam, Tanggal, Lokasi, Tombol Settings)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout leftHeader = new LinearLayout(this);
        leftHeader.setOrientation(LinearLayout.VERTICAL);

        clockText = new TextView(this);
        clockText.setTextColor(0xFFFFFFFF);
        clockText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        clockText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clockText.setText("00:00:00");
        leftHeader.addView(clockText);

        dateText = new TextView(this);
        dateText.setTextColor(0xB3FFFFFF);
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dateText.setText("Memuat tanggal...");
        leftHeader.addView(dateText);

        header.addView(leftHeader, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout rightHeader = new LinearLayout(this);
        rightHeader.setOrientation(LinearLayout.HORIZONTAL);
        rightHeader.setGravity(Gravity.CENTER_VERTICAL);

        locationBadge = pill("📍 " + prayerEngine.getCityName());
        rightHeader.addView(locationBadge, llp(-2, dp(38), 0, 0, dp(12), 0));

        settingsBtn = pill("⚙ Pengaturan");
        settingsBtn.setOnClickListener(v -> SettingsDialog.show(this, prayerEngine, alertManager, slideManager, this));
        rightHeader.addView(settingsBtn, llp(-2, dp(38), 0, 0, 0, 0));

        // Beri prioritas focusability pada tombol Settings & Location
        locationBadge.setNextFocusRightId(settingsBtn.getId());
        settingsBtn.setNextFocusLeftId(locationBadge.getId());

        header.addView(rightHeader, new LinearLayout.LayoutParams(-2, -2));
        content.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // 2. COUNTDOWN BANNER (Muncul jika < 5 menit sebelum masuk sholat)
        countdownBanner = new TextView(this);
        countdownBanner.setTextColor(0xFFFFD700);
        countdownBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        countdownBanner.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        countdownBanner.setGravity(Gravity.CENTER);
        countdownBanner.setPadding(0, dp(6), 0, dp(2));
        countdownBanner.setVisibility(View.GONE);
        content.addView(countdownBanner, new LinearLayout.LayoutParams(-1, -2));

        // 3. SLIDER CONTAINER (Multi-Slide Layout)
        sliderContainer = new FrameLayout(this);
        sliderContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));

        // Build Slide 1: Prayer Cards + Quran
        prayerSlideView = new LinearLayout(this);
        prayerSlideView.setOrientation(LinearLayout.VERTICAL);
        prayerSlideView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        prayerCardsRow = new LinearLayout(this);
        prayerCardsRow.setOrientation(LinearLayout.HORIZONTAL);
        prayerCardsRow.setGravity(Gravity.CENTER);
        prayerCardsRow.setPadding(0, dp(12), 0, dp(12));
        prayerSlideView.addView(prayerCardsRow, new LinearLayout.LayoutParams(-1, -2));

        quranCardContainer = new FrameLayout(this);
        GradientDrawable quranBg = new GradientDrawable();
        quranBg.setCornerRadius(dp(16));
        quranBg.setColor(0x40000000);
        quranBg.setStroke(dp(1), 0x33FFFFFF);
        quranCardContainer.setBackground(quranBg);
        quranCardContainer.setPadding(dp(24), dp(14), dp(24), dp(12));

        LinearLayout quranContent = new LinearLayout(this);
        quranContent.setOrientation(LinearLayout.VERTICAL);
        quranContent.setGravity(Gravity.CENTER_HORIZONTAL);

        quranRefText = new TextView(this);
        quranRefText.setTextColor(0xFFFFD700);
        quranRefText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        quranRefText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        quranRefText.setGravity(Gravity.CENTER);
        quranContent.addView(quranRefText, llp(-1, -2, 0, 0, 0, dp(6)));

        quranArabText = new TextView(this);
        quranArabText.setTextColor(0xFFFFFFFF);
        quranArabText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        quranArabText.setGravity(Gravity.CENTER);
        quranArabText.setLineSpacing(dp(6), 1f);
        quranContent.addView(quranArabText, llp(-1, -2, 0, 0, 0, dp(6)));

        quranLatinText = new TextView(this);
        quranLatinText.setTextColor(0xCC7EE787);
        quranLatinText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        quranLatinText.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        quranLatinText.setGravity(Gravity.CENTER);
        quranContent.addView(quranLatinText, llp(-1, -2, 0, 0, 0, dp(4)));

        quranTransText = new TextView(this);
        quranTransText.setTextColor(0xE6E6EDF3);
        quranTransText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        quranTransText.setGravity(Gravity.CENTER);
        quranContent.addView(quranTransText, llp(-1, -2, 0, 0, 0, 0));

        quranCardContainer.addView(quranContent, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        prayerSlideView.addView(quranCardContainer, new LinearLayout.LayoutParams(-1, 0, 1f));

        sliderContainer.addView(prayerSlideView);
        content.addView(sliderContainer);

        // 4. FOOTER (Kiri: Versi & IP Web Admin | Kanan: Dibuat oleh : ardith666)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(6), dp(4), 0);

        versionText = new TextView(this);
        versionText.setTextColor(0x80FFFFFF);
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        versionText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        footer.addView(versionText, new LinearLayout.LayoutParams(0, -2, 1f));

        creditText = new TextView(this);
        creditText.setText("Dibuat oleh : ardith666");
        creditText.setTextColor(0x80FFFFFF);
        creditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        creditText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        creditText.setGravity(Gravity.END);
        footer.addView(creditText, new LinearLayout.LayoutParams(-2, -2));

        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        rootLayout.addView(content);
        setContentView(rootLayout);
        rotateQuranVerse();
    }

    private void updateFooterIpText() {
        String ip = NetworkUtils.getLocalIpAddress(this);
        versionText.setText(String.format("v1.5 | 🌐 Web Admin: http://%s:%d (PIN: %s)",
                ip, WebServerManager.PORT, slideManager.getAdminPin()));
    }

    private TextView pill(String label) {
        TextView p = new TextView(this);
        p.setText(label);
        p.setTextColor(0xE6FFFFFF);
        p.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(16), 0, dp(16), 0);
        p.setFocusable(true);
        p.setId(View.generateViewId());

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(19));
        bg.setColor(0x4D161B22);
        bg.setStroke(dp(1), 0x33FFFFFF);
        p.setBackground(bg);

        p.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : 0x4D161B22);
            p.setTextColor(has ? 0xFF0D1117 : 0xE6FFFFFF);
            v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(180).start();
        });
        return p;
    }

    // ==========================================
    // MULTI-SLIDE CAROUSEL LOGIC
    // ==========================================
    private void refreshSlidesList() {
        activeSlides = slideManager.getEnabledSlides();
        currentSlideIndex = 0;
        showSlideAt(0);
    }

    private void scheduleNextSlide() {
        handler.removeCallbacks(slideNextRunnable);
        if (activeSlides.size() <= 1) return;

        // Jangan ganti slide jika adzan sedang mendekat (< 5 menit)
        if (isAdzanApproaching) return;

        SlideModel current = activeSlides.get(currentSlideIndex % activeSlides.size());

        // Jika slide bertipe LIVE_STREAM dan di-lock, jangan otomatis pindah slide
        if (SlideModel.TYPE_LIVE_STREAM.equals(current.type) && current.isLocked) {
            Log.i(TAG, "Live Stream slide is locked, pausing automatic slide rotation");
            return;
        }

        int delayMs = Math.max(5, current.durationSec) * 1000;
        handler.postDelayed(slideNextRunnable, delayMs);
    }

    private void advanceToNextSlide() {
        if (activeSlides.size() <= 1 || isAdzanApproaching) return;
        SlideModel current = activeSlides.get(currentSlideIndex % activeSlides.size());
        if (SlideModel.TYPE_LIVE_STREAM.equals(current.type) && current.isLocked) return;

        currentSlideIndex = (currentSlideIndex + 1) % activeSlides.size();
        showSlideAt(currentSlideIndex);
        scheduleNextSlide();
    }

    private void showSlideAt(int idx) {
        if (activeSlides.isEmpty()) return;
        stopAllVideoStreams();

        SlideModel model = activeSlides.get(idx % activeSlides.size());

        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(260);
        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                if (SlideModel.TYPE_PRAYER.equals(model.type)) {
                    if (customSlideView != null) sliderContainer.removeView(customSlideView);
                    prayerSlideView.setVisibility(View.VISIBLE);
                } else {
                    prayerSlideView.setVisibility(View.GONE);
                    if (customSlideView != null) sliderContainer.removeView(customSlideView);
                    customSlideView = SlideViewBuilder.buildSlideView(MainActivity.this, model, slideManager, MainActivity.this);
                    sliderContainer.addView(customSlideView);
                }
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(320);
                sliderContainer.startAnimation(fadeIn);
            }
            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });

        sliderContainer.startAnimation(fadeOut);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Tombol BACK: Jika sedang di slide LIVE_STREAM (baik locked maupun tidak), keluar dari live stream
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!activeSlides.isEmpty()) {
                SlideModel cur = activeSlides.get(currentSlideIndex % activeSlides.size());
                if (SlideModel.TYPE_LIVE_STREAM.equals(cur.type)) {
                    exitLiveStream();
                    return true;
                }
            }
        }

        // Tombol D-pad ATAS: Langsung arahkan fokus ke tombol Pengaturan jika belum fokus
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (settingsBtn != null && !settingsBtn.isFocused()) {
                settingsBtn.requestFocus();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // Tombol D-pad BAWAH: Lepaskan fokus dari header kembali ke konten slider
            if (settingsBtn != null && settingsBtn.isFocused()) {
                if (quranCardContainer != null) {
                    quranCardContainer.requestFocus();
                    return true;
                }
            }
        }

        // Navigasi manual slide remote D-pad Kiri / Kanan
        // Hanya berganti slide jika fokus BUKAN di tombol Settings atau Location Badge
        boolean isHeaderFocused = (settingsBtn != null && settingsBtn.isFocused())
                || (locationBadge != null && locationBadge.isFocused());

        if (!isHeaderFocused) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && activeSlides.size() > 1) {
                currentSlideIndex = (currentSlideIndex - 1 + activeSlides.size()) % activeSlides.size();
                showSlideAt(currentSlideIndex);
                scheduleNextSlide();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && activeSlides.size() > 1) {
                advanceToNextSlide();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (!activeSlides.isEmpty()) {
            SlideModel cur = activeSlides.get(currentSlideIndex % activeSlides.size());
            if (SlideModel.TYPE_LIVE_STREAM.equals(cur.type)) {
                exitLiveStream();
                return;
            }
        }
        super.onBackPressed();
    }

    private void exitLiveStream() {
        Toast.makeText(this, "Siaran Live ditutup. Kembali ke jadwal normal.", Toast.LENGTH_SHORT).show();
        stopAllVideoStreams();

        // 1. Nonaktifkan lock pada live stream di memory & file json
        for (SlideModel s : slideManager.getAllSlides()) {
            if (SlideModel.TYPE_LIVE_STREAM.equals(s.type)) {
                s.isLocked = false;
                s.enabled = false; // Matikan live agar tidak kembali looping ke live
            }
        }
        slideManager.persist();

        // 2. Reload daftar slide aktif
        activeSlides = slideManager.getEnabledSlides();

        // 3. Cari index slide Jadwal Sholat (PRAYER)
        int prayerIdx = 0;
        for (int i = 0; i < activeSlides.size(); i++) {
            if (SlideModel.TYPE_PRAYER.equals(activeSlides.get(i).type)) {
                prayerIdx = i;
                break;
            }
        }

        currentSlideIndex = prayerIdx;
        showSlideAt(currentSlideIndex);
        scheduleNextSlide();
    }

    // ==========================================
    // PRAYER ENGINE & CLOCK
    // ==========================================
    private void loadScheduleAsync() {
        new Thread(() -> {
            PrayerEngine.DaySchedule ds = prayerEngine.getTodaySchedule();
            runOnUiThread(() -> {
                currentSchedule = ds;
                renderScheduleCards();
                updateClockAndSchedule();
            });
        }).start();
    }

    private void renderScheduleCards() {
        if (prayerCardsRow == null || currentSchedule == null) return;
        prayerCardsRow.removeAllViews();

        List<PrayerEngine.Slot> slots = currentSchedule.slots;
        int count = slots.size();
        for (int i = 0; i < count; i++) {
            PrayerEngine.Slot slot = slots.get(i);
            boolean isHighlighted = (i == highlightedSlotIndex);
            View card = createPrayerCard(slot.name, slot.time, isHighlighted);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(106), 1f);
            lp.setMargins(i == 0 ? 0 : dp(8), 0, i == count - 1 ? 0 : dp(8), 0);
            prayerCardsRow.addView(card, lp);
        }
    }

    private View createPrayerCard(String name, String time, boolean active) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        if (active) {
            bg.setColor(0x662EA043);
            bg.setStroke(dp(2), 0xFFFFD700);
            card.setScaleX(1.04f);
            card.setScaleY(1.04f);
        } else {
            bg.setColor(0x4D161B22);
            bg.setStroke(dp(1), 0x26FFFFFF);
            card.setScaleX(1f);
            card.setScaleY(1f);
        }
        card.setBackground(bg);

        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(active ? 0xFFFFD700 : 0xB3FFFFFF);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        n.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(n);

        TextView t = new TextView(this);
        t.setText(time);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(t, llp(-2, -2, 0, dp(6), 0, 0));

        return card;
    }

    private void updateClockAndSchedule() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        clockText.setText(timeFmt.format(now.getTime()));

        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        dateText.setText(dateFmt.format(now.getTime()));

        if (currentSchedule == null || currentSchedule.slots.isEmpty()) return;

        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int nowSec = now.get(Calendar.SECOND);

        int nextIdx = -1;
        int nextDiffSec = Integer.MAX_VALUE;

        List<PrayerEngine.Slot> slots = currentSchedule.slots;
        for (int i = 0; i < slots.size(); i++) {
            PrayerEngine.Slot s = slots.get(i);
            int slotTotalSec = s.minutes * 60;
            int currentTotalSec = nowMin * 60 + nowSec;
            if (slotTotalSec >= currentTotalSec) {
                nextIdx = i;
                nextDiffSec = slotTotalSec - currentTotalSec;
                break;
            }
        }
        if (nextIdx == -1) {
            nextIdx = 1; // Fallback to Subuh besok
        }

        if (nextIdx != highlightedSlotIndex) {
            highlightedSlotIndex = nextIdx;
            renderScheduleCards();
        }

        // Check countdown (< 5 menit = 300 detik)
        PrayerEngine.Slot nextSlot = slots.get(highlightedSlotIndex);
        if (nextDiffSec > 0 && nextDiffSec <= 300 && alertManager.isCountdownEnabled()) {
            isAdzanApproaching = true;
            // Lock screen pada Slide Jadwal Sholat
            if (customSlideView != null && prayerSlideView.getVisibility() != View.VISIBLE) {
                showSlideAt(0);
            }

            int m = nextDiffSec / 60;
            int s = nextDiffSec % 60;
            countdownBanner.setVisibility(View.VISIBLE);
            countdownBanner.setText(String.format(Locale.getDefault(),
                    "⏳ Menuju Waktu %s: -%02d:%02d", nextSlot.name, m, s));

            // Suara hitung mundur: 30s, 20s, dan 10 s/d 0s
            if ((nextDiffSec == 30 || nextDiffSec == 20 || (nextDiffSec >= 0 && nextDiffSec <= 10))
                    && nextDiffSec != lastPlayedPipSecond) {
                lastPlayedPipSecond = nextDiffSec;
                alertManager.playCountdownPip(nextDiffSec);
            }
        } else {
            countdownBanner.setVisibility(View.GONE);
            if (isAdzanApproaching && nextDiffSec > 300) {
                isAdzanApproaching = false;
                scheduleNextSlide();
            }
        }

        // Trigger alert adzan tepat pada waktu sholat (detik 0)
        if (nextDiffSec == 0 && nowMin != lastAlertTriggeredMinute) {
            lastAlertTriggeredMinute = nowMin;
            onPrayerTimeArrived(nextSlot.name);
        }
    }

    private void onPrayerTimeArrived(String prayerName) {
        countdownBanner.setVisibility(View.VISIBLE);
        countdownBanner.setText("🔔 Telah Masuk Waktu Sholat " + prayerName + "!");
        alertManager.playPrayerAlert();
        Toast.makeText(this, "Waktu Sholat " + prayerName + " Tiba", Toast.LENGTH_LONG).show();
    }

    private void rotateQuranVerse() {
        if (quranCardContainer == null || !isActivityActive) return;
        QuranEngine.Verse v = quranEngine.getRandomVerse();

        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                quranRefText.setText(v.getReference());

                if (alertManager.isShowArab()) {
                    quranArabText.setVisibility(View.VISIBLE);
                    quranArabText.setText(v.arab);
                } else {
                    quranArabText.setVisibility(View.GONE);
                }

                if (alertManager.isShowLatin()) {
                    quranLatinText.setVisibility(View.VISIBLE);
                    quranLatinText.setText(v.latin);
                } else {
                    quranLatinText.setVisibility(View.GONE);
                }

                if (alertManager.isShowTranslation()) {
                    quranTransText.setVisibility(View.VISIBLE);
                    quranTransText.setText("“" + v.translation + "”");
                } else {
                    quranTransText.setVisibility(View.GONE);
                }

                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(400);
                quranCardContainer.startAnimation(fadeIn);
            }
            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });
        quranCardContainer.startAnimation(fadeOut);
    }

    // ==========================================
    // LISTENER CALLBACKS
    // ==========================================
    @Override
    public void onLocationChanged() {
        locationBadge.setText("📍 " + prayerEngine.getCityName());
        loadScheduleAsync();
        Toast.makeText(this, "Lokasi diubah ke " + prayerEngine.getCityName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPreferencesChanged() {
        applyKeepAwake();
        rotateQuranVerse();
        Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onThemeChanged() {
        applyTheme();
        Toast.makeText(this, "Tema background diubah", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPickImageFromGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Pilih Gambar Background TV"), SettingsDialog.REQUEST_CODE_PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi Galeri / File Picker di TV", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSlidesUpdated() {
        refreshSlidesList();
        Toast.makeText(this, "Slide diperbarui via Web Admin", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSettingsUpdated() {
        locationBadge.setText("📍 " + prayerEngine.getCityName());
        loadScheduleAsync();
        applyKeepAwake();
        applyTheme();
        rotateQuranVerse();
        updateFooterIpText();
        Toast.makeText(this, "Pengaturan diperbarui via Web Admin", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SettingsDialog.REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                alertManager.setCustomBgUri(selectedUri.toString());
                applyTheme();
                Toast.makeText(this, "Background kustom dari Galeri diterapkan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityActive = false;
        handler.removeCallbacksAndMessages(null);
        if (webServerManager != null) {
            webServerManager.stop();
        }
        if (alertManager != null) alertManager.stopAudio();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams llp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }
}
