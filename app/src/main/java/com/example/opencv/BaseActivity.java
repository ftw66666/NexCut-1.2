package com.example.opencv;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        // 单个Activity创建时初始化语言
        super.attachBaseContext(LanguageManager.initLanguage(newBase));
    }
}