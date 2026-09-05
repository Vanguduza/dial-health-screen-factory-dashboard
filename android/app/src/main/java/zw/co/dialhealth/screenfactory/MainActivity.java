package zw.co.dialhealth.screenfactory;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String ENTRY_URL = "https://vanguduza.github.io/dial-health-screen-factory-dashboard/";
    private static final String OAUTH_SCHEME = "dialhealthscreenfactory";
    private static final String APP_UA = " DialHealthScreenFactory/1.2";

    private FrameLayout root;
    private LinearLayout bootPanel;
    private TextView statusText;
    private Button retryButton;
    private Button browserButton;
    private WebView webView;
    private boolean firstPageVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderNativeBootShell();
        // Do not initialize WebView inside the launch-critical onCreate path.
        // Some Android/WebView providers can take many seconds to cold-start; if we
        // block here the OS appears to the user as if the app never opened. Render
        // the native boot shell first, then initialize the dashboard WebView.
        root.postDelayed(this::createWebViewSafely, 180);
    }

    private void renderNativeBootShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(245, 248, 250));
        setContentView(root);

        bootPanel = new LinearLayout(this);
        bootPanel.setOrientation(LinearLayout.VERTICAL);
        bootPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(28);
        bootPanel.setPadding(pad, pad, pad, pad);
        bootPanel.setBackgroundColor(Color.rgb(245, 248, 250));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(bootPanel, panelParams);

        TextView brand = new TextView(this);
        brand.setText("DIAL HEALTH · DESIGN CONTROL PLANE");
        brand.setTextColor(Color.rgb(15,118,110));
        brand.setTextSize(12);
        brand.setGravity(Gravity.CENTER);
        bootPanel.addView(brand, fullWidth(dp(12)));

        TextView title = new TextView(this);
        title.setText("Screen Factory");
        title.setTextColor(Color.rgb(16,32,51));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        bootPanel.addView(title, fullWidth(dp(8)));

        ProgressBar progress = new ProgressBar(this);
        bootPanel.addView(progress, centered(dp(44), dp(44), dp(18)));

        statusText = new TextView(this);
        statusText.setText("Starting the live image pipeline dashboard…");
        statusText.setTextColor(Color.rgb(100,116,139));
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setLineSpacing(0f, 1.18f);
        bootPanel.addView(statusText, fullWidth(dp(16)));

        retryButton = new Button(this);
        retryButton.setText("Retry dashboard");
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> loadEntry());
        bootPanel.addView(retryButton, centered(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48), dp(10)));

        browserButton = new Button(this);
        browserButton.setText("Open in browser");
        browserButton.setVisibility(View.GONE);
        browserButton.setOnClickListener(v -> openExternal(Uri.parse(ENTRY_URL)));
        bootPanel.addView(browserButton, centered(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48), dp(6)));
    }

    private LinearLayout.LayoutParams fullWidth(int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = topMargin;
        return p;
    }

    private LinearLayout.LayoutParams centered(int width, int height, int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.topMargin = topMargin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void createWebViewSafely() {
        try {
            webView = new WebView(this);
            webView.setVisibility(View.INVISIBLE);
            root.addView(webView, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            configureWebView();
            if (!handleOAuthIntent(getIntent())) loadEntry();
        } catch (Throwable error) {
            showFatalWebViewFallback(error);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUserAgentString(settings.getUserAgentString() + APP_UA);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUri(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                firstPageVisible = true;
                webView.setVisibility(View.VISIBLE);
                bootPanel.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    showLoadFailure("Dashboard could not be reached. Check your connection and retry.");
                }
            }
        });
        webView.setDownloadListener(createDownloadListener());
    }

    private void loadEntry() {
        retryButton.setVisibility(View.GONE);
        browserButton.setVisibility(View.GONE);
        bootPanel.setVisibility(View.VISIBLE);
        statusText.setText("Connecting to the live image pipeline dashboard…");
        firstPageVisible = false;
        if (webView != null) {
            webView.setVisibility(View.INVISIBLE);
            webView.loadUrl(ENTRY_URL);
        }
    }

    private void showLoadFailure(String message) {
        if (firstPageVisible) return;
        statusText.setText(message);
        retryButton.setVisibility(View.VISIBLE);
        browserButton.setVisibility(View.VISIBLE);
        bootPanel.setVisibility(View.VISIBLE);
    }

    private void showFatalWebViewFallback(Throwable error) {
        statusText.setText("Android WebView could not start on this device. The same dashboard can still be opened in your browser.");
        browserButton.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }

    private boolean routeUri(Uri uri) {
        if (uri == null) return false;
        if (OAUTH_SCHEME.equalsIgnoreCase(uri.getScheme())) return handleOAuthUri(uri);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (!("http".equals(scheme) || "https".equals(scheme))) return openExternal(uri);
        if (host.equals("accounts.google.com") || host.endsWith(".google.com") || host.equals("cloud.google.com")) return openExternal(uri);
        return false;
    }

    private boolean openExternal(Uri uri) {
        try {
            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browser);
            return true;
        } catch (Exception ignored) {
            Toast.makeText(this, "No browser is available for this link", Toast.LENGTH_LONG).show();
            return true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOAuthIntent(intent);
    }

    private boolean handleOAuthIntent(Intent intent) {
        return intent != null && handleOAuthUri(intent.getData());
    }

    private boolean handleOAuthUri(Uri uri) {
        if (uri == null || !OAUTH_SCHEME.equalsIgnoreCase(uri.getScheme())) return false;
        if (!"oauth".equalsIgnoreCase(uri.getHost()) || !"/google".equals(uri.getPath())) return false;
        Uri.Builder target = Uri.parse(ENTRY_URL).buildUpon();
        String code = uri.getQueryParameter("code");
        String state = uri.getQueryParameter("state");
        String error = uri.getQueryParameter("error");
        if (code != null) target.appendQueryParameter("google_oauth_code", code);
        if (state != null) target.appendQueryParameter("google_oauth_state", state);
        if (error != null) target.appendQueryParameter("google_oauth_error", error);
        if (webView != null) {
            bootPanel.setVisibility(View.VISIBLE);
            statusText.setText("Completing Google Drive authorization…");
            webView.loadUrl(target.build().toString());
        }
        return true;
    }

    private DownloadListener createDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setTitle(filename);
                request.setDescription("Dial Health Screen Factory download");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (manager != null) manager.enqueue(request);
                Toast.makeText(this, "Downloading " + filename, Toast.LENGTH_SHORT).show();
            } catch (Exception error) {
                Toast.makeText(this, "Download could not start", Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
