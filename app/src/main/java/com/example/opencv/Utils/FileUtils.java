package com.example.opencv.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FileUtils {
    /**
     * 清空应用专属的Pictures目录
     * @param context 上下文对象
     * @return 是否成功清空（true表示成功）
     */
    public static boolean clearAppPicturesDir(Context context) {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null || !storageDir.exists()) return true;

        try {
            // 遍历删除所有子文件/目录
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecursively(file);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("FileUtils", "清空目录失败", e);
            return false;
        }
    }


    public static boolean clearDir(Context context) {
        File storageDir = context.getExternalFilesDir(null);
        //File storageDir = context.getExternalFilesDir(null);
        if (storageDir == null || !storageDir.exists()) return true;

        try {
            // 遍历删除所有子文件/目录
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecursively(file);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("FileUtils", "清空目录失败", e);
            return false;
        }
    }

    public static boolean clearGcodesDir(Context context) {
        File storageDir = context.getExternalFilesDir("/gcodes");
        //File storageDir = context.getExternalFilesDir(null);
        if (storageDir == null || !storageDir.exists()) return true;

        try {
            // 遍历删除所有子文件/目录
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecursively(file);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("FileUtils", "清空目录失败", e);
            return false;
        }
    }

    /**
     * 递归删除文件/目录
     * @param file 要删除的文件或目录
     */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}