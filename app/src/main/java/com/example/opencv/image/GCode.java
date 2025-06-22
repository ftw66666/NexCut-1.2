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
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.Environment;
import android.widget.Toast;


public class GCode {

    public static String generateGCode0(Mat image, int rho, int targetWidth, int targetHeight, double startX, double startY, int laserPower) {

        int padding = 5;

        StringBuilder gcode = new StringBuilder();
        gcode.append("M4 S0\n");

        // 1. 原图转灰度图
        Bitmap bitmap = ImageProcessor.matToBitmap(image);
        Bitmap grayBitmap = ImageProcessor.toGrayscale(bitmap);
        Mat grayImage = ImageProcessor.bitmapToMat(grayBitmap);
        int originRows = grayImage.rows();
        int originCols = grayImage.cols();
//        if (originRows > targetHeight * rho) {
//            rho = originRows / targetHeight + 1;
//        }

        // 2. 缩放图像到目标尺寸
        int cols = targetWidth * rho;
        int rows = targetHeight * rho;
        Mat resized = new Mat();
        Imgproc.resize(grayImage, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_LINEAR);

        // 3. 生成 GCode
        double pixelWidth = 1.0 / rho;
        double pixelHeight = 1.0 / rho;

        for (int y = 0; y < rows; y++) {
            double realY = startY + targetHeight - y * pixelHeight;
            boolean isEngraving = false;
            boolean firstEngraveFound = false;
            double xStart = -1;
            double xEnd = -1;

            if (y % 2 == 0) {
                // 偶数行：左 → 右
                for (int x = 0; x < cols; x++) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 218;

                    if (shouldEngrave && !isEngraving) {
                        xStart = x * pixelWidth;
                        if (!firstEngraveFound) {
                            // 在第一个雕刻段之前加 padding 空移
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart - padding + startX, realY));
                            firstEngraveFound = true;
                        }
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + startX, realY));
                        isEngraving = true;
                    } else if (!shouldEngrave && isEngraving) {
                        xEnd = (x - 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    xEnd = (cols - 1) * pixelWidth;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                    isEngraving = false;
                }
                if (firstEngraveFound && xEnd >= 0) {
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd + padding + startX, realY));
                }

            } else {
                // 奇数行：右 → 左
                for (int x = cols - 1; x >= 0; x--) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < 128;

                    if (shouldEngrave && !isEngraving) {
                        xStart = x * pixelWidth;
                        if (!firstEngraveFound) {
                            // 在第一个雕刻段之前加 padding 空移
                            gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + padding + startX, realY));
                            firstEngraveFound = true;
                        }
                        gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xStart + startX, realY));
                        isEngraving = true;
                    } else if (!shouldEngrave && isEngraving) {
                        xEnd = (x + 1) * pixelWidth;
                        gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                        isEngraving = false;
                    }
                }
                if (isEngraving) {
                    xEnd = 0;
                    gcode.append(String.format("G1 X%.2f Y%.2f S%d\n", xEnd + startX, realY, laserPower));
                    isEngraving = false;
                }
                if (firstEngraveFound && xEnd >= 0) {
                    gcode.append(String.format("G0 X%.2f Y%.2f S0\n", xEnd - padding + startX, realY));
                }
            }
        }

        gcode.append("M5\n");
        grayImage.release();
        resized.release();
        return gcode.toString();
    }

    /**
     * 通过二值化和轮廓查找生成矢量路径G代码。
     * 适用于提取清晰对象的轮廓。
     * G代码格式严格遵循您提供的光栅扫描方法的规范（使用 M4 和 S 参数）。
     *
     * @param image        输入图像的Mat对象 (推荐使用BGR或灰度格式)
     * @param targetWidth  雕刻内容在机器上的目标宽度 (mm)
     * @param targetHeight 雕刻内容在机器上的目标高度 (mm)
     * @param startX       雕刻区域左下角在机器坐标系中的X坐标 (mm)
     * @param startY       雕刻区域左下角在机器坐标系中的Y坐标 (mm)
     * @param laserPower   雕刻时的激光功率 (例如 0-255 或 0-1000)
     * @param invertBinary 如果为true，则认为黑色是目标，白色是背景（会反转二值化结果）
     * @param simplifyEpsilonFactor 控制轮廓简化的程度，值越小，细节越多，点越多。0表示不简化。
     *                             建议值为 0.001 * Imgproc.arcLength (相对) 或 0.5-1.5 (绝对像素)
     * @return 边缘雕刻的G代码字符串
     */
    public static String generateGCodeFromEdges(Mat image,
                                                        double targetWidth, double targetHeight,
                                                        double startX, double startY,
                                                        int laserPower,
                                                        boolean invertBinary,
                                                        double simplifyEpsilonFactor) {

        if (image.empty()) {
            return "; Error: Input image is empty.\n";
        }

        // --- 1. 图像预处理 ---
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();

        // 转换为灰度图
        if (image.channels() == 3 || image.channels() == 4) {
            Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
        } else if (image.channels() == 1) {
            grayMat = image.clone(); // 已经是灰度图
        } else {
            return "; Error: Unsupported image format (channels)\n";
        }

        // 二值化
        // findContours 默认查找白色物体。
        // 如果您的马是黑色的，背景是白色的：
        //   - 如果 invertBinary = true, 使用 THRESH_BINARY_INV (黑色变白色)
        //   - 如果 invertBinary = false, 使用 THRESH_BINARY (需要图像本身马是白色，背景黑色)
        // 对于您提供的马图（黑色马，白色背景），应该使用 THRESH_BINARY_INV
        int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
        // 使用Otsu's方法自动确定阈值，或者手动指定一个阈值（例如127）
        Imgproc.threshold(grayMat, binaryMat, 0, 255, thresholdType | Imgproc.THRESH_OTSU);
        // 或者手动阈值： Imgproc.threshold(grayMat, binaryMat, 127, 255, thresholdType);


        // --- 2. 发现并简化轮廓 ---
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // binaryMat 现在是 CV_8UC1 类型
        Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        // Imgproc.RETR_EXTERNAL: 只检索最外层的轮廓。如果马内部有白色空洞且也想雕刻其边缘，则需用 RETR_TREE 并处理层级。
        // Imgproc.CHAIN_APPROX_SIMPLE: 压缩水平、垂直和对角线段，只保留其端点。

        List<MatOfPoint> processedContours = new ArrayList<>();
        if (simplifyEpsilonFactor > 0) {
            for (MatOfPoint contour : contours) {
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double perimeter = Imgproc.arcLength(contour2f, true);
                double epsilon = simplifyEpsilonFactor * perimeter; // 基于周长的相对简化
                // 或者使用绝对像素简化： double epsilon = simplifyEpsilonFactor; (此时 factor 应该是 0.5, 1.0 之类的值)

                MatOfPoint2f approxContour2f = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approxContour2f, epsilon, true); // true for closed contours

                if (approxContour2f.toArray().length > 1) { // 至少需要2个点形成一条线段
                    processedContours.add(new MatOfPoint(approxContour2f.toArray()));
                }
                contour2f.release();
                approxContour2f.release();
                contour.release(); // 原始contour可以释放，因为数据已复制到approxContour2f
            }
        } else {
            // 不简化，直接使用原始轮廓 (可能点很多)
            for (MatOfPoint contour : contours) {
                if (contour.toArray().length > 1) {
                    processedContours.add(contour); // 注意：这里是引用，如果后续 contours 列表被修改，会影响
                } else {
                    contour.release(); // 点太少的轮廓直接释放
                }
            }
            // 如果不简化，并且 contours 列表本身不再需要，可以不清空，但其内部的 MatOfPoint 如果不再使用应释放
            // 但因为 processedContours 现在持有它们，所以还不能释放。
            // 最佳实践是，如果使用原始轮廓，也进行一次克隆或确保生命周期管理正确。
            // 为简单起见，如果 simplifyEpsilonFactor <= 0, 也可以直接使用 contours 列表，
            // 并在循环 G 代码生成后，再统一释放 contours 里的 MatOfPoint。
            // 为了安全和清晰，我们还是用 processedContours 统一处理。
        }


        // --- 3. 生成G代码 ---
        StringBuilder gcode = new StringBuilder();
        gcode.append("M4 S0\n"); // 动态激光模式，初始功率0

        int imageWidth = binaryMat.width(); // 使用二值图的尺寸进行缩放计算
        int imageHeight = binaryMat.height();
        if (imageWidth == 0 || imageHeight == 0) {
            grayMat.release();
            binaryMat.release();
            hierarchy.release();
            for(MatOfPoint mop : contours) mop.release(); // 确保原始contours也释放
            for(MatOfPoint mop : processedContours) {
                // 如果 processedContours 包含的是原始 contours 的引用，这里重复释放会出错
                // 如果是 approxPolyDP 的结果，则是新的 MatOfPoint，需要释放
                // 在上面的简化逻辑中，原始contour已经释放了，processedContours持有的是新创建的
                if (simplifyEpsilonFactor <= 0) { /* 不做额外释放，将在循环后处理 */ }
                else { mop.release(); }
            }
            return "; Error: Invalid image dimensions after processing\n";
        }

        double scaleX = targetWidth / imageWidth;
        double scaleY = targetHeight / imageHeight;

        for (MatOfPoint contour : processedContours) {
            Point[] points = contour.toArray();
            if (points.length < 2) { // 至少需要两个点来画线
                if (simplifyEpsilonFactor > 0 || contours.contains(contour)) {
                    // 如果是简化后的或者原始的，但点太少，就释放它
                    // 如果不简化，contour可能在contours列表里，之后会统一释放
                }
                continue;
            }


            Point startPointImg = points[0];
            // Y轴翻转：图像(0,0)在左上，机器(0,0)在左下
            double machineStartX = startX + (startPointImg.x * scaleX);
            // 注意图像坐标是0到height-1
            double machineStartY = startY + ((imageHeight - 1 - startPointImg.y) * scaleY);

            // 快速移动到轮廓起点，激光关闭 (S0)
            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f S0\n", machineStartX, machineStartY));

            // 沿着轮廓进行线性雕刻
            for (int i = 0; i < points.length; i++) {
                Point pImg = points[i];
                double machineX = startX + (pImg.x * scaleX);
                double machineY = startY + ((imageHeight - 1 - pImg.y) * scaleY);

                if (i == 0) {
                    // 对于轮廓的第一个点，我们已经G0移动到那里，现在需要G1指令开启激光
                    // 即使是移动到同一点，这个G1也包含了S<laserPower>
                    gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f S%d\n", machineX, machineY, laserPower));
                } else {
                    // 后续点，S值会保持
                    gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineX, machineY));
                }
            }

            // 确保轮廓闭合：移动回起点（如果轮廓本身不是精确闭合的）
            // approxPolyDP(closed=true) 会尝试闭合，但可能不是完全重合
            // 对于 CHAIN_APPROX_SIMPLE 得到的轮廓，它们通常是闭合的
            // 为保险起见，可以加一个回到起点的G1指令
            Point endPointImg = points[points.length - 1];
            // 检查最后一个点是否与起点足够接近
            if (Math.abs(startPointImg.x - endPointImg.x) > 1e-2 || Math.abs(startPointImg.y - endPointImg.y) > 1e-2) {
                // 如果不接近，则显式闭合路径
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineStartX, machineStartY)); // S值保持
            }
        }

        // --- 4. 结束程序 ---
        gcode.append("M5\n"); // 关闭激光
        gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f S0\n", 0.0, 0.0)); // 返回机器原点

        // 释放内存
        grayMat.release();
        binaryMat.release();
        hierarchy.release();

        // 释放 processedContours 中的 MatOfPoint
        // 原始 contours 列表中的 MatOfPoint 在简化过程中或不简化但点少时已被释放，
        // 或被加入 processedContours (如果不简化)。
        // 为了确保所有 MatOfPoint 都被释放：
        // 1. 如果 simplifyEpsilonFactor > 0, 原始 contours 的 MatOfPoint 在循环中被释放，
        //    processedContours 中的是新创建的，也需要释放。
        // 2. 如果 simplifyEpsilonFactor <= 0, processedContours 中的是原始 contours 的引用。

        // 清理 contours 列表 (如果 simplifyEpsilonFactor <=0 且 processedContours 是其子集或引用)
        // 实际上，在上面简化逻辑调整后，原始 contours 的元素要么被释放，要么其数据被用于创建新的 MatOfPoint
        // 所以，只需要释放 processedContours 中的即可，前提是简化步骤中原始 contour 被正确处理。

        for (MatOfPoint mop : processedContours) {
            mop.release();
        }
        // 如果 simplifyEpsilonFactor <= 0，contours 列表里的 MatOfPoint 还没有被释放，且被 processedContours 引用。
        // processedContours 释放后，原始 contours 列表里的也相当于被释放了（如果它们是同一个对象）。
        // 如果是克隆的，那么原始 contours 列表还需要单独释放。
        // 为了最安全，如果 simplifyEpsilonFactor <=0, 应该这样：
        if (simplifyEpsilonFactor <= 0) {
            // processedContours 只是引用了 contours 里的对象，
            // 所以释放 processedContours 里的对象就够了。
            // contours 列表本身不需要再对其元素调用 release，因为它已经被 processedContours 处理了。
        } else {
            // 原始 contours 列表在简化循环中已经被释放了
            for(MatOfPoint mop : contours) { // 如果上面简化循环没有释放原始contour
                // mop.release(); // 则在这里释放，但当前代码是在简化循环里释放的
            }
        }
        // 最简单的做法是，确保 contours 列表在最后也被清空和释放其元素，除非元素已被转移和管理
        // 在当前代码中，简化部分已经释放了原始 contour。
        // 如果不简化，processedContours 就是 contours 的一个副本（或引用），释放 processedContours 就够了。
        contours.clear(); // 清空列表本身


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

//            File file = new File(context.getExternalFilesDir(null), fileName);
            File file = new File(context.getExternalFilesDir("/gcodes"), fileName);
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
//        File file = new File(context.getExternalFilesDir(null), s);
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), s);
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