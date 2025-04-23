package com.example.opencv.device;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.opencv.R;
import com.example.opencv.http.ApiClient;
import com.example.opencv.http.Control;
import com.example.opencv.modbus.ModbusTCPClient;

public class InfoService extends Service {
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    ApiClient apiClient = ApiClient.getInstance();

    Control control = new Control();
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "InfoDeviceServiceChannel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // onCreate()方法会在服务创建的时候调用
    @Override
    public void onCreate() {
        super.onCreate();
    }

    // onStartCommand()方法会在每次服务启动的时候调用
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, flags, startId);
        startForeground(NOTIFICATION_ID, createNotification());
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    control.GetMachineInfo(getApplicationContext());
                }
            }

        }).start();
        return START_STICKY;
    }

    // onDestroy()方法会在服务销毁的时候 调用
    @Override
    public void onDestroy() {
        super.onDestroy();
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
                    "InfoDevice Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
