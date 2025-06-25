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

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.os.Environment;
import android.widget.Toast;


public class GCode {
    private static final int MAX_POWER = 255;
    public static String generateGCode0(Mat image, int rho, int targetWidth, int targetHeight, double startX, double startY, int laserPower) {
        // 调用新的、功能更全的内部版本，并传入一个固定的默认阈值
        return generateGCode0(image, rho, targetWidth, targetHeight, startX, startY, laserPower, 127);
    }
    /**
     * 【新的内部实现版本】
     * 生成灰度雕刻的G-code，允许指定灰度阈值。
     * 这是实际执行工作负载的函数。
     */
    // 建议的重构版本 generateGCode0
    public static String generateGCode0(Mat image, int rho, int targetWidth, int targetHeight,
                                        double startX, double startY, int laserPower, int grayThreshold)
    {
        if (image == null || image.empty()) {
            return "; Error: Input image for grayscale is empty.\n";
        }

        int padding = 5;

        StringBuilder gcode = new StringBuilder();
        gcode.append("M4 S0\n");

        // 1. 原图转灰度图
        Bitmap bitmap = null;
        Bitmap grayBitmap = null;
        Mat grayImage = new Mat();
        Mat resized = new Mat();

        try {
            bitmap = ImageProcessor.matToBitmap(image);
            grayBitmap = ImageProcessor.toGrayscale(bitmap);
            grayImage = ImageProcessor.bitmapToMat(grayBitmap);

            // 2. 缩放图像到目标尺寸
            int cols = targetWidth * rho;
            int rows = targetHeight * rho;
            Imgproc.resize(grayImage, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_AREA); // INTER_AREA 更适合缩小图像

            // 3. 生成 GCode
            double pixelWidth = 1.0 / rho;

            for (int y = 0; y < rows; y++) {
                double realY = startY + targetHeight - (y * pixelWidth); // 使用pixelWidth，保持宽高比
                boolean isEngraving = false;
                double lineStartEngraveX = -1; // 记录本行第一个雕刻点，用于添加padding

                // 扫描方向：偶数行从左到右，奇数行从右到左 (Z字形扫描)
                int startCol = (y % 2 == 0) ? 0 : cols - 1;
                int endCol = (y % 2 == 0) ? cols : -1;
                int step = (y % 2 == 0) ? 1 : -1;

                for (int x = startCol; x != endCol; x += step) {
                    double gray = resized.get(y, x)[0];
                    boolean shouldEngrave = gray < grayThreshold;

                    if (shouldEngrave && !isEngraving) { // 从非雕刻区进入雕刻区
                        isEngraving = true;
                        double currentX = x * pixelWidth + startX;
                        if (lineStartEngraveX < 0) { // 是本行第一个雕刻段
                            lineStartEngraveX = currentX;
                            // 移动到雕刻段前方padding处
                            double safeMoveX = (step == 1) ? currentX - padding : currentX + padding;
                            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", safeMoveX, realY));
                        }
                        // 快速移动到雕刻起点，准备开激光
                        gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", currentX, realY));
                    } else if (!shouldEngrave && isEngraving) { // 从雕刻区进入非雕刻区
                        isEngraving = false;
                        // x-step 是因为当前x已经不雕刻了，上一个点才是雕刻段的终点
                        double endEngraveX = (x - step) * pixelWidth + startX;
                        // 执行雕刻指令
                        gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f S%d\n", endEngraveX, realY, laserPower));
                    }
                }

                // 如果一行扫描结束时仍在雕刻状态，需要收尾
                if (isEngraving) {
                    double finalX = ((step == 1 ? cols - 1 : 0) * pixelWidth) + startX;
                    gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f S%d\n", finalX, realY, laserPower));
                }
            }
        } finally {
            // !!! 关键：释放所有资源 !!!
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            if (grayBitmap != null && !grayBitmap.isRecycled()) grayBitmap.recycle();
            grayImage.release();
            resized.release();
        }

        gcode.append("M5\n");
        return gcode.toString();
    }
    // --- CHAIKIN算法辅助方法开始 ---
    /**
     * 使用Chaikin算法平滑折线。
     *
     * @param inputPoints 代表折线的点列表。
     * @param iterations  平滑迭代次数。
     * @param ratio       Chaikin比率（通常为0.25）。
     * @param isClosed    如果折线是闭合回路则为true，否则为false。
     *                    对于来自findContours的轮廓，通常应为true。
     * @return 一个新的平滑点列表。
     */
    private static List<Point> chaikinSmooth(List<Point> inputPoints, int iterations, double ratio, boolean isClosed) {
        if (inputPoints == null || inputPoints.size() < (isClosed ? 2 : 2)) { // 一个线段至少需要2个点
            return new ArrayList<>(inputPoints); // 如果点数不足，则返回原始副本
        }

        List<Point> currentPoints = new ArrayList<>(inputPoints);

        for (int iter = 0; iter < iterations; iter++) {
            if (currentPoints.size() < (isClosed ? 2 : 2)) { // 在循环中再次检查
                break; // 点数不足以进一步平滑
            }

            List<Point> smoothedPoints = new ArrayList<>();
            int n = currentPoints.size();

            if (isClosed) {
                if (n < 2) break; // 无法形成线段
                for (int i = 0; i < n; i++) {
                    Point p0 = currentPoints.get(i);
                    Point p1 = currentPoints.get((i + 1) % n); // 对于闭合轮廓，循环回到起点

                    Point q = new Point(
                            (1 - ratio) * p0.x + ratio * p1.x,
                            (1 - ratio) * p0.y + ratio * p1.y
                    );
                    Point r = new Point(
                            ratio * p0.x + (1 - ratio) * p1.x,
                            ratio * p0.y + (1 - ratio) * p1.y
                    );
                    smoothedPoints.add(q);
                    smoothedPoints.add(r);
                }
            } else { // 开放折线 - 保留端点
                if (n < 2) break;
                // 对于开放路径的Chaikin算法，通常会保留首尾端点，并对中间线段进行切割。
                // 这里的实现主要针对闭合轮廓，因为findContours通常产生闭合轮廓。
                // 如果确实需要精确的开放路径Chaikin平滑，此部分可能需要调整。
                // 为简单起见，如果isClosed为false，当前将回退到类似闭合路径的逻辑。
                // 一个更鲁棒的开放Chaikin会是：
                // smoothedPoints.add(currentPoints.get(0)); // 添加第一个点
                // for (int i = 0; i < n - 2; i++) { ... } // 处理中间点
                // smoothedPoints.add(currentPoints.get(n - 1)); // 添加最后一个点
                // 目前，我们假设isClosed=true用于轮廓。
                // 如果错误地将isClosed设为false，则暂时按闭合逻辑处理。
                for (int i = 0; i < n; i++) { // 实际上，对于开放路径，循环应该是到 n-1
                    Point p0 = currentPoints.get(i);
                    Point p1 = currentPoints.get((i + 1) % n); // 对于开放路径，这里应该是 (i+1)，并在i=n-1时特殊处理或不处理

                    Point q = new Point( (1 - ratio) * p0.x + ratio * p1.x, (1 - ratio) * p0.y + ratio * p1.y );
                    Point r = new Point( ratio * p0.x + (1 - ratio) * p1.x, ratio * p0.y + (1 - ratio) * p1.y );
                    smoothedPoints.add(q);
                    smoothedPoints.add(r);
                }
            }
            currentPoints = smoothedPoints;
        }
        return currentPoints;
    }
    // --- CHAIKIN算法辅助方法结束 ---


    public static String generateGCodeFromEdges(Mat image,
                                                double targetWidth, double targetHeight,
                                                double startX, double startY,
                                                int laserPower,
                                                boolean invertBinary,
                                                double simplifyEpsilonFactor) {

        // --- 用于预处理和平滑的参数（可以设为方法参数） ---
        // 高斯模糊
        boolean applyGaussianBlur = true;
        Size gaussianKernelSize = new Size(5, 5); // 必须是奇数，例如3,3或5,5
        double gaussianSigmaX = 0;                 // 0表示sigma由核大小计算得出

        // 形态学操作（可选，应用于二值图像）
        boolean applyMorphologicalOpening = true; // 设置为true以应用
        Size morphKernelSize = new Size(3, 3);   // 例如3,3或5,5
        int morphOperation = Imgproc.MORPH_OPEN; // 开运算

        // Chaikin平滑
        boolean applyChaikinSmoothing = true;     // 设置为true以应用Chaikin算法
        int chaikinIterations = 2;                // 迭代次数（通常1-3次）
        double chaikinRatio = 0.25;               // Chaikin算法的标准比率

        if (image == null || image.empty()) {
            return "; Error: 输入的轮廓图像为空。\n";
        }

        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>(); // 来自findContours的原始轮廓
        List<MatOfPoint2f> processedContours2f = new ArrayList<>(); // 用于处理的轮廓（浮点精度）

        try {
            // 1. 预处理：灰度化
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
            } else {
                grayMat = image.clone();
            }

            // 应用高斯模糊（阈值化之前）
            if (applyGaussianBlur) {
                Imgproc.GaussianBlur(grayMat, grayMat, gaussianKernelSize, gaussianSigmaX);
            }

            // 阈值化
            int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
            Imgproc.threshold(grayMat, binaryMat, 0, 255, thresholdType | Imgproc.THRESH_OTSU);

            // 可选：形态学操作（在二值图像上，findContours之前）
            if (applyMorphologicalOpening) {
                Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morphKernelSize);
                Imgproc.morphologyEx(binaryMat, binaryMat, morphOperation, morphKernel);
                morphKernel.release();
            }

            // 2. 查找轮廓（获取所有点）
            Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

            if (contours.isEmpty()) {
                return "; Info: 图像中未找到轮廓。\n";
            }

            // 将初始轮廓转换为MatOfPoint2f以进行浮点精度处理
            for (MatOfPoint contour : contours) {
                processedContours2f.add(new MatOfPoint2f(contour.toArray()));
            }

            // 3. 应用Chaikin平滑（如果启用）
            if (applyChaikinSmoothing && chaikinIterations > 0) {
                List<MatOfPoint2f> smoothedChaikinContours = new ArrayList<>();
                for (MatOfPoint2f contour2f : processedContours2f) {
                    Point[] pointsArray = contour2f.toArray();
                    if (pointsArray.length >= 2) { // Chaikin至少需要2个点形成线段
                        // 假设轮廓是闭合的，这对于findContours的输出是典型的
                        List<Point> smoothedList = chaikinSmooth(Arrays.asList(pointsArray), chaikinIterations, chaikinRatio, true);
                        if (!smoothedList.isEmpty()) {
                            smoothedChaikinContours.add(new MatOfPoint2f(smoothedList.toArray(new Point[0])));
                        } else {
                            smoothedChaikinContours.add(contour2f); // 如果平滑失败，则回退
                        }
                    } else {
                        smoothedChaikinContours.add(contour2f); // 点数不足以进行Chaikin平滑
                    }
                }
                processedContours2f = smoothedChaikinContours; // 更新为Chaikin平滑后的轮廓
            }

            // 4. 应用多边形逼近 (Douglas-Peucker)，如果 simplifyEpsilonFactor > 0
            List<MatOfPoint2f> finalContoursForGCode = new ArrayList<>();
            if (simplifyEpsilonFactor > 0) {
                for (MatOfPoint2f contour2f : processedContours2f) {
                    MatOfPoint2f approxContour2f = new MatOfPoint2f();
                    double perimeter = Imgproc.arcLength(contour2f, true); // true表示轮廓是闭合的
                    double epsilon = simplifyEpsilonFactor * perimeter;
                    Imgproc.approxPolyDP(contour2f, approxContour2f, epsilon, true);
                    if (approxContour2f.toArray().length > 1) {
                        finalContoursForGCode.add(approxContour2f);
                    } else if (contour2f.toArray().length > 1) {
                        // 如果approxPolyDP将点数减少到<2，但原始点数>1，则考虑保留原始（或逼近前）的轮廓
                        finalContoursForGCode.add(contour2f);
                    }
                }
            } else {
                finalContoursForGCode.addAll(processedContours2f); // 不进行简化，使用当前轮廓
            }

            if (finalContoursForGCode.isEmpty()) {
                return "; Info: 处理后没有可用的轮廓。\n";
            }

            // 5. 生成G代码
            StringBuilder gcode = new StringBuilder();
            gcode.append("M4 S0\n"); // 启用激光动态功率模式，初始功率为0

            double scaleX = targetWidth / binaryMat.width();
            double scaleY = targetHeight / binaryMat.height();

            for (MatOfPoint2f contour2f_final : finalContoursForGCode) {
                Point[] points = contour2f_final.toArray();
                if (points.length < 2) continue;

                Point startPointImg = points[0];
                // 图像坐标系Y轴向下（0,0在左上角），机器坐标系Y轴向上（0,0在左下角）
                double machineStartX = startX + (startPointImg.x * scaleX);
                double machineStartY = startY + ((binaryMat.height() - 1 - startPointImg.y) * scaleY);

                gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", machineStartX, machineStartY));
                gcode.append(String.format(Locale.US, "G1 S%d\n", laserPower)); // 以指定功率打开激光

                for (int i = 1; i < points.length; i++) {
                    Point pImg = points[i];
                    double machineX = startX + (pImg.x * scaleX);
                    double machineY = startY + ((binaryMat.height() - 1 - pImg.y) * scaleY);
                    gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineX, machineY));
                }
                // 通过返回当前轮廓的起点来闭合路径
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineStartX, machineStartY));
                gcode.append("G1 S0\n"); // 关闭激光（或将功率设置为0）
            }

            // G代码结束
            gcode.append("M5\n"); // 关闭激光
            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", startX, startY)); // 返回到全局起始X, Y
            // 或者返回到0,0: gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", 0.0, 0.0));


            return gcode.toString();

        } finally {
            // 释放Mat对象
            grayMat.release();
            binaryMat.release();
            hierarchy.release();
            for (MatOfPoint contour : contours) {
                contour.release();
            }
            // processedContours2f 和 finalContoursForGCode 中的 MatOfPoint2f 对象由Java GC管理，
            // 因为它们的内部Mat数据是从数组创建的，或者是由返回新Mat的OpenCV函数创建的。
        }
    }
    // 优化后的 generateGCodeFromEdges (改动很小)
//    public static String generateGCodeFromEdges(Mat image,
//                                                double targetWidth, double targetHeight,
//                                                double startX, double startY,
//                                                int laserPower,
//                                                boolean invertBinary,
//                                                double simplifyEpsilonFactor) {
//
//        if (image == null || image.empty()) {
//            return "; Error: Input image for outline is empty.\n";
//        }
//
//        // 资源将在 finally 块中释放
//        Mat grayMat = new Mat();
//        Mat binaryMat = new Mat();
//        Mat hierarchy = new Mat();
//        List<MatOfPoint> contours = new ArrayList<>();
//        List<MatOfPoint> processedContours = new ArrayList<>();
//
//        try {
//            // 1. 预处理：灰度化和二值化
//            if (image.channels() > 1) {
//                Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
//            } else {
//                grayMat = image.clone();
//            }
//
//            int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
//            Imgproc.threshold(grayMat, binaryMat, 200, 255, thresholdType | Imgproc.THRESH_OTSU);
//
//
//            // 2. 查找并简化轮廓
//            Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
//
//            if (simplifyEpsilonFactor > 0) {
//                for (MatOfPoint contour : contours) {
//                    MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
//                    double perimeter = Imgproc.arcLength(contour2f, true);
//                    double epsilon = simplifyEpsilonFactor * perimeter;
//                    MatOfPoint2f approxContour2f = new MatOfPoint2f();
//                    Imgproc.approxPolyDP(contour2f, approxContour2f, epsilon, true);
//                    if (approxContour2f.toArray().length > 1) {
//                        processedContours.add(new MatOfPoint(approxContour2f.toArray()));
//                    }
//                }
//            } else {
//                processedContours.addAll(contours);
//            }
//
//            if (processedContours.isEmpty()) {
//                return "; Info: No contours found in image.\n";
//            }
//
//            // 3. 遍历轮廓并生成G代码
//            StringBuilder gcode = new StringBuilder();
//            gcode.append("M4 S0\n"); // 启动动态激光模式，功率为0
//
//            double scaleX = targetWidth / binaryMat.width();
//            double scaleY = targetHeight / binaryMat.height();
//
//            for (MatOfPoint contour : processedContours) {
//                Point[] points = contour.toArray();
//                if (points.length < 2) continue;
//
//                Point startPointImg = points[0];
//                double machineStartX = startX + (startPointImg.x * scaleX);
//                double machineStartY = startY + ((binaryMat.height() - 1 - startPointImg.y) * scaleY);
//
//                gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", machineStartX, machineStartY)); // S0是默认值，可省略
//                gcode.append(String.format(Locale.US, "G1 S%d\n", laserPower)); // 开激光
//
//                for (int i = 1; i < points.length; i++) {
//                    Point pImg = points[i];
//                    double machineX = startX + (pImg.x * scaleX);
//                    double machineY = startY + ((binaryMat.height() - 1 - pImg.y) * scaleY);
//                    gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineX, machineY));
//                }
//                // 闭合路径
//                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineStartX, machineStartY));
//                gcode.append("G1 S0\n"); // 关激光
//            }
//
//            // 4. 结束指令
//            gcode.append("M5\n");
//            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", 0.0, 0.0));
//
//            return gcode.toString();
//
//        } finally {
//            // 确保所有 Mat 对象都被释放
//            grayMat.release();
//            binaryMat.release();
//            hierarchy.release();
//            for (MatOfPoint contour : contours) contour.release();
//            // processedContours 中的 MatOfPoint 是从 contours 移动或创建的，也需要考虑释放
//            // 但由于它们是局部变量，Java的GC会处理MatOfPoint对象本身，其内部的Mat数据在 findContours 后由 contours 列表管理并已释放
//        }
//    }

    /**
     * 生成包含灰度雕刻和外部轮廓切割的G-code。
     *
     * @param image                 源图像Mat对象
     * @param rho                   灰度雕刻分辨率 (lines/mm)
     * @param targetWidth           目标宽度 (mm)
     * @param targetHeight          目标高度 (mm)
     * @param startX                加工起始点X坐标
     * @param startY                加工起始点Y坐标
     * @param laserPower            激光功率
     * @param simplifyEpsilonFactor 轮廓简化因子 (推荐 0.001 - 0.01)
     * @param invertBinary          对于轮廓查找是否反转二值化图像
     * @return                      完整的G-code字符串
     */
    public static String generateGCodeWithOutline(Mat image, Mat frameImage,
                                                  int rho, int targetWidth, int targetHeight,
                                                  double startX, double startY, int laserPower,int cutPower,
                                                  double simplifyEpsilonFactor, boolean invertBinary) {

        // 1. 生成灰度雕刻部分的 G-code
        String grayscaleGCode = generateGCode0(image, rho, targetWidth, targetHeight, startX, startY, laserPower, 128); // 使用重构后的函数

        // 2. 生成轮廓切割部分的 G-code
        // 注意：这里的image应该是原始图像，或者与灰度雕刻使用相同的图像
        String outlineGCode = generateGCodeFromEdges(frameImage, targetWidth, targetHeight, startX, startY, cutPower, invertBinary, simplifyEpsilonFactor);

        // 3. 智能拼接 G-code
        StringBuilder finalGCode = new StringBuilder();

        // 添加统一的头部
        finalGCode.append("M4 S0\n"); // 启动动态激光模式

        // -- 添加灰度雕刻代码 --
        // 去除 generateGCode0 返回结果的 M4 和 M5
        if (grayscaleGCode != null && !grayscaleGCode.isEmpty()) {
            String[] grayLines = grayscaleGCode.split("\n");
            // 从第二行开始，到倒数第二行结束，忽略 M4 和 M5
            for (int i = 1; i < grayLines.length - 1; i++) {
                if (!grayLines[i].trim().isEmpty()) {
                    finalGCode.append(grayLines[i]).append("\n");
                }
            }
        }

        // -- 添加轮廓切割代码 --
        // 去除 generateGCodeFromEdges 返回结果的 M4, M5 和 G0 X0 Y0
        if (outlineGCode != null && !outlineGCode.isEmpty() && !outlineGCode.startsWith(";")) {
            String[] outlineLines = outlineGCode.split("\n");
            // 从第二行开始，到倒数第三行结束，忽略 M4, M5, 和最后的 G0
            for (int i = 1; i < outlineLines.length - 2; i++) {
                if (!outlineLines[i].trim().isEmpty()) {
                    finalGCode.append(outlineLines[i]).append("\n");
                }
            }
        }

        // 添加统一的尾部
        finalGCode.append("M5\n"); // 全部完成，关闭激光
        finalGCode.append(String.format(Locale.US, "G0 X%.3f Y%.3f S0\n", 0.0, 0.0)); // 返回机器原点

        return finalGCode.toString();
    }

    public static String generateGCodeWithOutline(Mat image,
                                                  int rho, int targetWidth, int targetHeight,
                                                  double startX, double startY, int laserPower,int cutPower,
                                                  double simplifyEpsilonFactor, boolean invertBinary)
    {
       return generateGCodeWithOutline(image,image,rho,targetWidth,targetHeight,startX,startY,laserPower,cutPower,simplifyEpsilonFactor,invertBinary);
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