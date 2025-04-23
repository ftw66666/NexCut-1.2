package com.example.opencv;

import static com.example.opencv.image.GCodeRead.copyNcFilesToStorageAsync;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.opencv.Utils.CenteredImageSpan;
import com.example.opencv.Utils.FileUtils;
import com.example.opencv.databinding.ActivityMainBinding;
import com.example.opencv.device.DeviceActivity;
import com.example.opencv.device.DeviceInfoActivity;
import com.example.opencv.device.InfoService;
import com.example.opencv.device.device_Control;
import com.example.opencv.http.ApiClient;
import com.example.opencv.http.Control;
import com.example.opencv.http.ProgressRequestBody;
import com.example.opencv.image.GCodeFileAdapter;
import com.example.opencv.image.GCodeRead;
import com.example.opencv.image.ImageEditActivity;
import com.example.opencv.misc.AboutActivity;
import com.example.opencv.misc.ExitMonitorService;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.databinding.ActivityMainBinding;
import com.example.opencv.whiteboard.SettingActivity;
import com.example.opencv.whiteboard.WhiteboardActivity;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;
    private String PIC_PATH = Environment.getDataDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
    private static final int PICK_IMAGE = 1;
    public static final int CAPTURE_IMAGE = 2;
    private static final int EDIT_IMAGE = 3;

    public static Uri imageUri;
    public Uri photoUri;
    public static Bitmap bitmap;
    public Toolbar toolbar;

    public static int textColor;
    public static int backgroundColor;

    private ViewPager mViewPager;
    private RadioGroup mRadioGroup;
    private RadioButton tab1, tab2, tab3, tab4;  //四个单选按钮
    private List<View> mViews;   //存放视图

    private ActivityMainBinding binding; // ViewBinding 绑定对象

    private AlertDialog currentDialog;
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    ApiClient apiClient = ApiClient.getInstance();

    Control control = new Control();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        new Thread(() -> FileUtils.clearAppPicturesDir(this)).start();


        textColor = getResources().getColor(R.color.light_black);
        backgroundColor = getResources().getColor(R.color.white);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // **应用启动时复制 .nc 预制文件到可访问目录**
        //GCodeRead.copyNcFilesToStorageAsync(this);
        InitialButtons();
        loadImagesFromAssets();
        requestAppPermissions();
        loadConstants();

        //开启读取信息服务
        Intent startIntent = new Intent(MainActivity.this, InfoService.class);
        MainActivity.this.startForegroundService(startIntent);

//        // 启动监听 Service
//        Intent serviceIntent = new Intent(this, ExitMonitorService.class);
//        startForegroundService(serviceIntent);


        handleImportIntent(getIntent());

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (apiClient.isConnected.get() && apiClient.isInfo.get()) {
                        int deviceState = apiClient.machineInfo.getMc().getRun() & 0xFFFF;
                        runOnUiThread(() -> SetDeviceButton(apiClient.ConnectDeviceId, deviceState));
                    } else {
                        if (Objects.equals(apiClient.ConnectDeviceId, ""))
                            runOnUiThread(() -> SetDeviceButton("NexCut-X1"));
                        else runOnUiThread(() -> SetDeviceButton(apiClient.ConnectDeviceId));
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleImportIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // ⭐ 这句很关键，更新 Activity 持有的 Intent！
    }

    public void onClickDevice(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
        startActivity(intent);
    }

    public void OnClickDeviceInfo(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(MainActivity.this, DeviceInfoActivity.class);
        startActivity(intent);
    }

    public void OnClickDeviceControl(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(MainActivity.this, device_Control.class);
        startActivity(intent);
    }

    public void onClickSetting(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
        startActivity(intent);
    }

    public void editImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(MainActivity.this, WhiteboardActivity.class);
        startActivity(intent);
    }

    public void selectImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent1 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent1, PICK_IMAGE);
    }

    private Uri createPublicImageUri() throws IOException {
        ContentValues values = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + timeStamp + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        return getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );
    }

    public void captureImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //takePictureIntent.addFlags(0);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoUri = createPublicImageUri();
                // 授予 URI 权限
                List<ResolveInfo> resolvedIntentActivities = getPackageManager()
                        .queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                    String packageName = resolvedIntentInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE);
            } catch (IOException ex) {
                Toast.makeText(this, "File creation failed", Toast.LENGTH_SHORT).show();
                ex.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
//        Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(intent1, CAPTURE_IMAGE);
    }

    public void readGCode(View view) {
        // 点击动画
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        // 获取文件列表
        List<File> ncFiles = GCodeRead.getCopiedNcFiles(this);
        // 自定义布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gcode_list, null);
        ListView lvFiles = dialogView.findViewById(R.id.lvFiles);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        ImageButton btnRefresh = dialogView.findViewById(R.id.btnRefresh);

        tvTitle.setText("选择一个加工文件");

        // Adapter
        GCodeFileAdapter adapter = new GCodeFileAdapter(this, ncFiles, () -> {
            if (ncFiles.isEmpty() && currentDialog != null) {
                currentDialog.dismiss();
            }
        });
        lvFiles.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> {

            ncFiles.clear();
            ncFiles.addAll(GCodeRead.getCopiedNcFiles(MainActivity.this));
            adapter.notifyDataSetChanged();
        });

        if (ncFiles.isEmpty()) {
//            Toast.makeText(this, "没有找到已复制的 GCode 文件", Toast.LENGTH_SHORT).show();
//            return;
            // 使用 Activity.this 确保上下文正确，并在主线程更新 UI
            copyNcFilesToStorageAsync(MainActivity.this, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    // 重新获取并刷新列表
                    ncFiles.clear();
                    ncFiles.addAll(GCodeRead.getCopiedNcFiles(MainActivity.this));
                    adapter.notifyDataSetChanged();
                });
            });
        }

        // 构造对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                //.setPositiveButton("分享", null)
                .setNeutralButton("重置", null)
                .setNegativeButton("关闭", null);

        currentDialog = builder.create();
        currentDialog.show();

        // 拿到按钮，设置自定义监听，防止点击默认关闭
        Button btnShare = currentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnReset = currentDialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        btnShare.setOnClickListener(v -> shareGcodeDir());

        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("是否重置为默认文件？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 用户点击“确定”后，先清空目录，再异步拷贝并刷新列表
                        boolean cleared = FileUtils.clearGcodesDir(MainActivity.this);
                        if (!cleared) {
                            Toast.makeText(MainActivity.this, "清空 GCode 目录失败", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 拷贝默认文件并刷新 UI
                        copyNcFilesToStorageAsync(MainActivity.this, () -> {
                            MainActivity.this.runOnUiThread(() -> {
                                ncFiles.clear();
                                ncFiles.addAll(GCodeRead.getCopiedNcFiles(MainActivity.this));
                                adapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "重置完成", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                    .show();
        });

    }

    private void shareGcodeDir() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_FILES);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);


//        File dir = getExternalFilesDir("gcodes");
//        if (dir == null || !dir.exists()) {
//            Toast.makeText(this, "目录不存在", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // 获取目录的 URI
//        Uri uri = Uri.parse(dir.getPath());
//
//        // 创建 Intent 来启动文件浏览器
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, "resource/folder");  // 这里 "resource/folder" 是 MIME 类型，表示目录
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // 授权读取权限
//
//        try {
//            // 启动文件浏览器
//            startActivity(Intent.createChooser(intent, "打开 GCode 文件夹"));
//        } catch (ActivityNotFoundException e) {
//            // 如果没有合适的文件浏览器
//            Toast.makeText(this, "没有文件浏览器可用", Toast.LENGTH_SHORT).show();
//        }
    }



    public void showConfirmationDialog(File selectedFile) {
        Toast.makeText(this, "已选择: " + selectedFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        new AlertDialog.Builder(this)
                .setMessage("是否选择传输: " + selectedFile.getName())
                .setPositiveButton("确定", (dialog, which) -> startFileTransfer(selectedFile))
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("分享", (dialog, which) ->
                {
                    startFileShare(selectedFile);
                    //dialog.dismiss();
                })
                .show();
    }


    private void startFileTransfer(File selectedFile) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                control.FileTransfer(selectedFile, MainActivity.this);
            }
             finally {
                executor.shutdown();
            }
        });
        }

    private void startFileShare(File selectedFile) {
        if (selectedFile == null || !selectedFile.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider",
                selectedFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(shareIntent, "分享文件"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有可用的应用来分享文件", Toast.LENGTH_SHORT).show();
        }
    }


    // 加载 Toolbar 菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // 监听 Toolbar 按钮点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.User_image) {
            Toast.makeText(this, "社区功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_IMAGE && resultCode == RESULT_OK) {
            Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
            startActivity(intent);
        }
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                //bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);

                //imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK) {
//            bitmap = (Bitmap) data.getExtras().get("data");
//            // 将 Bitmap 保存到临时文件并获取其 URI
//            try {
//                File tempFile = createImageFile(); // 创建临时文件
//                FileOutputStream out = new FileOutputStream(tempFile);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//                out.flush();
//                out.close();
//
//                imageUri = Uri.fromFile(tempFile);
//
//                // 传递 URI 给下一个 Activity
//                Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
//                intent.putExtra("imageUri", imageUri.toString());
//                startActivity(intent);
//
//                //imageView.setImageBitmap(bitmap);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // 传递 URI 给下一个 Activity
            Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
            intent.putExtra("imageUri", photoUri.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    // 创建临时图片文件
    private File createImageFile() throws IOException {
        String imageFileName = "PNG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        /* 前缀 */
        /* 后缀 */
        /* 目录 */
        return File.createTempFile(
                imageFileName,  /* 前缀 */
                ".png",         /* 后缀 */
                storageDir      /* 目录 */
        );
    }

    private void requestAppPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.CAMERA
        }, 101);

        // Permission request logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            //requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
            requestPermissions(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            }, 102);
            // Partial access on Android 14 (API level 34) or higher
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            }, 103);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 104);
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestPermissions(new String[]{
//                    android.Manifest.permission.CAMERA,
//                    android.Manifest.permission.READ_MEDIA_IMAGES
//            }, 101);
//        } else {
//            requestPermissions(new String[]{
//                    android.Manifest.permission.CAMERA,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//            }, 101);
//        }

    }

    private void InitialButtons() {
        SpannableString spannable;
        Drawable drawable;
        ImageSpan imageSpan;
        Button button;
        //设备
        SetDeviceButton("NexCut-X1");
        //拍摄
        button = findViewById(R.id.button1);
        drawable = getResources().getDrawable(R.drawable.camera_icon);
        drawable.setBounds(0, 0, 180, 180); // 自定义大小
        imageSpan = new CenteredImageSpan(drawable);
        spannable = new SpannableString(" 拍照");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
        //相册
        button = findViewById(R.id.button2);
        drawable = getResources().getDrawable(R.drawable.gallery_icon);
        drawable.setBounds(0, 0, 180, 180); // 设置大小
        imageSpan = new CenteredImageSpan(drawable);
        spannable = new SpannableString(" " + "相册");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);

//        button.setTextColor(textColor);
//        button.setBackgroundColor(backgroundColor);
        //文件
        button = findViewById(R.id.button3);
        drawable = getResources().getDrawable(R.drawable.file_icon);
        drawable.setBounds(0, 0, 180, 180); // 设置大小
        imageSpan = new CenteredImageSpan(drawable);
        spannable = new SpannableString(" " + "文件");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
        //预设
        //版本
        button = findViewById(R.id.button4);
        drawable = getResources().getDrawable(R.drawable.about_icon);
        drawable.setBounds(0, 0, 180, 180); // 设置大小
        imageSpan = new CenteredImageSpan(drawable);
        spannable = new SpannableString(" " + "关于");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
    }

    public void SetDeviceButton(String deviceName) {
        String buttonText;
        int deviceNameLength = deviceName.length();
        Button button = findViewById(R.id.button0);
        Drawable drawable;
        drawable = getResources().getDrawable(R.drawable.grayspot);
        buttonText = deviceName + "\n 未连接";
        drawable.setBounds(0, 0, 50, 50); // 设置大小
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
        SpannableString spannable = new SpannableString(buttonText);
        spannable.setSpan(imageSpan, deviceNameLength + 1, deviceNameLength + 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
    }

    public void SetDeviceButton(String deviceName, int connectCode) {
        String buttonText;
        int deviceNameLength = deviceName.length();
        Button button = findViewById(R.id.button0);
        Drawable drawable;
        if (connectCode == 0) {
            drawable = getResources().getDrawable(R.drawable.greenspot);
            buttonText = deviceName + "\n 待机中";
        } else if (connectCode == 1) {
            drawable = getResources().getDrawable(R.drawable.greenspot);
            buttonText = deviceName + "\n 加工中";
        } else if (connectCode == 2) {
            drawable = getResources().getDrawable(R.drawable.grayspot);
            buttonText = deviceName + "\n 告警";
        } else {
            drawable = getResources().getDrawable(R.drawable.grayspot);
            buttonText = deviceName + "\n 未知错误";
        }
        drawable.setBounds(0, 0, 50, 50); // 设置大小
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
        SpannableString spannable = new SpannableString(buttonText);
        spannable.setSpan(imageSpan, deviceNameLength + 1, deviceNameLength + 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
    }


    // 通过XML绑定的点击方法
    public void onClickOpenBrowser(View view) {
        String url = "https://www.au3tech.com/page168"; // 替换成你想跳转的网址


        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addCategory(Intent.CATEGORY_BROWSABLE); // 可选，明确用于浏览器

        // 启动浏览器
        try {
            view.getContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(view.getContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show();
        }
    }

    public void openFileWithSAF(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 设置文件类型过滤
        intent.setType("image/*");  // 只允许选择图片文件
        // 可选：指定MIME类型数组
        // String[] mimeTypes = {"image/*", "application/pdf"};
        // intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
    }

    public void appAbout(View view) {
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    private void handleImportIntent(Intent intent) {
        String fileName = intent.getStringExtra("imported_file_name");
        if (fileName != null) {
            Toast.makeText(this, "已导入文件：" + fileName, Toast.LENGTH_LONG).show();
            intent.removeExtra("imported_file_name"); // 防止重复显示
        }
    }

    private void loadConstants() {
        var sp = getSharedPreferences("config", MODE_PRIVATE);
        Constant.PlatformWidth = sp.getInt("PlatformWidth", Constant.PlatformWidth);
        Constant.PlatformHeight = sp.getInt("PlatformHeight", Constant.PlatformHeight);
        Constant.PrintWidth = sp.getInt("PrintWidth", Constant.PrintWidth);
        Constant.PrintHeight = sp.getInt("PrintHeight", Constant.PrintHeight);
        Constant.PrintStartX = (double) sp.getFloat("PrintStartX", (float) Constant.PrintStartX);
        Constant.PrintStartY = (double) sp.getFloat("PrintStartY", (float) Constant.PrintStartY);
    }

    private void loadImagesFromAssets() {
        LinearLayout container = findViewById(R.id.image_container);

        try {
            String[] fileList = getAssets().list("showimage"); // 文件夹名称
            if (fileList != null) {
                for (String fileName : fileList) {
                    String assetPath = "showimage/" + fileName;



                    ImageView imageView = new ImageView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            dpToPx(120), dpToPx(120)); // 缩小尺寸
                    params.setMargins(dpToPx(8), 0, dpToPx(8), 0);
                    imageView.setLayoutParams(params);

                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setAdjustViewBounds(true);
                    imageView.setTag(copyAssetToCache(assetPath));
                    //String uri = copyAssetToCache(assetPath);

                    // 加载图片
                    Picasso.get()
                            .load("file:///android_asset/" + assetPath)
                            .placeholder(R.drawable.placeholder) // 可选：加载中图标
                            .error(R.drawable.error)             // 可选：失败图标
                            .into(imageView);



                    // 点击事件
                    imageView.setOnClickListener(v -> {
                        String uri = (String) v.getTag();
                        if(uri != null) onImageClick(uri); // 传给你的函数
                    });

                    // 添加到容器
                    container.addView(imageView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void onImageClick(String uri) {

        //Toast.makeText(this, "点击图片：" + uri, Toast.LENGTH_SHORT).show();
        // TODO: 你可以在这里打开预览、下载、设置背景等
        Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
        intent.putExtra("imageUri", uri);
        startActivity(intent);
    }

    private String copyAssetToCache(String assetPath) {
        File cacheFile = new File(getCacheDir(), new File(assetPath).getName());
        try (InputStream is = getAssets().open(assetPath);
             FileOutputStream os = new FileOutputStream(cacheFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            return FileProvider.getUriForFile(this, getPackageName() + ".provider", cacheFile).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    @Override
    protected void onDestroy() {
        control.Logout(MainActivity.this,false);
        super.onDestroy();
        // 执行你需要的方法
    }
}
// add 多线程其他函数的