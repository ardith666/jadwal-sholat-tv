package com.ardith.sholattv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

public final class AlertManager {
    private static final String TAG = "AlertManager";
    public static final String PREFS_NAME = "sholattv_prefs";

    public static final String KEY_SOUND_MODE = "sound_mode";
    public static final String MODE_ADZAN = "adzan";
    public static final String MODE_BEEP = "beep";
    public static final String MODE_SILENT = "silent";

    public static final String KEY_COUNTDOWN_ENABLED = "countdown_enabled";
    public static final String KEY_COUNTDOWN_AUDIO = "countdown_audio";
    public static final String KEY_KEEP_AWAKE = "keep_awake";
    public static final String KEY_SHOW_ARAB = "show_arab";
    public static final String KEY_SHOW_LATIN = "show_latin";
    public static final String KEY_SHOW_TRANSLATION = "show_translation";
    public static final String KEY_THEME = "theme_bg";
    public static final String DEFAULT_THEME = "photo_dark_01.jpg";
    public static final String KEY_CUSTOM_BG_URI = "custom_bg_uri";
    public static final String KEY_AUTO_UPDATE_CHECK = "auto_update_check";

    private final Context ctx;
    private MediaPlayer mediaPlayer;
    private MediaPlayer pipPlayer;

    public AlertManager(Context context) {
        this.ctx = context.getApplicationContext();
    }

    public String getSoundMode() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SOUND_MODE, MODE_ADZAN);
    }

    public void setSoundMode(String mode) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SOUND_MODE, mode).apply();
    }

    public boolean isCountdownEnabled() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_COUNTDOWN_ENABLED, true);
    }

    public void setCountdownEnabled(boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COUNTDOWN_ENABLED, enabled).apply();
    }

    public boolean isCountdownAudioEnabled() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_COUNTDOWN_AUDIO, true);
    }

    public void setCountdownAudioEnabled(boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COUNTDOWN_AUDIO, enabled).apply();
    }

    public boolean isKeepAwake() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_KEEP_AWAKE, true);
    }

    public void setKeepAwake(boolean awake) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_KEEP_AWAKE, awake).apply();
    }

    public boolean isShowArab() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_ARAB, true);
    }

    public void setShowArab(boolean show) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW_ARAB, show).apply();
    }

    public boolean isShowLatin() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_LATIN, true);
    }

    public void setShowLatin(boolean show) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW_LATIN, show).apply();
    }

    public boolean isShowTranslation() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_TRANSLATION, true);
    }

    public void setShowTranslation(boolean show) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW_TRANSLATION, show).apply();
    }

    public boolean isAutoUpdateCheckEnabled() {
        // Default: TRUE (otomatis cek update saat aplikasi dibuka)
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_UPDATE_CHECK, true);
    }

    public void setAutoUpdateCheckEnabled(boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply();
    }

    public String getTheme() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_THEME, DEFAULT_THEME);
    }

    public void setTheme(String themeFile) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, themeFile)
                .remove(KEY_CUSTOM_BG_URI)
                .apply();
    }

    public String getCustomBgUri() {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_BG_URI, null);
    }

    public void setCustomBgUri(String uriStr) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CUSTOM_BG_URI, uriStr).apply();
    }

    public void playCountdownPip(int remainingSec) {
        if (!isCountdownAudioEnabled() || MODE_SILENT.equals(getSoundMode())) {
            return;
        }

        String assetFile = null;
        if (remainingSec == 30) {
            assetFile = "audio/warning_30s.wav";
        } else if (remainingSec == 20) {
            assetFile = "audio/warning_20s.wav";
        } else if (remainingSec >= 1 && remainingSec <= 10) {
            assetFile = "audio/count_" + remainingSec + ".wav";
        } else if (remainingSec == 0) {
            assetFile = "audio/count_0.wav";
        }

        if (assetFile != null) {
            playShortSound(assetFile);
        }
    }

    private void playShortSound(String assetPath) {
        try {
            if (pipPlayer != null) {
                try {
                    pipPlayer.reset();
                    pipPlayer.release();
                } catch (Exception ignored) {}
                pipPlayer = null;
            }
            AssetFileDescriptor afd = ctx.getAssets().openFd(assetPath);
            pipPlayer = new MediaPlayer();
            pipPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build());
            pipPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            pipPlayer.prepare();
            pipPlayer.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) {}
                if (pipPlayer == mp) pipPlayer = null;
            });
            pipPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + assetPath, e);
        }
    }

    public void playPrayerAlert() {
        String mode = getSoundMode();
        if (MODE_SILENT.equals(mode)) {
            Log.i(TAG, "Alert silent mode active");
            return;
        }

        stopAudio();

        try {
            String assetFile = MODE_BEEP.equals(mode) ? "audio/beep.wav" : "audio/adzan.ogg";
            AssetFileDescriptor afd = ctx.getAssets().openFd(assetFile);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build());
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopAudio());
            mediaPlayer.start();
            Log.i(TAG, "Playing alert sound: " + assetFile);
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio alert", e);
        }
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (pipPlayer != null) {
            try {
                if (pipPlayer.isPlaying()) pipPlayer.stop();
                pipPlayer.release();
            } catch (Exception ignored) {}
            pipPlayer = null;
        }
    }
}
