package com.example.opencv;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.opencv.webwhiteboard.WebWhiteBoardActivity;
import com.example.opencv.whiteboard.SettingActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        // 单个Activity创建时初始化语言
        super.attachBaseContext(LanguageManager.initLanguage(newBase));
    }

    /**
     * 执行点击缩放动画
     *
     * @param view 被点击的视图
     */
    public void animateClick(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);
    }

    /**
     * 带动画的页面跳转
     *
     * @param view           触发跳转的视图
     * @param targetActivity 目标Activity类
     */
    public void navigateTo(View view, Class<?> targetActivity) {
        animateClick(view);
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }

    /**
     * 通用的编辑图片方法 (跳转到 WebWhiteBoardActivity)
     *
     * @param view 触发视图
     */
    public void editImage(View view) {
        if (!Constant.IsOfficial) {
            Toast.makeText(this, R.string.NotOfficialError, Toast.LENGTH_SHORT).show();
            // dialog.dismiss(); // 根据你的流程考虑是否关闭对话框
            return; // 如果不是官方设备，阻止后续处理
        }
        navigateTo(view, WebWhiteBoardActivity.class);
    }

    /**
     * 通用的跳转到设置页面方法
     *
     * @param view 触发视图
     */
    public void onClickSetting(View view) {
        navigateTo(view, SettingActivity.class);
    }

    /**
     * 通用的返回主页方法
     *
     * @param view 触发视图
     */
    public void mainPage(View view) {
        animateClick(view);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * 通用的返回上一页方法
     *
     * @param view 触发视图
     */
    public void goBack(View view) {
        animateClick(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getOnBackPressedDispatcher().onBackPressed();
        } else {
            if (!isFinishing()) {
                finish();
            }
        }
    }

}
