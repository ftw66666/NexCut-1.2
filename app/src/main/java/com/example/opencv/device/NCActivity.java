package com.example.opencv.device;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv.Constant;
import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.whiteboard.SettingActivity;
import com.example.opencv.whiteboard.WhiteboardActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NCActivity extends AppCompatActivity {

    public Toolbar toolbar;

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        //在Activity的onCreate()或者onNewIntent()中

        Uri uri = intent.getData();

        if (uri != null) {

            String scheme= uri.getScheme();

            String host=uri.getHost();

            String port=uri.getPort()+"";

            String path=uri.getPath();

            String query=uri.getQuery();

            //Toast.makeText(this, "scheme:"+scheme+",host:"+host+",port:"+port+",path:"+path+",query:"+query, Toast.LENGTH_LONG).show();

            String fileName = getFileNameFromUri(uri);
            File destFile = new File(this.getExternalFilesDir("/gcodes"), fileName);
            copyUriToFile(uri, destFile, fileName);
        }
//你把这些参数打印出来看看，你就知道怎么获取到那个文件或者网址了
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nc);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null) {

            String scheme= uri.getScheme();

            String host=uri.getHost();

            String port=uri.getPort()+"";

            String path=uri.getPath();

            String query=uri.getQuery();

//            Toast.makeText(this, "scheme:"+scheme, Toast.LENGTH_LONG).show();
//            Toast.makeText(this, "host:"+host, Toast.LENGTH_LONG).show();
//            Toast.makeText(this, "port:"+port, Toast.LENGTH_LONG).show();
//            Toast.makeText(this, "path:"+path, Toast.LENGTH_LONG).show();
//            Toast.makeText(this, "query:"+query, Toast.LENGTH_LONG).show();
            String fileName = getFileNameFromUri(uri);
            File destFile = new File(this.getExternalFilesDir("/gcodes"), fileName);
            copyUriToFile(uri, destFile, fileName);
        }


    }


    public String getFileNameFromUri(Uri uri) {
        String result = "unknown_file";
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }

        // 如果还是得不到，尝试从 path 获取
        if (result.equals("unknown_file")) {
            String path = uri.getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                result = path.substring(lastSlash + 1);
            }
        }

        return result;
    }



    public void copyUriToFile(Uri uri, File destinationFile, String fileName) {
        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(destinationFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                runOnUiThread(() -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("imported_file_name", fileName);
                    startActivity(intent);
                    finish();
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    public void copyUriToFile(Uri uri, File destinationFile) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            Toast.makeText(this, "文件已复制到: " + destinationFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "文件复制失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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


    public void OnClickDeviceInfo(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(NCActivity.this, DeviceInfoActivity.class);
        startActivity(intent);
    }

    public void OnClickDeviceControl(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(NCActivity.this, device_Control.class);
        startActivity(intent);
    }

    public void OnClickSetPlanform(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入参数");
    }


    public void mainPage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void editImage(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, WhiteboardActivity.class);
        startActivity(intent);
    }

    public void onClickSetting(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }
}