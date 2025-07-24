package com.example.opencv.webwhiteboard;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.app.Activity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.opencv.Constant;
import com.example.opencv.R;
import com.example.opencv.server.LocalFileServer;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class WebWhiteBoardActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;

    private LocalFileServer localFileServer;

    private Uri imageUri;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // 启动本地文件服务器
//        try {
//            // 使用一个不易冲突的端口，比如 8080 或 8686
//            localFileServer = new LocalFileServer(this, 8686);
//            localFileServer.start();
//            android.util.Log.d("LocalServer", "本地服务器已启动在端口: " + localFileServer.getListeningPort());
//        } catch (java.io.IOException e) {
//            e.printStackTrace();
//            android.util.Log.e("LocalServer", "启动本地服务器失败!");
//        }

        // 1. 启用 EdgeToEdge 模式
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_webwhiteboard);

        // 实例化 WebView 并设置为内容视图 (只执行一次)
        webView = findViewById(R.id.webView);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // 获取系统栏（状态栏、导航栏）的 Insets
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 获取键盘（IME）的 Insets
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // 计算底部的 padding
            // 当键盘弹出时，ime.bottom 是键盘高度，通常大于 systemBars.bottom
            // 当键盘收起时，ime.bottom 是 0，我们取 systemBars.bottom 作为导航栏的间距
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);

            // 为根布局设置新的 padding
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);

            // 返回原始 insets，让系统继续处理
            return insets;
        });

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
            @SuppressLint("DefaultLocale")
            @JavascriptInterface
            public String getPlatformSize() {
                return String.format("{\"width\":%d,\"height\":%d}", Constant.PrintWidth, Constant.PrintHeight);
            }

//            @JavascriptInterface
//            public String getImageUri() {
//                // 7.24
//                if (getIntent().getStringExtra("imageUri") == null) ;
//                else {
//                    imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
//                    // 【关键逻辑开始】
//                    // 1. 检查服务器是否正在运行
//                    if (localFileServer != null && localFileServer.isAlive()) {
//
//
//                        // 3. 使用服务器为 Uri 授权，并获取一个本地 URL
//                        String localUrl = localFileServer.authorizeUri(imageUri);
//
//                        android.util.Log.d("JS_EXEC", "传送uri");
//
//                    } else {
//                        android.util.Log.e("JS_EXEC", "本地服务器未运行，无法处理文件。");
//                    }
//                }
//                return "";
//            }


        }, "Android");

        // 设置WebViewClient来处理页面加载完成后的操作
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 传递画布尺寸
                @SuppressLint("DefaultLocale") String canvasSizeJs = String.format(
                    "window.setCanvasSize(%d, %d);", 
                    Constant.PrintWidth,
                    Constant.PrintHeight
                );
                webView.evaluateJavascript(canvasSizeJs, null);
                
//                // 处理图片传递
//                String imagePath = getIntent().getStringExtra("imagePath");
//                if (imagePath != null && !imagePath.isEmpty()) {
//                    try {
//                        File file = new File(imagePath);
//                        if (file.exists()) {
//                            FileInputStream fis = new FileInputStream(file);
//                            byte[] bytes = new byte[(int) file.length()];
//                            fis.read(bytes);
//                            fis.close();
//                            String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
//
//                            // 根据文件扩展名确定MIME类型
//                            String mimeType = "image/png";
//                            String fileName = file.getName().toLowerCase();
//                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
//                                mimeType = "image/jpeg";
//                            } else if (fileName.endsWith(".gif")) {
//                                mimeType = "image/gif";
//                            } else if (fileName.endsWith(".webp")) {
//                                mimeType = "image/webp";
//                            }
//
//                            String dataUrl = "data:" + mimeType + ";base64," + base64;
//                            String jsCode = "window.setWhiteboardImage('" + dataUrl + "');";
//                            webView.evaluateJavascript(jsCode, null);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    // 旧方案：通过base64参数传递（已注释，保留作为备份）
//                    /*
//                    String imageBase64 = getIntent().getStringExtra("imageBase64");
//                    if (imageBase64 != null && !imageBase64.isEmpty()) {
//                        String jsCode = "window.setWhiteboardImage('" + imageBase64 + "');";
//                        webView.evaluateJavascript(jsCode, null);
//                    }
//                    */
//                }
                // 7.24
                if (getIntent().getStringExtra("imageUri") == null) ;
                else {
                    imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
                    // 【关键逻辑开始】
                    // 1. 检查服务器是否正在运行
                    if (localFileServer != null && localFileServer.isAlive()) {


                            // 3. 使用服务器为 Uri 授权，并获取一个本地 URL
                            String localUrl = localFileServer.authorizeUri(imageUri);

                            // 4. 构造将要执行的 JavaScript 语句
                            @SuppressLint("DefaultLocale") String jsCode = String.format(
                                    "window.addImageToCanvas('%s', %d, %d);",
                                    localUrl,
                                    Constant.PlatformWidth,
                                    Constant.PlatformHeight
                            );

                            // 5. 执行 JavaScript
                            webView.evaluateJavascript(jsCode, null);

                            android.util.Log.d("JS_EXEC", "执行JS: " + jsCode);

                    } else {
                        android.util.Log.e("JS_EXEC", "本地服务器未运行，无法处理文件。");
                    }
                }
            }
        });
        
        webView.loadUrl("file:///android_asset/whiteboard/index.html");
    }

    /**
     * 辅助函数：从 Uri 获取图片尺寸，而无需将整个图片加载到内存中。
     * @param uri 图片的 Uri
     * @return 一个包含 [width, height] 的 int 数组
     */
    private int[] getImageDimensions(Uri uri) {
        if (uri == null) return new int[]{0, 0};

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // 只解码边界信息
            BitmapFactory.decodeStream(inputStream, null, options);
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
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

    // 在 onDestroy 方法中，关闭服务器以释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (localFileServer != null) {
//            localFileServer.stop();
//            android.util.Log.d("LocalServer", "本地服务器已关闭。");
//        }
    }
}