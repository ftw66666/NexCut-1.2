package com.example.opencv.whiteboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.opencv.Constant;

public class DragBoxView extends View {
    // 原始尺寸（用户输入的数值）
    private int originalBigBoxWidth = 400;
    private int originalBigBoxHeight = 400;

    // 实际显示尺寸和变换矩阵
    private RectF displayBigBoxRect = new RectF();
    private Matrix transformMatrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();

    // 小框相关参数（基于原始坐标系）
    private RectF originalSmallBoxRect = new RectF(0, 0, 100, 100);
    private RectF displaySmallBoxRect = new RectF();

    private Paint paint = new Paint();
    private boolean isDragging = false;

    // 坐标监听接口
    public interface PositionListener {
        void onPositionChanged(float x, float y);
    }
    private PositionListener positionListener;

    public DragBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateTransformation();
    }

    private void updateTransformation() {
        // 计算保持比例的显示矩形
        float viewAspect = (float)getWidth() / getHeight();
        float boxAspect = (float)originalBigBoxWidth / originalBigBoxHeight;

        float scale;
        if (viewAspect > boxAspect) { // 高度方向充满
            scale = (float)getHeight() / originalBigBoxHeight;
            float displayWidth = originalBigBoxWidth * scale;
            displayBigBoxRect.set(
                    (getWidth() - displayWidth)/2, 0,
                    (getWidth() + displayWidth)/2, getHeight()
            );
        } else { // 宽度方向充满
            scale = (float)getWidth() / originalBigBoxWidth;
            float displayHeight = originalBigBoxHeight * scale;
            displayBigBoxRect.set(
                    0, (getHeight() - displayHeight)/2,
                    getWidth(), (getHeight() + displayHeight)/2
            );
        }

        // 创建坐标变换矩阵
        transformMatrix.setRectToRect(
                new RectF(0, 0, originalBigBoxWidth, originalBigBoxHeight),
                displayBigBoxRect,
                Matrix.ScaleToFit.CENTER
        );
        transformMatrix.invert(inverseMatrix);

        updateSmallBoxPosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制大框
        paint.setColor(Color.BLUE);
        canvas.drawRect(displayBigBoxRect, paint);

        // 变换并绘制小框
        transformMatrix.mapRect(displaySmallBoxRect, originalSmallBoxRect);
        paint.setColor(Color.RED);
        canvas.drawRect(displaySmallBoxRect, paint);

        // 触发坐标更新
        if (positionListener != null) {
            positionListener.onPositionChanged(
                    originalSmallBoxRect.left,
                    originalBigBoxHeight - originalSmallBoxRect.bottom // 转换为左下角坐标系
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float[] point = new float[]{event.getX(), event.getY()};
        inverseMatrix.mapPoints(point); // 转换到原始坐标系

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (displaySmallBoxRect.contains(event.getX(), event.getY())) {
                    isDragging = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    // 在原始坐标系中计算新位置
                    float halfWidth = originalSmallBoxRect.width()/2;
                    float halfHeight = originalSmallBoxRect.height()/2;

                    float newLeft = point[0] - halfWidth;
                    float newTop = point[1] - halfHeight;

                    // 限制边界
                    newLeft = Math.max(0, Math.min(newLeft, originalBigBoxWidth - 2*halfWidth));
                    newTop = Math.max(0, Math.min(newTop, originalBigBoxHeight - 2*halfHeight));

                    originalSmallBoxRect.set(
                            newLeft,
                            newTop,
                            newLeft + 2*halfWidth,
                            newTop + 2*halfHeight
                    );
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
        }
        return true;
    }

    // 设置大框原始尺寸
    public void setBigBoxSize(int width, int height) {
        originalBigBoxWidth = width;
        originalBigBoxHeight = height;
        Constant.PlatformWidth= width;
        Constant.PlatformHeight = height;
        updateTransformation();
        invalidate();
    }

    // 设置小框原始尺寸
    public void setSmallBoxSize(int width, int height) {
        originalSmallBoxRect.right = originalSmallBoxRect.left + width;
        originalSmallBoxRect.bottom = originalSmallBoxRect.top + height;
        Constant.PrintWidth = width;
        Constant.PrintHeight = height;
        updateSmallBoxPosition();
        invalidate();
    }

    // 设置小框位置（基于左下角坐标系）
    public void setSmallBoxPosition(float x, float y) {
        float height = originalSmallBoxRect.height();
        originalSmallBoxRect.set(
                x,
                originalBigBoxHeight - y - height, // 转换为内部坐标系
                x + originalSmallBoxRect.width(),
                originalBigBoxHeight - y
        );
        invalidate();
    }

    private void updateSmallBoxPosition() {
        // 保持当前位置的比例关系
        float centerX = originalSmallBoxRect.centerX();
        float centerY = originalSmallBoxRect.centerY();
        float width = originalSmallBoxRect.width();
        float height = originalSmallBoxRect.height();

        // 限制在更新后的大框范围内
        centerX = Math.max(width/2, Math.min(centerX, originalBigBoxWidth - width/2));
        centerY = Math.max(height/2, Math.min(centerY, originalBigBoxHeight - height/2));

        originalSmallBoxRect.set(
                centerX - width/2,
                centerY - height/2,
                centerX + width/2,
                centerY + height/2
        );
    }

    public void setPositionListener(PositionListener listener) {
        this.positionListener = listener;
    }
    // DragBoxView.java
        public int getBigBoxWidth() { return originalBigBoxWidth; }
        public int getBigBoxHeight() { return originalBigBoxHeight; }
        public int getSmallBoxWidth() { return (int)originalSmallBoxRect.width(); }
        public int getSmallBoxHeight() { return (int)originalSmallBoxRect.height(); }
        public float getSmallBoxPosX() { return originalSmallBoxRect.left; }
        public float getSmallBoxPosY() { return originalBigBoxHeight - originalSmallBoxRect.bottom; }
}