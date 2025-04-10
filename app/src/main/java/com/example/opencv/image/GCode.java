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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.widget.Toast;


public class GCode {
    private static final int MAX_POWER = 255;

    public static String generateGCode0(Mat image, int rho, int targetWidth, int targetHeight, double startX, double startY) {
        int laserPower = 20;
        int padding = 5;

        StringBuilder gcode = new StringBuilder();
//        gcode.append("G0 X0 Y0 F1000\n");
        gcode.append("M4 S0\n");

        // 1. 原图转灰度图
        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        Bitmap grayBitmap = ImageProcessor.toGrayscale(bitmap);
        Mat grayImage = ImageProcessor.bitmapToMat(grayBitmap);
        int originrows = grayImage.rows();
        int origincols = grayImage.cols();
        if (originrows > targetHeight*rho || origincols > targetWidth*rho) {
            rho =Math.max(origincols/targetWidth+1,originrows / targetHeight+1);
        }

        // 2. 根据目标尺寸和 rho 缩放图像
        int cols = (int) (targetWidth * rho);   // 横向像素数
        int rows = (int) (targetHeight * rho);  // 纵向像素数
        Mat resized = new Mat();
        Imgproc.resize(grayImage, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_LINEAR);

        // 3. 生成 GCode（逐像素扫描）
        double pixelWidth = 1.0 / rho;
        double pixelHeight = 1.0 / rho;


        for (int y = 0; y < rows; y++) {
            double realY = startY + targetHeight - y * pixelHeight;
            boolean flag = true;
            boolean isEngraving = false;
            double xStart = -1;

            if (y % 2 == 0) {
                // 偶数行：左 → 右
                for (int x = 0; x < cols; x++) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 128;

                    if (shouldEngrave && !isEngraving) {
                        xStart = x * pixelWidth;
                        if(flag) {
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart - padding + startX, realY));
                        flag = !flag;
                        }
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + startX, realY));
                        isEngraving = true;
                    } else if (!shouldEngrave && isEngraving) {
                        double xEnd = (x - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd + padding + startX, realY));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    double xEnd = (cols - 1) * pixelWidth;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd + padding + startX, realY));
                }

            } else {
                // 奇数行：右 → 左
                for (int x = cols - 1; x >= 0; x--) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 128;

                    if (shouldEngrave && !isEngraving) {
                        xStart = x * pixelWidth;
                        if(flag) {
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + padding + startX, realY));
                            flag = !flag;
                        }
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + startX, realY));
                        isEngraving = true;
                    } else if (!shouldEngrave && isEngraving) {
                        double xEnd = (x + 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd - padding + startX, realY));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    double xEnd = 0;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd - padding + startX, realY));
                }
            }
        }

        gcode.append("M5\n");
        grayImage.release();
        resized.release();
        return gcode.toString();
    }


    public static Mat cropGCode(Mat image, int targetWidth, int targetHeight, float whiteboardAspectRatio) {
        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        // 计算目标宽高比
        float targetAspect = targetWidth / (float) targetHeight;

        // 根据宽高比比较决定裁剪方向
        int cropWidth, cropHeight;
        if (whiteboardAspectRatio > targetAspect) {
            // 水平裁剪：保持高度，调整宽度
            cropHeight = originalHeight;
            cropWidth = (int) (cropHeight * targetAspect);

            // 如果计算宽度超过原图，改为保持宽度
            if (cropWidth > originalWidth) {
                cropWidth = originalWidth;
                cropHeight = (int) (cropWidth / targetAspect);
            }
        } else {
            // 竖直裁剪：保持宽度，调整高度（原始逻辑）
            cropWidth = originalWidth;
            cropHeight = (int) (cropWidth / targetAspect);

            // 如果计算高度超过原图，改为保持高度
            if (cropHeight > originalHeight) {
                cropHeight = originalHeight;
                cropWidth = (int) (cropHeight * targetAspect);
            }
        }

        // 计算居中裁剪位置
        int x1 = (originalWidth - cropWidth) / 2;
        int y1 = (originalHeight - cropHeight) / 2;

        // 执行裁剪
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x1, y1, cropWidth, cropHeight);
        return ImageProcessor.bitmapToMat(croppedBitmap);
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