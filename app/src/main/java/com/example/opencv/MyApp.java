package com.example.opencv;

import android.app.Application;
import android.content.Context;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LanguageManager.initLanguage(this); // 初始化语言配置
    }

    @Override
    protected void attachBaseContext(Context base) {
        // 替换全局Context，确保所有页面语言生效
        super.attachBaseContext(LanguageManager.initLanguage(base));
    }
}