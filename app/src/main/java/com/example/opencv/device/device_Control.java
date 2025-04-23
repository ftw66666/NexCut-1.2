package com.example.opencv.device;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.opencv.MainActivity;
import com.example.opencv.R;
import com.example.opencv.http.ApiClient;
import com.example.opencv.http.Control;
import com.example.opencv.http.MachineInfo;
import com.example.opencv.modbus.ModbusTCPClient;
import com.example.opencv.whiteboard.SettingActivity;
import com.example.opencv.whiteboard.WhiteboardActivity;

import java.util.ArrayList;
import java.util.List;

public class device_Control extends AppCompatActivity {

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 1;
    public static final int AXIS_Z = 3;
    public static final int DEFAULT_SPEED = 50;  //mm/s
    public static final int DEFAULT_DISTANCE = 1; // mm
    // public static final int LONG_PRESS_DELAY = 10;  // 长按触发延迟(ms)
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

    ApiClient apiClient = ApiClient.getInstance();
    Control control = new Control();
    private static final String TAG = "devicecontrol";
    private Button button_up;
    private Button button_down;
    private Button button_left;
    private Button button_right;
    private Button button_stop;
    private List<ImageView> DIImageViews;
    private Button button_Zup;
    private Button button_Zdown;
    private Toolbar toolbar;

    private ConstraintLayout allAxisLayout;

    private GridLayout zControls, moveControls;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_control_new);

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

        // 初始化轴控制
        //allAxisLayout = findViewById(R.id.all_move_controls);
        zControls = findViewById(R.id.z_controls);
        moveControls = findViewById(R.id.move_controls);
        button_up = findViewById(R.id.btn_up);
        button_down = findViewById(R.id.btn_down);
        button_left = findViewById(R.id.btn_left);
        button_right = findViewById(R.id.btn_right);
        button_Zup = findViewById(R.id.btn_z_plus);
        button_Zdown = findViewById(R.id.btn_z_minus);
        btnMoveControl(button_up, AXIS_X, DEFAULT_SPEED, DEFAULT_DISTANCE);
        btnMoveControl(button_down, AXIS_X, DEFAULT_SPEED, DEFAULT_DISTANCE * (-1));
        btnMoveControl(button_left, AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE * (-1));
        btnMoveControl(button_right, AXIS_Y, DEFAULT_SPEED, DEFAULT_DISTANCE);
        btnMoveControl(button_Zup, AXIS_Z, DEFAULT_SPEED, DEFAULT_DISTANCE);
        btnMoveControl(button_Zdown, AXIS_Z, DEFAULT_SPEED, DEFAULT_DISTANCE * (-1));
        button_stop = findViewById(R.id.command_2);


//        allAxisLayout.setMinHeight((int)((moveControls.getWidth()-zControls.getWidth()-button_up.getWidth()*2 ) / 2f
//        + button_up.getWidth() + zControls.getWidth() + allAxisLayout.getPaddingTop() + allAxisLayout.getPaddingBottom()));
//        ;
//        allAxisLayout.setMaxHeight((int)((moveControls.getWidth()-zControls.getWidth()-button_up.getWidth()*2 ) / 2f
//                + button_up.getWidth() + zControls.getWidth() + allAxisLayout.getPaddingTop() + allAxisLayout.getPaddingBottom()));

        // 初始化DO控制
        GridLayout doGrid = findViewById(R.id.doGrid);
        for (int i = 1; i <= 8; i++) {
            Switch doSwitch = new Switch(this);
            final int doNumber = i;
            doSwitch.setText("DO" + i);
            if (apiClient.isConnected.get() && apiClient.isInfo.get()) {
                doSwitch.setChecked(apiClient.machineInfo.getMc().getDO()[i - 1]);
            } else {
                doSwitch.setChecked(false);
            }
            doSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setDO(doNumber, isChecked, doSwitch);
                }
            });

            // 设置 LayoutParams
            //GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            // 关键设置：宽度填满，高度自适应
            //params.width = 0;  // 相当于 XML 的 android:layout_width="0dp"
            //params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            //params.columnSpec = GridLayout.spec(0, 1f);  // 权重=1
            // 设置内容居中（Gravity.CENTER）
//            params.setGravity(Gravity.CENTER);
//
//            // 应用 LayoutParams
//            doSwitch.setLayoutParams(params);

            doGrid.addView(doSwitch);
        }

        // 初始化DI显示
        GridLayout diStateGrid = findViewById(R.id.DI_State);
        DIImageViews = new ArrayList<>();
        for (int i = 0; i < diStateGrid.getChildCount(); i++) {
            View child = diStateGrid.getChildAt(i);
            // 确保子元素是 LinearLayout
            if (child instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) child;
                // 获取 LinearLayout 中的第二个子元素（ImageView）
                View imageView = linearLayout.getChildAt(1);
                if (imageView instanceof ImageView) {
                    DIImageViews.add((ImageView) imageView);
                }
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                        if (apiClient.isConnected.get() && apiClient.isInfo.get()) {
                            MachineInfo.MachineStatus mc = apiClient.machineInfo.getMc();
                            boolean[] di = mc.getDi();
                            if (di.length != 0) {
                                updateDIState(di);
                            }
                        } else {
                            shutDwonDI();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void updateDIState(boolean[] di) {

        for (int i = 0; i < di.length; i++) {
            ImageView imageView = DIImageViews.get(i);
            if (di[i]) {
                runOnUiThread(() -> {
                    imageView.setBackgroundTintList(getColorStateList(R.color.DI_True));
                });
            } else {
                runOnUiThread(() -> {
                    imageView.setBackgroundTintList(getColorStateList(R.color.DI_False));
                });
            }
        }
    }

    private void shutDwonDI() {
        for (int i = 0; i < DIImageViews.size(); i++) {
            ImageView imageView = DIImageViews.get(i);
            runOnUiThread(() -> {
                imageView.setBackgroundTintList(getColorStateList(R.color.DI_False));
            });
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

    private void configureNumberPicker(NumberPicker picker, final int daNumber) {
        picker.setMinValue(50);
        picker.setMaxValue(10000); // 假设DA范围是0-5000mV
        picker.setValue(5000);
        picker.setWrapSelectorWheel(false);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setDA(daNumber, newVal);
            }
        });
    }


    @SuppressLint("ClickableViewAccessibility")
    private void btnMoveControl(Button button, int axis, int speed, int distance) {

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveAxis(axis, speed, distance);
            }
        });
//        button.setOnTouchListener(new View.OnTouchListener() {
//            private Handler handler = new Handler();
//            private Runnable longPressRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    moveAxis(axis, speed, distance);
//                }
//            };
//
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        handler.post(longPressRunnable);
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        //button_stop.performClick();
//                        Stop(v);
//                        return true;
//                }
//                return false;
//            }
//
//    });
    }

    private void moveAxis(int axis, int speed, int distance) {
        // 在这里实现你的轴移动逻辑
        // 参数：axis (0-3), speed (mm/s), distance (mm)，均为整数
        control.MoveAxis(axis, distance, speed, this);
    }

    // 控制DO开关
    private void setDO(int doNumber, boolean state, Switch switch1) {
        // 在这里实现你的DO控制逻辑
        // 参数：doNumber (1-8), state (true/false)
        control.SetDO(this, doNumber, state, switch1);
    }

    // 控制DA输出
    private void setDA(int daNumber, int value) {
        // 在这里实现你的DA控制逻辑
        // 参数：daNumber (1-2), value (mV)
        new Thread(new Runnable() {
            final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    mtcp.ControlDA(daNumber, value);
                } catch (ModbusTCPClient.ModbusException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtcp.onWriteFailed(device_Control.this, e.getMessage());
                        }
                    });
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    public void Stop(View view) {
        control.Stop(this);
    }

    public void Pause(View view) {
        control.Pause(this);
    }

    public void Back(View view) {
        control.SystemOrigin(this);
    }

    public void BackZero(View view) {
        control.Zero(this);
    }

    public void FTCCalibration(View view) {
        control.FTCCalibration(this);
    }

    public void FTCFollow(View view) {
        control.FTCFollow(this);
    }

    public void workBroder(View view) {
//        new Thread(new Runnable() {
//            Handler handler = new Handler(Looper.getMainLooper());
//
//            @Override
//            public void run() {
//                try {
//                    mtcp.ControlWorkBroder();
//                } catch (ModbusTCPClient.ModbusException e) {
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            mtcp.onWriteFailed(device_Control.this, e.getMessage());
//                        }
//                    });
//                    Log.d("TCPTest", e.getMessage());
//                }
//            }
//        }).start();
    }

    public void LoadFile(View view) {
        control.LoadFile(this);
    }

    public void Process(View view) {
        control.Start(this, false, 100);
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
        Intent intent = new Intent(device_Control.this, DeviceActivity.class);
        startActivity(intent);
    }

    public void onClickSetting(View view) {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.anim_scale_in);
        view.startAnimation(scaleIn);

        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

}



