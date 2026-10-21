package com.ardith.sholattv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public final class SlideViewBuilder {

    public static View buildSlideView(Context ctx, SlideModel model, SlideManager sm) {
        if (SlideModel.TYPE_KAS.equals(model.type)) {
            return buildKasView(ctx, model);
        } else if (SlideModel.TYPE_ANNOUNCEMENT.equals(model.type)) {
            return buildAnnouncementView(ctx, model);
        } else if (SlideModel.TYPE_IMAGE.equals(model.type)) {
            return buildImageView(ctx, model, sm);
        }
        return new View(ctx);
    }

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

    private static View buildAnnouncementView(Context ctx, SlideModel m) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, 60), dp(ctx, 32), dp(ctx, 60), dp(ctx, 36));

        TextView title = new TextView(ctx);
        title.setText("📢 " + m.title);
        title.setTextColor(0xFFFFD700);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        if (m.tanggalAgenda != null && !m.tanggalAgenda.isEmpty()) {
            TextView time = new TextView(ctx);
            time.setText("🗓 " + m.tanggalAgenda + (m.narasumber != null && !m.narasumber.isEmpty() ? " | " + m.narasumber : ""));
            time.setTextColor(0xFF7EE787);
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            time.setGravity(Gravity.CENTER);
            time.setPadding(0, dp(ctx, 8), 0, dp(ctx, 18));
            root.addView(time);
        }

        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(ctx, 16));
        cardBg.setColor(0x40000000);
        cardBg.setStroke(dp(ctx, 1), 0x33FFFFFF);
        card.setBackground(cardBg);
        card.setPadding(dp(ctx, 32), dp(ctx, 24), dp(ctx, 32), dp(ctx, 24));

        TextView desc = new TextView(ctx);
        desc.setText(m.pengumumanTeks != null ? m.pengumumanTeks : "");
        desc.setTextColor(0xFFFFFFFF);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        desc.setLineSpacing(dp(ctx, 6), 1.15f);
        desc.setGravity(Gravity.CENTER);
        card.addView(desc);

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

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
