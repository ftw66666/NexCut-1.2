package com.example.opencv.image;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GCodeRead {

    private static final String TAG = "GCodeRead";

    /**
     * 获取 `res/raw` 目录下 `.nc` 文件的资源 ID
     *
     * @param context 上下文
     * @return `.nc` 资源 ID 列表
     */
    public static List<Integer> getNcResourceIds(Context context) {
        List<Integer> resourceIds = new ArrayList<>();
        Resources res = context.getResources();
        String packageName = context.getPackageName();

        try {
            // 获取 R.raw 类的所有字段
            Field[] fields = Class.forName(packageName + ".R$raw").getFields();
            for (Field field : fields) {
                int resId = field.getInt(null);
                resourceIds.add(resId);
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return resourceIds;
    }


    /**
     * 复制 `res/raw` 目录下的 `.nc` 文件到可访问目录
     *
     * @param context 上下文
     */
    public static void copyNcFilesToStorage(Context context) {
        List<Integer> ncResourceIds = getNcResourceIds(context);
        File destDir = context.getExternalFilesDir(null);

        if (ncResourceIds.isEmpty()) {
            Toast.makeText(context, "没有找到 GCode 预置文件", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int resId : ncResourceIds) {
            String fileName = context.getResources().getResourceEntryName(resId) + ".enc"; // 获取文件名
            File destFile = new File(destDir, fileName);

            if (!destFile.exists()) { // 避免重复复制
                try (InputStream is = context.getResources().openRawResource(resId);
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    Log.d(TAG, "已复制: " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "复制失败: " + e.getMessage());
                }
            }
        }
    }

    public static void copyNcFilesToStorageAsync(final Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Integer> ncResourceIds = getNcResourceIds(context);
            File destDir = context.getExternalFilesDir(null);

            if (ncResourceIds.isEmpty()) {
                // 如果需要在 UI 中提示，可以通过 Handler 或 runOnUiThread 来切换线程回主线程
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "没有找到 GCode 预置文件", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            for (int resId : ncResourceIds) {
                String fileName = context.getResources().getResourceEntryName(resId) + ".enc"; // 获取文件名
                File destFile = new File(destDir, fileName);

                if (!destFile.exists()) { // 避免重复复制
                    try (InputStream is = context.getResources().openRawResource(resId);
                         FileOutputStream fos = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                        Log.d(TAG, "已复制: " + destFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "复制失败: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 获取已复制到 `files` 目录的 `.nc` 文件列表
     *
     * @param context 上下文
     * @return `.nc` 文件列表
     */
    public static List<File> getCopiedNcFiles(Context context) {
        List<File> ncFiles = new ArrayList<>();
        File dir = context.getExternalFilesDir(null);
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".enc") || file.getName().endsWith(".nc"))
                        ncFiles.add(file);
                }
            }
        }
        return ncFiles;
    }

    public static void copyAFileToStorageAsync(Uri uri, Context context) {

            File destDir = context.getExternalFilesDir(null);
            File destFile = new File(destDir,"");

            if (!destFile.exists()) { // 避免重复复制
                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    Log.d(TAG, "已复制: " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "复制失败: " + e.getMessage());
                }
            }
    }
}
