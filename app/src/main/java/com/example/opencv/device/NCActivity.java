package com.example.opencv.device;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        //在Activity的onCreate()或者onNewIntent()中

        Uri uri = intent.getData();

        if (uri != null) {

            String scheme= uri.getScheme();

            String host=uri.getHost();

            String port=uri.getPort()+"";

            String path=uri.getPath();

            String query=uri.getQuery();

            Toast.makeText(this, "scheme:"+scheme+",host:"+host+",port:"+port+",path:"+path+",query:"+query, Toast.LENGTH_LONG).show();
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

            Toast.makeText(this, "scheme:"+scheme, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "host:"+host, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "port:"+port, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "path:"+path, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "query:"+query, Toast.LENGTH_LONG).show();
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

    private void handleIncomingFile() {
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null) {
            // 1. 检查URI权限
            checkUriPermission(uri);

            // 2. 获取文件并处理
            //processFileFromUri(uri);
        } else {
            Toast.makeText(this, "未接收到文件", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkUriPermission(Uri uri) {
        try {
            // 尝试读取URI以检查权限
            getContentResolver().openInputStream(uri).close();
        } catch (Exception e) {
            Toast.makeText(this, "无权限访问该文件", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

//    private void processFileFromUri(Uri uri) {
//        new AsyncTask<Uri, Void, File>() {
//            @Override
//            protected File doInBackground(Uri... uris) {
//                return getFileFromUri(this, uris[0]);
//            }
//
//            @Override
//            protected void onPostExecute(File file) {
//                if (file != null && file.exists()) {
//                    // 处理文件
//                    handleFile(file);
//                } else {
//                    Toast.makeText(FileReceiverActivity.this,
//                            "文件获取失败", Toast.LENGTH_SHORT).show();
//                    finish();
//                }
//            }
//        }.execute(uri);
//    }

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