package zw.co.dialhealth.screenfactory;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String ENTRY_URL = "https://vanguduza.github.io/dial-health-screen-factory-dashboard/";
    private static final String OAUTH_SCHEME = "dialhealthscreenfactory";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        if (!handleOAuthIntent(getIntent())) webView.loadUrl(ENTRY_URL);
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
        });
        webView.setDownloadListener(createDownloadListener());
    }

    private boolean routeUri(Uri uri) {
        if (uri == null) return false;
        if (OAUTH_SCHEME.equalsIgnoreCase(uri.getScheme())) return handleOAuthUri(uri);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (host.endsWith("accounts.google.com") || host.endsWith("google.com") || host.endsWith("cloud.google.com")) {
            try {
                Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browser);
                return true;
            } catch (Exception ignored) {
                Toast.makeText(this, "A browser is required for Google authorization", Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
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
        webView.loadUrl(target.build().toString());
        return true;
    }

    private DownloadListener createDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setTitle(filename);
                request.setDescription("Dial Health Screen Factory download");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
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
