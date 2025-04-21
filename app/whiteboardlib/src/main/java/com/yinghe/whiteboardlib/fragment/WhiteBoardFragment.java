package com.yinghe.whiteboardlib.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yinghe.whiteboardlib.MultiImageSelector;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.Utils.TimeUtils;
import com.yinghe.whiteboardlib.adapter.SketchDataGridAdapter;
import com.yinghe.whiteboardlib.bean.SketchData;
import com.yinghe.whiteboardlib.bean.StrokeRecord;
import com.yinghe.whiteboardlib.view.SketchView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.yinghe.whiteboardlib.Utils.ScreenUtils.sp2px;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WhiteBoardFragment extends Fragment implements SketchView.OnDrawChangedListener, View.OnClickListener {

    final String TAG = getClass().getSimpleName();
    File photoTextFile;
    public void setPhotoTextFile(File imageFile) {
        photoTextFile = imageFile;
    }
    public File getPhotoTextFile()
    {
        return photoTextFile;
    }
    public interface SendBtnCallback {
        void onSendBtnClick(File filePath);
    }

    static final int COLOR_BLACK = Color.parseColor("#ff000000");
    static final int COLOR_RED = Color.parseColor("#ffff4444");
    static final int COLOR_GREEN = Color.parseColor("#ff99cc00");
    static final int COLOR_ORANGE = Color.parseColor("#ffffbb33");
    static final int COLOR_BLUE = Color.parseColor("#ff33b5e5");
    public static final int REQUEST_IMAGE = 2;
    public static final int REQUEST_BACKGROUND = 3;

    private static final float BTN_ALPHA = 0.4f;

    //文件保存目录
//    public static final String TEMP_FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/YingHe/temp/";
//    public static final String FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/YingHe/sketchPhoto/";
    public static String TEMP_FILE_PATH;
    public static String FILE_PATH;
    public static final String TEMP_FILE_NAME = "temp_";

    int keyboardHeight;
    int textOffX;
    int textOffY;

    float printerAspectRatio = 1f;

    SketchView mSketchView;//画板

    View controlLayout;//控制布局

    ImageView btn_add;//添加画板
    ImageView btn_stroke;//画笔
    ImageView btn_eraser;//橡皮擦
    ImageView btn_undo;//撤销
    ImageView btn_redo;//取消撤销
    ImageView btn_photo;//加载图片
    ImageView btn_background;//背景图片
    ImageView btn_drag;//拖拽
    ImageView btn_save;//保存
    ImageView btn_empty;//清空
    ImageView btn_send;//推送
    ImageView btn_send_space;//推送按钮间隔


    RadioGroup strokeTypeRG, strokeColorRG;

    Activity activity;//上下文

    int strokeMode;//模式
    int strokeType;//模式

    EditText saveET;
    AlertDialog saveDialog;
    GridView sketchGV;
    SketchDataGridAdapter sketchGVAdapter;

    int pupWindowsDPWidth = 300;//弹窗宽度，单位DP
    int strokePupWindowsDPHeight = 275;//画笔弹窗高度，单位DP
    int eraserPupWindowsDPHeight = 90;//橡皮擦弹窗高度，单位DP


    SendBtnCallback sendBtnCallback;
    boolean isTeacher;
    PopupWindow strokePopupWindow, eraserPopupWindow, textPopupWindow;//画笔、橡皮擦参数设置弹窗实例
    private View popupStrokeLayout, popupEraserLayout, popupTextLayout;//画笔、橡皮擦弹窗布局
    private SeekBar strokeSeekBar, strokeAlphaSeekBar, eraserSeekBar;
    private ImageView strokeImageView, strokeAlphaImage, eraserImageView;//画笔宽度，画笔不透明度，橡皮擦宽度IV
    private EditText strokeET;//绘制文字的内容
    private int size;
    private AlertDialog dialog;
    private ArrayList<String> mSelectPath;

    private final List<SketchData> sketchDataList = new ArrayList<>();
//    //    private SketchData curSketchData;
//    private List<String> sketchPathList = new ArrayList<>();
//    private int dataPosition;

    //
    public static int sketchViewHeight;
    public static int sketchViewWidth;
    public static int sketchViewRight;
    public static int sketchViewBottom;
    public static int decorHeight;
    public static int decorWidth;

    private static int platformWidth = 0;
    private static int platformHeight = 0;

    public void setPlatformWidth(int width) {
        platformWidth = width;
    }
    public void setPlatformHeight(int height) {
        platformHeight = height;
    }
    public int getPlatformWidth() {
        return platformWidth;
    }

    public int getPlatformHeight() {
        return platformHeight;
    }
    /**
     * show 默认新建一个学生端功能
     *
     * @author TangentLu
     * create at 16/6/17 上午9:59
     */
    public static WhiteBoardFragment newInstance() {
        return new WhiteBoardFragment();
    }

    /**
     * show 新建一个教师端的画板碎片，有推送按钮
     *
     * @param callback 推送按钮监听器，接受返回的图片文件路径可用于显示文件
     * @author TangentLu
     * create at 16/6/17 上午9:57
     */
    public static WhiteBoardFragment newInstance(SendBtnCallback callback) {
        WhiteBoardFragment fragment = new WhiteBoardFragment();
        fragment.sendBtnCallback = callback;
        fragment.isTeacher = true;
        return fragment;
    }

    /**
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     * show 设置当前白板的背景图片
     */
    public void setCurBackgroundByPath(String imgPath) {
        showSketchView(true);
        mSketchView.setBackgroundByPath(imgPath);
    }

    /**
     * show  新增白板并设置白板的背景图片
     *
     * @param imgPath 添加的背景图片文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:39
     */
    public void setNewBackgroundByPath(String imgPath) {
        showSketchView(true);
        SketchData newSketchData = new SketchData();
        sketchDataList.add(newSketchData);
        mSketchView.updateSketchData(newSketchData);
        setCurBackgroundByPath(imgPath);
        mSketchView.setEditMode(SketchView.EDIT_STROKE);
    }

    /**
     * show 新增图片到当前白板
     *
     * @param imgPath 新增的图片路径
     * @author TangentLu
     * create at 16/6/21 下午3:42
     */
    public void addPhotoByPath(String imgPath, float x, float y) {
        showSketchView(true);
        mSketchView.addPhotoByPath(imgPath);
        mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
        mSketchView.onDragAction(x,y);
    }

    public void addPhotoByPath(String imgPath) {
        showSketchView(true);
        mSketchView.addPhotoByPath(imgPath);
        mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
        //mSketchView.onDragAction(x,y);
    }


    /**
     * show 获取当前白板的BitMap
     *
     * @author TangentLu
     * create at 16/6/21 下午3:44
     */
    public Bitmap getResultBitmap() {
        return mSketchView.getResultBitmap();
    }

    /**
     * show 手动保存当前画板到文件，耗时操作
     *
     * @param filePath 保存的文件路径
     * @param imgName  保存的文件名
     * @return 返回保存后的文件路径
     * @author TangentLu
     * create at 16/6/21 下午3:46
     */
    public File saveInOI(String filePath, String imgName) {
        return saveInOI(filePath, imgName, 100);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();//初始化上下文
        TEMP_FILE_PATH = activity.getExternalFilesDir(null).getPath() + "/WhiteBoard/temp/";
        FILE_PATH = activity.getExternalFilesDir(null).getPath() + "/WhiteBoard/sketchPhoto/";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_white_board, container, false);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //下面的代码主要是为了解决软键盘弹出后遮挡住文字录入PopWindow的问题
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);//获取rootView的可视区域
                int screenHeight = rootView.getHeight();//获取rootView的高度
                keyboardHeight = screenHeight - (r.bottom - r.top);//用rootView的高度减去rootView的可视区域高度得到软键盘高度
                if (textOffY > (sketchViewHeight - keyboardHeight)) {//如果输入焦点出现在软键盘显示的范围内则进行布局上移操作
                    rootView.setTop(-keyboardHeight);//rootView整体上移软键盘高度
                    //更新PopupWindow的位置
                    int x = textOffX;
                    int y = textOffY - mSketchView.getHeight();
                    textPopupWindow.update(mSketchView, x, y,
                            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
            }
        });
        findView(rootView);//载入所有的按钮实例
        initDrawParams();//初始化绘画参数
        initPopupWindows();//初始化弹框
        initSaveDialog();
        initData();
        initSketchGV();
        return rootView;
    }

    private void initData() {
        SketchData newSketchData = new SketchData();
        sketchDataList.add(newSketchData);
        mSketchView.setSketchData(newSketchData);
    }

    private void initSketchGV() {
        sketchGVAdapter = new SketchDataGridAdapter(activity, sketchDataList, new SketchDataGridAdapter.OnActionCallback() {
            @Override
            public void onDeleteCallback(int position) {
                sketchDataList.remove(position);
                sketchGVAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAddCallback() {
                SketchData newSketchData = new SketchData();
                sketchDataList.add(newSketchData);
                mSketchView.updateSketchData(newSketchData);
                mSketchView.setEditMode(SketchView.EDIT_STROKE);//切换笔画编辑模式
                showSketchView(true);
            }

            @Override
            public void onSelectCallback(SketchData sketchData) {
                mSketchView.updateSketchData(sketchData);
                mSketchView.setEditMode(SketchView.EDIT_PHOTO);//切换图片编辑模式
                showSketchView(true);
            }
        });
        sketchGV.setAdapter(sketchGVAdapter);
    }

    private void showSketchView(boolean b) {
        mSketchView.setVisibility(b ? View.VISIBLE : View.GONE);
        sketchGV.setVisibility(!b ? View.VISIBLE : View.GONE);
    }

    private void initSaveDialog() {
        saveET = new EditText(activity);
        saveET.setHint("新文件名");
        saveET.setGravity(Gravity.CENTER);

        saveET.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        saveET.setImeOptions(EditorInfo.IME_ACTION_DONE);
        saveET.setSelectAllOnFocus(true);
        saveET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    saveDialog.dismiss();
                    String input = saveET.getText().toString();
                    saveInUI(input + ".png");
                }
                return true;
            }
        });
        saveDialog = new AlertDialog.Builder(getActivity())
                .setTitle("请输入保存文件名")
                .setMessage("")
                .setView(saveET)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                        String input = saveET.getText().toString();
                        saveInUI(input + ".png");
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScreenUtils.hideInput(saveDialog.getCurrentFocus());
                    }
                })
                .setCancelable(false)
                .create();
    }


    private void initDrawParams() {
        //默认为画笔模式
        strokeMode = STROKE_TYPE_DRAW;

        //画笔宽度缩放基准参数
        Drawable circleDrawable = getResources().getDrawable(R.drawable.circle);
        assert circleDrawable != null;
        size = circleDrawable.getIntrinsicWidth();
    }

    private void initPopupWindows() {
        initStrokePop();
        initEraserPop();
        initTextPop();
    }

    private void initTextPop() {
        textPopupWindow = new PopupWindow(activity);
        textPopupWindow.setContentView(popupTextLayout);
        textPopupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);//宽度200dp
        textPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        textPopupWindow.setFocusable(true);
        textPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        textPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        textPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (!strokeET.getText().toString().equals("")) {
                    StrokeRecord record = new StrokeRecord(strokeType);
                    record.text = strokeET.getText().toString();
                }
            }
        });
    }

    private void initEraserPop() {
        //橡皮擦弹窗
        eraserPopupWindow = new PopupWindow(activity);
        eraserPopupWindow.setContentView(popupEraserLayout);//设置主体布局
        eraserPopupWindow.setWidth(ScreenUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度200dp
//        eraserPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        eraserPopupWindow.setHeight(ScreenUtils.dip2px(getActivity(), eraserPupWindowsDPHeight));//高度自适应
        eraserPopupWindow.setFocusable(true);
        eraserPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        eraserPopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        //橡皮擦宽度拖动条
        eraserSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_ERASER);
            }
        });
        eraserSeekBar.setProgress(SketchView.DEFAULT_ERASER_SIZE);
    }

    private void initStrokePop() {
        //画笔弹窗
        strokePopupWindow = new PopupWindow(activity);
        strokePopupWindow.setContentView(popupStrokeLayout);//设置主体布局
        strokePopupWindow.setWidth(ScreenUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度
//        strokePopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        strokePopupWindow.setHeight(ScreenUtils.dip2px(getActivity(), strokePupWindowsDPHeight));//高度
        strokePopupWindow.setFocusable(true);
        strokePopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        strokePopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        strokeTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int resId = R.drawable.stroke_type_rbtn_draw_checked;
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                    resId = R.drawable.stroke_type_rbtn_line_checked;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                    resId = R.drawable.stroke_type_rbtn_circle_checked;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                    resId = R.drawable.stroke_type_rbtn_rectangle_checked;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                    resId = R.drawable.stroke_type_rbtn_text_checked;
                }
                btn_stroke.setImageResource(resId);
                mSketchView.setStrokeType(strokeType);
                strokePopupWindow.dismiss();//切换画笔后隐藏
            }
        });
        strokeColorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int color = COLOR_BLACK;
                if (checkedId == R.id.stroke_color_black) {
                    color = COLOR_BLACK;
                } else if (checkedId == R.id.stroke_color_red) {
                    color = COLOR_RED;
                } else if (checkedId == R.id.stroke_color_green) {
                    color = COLOR_GREEN;
                } else if (checkedId == R.id.stroke_color_orange) {
                    color = COLOR_ORANGE;
                } else if (checkedId == R.id.stroke_color_blue) {
                    color = COLOR_BLUE;
                }
                mSketchView.setStrokeColor(color);
            }
        });
        //画笔宽度拖动条
        strokeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_DRAW);

                //Toast.makeText(activity, "画笔宽度：" + progress, Toast.LENGTH_SHORT).show();
            }
        });
        strokeSeekBar.setProgress(SketchView.DEFAULT_STROKE_SIZE);
//        strokeColorRG.check(R.id.stroke_color_black);

        //画笔不透明度拖动条
        strokeAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                int alpha = (progress * 255) / 100;//百分比转换成256级透明度
                mSketchView.setStrokeAlpha(alpha);
                strokeAlphaImage.setAlpha(alpha);
            }
        });
        strokeAlphaSeekBar.setProgress(SketchView.DEFAULT_STROKE_ALPHA);
    }


    private void findView(View view) {

        sketchGV = view.findViewById(R.id.sketch_data_gv);

        //画板整体布局
        mSketchView = view.findViewById(R.id.sketch_view);

        controlLayout = view.findViewById(R.id.controlLayout);

        btn_add = view.findViewById(R.id.btn_add);
        btn_stroke = view.findViewById(R.id.btn_stroke);
        btn_eraser = view.findViewById(R.id.btn_eraser);
        btn_undo = view.findViewById(R.id.btn_undo);
        btn_redo = view.findViewById(R.id.btn_redo);
        btn_photo = view.findViewById(R.id.btn_photo);
        btn_background = view.findViewById(R.id.btn_background);
        btn_drag = view.findViewById(R.id.btn_drag);
        btn_save = view.findViewById(R.id.btn_save);
        btn_empty = view.findViewById(R.id.btn_empty);
        btn_send = view.findViewById(R.id.btn_send);
        btn_send_space = view.findViewById(R.id.btn_send_space);
        if (isTeacher) {
            btn_send.setVisibility(View.VISIBLE);
            btn_send_space.setVisibility(View.VISIBLE);
        }

        //设置点击监听
        mSketchView.setOnDrawChangedListener(this);//设置撤销动作监听器
        btn_add.setOnClickListener(this);
        btn_stroke.setOnClickListener(this);
        btn_eraser.setOnClickListener(this);
        btn_undo.setOnClickListener(this);
        btn_redo.setOnClickListener(this);
        btn_empty.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_photo.setOnClickListener(this);
        btn_background.setOnClickListener(this);
        btn_drag.setOnClickListener(this);
        btn_send.setOnClickListener(this);
        mSketchView.setTextWindowCallback(new SketchView.TextWindowCallback() {
            @Override
            public void onText(View anchor, StrokeRecord record) {
                textOffX = record.textOffX;
                textOffY = record.textOffY;
                showTextPopupWindow(anchor, record);
            }
        });

        // popupWindow布局
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity
                .LAYOUT_INFLATER_SERVICE);
        //画笔弹窗布局
        popupStrokeLayout = inflater.inflate(R.layout.popup_sketch_stroke, null);
        strokeImageView = popupStrokeLayout.findViewById(R.id.stroke_circle);
        strokeAlphaImage = popupStrokeLayout.findViewById(R.id.stroke_alpha_circle);
        strokeSeekBar = popupStrokeLayout.findViewById(R.id.stroke_seekbar);
        strokeAlphaSeekBar = popupStrokeLayout.findViewById(R.id.stroke_alpha_seekbar);
        //画笔颜色
        strokeTypeRG = popupStrokeLayout.findViewById(R.id.stroke_type_radio_group);
        strokeColorRG = popupStrokeLayout.findViewById(R.id.stroke_color_radio_group);

        //橡皮擦弹窗布局
        popupEraserLayout = inflater.inflate(R.layout.popup_sketch_eraser, null);
        eraserImageView = popupEraserLayout.findViewById(R.id.stroke_circle);
        eraserSeekBar = popupEraserLayout.findViewById(R.id.stroke_seekbar);
        //文本录入弹窗布局
        popupTextLayout = inflater.inflate(R.layout.popup_sketch_text, null);
        strokeET = popupTextLayout.findViewById(R.id.text_pupwindow_et);
        getSketchSize();//计算选择图片弹窗的高宽
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSketchSize();
    }

    private void getSketchSize() {
        ViewTreeObserver vto = mSketchView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (sketchViewHeight == 0 && sketchViewWidth == 0) {
                    int height = mSketchView.getMeasuredHeight();
                    int width = mSketchView.getMeasuredWidth();
                    sketchViewHeight = height;
                    sketchViewWidth = width;
                    sketchViewRight = mSketchView.getRight();
                    sketchViewBottom = mSketchView.getBottom();
                    Log.i("onPreDraw", sketchViewHeight + "  " + sketchViewWidth);
                    decorHeight = getActivity().getWindow().getDecorView().getMeasuredHeight();
                    decorWidth = getActivity().getWindow().getDecorView().getMeasuredWidth();
                    Log.i("onPreDraw", "decor height:" + decorHeight + "   width:" + decorHeight);
                    int height3 = controlLayout.getMeasuredHeight();
                    int width3 = controlLayout.getMeasuredWidth();
                    Log.i("onPreDraw", "controlLayout  height:" + height3 + "   width:" + width3);
                }
                return true;
            }
        });
        Log.i("getSketchSize", sketchViewHeight + "  " + sketchViewWidth);
    }

    protected void setSeekBarProgress(int progress, int drawMode) {
        int calcProgress = progress > 1 ? progress : 1;
        int newSize = Math.round((size / 100f) * calcProgress);
        int offset = Math.round((size - newSize) / 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(newSize, newSize);
        lp.setMargins(offset, offset, offset, offset);
        if (drawMode == STROKE_TYPE_DRAW) {
            strokeImageView.setLayoutParams(lp);
        } else {
            eraserImageView.setLayoutParams(lp);
        }
        mSketchView.setSize(newSize, drawMode);
    }


    @Override
    public void onDrawChanged() {
        // Undo
        if (mSketchView.getStrokeRecordCount() > 0)
            btn_undo.setAlpha(1f);
        else
            btn_undo.setAlpha(0.4f);
        // Redo
        if (mSketchView.getRedoCount() > 0)
            btn_redo.setAlpha(1f);
        else
            btn_redo.setAlpha(0.4f);
    }

    private void updateGV() {
        sketchGVAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_add) {
            if (mSketchView.getVisibility() == View.VISIBLE) {
                mSketchView.createCurThumbnailBM();
                showSketchView(false);
            } else {
                showSketchView(true);
            }
            updateGV();
        } else if (id == R.id.btn_stroke) {
            if (mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() != STROKE_TYPE_ERASER) {
                showParamsPopupWindow(v, STROKE_TYPE_DRAW);
            } else {
                int checkedId = strokeTypeRG.getCheckedRadioButtonId();
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                }
                mSketchView.setStrokeType(strokeType);
            }
            mSketchView.setEditMode(SketchView.EDIT_STROKE);
            showBtn(btn_stroke);
        } else if (id == R.id.btn_eraser) {
            if (mSketchView.getEditMode() == SketchView.EDIT_STROKE && mSketchView.getStrokeType() == STROKE_TYPE_ERASER) {
                showParamsPopupWindow(v, STROKE_TYPE_ERASER);
            } else {
                mSketchView.setStrokeType(STROKE_TYPE_ERASER);
            }
            mSketchView.setEditMode(SketchView.EDIT_STROKE);
            showBtn(btn_eraser);
        } else if (id == R.id.btn_undo) {
            mSketchView.undo();
        } else if (id == R.id.btn_redo) {
            mSketchView.redo();
        } else if (id == R.id.btn_empty) {
            askForErase();
        } else if (id == R.id.btn_save) {
            if (mSketchView.getRecordCount() == 0) {
                Toast.makeText(getActivity(), "您还没有绘图", Toast.LENGTH_SHORT).show();
            } else {
                showSaveDialog();
            }
        } else if (id == R.id.btn_photo) {
            startMultiImageSelector(REQUEST_IMAGE);
        } else if (id == R.id.btn_background) {
            startMultiImageSelector(REQUEST_BACKGROUND);
        } else if (id == R.id.btn_drag) {
            mSketchView.setEditMode(SketchView.EDIT_PHOTO);
            showBtn(btn_drag);
        } else if (id == R.id.btn_send) {
            if (sendBtnCallback != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String photoName = TEMP_FILE_NAME + TimeUtils.getNowTimeString();
                        sendBtnCallback.onSendBtnClick(saveInOI(TEMP_FILE_PATH, photoName, 50));
                    }
                }).start();
            }
        }
    }

    private void startMultiImageSelector(int request) {
        MultiImageSelector selector = MultiImageSelector.create(getActivity());
        selector.showCamera(false);
        selector.count(9);
        selector.single();
        selector.origin(mSelectPath);
        Bundle boundsBundle = new Bundle();
        Rect rect = new Rect();
        mSketchView.getLocalVisibleRect(rect);
        int[] boundsInts = new int[4];
        //noinspection Range
        mSketchView.getLocationInWindow(boundsInts);
        boundsInts[1] -= ScreenUtils.getStatusBarHeight(activity);
        boundsInts[2] = mSketchView.getWidth();
        boundsInts[3] = mSketchView.getHeight();
        selector.start(this, boundsInts, request);
    }

    private void showSaveDialog() {
        saveDialog.show();
        saveET.setText(TimeUtils.getNowTimeString());
        saveET.selectAll();
        ScreenUtils.showInput(mSketchView);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE) {
            getActivity();
            if (resultCode == Activity.RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                String path = "";
                if (mSelectPath.size() == 1) {
                    path = mSelectPath.get(0);
                } else if (mSelectPath == null || mSelectPath.size() == 0) {
                    Toast.makeText(getActivity(), "图片加载失败,请重试!", Toast.LENGTH_LONG).show();
                }
                //加载图片
                mSketchView.addPhotoByPath(path);
                mSketchView.setEditMode(SketchView.EDIT_PHOTO);
                showBtn(btn_drag);
            }
        } else if (requestCode == REQUEST_BACKGROUND) {//设置背景成功
            getActivity();
            if (resultCode == Activity.RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                String path = "";
                if (mSelectPath.size() == 1) {
                    path = mSelectPath.get(0);
                } else if (mSelectPath == null || mSelectPath.size() == 0) {
                    Toast.makeText(getActivity(), "图片加载失败,请重试!", Toast.LENGTH_LONG).show();
                }
                mSketchView.setBackgroundByPath(path);
                Log.i("imgPath", path);
                //加载图片设置画板背景
            }
        }
    }

    private void showParamsPopupWindow(View anchor, int drawMode) {
        if (BitmapUtils.isLandScreen(activity)) {
            if (drawMode == STROKE_TYPE_DRAW) {
                strokePopupWindow.showAsDropDown(anchor, ScreenUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            } else {
                eraserPopupWindow.showAsDropDown(anchor, ScreenUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            }
        } else {
            if (drawMode == STROKE_TYPE_DRAW) {
//                strokePopupWindow.showAsDropDown(anchor, 0, ScreenUtils.dip2px(activity, -strokePupWindowsDPHeight) - anchor.getHeight());
                strokePopupWindow.showAsDropDown(anchor, 0, 0);
            } else {
//                eraserPopupWindow.showAsDropDown(anchor, 0, ScreenUtils.dip2px(activity, -eraserPupWindowsDPHeight) - anchor.getHeight());
                eraserPopupWindow.showAsDropDown(anchor, 0, 0);
            }
        }
    }

    private void showTextPopupWindow(View anchor, final StrokeRecord record) {
        strokeET.requestFocus();
        strokeET.setTextSize(24f + (mSketchView.strokeSize - 3f) * (64f - 24f) / (100f - 3f));
        strokeET.setSingleLine();
        textPopupWindow.showAsDropDown(anchor, record.textOffX, record.textOffY - mSketchView.getHeight());
        textPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        InputMethodManager imm = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        textPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (!strokeET.getText().toString().equals("")) {
//                    record.text = strokeET.getText().toString();
//                    record.textPaint.setTextSize(strokeET.getTextSize());
//                    record.textWidth = strokeET.getMaxWidth();
//                    mSketchView.addStrokeRecord(record);
                    createTextImage(strokeET.getText().toString(),record);
                }
            }
        });
    }

    // 新增方法：创建文字图片并添加到画板
    private void createTextImage(String text, StrokeRecord record) {
        // 1. 创建透明背景的Bitmap
        TextPaint textPaint = new TextPaint(record.textPaint);
        textPaint.setColor(mSketchView.strokeRealColor); // 使用当前画笔颜色
        //120 神秘参数
        //textPaint.setTextSize(mSketchView.strokeSize * 114); // 调整文字大小
        //textPaint.setTextSize(mSketchView.strokeSize + 14); // 调整文字大小
        float magicfactor = 2f;
        textPaint.setTextSize(sp2px(requireContext(),(24f + (mSketchView.strokeSize - 3f) * (64f - 24f) / (100f - 3f)) * magicfactor));
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();

        // 计算文本尺寸（包含上升和下降部分）
        int padding = 20;
        int width = (int) textPaint.measureText(text) + padding * 2;
        int height = (int) (fontMetrics.descent - fontMetrics.ascent) + padding * 2;

        Bitmap textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas textCanvas = new Canvas(textBitmap);
        FrameLayout whiteboard = getView().findViewById(R.id.whiteBoard);

        // 2. 绘制透明背景
        textCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // 3. 绘制文字
        textCanvas.drawText(text, padding, textPaint.getTextSize() + padding/2, textPaint);

        // 4. 保存临时文件
        String fileName = "text_" + System.currentTimeMillis() + ".png";

        try {

            FileOutputStream out = new FileOutputStream(photoTextFile);
            textBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            addPhotoByPath(Uri.fromFile(photoTextFile).getPath(),
                    2*record.textOffX - whiteboard.getWidth()/2f + width/4f,
                    2*record.textOffY - whiteboard.getHeight()/2f + height/4f);
            // 5. 添加到画板
//            if (photoTextFile != null) {
//                addPhotoByPath(photoTextFile.getAbsolutePath());
//            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void saveInUI(final String imgName) {
        new saveToFileTask().execute(imgName);
    }

    /**
     * show 保存图片到本地文件，耗时操作
     *
     * @param filePath 文件保存路径
     * @param imgName  文件名
     * @param compress 压缩百分比1-100
     * @return 返回保存的图片文件
     * @author TangentLu
     * create at 16/6/17 上午11:18
     */
    public File saveInOI(String filePath, String imgName, int compress) {
        if (!imgName.contains(".png")) {
            imgName += ".png";
        }
        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());
        Bitmap newBM = mSketchView.getResultBitmap();
        Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

        try {
            File dir = new File(filePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(filePath, imgName);
            if (!f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            if (compress >= 1 && compress <= 100)
                newBM.compress(Bitmap.CompressFormat.PNG, compress, out);
            else {
                newBM.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Log.e(TAG, "saveInOI: " + System.currentTimeMillis());

            out.close();
            newBM.recycle();
            newBM = null;
            return f;
        } catch (Exception e) {
            return null;
        }
    }


    private void askForErase() {
        new AlertDialog.Builder(getActivity())
                .setMessage("清空白板内容?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSketchView.erase();
                    }
                })
                .create()
                .show();
    }

    private void showBtn(ImageView iv) {
        btn_eraser.setAlpha(BTN_ALPHA);
        btn_stroke.setAlpha(BTN_ALPHA);
        btn_drag.setAlpha(BTN_ALPHA);
        iv.setAlpha(1f);
    }

    class saveToFileTask extends AsyncTask<String, Void, File> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new AlertDialog.Builder(activity)
                    .setTitle("保存画板")
                    .setMessage("保存中...")
                    .show();
        }

        @Override
        protected File doInBackground(String... photoName) {
            return saveInOI(FILE_PATH, photoName[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file.exists())
                Toast.makeText(getActivity(), file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), "保存失败！", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    public interface OnFragmentReadyListener {
        void onFragmentReady();
    }

    private OnFragmentReadyListener listener;

    public void setOnFragmentReadyListener(OnFragmentReadyListener listener) {
        this.listener = listener;
    }

    public void setPrinterAspectRatio(float printerAspectRatio) {
        this.printerAspectRatio = printerAspectRatio;
    }

    public float getPrinterAspectRatio() {
        return printerAspectRatio;
    }

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        if (listener != null) {
//            listener.onFragmentReady();
//        }
//        final View rootView = view;
//
//        FrameLayout toolbar = view.findViewById(R.id.controlLayout);
//
//        rootView.post(new Runnable() {
//            @Override
//            public void run() {
//                // 获取根视图的宽度
//                int viewWidth = rootView.getWidth();
//                int viewHeight = rootView.getHeight();
//
//                // 计算矩形尺寸
//                int rectWidth = viewWidth;
//                int rectHeight = (int) (viewWidth / printerAspectRatio);
//                int top = (viewHeight - toolbar.getHeight() - rectHeight) / 2;
//
//                // 创建四角边框的 Drawable
//                Drawable cornerBorder = new Drawable() {
//                    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    private final int cornerLength = 250; // 角线长度（像素）
//                    private final int borderWidth = 5;   // 边框粗细（像素）
//
//                    {
//                        paint.setColor(Color.RED);     // 红色边框
//                        paint.setStyle(Paint.Style.STROKE);
//                        paint.setStrokeWidth(borderWidth);
//                    }
//
//                    @Override
//                    public void draw(@NonNull Canvas canvas) {
//                        // 左上角
//                        canvas.drawLine(0, top, cornerLength, top, paint);
//                        canvas.drawLine(0, top, 0, top + cornerLength, paint);
//
//                        // 右上角
//                        canvas.drawLine(rectWidth - cornerLength, top, rectWidth, top, paint);
//                        canvas.drawLine(rectWidth, top, rectWidth, top + cornerLength, paint);
//
//                        // 左下角
//                        canvas.drawLine(0, top + rectHeight - cornerLength, 0, top + rectHeight, paint);
//                        canvas.drawLine(0, top + rectHeight, cornerLength, top + rectHeight, paint);
//
//                        // 右下角
//                        canvas.drawLine(rectWidth - cornerLength, top + rectHeight, rectWidth, top + rectHeight, paint);
//                        canvas.drawLine(rectWidth, top + rectHeight - cornerLength, rectWidth, top + rectHeight, paint);
//                    }
//
//                    @Override
//                    public void setAlpha(int alpha) {
//                        paint.setAlpha(alpha);
//                    }
//
//                    @Override
//                    public void setColorFilter(@Nullable ColorFilter colorFilter) {
//                        paint.setColorFilter(colorFilter);
//                    }
//
//                    @Override
//                    public int getOpacity() {
//                        return PixelFormat.TRANSLUCENT;
//                    }
//                };
//
//                // 设置绘制区域并添加到 Overlay
//                cornerBorder.setBounds(0, top, viewWidth, top + rectHeight);
//                rootView.getOverlay().add(cornerBorder);
////                // 创建矩形Drawable
////                GradientDrawable rectDrawable = new GradientDrawable();
////                rectDrawable.setShape(GradientDrawable.RECTANGLE);
////                rectDrawable.setStroke(5, Color.RED); // 边框宽度5px，红色
////                rectDrawable.setColor(Color.TRANSPARENT); // 透明填充
////
////                // 计算垂直居中位置
////                int left = 0;
////                int top = (viewHeight - rectHeight) / 2;
////                rectDrawable.setBounds(left, top, left + rectWidth, top + rectHeight);
////
////                // 添加Overlay
////                rootView.getOverlay().add(rectDrawable);
//            }
//        });
//    }
    private Drawable currentCornerBorder;
    private void updateCornerBorder(View rootView) {
        // 移除旧边框
        if (currentCornerBorder != null) {
            rootView.getOverlay().remove(currentCornerBorder);
        }

        FrameLayout toolbar = rootView.findViewById(R.id.controlLayout);
        int viewWidth = rootView.getWidth();
        int viewHeight = rootView.getHeight();
        int toolbarHeight = toolbar.getHeight();

        // 核心布局策略
        int rectWidth, rectHeight, left, top;
        float availableHeight = viewHeight - toolbarHeight;
        float viewAspectRatio = (float) viewWidth / availableHeight;

        if (printerAspectRatio < viewAspectRatio) {
            // 高度主导模式：填满垂直空间
            rectHeight = (int) availableHeight;
            rectWidth = (int) (rectHeight * printerAspectRatio);
            left = (viewWidth - rectWidth) / 2; // 水平居中
            top = 0;               // 从工具栏底部开始
        } else {
            // 宽度主导模式：填满水平空间
            rectWidth = viewWidth;
            rectHeight = (int) (viewWidth / printerAspectRatio);
            left = 0;
            top = (int)(availableHeight - rectHeight) / 2; // 垂直居中
        }

        // 边界保护
        rectWidth = Math.min(rectWidth, viewWidth);
        rectHeight = Math.min(rectHeight, (int)availableHeight);

        // 创建动态边框
        currentCornerBorder = createCornerBorderDrawable(rootView, rectWidth, rectHeight, left, top);
        currentCornerBorder.setBounds(left, top, left + rectWidth, top + rectHeight);
        rootView.getOverlay().add(currentCornerBorder);
    }

    private Drawable createCornerBorderDrawable(View rootView,int rectWidth, int rectHeight, int left, int top) {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final int cornerLength = 100; // 角线长度（像素）
            private final int borderWidth = 5;    // 边框粗细（像素）

            {
                // 初始化边框画笔
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(borderWidth);

                // 初始化文字画笔
                textPaint.setColor(Color.WHITE);        // 文字颜色
                textPaint.setTextSize(40);             // 文字大小（像素）
                textPaint.setTextAlign(Paint.Align.CENTER); // 水平居中
            }

            @Override
            public void draw(@NonNull Canvas canvas) {

                // 绘制四角边框（适配新坐标）
                // 左上角
                canvas.drawLine(left, top, left + cornerLength, top, paint);
                canvas.drawLine(left, top, left, top + cornerLength, paint);

                // 右上角
                canvas.drawLine(left + rectWidth - cornerLength, top, left + rectWidth, top, paint);
                canvas.drawLine(left + rectWidth, top, left + rectWidth, top + cornerLength, paint);

                // 左下角
                canvas.drawLine(left, top + rectHeight - cornerLength, left, top + rectHeight, paint);
                canvas.drawLine(left, top + rectHeight, left + cornerLength, top + rectHeight, paint);

                // 右下角
                canvas.drawLine(left + rectWidth - cornerLength, top + rectHeight, left + rectWidth, top + rectHeight, paint);
                canvas.drawLine(left + rectWidth, top + rectHeight - cornerLength, left + rectWidth, top + rectHeight, paint);

                // 智能文字定位（根据新坐标调整）
                float centerX = left + rectWidth / 2f;
                float textY = calculateTextPosition(canvas.getHeight(), top, rectHeight);


                // 智能文字布局
                String widthText = "宽度: " + platformWidth + "mm";
                String heightText = "高度: " + platformHeight + "mm";

                // 计算可用空间
                int viewHeight = rootView.getHeight();
                int textAreaHeight = (int) (textPaint.getTextSize() * 2 + 30); // 文字区域预估高度
                boolean canDrawBelow = (top + rectHeight + textAreaHeight) < viewHeight;
                boolean canDrawAbove = (top - textAreaHeight) > 0;

                // 动态选择绘制位置（优先下方，次选上方，最后覆盖在内部）
                float textBaseY;
                if (canDrawBelow) {
                    textBaseY = top + rectHeight + 40; // 下方留出间距
                } else if (canDrawAbove) {
                    textBaseY = top - 20; // 上方留出间距
                } else {
                    textBaseY = top + rectHeight/2f; // 绘制在矩形内部中央
                }

                // 计算文字背景
                float maxTextWidth = Math.max(
                        textPaint.measureText(widthText),
                        textPaint.measureText(heightText)
                );
                float bgPadding = 20;
                RectF bgRect = new RectF(
                        left + rectWidth/2f - maxTextWidth/2f - bgPadding,
                        textBaseY - textPaint.getTextSize() * 2 - 10,
                        left + rectWidth/2f + maxTextWidth/2f + bgPadding,
                        textBaseY + 10
                );

                // 绘制半透明背景
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#80000000"));
                canvas.drawRoundRect(bgRect, 16, 16, paint);

                // 绘制文字（自动调整位置）
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);

                float firstLineY = textBaseY - textPaint.getTextSize() - 5;
                float secondLineY = textBaseY;

                // 如果绘制在内部，调整颜色以提高可读性
                if (!canDrawBelow && !canDrawAbove) {
                    textPaint.setColor(Color.YELLOW); // 使用更醒目的颜色
                    canvas.drawText(widthText, left + rectWidth/2f, firstLineY, textPaint);
                    canvas.drawText(heightText, left + rectWidth/2f, secondLineY, textPaint);
                    textPaint.setColor(Color.WHITE); // 恢复默认颜色
                } else {
                    canvas.drawText(widthText, left + rectWidth/2f, firstLineY, textPaint);
                    canvas.drawText(heightText, left + rectWidth/2f, secondLineY, textPaint);
                }
            }

            private float calculateTextPosition(int viewHeight, int rectTop, int rectHeight) {
                // 动态计算文字Y坐标
                float textAreaHeight = textPaint.getTextSize() * 2 + 40;

                // 优先在下方
                if (rectTop + rectHeight + textAreaHeight < viewHeight) {
                    return rectTop + rectHeight + 40;
                }
                // 次选在上方
                if (rectTop - textAreaHeight > 0) {
                    return rectTop - 20;
                }
                // 最后在内部
                return rectTop + rectHeight/2f;
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }

            // ... 其他原有方法（setAlpha, setColorFilter, getOpacity）保持不变 ...
        };
        }

    // 供外部调用的刷新方法
    public void refreshCornerBorder() {
        if (getView() != null) {
            getView().post(() -> updateCornerBorder(getView()));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (listener != null) {
            listener.onFragmentReady();
        }

        // 初始自动调用（可选）
        view.post(() -> updateCornerBorder(view));
    }

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        if (listener != null) {
//            listener.onFragmentReady();
//        }
//        final View rootView = view;
//
//        FrameLayout toolbar = view.findViewById(R.id.controlLayout);
//
//        rootView.post(new Runnable() {
//            @Override
//            public void run() {
//                // 获取根视图的宽度
//                int viewWidth = rootView.getWidth();
//                int viewHeight = rootView.getHeight();
//
//                // 计算矩形尺寸
//                int rectWidth = viewWidth;
//                int rectHeight = (int) (viewWidth / printerAspectRatio);
//                int top = (viewHeight - toolbar.getHeight() - rectHeight) / 2;
//
//                // 创建四角边框的 Drawable
//                Drawable cornerBorder = new Drawable() {
//                    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    private final int cornerLength = 100; // 角线长度（像素）
//                    private final int borderWidth = 5;    // 边框粗细（像素）
//
//                    {
//                        // 初始化边框画笔
//                        paint.setColor(Color.RED);
//                        paint.setStyle(Paint.Style.STROKE);
//                        paint.setStrokeWidth(borderWidth);
//
//                        // 初始化文字画笔
//                        textPaint.setColor(Color.WHITE);        // 文字颜色
//                        textPaint.setTextSize(40);             // 文字大小（像素）
//                        textPaint.setTextAlign(Paint.Align.CENTER); // 水平居中
//                    }
//
//                    @Override
//                    public void draw(@NonNull Canvas canvas) {
//                        // 绘制四角边框
//                        // 左上角
//                        canvas.drawLine(0, top, cornerLength, top, paint);
//                        canvas.drawLine(0, top, 0, top + cornerLength, paint);
//
//                        // 右上角
//                        canvas.drawLine(rectWidth - cornerLength, top, rectWidth, top, paint);
//                        canvas.drawLine(rectWidth, top, rectWidth, top + cornerLength, paint);
//
//                        // 左下角
//                        canvas.drawLine(0, top + rectHeight - cornerLength, 0, top + rectHeight, paint);
//                        canvas.drawLine(0, top + rectHeight, cornerLength, top + rectHeight, paint);
//
//                        // 右下角
//                        canvas.drawLine(rectWidth - cornerLength, top + rectHeight, rectWidth, top + rectHeight, paint);
//                        canvas.drawLine(rectWidth, top + rectHeight - cornerLength, rectWidth, top + rectHeight, paint);
//
//                        // 添加宽度和高度的文字标注
//                        String widthText = "宽度: " + platformWidth + "mm";
//                        String heightText = "高度: " + platformHeight + "mm";
//
//                        // 计算文字位置（居中在边框下方）
//                        int textPadding = 20; // 文字与边框的间距
//                        float centerX = rectWidth / 2f;
//                        float widthTextY = top + rectHeight + textPadding + textPaint.getTextSize();
//                        float heightTextY = widthTextY + textPaint.getTextSize() + 10; // 行间距
//
//                        // 绘制文字背景（可选）
//                        paint.setStyle(Paint.Style.FILL);
//                        paint.setColor(Color.parseColor("#80000000")); // 半透明黑色背景
//                        float textWidth = Math.max(
//                                textPaint.measureText(widthText),
//                                textPaint.measureText(heightText)
//                        );
//                        canvas.drawRect(
//                                centerX - textWidth / 2 - 20,
//                                widthTextY - textPaint.getTextSize() - 10,
//                                centerX + textWidth / 2 + 20,
//                                heightTextY + 10,
//                                paint
//                        );
//
//                        // 恢复边框颜色
//                        paint.setColor(Color.RED);
//                        paint.setStyle(Paint.Style.STROKE);
//
//                        // 绘制文字
//                        canvas.drawText(widthText, centerX, widthTextY, textPaint);
//                        canvas.drawText(heightText, centerX, heightTextY, textPaint);
//                    }
//
//                    @Override
//                    public void setAlpha(int alpha) {
//                        paint.setAlpha(alpha);
//                    }
//
//                    @Override
//                    public void setColorFilter(@Nullable ColorFilter colorFilter) {
//                        paint.setColorFilter(colorFilter);
//                    }
//
//                    @Override
//                    public int getOpacity() {
//                        return PixelFormat.TRANSLUCENT;
//                    }
//
//                    // ... 其他原有方法（setAlpha, setColorFilter, getOpacity）保持不变 ...
//                };
//
//                // 设置绘制区域并添加到 Overlay
//                cornerBorder.setBounds(0, top, viewWidth, top + rectHeight);
//                rootView.getOverlay().add(cornerBorder);
//            }
//        });
//    }

}
