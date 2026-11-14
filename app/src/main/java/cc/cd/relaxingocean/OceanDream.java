package cc.cd.relaxingocean;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.util.Locale;

/** Full-screen ocean dream. Any remote key leaves it. */
public class OceanDream extends DreamService {
    private WebView web;
    private String userAgent = "";

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        try {
            web = new WebView(this);
        } catch (Throwable ignored) {
            setContentView(new View(this));
            return;
        }
        prepare(web);
        setContentView(web);
        web.loadUrl(OceanActivity.SITE);
    }

    @Override
    public void onDetachedFromWindow() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDetachedFromWindow();
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
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
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
        });
    }
}
