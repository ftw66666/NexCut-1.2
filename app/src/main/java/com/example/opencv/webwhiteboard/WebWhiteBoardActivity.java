package com.example.opencv.webwhiteboard;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import com.example.opencv.Constant;

import java.io.File;
import java.io.FileInputStream;

public class WebWhiteBoardActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowContentAccess(true);

        // 支持input type=file文件选择
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*"); // 只允许选择图片
                startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
        });

        // 注入JS接口
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onNextStep(String data) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(WebWhiteBoardActivity.this, com.example.opencv.image.LayerPreviewActivity.class);
                    intent.putExtra("layerData", data);
                    startActivity(intent);
                    // finish();
                });
            }
            @JavascriptInterface
            public String saveTempFile(String base64, String fileName) {
                try {
                    String pureBase64 = base64.contains(",") ? base64.split(",")[1] : base64;
                    byte[] decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                    java.io.File tempFile = new java.io.File(getCacheDir(), fileName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                    fos.write(decodedBytes);
                    fos.close();
                    return tempFile.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }
            // 新增：前端主动请求画布大小
            @JavascriptInterface
            public String getPlatformSize() {
                return String.format("{\"width\":%d,\"height\":%d}", Constant.PlatformWidth, Constant.PlatformHeight);
            }
        }, "Android");

        // 设置WebViewClient来处理页面加载完成后的操作
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 传递画布尺寸
                String canvasSizeJs = String.format(
                    "window.setCanvasSize(%d, %d);", 
                    Constant.PlatformWidth, 
                    Constant.PlatformHeight
                );
                webView.evaluateJavascript(canvasSizeJs, null);
                
                // 处理图片传递
                String imagePath = getIntent().getStringExtra("imagePath");
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        File file = new File(imagePath);
                        if (file.exists()) {
                            FileInputStream fis = new FileInputStream(file);
                            byte[] bytes = new byte[(int) file.length()];
                            fis.read(bytes);
                            fis.close();
                            String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                            
                            // 根据文件扩展名确定MIME类型
                            String mimeType = "image/png";
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                                mimeType = "image/jpeg";
                            } else if (fileName.endsWith(".gif")) {
                                mimeType = "image/gif";
                            } else if (fileName.endsWith(".webp")) {
                                mimeType = "image/webp";
                            }
                            
                            String dataUrl = "data:" + mimeType + ";base64," + base64;
                            String jsCode = "window.setWhiteboardImage('" + dataUrl + "');";
                            webView.evaluateJavascript(jsCode, null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 旧方案：通过base64参数传递（已注释，保留作为备份）
                    /*
                    String imageBase64 = getIntent().getStringExtra("imageBase64");
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        String jsCode = "window.setWhiteboardImage('" + imageBase64 + "');";
                        webView.evaluateJavascript(jsCode, null);
                    }
                    */
                }
            }
        });
        
        webView.loadUrl("file:///android_asset/whiteboard/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }
} 