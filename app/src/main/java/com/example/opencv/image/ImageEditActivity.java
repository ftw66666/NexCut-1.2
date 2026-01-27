package com.example.opencv.image;

import static android.content.ContentValues.TAG;
import static com.example.opencv.MainActivity.CAPTURE_IMAGE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.opencv.BaseActivity;
import com.example.opencv.Constant;
import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.whiteboard.SettingActivity;
import com.example.opencv.webwhiteboard.WebWhiteBoardActivity;
// import com.example.opencv.whiteboard.WhiteboardActivity; // 已注释，被WebWhiteBoardActivity替代
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class  ImageEditActivity extends BaseActivity {
    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;

    private ImageView imageView;
    private Bitmap selectedBitmap;
    private Bitmap originalBitmap;
    private Bitmap originalBitmap1;
    private Bitmap filterBaseBitmap; // 亮度/对比度操作的起点图像
    private PhotoSelector photoSelector;

    private Uri imageUri;

    private Uri frameUri;

    public Toolbar toolbar;
    private SeekBar brightnessSeekBar;
    private SeekBar contrastSeekBar;
    private float brightnessValue = 0f; // 范围：-255 到 255
    private float contrastValue = 1f;   // 范围：0.1 到 3

    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    static {
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
    }

    // 新增：图层信息类
    public static class LayerInfo {
        public String filePath;
        public String printingMethod;
        public LayerInfo(String filePath, String printingMethod) {
            this.filePath = filePath;
            this.printingMethod = printingMethod;
        }
    }
    private List<LayerInfo> layerList = new ArrayList<>();
    // 多图层G代码生成
    public void multiLayerGCode() {
        if (layerList == null || layerList.isEmpty()) {
            Toast.makeText(this, R.string.No_layer_data, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = ProgressDialog.show(
                ImageEditActivity.this,
                "处理中(Processing)",
                "Generating multi-layer G-code, please wait…",
                true
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                StringBuilder allGCode = new StringBuilder();
                for (LayerInfo layer : layerList) {
                    Bitmap bitmap = BitmapFactory.decodeFile(layer.filePath);
                    if (bitmap == null) continue;

                    Mat mat = ImageProcessor.bitmapToMat(bitmap);
                    String gcode;
                    // 根据 printingMethod 选择不同的G代码生成方式
                    if ("SCAN".equalsIgnoreCase(layer.printingMethod)) {
                        gcode = GCode.generateGCode0(
                                mat,
                                6, // rho
                                Constant.PrintWidth,
                                Constant.PrintHeight,
                                Constant.PrintStartX,
                                Constant.PrintStartY,
                                20 // laserPower
                        );
                    } else if ("VECTOR".equalsIgnoreCase(layer.printingMethod)) {
                        gcode = GCode.generateGCodeFollowBlackPixels(
                                mat,
                                Constant.PrintWidth,
                                Constant.PrintHeight,
                                Constant.PrintStartX,
                                Constant.PrintStartY,
                                100, // cutPower
                                true,
                                true
                        );
                    } else {
                        // 默认用光栅扫描
                        gcode = GCode.generateGCode0(
                                mat,
                                6,
                                Constant.PrintWidth,
                                Constant.PrintHeight,
                                Constant.PrintStartX,
                                Constant.PrintStartY,
                                20
                        );
                    }
                    allGCode.append(R.string.layer).append(layer.printingMethod).append("\n");
                    allGCode.append(gcode).append("\n");
                }

                handler.post(() -> {
                    progressDialog.dismiss();
                    showSaveDialog(ImageEditActivity.this, allGCode.toString());
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ImageEditActivity.this, R.string.Processing_failed + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_imageedit_new);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        imageView = findViewById(R.id.imageView);
        brightnessSeekBar = findViewById(R.id.seekBar1);
        contrastSeekBar = findViewById(R.id.seekBar2);

        brightnessSeekBar.setMax(200);
        brightnessSeekBar.setMin(0);
        brightnessSeekBar.setProgress(100);
        contrastSeekBar.setMax(200);
        contrastSeekBar.setMin(0);
        contrastSeekBar.setProgress(100);

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightnessSeekBar.setMax(200);
                brightnessValue = progress - 100;
//                applyFilters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                applyFilters();
            }
        });

        contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrastValue = progress / 100f; // 范围从 0.1 到 3.0
//                applyFilters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                applyFilters();
            }
        });


//        // **应用启动时复制 .nc 预制文件到可访问目录**
//        GCodeRead.copyNcFilesToStorage(this);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // 初始化 UI 组件
        imageView = findViewById(R.id.imageView);
//        Button btnSelect = findViewById(R.id.btnSelect);
//        Button btnCapture = findViewById(R.id.btnCapture);
        Button btnGrayscale = findViewById(R.id.btnGrayscale);
        Button btnBinary = findViewById(R.id.btnBinary);
        Button btnInvert = findViewById(R.id.btnInvert);
        Button btnBlur = findViewById(R.id.btnBlur);
        Button btnEdge = findViewById(R.id.btnEdge);
        Button btnRotate = findViewById(R.id.btnRotate);
        Button btnHalftone = findViewById(R.id.btnHalftone);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button GCodeGen = findViewById(R.id.GCodeGen);
//        Button GCodeRead = findViewById(R.id.readGCode);
        Button Graffiti = findViewById(R.id.graffiti);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnVerticalFlip = findViewById(R.id.btnVerticalFlip);
        Button btnHorizontalFlip = findViewById(R.id.btnHorizontalFlip);
        Button btnBack = findViewById(R.id.BackToOrigin);
        InitialImage();
        graffitiToGCode();


//        //OpenCV初始化
//        if (OpenCVLoader.initDebug()) {
//            Log.i(TAG, "OpenCV loaded successfully");
//        } else {
//            Log.e(TAG, "OpenCV initialization failed!");
//            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
//            return;
//        }

        photoSelector = new PhotoSelector();

        // 设置按钮点击事件
//        btnSelect.setOnClickListener(v -> photoSelector.selectFromGallery(ImageEditActivity.this));
//        btnCapture.setOnClickListener(v -> photoSelector.capturePhoto(ImageEditActivity.this, getApplicationContext()));
        btnBinary.setOnClickListener(v -> applyBinary());
        btnInvert.setOnClickListener(v -> applyInvert());
        btnGrayscale.setOnClickListener(v -> applyGrayscale());
        btnBlur.setOnClickListener(v -> applyBlur());
        btnEdge.setOnClickListener(v -> applyEdgeDetection());
        btnRotate.setOnClickListener(v -> applyRotation());
        btnHalftone.setOnClickListener(v -> applyHalftone());
        btnCrop.setOnClickListener(v -> applyCrop());
        GCodeGen.setOnClickListener(v -> generateCode());
//        GCodeRead.setOnClickListener(v -> readGCode());
        Graffiti.setOnClickListener(v -> graffiti());
        btnVerticalFlip.setOnClickListener(v -> verticalFlip());
        btnHorizontalFlip.setOnClickListener(v -> horizontalFlip());
        btnSave.setOnClickListener(v -> saveOnAssets());
        btnBack.setOnClickListener(v -> BackToOrigin());
        requestAppPermissions();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String layerDataJson = getIntent().getStringExtra("layerData");
        if (layerDataJson != null) {
            try {
                JSONArray arr2d = new JSONArray(layerDataJson);
                for (int i = 0; i < arr2d.length(); i++) {
                    JSONArray item = arr2d.getJSONArray(i);
                    String filePath = item.getString(0);
                    String printingMethod = item.getString(1);
                    layerList.add(new LayerInfo(filePath, printingMethod));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 自动处理多图层G代码
        if (!layerList.isEmpty()) {
            multiLayerGCode();
        }
        // 你可以在后续代码中遍历 layerList，按顺序处理每个图层
    }

    // 加载 Toolbar 菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

//    // 监听 Toolbar 按钮点击事件
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.User_image) {
//            Toast.makeText(this, getString(R.string.community_feature_in_progress), Toast.LENGTH_SHORT).show();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
    private void InitialImage() {
        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString == null) return;

        imageUri = Uri.parse(uriString);
        try {
            // 使用ContentResolver直接打开流
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap tempbitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            selectedBitmap = compressBitmap(tempbitmap);
            originalBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
            imageView.setImageBitmap(selectedBitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.Image_failed_to_load, Toast.LENGTH_SHORT).show();
        }
        filterBaseBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
    }

    private void BackToOrigin() {
        if (originalBitmap != null) {
            selectedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true); // 复制原始位图
            imageView.setImageBitmap(selectedBitmap);
            contrastSeekBar.setProgress(100);
            brightnessSeekBar.setProgress(100);
        } else {
            Toast.makeText(this, R.string.No_image_restoration, Toast.LENGTH_SHORT).show();
        }
        filterBaseBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);

    }

    /**
     * 应用亮度、对比度
     */
    private void applyFilters() {
        if (filterBaseBitmap == null) return; // 基于滤镜起点图像

        Bitmap filteredBitmap = Bitmap.createBitmap(
                filterBaseBitmap.getWidth(),
                filterBaseBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        float contrastScale = contrastValue;
        float contrastTranslate = (1 - contrastScale) * 128;

        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrastScale, 0, 0, 0, contrastTranslate,
                0, contrastScale, 0, 0, contrastTranslate,
                0, 0, contrastScale, 0, contrastTranslate,
                0, 0, 0, 1, 0
        });

        ColorMatrix brightnessMatrix = new ColorMatrix(new float[]{
                1, 0, 0, 0, brightnessValue,
                0, 1, 0, 0, brightnessValue,
                0, 0, 1, 0, brightnessValue,
                0, 0, 0, 1, 0
        });

        brightnessMatrix.postConcat(contrastMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(brightnessMatrix));

        Canvas canvas = new Canvas(filteredBitmap);
        canvas.drawBitmap(filterBaseBitmap, 0, 0, paint);

        imageView.setImageBitmap(filteredBitmap);
        selectedBitmap = filteredBitmap;
    }




    /**
     * 应用灰度处理
     */
    private void applyGrayscale() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.toGrayscale(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
            updateFilterBase();

        }
    }

    /**
     * 应用二值化处理
     */
    public void applyBinary() {
        if (selectedBitmap != null) {
            //Toast.makeText(this, "运算中", Toast.LENGTH_LONG).show();
            // 将Bitmap转换为Mat
            Mat mat = new Mat();
            Utils.bitmapToMat(selectedBitmap, mat);

            // 将图像转换为灰度图
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);

            // 进行二值化处理
            Mat binaryMat = new Mat();
            Imgproc.threshold(grayMat, binaryMat, 170, 255, Imgproc.THRESH_BINARY);

            // 将处理后的Mat转换回Bitmap
            Bitmap binaryBitmap = Bitmap.createBitmap(binaryMat.cols(), binaryMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(binaryMat, binaryBitmap);

            // 释放Mat对象的内存
            mat.release();
            grayMat.release();
            binaryMat.release();

            selectedBitmap = binaryBitmap;
            imageView.setImageBitmap(selectedBitmap);
            updateFilterBase();

        }
    }

    /**
     * 应用反色处理
     */
    private void applyInvert() {
        if (selectedBitmap != null) {
            // 创建反色颜色矩阵
            ColorMatrix colorMatrix = new ColorMatrix(new float[] {
                    -1,  0,  0, 0, 255, // 红色通道反色
                    0, -1,  0, 0, 255, // 绿色通道反色
                    0,  0, -1, 0, 255, // 蓝色通道反色
                    0,  0,  0, 1,   0  // Alpha通道保持不变
            });

            // 应用颜色矩阵
            Bitmap invertedBitmap = Bitmap.createBitmap(
                    selectedBitmap.getWidth(),
                    selectedBitmap.getHeight(),
                    selectedBitmap.getConfig()
            );

            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

            Canvas canvas = new Canvas(invertedBitmap);
            canvas.drawBitmap(selectedBitmap, 0, 0, paint);

            // 更新图像
            selectedBitmap = invertedBitmap;
            imageView.setImageBitmap(selectedBitmap);
        } else {
            Toast.makeText(this, R.string.image_first, Toast.LENGTH_SHORT).show();
        }
        updateFilterBase();

    }

    /**
     * 应用高斯模糊
     */
    private void applyBlur() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.applyGaussianBlur(selectedBitmap, 15);
            imageView.setImageBitmap(selectedBitmap);
        }
        updateFilterBase();

    }

    /**
     * 应用 Canny 边缘检测
     */
    private void applyEdgeDetection() {
        if (selectedBitmap == null) {
            Log.e("ContourError", "selectedBitmap is null before processing.");
            return;
        }

        // 1. 将 Bitmap 转换为 Mat
        Mat originalMat = new Mat();
        Utils.bitmapToMat(selectedBitmap, originalMat);

        // 2. 放大图像（例如放大 2 倍）
        int targetWidth = originalMat.cols() * 2;
        int targetHeight = originalMat.rows() * 2;
        Mat resizedMat = new Mat();
        Imgproc.resize(originalMat, resizedMat, new Size(targetWidth, targetHeight), 0, 0, Imgproc.INTER_LINEAR);

        // 3. 转换为灰度图
        Mat grayMat = new Mat();
        Imgproc.cvtColor(resizedMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

        // 4. Canny 边缘检测
        Mat edgesMat = new Mat();
        Imgproc.Canny(grayMat, edgesMat, 125, 250);

        // 5. 查找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgesMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // 6. 使用 approxPolyDP 对每个轮廓进行逼近平滑
        List<MatOfPoint> approxContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double epsilon = 0.01 ;
            Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);
            approxContours.add(new MatOfPoint(approxCurve.toArray()));
            contour2f.release();
            approxCurve.release();
            contour.release();
        }

        // 7. 创建白色背景图像用于绘制轮廓
        Mat wireframeMat = new Mat(edgesMat.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        Scalar contourColor = new Scalar(0, 0, 0); // 黑色
        int contourThickness = 2; // 线宽为 2 像素

        // 8. 绘制轮廓
        for (int i = 0; i < approxContours.size(); i++) {
            Imgproc.drawContours(wireframeMat, approxContours, i, contourColor, contourThickness, Imgproc.LINE_AA);
        }

        // 9. 转换回 Bitmap 显示
        Bitmap wireframeBitmap = Bitmap.createBitmap(wireframeMat.cols(), wireframeMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(wireframeMat, wireframeBitmap);

        // 10. 显示到 ImageView
        if (imageView != null) {
            imageView.setImageBitmap(wireframeBitmap);
        }

        // 11. 更新 selectedBitmap
        selectedBitmap = wireframeBitmap;

        // 12. 释放资源
        originalMat.release();
        resizedMat.release();
        grayMat.release();
        edgesMat.release();
        hierarchy.release();
        wireframeMat.release();
        for (MatOfPoint contour : approxContours) {
            contour.release();
        }
    }
//    private  void applyEdgeDetection() {
//        if (selectedBitmap == null) {
//            Log.e("ContourError", "selectedBitmap is null before processing.");
//            return;
//        }
//
//        originalBitmap1 = selectedBitmap;
//
//        // --- 参数控制 ---
//        boolean applyChaikin = true; // 是否应用Chaikin平滑
//        int chaikinIterations = 3;   // Chaikin迭代次数 (1-3次效果较好)
//        double chaikinRatio = 0.25;  // Chaikin标准比率
//
//        // 可选：在Canny之前进行高斯模糊，有助于从源头减少噪点，可能使Canny边缘更平滑
//        boolean applyGaussianBlurToSource = false; // 如果Canny边缘本身就很粗糙，可以尝试开启
//        // Mat sourceForCannyMat = new Mat();
//        // Utils.bitmapToMat(selectedBitmap, sourceForCannyMat);
//        // if (applyGaussianBlurToSource) {
//        //     Imgproc.GaussianBlur(sourceForCannyMat, sourceForCannyMat, new Size(3,3), 0);
//        // }
//        // Bitmap bitmapForCanny = Bitmap.createBitmap(sourceForCannyMat.cols(), sourceForCannyMat.rows(), Bitmap.Config.ARGB_8888);
//        // Utils.matToBitmap(sourceForCannyMat, bitmapForCanny);
//        // sourceForCannyMat.release();
//        // Bitmap cannyEdgesBitmap = ImageProcessor.applyCannyEdgeDetection(bitmapForCanny, 107, 250);
//        // if (applyGaussianBlurToSource && bitmapForCanny != selectedBitmap) bitmapForCanny.recycle();
//
//        Bitmap cannyEdgesBitmap = ImageProcessor.applyCannyEdgeDetection(selectedBitmap, 100, 200);
//
//
//        if (cannyEdgesBitmap == null) {
//            Log.e("ContourError", "Canny edge detection returned null bitmap.");
//            return;
//        }
//
//        Mat edgesMat = new Mat();
//        Utils.bitmapToMat(cannyEdgesBitmap, edgesMat);
//
//        if (edgesMat.type() != CvType.CV_8UC1) {
//            if (edgesMat.channels() > 1) {
//                Imgproc.cvtColor(edgesMat, edgesMat, Imgproc.COLOR_RGBA2GRAY); // 或其他适当的转换
//            }
//        }
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        try {
//            // 使用 CHAIN_APPROX_NONE 获取更多原始点，为平滑提供基础
//            Imgproc.findContours(edgesMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
//        } catch (Exception e) {
//            Log.e("ContourError", "Error in findContours: " + e.getMessage());
//            edgesMat.release(); hierarchy.release(); return;
//        }
//
//// --- 轮廓平滑处理 ---
//        List<MatOfPoint> smoothedContours = new ArrayList<>();
//
//        for (MatOfPoint contour : contours) {
//            // 将 MatOfPoint 转换为 MatOfPoint2f（approxPolyDP 需要浮点坐标）
//            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
//            MatOfPoint2f approxCurve = new MatOfPoint2f();
//
//            // 设置逼近精度（值越小越接近原始轮廓，建议 1~5 之间）
//            double epsilon = 1; // 可调参数
//            Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);
//
//            // 将结果转回 MatOfPoint
//            MatOfPoint approxContour = new MatOfPoint(approxCurve.toArray());
//            smoothedContours.add(approxContour);
//
//            // 释放资源
//            contour2f.release();
//            approxCurve.release();
//            contour.release(); // 释放原始轮廓
//        }
//
//        contours.clear(); // 清空原始轮廓列表
//        contours.addAll(smoothedContours); // 用逼近后的轮廓替换
//
//
//        Mat wireframeMat = new Mat(edgesMat.size(), CvType.CV_8UC3, new Scalar(255, 255, 255)); // 白色背景
//
//        if (!contours.isEmpty()) {
//            Scalar contourColor = new Scalar(0, 0, 0); // 黑色轮廓
//            int contourThickness = 1;
//
//            for (int i = 0; i < contours.size(); i++) {
//                Imgproc.drawContours(wireframeMat, contours, i, contourColor, contourThickness,Imgproc.LINE_8);
//            }
//        } else {
//            Log.d("ContourDebug", "No contours found to draw.");
//        }
//
//        Bitmap wireframeBitmap = Bitmap.createBitmap(wireframeMat.cols(), wireframeMat.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(wireframeMat, wireframeBitmap);
//
//        if (imageView != null) {
//            imageView.setImageBitmap(wireframeBitmap);
//        }
//
//        selectedBitmap = wireframeBitmap;
//
//        edgesMat.release();
//        hierarchy.release();
//        wireframeMat.release();
//        for (MatOfPoint contour : contours) { // 释放平滑后的轮廓（或原始轮廓如果未平滑）
//            contour.release();
//        }
//        // if (cannyEdgesBitmap != null && !cannyEdgesBitmap.isRecycled()) cannyEdgesBitmap.recycle();
//
//        updateFilterBase();
//    }

    /**
     * 旋转图像 90 度
     */
    private void applyRotation() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.rotateImage(selectedBitmap, 90);
            imageView.setImageBitmap(selectedBitmap);
        }
        updateFilterBase();

    }

    /**
     * 裁剪图像（默认裁剪中间部分）等待修改为客户可选
     */
    private void applyCrop() {
        if (selectedBitmap != null) {
            // 创建一个临时文件，用于裁剪结果存储
            try {
                File tempFile = createImageFile(); // 创建临时文件
                Uri tempUri = Uri.fromFile(tempFile);

                // 启动 UCrop 裁剪界面
                UCrop.of(getImageUri(selectedBitmap), tempUri)
//                        .withAspectRatio(0,0)
                        .withMaxResultSize(800, 800)  // 设置最大裁剪结果大小
                        .start(this);

                InputStream inputStream = getContentResolver().openInputStream(tempUri);
                selectedBitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private Uri getImageUri(Bitmap bitmap) {
        // 将 bitmap 保存为临时文件并返回 Uri
        try {
            File tempFile = createImageFile();
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void horizontalFlip()
    {
        if(selectedBitmap != null)
        {
            selectedBitmap = ImageProcessor.flipImageHorizontally(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
        }
        updateFilterBase();

    }

    private void verticalFlip()
    {
        if(selectedBitmap != null)
        {
            selectedBitmap = ImageProcessor.flipImageVertically(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
        }
        updateFilterBase();

    }

    private void saveOnAssets() {
        String imageFileName = "PNG_" + System.currentTimeMillis() + "_" + "file:///android_asset/";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        /* 前缀 */
        /* 后缀 */
        /* 目录 */
    }



    private void generateCode() {
//        if (selectedBitmap != null) {
//            Mat m = ImageProcessor.bitmapToMat(selectedBitmap);
//            //rho:线密度
//            String gcode = GCode.cropGCode(m, 96,ImageEditActivity.this, Constant.PlatformWidth,Constant.PlatformHeight);
//            showSaveDialog(this, gcode); // 弹出文件名输入框
//        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == REQUEST_GALLERY && data != null) {
//                // 从相册获取图片
//                imageUri = data.getData();
//                try {
//                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
//                    imageView.setImageBitmap(selectedBitmap);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else if (requestCode == REQUEST_CAMERA) {
//                // 从相机拍摄获取图片
//                File imageFile = new File(photoSelector.getCurrentPhotoPath());
//                if (imageFile.exists()) {
//                    selectedBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
//                    imageView.setImageBitmap(selectedBitmap);
//
    ////                    // 保存图片到相册
    ////                    saveImageToGallery(imageFile);
//                }
//            }
//        }
//        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
//            // 获取裁剪后的图片
//            Uri resultUri = UCrop.getOutput(data);
//            if (resultUri != null) {
//                try {
//                    // 将裁剪后的图片加载到 ImageView 中
//                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
//                    imageView.setImageBitmap(selectedBitmap);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        } else if (resultCode == UCrop.RESULT_ERROR) {
//            // 错误处理
//            Throwable cropError = UCrop.getError(data);
//            if (cropError != null) {
//                Log.e(TAG, "Crop error: " + cropError.getMessage());
//            }
//        }
//    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                // 从相册获取图片
                imageUri = data.getData();
                if (imageUri != null) {
                    Picasso.get().load(imageUri).into(imageView);
                }
            } else if (requestCode == /*REQUEST_CAMERA*/CAPTURE_IMAGE && resultCode == RESULT_OK) {
//           从相机拍摄获取图片
//            File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo.jpg");
//            if (imageFile.exists()) {
//             使用 Picasso 加载原图
//                Picasso.get()
//                        .load(imageFile)
//                        .config(Bitmap.Config.ARGB_8888) // 提高质量
//                        .resize(1500, 1500) // 调整大小
//                        .centerInside()i
//                        .into(imageView);
//
//                    保存图片到相册
//               saveImageToGallery(imageFile);
//            }
                Picasso.get()
                        .load(imageUri)
                        .config(Bitmap.Config.ARGB_8888)
                        .into(imageView);
            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            // 获取裁剪后的图片
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                Picasso.get().load(resultUri).into(imageView);
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                    // ✅ 设置滤镜操作的基准图像
                    filterBaseBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);

                    // 显示裁剪后的图像
                    imageView.setImageBitmap(selectedBitmap);

                    // 重置亮度/对比度滑条为初始值
                    brightnessSeekBar.setProgress(100);
                    contrastSeekBar.setProgress(100);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e(TAG, "Crop error: " + cropError.getMessage());
            }
        }
    }
    private void updateFilterBase() {
        if (selectedBitmap != null)
            filterBaseBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
    }


    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            }, 101);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 101);
        }

    }

    public void showSaveDialog(Context context, String gcode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.Enter_file_name);

        // 创建输入框
        final EditText input = new EditText(context);
        input.setHint(R.string.Enter_file_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            GCode.saveGCodeToFile(gcode, context, fileName);
            Toast.makeText(context, R.string.Saved_successfully, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.show();
    }
    private void saveImageToGallery(File imageFile) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)); // 通知系统刷新相册
        }
    }

    private void readGCode() {
        List<File> ncFiles = GCodeRead.getCopiedNcFiles(this);
        if (ncFiles.isEmpty()) {
            Toast.makeText(this, R.string.no_gcode_found, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gcode_list_title));

        String[] fileArray = new String[ncFiles.size()];
        for (int i = 0; i < ncFiles.size(); i++) {
            fileArray[i] = ncFiles.get(i).getName();
        }

        builder.setItems(fileArray, (dialog, which) -> {
            File selectedFile = ncFiles.get(which);
            // 创建第二个AlertDialog
            AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(ImageEditActivity.this);
            secondDialogBuilder.setMessage(R.string.transmit + selectedFile.getName());
            secondDialogBuilder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Looper.prepare();
                                mtcp.FileTransport(1000, selectedFile, ImageEditActivity.this);
                            } catch (ModbusTCPClient.ModbusException e) {
                                //mtcp.onFileFailed(ImageEditActivity.this);
                                //Looper.loop();
                                Log.d("TCPTest", e.getMessage());
                            }
                        }
                    }).start();
                }
            });
            secondDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            // 显示第二个对话框
            secondDialogBuilder.show();
            //Toast.makeText(this, "已选择: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void applyHalftone() {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.In_operation, Toast.LENGTH_LONG).show();
        });
        if (selectedBitmap != null) {
            selectedBitmap = HalftoneDithering.applyHalftone(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
//            HalftoneDithering.saveBitmapToFile(selectedBitmap, this);
        }
    }

    // 创建临时图片文件
    private File createImageFile() throws IOException {
        String imageFileName = "PNG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        /* 前缀 */
        /* 后缀 */
        /* 目录 */
        return File.createTempFile(
                imageFileName,  /* 前缀 */
                ".png",         /* 后缀 */
                storageDir      /* 目录 */
        );
    }

    private Bitmap compressBitmap(Bitmap bitmap) {
        int maxWidth = 1920;
        int maxHeight = 1080;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxWidth || height > maxHeight) {
            float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        } else {
            return bitmap;
        }

    }

//    private void graffitiToGCode() {
//
//        if (getIntent().getStringExtra("GCodeImageUri") == null) {
//        }
//        else {
//            float whiteboardAspectRatio = getIntent().getFloatExtra("whiteboardAspectRatio",1f);
//            try {
//                imageUri = Uri.parse(getIntent().getStringExtra("GCodeImageUri"));
//
//                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
//                originalBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
//                GCode.saveBitmapToFile(originalBitmap, this, "original.png");
//                Mat m = ImageProcessor.bitmapToMat(selectedBitmap);
//                Mat createdMat = GCode.cropGCode(m, Constant.PrintWidth,Constant.PrintHeight,whiteboardAspectRatio);
//                GCode.saveBitmapToFile(ImageProcessor.matToBitmap(createdMat), this, "created.png");
//                selectedBitmap = ImageProcessor.matToBitmap(createdMat);
////               Mat m = ImageProcessor.bitmapToMat(selectedBitmap);
////                Mat createdMat = GCode.cropGCode(ImageProcessor.bitmapToMat(selectedBitmap), Constant.PlatformWidth,Constant.PlatformHeight);
////                selectedBitmap = ImageProcessor.matToBitmap(GCode.cropGCode(ImageProcessor.bitmapToMat(selectedBitmap), Constant.PlatformWidth,Constant.PlatformHeight));
//
//                int rho = getIntent().getIntExtra("rho", 6);
//                int laserPower = getIntent().getIntExtra("laserPower", 20);
////                Toast.makeText(ImageEditActivity.this, rho+ " "+laserPower, Toast.LENGTH_SHORT).show();;
//                if(getIntent().getBooleanExtra("isHalftone",false))
//                {
//                    applyHalftone();
//                }
//
//                imageView.setImageBitmap(selectedBitmap);
//
//                createdMat = ImageProcessor.bitmapToMat(selectedBitmap);
//
//                // 异步生成 GCode + 显示保存框
//                ProgressDialog progressDialog = ProgressDialog.show(
//                        ImageEditActivity.this,
//                        "生成中",
//                        "正在生成 G 代码，请稍候……",
//                        true
//                );
//
////                ExecutorService executor = Executors.newSingleThreadExecutor();
////                Handler handler = new Handler(Looper.getMainLooper());
////
////                Mat finalCreatedMat = createdMat;
////                GCode.saveBitmapToFile(ImageProcessor.matToBitmap(finalCreatedMat), this, "final.png");
////
////                if(getIntent().getStringExtra("GCodeFrameUri") == null) {
////                    executor.execute(() -> {
////                        try {
////                            String gcode = GCode.generateGCode0(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower);
////
////                            handler.post(() -> {
////                                progressDialog.dismiss();
////                                showSaveDialog(ImageEditActivity.this, gcode);
////                            });
////                        } catch (Exception e) {
////                            handler.post(() -> {
////                                progressDialog.dismiss();
////                                Toast.makeText(ImageEditActivity.this, "G 代码生成失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
////                            });
////                        }
////                    });
////                }
////                else
////                {
////                    frameUri = Uri.parse(getIntent().getStringExtra("GCodeFrameUri"));
////                    Mat frameMat = ImageProcessor.bitmapToMat(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri));
////                    Mat createdFrameMat = GCode.cropGCode(frameMat, Constant.PrintWidth,Constant.PrintHeight,whiteboardAspectRatio);
////                    int cutPower = 100;
////                    double simplifyEpsilonFactor = 0.0002;
////                    boolean invertBinary = true;
////                    executor.execute(() -> {
////                        try {
////                            String gcode = GCode.generateGCodeWithOutline(finalCreatedMat, createdFrameMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower, cutPower, simplifyEpsilonFactor, invertBinary);
////                            handler.post(() -> {
////                                progressDialog.dismiss();
////                                showSaveDialog(ImageEditActivity.this, gcode);
////                            });
////                        } catch (Exception e) {
////                            handler.post(() -> {
////                                progressDialog.dismiss();
////                                Toast.makeText(ImageEditActivity.this, "G 代码生成失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
////                            });
////                        }
////                    });
////                }
//                ExecutorService executor = Executors.newSingleThreadExecutor();
//                Handler handler = new Handler(Looper.getMainLooper());
//
//// 【修改点2】: 将 isHalftone 的检查和 applyHalftone() 调用移入后台线程
//                executor.execute(() -> {
//                    try {
//                        // ---- 后台任务开始 ----
//
//                        // 步骤1: 检查是否需要半色调处理 (在后台线程)
//                        if (getIntent().getBooleanExtra("isHalftone", false)) {
//                            // 这是耗时操作，现在安全地在后台执行
//                            applyHalftone();
//
//                            // 步骤2: 半色调处理完成后，通知UI线程更新ImageView
//                            // selectedBitmap 已经被 applyHalftone() 修改
//                            handler.post(() -> imageView.setImageBitmap(selectedBitmap));
//                        }
//
//                        // 步骤3: 使用最终的图像数据（可能已半色调）创建Mat，用于生成G-code
//                        Mat finalCreatedMat = ImageProcessor.bitmapToMat(selectedBitmap);
//                        GCode.saveBitmapToFile(ImageProcessor.matToBitmap(finalCreatedMat), this, "final.png");
//
//                        // 步骤4: 根据是否有边框，生成相应的G-code (仍在后台线程)
//                        String gcode;
//                        if (getIntent().getStringExtra("GCodeFrameUri") == null) {
//                            // 生成普通G-code
//                            int rho1 = getIntent().getIntExtra("rho", 6);
//                            int laserPower1 = getIntent().getIntExtra("laserPower", 20);
//                            gcode = GCode.generateGCode0(finalCreatedMat, rho1, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower1);
//                        }
//                        else {
//                            // 生成带边框的G-code
//                            Uri frameUri = Uri.parse(getIntent().getStringExtra("GCodeFrameUri"));
//                            // 注意：Bitmap加载也可能是耗时操作，放在后台是正确的
//                            Mat frameMat = ImageProcessor.bitmapToMat(MediaStore.Images.Media.getBitmap(this.getContentResolver(), frameUri));
//                            Mat createdFrameMat = GCode.cropGCode(frameMat, Constant.PrintWidth, Constant.PrintHeight, whiteboardAspectRatio);
//
//                            int rho1 = getIntent().getIntExtra("rho", 6);
//                            int laserPower1 = getIntent().getIntExtra("laserPower", 20);
//                            int cutPower = 100;
//                            double simplifyEpsilonFactor = 0.002; // 使用一个更合理的值
//                            boolean invertBinary = true;
//
//                            gcode = GCode.generateGCodeWithOutline(finalCreatedMat, createdFrameMat, rho1, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower1, cutPower, simplifyEpsilonFactor, invertBinary);
//                        }
//
//                        // 步骤5: 所有后台工作完成，通知UI线程显示结果
//                        handler.post(() -> {
//                            progressDialog.dismiss();
//                            showSaveDialog(ImageEditActivity.this, gcode);
//                        });
//
//                    } catch (Exception e) {
//                        // 异常处理
//                        handler.post(() -> {
//                            progressDialog.dismiss();
//                            Toast.makeText(ImageEditActivity.this, "处理失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
//                            e.printStackTrace(); // 在logcat中打印详细错误
//                        });
//                    }
//                });
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                Toast.makeText(this, "加载图像失败", Toast.LENGTH_SHORT).show();
//            }
//    }
//    }

//private void graffitiToGCode() {
//
//    if(getIntent().getStringExtra("GCodeImageUri") == null) return;
//
//    // 【第1步】: 立即显示加载对话框。这是UI线程唯一要做的重活。
//    ProgressDialog progressDialog = ProgressDialog.show(
//            ImageEditActivity.this,
//            "处理中",
//            "正在加载图像并生成 G 代码，请稍候……",
//            true
//    );
//    // 【第2步】: 准备后台任务
//    ExecutorService executor = Executors.newSingleThreadExecutor();
//    Handler handler = new Handler(Looper.getMainLooper());
//
//    executor.execute(() -> {
//        // --- 从这里开始，所有代码都在后台线程执行 ---
//        try {
//            // 【第3步】: 在后台加载和处理主图像
//            String imageUriString = getIntent().getStringExtra("GCodeImageUri");
//            if (imageUriString == null) {
//                throw new IOException("Image URI is missing.");
//            }
//            Uri imageUri = Uri.parse(imageUriString);
//            float whiteboardAspectRatio = getIntent().getFloatExtra("whiteboardAspectRatio", 1f);
//
//            Bitmap initialBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
//            Mat initialMat = ImageProcessor.bitmapToMat(initialBitmap);
//            Mat croppedMat = GCode.cropGCode(initialMat, Constant.PrintWidth, Constant.PrintHeight, whiteboardAspectRatio);
//            Bitmap selectedBitmap = ImageProcessor.matToBitmap(croppedMat); // 这个selectedBitmap是我们的工作副本
//            Mat origianalmat = ImageProcessor.bitmapToMat(originalBitmap1);
//
//            // (可选，但推荐) 让用户能看到裁剪后的图，提升体验
//            Bitmap finalSelectedBitmap1 = selectedBitmap;
//            handler.post(() -> imageView.setImageBitmap(finalSelectedBitmap1));
//
//            // 【第4步】: 在后台执行半色调（如果需要）
//            if (getIntent().getBooleanExtra("isHalftone", false)) {
//                // applyHalftone() 应该修改的是成员变量 selectedBitmap
//                // 请确保 applyHalftone 是线程安全的，或者它操作的 Bitmap 是局部变量
//                // 为了安全，我们传递并返回Bitmap
//                applyHalftone(); // 修改applyHalftone以返回Bitmap
//
//                // 更新UI显示半色调后的图像
//                Bitmap finalSelectedBitmap2 = selectedBitmap;
//                handler.post(() -> imageView.setImageBitmap(finalSelectedBitmap2));
//            }
//
//            // 【第5步】: 在后台准备最终的Mat用于G-code生成
//            Mat finalCreatedMat = ImageProcessor.bitmapToMat(selectedBitmap);
//
//            // 【第6步】: 在后台生成G-code
//            final String gcode;
//            String frameUriString = getIntent().getStringExtra("GCodeFrameUri");
//            int rho = getIntent().getIntExtra("rho", 6);
//            int laserPower = getIntent().getIntExtra("laserPower", 20);
//
//            int cutPower = 100;
//            double simplifyEpsilonFactor =0;
//            boolean invertBinary = true;
//
//            int isCurved = getIntent().getIntExtra("isCurved", 0);
//
//            if (frameUriString == null) {
//                // 只生成灰度G-code
//                if(isCurved==1)
//                    gcode = GCode.generateGCodeFromEdges(origianalmat,  Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, cutPower,invertBinary,simplifyEpsilonFactor);
//                else if(isCurved==0)
//                    gcode = GCode.generateGCode0(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower);
//                else if(isCurved==2)
//                    gcode = GCode.generateGCodeWithOutline(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower,cutPower,simplifyEpsilonFactor, invertBinary);
//                else {
//                    gcode = "";
//                    //fuapex
//                }
//            } else {
//                // 生成灰度+轮廓G-code
//                // 在后台加载和处理边框图像
//                Uri frameUri = Uri.parse(frameUriString);
//                Bitmap frameBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), frameUri);
//                Mat frameMat = ImageProcessor.bitmapToMat(frameBitmap);
//                Mat createdFrameMat = GCode.cropGCode(frameMat, Constant.PrintWidth, Constant.PrintHeight, whiteboardAspectRatio);
//
//                //gcode = GCode.generateGCodeWithOutline(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower,cutPower,simplifyEpsilonFactor, invertBinary);
//                gcode = GCode.generateGCodeWithOutline(finalCreatedMat,createdFrameMat,rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower,cutPower, simplifyEpsilonFactor, invertBinary);
//            }
//
//            // 【第7步】: 所有后台工作完成，回到UI线程显示结果
//            handler.post(() -> {
//                progressDialog.dismiss();
//                showSaveDialog(ImageEditActivity.this, gcode);
//            });
//
//        } catch (Exception e) {
//            // 【第8步】: 任何步骤出错，都回到UI线程显示错误信息
//            handler.post(() -> {
//                progressDialog.dismiss();
//                Toast.makeText(ImageEditActivity.this, "处理失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
//                e.printStackTrace(); // 在logcat中打印详细错误，方便调试
//            });
//        }
//        // --- 后台线程执行结束 ---
//    });
//}
private void graffitiToGCode() {

    // 1. 检查是否需要执行G代码生成逻辑，如果不是，则立即退出
    if (getIntent().getStringExtra("GCodeImageUri") == null) {
        return;
    }

    // 2. 立即向用户显示一个加载提示框
    ProgressDialog progressDialog = ProgressDialog.show(
            ImageEditActivity.this,
            "处理中(Processing)",
            "Loading image and generating G-code, please wait...",
            true
    );

    // 3. 准备后台线程，用于执行所有耗时操作（文件读取、图像处理等）
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    executor.execute(() -> {
        // --- 从这里开始，所有代码都在后台线程执行 ---
        try {
            // 4. 从Intent中获取所需数据
            String imageUriString = getIntent().getStringExtra("GCodeImageUri");
            Uri imageUri = Uri.parse(imageUriString);
            float whiteboardAspectRatio = getIntent().getFloatExtra("whiteboardAspectRatio", 1f);

            // 5. 在后台加载并裁剪主图像
            Bitmap initialBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            Mat initialMat = ImageProcessor.bitmapToMat(initialBitmap); // [修复] 这是未经处理的原始图像Mat，用于isCurved==1的情况
            Mat croppedMat = GCode.cropGCode(initialMat, Constant.PrintWidth, Constant.PrintHeight, whiteboardAspectRatio);

            // 这个 selectedBitmap 是我们后续处理的工作副本
            selectedBitmap = ImageProcessor.matToBitmap(croppedMat);

            // (可选但推荐) 更新UI，让用户能看到裁剪后的图像
            handler.post(() -> imageView.setImageBitmap(selectedBitmap));

            // 6. 如果需要，在后台执行半色调处理
            if (getIntent().getBooleanExtra("isHalftone", false)) {
                // applyHalftone() 是耗时操作，它会直接修改成员变量 selectedBitmap
                applyHalftone();

                // 再次更新UI以显示半色调处理后的结果
                Bitmap halftoneResult = selectedBitmap;
                handler.post(() -> imageView.setImageBitmap(halftoneResult));
            }

            // 7. 准备最终的Mat用于G代码生成
            // 此时的selectedBitmap可能是原图，也可能是半色调处理后的图
            Mat finalCreatedMat = ImageProcessor.bitmapToMat(selectedBitmap);

            // 8. 在后台生成G代码字符串
            final String gcode;
            String frameUriString = getIntent().getStringExtra("GCodeFrameUri");
            int rho = getIntent().getIntExtra("rho", 6);
            int laserPower = getIntent().getIntExtra("laserPower", 20);
            int cutPower = 100;
            double simplifyEpsilonFactor = 0.2; // [优化] 使用一个比0更合理的小值作为默认值
            boolean invertBinary = true;
            int isCurved = getIntent().getIntExtra("isCurved", 0);

            if (frameUriString == null) {
                // 没有边框，根据 isCurved 参数生成G代码
                switch (isCurved) {
                    case 1: // 基于边缘生成（矢量路径）
                        // [修复] 使用 initialMat (原始未处理的图像) 来提取边缘，而不是使用会崩溃的 originalBitmap1
                        gcode = GCode.generateGCodeFollowBlackPixels(initialMat, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, cutPower,  invertBinary,true);
                        break;
                    case 2: // 灰度图 + 轮廓
                        gcode = GCode.generateGCodeWithOutline(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower, cutPower, simplifyEpsilonFactor, invertBinary);
                        break;
                    case 0: // 默认：只生成灰度图G代码（光栅扫描）
                    default:
                        gcode = GCode.generateGCode0(finalCreatedMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower);
                        break;
                }
            } else {
                // 有边框，生成灰度+边框G代码
                Uri frameUri = Uri.parse(frameUriString);
                Bitmap frameBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), frameUri);
                Mat frameMat = ImageProcessor.bitmapToMat(frameBitmap);
                Mat createdFrameMat = GCode.cropGCode(frameMat, Constant.PrintWidth, Constant.PrintHeight, whiteboardAspectRatio);
                gcode = GCode.generateGCodeWithOutline(finalCreatedMat, createdFrameMat, rho, Constant.PrintWidth, Constant.PrintHeight, Constant.PrintStartX, Constant.PrintStartY, laserPower, cutPower, simplifyEpsilonFactor, invertBinary);
            }

            // 9. 所有后台工作完成，回到UI线程显示保存对话框
            handler.post(() -> {
                progressDialog.dismiss();
                showSaveDialog(ImageEditActivity.this, gcode);
            });

        } catch (Exception e) {
            // 10. 任何步骤出错，都回到UI线程显示错误信息
            handler.post(() -> {
                progressDialog.dismiss();
                Toast.makeText(ImageEditActivity.this, R.string.Processing_failed + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace(); // 在logcat中打印详细错误堆栈，方便调试
            });
        }
    });
}
    public void graffiti() {
        // 推荐方案：通过文件路径传递图片，避免TransactionTooLargeException
        try {
            Intent intent = new Intent(ImageEditActivity.this, WebWhiteBoardActivity.class);
            if(selectedBitmap != null) {
                File tempFile = createImageFile(); // 创建临时文件
                FileOutputStream out = new FileOutputStream(tempFile);
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                imageUri = Uri.fromFile(tempFile);
                // 传递文件路径给WebWhiteBoardActivity
                intent.putExtra("imagePath", tempFile.getAbsolutePath());
            }
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.Image_processing_failed, Toast.LENGTH_SHORT).show();
        }

        // 旧方案：通过base64传递图片（已注释，因大图会报TransactionTooLargeException）
        /*
        try {
            Intent intent = new Intent(ImageEditActivity.this, WebWhiteBoardActivity.class);
            if(selectedBitmap != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
                String dataUrl = "data:image/png;base64," + base64Image;
                intent.putExtra("imageBase64", dataUrl);
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
        }
        */
    }

    public void editImage(View view) {
        graffiti();
    }



}

