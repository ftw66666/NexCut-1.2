package com.example.opencv;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.example.opencv.Utils.FileUtils;
import com.example.opencv.device.DeviceActivity;
import com.example.opencv.device.DeviceInfoActivity;
import com.example.opencv.device.InfoService;
import com.example.opencv.device.device_Control;
import com.example.opencv.image.GCodeRead;
import com.example.opencv.image.ImageEditActivity;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.databinding.ActivityMainBinding;
import com.example.opencv.modbus.NettyModbusTCPClient;
import com.example.opencv.whiteboard.SettingActivity;
import com.example.opencv.whiteboard.WhiteboardActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private String PIC_PATH = Environment.getDataDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
    private static final int PICK_IMAGE = 1;
    public static final int CAPTURE_IMAGE = 2;
    private static final int EDIT_IMAGE = 3;
    //private ImageView imageView;
    private String currentPhotoPath;
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
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        new Thread(() -> FileUtils.clearDir(this)).start();


        textColor = getResources().getColor(R.color.light_black);
        backgroundColor = getResources().getColor(R.color.white);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // **应用启动时复制 .nc 预制文件到可访问目录**
        GCodeRead.copyNcFilesToStorage(this);
        InitialButtons();
        requestAppPermissions();
        //开启读取信息服务
        Intent startIntent = new Intent(MainActivity.this, InfoService.class);
        MainActivity.this.startForegroundService(startIntent);

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mtcp.connect("10.84.164.63", 4002, 1, MainActivity.this);
                } catch (NettyModbusTCPClient.ModbusException e) {
                    Log.d("TCPtest", e.getMessage());
                }
            }
        }).start();*/

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
                    if (!mtcp.isConnected.get()) {
                        if (Objects.equals(mtcp.ConnectDeviceId, ""))
                            runOnUiThread(() -> SetDeviceButton("NexCut-X1"));
                        else runOnUiThread(() -> SetDeviceButton(mtcp.ConnectDeviceId));
                    } else if (!mtcp.deviceInfo.isEmpty()) {
                        int deviceState = mtcp.deviceInfo.get(8) & 0xFFFF;
                        runOnUiThread(() -> SetDeviceButton(mtcp.ConnectDeviceId, deviceState));
                    }
                }
            }
        }).start();
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
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);
        List<File> ncFiles = GCodeRead.getCopiedNcFiles(this);
        if (ncFiles.isEmpty()) {
            Toast.makeText(this, "没有找到已复制的 GCode 文件", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择一个 GCode 文件");

        String[] fileArray = new String[ncFiles.size()];
        for (int i = 0; i < ncFiles.size(); i++) {
            fileArray[i] = ncFiles.get(i).getName();
        }

        builder.setItems(fileArray, (dialog, which) -> {
            File selectedFile = ncFiles.get(which);
            Toast.makeText(this, "已选择: " + selectedFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        });

        builder.setItems(fileArray, (dialog, which) -> {
            File selectedFile = ncFiles.get(which);
            // 创建第二个AlertDialog
            AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            secondDialogBuilder.setMessage("是否选择传输: " + selectedFile.getName());
            secondDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mtcp.FileTransport(0, selectedFile, MainActivity.this);
                            } catch (ModbusTCPClient.ModbusException e) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mtcp.onFileFailed(MainActivity.this);
                                    }
                                });
                                Log.d("TCPTest", e.getMessage());
                            }
                        }
                    }).start();
                }
            });
            secondDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            // 显示第二个对话框
            secondDialogBuilder.show();
        });

        builder.show();
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
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);

                //imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
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
        drawable.setBounds(0, 0, 80, 80); // 设置大小
        imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        spannable = new SpannableString(" " + "拍照");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
        //相册
        button = findViewById(R.id.button2);
        drawable = getResources().getDrawable(R.drawable.gallery_icon);
        drawable.setBounds(0, 0, 80, 80); // 设置大小
        imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        spannable = new SpannableString(" " + "相册");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
        //预设
        button = findViewById(R.id.button3);
        drawable = getResources().getDrawable(R.drawable.pics_icon);
        drawable.setBounds(0, 0, 80, 80); // 设置大小
        imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        spannable = new SpannableString(" " + "素材");
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        button.setText(spannable);
//        button.setTextColor(textColor);
//        button.setBackgroundColor(backgroundColor);
        //文件
        button = findViewById(R.id.button4);
        drawable = getResources().getDrawable(R.drawable.file_icon);
        drawable.setBounds(0, 0, 80, 80); // 设置大小
        imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        spannable = new SpannableString(" " + "文件");
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


}
// add 多线程其他函数的