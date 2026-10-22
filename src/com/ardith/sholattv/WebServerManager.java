package com.ardith.sholattv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class WebServerManager {
    private static final String TAG = "WebServerManager";
    public static final int PORT = 8080;

    public interface OnWebUpdateListener {
        void onSlidesUpdated();
        void onSettingsUpdated();
    }

    private final Context ctx;
    private final SlideManager slideManager;
    private final PrayerEngine prayerEngine;
    private final AlertManager alertManager;
    private final OnWebUpdateListener listener;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WebServerManager(Context context, SlideManager slideManager, PrayerEngine prayerEngine, AlertManager alertManager, OnWebUpdateListener listener) {
        this.ctx = context.getApplicationContext();
        this.slideManager = slideManager;
        this.prayerEngine = prayerEngine;
        this.alertManager = alertManager;
        this.listener = listener;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.i(TAG, "WebServer started on port " + PORT);
                while (isRunning) {
                    try {
                        Socket client = serverSocket.accept();
                        new Thread(() -> handleClient(client)).start();
                    } catch (Exception e) {
                        if (!isRunning) break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Server error", e);
            }
        });
        serverThread.start();
    }

    public synchronized void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        serverSocket = null;
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        Log.i(TAG, "WebServer stopped");
    }

    private void handleClient(Socket client) {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null) {
                client.close();
                return;
            }

            String[] reqParts = requestLine.split(" ");
            if (reqParts.length < 2) {
                client.close();
                return;
            }

            String method = reqParts[0];
            String uri = reqParts[1];

            // Read headers to extract Content-Length
            int contentLength = 0;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                }
            }

            // Read body if POST
            String body = "";
            if ("POST".equalsIgnoreCase(method) && contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(bodyChars, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                body = new String(bodyChars, 0, read);
            }

            routeRequest(method, uri, body, out);
            out.flush();
            client.close();
        } catch (Exception e) {
            Log.w(TAG, "Client handle error: " + e.getMessage());
        }
    }

    private void routeRequest(String method, String uri, String body, OutputStream out) throws Exception {
        String path = uri;
        String query = "";
        int qIdx = uri.indexOf("?");
        if (qIdx != -1) {
            path = uri.substring(0, qIdx);
            query = uri.substring(qIdx + 1);
        }

        // STATIC ASSETS
        if ("GET".equalsIgnoreCase(method)) {
            if ("/".equals(path) || "/index.html".equals(path)) {
                sendAssetFile("web/index.html", "text/html; charset=utf-8", out);
                return;
            } else if ("/style.css".equals(path)) {
                sendAssetFile("web/style.css", "text/css; charset=utf-8", out);
                return;
            } else if ("/app.js".equals(path)) {
                sendAssetFile("web/app.js", "application/javascript; charset=utf-8", out);
                return;
            } else if (path.startsWith("/images/")) {
                // Serve custom uploaded slide images
                String imgName = path.substring(8);
                File imgFile = new File(slideManager.getImageFolder(), imgName);
                if (imgFile.exists()) {
                    sendFile(imgFile, guessImageType(imgFile.getName()), out);
                    return;
                }
            } else if (path.startsWith("/backgrounds/")) {
                // Serve bundled theme backgrounds for the admin preview gallery
                String bgName = safeFileName(path.substring(13));
                if (bgName != null) {
                    try {
                        sendAssetFile("backgrounds/" + bgName, "image/jpeg", out);
                        return;
                    } catch (Exception ignored) {}
                }
            }
        }

        // REST API ROUTES
        if ("/api/auth".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            boolean ok = slideManager.getAdminPin().equals(pin);
            sendJsonResponse(out, new JSONObject().put("status", ok));
            return;
        }

        if ("/api/slides".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<SlideModel> list = slideManager.getAllSlides();
            JSONArray arr = new JSONArray();
            for (SlideModel s : list) arr.put(s.toJsonObject());
            sendJsonResponse(out, new JSONObject().put("status", true).put("slides", arr));
            return;
        }

        if ("/api/slides/save".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }
            JSONArray arr = req.optJSONArray("slides");
            List<SlideModel> newSlides = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    SlideModel m = SlideModel.fromJsonObject(arr.getJSONObject(i));
                    if (m != null) newSlides.add(m);
                }
            }
            slideManager.saveSlides(newSlides);
            notifySlidesUpdated();
            sendJsonResponse(out, new JSONObject().put("status", true));
            return;
        }

        if ("/api/upload-image".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }
            String fileName = req.optString("fileName", "img_" + System.currentTimeMillis() + ".jpg");
            String base64Data = req.optString("base64Data", "");
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

            File target = new File(slideManager.getImageFolder(), fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(bytes);
            }
            sendJsonResponse(out, new JSONObject().put("status", true).put("fileName", fileName));
            return;
        }

        if ("/api/upload-background".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }
            String fileName = req.optString("fileName", "");
            String safe = safeFileName(fileName);
            if (safe == null) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "Nama file tidak valid"));
                return;
            }
            String base64Data = req.optString("base64Data", "");
            if (base64Data.isEmpty()) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "Data gambar kosong"));
                return;
            }
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            File target = new File(slideManager.getImageFolder(), safe);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(bytes);
            }
            alertManager.setCustomBgUri(android.net.Uri.fromFile(target).toString());
            notifySettingsUpdated();
            sendJsonResponse(out, new JSONObject().put("status", true).put("fileName", safe));
            return;
        }

        if ("/api/theme/save".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }
            String theme = req.optString("theme", "");
            if (safeFileName(theme) == null) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "Tema tidak valid"));
                return;
            }
            alertManager.setTheme(theme);
            notifySettingsUpdated();
            sendJsonResponse(out, new JSONObject().put("status", true));
            return;
        }

        if ("/api/background/reset".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }
            alertManager.clearCustomBg();
            notifySettingsUpdated();
            sendJsonResponse(out, new JSONObject().put("status", true));
            return;
        }

        if ("/api/settings".equals(path) && "GET".equalsIgnoreCase(method)) {
            JSONObject s = new JSONObject();
            s.put("currentCityId", prayerEngine.getCityId());
            s.put("currentCityName", prayerEngine.getCityName());
            s.put("mosqueName", prayerEngine.getMosqueName());
            s.put("currentTheme", alertManager.getTheme());
            s.put("soundMode", alertManager.getSoundMode());
            s.put("countdownEnabled", alertManager.isCountdownEnabled());
            s.put("countdownAudioEnabled", alertManager.isCountdownAudioEnabled());
            s.put("keepAwake", alertManager.isKeepAwake());
            s.put("kioskEnabled", alertManager.isKioskEnabled());
            s.put("showArab", alertManager.isShowArab());
            s.put("showLatin", alertManager.isShowLatin());
            s.put("showTranslation", alertManager.isShowTranslation());
            s.put("iqomahEnabled", alertManager.isIqomahEnabled());
            s.put("iqomahSubuh", alertManager.getIqomahMinutes("SUBUH"));
            s.put("iqomahDzuhur", alertManager.getIqomahMinutes("DZUHUR"));
            s.put("iqomahAshar", alertManager.getIqomahMinutes("ASHAR"));
            s.put("iqomahMaghrib", alertManager.getIqomahMinutes("MAGHRIB"));
            s.put("iqomahIsya", alertManager.getIqomahMinutes("ISYA"));
            s.put("greenScreenDuration", alertManager.getGreenScreenDurationSec());

            List<PrayerEngine.City> cities = prayerEngine.loadCities();
            Collections.sort(cities, (a, b) -> a.name.compareToIgnoreCase(b.name));
            JSONArray cArr = new JSONArray();
            for (PrayerEngine.City c : cities) {
                cArr.put(new JSONObject().put("id", c.id).put("name", c.name).put("province", c.province));
            }
            s.put("cities", cArr);

            String customBg = alertManager.getCustomBgUri();
            String customBgFile = "";
            if (customBg != null) {
                int slash = Math.max(customBg.lastIndexOf('/'), customBg.lastIndexOf("%2F"));
                customBgFile = slash >= 0 ? customBg.substring(slash + 1) : customBg;
                try {
                    customBgFile = java.net.URLDecoder.decode(customBgFile, "UTF-8");
                } catch (Exception ignored) {}
            }
            s.put("customBgActive", customBg != null);
            s.put("customBgFile", customBgFile);

            JSONArray tArr = new JSONArray();
            for (String[] t : SettingsDialog.DARK_THEMES) {
                tArr.put(new JSONArray().put(t[0]).put(t[1]));
            }
            for (String[] t : SettingsDialog.LIGHT_THEMES) {
                tArr.put(new JSONArray().put(t[0]).put(t[1]));
            }
            s.put("themes", tArr);

            sendJsonResponse(out, new JSONObject().put("status", true).put("settings", s));
            return;
        }

        if ("/api/settings/save".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String pin = req.optString("pin", "");
            if (!slideManager.getAdminPin().equals(pin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Salah"));
                return;
            }

            prayerEngine.setCity(req.optString("cityId"), req.optString("cityName"));
            prayerEngine.setMosqueName(req.optString("mosqueName"));
            String savedTheme = req.optString("theme", "");
            if (!savedTheme.isEmpty() && !savedTheme.equals(alertManager.getTheme())) {
                alertManager.setTheme(savedTheme);
            }
            alertManager.setSoundMode(req.optString("soundMode"));
            alertManager.setCountdownEnabled(req.optBoolean("countdownEnabled", true));
            alertManager.setCountdownAudioEnabled(req.optBoolean("countdownAudioEnabled", true));
            alertManager.setKeepAwake(req.optBoolean("keepAwake", true));
            alertManager.setKioskEnabled(req.optBoolean("kioskEnabled", true));
            alertManager.setShowArab(req.optBoolean("showArab", true));
            alertManager.setShowLatin(req.optBoolean("showLatin", true));
            alertManager.setShowTranslation(req.optBoolean("showTranslation", true));

            alertManager.setIqomahEnabled(req.optBoolean("iqomahEnabled", true));
            alertManager.setIqomahMinutes("SUBUH", req.optInt("iqomahSubuh", 10));
            alertManager.setIqomahMinutes("DZUHUR", req.optInt("iqomahDzuhur", 5));
            alertManager.setIqomahMinutes("ASHAR", req.optInt("iqomahAshar", 5));
            alertManager.setIqomahMinutes("MAGHRIB", req.optInt("iqomahMaghrib", 7));
            alertManager.setIqomahMinutes("ISYA", req.optInt("iqomahIsya", 5));
            alertManager.setGreenScreenDurationSec(req.optInt("greenScreenDuration", 60));

            notifySettingsUpdated();
            sendJsonResponse(out, new JSONObject().put("status", true));
            return;
        }

        if ("/api/pin/save".equals(path) && "POST".equalsIgnoreCase(method)) {
            JSONObject req = new JSONObject(body);
            String curPin = req.optString("currentPin", "");
            String newPin = req.optString("newPin", "").trim();
            if (!slideManager.getAdminPin().equals(curPin)) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Lama Salah"));
                return;
            }
            if (newPin.isEmpty()) {
                sendJsonResponse(out, new JSONObject().put("status", false).put("message", "PIN Baru tidak boleh kosong"));
                return;
            }
            slideManager.setAdminPin(newPin);
            sendJsonResponse(out, new JSONObject().put("status", true));
            return;
        }

        // 404 NOT FOUND
        send404(out);
    }

    private static String guessImageType(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private static String safeFileName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) return null;
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void notifySlidesUpdated() {
        if (listener != null) mainHandler.post(listener::onSlidesUpdated);
    }

    private void notifySettingsUpdated() {
        if (listener != null) mainHandler.post(listener::onSettingsUpdated);
    }

    private void sendJsonResponse(OutputStream out, JSONObject json) throws Exception {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
    }

    private void sendAssetFile(String assetPath, String contentType, OutputStream out) throws Exception {
        try (InputStream in = ctx.getAssets().open(assetPath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            byte[] bytes = baos.toByteArray();

            String header = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
        }
    }

    private void sendFile(File file, String contentType, OutputStream out) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) baos.write(buf, 0, n);
            byte[] bytes = baos.toByteArray();

            String header = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
        }
    }

    private void send404(OutputStream out) throws Exception {
        String res = "HTTP/1.1 404 Not Found\r\nContent-Length: 13\r\nConnection: close\r\n\r\n404 Not Found";
        out.write(res.getBytes(StandardCharsets.UTF_8));
    }
}
