package com.example.opencv.image;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Looper;
import android.provider.MediaStore;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.opencv.android.OpenCVLoader;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.opencv.Constant;
import com.example.opencv.device.DeviceActivity;
import com.example.opencv.device.DeviceInfoActivity;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.util.FileUtils;
import com.squareup.picasso.Picasso;

import static android.content.ContentValues.TAG;


import static com.example.opencv.MainActivity.CAPTURE_IMAGE;


import android.Manifest;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.modbus.NettyModbusTCPClient;
import com.example.opencv.whiteboard.WhiteboardActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class  ImageEditActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;

    private ImageView imageView;
    private Bitmap selectedBitmap;
    private Bitmap originalBitmap;
    private PhotoSelector photoSelector;

    private Uri imageUri;

    public Toolbar toolbar;
    private SeekBar brightnessSeekBar;
    private SeekBar contrastSeekBar;
    private float brightnessValue = 0f; // 范围：-255 到 255
    private float contrastValue = 1f;   // 范围：0.1 到 3



    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

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
                applyFilters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrastValue = progress / 100f; // 范围从 0.1 到 3.0
                applyFilters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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
//        Button btnEdge = findViewById(R.id.btnEdge);
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


        //OpenCV初始化
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        photoSelector = new PhotoSelector();

        // 设置按钮点击事件
//        btnSelect.setOnClickListener(v -> photoSelector.selectFromGallery(ImageEditActivity.this));
//        btnCapture.setOnClickListener(v -> photoSelector.capturePhoto(ImageEditActivity.this, getApplicationContext()));
        btnBinary.setOnClickListener(v -> applyBinary());
        btnInvert.setOnClickListener(v -> applyInvert());
        btnGrayscale.setOnClickListener(v -> applyGrayscale());
        btnBlur.setOnClickListener(v -> applyBlur());
//        btnEdge.setOnClickListener(v -> applyEdgeDetection());
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
    }

    // 加载 Toolbar 菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    // 监听 Toolbar 按钮点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.User_image) {
            Toast.makeText(this, "社区功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void InitialImage() {
        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString == null) return;

        imageUri = Uri.parse(uriString);
        try {
            // 使用ContentResolver直接打开流
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            selectedBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            originalBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
            imageView.setImageBitmap(selectedBitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void BackToOrigin() {
        if (originalBitmap != null) {
            selectedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true); // 复制原始位图
            imageView.setImageBitmap(selectedBitmap);
        } else {
            Toast.makeText(this, "没有原始图像可还原", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyFilters() {
        Bitmap filteredBitmap = Bitmap.createBitmap(selectedBitmap.getWidth(), selectedBitmap.getHeight(), selectedBitmap.getConfig());

        ColorMatrix colorMatrix = new ColorMatrix();

        // 调整对比度
        float scale = contrastValue;
        float translate = (-0.5f * scale + 0.5f) * 255f;
        colorMatrix.set(new float[]{
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        });

        // 调整亮度
        ColorMatrix brightnessMatrix = new ColorMatrix(new float[] {
                1, 0, 0, 0, brightnessValue,
                0, 1, 0, 0, brightnessValue,
                0, 0, 1, 0, brightnessValue,
                0, 0, 0, 1, 0
        });

        colorMatrix.preConcat(brightnessMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

        Canvas canvas = new Canvas(filteredBitmap);
        canvas.drawBitmap(selectedBitmap, 0, 0, paint);

        imageView.setImageBitmap(filteredBitmap);
    }


    /**
     * 应用灰度处理
     */
    private void applyGrayscale() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.toGrayscale(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
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
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 应用高斯模糊
     */
    private void applyBlur() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.applyGaussianBlur(selectedBitmap, 15);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 应用 Canny 边缘检测
     */
    private void applyEdgeDetection() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.applyCannyEdgeDetection(selectedBitmap, 107, 250);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    /**
     * 旋转图像 90 度
     */
    private void applyRotation() {
        if (selectedBitmap != null) {
            selectedBitmap = ImageProcessor.rotateImage(selectedBitmap, 90);
            imageView.setImageBitmap(selectedBitmap);
        }
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
    }

    private void verticalFlip()
    {
        if(selectedBitmap != null)
        {
            selectedBitmap = ImageProcessor.flipImageVertically(selectedBitmap);
            imageView.setImageBitmap(selectedBitmap);
        }
    }

    private void saveOnAssets() {
        String imageFileName = "PNG_" + System.currentTimeMillis() + "_" + "file:///android_asset/";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        /* 前缀 */
        /* 后缀 */
        /* 目录 */
        return;
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
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            // 获取裁剪后的图片
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                Picasso.get().load(resultUri).into(imageView);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            // 错误处理
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e(TAG, "Crop error: " + cropError.getMessage());
            }
        }
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
        builder.setTitle("输入文件名");

        // 创建输入框
        final EditText input = new EditText(context);
        input.setHint("请输入文件名");
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            GCode.saveGCodeToFile(gcode, context, fileName);
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

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
            Toast.makeText(this, "没有找到已复制的 GCode 文件", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择一个 GCode 文件");

        String[] fileArray = new String[ncFiles.size()];
        for (int i = 0; i < ncFiles.size(); i++) {
            fileArray[i] = ncFiles.get(i).getName();
        }

        builder.setItems(fileArray, (dialog, which) -> {
            File selectedFile = ncFiles.get(which);
            // 创建第二个AlertDialog
            AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(ImageEditActivity.this);
            secondDialogBuilder.setMessage("是否选择传输: " + selectedFile.getName());
            secondDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
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
            secondDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
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
        Toast.makeText(this, "运算中", Toast.LENGTH_LONG).show();
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

    private void graffitiToGCode() {
        if (getIntent().getStringExtra("GCodeimageUri") == null) return;
        else {
            float corpAspectRatio = getIntent().getFloatExtra("printerAspectRatio",1.2f);
            try {
                imageUri = Uri.parse(getIntent().getStringExtra("GCodeimageUri"));
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                originalBitmap = selectedBitmap.copy(selectedBitmap.getConfig(), true);
                GCode.saveBitmapToFile(originalBitmap, this, "original.png");
                Mat m = ImageProcessor.bitmapToMat(selectedBitmap);
                Mat createdMat = GCode.cropGCode(m, Constant.Printwidth,Constant.Printheight);
                GCode.saveBitmapToFile(ImageProcessor.matToBitmap(createdMat), this, "created.png");
                selectedBitmap = ImageProcessor.matToBitmap(createdMat);
//               Mat m = ImageProcessor.bitmapToMat(selectedBitmap);
//                Mat createdMat = GCode.cropGCode(ImageProcessor.bitmapToMat(selectedBitmap), Constant.PlatformWidth,Constant.PlatformHeight);
//                selectedBitmap = ImageProcessor.matToBitmap(GCode.cropGCode(ImageProcessor.bitmapToMat(selectedBitmap), Constant.PlatformWidth,Constant.PlatformHeight));

                if(getIntent().getBooleanExtra("isHalftone",false))
                {
                    applyHalftone();
                }
                else imageView.setImageBitmap(selectedBitmap);

                createdMat = ImageProcessor.bitmapToMat(selectedBitmap);

                // 异步生成 GCode + 显示保存框
                ProgressDialog progressDialog = ProgressDialog.show(
                        ImageEditActivity.this,
                        "生成中",
                        "正在生成 G 代码，请稍候……",
                        true
                );

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                Mat finalCreatedMat = createdMat;
                GCode.saveBitmapToFile(ImageProcessor.matToBitmap(finalCreatedMat), this, "final.png");
                executor.execute(() -> {
                    try {
                        String gcode = GCode.generateGCode0(finalCreatedMat, 6, Constant.Printwidth, Constant.Printheight,Constant.PrintStartX,Constant.PrintStartY);

                        handler.post(() -> {
                            progressDialog.dismiss();
                            showSaveDialog(ImageEditActivity.this, gcode);
                        });
                    } catch (Exception e) {
                        handler.post(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ImageEditActivity.this, "G 代码生成失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "加载图像失败", Toast.LENGTH_SHORT).show();
            }
    }
    }

    public void graffiti() {

        try {
            Intent intent = new Intent(ImageEditActivity.this, WhiteboardActivity.class);
            if(selectedBitmap != null) {
                File tempFile = createImageFile(); // 创建临时文件
                FileOutputStream out = new FileOutputStream(tempFile);
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                imageUri = Uri.fromFile(tempFile);
                // 传递 URI 给下一个 Activity
                intent.putExtra("imageUri", imageUri.toString());
            }
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void mainPage(View view)
    {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void editImage(View view) {
        graffiti();
    }

    public void goNext(View view) {
        graffiti();
    }


}

