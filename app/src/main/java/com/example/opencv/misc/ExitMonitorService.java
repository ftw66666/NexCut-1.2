package com.example.opencv.misc;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.http.Control;

public class ExitMonitorService extends Service {

    private Control control = new Control();
    private static final int NOTIFICATION_ID = 2;
    private static final String CHANNEL_ID = "ExitmonitorServiceChannel";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不绑定
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
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
        Log.d("ExitMonitor", "App1 已退出");
        // control.Logout(getApplicationContext(), true);
        Log.d("ExitMonitor", "App 被关闭了，执行清理任务！");
        // TODO: 你的操作，比如清理缓存、上传状态、退出登录等
    }

    private Notification createNotification() {
        createNotificationChannel();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("后台服务运行中")
                .setContentText("正在执行任务...")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ExitMonitor Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
