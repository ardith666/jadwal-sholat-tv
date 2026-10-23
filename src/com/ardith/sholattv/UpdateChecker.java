package com.ardith.sholattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_REPO = "ardith666/jadwal-sholat-tv";
    private static final String CURRENT_VERSION = "1.8";

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
                + "Apakah Anda ingin langsung mengunduh dan memperbarui aplikasi?";

        KioskService.setWatchdogEnabled(false);
        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🚀 Pembaruan Aplikasi Tersedia")
                .setMessage(msg)
                .setPositiveButton("Perbarui Sekarang", (d, which) -> {
                    KioskService.setWatchdogEnabled(true);
                    startDirectDownloadAndInstall(context, downloadUrl, latestTag);
                })
                .setNegativeButton("Nanti Saja", (d, which) -> KioskService.setWatchdogEnabled(true))
                .setOnCancelListener(d -> KioskService.setWatchdogEnabled(true))
                .create();
        dialog.setOnDismissListener(d -> KioskService.setWatchdogEnabled(true));
        dialog.show();
        try {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        } catch (Exception ignored) {}
    }

    private static void startDirectDownloadAndInstall(Context context, String downloadUrl, String versionTag) {
        ProgressDialog progress = new ProgressDialog(context, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        progress.setTitle("Mengunduh Pembaruan");
        progress.setMessage("Sedang mengunduh APK v" + versionTag + " langsung dari GitHub...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            File apkFile = new File(context.getCacheDir(), "jadwal-sholat-update.apk");
            if (apkFile.exists()) apkFile.delete();

            boolean success = false;
            try {
                URL currentUrl = new URL(downloadUrl);
                HttpURLConnection conn = null;
                int redirects = 0;

                while (redirects < 8) {
                    conn = (HttpURLConnection) currentUrl.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV; JadwalSholatTV-Updater)");
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);

                    int status = conn.getResponseCode();
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                        String loc = conn.getHeaderField("Location");
                        conn.disconnect();
                        currentUrl = new URL(loc);
                        redirects++;
                        continue;
                    }
                    break;
                }

                if (conn != null && conn.getResponseCode() == 200) {
                    int totalLength = conn.getContentLength();
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(apkFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        long downloaded = 0;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            downloaded += len;
                            if (totalLength > 0) {
                                int pct = (int) ((downloaded * 100) / totalLength);
                                ((Activity) context).runOnUiThread(() -> progress.setProgress(pct));
                            }
                        }
                        out.flush();
                        success = true;
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Download APK failed", e);
            }

            final boolean downloadedOk = success && apkFile.exists() && apkFile.length() > 50000;
            ((Activity) context).runOnUiThread(() -> {
                progress.dismiss();
                if (downloadedOk) {
                    installApk(context, apkFile);
                } else {
                    Toast.makeText(context, "Gagal mengunduh file update. Coba lagi nanti.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private static void installApk(Context context, File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Uri uri = AppFileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apkFile
            );

            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed launching package installer", e);
            Toast.makeText(context, "Gagal membuka installer APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
