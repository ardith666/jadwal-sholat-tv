package com.ardith.sholattv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public final class KioskService extends Service {
    private static final String TAG = "KioskService";
    private static final String CHANNEL_ID = "kiosk_service";

    private static volatile boolean appVisible = false;

    public static void start(Context context) {
        try {
            Intent i = new Intent(context, KioskService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception e) {
            Log.w(TAG, "KioskService start failed", e);
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, KioskService.class));
        } catch (Exception ignored) {}
    }

    public static void notifyActivityResumed() {
        appVisible = true;
    }

    public static void notifyActivityPaused() {
        appVisible = false;
    }

    public static boolean canRelaunch(Context context) {
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Mode Kiosk", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle("Jadwal Sholat TV")
                .setContentText("Mode kiosk aktif: aplikasi dikunci di layar TV")
                .setSmallIcon(R.drawable.ic_app)
                .setOngoing(true);
        try {
            startForeground(1, builder.build());
        } catch (Exception e) {
            Log.w(TAG, "startForeground failed", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        startWatchdog();
        return START_STICKY;
    }

    private void startWatchdog() {
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog, 800);
    }

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (!appVisible) {
                try {
                    Intent launch = new Intent(KioskService.this, MainActivity.class);
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    launch.setPackage(getPackageName());
                    startActivity(launch);
                    Log.i(TAG, "kiosk watchdog: relaunch aplikasi");
                } catch (Exception e) {
                    Log.w(TAG, "kiosk watchdog relaunch failed", e);
                }
            }
            handler.postDelayed(this, 800);
        }
    };

    @Override
    public void onDestroy() {
        handler.removeCallbacks(watchdog);
        super.onDestroy();
    }
}
