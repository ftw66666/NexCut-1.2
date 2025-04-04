package com.example.opencv.device;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.example.opencv.image.ImageEditActivity;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.modbus.NettyModbusTCPClient;
import com.example.opencv.whiteboard.WhiteboardActivity;

import java.util.ArrayList;
import java.util.List;

public class DeviceInfoActivity extends AppCompatActivity {

    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    private RecyclerView recyclerView;
    private device_InfoItemAdapter adapter;
    private List<DeviceDataItem> dataList;
    private Handler handler;
    private Thread readThread;
    private volatile boolean isRunning = true;
    public static Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_info);

        // 隐藏导航栏和状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new device_InfoItemAdapter(dataList);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        // 初始化Handler
        handler = new Handler(Looper.getMainLooper());

        // 开始更新
        startUpdating();
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

    private void startUpdating() {
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        // 获取设备信息
                        //List<Integer> deviceInfo = mtcp.ReadDeviceInfo();

                        final List<DeviceDataItem> deviceData = praseDeviceData(mtcp.deviceInfo);

                        // 在主线程更新UI
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 清空当前数据
                                dataList.clear();
                                dataList.addAll(deviceData);
                                // 通知Adapter数据已更新
                                adapter.notifyDataSetChanged();
                            }
                        });
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        readThread.start();
    }

    private List<DeviceDataItem> praseAxisData(List<Integer> axisInfo) {
        List<DeviceDataItem> axisData = new ArrayList<>();
        for (int i = 0; i < axisInfo.size(); i++) {
            switch (Constant.AxisRegnName[i]) {
                case "脉冲位置":
                case "加工停止时位置":
                case "累计行程":
                    axisData.add(new DeviceDataItem(Constant.DeviceRegnName[i], axisInfo.get(i) + "um"));
                    break;
                case "速度":
                    axisData.add(new DeviceDataItem(Constant.DeviceRegnName[i], axisInfo.get(i) + "um/s"));
                    break;
            }
        }
        return axisData;
    }

    private List<DeviceDataItem> praseDeviceData(List<Integer> deviceInfo) {
        List<DeviceDataItem> deviceData = new ArrayList<>();
        for (int i = 0; i < deviceInfo.size(); i++) {
            switch (Constant.DeviceRegnName[i]) {
                case "NULL":
                    break;
                case "日期":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], praseDate(deviceInfo.get(i))));
                    break;
                case "时间":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], praseTime(deviceInfo.get(i))));
                    break;
                case "软件版本":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i))));
                    break;
                case "扩展输入状态":
                case "扩展输出状态":
                case "输入状态":
                case "输出状态":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i), 2)));
                    break;
                case "运行状态":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], praseRunState(deviceInfo.get(i))));
                    break;
                case "DA1":
                case "DA2":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], deviceInfo.get(i) + "mv"));
                    break;
                case "PWM频率":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i)) + "Hz"));
                    break;
                case "PWM占空比":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i)) + "%"));
                    break;
                case "通电时间":
                case "通讯时间":
                case "开光时间":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i)) + "s"));
                    break;
                case "双驱反馈偏差":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i)) + "um"));
                    break;
                case "参数状态":
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], deviceInfo.get(i) == 0 ? "正常" : "需要重启生效"));
                    break;
                default:
                    deviceData.add(new DeviceDataItem(Constant.DeviceRegnName[i], Integer.toUnsignedString(deviceInfo.get(i))));
                    break;
            }
        }
        return deviceData;
    }

    private List<DeviceDataItem> praseExternDO(int data) {
        List<DeviceDataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            dataItems.add(new DeviceDataItem("扩展DO" + i, ((data >> i) & 1) == 1 ? "有效" : "无效"));
        }
        return dataItems;
    }

    private List<DeviceDataItem> praseExternDI(int data) {
        List<DeviceDataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            dataItems.add(new DeviceDataItem("扩展DI" + i, ((data >> i) & 1) == 1 ? "有效" : "无效"));
        }
        return dataItems;
    }

    private List<DeviceDataItem> praseDO(int data) {
        List<DeviceDataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            dataItems.add(new DeviceDataItem("DO" + i, ((data >> i) & 1) == 1 ? "有效" : "无效"));
        }
        return dataItems;
    }

    private List<DeviceDataItem> praseDI(int data) {
        List<DeviceDataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            dataItems.add(new DeviceDataItem("DI" + i, ((data >> i) & 1) == 1 ? "有效" : "无效"));
        }
        return dataItems;
    }

    private String praseRunState(int data) {
        int state = data & 0xFFFF;
        if (state == 0) {
            return "运行OK";
        } else if (state == 1) {
            return "运行中";
        } else if (state == 2) {
            return "运行异常";
        } else {
            return "其他异常";
        }
    }

    public String praseDate(int data) {
        int year = (data >> 16) & 0xFF;
        int month = (data >> 8) & 0xFF;
        int day = data & 0xFF;
        return year + "-" + month + "-" + day;
    }

    public String praseTime(int data) {
        int hour = (data >> 16) & 0xFF;
        int minute = (data >> 8) & 0xFF;
        int second = data & 0xFF;
        return hour + ":" + minute + ":" + second;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止读取线程
        isRunning = false;
        if (readThread != null) {
            readThread.interrupt();
        }
        // 清理Handler
        handler.removeCallbacksAndMessages(null);
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

    public void OnClickDeviceControl(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(DeviceInfoActivity.this, DeviceActivity.class);
        startActivity(intent);
    }
}