package com.example.opencv.whiteboard;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.opencv.Constant;
import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.Utils.ProgressBarUtils;
import com.example.opencv.image.ImageEditActivity;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class WhiteboardActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE = 2;
    private ArrayList<String> mSelectPath;
    private WhiteBoardFragment whiteBoardFragment;

    private Bitmap bitmap;
    private Uri imageUri;

    private FrameLayout whiteboardlayout;

    public Toolbar toolbar;

    public Handler handler;
    public ProgressBarUtils progressHelper;

    private static final int TARGET_WIDTH = 1920;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_whiteboard);
        whiteBoardFragment = WhiteBoardFragment.newInstance();

        try {
            whiteBoardFragment.setPhotoTextFile(createImageFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        whiteBoardFragment.setPrinterAspectRatio(Constant.PrintWidth / (float) Constant.PrintHeight);
        whiteBoardFragment.setPlatformWidth(Constant.PrintWidth);
        whiteBoardFragment.setPlatformHeight(Constant.PrintHeight);
        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        handler = new Handler(Looper.getMainLooper());
        progressHelper = new ProgressBarUtils();

        FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
        ts.add(R.id.fl_main, whiteBoardFragment, "wb").commitNow();


        whiteboardlayout = findViewById(R.id.fl_main); // whiteboardlayout
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        whiteBoardFragment.setOnFragmentReadyListener(() -> {
            InitialWhiteboard();  // 确保 UI 加载完成后再调用
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteBoardFragment.setPrinterAspectRatio(Constant.PrintWidth / (float) Constant.PrintHeight);
        whiteBoardFragment.setPlatformWidth(Constant.PrintWidth);
        whiteBoardFragment.setPlatformHeight(Constant.PrintHeight);
        whiteBoardFragment.refreshCornerBorder();
    }

    public void setPrinterAspectRatio(float printerAspectRatio) {
        whiteBoardFragment.setPrinterAspectRatio(printerAspectRatio);
    }

    public float getPrinterAspectRatio(){
        return whiteBoardFragment.getPrinterAspectRatio();
    }

    private void InitialWhiteboard() {
        if (getIntent().getStringExtra("imageUri") == null) ;
        else {
            imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));

            try {
                whiteboardlayout.post(new Runnable() {
                    @Override
                    public void run() {
                        whiteBoardFragment.addPhotoByPath(
                                imageUri.getPath());
                                //imageUri.getPath(), whiteboardlayout.getWidth(), whiteboardlayout.getHeight());
                    }
                });
                //whiteBoardFragment.setCurBackgroundByPath(imageUri.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

//    @Override
//    public void onBackPressed() {
////        whiteBoardFragment.setCurBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_035725.png");
////        whiteBoardFragment.setNewBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_035725.png");
////        whiteBoardFragment.setNewBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_041513.png");
////         File f= whiteBoardFragment.saveInOI(WhiteBoardFragment.FILE_PATH, "ss");
////
////        whiteBoardFragment.addPhotoByPath(f.toString());
////        whiteBoardFragment.setCurBackgroundByPath("/storage/emulated/0/YingHe/sketchPhoto/2016-06-21_04151  3.png");
//        super.onBackPressed();
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_HOME);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }

    public void mainPage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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

    public void generateGCode(View view)
    {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        showHalftoneDialog(this);
        //bitmap = whiteBoardFragment.getResultBitmap();
//        bitmap = resizeBitmapByWidth(whiteBoardFragment.getResultBitmap(), Constant.PlatformWidth);
//        try {
//            File tempFile = createImageFile(); // 创建临时文件
//            FileOutputStream out = new FileOutputStream(tempFile);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//            out.flush();
//            out.close();
//
//            imageUri = Uri.fromFile(tempFile);
//
//            // 传递 URI 给下一个 Activity
//            Intent intent = new Intent(this, ImageEditActivity.class);
//            intent.putExtra("GCodeimageUri", imageUri.toString());
//            intent.putExtra("printerAspectRatio", getPrinterAspectRatio());
//            startActivity(intent);
//
//            //imageView.setImageBitmap(bitmap);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void onClickSetting(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void imageEditActivityGCode(boolean isHalftone,int rho,int laserPower)
    {
        bitmap = whiteBoardFragment.getResultBitmap();
        //bitmap = resizeBitmapByWidth(whiteBoardFragment.getResultBitmap(), TARGET_WIDTH);
        try {
            File tempFile = createImageFile(); // 创建临时文件
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            imageUri = Uri.fromFile(tempFile);

            // 传递 URI 给下一个 Activity
            Intent intent = new Intent(this, ImageEditActivity.class);
            intent.putExtra("GCodeimageUri", imageUri.toString());
            intent.putExtra("isHalftone",isHalftone);
            intent.putExtra("whiteboardAspectRatio", getPrinterAspectRatio());
            intent.putExtra("rho",rho);
            intent.putExtra("laserPower",laserPower);
            startActivity(intent);

            //imageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Bitmap resizeBitmapByWidth(Bitmap originalBitmap, int targetWidth) {
        // 获取原始尺寸
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // 计算缩放比例
        float scale = (float) targetWidth / originalWidth;

        // 计算新高度（保持长宽比）
        int targetHeight = Math.round(originalHeight * scale);

        // 使用 Matrix 进行缩放
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // 创建新的 Bitmap
        return Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalWidth,
                originalHeight,
                matrix,
                true
        );
    }


    public void showHalftoneDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("GCode选项");

        // 创建一个垂直方向的 LinearLayout 作为容器
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (context.getResources().getDisplayMetrics().density * 20); // 20dp padding
        layout.setPadding(padding, padding, padding, padding);

        // 创建 CheckBox
        CheckBox halftoneCheckbox = new CheckBox(context);
        halftoneCheckbox.setText("启用半调网屏");
        layout.addView(halftoneCheckbox);

        // 添加 "线密度大小：" 标签
        TextView lineDensityLabel = new TextView(context);
        lineDensityLabel.setText("线密度大小(>0)：");
        layout.addView(lineDensityLabel);

        // 创建 EditText 用于输入线密度
        EditText lineDensityInput = new EditText(context);
        lineDensityInput.setHint("6");
        lineDensityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(lineDensityInput);

        // 添加 "激光功率大小：" 标签
        TextView laserPowerLabel = new TextView(context);
        laserPowerLabel.setText("激光功率大小(>0)：");
        layout.addView(laserPowerLabel);

        // 创建 EditText 用于输入线密度
        EditText laserPowerInput = new EditText(context);
        laserPowerInput.setHint("20");
        laserPowerInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(laserPowerInput);


        builder.setView(layout);

        builder.setPositiveButton("确认", (dialog, which) -> {
            if(!Constant.IsOfficial)
            {
                Toast.makeText(this, "请先连接至NexCut官方设备", Toast.LENGTH_SHORT).show();
                //dialog.dismiss();
            }
            else {
                boolean useHalftone = halftoneCheckbox.isChecked();
                String lineDensityInputText = lineDensityInput.getText().toString().trim();
                String laserPowerInputText = laserPowerInput.getText().toString().trim();

                int lineDensity = (lineDensityInputText.isEmpty() || Integer.parseInt(lineDensityInputText) <= 0) ? 6 : Integer.parseInt(lineDensityInputText); // 默认0，如果未输入
                int laserPower = (laserPowerInputText.isEmpty() || Integer.parseInt(laserPowerInputText) <= 0) ? 20 : Integer.parseInt(laserPowerInputText);
                //Toast.makeText(this, "rho , laserPower = " + lineDensity + " " + laserPower, Toast.LENGTH_SHORT).show();

                // 调用你的处理函数
                imageEditActivityGCode(useHalftone, lineDensity, laserPower);
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.show();
    }



}

