package cc.cd.relaxingocean;

import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/** Loads the live ocean page and applies the TV-only picture cap. */
final class OceanFetch {
    private OceanFetch() {}

    static boolean target(WebResourceRequest request) {
        if (request == null || request.getUrl() == null) return false;
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        if (!"https".equalsIgnoreCase(request.getUrl().getScheme())) return false;
        if (!"relaxingocean.cc.cd".equalsIgnoreCase(request.getUrl().getHost())) return false;
        String path = request.getUrl().getPath();
        if (path == null) path = "";
        if (request.isForMainFrame()) return path.isEmpty() || "/".equals(path) || "/index.html".equals(path);
        return "/main.js".equals(path) || "/whale_dynamics.mjs".equals(path);
    }

    static WebResourceResponse response(android.net.Uri uri, String userAgent) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setInstanceFollowRedirects(true);
        conn.setUseCaches(false);
        if (userAgent != null && !userAgent.isEmpty()) conn.setRequestProperty("User-Agent", userAgent);
        int code;
        byte[] body;
        boolean html;
        try {
            code = conn.getResponseCode();
            URL landed = conn.getURL();
            String path = uri.getPath() == null ? "" : uri.getPath();
            html = !path.endsWith("/main.js") && !path.endsWith("/whale_dynamics.mjs");
            if (code != HttpURLConnection.HTTP_OK || landed == null
                    || !"relaxingocean.cc.cd".equalsIgnoreCase(landed.getHost())) {
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                body = readLimited(in, html ? 2_000_000 : 4_000_000);
            }
        } finally {
            conn.disconnect();
        }
        boolean motion = uri.getPath() != null && uri.getPath().endsWith("/whale_dynamics.mjs");
        Log.i("OceanLite", motion ? "motion" : (html ? "html" : "script"));
        if (motion) {
            body = TvLite.motion(body);
            return javascript(body);
        }
        if (html) {
            body = TvLite.html(new String(body, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            return new WebResourceResponse("text/html", "utf-8", 200, "OK",
                    Collections.singletonMap("Content-Type", "text/html; charset=utf-8"),
                    new ByteArrayInputStream(body));
        }
        return javascript(TvLite.script(body));
    }

    private static WebResourceResponse javascript(byte[] body) {
        return new WebResourceResponse("application/javascript", "utf-8", 200, "OK",
                Collections.singletonMap("Content-Type", "application/javascript; charset=utf-8"),
                new ByteArrayInputStream(body));
    }

    private static byte[] readLimited(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > max) throw new IOException("too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
