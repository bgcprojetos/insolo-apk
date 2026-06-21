package br.com.bgcprojetos.insolo;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Aplicativo INSOLO.
 *
 * A interface e o banco permanecem centralizados no site GitHub Pages e no
 * Supabase. O app apenas abre esse site dentro de um WebView protegido, para
 * que APK e navegador trabalhem com os mesmos registros em tempo real.
 */
public class MainActivity extends android.app.Activity {

    private static final String HOME_URL = "https://bgcprojetos.github.io/Insolo/";
    private static final String APP_HOST = "bgcprojetos.github.io";
    private static final String SUPABASE_HOST = "llimgdzhxhmtbiusiyyss.supabase.co";

    private WebView webView;
    private LinearLayout loadingView;
    private LinearLayout errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(44, 62, 80));
        getWindow().setNavigationBarColor(Color.rgb(245, 245, 245));
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.insoloWebView);
        loadingView = findViewById(R.id.loadingView);
        errorView = findViewById(R.id.errorView);
        Button retryButton = findViewById(R.id.retryButton);

        configureWebView();
        retryButton.setOnClickListener(v -> loadHomePage());

        if (savedInstanceState == null) {
            loadHomePage();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();

        // Necessário para o formulário, Supabase, assinatura em canvas e PDF.
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " INSOLO-Android/1.0");

        // O app usa somente conteúdo HTTPS remoto. Não autorize file:// nem content://.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        webView.setWebViewClient(new InsoloWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress >= 95) {
                    hideLoading();
                }
            }
        });

        webView.setDownloadListener(new InsoloDownloadListener());
    }

    private void loadHomePage() {
        errorView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(HOME_URL);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void showConnectionError() {
        loadingView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    private boolean isTrustedInternalUri(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost();
        return APP_HOST.equalsIgnoreCase(host) || SUPABASE_HOST.equalsIgnoreCase(host);
    }

    private void openExternally(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Não há aplicativo para abrir este link.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    private class InsoloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();
            if ("blob".equalsIgnoreCase(scheme)
                    || "data".equalsIgnoreCase(scheme)
                    || "about".equalsIgnoreCase(scheme)
                    || isTrustedInternalUri(uri)) {
                return false;
            }
            openExternally(uri);
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if ("blob".equalsIgnoreCase(scheme)
                    || "data".equalsIgnoreCase(scheme)
                    || "about".equalsIgnoreCase(scheme)
                    || isTrustedInternalUri(uri)) {
                return false;
            }
            openExternally(uri);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideLoading();
        }

        @Override
        public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
        ) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showConnectionError();
            }
        }

        @Override
        public void onReceivedHttpError(
                WebView view,
                WebResourceRequest request,
                WebResourceResponse errorResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                showConnectionError();
            }
        }
    }

    private class InsoloDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimeType,
                long contentLength
        ) {
            if (url == null || url.startsWith("blob:")) {
                Toast.makeText(
                        MainActivity.this,
                        "Para salvar este PDF, use o botão de compartilhamento do relatório.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            try {
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(fileName);
                request.setDescription("Baixando relatório INSOLO");
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                );
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (manager != null) {
                    manager.enqueue(request);
                    Toast.makeText(MainActivity.this, "Download iniciado.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception exception) {
                Toast.makeText(
                        MainActivity.this,
                        "Não foi possível iniciar o download.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
