package com.example.opencv.webwhiteboard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.opencv.BaseActivity;
import com.example.opencv.Constant;
import com.example.opencv.LanguageManager;
import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.http.Control;
import com.example.opencv.webwhiteboard.WebLanguageBridge;

import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.content.Context;
import android.app.DownloadManager;
import android.os.Build;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class WebWhiteBoardActivity extends BaseActivity {
    private WebView webView;
    private String languageCode;
    private WebLanguageBridge languageBridge;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private final static int CAMERA_PERMISSION_REQUEST_CODE = 10001;
    private final static int STORAGE_PERMISSION_REQUEST_CODE = 10002;

    // 自定义下载路径，默认为 Downloads 文件夹
    private String customDownloadPath = Environment.DIRECTORY_DOWNLOADS;
    private static boolean isFirstLaunchInProcess = true;
    private boolean shouldClearLocalStorageOnLoad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --- 核心修改部分 2：根据静态标志位来决定是否清空 ---
        // --- 结束核心修改部分 2 ---
        // 1. 启用 EdgeToEdge 模式
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_webwhiteboard);

        // 开启WebView调试（方便排查问题）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        View rootLayout = findViewById(R.id.main);
        LinearLayout rgTab = findViewById(R.id.rg_tab);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 检查软键盘（IME）是否可见
            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            // 3. 根据键盘可见性，更新 rg_tab 的可见状态
            if (isKeyboardVisible) {
                // 键盘弹出了，隐藏 rg_tab
                rgTab.setVisibility(View.GONE);
            } else {
                // 键盘收起了，显示 rg_tab
                rgTab.setVisibility(View.VISIBLE);
            }

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


        // 检查当前进程中是否是首次启动
        if (isFirstLaunchInProcess) {
            // 如果是，则设置清空标志
            shouldClearLocalStorageOnLoad = true;

            // 立即将静态标志位设为 false，这样当前进程中后续再打开本 Activity 就不会清空了
            isFirstLaunchInProcess = false;

            Log.d("WebWhiteBoardActivity", "应用进程首次启动，将在页面加载后清空 LocalStorage。");
        } else {
            Log.d("WebWhiteBoardActivity", "在同一进程中再次启动，不执行清空操作。");
        }

        // 设置自定义下载路径为 gcodes 文件夹
        File gcodesDir = getExternalFilesDir("gcodes");
        if (gcodesDir != null) {
            customDownloadPath = gcodesDir.getAbsolutePath();
        }

        // 实例化 WebView 并设置为内容视图 (只执行一次)
        webView = findViewById(R.id.webView);
//        webView = new WebView(this);
//        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowContentAccess(true);

        // 启用摄像头和麦克风权限
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        languageCode = LanguageManager.getSelectedLanguageCode(this);
        languageBridge = new WebLanguageBridge(languageCode);
        webView.addJavascriptInterface(languageBridge, "AndroidLanguage");

        // 设置下载监听器
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                // 检查是否是 blob URL
                if (url.startsWith("blob:")) {
                    // 处理 blob URL
                    handleBlobDownload(url, contentDisposition, mimeType);
                    return;
                }

                // 检查存储权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(WebWhiteBoardActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(WebWhiteBoardActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                STORAGE_PERMISSION_REQUEST_CODE);
                        return;
                    }
                }

                // 开始下载
                startDownload(url, contentDisposition, mimeType);
            }
        });

        // 支持input type=file文件选择 + 拦截JS弹窗逻辑
        webView.setWebChromeClient(new WebChromeClient() {
            // ========== 原有功能：文件选择 ==========
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

            // ========== 原有功能：摄像头权限请求 ==========
            @Override
            public void onPermissionRequest(android.webkit.PermissionRequest request) {
                String[] resources = request.getResources();
                for (String resource : resources) {
                    if (resource.equals(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        // 检查摄像头权限
                        if (ContextCompat.checkSelfPermission(WebWhiteBoardActivity.this,
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            // 请求摄像头权限
                            ActivityCompat.requestPermissions(WebWhiteBoardActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    CAMERA_PERMISSION_REQUEST_CODE);
                        } else {
                            // 权限已授予，允许访问
                            request.grant(request.getResources());
                        }
                        return;
                    }
                }
                // 其他权限请求
                request.grant(request.getResources());
            }

            // ========== 核心：拦截原生JS Alert弹窗（最高优先级） ==========
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d("JsAlertInterceptor", "拦截到原生alert: " + message);

                // 1. 翻译弹窗文本
                String translatedMsg = languageBridge.translate(message);
                Log.d("JsAlertInterceptor", "翻译后文本: " + translatedMsg);

                // 2. 显示Android原生弹窗（替代WebView的alert）
                new AlertDialog.Builder(WebWhiteBoardActivity.this)
                        .setMessage(translatedMsg)
                        .setPositiveButton("OK", (dialog, which) -> {
                            result.confirm(); // 必须调用，否则JS会阻塞
                            dialog.dismiss();
                        })
                        .setCancelable(false) // 禁止点击外部关闭，和JS Alert行为一致
                        .show();

                // 返回true表示拦截，不再执行原生alert
                return true;
            }

            // ========== 拦截原生JS Confirm弹窗（可选） ==========
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                String translatedMsg = languageBridge.translate(message);

                new AlertDialog.Builder(WebWhiteBoardActivity.this)
                        .setMessage(translatedMsg)
                        .setPositiveButton("OK", (dialog, which) -> {
                            result.confirm();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            result.cancel();
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();

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

            // 新增：设置自定义下载路径
            @JavascriptInterface
            public void setDownloadPath(String path) {
                runOnUiThread(() -> {
                    customDownloadPath = path;
                    Toast.makeText(WebWhiteBoardActivity.this, "下载路径已设置为: " + path, Toast.LENGTH_SHORT).show();
                });
            }

            // 新增：获取当前下载路径
            @JavascriptInterface
            public String getDownloadPath() {
                return customDownloadPath;
            }

            // 新增：保存 blob 文件
            @JavascriptInterface
            public void saveBlobFile(String base64, String fileName, String mimeType) {
                runOnUiThread(() -> {
                    try {
                        // 解码 base64 数据
                        byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);

                        // 创建目标文件
                        File targetFile;
                        if (customDownloadPath.startsWith("/")) {
                            // 绝对路径
                            File downloadDir = new File(customDownloadPath);
                            if (!downloadDir.exists()) {
                                downloadDir.mkdirs();
                            }
                            targetFile = new File(downloadDir, fileName);
                        } else {
                            // 相对路径（相对于外部存储）
                            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(customDownloadPath), fileName);
                            targetFile = downloadDir;
                        }

                        // 写入文件0417
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
                        fos.write(decodedBytes);
                        fos.close();

                        // --- 核心修改：保存成功后直接调用分享 ---
                        showConfirmationDialog(targetFile);

                        Toast.makeText(WebWhiteBoardActivity.this,
                                R.string.File_has_been_saved + fileName + R.string.to + customDownloadPath,
                                Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(WebWhiteBoardActivity.this,
                                R.string.Failed_to_save_file + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "Android");

        // 设置WebViewClient来处理页面加载完成后的操作
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                /// 9.18
                // 检查是否需要清除 localStorage
                if (shouldClearLocalStorageOnLoad) {
                    // 执行清除操作
                    view.evaluateJavascript("localStorage.clear();", null);
                    // 重置标志位，防止在页面内部跳转时重复清除
                    shouldClearLocalStorageOnLoad = false;

                    // 打印日志以便调试
                    android.util.Log.d("WebViewDebug", "LocalStorage cleared on page finished.");
                }

                applyWebLanguage(view);

                // ========== 兜底：再次覆盖原生alert（防止页面加载后被重写） ==========
                String reOverrideAlertScript = "(function(){" +
                        "  const originAlert = window.alert;" +
                        "  window.alert = function(msg) {" +
                        "    console.log('JS层拦截alert:', msg);" +
                        "    // 先尝试翻译，再调用原方法（备用方案）" +
                        "    let translated = msg;" +
                        "    try {" +
                        "      translated = window.AndroidLanguage.translate(msg);" +
                        "    } catch(e) {}" +
                        "    originAlert(translated);" +
                        "  };" +
                        "})();";

                view.evaluateJavascript(reOverrideAlertScript, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d("JsAlertInterceptor", "兜底alert覆盖脚本执行完成: " + value);
                    }
                });

                // 处理图片传递
                String imagePath = getIntent().getStringExtra("imagePath");
                String vectorIMAGE = getIntent().getStringExtra("vectorIMAGE");
                //Uri imageUri = getIntent().getParcelableExtra("imageUri");

                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        File file;
                        if (imagePath.startsWith("content://")) {
                            // 通过ContentResolver读取并保存为临时文件
                            Uri uri = Uri.parse(imagePath);
                            InputStream is = getContentResolver().openInputStream(uri);
                            file = new File(getCacheDir(), "temp_image.jpg");
                            OutputStream fos = new java.io.FileOutputStream(file);
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            is.close();
                        } else {
                            if (imagePath.startsWith("file://")) {
                                imagePath = imagePath.substring(7);
                            }
                            file = new File(imagePath);
                        }
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
                            String safeJsArg = JSONObject.quote(dataUrl);
                            String jsCode = "window.setHomePageImage(" + safeJsArg + ");";
                            webView.evaluateJavascript(jsCode, null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (vectorIMAGE != null && !vectorIMAGE.isEmpty()) {
                    try {
                        File file;
                        if (vectorIMAGE.startsWith("content://")) {
                            Uri uri = Uri.parse(vectorIMAGE);
                            InputStream is = getContentResolver().openInputStream(uri);
                            // 读取为字符串
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                baos.write(buffer, 0, len);
                            }
                            is.close();
                            String content = new String(baos.toByteArray(), "UTF-8");

                            // 尝试从 uri 推断扩展名
                            String ext = "svg";
                            String path = uri.getPath();
                            if (path != null) {
                                String lower = path.toLowerCase();
                                if (lower.endsWith(".dxf")) ext = "dxf";
                                else if (lower.endsWith(".plt")) ext = "plt";
                                else if (lower.endsWith(".svg")) ext = "svg";
                            }
                            String temp = JSONObject.quote(content);
                            String js = "window.setWhiteboardVector(" + JSONObject.quote(content) + ", '" + ext + "')";
                            webView.evaluateJavascript(js, null);
                        } else {
                            if (vectorIMAGE.startsWith("file://")) {
                                vectorIMAGE = vectorIMAGE.substring(7);
                            }
                            file = new File(vectorIMAGE);
                            if (file.exists()) {
                                FileInputStream fis = new FileInputStream(file);
                                byte[] bytes = new byte[(int) file.length()];
                                int read = fis.read(bytes);
                                fis.close();
                                String content = new String(bytes, "UTF-8");

                                String ext = "svg";
                                String fileName = file.getName().toLowerCase();
                                if (fileName.endsWith(".dxf")) ext = "dxf";
                                else if (fileName.endsWith(".plt")) ext = "plt";
                                else if (fileName.endsWith(".svg")) ext = "svg";

                                String js = "window.setWhiteboardVector(" + JSONObject.quote(content) + ", '" + ext + "')";
                                webView.evaluateJavascript(js, null);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 当既没有位图也没有矢量数据时，调用首页接口但不传递任何数据
//                    String jsCode = "window.setHomePageImage();";
//                    webView.evaluateJavascript(jsCode, null);

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

        // ========== 核心修改：提前注入alert覆盖脚本，再加载页面 ==========
        // 1. 定义提前覆盖alert的脚本
        String preOverrideAlertScript = "(function(){" +
                "  // 页面加载初期立即覆盖alert" +
                "  const originAlert = window.alert;" +
                "  window.alert = function(msg) {" +
                "    console.log('提前拦截alert:', msg);" +
                "    // 直接调用Android的onJsAlert（优先级最高）" +
                "    originAlert(msg);" +
                "  };" +
                "  console.log('alert已提前覆盖');" +
                "})();";

        // 2. 先注入脚本，再加载页面
        webView.evaluateJavascript(preOverrideAlertScript, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("JsAlertInterceptor", "提前alert覆盖脚本执行完成: " + value);

                // 注入完成后加载页面
                String startImagePath = getIntent().getStringExtra("imagePath");
                String startVector = getIntent().getStringExtra("vectorIMAGE");
                String startImageBase64 = getIntent().getStringExtra("imageBase64");
                boolean hasData = (startImagePath != null && !startImagePath.isEmpty())
                        || (startVector != null && !startVector.isEmpty())
                        || (startImageBase64 != null && !startImageBase64.isEmpty());
                String startUrl = hasData
                        ? "file:///android_asset/whiteboard/index.html#/"
                        : "file:///android_asset/whiteboard/index.html#/whiteboard";

                webView.loadUrl(startUrl);
            }
        });
    }

    /**
     * 设置自定义下载路径
     *
     * @param path 下载路径，可以是相对路径或绝对路径
     */
    public void setCustomDownloadPath(String path) {
        this.customDownloadPath = path;
    }

    /**
     * 获取当前下载路径
     *
     * @return 当前下载路径
     */
    public String getCustomDownloadPath() {
        return this.customDownloadPath;
    }

    /**
     * 开始下载文件
     */
    private void startDownload(String url, String contentDisposition, String mimeType) {
        try {
            // 获取文件名
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

            // 创建下载请求
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("下载文件");
            request.setDescription("正在下载: " + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // 根据自定义路径设置下载位置
            if (customDownloadPath.startsWith("/")) {
                // 绝对路径
                File downloadDir = new File(customDownloadPath);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
                request.setDestinationUri(Uri.fromFile(new File(downloadDir, fileName)));
            } else {
                // 相对路径（相对于外部存储）
                request.setDestinationInExternalPublicDir(customDownloadPath, fileName);
            }

            request.setMimeType(mimeType);

            // 获取下载管理器
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                long downloadId = downloadManager.enqueue(request);
                Toast.makeText(this, "开始下载: " + fileName + " 到 " + customDownloadPath, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "下载管理器不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理 blob URL 下载
     */
    private void handleBlobDownload(String url, String contentDisposition, String mimeType) {
        try {
            // 获取文件名
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

            // 通过 JavaScript 获取 blob 数据
            String jsCode = String.format(
                    "(function() {" +
                            "  var xhr = new XMLHttpRequest();" +
                            "  xhr.open('GET', '%s', false);" +
                            "  xhr.responseType = 'blob';" +
                            "  xhr.send();" +
                            "  var reader = new FileReader();" +
                            "  reader.onload = function() {" +
                            "    var base64 = reader.result.split(',')[1];" +
                            "    Android.saveBlobFile(base64, '%s', '%s');" +
                            "  };" +
                            "  reader.readAsDataURL(xhr.response);" +
                            "})();", url, fileName, mimeType);

            webView.evaluateJavascript(jsCode, null);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 摄像头权限已授予
                Toast.makeText(this, getString(R.string.toast_camera_permission_granted), Toast.LENGTH_SHORT).show();
                // 重新加载页面以启用摄像头功能
                webView.reload();
            } else {
                // 摄像头权限被拒绝
                Toast.makeText(this, getString(R.string.toast_camera_permission_denied), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 存储权限已授予
                Toast.makeText(this, getString(R.string.toast_storage_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                // 存储权限被拒绝
                Toast.makeText(this, getString(R.string.toast_storage_permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void applyWebLanguage(WebView view) {
        if (view == null) {
            return;
        }
        String script =
                "(function(){try{"
                        + "if(typeof AndroidLanguage==='undefined'){return;}"
                        + "var lang=AndroidLanguage.getLanguage();"
                        + "if(lang){document.documentElement.setAttribute('lang',lang);}"
                        + "if(lang!=='en'){return;}"
                        + "var translations={};"
                        + "try{translations=JSON.parse(AndroidLanguage.getTranslations()||'{}');}catch(e){translations={};}"
                        + "var keys=Object.keys(translations).sort(function(a,b){return b.length-a.length;});"
                        + "var translateNative=function(value){if(!value){return value;}try{var nativeResult=AndroidLanguage.translate(value);if(nativeResult){return nativeResult;}}catch(e){}return value;};"
                        + "var replaceText=function(value){if(!value){return value;}var nativeCandidate=translateNative(value);if(nativeCandidate!==value){return nativeCandidate;}var result=value;"
                        + "keys.forEach(function(key){if(!key){return;}if(result.indexOf(key)!==-1){var replacement=translations[key];"
                        + "if(typeof replacement==='string'){result=result.split(key).join(replacement);}}});"
                        + "return result;};"
                        + "var apply=function(){"
                        + "if(!document.body){return;}"
                        + "var walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT,null,false);"
                        + "var nodes=[];while(walker.nextNode()){nodes.push(walker.currentNode);}"
                        + "nodes.forEach(function(node){if(!node||!node.nodeValue){return;}var updated=replaceText(node.nodeValue);"
                        + "if(updated!==node.nodeValue){node.nodeValue=updated;}});"
                        + "['placeholder','title','aria-label','value'].forEach(function(attr){"
                        + "Array.prototype.slice.call(document.querySelectorAll('['+attr+']')).forEach(function(el){"
                        + "var val=el.getAttribute(attr);if(!val){return;}var updated=replaceText(val);"
                        + "if(updated!==val){el.setAttribute(attr,updated);}});"
                        + "});"
                        + "Array.prototype.slice.call(document.querySelectorAll('option,button')).forEach(function(el){"
                        + "var text=el.textContent;if(!text){return;}var updated=replaceText(text);"
                        + "if(updated!==text){el.textContent=updated;}"
                        + "});"
                        + "};"
                        + "apply();"
                        + "if(!window.__langObserver){"
                        + "var observer=new MutationObserver(function(){apply();});"
                        + "observer.observe(document.body,{childList:true,subtree:true,characterData:true});"
                        + "window.__langObserver=observer;"
                        + "}"
                        + "}catch(err){console.error('apply language failed',err);}})();";
        view.evaluateJavascript(script, null);
    }
    Control control = new Control();
    private void startFileTransfer(File selectedFile) {
        // 开启线程执行传输，防止阻塞 UI
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                control.FileTransfer(selectedFile, WebWhiteBoardActivity.this);
            } finally {
                executor.shutdown();
            }
        });
    }
    public void showConfirmationDialog(File selectedFile) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.transmit_decide) + " " + selectedFile.getName())
                .setPositiveButton(R.string.confirm, (dialog, which) -> startFileTransfer(selectedFile))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setNeutralButton(R.string.share, (dialog, which) -> {
                    startFileShare(selectedFile);
                })
                .show();
    }

    private void startFileShare(File selectedFile) {
        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(this,
                getPackageName() + ".provider",
                selectedFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(shareIntent, "分享文件"));
        } catch (Exception e) {
            Toast.makeText(this, R.string.No_available_apps_to_share_the_file, Toast.LENGTH_SHORT).show();
        }
    }
}