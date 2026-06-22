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
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
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

import java.io.OutputStream;

/**
 * Aplicativo INSOLO.
 *
 * A interface e o banco permanecem centralizados no site GitHub Pages e no
 * Supabase. O app abre o site dentro de um WebView protegido e oferece duas
 * integrações nativas:
 * - Compartilhar relatório no menu do Android (WhatsApp aparece se instalado);
 * - Salvar PDF usando o seletor de destino do Android.
 */
public class MainActivity extends android.app.Activity {

    private static final String HOME_URL = "https://bgcprojetos.github.io/Insolo/";
    private static final String APP_HOST = "bgcprojetos.github.io";
    private static final String SUPABASE_HOST = "llimgdxhxhmtbiusiyss.supabase.co";
    private static final int CREATE_PDF_REQUEST_CODE = 7401;

    private WebView webView;
    private LinearLayout loadingView;
    private LinearLayout errorView;
    private byte[] pendingPdfBytes;
    private String pendingPdfFileName;

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

        // Necessário para formulário, Supabase, assinatura em canvas e PDF.
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " INSOLO-Android/1.1");

        // O app usa apenas conteúdo HTTPS remoto. Não autorize file:// nem content://.
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

        webView.addJavascriptInterface(new InsoloAndroidBridge(), "InsoloAndroid");
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

    private String safePdfFileName(String suggestedName) {
        String name = suggestedName == null ? "relatorio_insolo.pdf" : suggestedName.trim();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.isEmpty()) name = "relatorio_insolo.pdf";
        if (!name.toLowerCase().endsWith(".pdf")) name += ".pdf";
        return name;
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

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != CREATE_PDF_REQUEST_CODE) return;

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            Toast.makeText(this, "Salvamento do PDF cancelado.", Toast.LENGTH_SHORT).show();
            pendingPdfBytes = null;
            pendingPdfFileName = null;
            return;
        }

        Uri destination = data.getData();
        try (OutputStream output = getContentResolver().openOutputStream(destination)) {
            if (output == null || pendingPdfBytes == null) {
                throw new IllegalStateException("Não foi possível abrir o destino para gravar o PDF.");
            }
            output.write(pendingPdfBytes);
            output.flush();
            Toast.makeText(this, "PDF salvo com sucesso.", Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Não foi possível salvar o PDF.", Toast.LENGTH_LONG).show();
        } finally {
            pendingPdfBytes = null;
            pendingPdfFileName = null;
        }
    }

    /** Ponte exclusiva para as ações iniciadas pelo site INSOLO carregado no WebView. */
    private class InsoloAndroidBridge {
        @JavascriptInterface
        public void shareText(String title, String text) {
            runOnUiThread(() -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, title == null ? "Relatório INSOLO" : title);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
                    startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório com"));
                } catch (Exception exception) {
                    Toast.makeText(MainActivity.this, "Não foi possível abrir as opções de compartilhamento.", Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void savePdf(String base64Pdf, String fileName) {
            runOnUiThread(() -> {
                try {
                    if (base64Pdf == null || base64Pdf.trim().isEmpty()) {
                        throw new IllegalArgumentException("PDF vazio.");
                    }

                    pendingPdfBytes = Base64.decode(base64Pdf.replaceAll("\\s", ""), Base64.DEFAULT);
                    pendingPdfFileName = safePdfFileName(fileName);

                    Intent createDocument = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    createDocument.addCategory(Intent.CATEGORY_OPENABLE);
                    createDocument.setType("application/pdf");
                    createDocument.putExtra(Intent.EXTRA_TITLE, pendingPdfFileName);
                    startActivityForResult(createDocument, CREATE_PDF_REQUEST_CODE);
                } catch (Exception exception) {
                    pendingPdfBytes = null;
                    pendingPdfFileName = null;
                    Toast.makeText(MainActivity.this, "Não foi possível preparar o PDF para salvar.", Toast.LENGTH_LONG).show();
                }
            });
        }
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
                        "Use o botão Baixar relatório (PDF) para escolher onde salvar.",
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
