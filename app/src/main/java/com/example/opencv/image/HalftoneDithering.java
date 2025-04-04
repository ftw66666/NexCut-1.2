package com.example.opencv.image;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HalftoneDithering {
    public static Bitmap applyHalftone(Bitmap inputBitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(inputBitmap, src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY); // 转换为灰度图

        int width = src.cols();
        int height = src.rows();
        Mat halftone = new Mat(height, width, CvType.CV_8UC1);

        int[][] bayerMatrix = {
                {  0,  8,  2, 10 },
                { 12,  4, 14,  6 },
                {  3, 11,  1,  9 },
                { 15,  7, 13,  5 }
        };

        int matrixSize = 4;
        int thresholdBase = 255 / (matrixSize * matrixSize);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double grayValue = src.get(y, x)[0];
                int threshold = bayerMatrix[y % matrixSize][x % matrixSize] * thresholdBase;
                halftone.put(y, x, grayValue < threshold ? 0 : 255);
            }
        }

        Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(halftone, outputBitmap);
        return outputBitmap;
    }

}
