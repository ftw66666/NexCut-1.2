//package com.example.opencv.image;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.graphics.Bitmap;
//import org.opencv.core.Mat;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import android.widget.Toast;
//
package com.example.opencv.image;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.widget.Toast;


public class GCode {
    private static final int MAX_POWER = 255;

    public static String generateGCode0(Mat image, double rho, int targetWidth, int targetHeight) {
        int laserPower = 20;
        StringBuilder gcode = new StringBuilder();
        gcode.append("G0 X0 Y0 F1000\n");
        gcode.append("M4 S0\n");

        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        Bitmap grayImageBit = ImageProcessor.toGrayscale(bitmap);
        Mat grayImage = ImageProcessor.bitmapToMat(grayImageBit);
        int rows = grayImage.rows();
        int cols = grayImage.cols();

        double width_mm = targetWidth;
        double height_mm = targetHeight;
        double pixelWidth = width_mm / cols;
        double yStep = 1.0 / rho;

        int totalYSteps = (int) (height_mm * rho);

        for (int step = 0; step < totalYSteps; step++) {
            double currentY = step * yStep;
            double flippedY = height_mm - currentY; // 翻转Y轴，图像从下往上雕刻
            int yPixel = (int) (currentY / height_mm * rows);
            if (yPixel >= rows) yPixel = rows - 1;

            boolean isEngraving = false;
            double xStart = -1;

            if (step % 2 == 0) {
                // 偶数行：从左到右
                for (int x = 0; x < cols; x++) {
                    double grayValue = grayImage.get(yPixel, x)[0];
                    boolean shouldEngrave = grayValue < 128;

                    if (shouldEngrave) {
                        if (!isEngraving) {
                            xStart = x * pixelWidth;
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart, flippedY));
                            isEngraving = true;
                        }
                    } else if (isEngraving) {
                        double xEnd = (x - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, flippedY, laserPower));
                        isEngraving = false;
                    }
                }

                if (isEngraving) {
                    double xEnd = (cols - 1) * pixelWidth;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, flippedY, laserPower));
                }

            } else {
                // 奇数行：从右到左
                for (int x = cols - 1; x >= 0; x--) {
                    double grayValue = grayImage.get(yPixel, x)[0];
                    boolean shouldEngrave = grayValue < 128;

                    if (shouldEngrave) {
                        if (!isEngraving) {
                            xStart = x * pixelWidth;
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart, flippedY));
                            isEngraving = true;
                        }
                    } else if (isEngraving) {
                        double xEnd = (x + 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, flippedY, laserPower));
                        isEngraving = false;
                    }
                }

                if (isEngraving) {
                    double xEnd = 0;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd, flippedY, laserPower));
                }
            }
        }

        gcode.append("M5\n");
        grayImage.release();
        return gcode.toString();
    }

    public static Mat cropGCode (Mat image, int targetWidth, int targetHeight) {
        Bitmap bitmap = ImageProcessor.matToBitmap(image);

        // 仅裁剪不缩放
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        // 计算裁剪区域（保持目标宽高比）
        int cropWidth = originalWidth;
        int cropHeight = (int)(cropWidth * targetHeight / (double)targetWidth);
        if (cropHeight > originalHeight) {
            cropHeight = originalHeight;
            cropWidth = (int)(cropHeight * targetWidth / (double)targetHeight);
        }

        int x1 = (originalWidth - cropWidth) / 2;
        int y1 = (originalHeight - cropHeight) / 2;
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x1, y1, cropWidth, cropHeight);

        Mat croppedMat = ImageProcessor.bitmapToMat(croppedBitmap);
        return croppedMat;
    }

    // 保存方法保持不变
    public static void saveGCodeToFile(String gcode, Context context, String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "默认";
            }
            fileName = fileName.replaceAll("[/\\\\:*?\"<>|]", "");
            if (!fileName.endsWith(".nc")) {
                fileName += ".nc";
            }

            File file = new File(context.getExternalFilesDir(null), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(gcode.getBytes());
            fos.close();

            showSuccessDialog(context, file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "文件保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static void showSuccessDialog(Context context, String filePath) {
        new AlertDialog.Builder(context)
                .setTitle("保存成功")
                .setMessage("GCode 文件已生成：\n" + filePath)
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, Context context, String s) {
        File file = new File(context.getExternalFilesDir(null), s);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // 保存位图为JPEG格式
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}