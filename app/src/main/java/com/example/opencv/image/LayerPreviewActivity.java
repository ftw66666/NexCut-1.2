package com.example.opencv.image;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv.Constant;
import com.example.opencv.R;
import com.example.opencv.image.GCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.core.Mat;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LayerPreviewActivity extends AppCompatActivity {
    public static class LayerParam {
        public String filePath;
        public String printingMethod;
        public int rho = 6; // 线密度
        public boolean isHalftone = false;
        public int laserPower = 20; // 激光功率
        public LayerParam(String filePath, String printingMethod) {
            this.filePath = filePath;
            this.printingMethod = printingMethod;
        }
    }
    private List<LayerParam> layerParams = new ArrayList<>();
    private RecyclerView recyclerView;
    private Button btnGenerate;
    private LayerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layer_preview);

        recyclerView = findViewById(R.id.recyclerView);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setEnabled(false); // 先禁用按钮，防止so未加载时被点击

        // OpenCV同步加载
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV 加载失败", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnGenerate.setEnabled(true); // so加载成功后再允许点击

        // 解析layerData
        String layerDataJson = getIntent().getStringExtra("layerData");
        if (layerDataJson != null) {
            try {
                JSONArray arr2d = new JSONArray(layerDataJson);
                for (int i = 0; i < arr2d.length(); i++) {
                    JSONArray item = arr2d.getJSONArray(i);
                    String filePath = item.getString(0);
                    String printingMethod = item.getString(1);
                    layerParams.add(new LayerParam(filePath, printingMethod));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        adapter = new LayerAdapter(layerParams);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnGenerate.setOnClickListener(v -> generateMultiLayerGCode());
    }

    public void generateMultiLayerGCode() {
        ProgressDialog progressDialog = ProgressDialog.show(
                this, "处理中", "正在生成多图层 G 代码，请稍候……", true
        );
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                StringBuilder allGCode = new StringBuilder();
                for (LayerParam layer : layerParams) {
                    Bitmap bitmap = BitmapFactory.decodeFile(layer.filePath);
                    if (bitmap == null) continue;
                    Mat mat = com.example.opencv.image.ImageProcessor.bitmapToMat(bitmap);
                    String gcode;
                    if ("SCAN".equalsIgnoreCase(layer.printingMethod)) {
                        if (layer.isHalftone) {
                            bitmap = com.example.opencv.image.HalftoneDithering.applyHalftone(bitmap);
                            mat = com.example.opencv.image.ImageProcessor.bitmapToMat(bitmap);
                        }
                        gcode = GCode.generateGCode0(
                                mat, layer.rho, Constant.PlatformWidth, Constant.PlatformHeight,
                                Constant.PrintStartX, Constant.PrintStartY, layer.laserPower
                        );
                    } else if ("VECTOR".equalsIgnoreCase(layer.printingMethod) ||
                            "ENGRAVE".equalsIgnoreCase(layer.printingMethod) ||
                            "雕刻".equals(layer.printingMethod)) {
                        gcode = GCode.generateGCodeFromSkeleton(
                                mat, Constant.PlatformWidth, Constant.PlatformHeight,
                                Constant.PrintStartX, Constant.PrintStartY, layer.laserPower,
                                true, 1
                        );
                    } else {
                        gcode = null;
                    }
                    allGCode.append("; 图层: ").append(layer.printingMethod).append("\n");
                    allGCode.append(gcode).append("\n");
                }
                handler.post(() -> {
                    progressDialog.dismiss();
                    showSaveDialog(LayerPreviewActivity.this, allGCode.toString());
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(LayerPreviewActivity.this, "处理失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        });
    }

    public void showSaveDialog(Context context, String gcode) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("输入文件名");
        final EditText input = new EditText(context);
        input.setHint("请输入文件名");
        builder.setView(input);
        builder.setPositiveButton("保存", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            GCode.saveGCodeToFile(gcode, context, fileName);
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
} 