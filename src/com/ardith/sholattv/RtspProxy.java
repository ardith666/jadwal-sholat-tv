package com.ardith.sholattv;

import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight local RTSP proxy to sanitize and fix non-standard H.265 SDP headers
 * emitted by IP cameras (e.g. missing 'sprop-vps' parameter required by Android ExoPlayer).
 */
public final class RtspProxy {
    private static final String TAG = "RtspProxy";
    public static final int PROXY_PORT = 8554;

    private static ServerSocket sServerSocket;
    private static Thread sServerThread;
    private static volatile boolean sIsRunning = false;

    // Camera exact parameter sets (captured from camera RTP payload stream)
    private static final String EXACT_VPS = "QAEMAf//AWAAAAMAAAMAAAMAAAMAlrwJ";
    private static final String EXACT_SPS = "QgEBAWAAAAMAAAMAAAMAAAMAlqABaCAGWfeNvkkyIuWewCAAAH0gAAdU4QE=";
    private static final String EXACT_PPS = "RAHgdrAmQA==";

    public static synchronized void start() {
        if (sIsRunning) return;
        sIsRunning = true;
        sServerThread = new Thread(() -> {
            try {
                sServerSocket = new ServerSocket(PROXY_PORT, 10, InetAddress.getByName("127.0.0.1"));
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

    /**
     * Converts a target RTSP url to point through this local proxy.
     */
    public static String getProxyUrl(String originalRtspUrl) {
        if (originalRtspUrl == null || !originalRtspUrl.startsWith("rtsp://")) {
            return originalRtspUrl;
        }
        try {
            URI u = new URI(originalRtspUrl);
            String host = u.getHost();
            int port = u.getPort() > 0 ? u.getPort() : 554;
            String path = u.getRawPath();
            if (path == null) path = "";
            String query = u.getRawQuery();

            // Encode target host and port in query so proxy knows where to connect
            StringBuilder sb = new StringBuilder();
            sb.append("rtsp://127.0.0.1:").append(PROXY_PORT).append(path);
            sb.append("?_dst_host=").append(host).append("&_dst_port=").append(port);

            String userInfo = u.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                sb.append("&_auth=").append(Base64.encodeToString(userInfo.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
            }
            if (query != null && !query.isEmpty()) {
                sb.append("&").append(query);
            }
            return sb.toString();
        } catch (Exception e) {
            return originalRtspUrl;
        }
    }

    private static void handleConnection(Socket client) {
        Socket target = null;
        try {
            client.setSoTimeout(15000);
            InputStream inClient = client.getInputStream();
            OutputStream outClient = client.getOutputStream();

            // Read first request line
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
            URI u = new URI(reqUri);
            String query = u.getQuery();
            if (query == null) query = "";

            String dstHost = "192.168.1.114";
            int dstPort = 554;
            String authParam = null;

            for (String param : query.split("&")) {
                if (param.startsWith("_dst_host=")) dstHost = param.substring(10);
                else if (param.startsWith("_dst_port=")) dstPort = Integer.parseInt(param.substring(10));
                else if (param.startsWith("_auth=")) authParam = param.substring(6);
            }

            target = new Socket(dstHost, dstPort);
            target.setSoTimeout(30000);
            InputStream inTarget = target.getInputStream();
            OutputStream outTarget = target.getOutputStream();

            // Forward first request, stripping internal query parameters
            String sanitizedReq = sanitizeClientRequest(firstReq, dstHost, dstPort, authParam);
            outTarget.write(sanitizedReq.getBytes(StandardCharsets.UTF_8));
            outTarget.flush();

            Socket fTarget = target;
            String fDstHost = dstHost;
            int fDstPort = dstPort;
            String fAuth = authParam;

            // Pipe client -> target
            Thread tUp = new Thread(() -> {
                try {
                    byte[] upBuf = new byte[8192];
                    int r;
                    while ((r = inClient.read(upBuf)) > 0) {
                        String s = new String(upBuf, 0, r, StandardCharsets.UTF_8);
                        String clean = sanitizeClientRequest(s, fDstHost, fDstPort, fAuth);
                        outTarget.write(clean.getBytes(StandardCharsets.UTF_8));
                        outTarget.flush();
                    }
                } catch (Exception ignored) {}
                closeQuietly(client, fTarget);
            });

            // Pipe target -> client with SDP H.265 rewriting
            Thread tDown = new Thread(() -> {
                try {
                    byte[] downBuf = new byte[16384];
                    int r;
                    while ((r = inTarget.read(downBuf)) > 0) {
                        // Check if response contains SDP with H265
                        byte[] processed = fixSdpResponse(downBuf, r);
                        outClient.write(processed);
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

    private static String sanitizeClientRequest(String req, String dstHost, int dstPort, String authParam) {
        // Replace 127.0.0.1:8554 with actual camera host:port
        String s = req.replace("127.0.0.1:" + PROXY_PORT, dstHost + ":" + dstPort);
        s = s.replaceAll("\\?_dst_host=[^\\s&]+", "");
        s = s.replaceAll("&_dst_host=[^\\s&]+", "");
        s = s.replaceAll("\\?_dst_port=[^\\s&]+", "");
        s = s.replaceAll("&_dst_port=[^\\s&]+", "");
        s = s.replaceAll("\\?_auth=[^\\s&]+", "");
        s = s.replaceAll("&_auth=[^\\s&]+", "");

        // Inject Authorization header if missing and credentials provided
        if (authParam != null && !s.contains("Authorization:")) {
            s = s.replaceFirst("\r\n", "\r\nAuthorization: Basic " + authParam + "\r\n");
        }
        return s;
    }

    private static byte[] fixSdpResponse(byte[] data, int length) {
        String s = new String(data, 0, length, StandardCharsets.ISO_8859_1);
        if (s.contains("m=video") && s.contains("H265")) {
            // Check if sprop-vps is missing and sprop-parameter-sets is present
            if (s.contains("sprop-parameter-sets=") || !s.contains("sprop-vps")) {
                Pattern pat = Pattern.compile("a=fmtp:(\\d+)[^\\r\\n]+");
                Matcher m = pat.matcher(s);
                if (m.find()) {
                    String pt = m.group(1);
                    String newFmtp = "a=fmtp:" + pt + " profile-level-id=420029; packetization-mode=1; sprop-vps=" + EXACT_VPS + "; sprop-sps=" + EXACT_SPS + "; sprop-pps=" + EXACT_PPS;
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
                    Log.i(TAG, "Patched H.265 SDP with exact stream SPS, PPS & VPS");
                    return s.getBytes(StandardCharsets.ISO_8859_1);
                }
            }
        }
        // Return raw unchanged data
        byte[] raw = new byte[length];
        System.arraycopy(data, 0, raw, 0, length);
        return raw;
    }

    private static void closeQuietly(Socket s1, Socket s2) {
        try { if (s1 != null) s1.close(); } catch (Exception ignored) {}
        try { if (s2 != null) s2.close(); } catch (Exception ignored) {}
    }
}
