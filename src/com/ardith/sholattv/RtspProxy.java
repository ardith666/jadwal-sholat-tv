package com.ardith.sholattv;

import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust local RTSP proxy.
 * - Handles cameras that require Digest/Basic Authentication on OPTIONS, DESCRIBE, SETUP, and PLAY.
 * - Strips credentials from URL authority to avoid Android's Uri '@' bug.
 * - Injects missing H.265 SDP sprop-vps / sprop-sps / sprop-pps parameters.
 */
public final class RtspProxy {
    private static final String TAG = "RtspProxy";
    public static final int PROXY_PORT = 8554;

    private static ServerSocket sServerSocket;
    private static Thread sServerThread;
    private static volatile boolean sIsRunning = false;

    private static final String DEFAULT_VPS = "QAEMAf//AWAAAAMAAAMAAAMAAAMAlrwJ";
    private static final String DEFAULT_SPS_MAIN = "QgEBAWAAAAMAAAMAAAMAAAMAlqABaCAGWfeNvkkyIuWewCAAAH0gAAdU4QE=";
    private static final String DEFAULT_SPS_SUB = "QgEBAWAAAAMAAAMAAAMAAAMAWqAGAgGxY2uSTJrljAIAAAfSAABONBBBqAM=";
    private static final String DEFAULT_PPS = "RAHgdrAmQA==";

    private static final Pattern RTSP_URL_PATTERN =
            Pattern.compile("^rtsp://(?:([^:]+):(.*)@)?([^:/]+)(?::(\\d+))?(/.*)?$");

    public static synchronized void start() {
        if (sIsRunning) return;
        sIsRunning = true;
        sServerThread = new Thread(() -> {
            try {
                sServerSocket = new ServerSocket(PROXY_PORT, 20, InetAddress.getByName("127.0.0.1"));
                Log.i(TAG, "RTSP Proxy listening on 127.0.0.1:" + PROXY_PORT);
                while (sIsRunning) {
                    try {
                        Socket client = sServerSocket.accept();
                        new Thread(() -> handleConnection(client)).start();
                    } catch (Exception e) {
                        if (!sIsRunning) break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "RTSP Proxy Server error", e);
            }
        });
        sServerThread.start();
    }

    public static synchronized void stop() {
        sIsRunning = false;
        try {
            if (sServerSocket != null) sServerSocket.close();
        } catch (Exception ignored) {}
        sServerSocket = null;
        if (sServerThread != null) {
            sServerThread.interrupt();
            sServerThread = null;
        }
        Log.i(TAG, "RTSP Proxy stopped");
    }

    public static String getProxyUrl(String originalRtspUrl) {
        if (originalRtspUrl == null || !originalRtspUrl.startsWith("rtsp://")) {
            return originalRtspUrl;
        }

        String effectiveUrl = originalRtspUrl.trim();

        if (effectiveUrl.contains("192.168.1.114") && effectiveUrl.contains("/ch1/main")) {
            Log.i(TAG, "Auto-switching 192.168.1.114 from 3K main-stream to 720p sub-stream for TV decoder compatibility");
            effectiveUrl = effectiveUrl.replace("/ch1/main", "/ch1/sub");
        }

        Matcher m = RTSP_URL_PATTERN.matcher(effectiveUrl);
        if (!m.matches()) {
            return originalRtspUrl;
        }

        String user = m.group(1);
        String pass = m.group(2);
        String host = m.group(3);
        String portStr = m.group(4);
        String pathWithQuery = m.group(5);

        int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 554;
        if (pathWithQuery == null) pathWithQuery = "/";

        String path = pathWithQuery;
        String query = "";
        int qIdx = pathWithQuery.indexOf('?');
        if (qIdx != -1) {
            path = pathWithQuery.substring(0, qIdx);
            query = pathWithQuery.substring(qIdx + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("rtsp://127.0.0.1:").append(PROXY_PORT).append(path);

        if (query != null && !query.isEmpty()) {
            sb.append("?").append(query).append("&_dst_host=").append(host).append("&_dst_port=").append(port);
        } else {
            sb.append("?_dst_host=").append(host).append("&_dst_port=").append(port);
        }

        if (user != null) {
            sb.append("&_u=").append(Base64.encodeToString(user.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        }
        if (pass != null) {
            sb.append("&_p=").append(Base64.encodeToString(pass.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        }

        return sb.toString();
    }

    private static void handleConnection(Socket client) {
        Socket target = null;
        try {
            client.setSoTimeout(15000);
            InputStream inClient = client.getInputStream();
            OutputStream outClient = client.getOutputStream();

            byte[] buf = new byte[8192];
            int n = inClient.read(buf);
            if (n <= 0) {
                client.close();
                return;
            }

            String firstReq = new String(buf, 0, n, StandardCharsets.UTF_8);
            String firstLine = firstReq.split("\r\n", 2)[0];
            String[] parts = firstLine.split(" ");
            if (parts.length < 2) {
                client.close();
                return;
            }

            String reqUri = parts[1];
            String query = "";
            int qIdx = reqUri.indexOf('?');
            if (qIdx != -1) {
                query = reqUri.substring(qIdx + 1);
            }

            String dstHost = "192.168.1.114";
            int dstPort = 554;
            String user = null;
            String pass = null;

            for (String param : query.split("&")) {
                if (param.startsWith("_dst_host=")) {
                    dstHost = param.substring(10);
                } else if (param.startsWith("_dst_port=")) {
                    try { dstPort = Integer.parseInt(param.substring(10)); } catch (Exception ignored) {}
                } else if (param.startsWith("_u=")) {
                    try { user = new String(Base64.decode(param.substring(3), Base64.NO_WRAP), StandardCharsets.UTF_8); } catch (Exception ignored) {}
                } else if (param.startsWith("_p=")) {
                    try { pass = new String(Base64.decode(param.substring(3), Base64.NO_WRAP), StandardCharsets.UTF_8); } catch (Exception ignored) {}
                }
            }

            target = new Socket(dstHost, dstPort);
            target.setSoTimeout(30000);
            InputStream inTarget = target.getInputStream();
            OutputStream outTarget = target.getOutputStream();

            final String fUser = user;
            final String fPass = pass;
            final String fHost = dstHost;
            final int fPort = dstPort;

            final String[] activeAuthHolder = new String[1]; // Holds current Digest/Basic header

            // Forward first request
            String cleanFirstReq = sanitizeClientRequest(firstReq, fHost, fPort);
            outTarget.write(cleanFirstReq.getBytes(StandardCharsets.UTF_8));
            outTarget.flush();

            // Read target response
            byte[] targetBuf = new byte[16384];
            int rTarget = inTarget.read(targetBuf);
            if (rTarget <= 0) {
                closeQuietly(client, target);
                return;
            }

            String targetResp = new String(targetBuf, 0, rTarget, StandardCharsets.ISO_8859_1);

            // If 401 Unauthorized, solve challenge immediately!
            if (targetResp.startsWith("RTSP/1.0 401") && fUser != null && fPass != null) {
                String method = parts[0];
                String uri = extractTargetUri(cleanFirstReq);
                activeAuthHolder[0] = computeAuthHeader(targetResp, method, uri, fUser, fPass);

                if (activeAuthHolder[0] != null) {
                    String authedReq = injectAuth(cleanFirstReq, activeAuthHolder[0]);
                    outTarget.write(authedReq.getBytes(StandardCharsets.UTF_8));
                    outTarget.flush();

                    rTarget = inTarget.read(targetBuf);
                    if (rTarget <= 0) {
                        closeQuietly(client, target);
                        return;
                    }
                }
            }

            byte[] processed = fixSdpResponse(targetBuf, rTarget);
            outClient.write(processed);
            outClient.flush();

            final Socket fTarget = target;

            // Pipe Client -> Target (translates URIs & injects fresh Auth per method/uri)
            Thread tUp = new Thread(() -> {
                try {
                    byte[] upBuf = new byte[8192];
                    int r;
                    while ((r = inClient.read(upBuf)) > 0) {
                        String s = new String(upBuf, 0, r, StandardCharsets.UTF_8);
                        String clean = sanitizeClientRequest(s, fHost, fPort);
                        if (activeAuthHolder[0] != null && !clean.contains("Authorization:")) {
                            String mth = clean.split(" ")[0];
                            String uTarget = extractTargetUri(clean);
                            String dynAuth = computeDigestAuth(activeAuthHolder[0], mth, uTarget, fUser, fPass);
                            clean = injectAuth(clean, dynAuth != null ? dynAuth : activeAuthHolder[0]);
                        }
                        outTarget.write(clean.getBytes(StandardCharsets.UTF_8));
                        outTarget.flush();
                    }
                } catch (Exception ignored) {}
                closeQuietly(client, fTarget);
            });

            // Pipe Target -> Client (re-calculates SDP if needed)
            Thread tDown = new Thread(() -> {
                try {
                    byte[] downBuf = new byte[16384];
                    int r;
                    while ((r = inTarget.read(downBuf)) > 0) {
                        byte[] p = fixSdpResponse(downBuf, r);
                        outClient.write(p);
                        outClient.flush();
                    }
                } catch (Exception ignored) {}
                closeQuietly(client, fTarget);
            });

            tUp.start();
            tDown.start();

        } catch (Exception e) {
            Log.w(TAG, "Proxy pipe error: " + e.getMessage());
            closeQuietly(client, target);
        }
    }

    private static String extractTargetUri(String req) {
        try {
            String[] parts = req.split("\r\n")[0].split(" ");
            return parts[1];
        } catch (Exception e) {
            return "";
        }
    }

    private static String injectAuth(String req, String authHeader) {
        return req.replaceFirst("\r\n", "\r\nAuthorization: " + authHeader + "\r\n");
    }

    private static String computeAuthHeader(String resp401, String method, String uri, String user, String pass) {
        if (resp401.contains("Digest")) {
            Pattern realmPat = Pattern.compile("realm=\"([^\"]+)\"");
            Pattern noncePat = Pattern.compile("nonce=\"([^\"]+)\"");
            Matcher mR = realmPat.matcher(resp401);
            Matcher mN = noncePat.matcher(resp401);
            if (mR.find() && mN.find()) {
                String realm = mR.group(1);
                String nonce = mN.group(1);
                String ha1 = md5(user + ":" + realm + ":" + pass);
                String ha2 = md5(method + ":" + uri);
                String response = md5(ha1 + ":" + nonce + ":" + ha2);
                return "Digest username=\"" + user + "\", realm=\"" + realm + "\", nonce=\"" + nonce + "\", uri=\"" + uri + "\", response=\"" + response + "\"";
            }
        }
        String cred = Base64.encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return "Basic " + cred;
    }

    private static String computeDigestAuth(String baseAuth, String method, String uri, String user, String pass) {
        if (baseAuth == null || !baseAuth.startsWith("Digest")) return baseAuth;
        try {
            Pattern realmPat = Pattern.compile("realm=\"([^\"]+)\"");
            Pattern noncePat = Pattern.compile("nonce=\"([^\"]+)\"");
            Matcher mR = realmPat.matcher(baseAuth);
            Matcher mN = noncePat.matcher(baseAuth);
            if (mR.find() && mN.find()) {
                String realm = mR.group(1);
                String nonce = mN.group(1);
                String ha1 = md5(user + ":" + realm + ":" + pass);
                String ha2 = md5(method + ":" + uri);
                String response = md5(ha1 + ":" + nonce + ":" + ha2);
                return "Digest username=\"" + user + "\", realm=\"" + realm + "\", nonce=\"" + nonce + "\", uri=\"" + uri + "\", response=\"" + response + "\"";
            }
        } catch (Exception ignored) {}
        return baseAuth;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String sanitizeClientRequest(String req, String dstHost, int dstPort) {
        String s = req.replaceAll("127\\.0\\.0\\.1:" + PROXY_PORT, dstHost + ":" + dstPort);
        s = s.replaceAll("[?&]_dst_host=[^\\s&]+", "");
        s = s.replaceAll("[?&]_dst_port=[^\\s&]+", "");
        s = s.replaceAll("[?&]_u=[^\\s&]+", "");
        s = s.replaceAll("[?&]_p=[^\\s&]+", "");
        // Jika query string asli menjadi diawali '&' setelah parameter proxy dihapus, ubah ke '?'
        s = s.replaceAll("/&", "/?");
        s = s.replaceAll("\\?&", "?");
        return s;
    }

    private static byte[] fixSdpResponse(byte[] data, int length) {
        String s = new String(data, 0, length, StandardCharsets.ISO_8859_1);
        if (s.contains("m=video") && s.contains("H265")) {
            if (!s.contains("sprop-vps")) {
                Pattern pat = Pattern.compile("a=fmtp:(\\d+)[^\\r\\n]+");
                Matcher m = pat.matcher(s);
                if (m.find()) {
                    String pt = m.group(1);
                    boolean isSub = s.contains("/ch1/sub") || s.contains("subtype=1");
                    String spsToUse = isSub ? DEFAULT_SPS_SUB : DEFAULT_SPS_MAIN;

                    String newFmtp = "a=fmtp:" + pt + " profile-level-id=420029; packetization-mode=1; sprop-vps=" + DEFAULT_VPS + "; sprop-sps=" + spsToUse + "; sprop-pps=" + DEFAULT_PPS;
                    s = s.replace(m.group(0), newFmtp);

                    Pattern lenPat = Pattern.compile("Content-Length:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
                    Matcher lenM = lenPat.matcher(s);
                    if (lenM.find()) {
                        String[] parts = s.split("\r\n\r\n", 2);
                        if (parts.length == 2) {
                            int bodyLen = parts[1].getBytes(StandardCharsets.ISO_8859_1).length;
                            String header = lenM.replaceFirst("Content-Length: " + bodyLen);
                            s = header;
                        }
                    }
                    Log.i(TAG, "Patched missing H.265 sprop-vps in SDP successfully");
                    return s.getBytes(StandardCharsets.ISO_8859_1);
                }
            }
        }
        byte[] raw = new byte[length];
        System.arraycopy(data, 0, raw, 0, length);
        return raw;
    }

    private static void closeQuietly(Socket s1, Socket s2) {
        try { if (s1 != null) s1.close(); } catch (Exception ignored) {}
        try { if (s2 != null) s2.close(); } catch (Exception ignored) {}
    }
}
