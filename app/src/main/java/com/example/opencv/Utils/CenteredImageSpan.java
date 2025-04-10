package com.example.opencv.Utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class CenteredImageSpan extends ImageSpan {
    public CenteredImageSpan(Drawable drawable) {
        super(drawable);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            Paint.FontMetricsInt pfm = paint.getFontMetricsInt();
            // 让图片垂直居中
            int fontHeight = pfm.descent - pfm.ascent;
            int drHeight = rect.bottom - rect.top;

            int centerY = pfm.ascent + fontHeight / 2;
            fm.ascent = centerY - drHeight / 2;
            fm.top = fm.ascent;
            fm.bottom = centerY + drHeight / 2;
            fm.descent = fm.bottom;
        }

        return rect.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getDrawable();
        canvas.save();

        int transY = ((bottom - top) - b.getBounds().bottom) / 2 + top;
        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }
}
