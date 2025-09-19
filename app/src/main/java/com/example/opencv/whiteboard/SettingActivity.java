package com.example.opencv.whiteboard;

import android.os.Bundle;
import android.provider.SyncStateContract;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.http.ApiClient;
import com.example.opencv.webwhiteboard.WebWhiteBoardActivity;
import com.example.opencv.Constant;

import android.content.Intent;

public class SettingActivity extends AppCompatActivity {
    private DragBoxView dragBoxView;
    private TextView tvSizeInfo;
    private EditText etSmallWidth, etSmallHeight;
    public Toolbar toolbar;
    private ApiClient apiClient = ApiClient.getInstance();

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
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // 初始化视图
        etSmallWidth = findViewById(R.id.etBigWidth);
        etSmallHeight = findViewById(R.id.etBigHeight);
        toolbar = findViewById(R.id.toolbar);
        tvSizeInfo = findViewById(R.id.tvSizeInfo);
        dragBoxView = findViewById(R.id.dragBoxView);
        dragBoxView.setSmallBoxSize(0, 0);
        setSupportActionBar(toolbar);

        // 设置小框尺寸
        Button btnSetSmall = findViewById(R.id.btnSetBig);
        btnSetSmall.setOnClickListener(v -> {
            try {
                int width = parseInt(etSmallWidth.getText().toString(), 100);
                int height = parseInt(etSmallHeight.getText().toString(), 100);

                if (width <= 0 || height <= 0) {
                    showToast("打印尺寸必须大于0");
                    return;
                }

                // 更新常量
                updateConstants(width, height);
                dragBoxView.setBigBoxSize(width, height);
                updateSizeDisplay();
                showToast("已更新绘图区尺寸");
            } catch (NumberFormatException e) {
                showToast("请输入有效数字");
            }
        });
    }

    private void updateSizeDisplay() {
        String tvSizeText = String.format("画布尺寸(蓝框)：%dx%d",
                dragBoxView.getBigBoxWidth(),
                dragBoxView.getBigBoxHeight());

        tvSizeInfo.setText(tvSizeText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        finish();
    }

    public void editImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, WebWhiteBoardActivity.class);
        startActivity(intent);
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

    private void updateConstants(int width, int height) {
        Constant.PlatformHeight = height;
        Constant.PlatformWidth = width;
    }

    private int parseInt(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(SettingActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}
