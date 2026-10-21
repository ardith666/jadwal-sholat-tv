package com.ardith.sholattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_REPO = "ardith666/jadwal-sholat-tv";
    private static final String CURRENT_VERSION = "1.3";

    public interface UpdateCheckCallback {
        void onCheckFinished(boolean updateAvailable, String latestTag, String downloadUrl, String releaseNotes);
    }

    public static void check(Context context, boolean isManualTrigger, UpdateCheckCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "JadwalSholatTV-App");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String l;
                    while ((l = r.readLine()) != null) sb.append(l);
                    conn.disconnect();

                    JSONObject root = new JSONObject(sb.toString());
                    String tagName = root.optString("tag_name", "").replace("v", "").trim();
                    String body = root.optString("body", "");
                    String htmlUrl = root.optString("html_url", "https://github.com/" + GITHUB_REPO + "/releases");
                    String apkDownloadUrl = htmlUrl;

                    JSONArray assets = root.optJSONArray("assets");
                    if (assets != null && assets.length() > 0) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.getJSONObject(i);
                            String name = a.optString("name", "");
                            if (name.endsWith(".apk")) {
                                apkDownloadUrl = a.optString("browser_download_url", htmlUrl);
                                break;
                            }
                        }
                    }

                    boolean isNewer = compareVersions(tagName, CURRENT_VERSION) > 0;
                    if (callback != null) {
                        final String finalTag = tagName;
                        final String finalUrl = apkDownloadUrl;
                        final String finalBody = body;
                        ((Activity) context).runOnUiThread(() ->
                                callback.onCheckFinished(isNewer, finalTag, finalUrl, finalBody));
                    }
                    return;
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Failed to check update: " + e.getMessage());
            }

            if (callback != null) {
                ((Activity) context).runOnUiThread(() ->
                        callback.onCheckFinished(false, null, null, null));
            }
        }).start();
    }

    public static void showUpdateDialog(Context context, String latestTag, String downloadUrl, String notes) {
        String msg = "Versi Baru Tersedia: v" + latestTag + " (Versi saat ini: v" + CURRENT_VERSION + ")\n\n"
                + (notes != null && !notes.isEmpty() ? "Catatan Rilis:\n" + notes + "\n\n" : "")
                + "Apakah Anda ingin membuka link download rilis terbaru?";

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🚀 Pembaruan Aplikasi Tersedia")
                .setMessage(msg)
                .setPositiveButton("Buka Unduhan", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Tidak ada browser untuk membuka link unduhan", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Nanti Saja", null)
                .show();
    }

    private static int compareVersions(String v1, String v2) {
        if (v1 == null || v1.isEmpty()) return -1;
        if (v2 == null || v2.isEmpty()) return 1;
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseSafe(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseSafe(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static int parseSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
