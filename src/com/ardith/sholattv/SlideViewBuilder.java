package com.ardith.sholattv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class SlideViewBuilder {
    private static final String TAG = "SlideViewBuilder";

    public interface StreamHolder {
        void registerPlayer(ExoPlayer player);
        void registerWebView(android.webkit.WebView webView);
        void onVideoEnded();
    }

    public static View buildSlideView(Context ctx, SlideModel model, SlideManager sm, StreamHolder holder) {
        if (SlideModel.TYPE_KAS.equals(model.type)) {
            return buildKasView(ctx, model, false);
        } else if (SlideModel.TYPE_ANNOUNCEMENT.equals(model.type)) {
            if (model.agendaDisplayMode == 1) {
                return buildAgendaGridView(ctx, model);
            } else {
                return buildAgendaHeroView(ctx, model, false);
            }
        } else if (SlideModel.TYPE_LIVE_STREAM.equals(model.type)) {
            return buildLiveStreamView(ctx, model, holder);
        } else if (SlideModel.TYPE_YOUTUBE.equals(model.type)) {
            return buildYouTubeView(ctx, model, holder);
        } else if (SlideModel.TYPE_CCTV.equals(model.type)) {
            return buildCctvGridView(ctx, model, holder, false);
        } else if (SlideModel.TYPE_MULTI_PANEL.equals(model.type)) {
            return buildMultiPanelView(ctx, model, sm, holder);
        } else if (SlideModel.TYPE_IMAGE.equals(model.type)) {
            return buildImageView(ctx, model, sm);
        }
        return new View(ctx);
    }

    // ==========================================
    // 1. KAS MASJID
    // compact = true -> tampilan ringkas untuk slot panel multi-panel (2x2 card kecil)
    // ==========================================
    private static View buildKasView(Context ctx, SlideModel m, boolean compact) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, compact ? 12 : 48), dp(ctx, compact ? 8 : 32),
                dp(ctx, compact ? 12 : 48), dp(ctx, compact ? 10 : 36));

        TextView title = new TextView(ctx);
        title.setText("💰 " + m.title);
        title.setTextColor(0xFFFFD700);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 14 : 26);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        root.addView(title);

        long saldoAkhir = m.saldoAwal + m.pemasukan - m.pengeluaran;

        if (compact) {
            // Susun 4 kartu info dalam grid 2x2 agar hemat ruang di panel sempit
            LinearLayout grid = new LinearLayout(ctx);
            grid.setOrientation(LinearLayout.VERTICAL);
            grid.setPadding(0, dp(ctx, 8), 0, dp(ctx, 4));

            LinearLayout rowTop = new LinearLayout(ctx);
            rowTop.setOrientation(LinearLayout.HORIZONTAL);
            rowTop.addView(createKasBox(ctx, "Saldo Awal", m.saldoAwal, 0x33FFFFFF, 0xE6FFFFFF, true));
            rowTop.addView(createKasBox(ctx, "Infaq (+)", m.pemasukan, 0x4D2EA043, 0xFF7EE787, true));
            grid.addView(rowTop);

            LinearLayout rowBottom = new LinearLayout(ctx);
            rowBottom.setOrientation(LinearLayout.HORIZONTAL);
            rowBottom.addView(createKasBox(ctx, "Pengeluaran (-)", m.pengeluaran, 0x4DF85149, 0xFFFF7B72, true));
            rowBottom.addView(createKasBox(ctx, "Saldo Akhir", saldoAkhir, 0x661F6FEB, 0xFFFFD700, true));
            grid.addView(rowBottom);

            root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, dp(ctx, 32), 0, dp(ctx, 24));

            row.addView(createKasBox(ctx, "Saldo Awal", m.saldoAwal, 0x33FFFFFF, 0xE6FFFFFF, false));
            row.addView(createKasBox(ctx, "Penerimaan Infaq (+)", m.pemasukan, 0x4D2EA043, 0xFF7EE787, false));
            row.addView(createKasBox(ctx, "Pengeluaran (-)", m.pengeluaran, 0x4DF85149, 0xFFFF7B72, false));
            row.addView(createKasBox(ctx, "Saldo Akhir Kas", saldoAkhir, 0x661F6FEB, 0xFFFFD700, false));

            root.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }

        if (m.catatanKas != null && !m.catatanKas.isEmpty()) {
            TextView note = new TextView(ctx);
            note.setText("Keterangan: " + m.catatanKas);
            note.setTextColor(0xB3FFFFFF);
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 10 : 15);
            note.setGravity(Gravity.CENTER);
            note.setMaxLines(2);
            root.addView(note);
        }

        return root;
    }

    private static View createKasBox(Context ctx, String label, long amount, int bgColor, int textColor, boolean compact) {
        LinearLayout b = new LinearLayout(ctx);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(ctx, compact ? 8 : 16), dp(ctx, compact ? 6 : 16),
                dp(ctx, compact ? 8 : 16), dp(ctx, compact ? 7 : 18));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, compact ? 8 : 14));
        bg.setColor(bgColor);
        bg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        b.setBackground(bg);

        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextColor(0xB3FFFFFF);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 9 : 13);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(1);
        b.addView(l);

        TextView v = new TextView(ctx);
        v.setText(compact ? formatRupiahCompact(amount) : "Rp " + String.format(Locale.getDefault(), "%,d", amount).replace(',', '.'));
        v.setTextColor(textColor);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 13 : 20);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        v.setGravity(Gravity.CENTER);
        v.setMaxLines(1);
        v.setPadding(0, dp(ctx, compact ? 3 : 6), 0, 0);
        b.addView(v);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(ctx, compact ? 4 : 8), dp(ctx, compact ? 4 : 0), dp(ctx, compact ? 4 : 8), dp(ctx, compact ? 4 : 0));
        b.setLayoutParams(lp);
        return b;
    }

    /** Format rupiah ringkas untuk panel sempit: 12.500.000 -> "Rp 12,5 Jt". */
    private static String formatRupiahCompact(long amount) {
        long abs = Math.abs(amount);
        if (abs >= 1_000_000_000L) {
            return "Rp " + trimDecimal((double) amount / 1_000_000_000d) + " M";
        }
        if (abs >= 1_000_000L) {
            return "Rp " + trimDecimal((double) amount / 1_000_000d) + " Jt";
        }
        return "Rp " + String.format(Locale.getDefault(), "%,d", amount).replace(',', '.');
    }

    private static String trimDecimal(double val) {
        String s = String.format(Locale.getDefault(), "%.2f", val);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.replace('.', ',');
    }

    // ==========================================
    // 2A. AGENDA TERDEKAT (HERO SLIDE)
    // compact = true -> versi ringkas untuk slot panel multi-panel
    // ==========================================
    private static View buildAgendaHeroView(Context ctx, SlideModel m, boolean compact) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, compact ? 14 : 50), dp(ctx, compact ? 8 : 24),
                dp(ctx, compact ? 14 : 50), dp(ctx, compact ? 10 : 28));

        TextView tag = new TextView(ctx);
        tag.setText("⭐ AGENDA TERDEKAT MASJID");
        tag.setTextColor(0xFFFFD700);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 13 : 22);
        tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tag.setGravity(Gravity.CENTER);
        root.addView(tag);

        SlideModel.AgendaItem first = (m.agendaList != null && !m.agendaList.isEmpty())
                ? m.agendaList.get(0)
                : new SlideModel.AgendaItem("Kajian Akbar Masjid", "Ahad, 06.00 WIB", "DKM Masjid", "Terbuka untuk umum.");

        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(ctx, compact ? 10 : 18));
        cardBg.setColor(0x59000000);
        cardBg.setStroke(dp(ctx, compact ? 1 : 2), 0xFFFFD700);
        card.setBackground(cardBg);
        card.setPadding(dp(ctx, compact ? 14 : 36), dp(ctx, compact ? 10 : 24),
                dp(ctx, compact ? 14 : 36), dp(ctx, compact ? 10 : 24));

        LinearLayout cardCol = new LinearLayout(ctx);
        cardCol.setOrientation(LinearLayout.VERTICAL);
        cardCol.setGravity(Gravity.CENTER);

        TextView title = new TextView(ctx);
        title.setText(first.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 16 : 24);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(compact ? 3 : 10);
        cardCol.addView(title);

        TextView time = new TextView(ctx);
        String sub = first.waktu + (first.narasumber != null && !first.narasumber.isEmpty() ? " | " + first.narasumber : "");
        time.setText("🗓 " + sub);
        time.setTextColor(0xFF7EE787);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 11 : 16);
        time.setGravity(Gravity.CENTER);
        time.setMaxLines(2);
        time.setPadding(0, dp(ctx, compact ? 4 : 8), 0, dp(ctx, compact ? 6 : 14));
        cardCol.addView(time);

        TextView desc = new TextView(ctx);
        desc.setText(first.desc != null ? first.desc : "");
        desc.setTextColor(0xE6E6EDF3);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, compact ? 11 : 17);
        desc.setLineSpacing(dp(ctx, 2), 1.1f);
        desc.setGravity(Gravity.CENTER);
        desc.setMaxLines(compact ? 4 : 20);
        cardCol.addView(desc);

        card.addView(cardCol);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(ctx, compact ? 6 : 16), 0, 0);
        root.addView(card, clp);

        return root;
    }

    /**
     * Widget Agenda untuk slot panel multi-panel.
     * panelAgendaMode 0 -> sorotan agenda terdekat, 1 -> daftar seluruh agenda.
     */
    private static View buildAgendaPanelView(Context ctx, SlideModel m) {
        if (m.panelAgendaMode == 1) return buildAgendaListCompactView(ctx, m);
        return buildAgendaHeroView(ctx, m, true);
    }

    /** Daftar agenda (semua agenda terdaftar) dalam bentuk stack vertikal ringkas. */
    private static View buildAgendaListCompactView(Context ctx, SlideModel m) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8));

        TextView tag = new TextView(ctx);
        tag.setText("🗓 DAFTAR AGENDA MASJID");
        tag.setTextColor(0xFFFFD700);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tag.setGravity(Gravity.CENTER);
        root.addView(tag);

        List<SlideModel.AgendaItem> items = m.agendaList != null ? m.agendaList : new ArrayList<>();
        if (items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("Belum ada agenda terdaftar.");
            empty.setTextColor(0xB3FFFFFF);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            empty.setGravity(Gravity.CENTER);
            root.addView(empty);
            return root;
        }

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(ctx, 6), 0, 0);

        int n = Math.min(items.size(), 6);
        for (int i = 0; i < n; i++) {
            View card = createAgendaGridCard(ctx, items.get(i));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
            lp.setMargins(dp(ctx, 3), dp(ctx, 3), dp(ctx, 3), dp(ctx, 3));
            list.addView(card, lp);
        }

        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        return root;
    }

    // ==========================================
    // 2B. DAFTAR 6 AGENDA BERIKUTNYA (GRID SLIDE)
    // ==========================================
    private static View buildAgendaGridView(Context ctx, SlideModel m) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(ctx, 36), dp(ctx, 18), dp(ctx, 36), dp(ctx, 20));

        TextView tag = new TextView(ctx);
        tag.setText("🗓 JADWAL AGENDA & KAJIAN MENDATANG");
        tag.setTextColor(0xFFFFD700);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tag.setGravity(Gravity.CENTER);
        root.addView(tag);

        List<SlideModel.AgendaItem> remaining = new ArrayList<>();
        if (m.agendaList != null && m.agendaList.size() > 1) {
            for (int i = 1; i < m.agendaList.size() && remaining.size() < 6; i++) {
                remaining.add(m.agendaList.get(i));
            }
        }

        LinearLayout gridLayout = new LinearLayout(ctx);
        gridLayout.setOrientation(LinearLayout.VERTICAL);
        gridLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        gridLayout.setGravity(Gravity.CENTER);
        gridLayout.setPadding(0, dp(ctx, 12), 0, 0);

        int total = remaining.size();
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));

            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                if (idx < total) {
                    SlideModel.AgendaItem item = remaining.get(idx);
                    row.addView(createAgendaGridCard(ctx, item));
                } else {
                    View spacer = new View(ctx);
                    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, -1, 1f);
                    slp.setMargins(dp(ctx, 6), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6));
                    row.addView(spacer, slp);
                }
            }
            gridLayout.addView(row);
        }

        root.addView(gridLayout);
        return root;
    }

    private static View createAgendaGridCard(Context ctx, SlideModel.AgendaItem item) {
        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        bg.setColor(0x40000000);
        bg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        card.setBackground(bg);
        card.setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10));

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_VERTICAL);

        TextView time = new TextView(ctx);
        time.setText("🗓 " + item.waktu);
        time.setTextColor(0xFF7EE787);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        time.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        time.setSingleLine(true);
        col.addView(time);

        TextView title = new TextView(ctx);
        title.setText(item.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setMaxLines(2);
        col.addView(title);

        if (item.narasumber != null && !item.narasumber.isEmpty()) {
            TextView ust = new TextView(ctx);
            ust.setText("👤 " + item.narasumber);
            ust.setTextColor(0xFFFFD700);
            ust.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            ust.setSingleLine(true);
            col.addView(ust);
        }

        card.addView(col);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(ctx, 6), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6));
        card.setLayoutParams(lp);
        return card;
    }

    // ==========================================
    // 3. LIVE STREAMING KHOTBAH / KAJIAN (EXOPLAYER RTSP SURFACEVIEW)
    // ==========================================
    private static View buildLiveStreamView(Context ctx, SlideModel m, StreamHolder holder) {
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setBackgroundColor(0xFF000000);

        android.view.SurfaceView surfaceView = new android.view.SurfaceView(ctx);
        surfaceView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(surfaceView);

        if (m.liveRtspUrl != null && !m.liveRtspUrl.isEmpty()) {
            try {
                ExoPlayer player = new ExoPlayer.Builder(ctx).build();
                player.setVideoSurfaceView(surfaceView);

                String effectiveUrl = RtspProxy.getProxyUrl(m.liveRtspUrl);
                Log.i(TAG, "Playing live stream via: " + effectiveUrl);

                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(Uri.parse(effectiveUrl))
                        .build();
                RtspMediaSource mediaSource = new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(mediaItem);

                player.setMediaSource(mediaSource);
                if (!m.audioEnabled) {
                    player.setVolume(0f);
                } else {
                    player.setVolume(1.0f);
                }
                player.prepare();
                player.play();

                if (holder != null) holder.registerPlayer(player);
            } catch (Exception e) {
                Log.e(TAG, "Error starting ExoPlayer RTSP: " + e.getMessage());
                Toast.makeText(ctx, "Gagal memutar RTSP Live Kajian", Toast.LENGTH_SHORT).show();
            }
        }

        // Header Live Overlay Badge: [ ● LIVE ] dengan titik merah kedip
        LinearLayout badge = new LinearLayout(ctx);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(ctx, 14), dp(ctx, 6), dp(ctx, 16), dp(ctx, 6));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(ctx, 14));
        badgeBg.setColor(0xCC000000);
        badgeBg.setStroke(dp(ctx, 1), 0x4DF85149);
        badge.setBackground(badgeBg);

        TextView redDot = new TextView(ctx);
        redDot.setText("● ");
        redDot.setTextColor(0xFFE50914);
        redDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        android.view.animation.AlphaAnimation blink = new android.view.animation.AlphaAnimation(1.0f, 0.15f);
        blink.setDuration(600);
        blink.setRepeatMode(android.view.animation.Animation.REVERSE);
        blink.setRepeatCount(android.view.animation.Animation.INFINITE);
        redDot.startAnimation(blink);
        badge.addView(redDot);

        TextView liveTxt = new TextView(ctx);
        liveTxt.setText("LIVE");
        liveTxt.setTextColor(0xFFFFFFFF);
        liveTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        liveTxt.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.addView(liveTxt);

        FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        bLp.setMargins(dp(ctx, 24), dp(ctx, 20), 0, 0);
        root.addView(badge, bLp);

        // Footer Hint: Tekan BACK untuk keluar
        TextView hint = new TextView(ctx);
        hint.setText(m.isLocked ? "🔒 Layar Terkunci | Tekan tombol BACK pada remote untuk keluar dari siaran" : "");
        hint.setTextColor(0xB3FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));

        GradientDrawable hintBg = new GradientDrawable();
        hintBg.setCornerRadius(dp(ctx, 10));
        hintBg.setColor(0x80000000);
        hint.setBackground(hintBg);

        FrameLayout.LayoutParams hLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        hLp.setMargins(0, 0, dp(ctx, 24), dp(ctx, 18));
        root.addView(hint, hLp);

        return root;
    }

    // ==========================================
    // 3B. YOUTUBE VIDEO / LIVE STREAM (WEBVIEW IFRAME)
    // ==========================================
    private static View buildYouTubeView(Context ctx, SlideModel m, StreamHolder holder) {
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setBackgroundColor(0xFF000000);

        android.webkit.WebView webView = new android.webkit.WebView(ctx);
        webView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        android.webkit.WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        String videoId = extractYouTubeVideoId(m.youtubeUrl);
        String appOrigin = "https://" + ctx.getPackageName();

        String embedHtml = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>body,html{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;}"
                + "iframe{width:100%;height:100%;border:none;}</style></head><body>"
                + "<div id='player'></div>"
                + "<script>"
                + "var tag = document.createElement('script'); tag.src = 'https://www.youtube.com/iframe_api';"
                + "var firstScriptTag = document.getElementsByTagName('script')[0]; firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);"
                + "var player;"
                + "function onYouTubeIframeAPIReady() {"
                + "  player = new YT.Player('player', {"
                + "    videoId: '" + videoId + "',"
                + "    playerVars: { 'autoplay': 1, 'controls': 0, 'rel': 0, 'playsinline': 1, 'enablejsapi': 1, 'fs': 0, 'origin': '" + appOrigin + "' },"
                + "    events: { 'onReady': onPlayerReady, 'onStateChange': onPlayerStateChange, 'onError': onPlayerError }"
                + "  });"
                + "}"
                + "function onPlayerReady(event) { event.target.playVideo(); }"
                + "function onPlayerStateChange(event) {"
                + "  if (event.data === YT.PlayerState.ENDED) { Android.onEnded(); }"
                + "}"
                + "function onPlayerError(event) {"
                + "  Android.onError(event.data);"
                + "}"
                + "</script></body></html>";

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onEnded() {
                if (holder != null) {
                    ((android.app.Activity) ctx).runOnUiThread(holder::onVideoEnded);
                }
            }
            @android.webkit.JavascriptInterface
            public void onError(int errorCode) {
                ((android.app.Activity) ctx).runOnUiThread(() -> {
                    String msg = "Error " + errorCode;
                    if (errorCode == 101 || errorCode == 150) {
                        msg = "Video tidak mengizinkan pemutaran via aplikasi luar (Pembatasan Hak Cipta/Embedding oleh pemilik video).";
                    } else if (errorCode == 100) {
                        msg = "Video YouTube tidak ditemukan atau sudah dihapus/privat.";
                    }
                    Toast.makeText(ctx, "YouTube: " + msg, Toast.LENGTH_LONG).show();
                    if (holder != null) {
                        holder.onVideoEnded(); // auto-skip jika diblokir hak cipta
                    }
                });
            }
        }, "Android");

        webView.loadDataWithBaseURL(appOrigin, embedHtml, "text/html", "UTF-8", null);

        if (holder != null) {
            holder.registerWebView(webView);
        }
        root.addView(webView);

        // Header YouTube Live Badge
        LinearLayout badge = new LinearLayout(ctx);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(ctx, 14), dp(ctx, 6), dp(ctx, 16), dp(ctx, 6));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(ctx, 14));
        badgeBg.setColor(0xCC000000);
        badgeBg.setStroke(dp(ctx, 1), 0x4DF85149);
        badge.setBackground(badgeBg);

        TextView redDot = new TextView(ctx);
        redDot.setText("▶ ");
        redDot.setTextColor(0xFFE50914);
        redDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        badge.addView(redDot);

        TextView ytTxt = new TextView(ctx);
        ytTxt.setText("YOUTUBE");
        ytTxt.setTextColor(0xFFFFFFFF);
        ytTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ytTxt.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.addView(ytTxt);

        FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        bLp.setMargins(dp(ctx, 24), dp(ctx, 20), 0, 0);
        root.addView(badge, bLp);

        // Footer Hint
        TextView hint = new TextView(ctx);
        hint.setText(m.isLocked ? "🔒 Layar Terkunci | Tekan tombol BACK pada remote untuk keluar" : "");
        hint.setTextColor(0xB3FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));

        GradientDrawable hintBg = new GradientDrawable();
        hintBg.setCornerRadius(dp(ctx, 10));
        hintBg.setColor(0x80000000);
        hint.setBackground(hintBg);

        FrameLayout.LayoutParams hLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        hLp.setMargins(0, 0, dp(ctx, 24), dp(ctx, 18));
        root.addView(hint, hLp);

        return root;
    }

    private static String extractYouTubeVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        String vId = "";
        try {
            if (url.contains("v=")) {
                String[] p = url.split("v=")[1].split("&");
                vId = p[0];
            } else if (url.contains("youtu.be/")) {
                String[] p = url.split("youtu.be/")[1].split("\\?");
                vId = p[0];
            } else if (url.contains("live/")) {
                String[] p = url.split("live/")[1].split("\\?");
                vId = p[0];
            } else if (url.contains("embed/")) {
                String[] p = url.split("embed/")[1].split("\\?");
                vId = p[0];
            } else {
                vId = url;
            }
        } catch (Exception ignored) {}
        return vId;
    }

    // ==========================================
    // 4. MULTI-STREAM CCTV (EXOPLAYER RTSP GRID)
    // ==========================================
    private static View buildCctvGridView(Context ctx, SlideModel m, StreamHolder holder, boolean compact) {
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setBackgroundColor(0xFF000000);

        List<SlideModel.CctvStream> list = m.cctvStreams != null ? m.cctvStreams : new ArrayList<>();
        int count = list.size();

        if (count == 0) {
            TextView empty = new TextView(ctx);
            empty.setText("Belum ada kamera CCTV terdaftar di slide ini.");
            empty.setTextColor(0xFFFFFFFF);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            empty.setGravity(Gravity.CENTER);
            root.addView(empty);
            return root;
        }

        LinearLayout container = new LinearLayout(ctx);
        container.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        container.setOrientation(LinearLayout.VERTICAL);

        if (count == 1) {
            container.addView(createCctvBox(ctx, list.get(0), holder), new LinearLayout.LayoutParams(-1, -1));
        } else if (count == 2) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            row.addView(createCctvBox(ctx, list.get(0), holder), new LinearLayout.LayoutParams(0, -1, 1f));
            row.addView(createCctvBox(ctx, list.get(1), holder), new LinearLayout.LayoutParams(0, -1, 1f));
            container.addView(row);
        } else {
            int perRow = (count <= 4) ? 2 : 3;
            for (int r = 0; r < 2; r++) {
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
                for (int c = 0; c < perRow; c++) {
                    int idx = r * perRow + c;
                    if (idx < count) {
                        row.addView(createCctvBox(ctx, list.get(idx), holder), new LinearLayout.LayoutParams(0, -1, 1f));
                    } else {
                        View sp = new View(ctx);
                        sp.setBackgroundColor(0xFF111111);
                        row.addView(sp, new LinearLayout.LayoutParams(0, -1, 1f));
                    }
                }
                container.addView(row);
            }
        }

        root.addView(container);

        // Header Title Badge ( disembunyikan pada slot panel agar tidak menutupi grid)
        if (compact) return root;

        TextView titleBadge = new TextView(ctx);
        titleBadge.setText("📹 CCTV PENGAWASAN MASJID (" + count + " KAMERA)");
        titleBadge.setTextColor(0xFFFFFFFF);
        titleBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleBadge.setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 12), dp(ctx, 4));

        GradientDrawable tbg = new GradientDrawable();
        tbg.setCornerRadius(dp(ctx, 8));
        tbg.setColor(0x99000000);
        titleBadge.setBackground(tbg);

        FrameLayout.LayoutParams tbLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        tbLp.setMargins(dp(ctx, 16), dp(ctx, 12), 0, 0);
        root.addView(titleBadge, tbLp);

        return root;
    }

    private static View createCctvBox(Context ctx, SlideModel.CctvStream cam, StreamHolder holder) {
        FrameLayout box = new FrameLayout(ctx);
        box.setBackgroundColor(0xFF161B22);

        android.view.SurfaceView surfaceView = new android.view.SurfaceView(ctx);
        surfaceView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        box.addView(surfaceView);

        if (cam.rtspUrl != null && !cam.rtspUrl.isEmpty()) {
            try {
                ExoPlayer player = new ExoPlayer.Builder(ctx).build();
                player.setVideoSurfaceView(surfaceView);

                String effectiveUrl = RtspProxy.getProxyUrl(cam.rtspUrl);
                MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(effectiveUrl)).build();
                RtspMediaSource mediaSource = new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(mediaItem);

                player.setMediaSource(mediaSource);
                player.setVolume(0f); // Always mute CCTV
                player.prepare();
                player.play();

                if (holder != null) holder.registerPlayer(player);
            } catch (Exception e) {
                Log.e(TAG, "Error playing CCTV stream: " + cam.rtspUrl, e);
            }
        }

        // Label Nama Kamera
        TextView lbl = new TextView(ctx);
        lbl.setText("● " + cam.name);
        lbl.setTextColor(0xFFFFFFFF);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lbl.setPadding(dp(ctx, 8), dp(ctx, 3), dp(ctx, 8), dp(ctx, 3));

        GradientDrawable lbg = new GradientDrawable();
        lbg.setCornerRadius(dp(ctx, 6));
        lbg.setColor(0x80000000);
        lbl.setBackground(lbg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        lp.setMargins(dp(ctx, 8), 0, 0, dp(ctx, 8));
        box.addView(lbl, lp);

        return box;
    }

    // ==========================================
    // 5. GAMBAR POSTER FLYER
    // ==========================================
    private static View buildImageView(Context ctx, SlideModel m, SlideManager sm) {
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        ImageView iv = new ImageView(ctx);
        iv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (m.imageFileName != null && !m.imageFileName.isEmpty()) {
            File file = new File(sm.getImageFolder(), m.imageFileName);
            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) iv.setImageBitmap(bmp);
                } catch (Exception ignored) {}
            }
        }

        root.addView(iv);
        return root;
    }

    // ==========================================
    // 6. MULTI-PANEL KIOSK DASHBOARD (FLEKSIBEL)
    // ==========================================
    private static View buildMultiPanelView(Context ctx, SlideModel m, SlideManager sm, StreamHolder holder) {
        LinearLayout root = new LinearLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);

        // Container Split Tengah (Panels)
        LinearLayout centerRow = new LinearLayout(ctx);
        centerRow.setOrientation(LinearLayout.HORIZONTAL);
        centerRow.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));

        String preset = m.multiPanelPreset != null ? m.multiPanelPreset : "SPLIT_60_40";

        if ("THREE_COLUMNS".equals(preset)) {
            // Layout 3 Kolom Sejajar: 38% - 31% - 31%
            centerRow.addView(createPanelSlot(ctx, m.slotLeftType, m.slotLeftUrl, m, sm, holder), new LinearLayout.LayoutParams(0, -1, 0.38f));
            centerRow.addView(createPanelSlot(ctx, m.slotRightType, m.slotRightUrl, m, sm, holder), new LinearLayout.LayoutParams(0, -1, 0.31f));
            centerRow.addView(createPanelSlot(ctx, m.slotThirdType, "", m, sm, holder), new LinearLayout.LayoutParams(0, -1, 0.31f));
        } else if ("TOP_BOTTOM".equals(preset)) {
            // Layout Atas-Bawah: Atas 65% Video, Bawah 35% Jadwal
            centerRow.setOrientation(LinearLayout.VERTICAL);
            centerRow.addView(createPanelSlot(ctx, m.slotLeftType, m.slotLeftUrl, m, sm, holder), new LinearLayout.LayoutParams(-1, 0, 0.65f));
            centerRow.addView(createPanelSlot(ctx, m.slotRightType, m.slotRightUrl, m, sm, holder), new LinearLayout.LayoutParams(-1, 0, 0.35f));
        } else {
            // Default SPLIT_60_40: Kiri 60% Video/Visual, Kanan 40% Jadwal/Info
            centerRow.addView(createPanelSlot(ctx, m.slotLeftType, m.slotLeftUrl, m, sm, holder), new LinearLayout.LayoutParams(0, -1, 0.60f));
            centerRow.addView(createPanelSlot(ctx, m.slotRightType, m.slotRightUrl, m, sm, holder), new LinearLayout.LayoutParams(0, -1, 0.40f));
        }

        root.addView(centerRow);

        // Running Text Bar di Bagian Bawah
        if (m.runningText != null && !m.runningText.trim().isEmpty()) {
            LinearLayout marqueeBar = new LinearLayout(ctx);
            marqueeBar.setOrientation(LinearLayout.HORIZONTAL);
            marqueeBar.setGravity(Gravity.CENTER_VERTICAL);
            marqueeBar.setPadding(dp(ctx, 16), dp(ctx, 6), dp(ctx, 16), dp(ctx, 6));

            int bgCol = 0xFF161B22;
            if ("GREEN".equalsIgnoreCase(m.runningTextBg)) bgCol = 0xFF144620;
            else if ("GOLD".equalsIgnoreCase(m.runningTextBg)) bgCol = 0xFF4D3800;
            marqueeBar.setBackgroundColor(bgCol);

            TextView tag = new TextView(ctx);
            tag.setText("📢 INFO: ");
            tag.setTextColor(0xFFFFD700);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            marqueeBar.addView(tag);

            TextView marquee = new TextView(ctx);
            marquee.setText(m.runningText);
            marquee.setTextColor(0xFFFFFFFF);
            marquee.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            marquee.setSingleLine(true);
            marquee.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
            marquee.setMarqueeRepeatLimit(-1);
            marquee.setSelected(true);
            marqueeBar.addView(marquee, new LinearLayout.LayoutParams(0, -2, 1f));

            root.addView(marqueeBar, new LinearLayout.LayoutParams(-1, dp(ctx, 36)));
        }

        return root;
    }

    private static View createPanelSlot(Context ctx, String type, String url, SlideModel m, SlideManager sm, StreamHolder holder) {
        FrameLayout slot = new FrameLayout(ctx);
        slot.setBackgroundColor(0xFF0D1117);

        if ("LIVE_STREAM".equalsIgnoreCase(type)) {
            SlideModel dummy = new SlideModel("slot_live", SlideModel.TYPE_LIVE_STREAM, "Live", false, 60);
            dummy.liveRtspUrl = url != null && !url.isEmpty() ? url : m.liveRtspUrl;
            dummy.audioEnabled = false;
            slot.addView(buildLiveStreamView(ctx, dummy, holder));
        } else if ("YOUTUBE".equalsIgnoreCase(type)) {
            SlideModel dummy = new SlideModel("slot_yt", SlideModel.TYPE_YOUTUBE, "YouTube", false, 60);
            dummy.youtubeUrl = url != null && !url.isEmpty() ? url : m.youtubeUrl;
            slot.addView(buildYouTubeView(ctx, dummy, holder));
        } else if ("CCTV".equalsIgnoreCase(type)) {
            slot.addView(buildCctvGridView(ctx, m, holder, true));
        } else if ("KAS".equalsIgnoreCase(type)) {
            slot.addView(buildKasView(ctx, m, true));
        } else if ("AGENDA".equalsIgnoreCase(type)) {
            slot.addView(buildAgendaPanelView(ctx, m));
        } else {
            // Default "PRAYER" (Jadwal Sholat Mini)
            slot.addView(buildMiniPrayerView(ctx));
        }

        return slot;
    }

    private static View buildMiniPrayerView(Context ctx) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));

        PrayerEngine pe = new PrayerEngine(ctx);
        PrayerEngine.DaySchedule ds = pe.getTodaySchedule();

        TextView title = new TextView(ctx);
        title.setText("🕌 " + ds.cityName);
        title.setTextColor(0xFFFFD700);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        col.addView(title);

        // ---- Kartu highlight waktu sholat berikutnya ----
        LinearLayout nextCard = new LinearLayout(ctx);
        nextCard.setOrientation(LinearLayout.HORIZONTAL);
        nextCard.setGravity(Gravity.CENTER_VERTICAL);
        nextCard.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));

        GradientDrawable nextBg = new GradientDrawable();
        nextBg.setCornerRadius(dp(ctx, 10));
        nextBg.setColor(0x4D2EA043);
        nextBg.setStroke(dp(ctx, 1), 0xFFFFD700);
        nextCard.setBackground(nextBg);

        LinearLayout nextInfo = new LinearLayout(ctx);
        nextInfo.setOrientation(LinearLayout.VERTICAL);
        nextInfo.setGravity(Gravity.CENTER_VERTICAL);

        TextView nextLabel = new TextView(ctx);
        nextLabel.setText("WAKTU SHOLAT BERIKUTNYA");
        nextLabel.setTextColor(0xB3FFFFFF);
        nextLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        nextLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        nextInfo.addView(nextLabel);

        TextView nextName = new TextView(ctx);
        nextName.setText("--");
        nextName.setTextColor(0xFFFFD700);
        nextName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nextName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        nextInfo.addView(nextName);

        nextCard.addView(nextInfo, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView nextCountdown = new TextView(ctx);
        nextCountdown.setText("--:--:--");
        nextCountdown.setTextColor(0xFF7EE787);
        nextCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        nextCountdown.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        nextCountdown.setGravity(Gravity.CENTER);
        nextCard.addView(nextCountdown);

        LinearLayout.LayoutParams ncp = new LinearLayout.LayoutParams(-1, -2);
        ncp.setMargins(0, dp(ctx, 8), 0, dp(ctx, 4));
        col.addView(nextCard, ncp);

        // ---- Daftar waktu sholat (baris berikutnya disorot) ----
        final List<PrayerEngine.Slot> slots = ds.slots;
        final List<LinearLayout> rows = new ArrayList<>();
        final List<TextView> nameViews = new ArrayList<>();
        final List<TextView> timeViews = new ArrayList<>();

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        for (PrayerEngine.Slot s : slots) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 10), dp(ctx, 3), dp(ctx, 10), dp(ctx, 3));

            TextView n = new TextView(ctx);
            n.setText(s.name);
            n.setTextColor(0xB3FFFFFF);
            n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            n.setSingleLine(true);
            row.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView t = new TextView(ctx);
            t.setText(s.time);
            t.setTextColor(0xFFFFFFFF);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            row.addView(t);

            list.addView(row);
            rows.add(row);
            nameViews.add(n);
            timeViews.add(t);
        }

        col.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ---- Penanda waktu sholat berikutnya (dalam daftar) ----
        final int[] highlighted = {-1};

        Runnable updater = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                int nowSec = now.get(Calendar.SECOND);

                int idx = -1;
                for (int i = 0; i < slots.size(); i++) {
                    PrayerEngine.Slot s = slots.get(i);
                    if (s.minutes >= nowMin && isFardhuPrayer(s.name)) {
                        idx = i;
                        break;
                    }
                }
                boolean tomorrow = false;
                if (idx == -1) {
                    // Semua waktu sholat fardhu hari ini sudah lewat -> Subuh besok
                    for (int i = 0; i < slots.size(); i++) {
                        PrayerEngine.Slot s = slots.get(i);
                        if (isFardhuPrayer(s.name)) {
                            idx = i;
                            tomorrow = true;
                            break;
                        }
                    }
                }
                if (idx == -1) return;

                int targetSec = slots.get(idx).minutes * 60;
                int nowTotalSec = nowMin * 60 + nowSec;
                int diffSec = targetSec - nowTotalSec;
                if (tomorrow || diffSec < 0) diffSec += 24 * 3600;

                if (idx != highlighted[0]) {
                    highlighted[0] = idx;
                    for (int i = 0; i < rows.size(); i++) {
                        boolean active = (i == idx);
                        GradientDrawable bg = new GradientDrawable();
                        bg.setCornerRadius(dp(ctx, 8));
                        bg.setColor(active ? 0x662EA043 : 0x00000000);
                        bg.setStroke(dp(ctx, 1), active ? 0xFFFFD700 : 0x00FFFFFF);
                        rows.get(i).setBackground(bg);
                        nameViews.get(i).setTextColor(active ? 0xFFFFD700 : 0xB3FFFFFF);
                        nameViews.get(i).setText(active ? "▶ " + slots.get(i).name : slots.get(i).name);
                        timeViews.get(i).setTextColor(active ? 0xFFFFD700 : 0xFFFFFFFF);
                    }
                    nextName.setText(slots.get(idx).name.toUpperCase() + "  " + slots.get(idx).time);
                }

                nextCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        diffSec / 3600, (diffSec % 3600) / 60, diffSec % 60));
            }
        };

        updater.run();
        tickEverySecond(col, updater);

        return col;
    }

    /** Ticker 1 detik yang berhenti sendiri saat view dilepas dari layar. */
    private static void tickEverySecond(View host, Runnable action) {
        host.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!host.isAttachedToWindow()) return;
                action.run();
                host.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private static boolean isFardhuPrayer(String name) {
        if (name == null) return false;
        String n = name.toUpperCase();
        return n.contains("SUBUH") || n.contains("DZUHUR") || n.contains("ASHAR")
                || n.contains("MAGHRIB") || n.contains("ISYA");
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
