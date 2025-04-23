package com.example.opencv.image;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class ImageProcessor {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * 将 Bitmap 转换为 Mat 对象。
     */
    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    /** 
     * 将 Mat 对象转换为 Bitmap。
     */
    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    /**
     * 将图像转换为灰度图。
     */
    public static Bitmap toGrayscale(Bitmap bitmap) {
        Mat mat = bitmapToMat(bitmap);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        return matToBitmap(mat);
    }

    /**
     * 对图像应用高斯模糊。
     * @param kernelSize 高斯核的大小，必须是奇数。
     */
    public static Bitmap applyGaussianBlur(Bitmap bitmap, int kernelSize) {
        Mat mat = bitmapToMat(bitmap);
        Imgproc.GaussianBlur(mat, mat, new Size(kernelSize, kernelSize), 0);
        return matToBitmap(mat);
    }

    /**
     * 对图像应用 Canny 边缘检测。
     * @param threshold1 第一个阈值。
     * @param threshold2 第二个阈值。
     */
    public static Bitmap applyCannyEdgeDetection(Bitmap bitmap, double threshold1, double threshold2) {
        Mat mat = bitmapToMat(bitmap);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(mat, mat, threshold1, threshold2);
        return matToBitmap(mat);
    }

    /**
     * 旋转图像指定角度。
     * @param angle 旋转角度（以度为单位）。
     */
    public static Bitmap rotateImage(Bitmap bitmap, double angle) {
        Mat mat = bitmapToMat(bitmap);
        double radians = Math.toRadians(angle);
        double cos = Math.abs(Math.cos(radians));
        double sin = Math.abs(Math.sin(radians));
        int newWidth = (int) (mat.cols() * cos + mat.rows() * sin);
        int newHeight = (int) (mat.cols() * sin + mat.rows() * cos);

        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new org.opencv.core.Point(mat.cols() / 2, mat.rows() / 2), angle, 1);
        double[][] rotationMatrixArray = new double[2][3];
        rotationMatrix.get(0, 0, rotationMatrixArray[0]);
        rotationMatrix.get(1, 0, rotationMatrixArray[1]);
        rotationMatrixArray[0][2] += (newWidth - mat.cols()) / 2;
        rotationMatrixArray[1][2] += (newHeight - mat.rows()) / 2;
        rotationMatrix.put(0, 0, rotationMatrixArray[0]);
        rotationMatrix.put(1, 0, rotationMatrixArray[1]);

        Mat rotatedMat = new Mat();
        Imgproc.warpAffine(mat, rotatedMat, rotationMatrix, new Size(newWidth, newHeight));
        return matToBitmap(rotatedMat);
    }

    /**
     * 裁剪图像。
     * @param bitmap 原始图像。
     * @param cropRect 用户选择的裁剪区域。
     * @return 裁剪后的图像。
     */
    public static Bitmap cropImage(Bitmap bitmap, Rect cropRect) {
        Mat mat = bitmapToMat(bitmap);
        Mat croppedMat = new Mat(mat, cropRect);
        return matToBitmap(croppedMat);
    }

    /**
     * 水平翻转图像。
     * @param bitmap 原始图像。
     * @return 水平翻转后的图像。
     */
    public static Bitmap flipImageHorizontally(Bitmap bitmap) {
        Mat mat = bitmapToMat(bitmap);
        Mat flippedMat = new Mat();
        Core.flip(mat, flippedMat, 1);
        return matToBitmap(flippedMat);
    }

    /**
     * 垂直翻转图像。
     * @param bitmap 原始图像。
     * @return 垂直翻转后的图像。
     */
    public static Bitmap flipImageVertically(Bitmap bitmap) {
        Mat mat = bitmapToMat(bitmap);
        Mat flippedMat = new Mat();
        Core.flip(mat, flippedMat, 0);
        return matToBitmap(flippedMat);
    }

}
