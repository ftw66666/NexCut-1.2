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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ximgproc.Ximgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import android.os.Environment;


public class GCode {
    private static final int MAX_POWER = 255;
    public static String generateGCode0(Mat image, int rho, int targetWidth, int targetHeight, double startX, double startY, int laserPower) {
        // 调用新的、功能更全的内部版本，并传入一个固定的默认阈值
        return generateGCodeForPlatform(image, rho, targetWidth, targetHeight, laserPower, 127);
    }

    /**
     * 根据代表整个工作平台的图像生成优化的灰度G代码。
     * 雕刻区域从工作坐标系的原点 (0,0) 开始。
     * 该方法会合并一行中功率相同的连续像素，以减小文件大小。
     *
     * @param image        输入的OpenCV Mat图像，代表整个平台
     * @param rho          分辨率 (每毫米的点数/线数)
     * @param targetWidth  目标雕刻宽度 (毫米)，即平台的宽度
     * @param targetHeight 目标雕刻高度 (毫米)，即平台的高度
     * @param laserPower   最大激光功率 (对应纯黑色)
     * @param grayThreshold 灰度阈值 (0-255)，高于此值的像素将被忽略 (视为白色)
     * @return 生成的优化G代码字符串
     */
    public static String generateGCodeForPlatform(Mat image, int rho, int targetWidth, int targetHeight,
                                                  int laserPower, int grayThreshold) {
        if (image == null || image.empty()) {
            return "; Error: Input image is empty.\n";
        }

        StringBuilder gcode = new StringBuilder();

        // 标准G代码头部，确保机器状态正确
        gcode.append("G21\n"); // 设置单位为毫米
        gcode.append("G90\n"); // 设置为绝对坐标模式
        gcode.append("M4 S0\n"); // 使用动态激光功率模式，初始功率为0

        Mat grayImage = new Mat();
        Mat resized = new Mat();

        try {
            // 1. 确保图像是灰度图
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            } else {
                image.copyTo(grayImage);
            }

            // 2. 缩放图像以匹配物理尺寸和分辨率
            int cols = targetWidth * rho;
            int rows = targetHeight * rho;
            Imgproc.resize(grayImage, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_AREA);

            // 3. 生成GCode
            double pixelWidth = 1.0 / rho;

            for (int y = 0; y < rows; y++) {
                // Y坐标计算：从平台的顶部 (targetHeight) 向下移动
                double realY = targetHeight - (y * pixelWidth);

                // Z字形扫描方向
                int startCol = (y % 2 == 0) ? 0 : cols - 1;
                int endCol = (y % 2 == 0) ? cols : -1;
                int step = (y % 2 == 0) ? 1 : -1;

                // 计算并移动到当前行的起始X坐标
                double lineStartX = startCol * pixelWidth;
                gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", lineStartX, realY));

                if (cols == 0) continue;

                // 初始化第一个区段
                int lastPower = calculatePower(resized.get(y, startCol)[0], laserPower, grayThreshold);

                for (int x = startCol + step; x != endCol; x += step) {
                    int currentPower = calculatePower(resized.get(y, x)[0], laserPower, grayThreshold);

                    if (currentPower != lastPower) {
                        // 区段结束，生成GCode
                        // 上个区段的终点X坐标
                        double segmentEndX = (x - step) * pixelWidth;

                        // 只在功率大于0时才设置功率和移动，对于功率为0的区段可以优化
                        if (lastPower > 0) {
                            gcode.append(String.format(Locale.US, "S%d\n", lastPower));
                            gcode.append(String.format(Locale.US, "G1 X%.3f\n", segmentEndX));
                        } else {
                            // 如果上一段是空白（功率为0），我们只需要快速移动到新段的起点
                            // 新段起点就是当前像素位置
                            double newSegmentStartX = x * pixelWidth;
                            gcode.append(String.format(Locale.US, "G0 X%.3f\n", newSegmentStartX));
                        }

                        lastPower = currentPower;
                    }
                }

                // 处理并生成行尾的最后一个区段
                if (lastPower > 0) {
                    double finalSegmentEndX = (endCol - step) * pixelWidth;
                    gcode.append(String.format(Locale.US, "S%d\n", lastPower));
                    gcode.append(String.format(Locale.US, "G1 X%.3f\n", finalSegmentEndX));
                }
            }
        } finally {
            grayImage.release();
            resized.release();
        }

        gcode.append("M5\n"); // 雕刻结束，彻底关闭激光
        gcode.append("G0 X0 Y0\n"); // 可选：任务结束后返回工作区原点
        return gcode.toString();
    }

    /**
     * 根据灰度值计算激光功率的辅助函数
     */
    private static int calculatePower(double grayValue, int maxPower, int threshold) {
        if (grayValue >= threshold) {
            return 0;
        }
        double powerRatio = 1.0 - (grayValue / (double) threshold);
        int power = (int) (maxPower * powerRatio);
        return Math.max(0, Math.min(power, maxPower));
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
    public static List<Point> chaikinSmooth(List<Point> inputPoints, int iterations, double ratio, boolean isClosed) {
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
    /**
     * [最终完整版] 从轮廓图中提取骨架，并生成优化后的G代码。
     *
     * 该方法集成了所有优化：
     * 1.  健壮的路径追踪，能正确处理分叉点和闭环。
     * 2.  保持原始图像宽高比，防止输出被拉伸，并使其在目标区域居中。
     * 3.  使用Douglas-Peucker算法简化路径，减少G代码冗余，使运动更平滑。
     * 4.  通过最近邻算法优化雕刻顺序，减少空载移动时间。
     * 5.  生成包含速度(F)和功率(S)参数的标准化G代码。
     *
     * @param image         输入的线框图 (BGR或灰度图)
     * @param targetWidth   G代码坐标系中的目标宽度 (mm)
     * @param targetHeight  G代码坐标系中的目标高度 (mm)
     * @param startX        G代码坐标系中的全局起始X偏移 (mm)
     * @param startY        G代码坐标系中的全局起始Y偏移 (mm)
     * @param laserPower    激光工作功率 (S值, 例如 0-1000)
     * @param invertBinary  是否反转二值化。对于白底黑线的图，应设为 true。
     * @param simplifyEpsilon 路径简化的容差(epsilon)。值越大，简化程度越高。建议值 1.0-2.0。
     * @return 生成的G代码字符串
     */
    public static String generateGCodeFromSkeleton(Mat image,
                                                   double targetWidth, double targetHeight,
                                                   double startX, double startY,
                                                   int laserPower,
                                                   boolean invertBinary, double simplifyEpsilon) {

        if (image == null || image.empty()) {
            return "; Error: Input image is empty.\n";
        }

        Mat gray = new Mat();
        Mat binary = new Mat();
        Mat skeleton = new Mat();

        try {
            // 1. 预处理：灰度化和二值化
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                gray = image.clone();
            }
            int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
            Imgproc.threshold(gray, binary, 0, 255, thresholdType | Imgproc.THRESH_OTSU);

            // 形态学去噪（只做一次闭运算，核为3x3）
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);
            kernel.release();

            // 2. 骨架提取
            Ximgproc.thinning(binary, skeleton, Ximgproc.THINNING_ZHANGSUEN);

            // 3. [核心] 使用健壮的算法追踪路径
            List<List<Point>> rawPaths = SkeletonTracerV2.trace(skeleton);
            // 过滤掉短路径（只过滤掉孤立点）
            List<List<Point>> filteredPaths = new ArrayList<>();
            for (List<Point> path : rawPaths) {
                if (path.size() >= 2) {
                    filteredPaths.add(path);
                }
            }
            // 合并端点距离很近的线段
            filteredPaths = mergeClosePaths(filteredPaths, 1.0); // 1.0像素为合并阈值，减少飞线
            if (filteredPaths.isEmpty()) {
                return "; Info: No valid paths could be traced from the skeleton.\n";
            }

            // 4. [优化] 简化路径
            List<List<Point>> simplifiedPaths = new ArrayList<>();
            for (List<Point> path : filteredPaths) {
                if (path.size() < 2) continue;
                MatOfPoint2f pathMat = new MatOfPoint2f(path.toArray(new Point[0]));
                MatOfPoint2f simplifiedMat = new MatOfPoint2f();
                Imgproc.approxPolyDP(pathMat, simplifiedMat, simplifyEpsilon, false);
                if (simplifiedMat.rows() > 1) {
                    simplifiedPaths.add(Arrays.asList(simplifiedMat.toArray()));
                }
                pathMat.release();
                simplifiedMat.release();
            }

            // 5. [优化] 优化路径顺序
            List<List<Point>> orderedPaths = optimizePathOrder(simplifiedPaths);
            Bitmap skeletonb = ImageProcessor.matToBitmap(skeleton);

            // 6. G代码生成
            return buildGCode(orderedPaths, skeleton.size(), targetWidth, targetHeight, startX, startY, laserPower);

        } finally {
            gray.release();
            binary.release();
            skeleton.release();
        }
    }


    /**
     * [最终版] 构建G代码字符串。
     * 该版本执行纯粹的坐标映射，将图像像素位置直接转换为平台的绝对物理坐标，
     * 忽略任何额外的偏移量 (startX, startY 被视为 0)。
     */
    private static String buildGCode(List<List<Point>> paths, Size imageSize,
                                     double targetWidth, double targetHeight,
                                     double startX, double startY, // 参数保留以兼容主函数，但内部不使用
                                     int laserPower) {
        StringBuilder gcode = new StringBuilder();
        // G代码标准头部
        gcode.append("G21 ; Set units to millimeters\n");
        gcode.append("G90 ; Use absolute coordinates\n");
        gcode.append("M4 S0 ; Use dynamic laser mode, power off\n\n");

        // --- [核心修改] 纯粹的坐标系映射 ---
        // 计算X和Y方向各自的缩放比例
        double scaleX = targetWidth / imageSize.width;
        double scaleY = targetHeight / imageSize.height;
        // --- 映射关系确定 ---

        for (List<Point> path : paths) {
            Point startPointImg = path.get(0);

            // 将图像坐标(左上角为0,0)直接转换为机器的绝对坐标(左下角为0,0)
            double machineStartX = startPointImg.x * scaleX;
            double machineStartY = targetHeight - (startPointImg.y * scaleY); // Y轴反转

            // 快速移动到路径起点
            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", machineStartX, machineStartY));

            // 设置功率，然后开始雕刻
            gcode.append(String.format(Locale.US, "S%d\n", laserPower));
            gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineStartX, machineStartY)); // 移动到同一位置以启动M4

            // 沿着路径移动
            for (int i = 1; i < path.size(); i++) {
                Point pImg = path.get(i);
                double machineX = pImg.x * scaleX;
                double machineY = targetHeight - (pImg.y * scaleY);
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", machineX, machineY));
            }
            // 路径结束，关闭激光
            gcode.append("S0\n\n");
        }

        // G代码标准尾部
        gcode.append("M5 ; Turn off laser module\n");
        gcode.append("G0 X0 Y0 ; Return to work origin (0,0)\n"); // 任务结束后返回物理原点

        return gcode.toString();
    }
    /**
     * [优化] 使用最近邻算法优化路径顺序，减少空载移动。
     */
    private static List<List<Point>> optimizePathOrder(List<List<Point>> paths) {
        if (paths.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Point>> sortedPaths = new ArrayList<>();
        LinkedList<List<Point>> remainingPaths = new LinkedList<>(paths);

        // 从离(0,0)最近的路径开始
        Point currentLocation = new Point(0, 0);

        while (!remainingPaths.isEmpty()) {
            List<Point> bestPath = null;
            double minDistance = Double.MAX_VALUE;
            boolean reversePath = false;

            for (List<Point> path : remainingPaths) {
                // 检查到路径起点的距离
                double distToStart = distanceSq(currentLocation, path.get(0));
                if (distToStart < minDistance) {
                    minDistance = distToStart;
                    bestPath = path;
                    reversePath = false;
                }
                // 检查到路径终点的距离（如果反转路径会更优）
                double distToEnd = distanceSq(currentLocation, path.get(path.size() - 1));
                if (distToEnd < minDistance) {
                    minDistance = distToEnd;
                    bestPath = path;
                    reversePath = true;
                }
            }

            remainingPaths.remove(bestPath);
            if (reversePath) {
                Collections.reverse(bestPath);
            }
            sortedPaths.add(bestPath);
            currentLocation = bestPath.get(bestPath.size() - 1);
        }
        return sortedPaths;
    }

    private static double distanceSq(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return dx * dx + dy * dy;
    }


    /**
     * 内部静态类，用于路径追踪。封装了所有追踪逻辑。
     */
    private static class SkeletonTracerV2 {

        public static List<List<Point>> trace(Mat skeletonMat) {
            Mat nodeMat = Mat.zeros(skeletonMat.size(), CvType.CV_8UC1);
            Map<Point, List<Point>> nodeNeighbors = findNodes(skeletonMat, nodeMat);

            List<List<Point>> paths = new ArrayList<>();
            Set<Point> visitedPixels = new HashSet<>();

            for (Point startNode : nodeNeighbors.keySet()) {
                for (Point neighbor : nodeNeighbors.get(startNode)) {
                    if (!visitedPixels.contains(neighbor)) {
                        List<Point> path = new ArrayList<>();
                        path.add(startNode);
                        visitedPixels.add(startNode);

                        List<Point> segment = traceSegment(neighbor, startNode, skeletonMat, nodeMat, visitedPixels);
                        path.addAll(segment);
                        paths.add(path);
                    }
                }
            }
            nodeMat.release();
            return paths;
        }

        private static List<Point> traceSegment(Point startPixel, Point prevPixel, Mat skeleton, Mat nodeMat, Set<Point> visited) {
            List<Point> segment = new ArrayList<>();
            Point current = startPixel;
            Point prev = prevPixel;

            while (current != null && !visited.contains(current)) {
                visited.add(current);
                segment.add(current);

                if (nodeMat.get((int)current.y, (int)current.x)[0] == 255) {
                    break;
                }

                List<Point> neighbors = findNeighborsForPixel(current, skeleton);
                Point next = null;
                for (Point n : neighbors) {
                    if (!n.equals(prev)) {
                        next = n;
                        break;
                    }
                }
                prev = current;
                current = next;
            }
            return segment;
        }

        private static Map<Point, List<Point>> findNodes(Mat skeleton, Mat nodeMat) {
            Map<Point, List<Point>> nodeNeighbors = new HashMap<>();
            for (int y = 0; y < skeleton.rows(); y++) {
                for (int x = 0; x < skeleton.cols(); x++) {
                    if (skeleton.get(y, x)[0] == 255) {
                        Point p = new Point(x, y);
                        List<Point> neighbors = findNeighborsForPixel(p, skeleton);
                        if (neighbors.size() != 2) {
                            nodeMat.put(y, x, (byte)255);
                            nodeNeighbors.put(p, neighbors);
                        }
                    }
                }
            }
            return nodeNeighbors;
        }

        private static List<Point> findNeighborsForPixel(Point p, Mat skeleton) {
            List<Point> neighbors = new ArrayList<>();
            int x = (int)p.x;
            int y = (int)p.y;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    int ny = y + i;
                    int nx = x + j;
                    if (ny >= 0 && ny < skeleton.rows() && nx >= 0 && nx < skeleton.cols() &&
                            skeleton.get(ny, nx)[0] == 255) {
                        neighbors.add(new Point(nx, ny));
                    }
                }
            }
            return neighbors;
        }
    }


    /**
     * [全新辅助方法] 从骨架图中追踪出所有连续路径。
     *
     * @param skeletonMat 单像素宽的骨架二值图 (白色为路径)
     * @return 一个包含多个路径的列表，每个路径是一个有序的点列表。
     */
    private static List<List<Point>> tracePathsFromSkeleton(Mat skeletonMat) {
        List<List<Point>> allPaths = new ArrayList<>();
        Mat visited = Mat.zeros(skeletonMat.size(), CvType.CV_8U); // 用于标记已访问的像素

        for (int y = 0; y < skeletonMat.rows(); y++) {
            for (int x = 0; x < skeletonMat.cols(); x++) {
                // 只处理未访问过的骨架点
                if (skeletonMat.get(y, x)[0] == 255 && visited.get(y, x)[0] == 0) {
                    int neighbors = countNeighbors(skeletonMat, x, y);

                    // 从端点(neighbor=1)或孤立点(neighbor=0)开始追踪新路径
                    // 同时也处理闭环中的任意一点（neighbor=2）
                    if (neighbors != 2) {
                        List<Point> path = new ArrayList<>();
                        Point startPoint = new Point(x, y);
                        trace(startPoint, skeletonMat, visited, path);
                        if (!path.isEmpty()) {
                            allPaths.add(path);
                        }
                    }
                }
            }
        }

        // 再次遍历，确保所有闭环路径都被找到
        for (int y = 0; y < skeletonMat.rows(); y++) {
            for (int x = 0; x < skeletonMat.cols(); x++) {
                if (skeletonMat.get(y, x)[0] == 255 && visited.get(y, x)[0] == 0) {
                    List<Point> path = new ArrayList<>();
                    Point startPoint = new Point(x, y);
                    trace(startPoint, skeletonMat, visited, path);
                    if (!path.isEmpty()) {
                        allPaths.add(path);
                    }
                }
            }
        }

        visited.release();
        return allPaths;
    }

    /**
     * 递归或迭代地追踪路径
     */
    private static void trace(Point startPoint, Mat skeleton, Mat visited, List<Point> path) {
        Stack<Point> stack = new Stack<>();
        stack.push(startPoint);

        while (!stack.isEmpty()) {
            Point currentPoint = stack.pop();
            int x = (int) currentPoint.x;
            int y = (int) currentPoint.y;

            if (visited.get(y, x)[0] == 255) {
                continue;
            }

            visited.put(y, x, (byte) 255);
            path.add(currentPoint);

            int neighborsCount = countNeighbors(skeleton, x, y);
            // 在分叉点处停止当前路径段的追踪
            if (neighborsCount > 2 && path.size() > 1) {
                // 将分叉点本身作为路径的终点，然后从其邻居重新开始新路径
                // (由外层循环处理)
                return;
            }

            // 寻找下一个未访问的邻居
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    int nx = x + j;
                    int ny = y + i;

                    if (nx >= 0 && nx < skeleton.cols() && ny >= 0 && ny < skeleton.rows() &&
                            skeleton.get(ny, nx)[0] == 255 && visited.get(ny, nx)[0] == 0) {
                        stack.push(new Point(nx, ny));
                    }
                }
            }
        }
    }

    /**
     * 计算一个点在骨架上的8邻域内的邻居数量
     */
    private static int countNeighbors(Mat skeleton, int x, int y) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int nx = x + j;
                int ny = y + i;
                if (nx >= 0 && nx < skeleton.cols() && ny >= 0 && ny < skeleton.rows() &&
                        skeleton.get(ny, nx)[0] == 255) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * [可选优化] 对路径进行排序，以减少激光头的空跑行程。
     * 使用简单的最近邻方法对整个路径列表进行排序。
     */
    private static List<List<Point>> sortPaths(List<List<Point>> paths, Point startFrom) {
        if (paths.isEmpty()) {
            return paths;
        }
        List<List<Point>> sorted = new ArrayList<>();
        List<List<Point>> remaining = new ArrayList<>(paths);
        Point currentLocation = startFrom;

        while (!remaining.isEmpty()) {
            int bestIndex = -1;
            double minDistance = Double.MAX_VALUE;

            for (int i = 0; i < remaining.size(); i++) {
                List<Point> path = remaining.get(i);
                Point pathStart = path.get(0);
                Point pathEnd = path.get(path.size() - 1);

                double distToStart = Math.hypot(currentLocation.x - pathStart.x, currentLocation.y - pathStart.y);
                double distToEnd = Math.hypot(currentLocation.x - pathEnd.x, currentLocation.y - pathEnd.y);

                if (distToStart < minDistance) {
                    minDistance = distToStart;
                    bestIndex = i;
                }
                if (distToEnd < minDistance) {
                    minDistance = distToEnd;
                    bestIndex = i;
                }
            }

            List<Point> bestPath = remaining.remove(bestIndex);
            Point pathStart = bestPath.get(0);
            double distToStart = Math.hypot(currentLocation.x - pathStart.x, currentLocation.y - pathStart.y);
            double distToEnd = Math.hypot(currentLocation.x - bestPath.get(bestPath.size() - 1).x, currentLocation.y - bestPath.get(bestPath.size() - 1).y);

            if (distToEnd < distToStart) {
                Collections.reverse(bestPath);
            }

            sorted.add(bestPath);
            currentLocation = bestPath.get(bestPath.size() - 1);
        }
        return sorted;
    }

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

    // 在类中添加合并函数
    private static List<List<Point>> mergeClosePaths(List<List<Point>> paths, double maxDistance) {
        boolean merged;
        do {
            merged = false;
            outer:
            for (int i = 0; i < paths.size(); i++) {
                List<Point> pathA = paths.get(i);
                if (isClosed(pathA, 2.0)) continue; // 跳过闭合路径
                Point aStart = pathA.get(0);
                Point aEnd = pathA.get(pathA.size() - 1);
                Point aEndPrev = pathA.size() > 1 ? pathA.get(pathA.size() - 2) : aEnd;
                Point aStartNext = pathA.size() > 1 ? pathA.get(1) : aStart;
                for (int j = 0; j < paths.size(); j++) {
                    if (i == j) continue;
                    List<Point> pathB = paths.get(j);
                    if (isClosed(pathB, 2.0)) continue; // 跳过闭合路径
                    Point bStart = pathB.get(0);
                    Point bEnd = pathB.get(pathB.size() - 1);
                    Point bStartNext = pathB.size() > 1 ? pathB.get(1) : bStart;
                    Point bEndPrev = pathB.size() > 1 ? pathB.get(pathB.size() - 2) : bEnd;

                    // 只合并开放路径，且方向夹角小于45°
                    if (distance(aEnd, bStart) < maxDistance && angleDiff(aEndPrev, aEnd, bStart, bStartNext) < 45) {
                        // A尾接B头
                        pathA.addAll(pathB);
                        paths.remove(j);
                        merged = true;
                        break outer;
                    } else if (distance(aEnd, bEnd) < maxDistance && angleDiff(aEndPrev, aEnd, bEnd, bEndPrev) < 45) {
                        // A尾接B尾（B反转）
                        Collections.reverse(pathB);
                        pathA.addAll(pathB);
                        paths.remove(j);
                        merged = true;
                        break outer;
                    } else if (distance(aStart, bEnd) < maxDistance && angleDiff(aStartNext, aStart, bEnd, bEndPrev) < 45) {
                        // B尾接A头
                        pathB.addAll(pathA);
                        paths.remove(i);
                        merged = true;
                        break outer;
                    } else if (distance(aStart, bStart) < maxDistance && angleDiff(aStartNext, aStart, bStart, bStartNext) < 45) {
                        // A头接B头（A反转）
                        Collections.reverse(pathA);
                        pathA.addAll(pathB);
                        paths.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        } while (merged);
        // 合并后自动闭合首尾距离极小且方向接近180°的路径
        for (List<Point> path : paths) {
            if (path.size() > 3) {
                double d = distance(path.get(0), path.get(path.size() - 1));
                double ang = angleDiff(path.get(1), path.get(0), path.get(path.size() - 2), path.get(path.size() - 1));
                if (d < 1.0 && Math.abs(ang - 180) < 20) {
                    // 可以闭合：把首点加到末尾
                    path.add(path.get(0));
                }
            }
        }
        return paths;
    }

    private static boolean isClosed(List<Point> path, double threshold) {
        if (path.size() < 3) return false;
        return distance(path.get(0), path.get(path.size() - 1)) < threshold;
    }

    private static double angleDiff(Point p1, Point p2, Point q1, Point q2) {
        // 计算向量p1->p2和q1->q2的夹角（度）
        double vx1 = p2.x - p1.x;
        double vy1 = p2.y - p1.y;
        double vx2 = q2.x - q1.x;
        double vy2 = q2.y - q1.y;
        double dot = vx1 * vx2 + vy1 * vy2;
        double norm1 = Math.sqrt(vx1 * vx1 + vy1 * vy1);
        double norm2 = Math.sqrt(vx2 * vx2 + vy2 * vy2);
        if (norm1 == 0 || norm2 == 0) return 180.0;
        double cos = dot / (norm1 * norm2);
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return Math.toDegrees(Math.acos(cos));
    }

    private static double distance(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 适用于白底黑线的干净轮廓图，直接提取轮廓并生成G代码。
     */
    public static String generateGCodeFromCleanContours(
            Mat image,
            double targetWidth, double targetHeight,
            double startX, double startY,
            int laserPower,
            double simplifyEpsilon,
            boolean invertBinary,
            boolean saveDebugImages
    ) {
        // 1. 灰度化
        Mat gray = new Mat();
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = image.clone();
        }

        // 2. 二值化
        Mat binary = new Mat();
        int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
        Imgproc.threshold(gray, binary, 0, 255, thresholdType | Imgproc.THRESH_OTSU);

        // 3. 可选：调试提示
        if (saveDebugImages) {
            System.out.println("[DEBUG] saveDebugImages is true, but no context provided. Skipping image save.");
        }

        // 4. 轮廓提取
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        // 5. 轮廓简化
        List<MatOfPoint2f> simplifiedContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, simplifyEpsilon, false);
            if (approx.total() > 1) {
                simplifiedContours.add(approx);
            }
            contour2f.release();
        }

        // 6. G代码生成
        StringBuilder gcode = new StringBuilder();
        gcode.append("M4 S0\n");
        double scaleX = targetWidth / binary.width();
        double scaleY = targetHeight / binary.height();

        for (MatOfPoint2f contour : simplifiedContours) {
            Point[] pts = contour.toArray();
            if (pts.length < 2) continue;
            // 起点
            double x0 = startX + pts[0].x * scaleX;
            double y0 = startY + (binary.height() - 1 - pts[0].y) * scaleY;
            gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", x0, y0));
            gcode.append(String.format(Locale.US, "G1 S%d\n", laserPower));
            for (int i = 1; i < pts.length; i++) {
                double xi = startX + pts[i].x * scaleX;
                double yi = startY + (binary.height() - 1 - pts[i].y) * scaleY;
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", xi, yi));
            }
            // 闭合路径
            if (Math.hypot(pts[0].x - pts[pts.length - 1].x, pts[0].y - pts[pts.length - 1].y) < 2.0) {
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", x0, y0));
            }
            gcode.append("G1 S0\n");
        }
        gcode.append("M5\n");
        gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", startX, startY));

        // 释放资源
        gray.release();
        binary.release();
        hierarchy.release();
        for (MatOfPoint2f c : simplifiedContours) c.release();

        return gcode.toString();
    }

    /**
     * 沿着所有黑色像素点走激光，适用于白底黑线的单像素线稿图。
     */
    public static String generateGCodeFollowBlackPixels(
            Mat image,
            double targetWidth, double targetHeight,
            double startX, double startY,
            int laserPower,
            boolean invertBinary,
            boolean saveDebugImages
    ) {
        // 1. 灰度化
        Mat gray = new Mat();
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = image.clone();
        }

        // 2. 二值化
        Mat binary = new Mat();
        int thresholdType = invertBinary ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY;
        Imgproc.threshold(gray, binary, 0, 255, thresholdType | Imgproc.THRESH_OTSU);

        // 3. 可选：调试提示
        if (saveDebugImages) {
            System.out.println("[DEBUG] saveDebugImages is true, but no context provided. Skipping image save.");
        }

        // 4. 计算等比缩放和居中偏移
        double imageWidth = binary.width();
        double imageHeight = binary.height();
        double scale = Math.min(targetWidth / imageWidth, targetHeight / imageHeight);
        double offsetX = startX + (targetWidth - imageWidth * scale) / 2.0;
        double offsetY = startY + (targetHeight - imageHeight * scale) / 2.0;

        // 5. 按行合并连续黑点为线段雕刻
        StringBuilder gcode = new StringBuilder();
        gcode.append("M4 S0\n");
        for (int y = 0; y < binary.height(); y++) {
            int x = 0;
            while (x < binary.width()) {
                // 找到第一个黑点
                while (x < binary.width() && binary.get(y, x)[0] != 0) x++;
                if (x == binary.width()) break;
                int startXPix = x;
                // 找到连续黑点的末尾
                while (x < binary.width() && binary.get(y, x)[0] == 0) x++;
                int endXPix = x - 1;
                // 生成一条线段的G代码
                double gx0 = offsetX + startXPix * scale;
                double gy = offsetY + (imageHeight - 1 - y) * scale;
                double gx1 = offsetX + endXPix * scale;
                gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", gx0, gy));
                gcode.append(String.format(Locale.US, "G1 S%d\n", laserPower));
                gcode.append(String.format(Locale.US, "G1 X%.3f Y%.3f\n", gx1, gy));
                gcode.append("G1 S0\n");
            }
        }
        gcode.append("M5\n");
        gcode.append(String.format(Locale.US, "G0 X%.3f Y%.3f\n", startX, startY));

        // 释放资源
        gray.release();
        binary.release();

        return gcode.toString();
    }
}