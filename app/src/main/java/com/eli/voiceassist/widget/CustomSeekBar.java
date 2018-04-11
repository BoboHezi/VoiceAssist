package com.eli.voiceassist.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.eli.voiceassist.R;

/**
 * 自定义进度条
 *
 * @author eli chang
 */
public class CustomSeekBar extends View {

    private static final String TAG = "elifli";

    private int eBackColor;
    private int eProgressColor;
    private float eBackHeight;
    private float eProgressHeight;

    private int realWidth;
    private int realHeight;

    private int percent;

    private int radius = 12;

    private float splitX = radius;

    private Paint paint;

    OnPercentChangedListener listener;

    public CustomSeekBar(Context context) {
        this(context, null);
    }

    public CustomSeekBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CustomSeekBar(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        paint = new Paint();
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.styleable_video_seek_bar);
        eBackColor = typedArray.getColor(R.styleable.styleable_video_seek_bar_seekBar_backColor, Color.GRAY);
        eBackHeight = typedArray.getDimension(R.styleable.styleable_video_seek_bar_seekBar_backHeight, 4);

        eProgressColor = typedArray.getColor(R.styleable.styleable_video_seek_bar_seekBar_progressColor, Color.RED);
        eProgressHeight = typedArray.getDimension(R.styleable.styleable_video_seek_bar_seekBar_progressHeight, 4);

        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        realWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        realHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        setPercent(percent);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        paint.setColor(eBackColor);
        paint.setStrokeWidth(eBackHeight);
        paint.setAntiAlias(true);
        //draw line negative
        canvas.drawLine(splitX, realHeight / 2, realWidth - radius, realHeight / 2, paint);

        paint.setColor(eProgressColor);
        paint.setStrokeWidth(eProgressHeight);
        //draw line positive
        canvas.drawLine(radius, realHeight / 2, splitX, realHeight / 2, paint);
        //draw point
        canvas.drawCircle(splitX, realHeight / 2, radius, paint);
    }

    /**
     * 处理触摸事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        splitX = event.getX();
        if (splitX < radius) {
            splitX = radius;
        } else if (splitX + radius > realWidth) {
            splitX = realWidth - radius;
        }
        postInvalidate();

        if (event.getAction() == MotionEvent.ACTION_UP) {
            percent = (int) (((splitX - radius) / (realWidth - 2 * radius)) * 100);
            if (listener != null)
                listener.onPercentChanged(percent);
        }
        return super.onTouchEvent(event);
    }

    public int getPercent() {
        return percent;
    }

    /**
     * set percent
     *
     * @param percent
     */
    public void setPercent(int percent) {
        this.percent = percent;
        splitX = ((percent * (realWidth - 2 * radius)) / 100) + radius;
        postInvalidate();
    }

    public void setListener(OnPercentChangedListener listener) {
        this.listener = listener;
    }

    public interface OnPercentChangedListener {
        void onPercentChanged(int percent);
    }
}
