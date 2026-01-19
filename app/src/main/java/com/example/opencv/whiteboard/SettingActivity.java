package com.example.opencv.whiteboard;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.opencv.BaseActivity;
import com.example.opencv.Constant;
import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.http.ApiClient;
import com.example.opencv.webwhiteboard.WebWhiteBoardActivity;


public class SettingActivity extends BaseActivity implements DragBoxView.PositionListener {
    private DragBoxView dragBoxView;
    private EditText etBigWidth, etBigHeight, etSmallWidth, etSmallHeight, etPosX, etPosY;
    private TextView etSizeInfo, tvSizeInfo, tvPosition;

    public Toolbar toolbar;

    private ApiClient apiClient = ApiClient.getInstance();

    //private static int TARGET_WIDTH=1600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_setting);


        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 初始化视图
        dragBoxView = findViewById(R.id.dragBoxView);
        etBigWidth = findViewById(R.id.etBigWidth);
        etBigHeight = findViewById(R.id.etBigHeight);
        etSmallWidth = findViewById(R.id.etSmallWidth);
        etSmallHeight = findViewById(R.id.etSmallHeight);
        etPosX = findViewById(R.id.etPosX);
        etPosY = findViewById(R.id.etPosY);
        tvPosition = findViewById(R.id.tvPosition);
        tvSizeInfo = findViewById(R.id.tvSizeInfo);
        etSizeInfo = findViewById(R.id.etSizeInfo);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialSize();
        initialInputHint();
        // 设置初始尺寸显示
        updateSizeDisplay();
        dragBoxView.setPositionListener(this);


        // 设置大框尺寸
        Button btnSetBig = findViewById(R.id.btnSetBig);
        // 设置小框尺寸
        Button btnSetSmall = findViewById(R.id.btnSetSmall);

        btnSetBig.setOnClickListener(v -> {
            try {
                int width = parseInt(etBigWidth.getText().toString(), dragBoxView.getBigBoxWidth());
                int height = parseInt(etBigHeight.getText().toString(), dragBoxView.getBigBoxHeight());

                if (width <= 0 || height <= 0) {
                    showToast(getString(R.string.error_platform_positive));
                    return;
                }


                dragBoxView.setSmallBoxSize(width, height);
                dragBoxView.setBigBoxSize(width, height);
                updateSizeDisplay();
                runOnUiThread(this::initialInputHint);
            } catch (NumberFormatException e) {
                showToast(getString(R.string.error_invalid_number));
            }
        });

        btnSetSmall.setOnClickListener(v -> {
            try {
                int width = parseInt(etSmallWidth.getText().toString(), dragBoxView.getSmallBoxWidth());
                int height = parseInt(etSmallHeight.getText().toString(), dragBoxView.getSmallBoxHeight());

                if (width <= 0 || height <= 0) {
                    showToast(getString(R.string.error_small_positive));
                    return;
                }

                if (width > dragBoxView.getBigBoxWidth()) {
                    showToast(getString(R.string.error_small_width_exceed));
                    return;
                }

                if (height > dragBoxView.getBigBoxHeight()) {
                    showToast(getString(R.string.error_small_height_exceed));
                    return;
                }

                dragBoxView.setSmallBoxSize(width, height);
                updateSizeDisplay();
                runOnUiThread(this::initialInputHint);
            } catch (NumberFormatException e) {
                showToast(getString(R.string.error_invalid_number));
            }
        });

        // 设置小框位置
        Button btnSetPos = findViewById(R.id.btnSetPosition);
        btnSetPos.setOnClickListener(v -> {
            float x = parseFloat(etPosX.getText().toString(), Float.parseFloat(etPosX.getHint().toString()));
            float y = parseFloat(etPosY.getText().toString(), Float.parseFloat(etPosY.getHint().toString()));

            float maxX = dragBoxView.getBigBoxWidth() - dragBoxView.getSmallBoxWidth();
            float maxY = dragBoxView.getBigBoxHeight() - dragBoxView.getSmallBoxHeight();

            if (x < 0 || x > maxX) {
                showToast(getString(R.string.error_x_range, maxX));
                return;
            }

            if (y < 0 || y > maxY) {
                showToast(getString(R.string.error_y_range, maxY));
                return;
            }
            dragBoxView.setSmallBoxPosition(x, y);
            updateSizeDisplay();
            runOnUiThread(this::initialInputHint);
        });

        Button saveConstants = findViewById(R.id.saveConstants);
        saveConstants.setOnClickListener(v -> saveConstants());
    }

    @Override
    protected void onDestroy() {
        saveConstants(); // 保存配置
        super.onDestroy(); // 调用父类
    }

    // 加载 Toolbar 菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

//    // 监听 Toolbar 按钮点击事件
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.User_image) {
//            Toast.makeText(this, getString(R.string.community_feature_in_progress), Toast.LENGTH_SHORT).show();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private void saveConstants() {
        getSharedPreferences("config", MODE_PRIVATE)
                .edit()
                .putInt("PlatformWidth", Constant.PlatformWidth)
                .putInt("PlatformHeight", Constant.PlatformHeight)
                .putInt("PrintWidth", Constant.PrintWidth)
                .putInt("PrintHeight", Constant.PrintHeight)
                .putFloat("PrintStartX", (float) Constant.PrintStartX)
                .putFloat("PrintStartY", (float) Constant.PrintStartY)
                .apply(); // 异步保存
        showToast(getString(R.string.toast_params_saved));
    }


    private void initialSize() {
        if (apiClient.isConnected.get() && apiClient.isInfo.get()) {
            int[] Platform = apiClient.machineInfo.getMc().getLimit();
            Constant.PlatformWidth = Platform[0];
            Constant.PlatformHeight = Platform[1];
        }
        dragBoxView.setBigBoxSize(Constant.PlatformWidth, Constant.PlatformHeight);
        dragBoxView.setSmallBoxSize(Constant.PrintWidth, Constant.PrintHeight);
        dragBoxView.setSmallBoxPosition((float) Constant.PrintStartX, (float) Constant.PrintStartY);
    }

    private void initialInputHint() {
        etBigWidth.setHint(String.valueOf(dragBoxView.getBigBoxWidth()));
        etBigHeight.setHint(String.valueOf(dragBoxView.getBigBoxHeight()));

        etSmallWidth.setHint(String.valueOf(dragBoxView.getSmallBoxWidth()));
        etSmallHeight.setHint(String.valueOf(dragBoxView.getSmallBoxHeight()));

        etPosX.setHint(String.valueOf(dragBoxView.getSmallBoxPosX()));
        etPosY.setHint(String.valueOf(dragBoxView.getSmallBoxPosY()));
    }

    // 更新尺寸显示
    private void updateSizeDisplay() {
        saveConstants(); // 保存配置
        String tvSizeText = getString(R.string.size_big_format,
                dragBoxView.getBigBoxWidth(),
                dragBoxView.getBigBoxHeight());

        String etSizeText = getString(R.string.size_small_format,
                dragBoxView.getSmallBoxWidth(),
                dragBoxView.getSmallBoxHeight());

        tvSizeInfo.setText(tvSizeText);
        etSizeInfo.setText(etSizeText);
    }

    // 正确实现接口方法
    @Override
    public void onPositionChanged(float x, float y) {
        Constant.PrintStartX = x;
        Constant.PrintStartY = y;
        runOnUiThread(() -> {
            // 设置 hint
            etPosX.setHint(String.valueOf(x));
            etPosY.setHint(String.valueOf(y));

            // 设置坐标显示
            tvPosition.setText(getString(R.string.position_format, x, y));
        });
    }



    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private float parseFloat(String str, float defaultValue) {
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    public void mainPage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void goBack(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        // 方式1：直接调用返回键逻辑（API 29+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        // 方式2：兼容旧版本
        else {
            if (!isFinishing()) {
                finish();
                // 如果需要带动画
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }
    }

    public void editImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, WebWhiteBoardActivity.class);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}

