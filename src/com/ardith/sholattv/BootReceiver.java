package com.ardith.sholattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !ACTION_QUICKBOOT.equals(action)) return;

        AlertManager alertManager = new AlertManager(context);
        if (!alertManager.isKioskEnabled()) return;

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(launch);
            Log.i(TAG, "Kiosk auto-start launched");
        } catch (Exception e) {
            Log.e(TAG, "Kiosk auto-start blocked", e);
        }
    }
}
