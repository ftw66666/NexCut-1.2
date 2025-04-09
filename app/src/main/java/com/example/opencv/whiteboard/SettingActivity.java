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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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


public class SettingActivity extends AppCompatActivity implements DragBoxView.PositionListener{
    private DragBoxView dragBoxView;
    private EditText etBigWidth, etBigHeight, etSmallWidth, etSmallHeight, etPosX, etPosY;
    private TextView tvSizeInfo, tvPosition;
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

        initialSize();
        initialInputHint();
        // 设置初始尺寸显示
        updateSizeDisplay();
        dragBoxView.setPositionListener(this);


        // 设置大框尺寸
        Button btnSetBig = findViewById(R.id.btnSetBig);
        btnSetBig.setOnClickListener(v -> {
            try {
                int width = parseInt(etBigWidth.getText().toString(), dragBoxView.getBigBoxWidth());
                int height = parseInt(etBigHeight.getText().toString(), dragBoxView.getBigBoxHeight());

                if (width <= 0 || height <= 0) {
                    showToast("平台尺寸必须大于0");
                    return;
                }

                dragBoxView.setSmallBoxSize(Math.min(width, dragBoxView.getSmallBoxWidth()),Math.min(height,dragBoxView.getSmallBoxHeight()));
                dragBoxView.setBigBoxSize(width, height);
                updateSizeDisplay();
                runOnUiThread(this::initialInputHint);
            } catch (NumberFormatException e) {
                showToast("请输入有效数字");
            }
        });

        // 设置小框尺寸
        Button btnSetSmall = findViewById(R.id.btnSetSmall);
        btnSetSmall.setOnClickListener(v -> {
            try {
                int width = parseInt(etSmallWidth.getText().toString(), dragBoxView.getSmallBoxWidth());
                int height = parseInt(etSmallHeight.getText().toString(), dragBoxView.getSmallBoxHeight());

                if(width <= 0 || height <= 0){
                    showToast("打印尺寸必须大于0");
                    return;
                }

                if(width > dragBoxView.getBigBoxWidth()){
                    showToast("打印宽度不能超过平台宽度");
                    return;
                }

                if(height > dragBoxView.getBigBoxHeight()){
                    showToast("打印长度不能超过平台长度");
                    return;
                }

                dragBoxView.setSmallBoxSize(width, height);
                updateSizeDisplay();
                runOnUiThread(this::initialInputHint);
            } catch (NumberFormatException e) {
                showToast("请输入有效数字");
            }
        });

        // 设置小框位置
        Button btnSetPos = findViewById(R.id.btnSetPosition);
        btnSetPos.setOnClickListener(v -> {
            float x = parseFloat(etPosX.getText().toString(), Float.parseFloat(etPosX.getHint().toString()));
            float y = parseFloat(etPosY.getText().toString(), Float.parseFloat(etPosY.getHint().toString()));

            float maxX = dragBoxView.getBigBoxWidth() - dragBoxView.getSmallBoxWidth();
            float maxY = dragBoxView.getBigBoxHeight() - dragBoxView.getSmallBoxHeight();

            if(x < 0 || x > maxX){
                showToast("X坐标需在0~" + maxX + "之间");
                return;
            }

            if(y < 0 || y > maxY){
                showToast("Y坐标需在0~" + maxY + "之间");
                return;
            }
            dragBoxView.setSmallBoxPosition(x, y);
            updateSizeDisplay();
            runOnUiThread(this::initialInputHint);
        });
    }

    private void initialSize() {
        dragBoxView.setBigBoxSize(Constant.PlatformWidth, Constant.PlatformHeight);
        dragBoxView.setSmallBoxSize(Constant.Printwidth, Constant.Printheight);
        dragBoxView.setSmallBoxPosition((Constant.PlatformWidth - Constant.Printwidth) / 2f, (Constant.PlatformHeight - Constant.Printheight) / 2f);
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
        String sizeText = String.format("打印平台大小：%dx%d 打印区域大小：%dx%d",
                dragBoxView.getBigBoxWidth(),
                dragBoxView.getBigBoxHeight(),
                dragBoxView.getSmallBoxWidth(),
                dragBoxView.getSmallBoxHeight());
        tvSizeInfo.setText(sizeText);
    }

    // 正确实现接口方法
    @Override
    public void onPositionChanged(float x, float y) {
        runOnUiThread(() -> tvPosition.setText(
                String.format("实时坐标：X=%.1f, Y=%.1f", x, y)
        ));
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}

