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

    public static String generateGCode0(Mat image, double rho, int targetWidth, int targetHeight,double startX,double startY) {
        int laserPower = 20;
        int padding = 5;
        StringBuilder gcode = new StringBuilder();
        gcode.append("G0 X0 Y0 F1000\n");
        gcode.append("M4 S0\n");

        // 1. 原图转灰度图
        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        Bitmap grayBitmap = ImageProcessor.toGrayscale(bitmap);
        Mat grayImage = ImageProcessor.bitmapToMat(grayBitmap);

        // 2. 放大图像到目标尺寸对应的像素（单位为像素）
        int targetCols = (int) (targetWidth * rho);
        int targetRows = (int) (targetHeight * rho);
        Mat resized = new Mat();
        Imgproc.resize(grayImage, resized, new Size(targetCols, targetRows), 0, 0, Imgproc.INTER_LINEAR);

        // 3. 用放大图像生成 GCode
        int rows = resized.rows();
        int cols = resized.cols();

        double pixelWidth = (double) targetWidth / cols;
        double pixelHeight = (double) targetHeight / rows;

        for (int y = 0; y < rows; y++) {
            double flippedY = targetHeight - y * pixelHeight;

            boolean isEngraving = false;
            double xStart = -1;

            if (y % 2 == 0) {
                // 偶数行：左 → 右
                for (int x = 0; x < cols; x++) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 128;

                    if (shouldEngrave) {
                        if (!isEngraving) {
                            xStart = x * pixelWidth;
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart-padding+startX, flippedY+startY));
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart+startX, flippedY+startY));
                            isEngraving = true;
                        }
                    } else if (isEngraving) {
                        double xEnd = (x - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, flippedY+startY, laserPower));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    double xEnd = (cols - 1) * pixelWidth;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, flippedY+startY, laserPower));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd+padding+startX, flippedY+startY));
                }

            } else {
                // 奇数行：右 → 左
                for (int x = cols - 1; x >= 0; x--) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 128;

                    if (shouldEngrave) {
                        if (!isEngraving) {
                            xStart = x * pixelWidth;
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart+padding+startX, flippedY+startY));
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart+startX, flippedY+startY));
                            isEngraving = true;
                        }
                    } else if (isEngraving) {
                        double xEnd = (x + 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, flippedY+startY, laserPower));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    double xEnd = 0;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, flippedY+startY, laserPower));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd-padding+startX, flippedY+startY));
                }
            }

            // Y 方向放大：重复输出这一行
            int repeatCount = (int) Math.round(pixelHeight);  // 重复次数由 Y 方向的放大比例决定
            for (int i = 1; i < repeatCount; i++) {
                if(i%2==1)
                {
                    double newFlippedY = flippedY - i * pixelHeight;
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart - padding+startX, newFlippedY+startY));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart+startX, newFlippedY+startY));
                    if (isEngraving) {
                        double xEnd = (cols - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, newFlippedY+startY, laserPower));
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd + padding+startX, newFlippedY+startY));
                    }
                }
                if(i%2==0)
                {
                    double newFlippedY = flippedY - i * pixelHeight;
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + padding+startX, newFlippedY+startY));
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart+startX, newFlippedY+startY));
                    if (isEngraving) {
                        double xEnd = (cols - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd+startX, newFlippedY+startY, laserPower));
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd - padding+startX, newFlippedY+startY));
                    }
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