package com.example.opencv.misc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ExitMonitorService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不绑定
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 可选：设置前台服务以防止被系统杀死
        Log.d("ExitMonitor", "服务已启动");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // 🔥 用户划掉 App 或从最近任务关闭时会调用这里
        doSomethingOnAppExit();
        super.onTaskRemoved(rootIntent);


    }

    private void doSomethingOnAppExit() {
        Log.d("ExitMonitor", "App 被关闭了，执行清理任务！");
        // TODO: 你的操作，比如清理缓存、上传状态、退出登录等
    }
}
