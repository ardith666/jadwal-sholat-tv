package com.ardith.sholattv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SlideViewBuilder {

    public interface StreamHolder {
        void registerVideoView(VideoView vv);
    }

    public static View buildSlideView(Context ctx, SlideModel model, SlideManager sm, StreamHolder holder) {
        if (SlideModel.TYPE_KAS.equals(model.type)) {
            return buildKasView(ctx, model);
        } else if (SlideModel.TYPE_ANNOUNCEMENT.equals(model.type)) {
            if (model.agendaDisplayMode == 1) {
                return buildAgendaGridView(ctx, model);
            } else {
                return buildAgendaHeroView(ctx, model);
            }
        } else if (SlideModel.TYPE_LIVE_STREAM.equals(model.type)) {
            return buildLiveStreamView(ctx, model, holder);
        } else if (SlideModel.TYPE_CCTV.equals(model.type)) {
            return buildCctvGridView(ctx, model, holder);
        } else if (SlideModel.TYPE_IMAGE.equals(model.type)) {
            return buildImageView(ctx, model, sm);
        }
        return new View(ctx);
    }

    // ==========================================
    // 1. KAS MASJID
    // ==========================================
    private static View buildKasView(Context ctx, SlideModel m) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, 48), dp(ctx, 32), dp(ctx, 48), dp(ctx, 36));

        TextView title = new TextView(ctx);
        title.setText("💰 " + m.title);
        title.setTextColor(0xFFFFD700);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(ctx, 32), 0, dp(ctx, 24));

        long saldoAkhir = m.saldoAwal + m.pemasukan - m.pengeluaran;

        row.addView(createKasBox(ctx, "Saldo Awal", m.saldoAwal, 0x33FFFFFF, 0xE6FFFFFF));
        row.addView(createKasBox(ctx, "Penerimaan Infaq (+)", m.pemasukan, 0x4D2EA043, 0xFF7EE787));
        row.addView(createKasBox(ctx, "Pengeluaran (-)", m.pengeluaran, 0x4DF85149, 0xFFFF7B72));
        row.addView(createKasBox(ctx, "Saldo Akhir Kas", saldoAkhir, 0x661F6FEB, 0xFFFFD700));

        root.addView(row, new LinearLayout.LayoutParams(-1, -2));

        if (m.catatanKas != null && !m.catatanKas.isEmpty()) {
            TextView note = new TextView(ctx);
            note.setText("Keterangan: " + m.catatanKas);
            note.setTextColor(0xB3FFFFFF);
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            note.setGravity(Gravity.CENTER);
            root.addView(note);
        }

        return root;
    }

    private static View createKasBox(Context ctx, String label, long amount, int bgColor, int textColor) {
        LinearLayout b = new LinearLayout(ctx);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 18));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 14));
        bg.setColor(bgColor);
        bg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        b.setBackground(bg);

        TextView l = new TextView(ctx);
        l.setText(label);
        l.setTextColor(0xB3FFFFFF);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        l.setGravity(Gravity.CENTER);
        b.addView(l);

        TextView v = new TextView(ctx);
        v.setText("Rp " + String.format(Locale.getDefault(), "%,d", amount).replace(',', '.'));
        v.setTextColor(textColor);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(ctx, 6), 0, 0);
        b.addView(v);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(ctx, 8), 0, dp(ctx, 8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ==========================================
    // 2A. AGENDA TERDEKAT (HERO SLIDE)
    // ==========================================
    private static View buildAgendaHeroView(Context ctx, SlideModel m) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, 50), dp(ctx, 24), dp(ctx, 50), dp(ctx, 28));

        TextView tag = new TextView(ctx);
        tag.setText("⭐ AGENDA TERDEKAT MASJID");
        tag.setTextColor(0xFFFFD700);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tag.setGravity(Gravity.CENTER);
        root.addView(tag);

        SlideModel.AgendaItem first = (m.agendaList != null && !m.agendaList.isEmpty())
                ? m.agendaList.get(0)
                : new SlideModel.AgendaItem("Kajian Akbar Masjid", "Ahad, 06.00 WIB", "DKM Masjid", "Terbuka untuk umum.");

        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(ctx, 18));
        cardBg.setColor(0x59000000);
        cardBg.setStroke(dp(ctx, 2), 0xFFFFD700);
        card.setBackground(cardBg);
        card.setPadding(dp(ctx, 36), dp(ctx, 24), dp(ctx, 36), dp(ctx, 24));

        LinearLayout cardCol = new LinearLayout(ctx);
        cardCol.setOrientation(LinearLayout.VERTICAL);
        cardCol.setGravity(Gravity.CENTER);

        TextView title = new TextView(ctx);
        title.setText(first.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        cardCol.addView(title);

        TextView time = new TextView(ctx);
        String sub = first.waktu + (first.narasumber != null && !first.narasumber.isEmpty() ? " | " + first.narasumber : "");
        time.setText("🗓 " + sub);
        time.setTextColor(0xFF7EE787);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        time.setGravity(Gravity.CENTER);
        time.setPadding(0, dp(ctx, 8), 0, dp(ctx, 14));
        cardCol.addView(time);

        TextView desc = new TextView(ctx);
        desc.setText(first.desc != null ? first.desc : "");
        desc.setTextColor(0xE6E6EDF3);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        desc.setLineSpacing(dp(ctx, 4), 1.15f);
        desc.setGravity(Gravity.CENTER);
        cardCol.addView(desc);

        card.addView(cardCol);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(ctx, 16), 0, 0);
        root.addView(card, clp);

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

        // Layout 2 baris x 3 kolom
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
    // 3. LIVE STREAMING KHOTBAH / KAJIAN (RTSP)
    // ==========================================
    private static View buildLiveStreamView(Context ctx, SlideModel m, StreamHolder holder) {
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setBackgroundColor(0xFF000000);

        VideoView vv = new VideoView(ctx);
        FrameLayout.LayoutParams vvLp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        vv.setLayoutParams(vvLp);

        if (m.liveRtspUrl != null && !m.liveRtspUrl.isEmpty()) {
            vv.setVideoURI(Uri.parse(m.liveRtspUrl));
            vv.setOnPreparedListener(mp -> {
                if (!m.audioEnabled) {
                    mp.setVolume(0f, 0f); // Mute jika audio dimatikan
                }
                vv.start();
            });
            vv.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(ctx, "Gagal memutar RTSP Live Kajian", Toast.LENGTH_SHORT).show();
                return true;
            });
            if (holder != null) holder.registerVideoView(vv);
        }
        root.addView(vv);

        // Header Live Overlay Badge
        LinearLayout badge = new LinearLayout(ctx);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 16), dp(ctx, 8));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(ctx, 16));
        badgeBg.setColor(0xCCB00020);
        badge.setBackground(badgeBg);

        TextView dot = new TextView(ctx);
        dot.setText("● ");
        dot.setTextColor(0xFFFFFFFF);
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        badge.addView(dot);

        TextView liveTxt = new TextView(ctx);
        liveTxt.setText("LIVE SIARAN KHOTBAH / KAJIAN");
        liveTxt.setTextColor(0xFFFFFFFF);
        liveTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
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
    // 4. MULTI-STREAM CCTV (GRID DINAMIS 1 - 6)
    // ==========================================
    private static View buildCctvGridView(Context ctx, SlideModel m, StreamHolder holder) {
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

        // Tentukan grid layout berdasarkan jumlah kamera
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
            // 3 s/d 6 kamera: 2 baris
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

        // Header Title Badge
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

        VideoView vv = new VideoView(ctx);
        vv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        if (cam.rtspUrl != null && !cam.rtspUrl.isEmpty()) {
            vv.setVideoURI(Uri.parse(cam.rtspUrl));
            vv.setOnPreparedListener(mp -> {
                mp.setVolume(0f, 0f); // Mute CCTV
                vv.start();
            });
            vv.setOnErrorListener((mp, what, extra) -> true);
            if (holder != null) holder.registerVideoView(vv);
        }
        box.addView(vv);

        // Label Nama Kamera di pojok kiri bawah box
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

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
