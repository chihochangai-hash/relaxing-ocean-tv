package cc.cd.relaxingocean;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Full-screen Relaxing Ocean. Arrows move a ring over the page buttons.
 * Center sends a real tap, so the site treats it as a press.
 * After the manual-control button, Up swims forward, Left and Right turn, and Down stops.
 * Back leaves that mode.
 */
public class OceanActivity extends Activity {
    static final String SITE = "https://relaxingocean.cc.cd/";
    private static final String COLLECT =
            "(function(){return window.__oceanTv?window.__oceanTv.collect():null;})()";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private WebView web;
    private TextView message;
    private String script = "";
    private volatile String userAgent = "";
    private String focused;
    private boolean userMoved;
    private boolean failed;
    private int ticket;
    private final Runnable settle = this::settle;
    private int liteTries;
    private final Runnable liteCheck = this::checkLite;
    private final Steer steer = new Steer();
    private static final String LITE_CHECK =
            "(function(){var r=window.oceanApp&&window.oceanApp.renderer;"
                    + "var c=document.getElementById('main-canvas');"
                    + "var gl=c&&(c.getContext('webgl2')||c.getContext('webgl'));"
                    + "var aa=gl&&gl.getContextAttributes()?gl.getContextAttributes().antialias:null;"
                    + "return JSON.stringify({lite:window.__oceanTvLite||0,ratio:r?r.getPixelRatio():null,aa:aa});})()";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#041018"));
        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(20);
        message.setGravity(Gravity.CENTER);
        message.setBackgroundColor(Color.parseColor("#041018"));
        message.setVisibility(View.GONE);
        script = readScript();
        if (!mount()) {
            message.setText(getString(R.string.no_web));
            message.setVisibility(View.VISIBLE);
        }
        root.addView(message, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        hideBars();
        if (web != null) load();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        if (steer.driving && web != null) {
            web.evaluateJavascript("window.__oceanTv&&window.__oceanTv.releaseSteer()", null);
        }
        if (web != null) {
            web.onPause();
            web.pauseTimers();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(settle);
        handler.removeCallbacks(liteCheck);
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (web == null) return super.dispatchKeyEvent(event);
        int key = event.getKeyCode();
        if (back(key)) {
            int decision = steer.back(event.getAction(), event.getRepeatCount());
            if (decision == Steer.EXIT) leaveManual();
            if (decision != Steer.IGNORE) return true;
            return super.dispatchKeyEvent(event);
        }
        Integer direction = direction(key);
        if (direction != null) {
            if (steer.driving) {
                if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP) {
                    steerKey(Steer.arrow(direction), event.getAction() == KeyEvent.ACTION_DOWN);
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) move(direction);
            return true;
        }
        if (steer.driving && center(key)) return true;
        if (center(key)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) press();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean mount() {
        WebView created;
        try {
            created = new WebView(this);
        } catch (Throwable ignored) {
            return false;
        }
        prepare(created);
        root.addView(created, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        web = created;
        return true;
    }

    private void load() {
        if (web == null && !mount()) return;
        failed = false;
        message.setVisibility(View.GONE);
        web.loadUrl(SITE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void prepare(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        userAgent = settings.getUserAgentString();
        view.setBackgroundColor(Color.parseColor("#041018"));
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setVerticalScrollBarEnabled(false);
        view.setHorizontalScrollBarEnabled(false);
        view.setFocusable(false);
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String line = consoleMessage.message();
                if (line != null && line.contains(TvLite.MARK)) Log.i("OceanLite", line);
                return false;
            }
        });
        view.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!OceanFetch.target(request)) return null;
                try {
                    return OceanFetch.response(request.getUrl(), userAgent);
                } catch (IOException ignored) {
                    Log.i("OceanLite", "fetch failed");
                    return null;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl() == null ? "" : request.getUrl().getScheme();
                scheme = scheme == null ? "" : scheme.toLowerCase(Locale.US);
                return !"http".equals(scheme) && !"https".equals(scheme);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                failed = false;
                userMoved = false;
                steer.driving = false;
                steer.swallowBackUp = false;
                focused = null;
                ticket++;
                message.setVisibility(View.GONE);
                handler.removeCallbacks(liteCheck);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                arm();
                watchLite();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showFailure();
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                handler.removeCallbacks(settle);
                if (view != null) root.removeView(view);
                if (web == view) web = null;
                showFailure();
                return true;
            }
        });
    }

    private void watchLite() {
        liteTries = 0;
        handler.removeCallbacks(liteCheck);
        handler.postDelayed(liteCheck, 2000);
    }

    private void checkLite() {
        if (web == null || failed) return;
        liteTries++;
        web.evaluateJavascript(LITE_CHECK, value -> Log.i("OceanLite", "check " + value));
        if (liteTries < 4) handler.postDelayed(liteCheck, 3000);
    }

    private void arm() {
        if (web == null || script.isEmpty()) return;
        web.evaluateJavascript(script, value -> scheduleSettle());
    }

    private void scheduleSettle() {
        handler.removeCallbacks(settle);
        handler.postDelayed(settle, 600);
        handler.postDelayed(settle, 2000);
        handler.postDelayed(settle, 5500);
    }

    private void settle() {
        if (web == null || userMoved || failed) return;
        int mine = ++ticket;
        web.evaluateJavascript(COLLECT, value -> {
            if (mine != ticket || userMoved || web == null) return;
            TvFocus.Page page = TvFocus.parse(value);
            String id = TvFocus.first(page.boxes);
            if (id == null) return;
            focused = id;
            web.evaluateJavascript(focusCall(id), null);
        });
    }

    private void move(int direction) {
        userMoved = true;
        if (web == null || failed) return;
        int mine = ++ticket;
        web.evaluateJavascript(COLLECT, value -> {
            if (mine != ticket || web == null) return;
            TvFocus.Page page = TvFocus.parse(value);
            TvFocus.Box current = TvFocus.find(page.boxes, focused);
            if (current != null && TvFocus.nudgeRange(current.kind, direction)) {
                int sign = direction == TvFocus.RIGHT ? 1 : -1;
                web.evaluateJavascript(
                        "window.__oceanTv&&window.__oceanTv.nudge('" + TvFocus.jsQuote(focused) + "'," + sign + ")",
                        null);
                return;
            }
            String next = TvFocus.step(page.boxes, focused, direction);
            if (next == null) return;
            focused = next;
            web.evaluateJavascript(focusCall(next), null);
        });
    }

    private void press() {
        userMoved = true;
        if (web == null || failed) {
            load();
            return;
        }
        int mine = ++ticket;
        web.evaluateJavascript(COLLECT, value -> {
            if (mine != ticket || web == null) return;
            TvFocus.Page page = TvFocus.parse(value);
            if (TvFocus.find(page.boxes, focused) == null) focused = TvFocus.first(page.boxes);
            TvFocus.Box box = TvFocus.find(page.boxes, focused);
            if (box == null) return;
            web.evaluateJavascript(focusCall(box.id), null);
            if ("range".equals(box.kind)) return;
            float[] point = TvFocus.viewPoint(
                    (box.left + box.right) / 2f,
                    (box.top + box.bottom) / 2f,
                    page.width,
                    page.height,
                    web.getWidth(),
                    web.getHeight());
            if (point == null) return;
            tap(point[0], point[1]);
            if (Steer.button(box.id)) engageManual();
        });
    }

    /** The manual button stays on. Arrows steer until Back. */
    private void engageManual() {
        steer.driving = true;
        if (web == null) return;
        web.evaluateJavascript("window.__oceanTv&&window.__oceanTv.hideRing()", null);
        web.evaluateJavascript(
                "(function(){return window.__oceanTv&&window.__oceanTv.manualOn()?1:0})()",
                value -> {
                    if (Steer.pageOn(value) || web == null) return;
                    steer.driving = false;
                    web.evaluateJavascript(focusCall(Steer.BUTTON), null);
                });
    }

    private void leaveManual() {
        focused = Steer.BUTTON;
        if (web == null) return;
        web.evaluateJavascript("window.__oceanTv&&window.__oceanTv.leaveManual()", null);
    }

    private void steerKey(String code, boolean down) {
        if (web == null || code == null) return;
        web.evaluateJavascript(
                "window.__oceanTv&&window.__oceanTv.drive('" + code + "'," + (down ? "1" : "0") + ")",
                null);
    }

    private void tap(float x, float y) {
        if (web == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 16, MotionEvent.ACTION_UP, x, y, 0);
        web.dispatchTouchEvent(down);
        web.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private void showFailure() {
        failed = true;
        handler.removeCallbacks(settle);
        message.setText(getString(R.string.page_error));
        message.setVisibility(View.VISIBLE);
    }

    private static String focusCall(String id) {
        return "window.__oceanTv&&window.__oceanTv.focus('" + TvFocus.jsQuote(id) + "')";
    }

    private String readScript() {
        try (InputStream in = getAssets().open("tv-focus.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return "";
        }
    }

    private void hideBars() {
        // This TV throws if insets are asked before the decor view exists.
        if (getWindow().peekDecorView() == null) return;
        try {
            if (Build.VERSION.SDK_INT < 30) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                return;
            }
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) return;
            controller.hide(WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } catch (RuntimeException ignored) {
        }
    }

    static boolean back(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE;
    }

    static Integer direction(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return TvFocus.LEFT;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return TvFocus.RIGHT;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return TvFocus.UP;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return TvFocus.DOWN;
        return null;
    }

    static boolean center(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }
}
