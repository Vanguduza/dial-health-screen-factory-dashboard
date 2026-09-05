package zw.co.dialhealth.screenfactory;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String ENTRY_URL = "https://vanguduza.github.io/dial-health-screen-factory-dashboard/";
    private static final String OAUTH_SCHEME = "dialhealthscreenfactory";
    private TextView statusText;
    private Button openButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderNativeShell();
        getWindow().getDecorView().postDelayed(() -> {
            Uri callback = getIntent() != null ? getIntent().getData() : null;
            if (isOAuthCallback(callback)) openOAuthResult(callback);
            else openDashboard();
        }, 350);
    }

    private void renderNativeShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(245, 248, 250));
        setContentView(root);

        TextView brand = new TextView(this);
        brand.setText("DIAL HEALTH · DESIGN CONTROL PLANE");
        brand.setTextColor(Color.rgb(15,118,110));
        brand.setTextSize(12);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, fullWidth(dp(10)));

        TextView title = new TextView(this);
        title.setText("Screen Factory");
        title.setTextColor(Color.rgb(16,32,51));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth(dp(12)));

        ProgressBar progress = new ProgressBar(this);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(44), dp(44));
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        pp.topMargin = dp(18);
        root.addView(progress, pp);

        statusText = new TextView(this);
        statusText.setText("Opening the live image pipeline dashboard…");
        statusText.setTextColor(Color.rgb(100,116,139));
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setLineSpacing(0f, 1.18f);
        root.addView(statusText, fullWidth(dp(16)));

        openButton = new Button(this);
        openButton.setText("Open dashboard");
        openButton.setOnClickListener(v -> openDashboard());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(50));
        bp.gravity = Gravity.CENTER_HORIZONTAL;
        bp.topMargin = dp(16);
        root.addView(openButton, bp);
    }

    private LinearLayout.LayoutParams fullWidth(int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = topMargin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isOAuthCallback(Uri uri) {
        return uri != null && OAUTH_SCHEME.equalsIgnoreCase(uri.getScheme()) &&
            "oauth".equalsIgnoreCase(uri.getHost()) && "/google".equals(uri.getPath());
    }

    private void openOAuthResult(Uri uri) {
        Uri.Builder target = Uri.parse(ENTRY_URL).buildUpon();
        String code = uri.getQueryParameter("code");
        String state = uri.getQueryParameter("state");
        String error = uri.getQueryParameter("error");
        if (code != null) target.appendQueryParameter("google_oauth_code", code);
        if (state != null) target.appendQueryParameter("google_oauth_state", state);
        if (error != null) target.appendQueryParameter("google_oauth_error", error);
        launchBrowser(target.build());
    }

    private void openDashboard() {
        launchBrowser(Uri.parse(ENTRY_URL));
    }

    private void launchBrowser(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
            statusText.setText("Dashboard opened in your secure browser. Return here any time to reopen it.");
            openButton.setText("Reopen dashboard");
        } catch (Exception error) {
            statusText.setText("No browser could open the dashboard on this device.");
            openButton.setText("Retry");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri uri = intent != null ? intent.getData() : null;
        if (isOAuthCallback(uri)) openOAuthResult(uri);
        else openDashboard();
    }
}
