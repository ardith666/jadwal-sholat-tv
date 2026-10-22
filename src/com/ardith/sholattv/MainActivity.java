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

import androidx.media3.exoplayer.ExoPlayer;

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

        // Active ExoPlayers to release when changing slides or pausing
    private final List<ExoPlayer> activePlayers = new ArrayList<>();
    private final List<android.webkit.WebView> activeWebViews = new ArrayList<>();

    // Overlay State: Iqomah Countdown Besar & Layar Hijau Sholat
    private FrameLayout iqomahOverlay;
    private TextView iqomahCountdownText;
    private TextView iqomahLabelText;

    private FrameLayout greenScreenOverlay;
    private TextView greenScreenText;

    private boolean isIqomahPhaseActive = false;
    private long iqomahTargetTimestamp = 0;
    private long greenScreenEndTimestamp = 0;
    private String currentPrayerNameForIqomah = "";

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
    public void registerPlayer(ExoPlayer player) {
        if (player != null && !activePlayers.contains(player)) {
            activePlayers.add(player);
        }
    }

    @Override
    public void registerWebView(android.webkit.WebView webView) {
        if (webView != null && !activeWebViews.contains(webView)) {
            activeWebViews.add(webView);
        }
    }

    @Override
    public void onVideoEnded() {
        Log.i(TAG, "YouTube video ended, advancing to next slide automatically");
        advanceToNextSlide();
    }

    private void stopAllVideoStreams() {
        for (ExoPlayer player : activePlayers) {
            try {
                if (player != null) {
                    player.stop();
                    player.release();
                }
            } catch (Exception ignored) {}
        }
        activePlayers.clear();

        for (android.webkit.WebView wv : activeWebViews) {
            try {
                if (wv != null) {
                    wv.loadUrl("about:blank");
                    wv.destroy();
                }
            } catch (Exception ignored) {}
        }
        activeWebViews.clear();
    }

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

        RtspProxy.start();

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
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
        handler.removeCallbacksAndMessages(null);
        stopAllVideoStreams();
        RtspProxy.stop();
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
        stopAllVideoStreams();
        RtspProxy.stop();
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

        bgImageView = new ImageView(this);
        bgImageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        bgImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootLayout.addView(bgImageView);

        scrimOverlay = new View(this);
        scrimOverlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        scrimOverlay.setBackgroundColor(0x99000000);
        rootLayout.addView(scrimOverlay);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(36), dp(20), dp(36), dp(16));
        content.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        // 1. HEADER
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

        locationBadge.setNextFocusRightId(settingsBtn.getId());
        settingsBtn.setNextFocusLeftId(locationBadge.getId());

        header.addView(rightHeader, new LinearLayout.LayoutParams(-2, -2));
        content.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // 2. COUNTDOWN BANNER
        countdownBanner = new TextView(this);
        countdownBanner.setTextColor(0xFFFFD700);
        countdownBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        countdownBanner.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        countdownBanner.setGravity(Gravity.CENTER);
        countdownBanner.setPadding(0, dp(6), 0, dp(2));
        countdownBanner.setVisibility(View.GONE);
        content.addView(countdownBanner, new LinearLayout.LayoutParams(-1, -2));

        // 3. SLIDER CONTAINER
        sliderContainer = new FrameLayout(this);
        sliderContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));

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

        // 4. FOOTER
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(6), dp(4), 0);

        versionText = new TextView(this);
        versionText.setText("v1.6 (Rilis: 22 Okt 2026)");
        versionText.setTextColor(0x80FFFFFF);
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        versionText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        footer.addView(versionText, new LinearLayout.LayoutParams(0, -2, 1f));

        creditText = new TextView(this);
        creditText.setText("Dibuat oleh : https://github.com/ardith666");
        creditText.setTextColor(0x80FFFFFF);
        creditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        creditText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        creditText.setGravity(Gravity.END);
        footer.addView(creditText, new LinearLayout.LayoutParams(-2, -2));

        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        rootLayout.addView(content);

        // ==========================================
        // 5. IQOMAH OVERLAY (COUNTDOWN BESAR TENGAH LAYAR)
        // ==========================================
        iqomahOverlay = new FrameLayout(this);
        iqomahOverlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        iqomahOverlay.setBackgroundColor(0xF00D1117);
        iqomahOverlay.setVisibility(View.GONE);

        LinearLayout iqomahBox = new LinearLayout(this);
        iqomahBox.setOrientation(LinearLayout.VERTICAL);
        iqomahBox.setGravity(Gravity.CENTER);
        iqomahBox.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        iqomahLabelText = new TextView(this);
        iqomahLabelText.setText("MENUJU IQOMAH");
        iqomahLabelText.setTextColor(0xFFFFD700);
        iqomahLabelText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        iqomahLabelText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        iqomahLabelText.setGravity(Gravity.CENTER);
        iqomahBox.addView(iqomahLabelText);

        iqomahCountdownText = new TextView(this);
        iqomahCountdownText.setText("05:00");
        iqomahCountdownText.setTextColor(0xFFFFFFFF);
        iqomahCountdownText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 120);
        iqomahCountdownText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        iqomahCountdownText.setGravity(Gravity.CENTER);
        iqomahBox.addView(iqomahCountdownText);

        TextView iqomahSub = new TextView(this);
        iqomahSub.setText("Persiapkan diri untuk menunaikan sholat berjamaah\nPerbanyak doa di antara adzan dan iqomah");
        iqomahSub.setTextColor(0xFF7EE787);
        iqomahSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        iqomahSub.setGravity(Gravity.CENTER);
        iqomahSub.setLineSpacing(dp(4), 1.2f);
        iqomahBox.addView(iqomahSub);

        iqomahOverlay.addView(iqomahBox);
        rootLayout.addView(iqomahOverlay);

        // ==========================================
        // 6. GREEN SCREEN OVERLAY (SAAT SHOLAT / RAPATKAN SHAF)
        // ==========================================
        greenScreenOverlay = new FrameLayout(this);
        greenScreenOverlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        greenScreenOverlay.setBackgroundColor(0xFF0F5132); // Deep Mosque Green
        greenScreenOverlay.setVisibility(View.GONE);

        LinearLayout greenBox = new LinearLayout(this);
        greenBox.setOrientation(LinearLayout.VERTICAL);
        greenBox.setGravity(Gravity.CENTER);
        greenBox.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        greenBox.setPadding(dp(48), dp(36), dp(48), dp(36));

        TextView iconShaf = new TextView(this);
        iconShaf.setText("🕌");
        iconShaf.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        iconShaf.setGravity(Gravity.CENTER);
        greenBox.addView(iconShaf);

        greenScreenText = new TextView(this);
        greenScreenText.setText("LURUS & RAPATKAN SHAF");
        greenScreenText.setTextColor(0xFFFFFFFF);
        greenScreenText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 46);
        greenScreenText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        greenScreenText.setGravity(Gravity.CENTER);
        greenScreenText.setPadding(0, dp(14), 0, dp(8));
        greenBox.addView(greenScreenText);

        TextView sholatSub = new TextView(this);
        sholatSub.setText("“Luruskan shaf-shaf kalian, karena sesungguhnya kelurusan shaf adalah bagian dari kesempurnaan sholat.”\n(HR. Bukhari & Muslim)\n\n🔇 Harap Matikan / Senyapkan Nada Dering HP");
        sholatSub.setTextColor(0xFFFFD700);
        sholatSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        sholatSub.setGravity(Gravity.CENTER);
        sholatSub.setLineSpacing(dp(6), 1.25f);
        greenBox.addView(sholatSub);

        greenScreenOverlay.addView(greenBox);
        rootLayout.addView(greenScreenOverlay);

        setContentView(rootLayout);
        rotateQuranVerse();
    }

    private void updateFooterIpText() {
        // Tampilkan versi & tanggal build saja di pojok kiri bawah (keterangan IP cukup di Pengaturan)
        versionText.setText("v1.6 (Rilis: 22 Okt 2026)");
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

        if (isAdzanApproaching) return;

        SlideModel current = activeSlides.get(currentSlideIndex % activeSlides.size());

        if ((SlideModel.TYPE_LIVE_STREAM.equals(current.type) || SlideModel.TYPE_YOUTUBE.equals(current.type)) && current.isLocked) {
            Log.i(TAG, "Live Stream / YouTube slide is locked, pausing automatic slide rotation");
            return;
        }

        int delayMs = Math.max(5, current.durationSec) * 1000;
        handler.postDelayed(slideNextRunnable, delayMs);
    }

    private void advanceToNextSlide() {
        if (activeSlides.size() <= 1 || isAdzanApproaching) return;
        SlideModel current = activeSlides.get(currentSlideIndex % activeSlides.size());
        if ((SlideModel.TYPE_LIVE_STREAM.equals(current.type) || SlideModel.TYPE_YOUTUBE.equals(current.type)) && current.isLocked) return;

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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!activeSlides.isEmpty()) {
                SlideModel cur = activeSlides.get(currentSlideIndex % activeSlides.size());
                if (SlideModel.TYPE_LIVE_STREAM.equals(cur.type) || SlideModel.TYPE_YOUTUBE.equals(cur.type)) {
                    exitLiveStream();
                    return true;
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (settingsBtn != null && !settingsBtn.isFocused()) {
                settingsBtn.requestFocus();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (settingsBtn != null && settingsBtn.isFocused()) {
                if (quranCardContainer != null) {
                    quranCardContainer.requestFocus();
                    return true;
                }
            }
        }

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
            if (SlideModel.TYPE_LIVE_STREAM.equals(cur.type) || SlideModel.TYPE_YOUTUBE.equals(cur.type)) {
                exitLiveStream();
                return;
            }
        }
        super.onBackPressed();
    }

    private void exitLiveStream() {
        Toast.makeText(this, "Siaran Live / YouTube ditutup. Kembali ke jadwal normal.", Toast.LENGTH_SHORT).show();
        stopAllVideoStreams();

        for (SlideModel s : slideManager.getAllSlides()) {
            if (SlideModel.TYPE_LIVE_STREAM.equals(s.type) || SlideModel.TYPE_YOUTUBE.equals(s.type)) {
                s.isLocked = false;
                s.enabled = false;
            }
        }
        slideManager.persist();

        activeSlides = slideManager.getEnabledSlides();

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
            nextIdx = 1;
        }

        if (nextIdx != highlightedSlotIndex) {
            highlightedSlotIndex = nextIdx;
            renderScheduleCards();
        }

        PrayerEngine.Slot nextSlot = slots.get(highlightedSlotIndex);

        // 1. CEK FASE LAYAR HIJAU SHOLAT
        long nowEpoch = System.currentTimeMillis();
        if (greenScreenEndTimestamp > 0) {
            long remainingGreenSec = (greenScreenEndTimestamp - nowEpoch) / 1000;
            if (remainingGreenSec > 0) {
                if (greenScreenOverlay.getVisibility() != View.VISIBLE) {
                    greenScreenOverlay.setVisibility(View.VISIBLE);
                }
                return;
            } else {
                greenScreenEndTimestamp = 0;
                greenScreenOverlay.setVisibility(View.GONE);
                isAdzanApproaching = false;
                scheduleNextSlide();
            }
        }

        // 2. CEK FASE HITUNG MUNDUR IQOMAH
        if (isIqomahPhaseActive && iqomahTargetTimestamp > 0) {
            long diffIqomahSec = (iqomahTargetTimestamp - nowEpoch) / 1000;
            if (diffIqomahSec > 0) {
                if (iqomahOverlay.getVisibility() != View.VISIBLE) {
                    iqomahOverlay.setVisibility(View.VISIBLE);
                }
                int m = (int) (diffIqomahSec / 60);
                int s = (int) (diffIqomahSec % 60);
                iqomahLabelText.setText("MENUJU IQOMAH " + currentPrayerNameForIqomah.toUpperCase());
                iqomahCountdownText.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));

                if (diffIqomahSec <= 10 && diffIqomahSec != lastPlayedPipSecond) {
                    lastPlayedPipSecond = (int) diffIqomahSec;
                    alertManager.playCountdownPip((int) diffIqomahSec);
                }
                return;
            } else {
                isIqomahPhaseActive = false;
                iqomahOverlay.setVisibility(View.GONE);
                alertManager.playCountdownPip(0);

                int greenDuration = alertManager.getGreenScreenDurationSec();
                if (greenDuration > 0) {
                    greenScreenEndTimestamp = nowEpoch + (greenDuration * 1000L);
                    greenScreenOverlay.setVisibility(View.VISIBLE);
                    greenScreenText.setText("WAKTU SHOLAT " + currentPrayerNameForIqomah.toUpperCase() + "\nLURUS & RAPATKAN SHAF");
                } else {
                    isAdzanApproaching = false;
                    scheduleNextSlide();
                }
                return;
            }
        }

        if (nextDiffSec > 0 && nextDiffSec <= 300 && alertManager.isCountdownEnabled()) {
            isAdzanApproaching = true;
            if (customSlideView != null && prayerSlideView.getVisibility() != View.VISIBLE) {
                showSlideAt(0);
            }

            int m = nextDiffSec / 60;
            int s = nextDiffSec % 60;
            countdownBanner.setVisibility(View.VISIBLE);
            countdownBanner.setText(String.format(Locale.getDefault(),
                    "⏳ Menuju Waktu %s: -%02d:%02d", nextSlot.name, m, s));

            if ((nextDiffSec == 30 || nextDiffSec == 20 || (nextDiffSec >= 0 && nextDiffSec <= 10))
                    && nextDiffSec != lastPlayedPipSecond) {
                lastPlayedPipSecond = nextDiffSec;
                alertManager.playCountdownPip(nextDiffSec);
            }
        } else {
            countdownBanner.setVisibility(View.GONE);
            if (isAdzanApproaching && nextDiffSec > 300 && !isIqomahPhaseActive && greenScreenEndTimestamp == 0) {
                isAdzanApproaching = false;
                scheduleNextSlide();
            }
        }

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

        if (alertManager.isIqomahEnabled() && isFardhuPrayer(prayerName)) {
            currentPrayerNameForIqomah = prayerName;
            isAdzanApproaching = true;
            isIqomahPhaseActive = true;
            if (customSlideView != null) showSlideAt(0);

            int iqomahMin = alertManager.getIqomahMinutes(prayerName);
            iqomahTargetTimestamp = System.currentTimeMillis() + (iqomahMin * 60 * 1000L);
        }
    }

    private boolean isFardhuPrayer(String name) {
        if (name == null) return false;
        String n = name.toUpperCase();
        return n.contains("SUBUH") || n.contains("DZUHUR") || n.contains("ASHAR") || n.contains("MAGHRIB") || n.contains("ISYA");
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
        stopAllVideoStreams();
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
