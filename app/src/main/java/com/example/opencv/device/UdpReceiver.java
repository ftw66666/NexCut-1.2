package com.example.opencv.device;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpReceiver {
    private static final String TAG = "UdpReceiver";
    private DatagramSocket socket;
    private final int port;
    private final Handler handler;
    private final OnDeviceReceivedListener listener;
    private final ExecutorService executorService;

    public UdpReceiver(int port, Handler handler, OnDeviceReceivedListener listener) {
        this.port = port;
        this.handler = handler;
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startReceiving() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new DatagramSocket(port);
                    byte[] buffer = new byte[4096];
                    while (true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength());
                        Log.d(TAG, "收到数据: " + data);
                        // 解析数据
                        String[] parts = data.split("&");
                        if (parts.length == 4) {
                            String ip = parts[0];
                            int port = Integer.parseInt(parts[1]);
                            String model = parts[2];
                            String deviceId = parts[3];
                            Device device = new Device(ip, port, model, deviceId);
                            // 通知UI
                            if (listener != null) {
                                handler.post(() -> listener.onDeviceReceived(device));
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "接收失败", e);
                } finally {
                    closeSocket();
                }
            }
        });
    }

    public void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                Log.d(TAG, "关闭DatagramSocket");
            } catch (Exception e) {
                Log.e(TAG, "关闭端口异常", e);
            }
        }
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public interface OnDeviceReceivedListener {
        void onDeviceReceived(Device device);
    }
}