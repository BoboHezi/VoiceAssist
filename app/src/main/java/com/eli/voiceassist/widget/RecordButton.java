package com.eli.voiceassist.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.HashMap;
import java.util.Map;

public class RecordButton extends View {

    private static final String TAG = "RecordButton";

    private int circleColor = 0x884FACEF;

    private Context context;

    private Map<String, Integer> drawSize;

    private int circleRadius;

    private int width = 300;

    private Paint paint;

    private float breathStrength = (float) 0.1;

    private BreathThread breath;

    private OnBreathListener listener;

    public RecordButton(Context context) {
        this(context, null);
    }

    public RecordButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecordButton(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        this.context = context;
        paint = new Paint();
        paint.setAntiAlias(true);
        drawSize = new HashMap<>();

        breath = new BreathThread();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int minSize = Math.min((widthSpecMode == MeasureSpec.AT_MOST ? 0xffffffff : widthSize), (heightSpecMode == MeasureSpec.AT_MOST ? 0xffffffff : heightSize));
        width = (minSize == 0xffffffff) ? width : minSize;
        //设置组件尺寸
        setMeasuredDimension(width, width);
        circleRadius = width / 2;
        calculateSizeSize(circleRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawColor(0x99ffffff);

        //绘制晕染
        paint.setStyle(Paint.Style.FILL);
        RadialGradient gradient = new RadialGradient(width / 2, width / 2, width * breathStrength, 0xFF136DAE, 0x0, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(width / 2, width / 2, width / 2, paint);

        //绘制实心圆
        paint.setColor(0xFF136DAE);
        paint.setShader(null);
        canvas.drawCircle(width / 2, width / 2, circleRadius / 2, paint);

        //绘制圆环
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(circleRadius / 30);
        paint.setColor(circleColor);
        canvas.drawCircle(width / 2, width / 2, circleRadius / 2 + circleRadius / 60, paint);
        //绘制图形
        canvas.drawArc(drawSize.get("topArcLeft"), drawSize.get("topArcTop"), drawSize.get("topArcRight"), drawSize.get("topArcBottom"), 180, 180, false, paint);
        canvas.drawArc(drawSize.get("bottomArcLeft"), drawSize.get("bottomArcTop"), drawSize.get("bottomArcRight"), drawSize.get("bottomArcBottom"), 0, 180, false, paint);
        canvas.drawLine(drawSize.get("leftLineLeft"), drawSize.get("leftLineTop"), drawSize.get("leftLineRight"), drawSize.get("leftLineBottom"), paint);
        canvas.drawLine(drawSize.get("rightLineLeft"), drawSize.get("rightLineTop"), drawSize.get("rightLineRight"), drawSize.get("rightLineBottom"), paint);
        canvas.drawArc(drawSize.get("bigArcLeft"), drawSize.get("bigArcTop"), drawSize.get("bigArcRight"), drawSize.get("bigArcBottom"), 10, 160, false, paint);
        canvas.drawLine(drawSize.get("centerLineLeft"), drawSize.get("centerLineTop"), drawSize.get("centerLineRight"), drawSize.get("centerLineBottom"), paint);
        canvas.drawLine(drawSize.get("endLineLeft"), drawSize.get("endLineTop"), drawSize.get("endLineRight"), drawSize.get("endLineBottom"), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            this.animate().scaleX(0.87f).scaleY(0.87f).setDuration(150).setInterpolator(new DecelerateInterpolator());
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            this.animate().scaleX(1).scaleY(1).setDuration(150).setInterpolator(new DecelerateInterpolator());
            //Util.playSound(context, R.raw.effect_tick);
            //this.playSoundEffect(SoundEffectConstants.CLICK);

            if (!breath.isBreathing()) {
                startBreath();
                if (listener != null)
                    listener.onBreathStateChanged(OnBreathListener.BREATH_STATE_START);
            } else {
                stopBreath();
                if (listener != null)
                    listener.onBreathStateChanged(OnBreathListener.BREATH_STATE_STOP);
            }
        }
        return super.onTouchEvent(event);
    }

    public void setOnBreathListener(OnBreathListener listener) {
        this.listener = listener;
    }

    public void startBreath() {
        if (breath != null)
            breath.stopBreath();
        breath = null;
        breath = new BreathThread();
        breath.start();
    }

    public void stopBreath() {
        if (breath == null)
            return;

        breath.stopBreath();
    }

    private void calculateSizeSize(int width) {
        int halfSize = width / 2;
        int s30 = width / 10;
        int s40 = (int) (width / 7.5);
        int s80 = s40 * 2;
        int s10 = width / 30;
        int s50 = s40 + s10;

        drawSize.put("topArcLeft", halfSize - s30 + this.width / 4);
        drawSize.put("topArcTop", s30 + this.width / 4);
        drawSize.put("topArcRight", halfSize + s30 + this.width / 4);
        drawSize.put("topArcBottom", s30 * 3 + this.width / 4);

        drawSize.put("bottomArcLeft", halfSize - s30 + this.width / 4);
        drawSize.put("bottomArcTop", halfSize - s50 + this.width / 4);
        drawSize.put("bottomArcRight", halfSize + s30 + this.width / 4);
        drawSize.put("bottomArcBottom", halfSize + s10 + this.width / 4);

        drawSize.put("leftLineLeft", halfSize - s30 + this.width / 4);
        drawSize.put("leftLineTop", s30 * 2 + this.width / 4);
        drawSize.put("leftLineRight", halfSize - s30 + this.width / 4);
        drawSize.put("leftLineBottom", halfSize - s10 * 2 + this.width / 4);

        drawSize.put("rightLineLeft", halfSize + s30 + this.width / 4);
        drawSize.put("rightLineTop", s30 * 2 + this.width / 4);
        drawSize.put("rightLineRight", halfSize + s30 + this.width / 4);
        drawSize.put("rightLineBottom", halfSize - s10 * 2 + this.width / 4);

        drawSize.put("bigArcLeft", halfSize - s80 + this.width / 4);
        drawSize.put("bigArcTop", s10 * 2 + this.width / 4);
        drawSize.put("bigArcRight", halfSize + s80 + this.width / 4);
        drawSize.put("bigArcBottom", halfSize + s30 + this.width / 4);

        drawSize.put("centerLineLeft", halfSize + this.width / 4);
        drawSize.put("centerLineTop", halfSize + s30 + this.width / 4);
        drawSize.put("centerLineRight", halfSize + this.width / 4);
        drawSize.put("centerLineBottom", width - s50 + this.width / 4);

        drawSize.put("endLineLeft", halfSize - s50 + this.width / 4);
        drawSize.put("endLineTop", width - s50 + this.width / 4);
        drawSize.put("endLineRight", halfSize + s50 + this.width / 4);
        drawSize.put("endLineBottom", width - s50 + this.width / 4);
    }

    class BreathThread extends Thread {

        private boolean isBreathing = false;

        public void stopBreath() {
            isBreathing = false;
            super.interrupt();
        }

        public boolean isBreathing() {
            return isBreathing;
        }

        @Override
        public void run() {
            Log.i(TAG, "breath start");
            isBreathing = true;
            int sleepTime;
            float breathOffset = (float) 0.05;
            int alphaOffset = 10;
            int lineAlpha = 85;
            while (true) {
                if (breathStrength > 0.8 || breathStrength < 0.1) {
                    alphaOffset = -alphaOffset;
                    breathOffset = - breathOffset;
                }

                breathStrength += breathOffset;
                lineAlpha += alphaOffset;
                circleColor = (lineAlpha << 24) + ((circleColor << 8) >> 8);

                sleepTime = (int) (100 + breathStrength * 50);
                postInvalidate();
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    isBreathing = false;
                } finally {
                    if (!isBreathing) {
                        breathStrength = (float) 0.1;
                        circleColor = 0x884FACEF;
                        postInvalidate();
                        return;
                    }
                }
            }
        }
    }

    public interface OnBreathListener {
        int BREATH_STATE_START = 1;
        int BREATH_STATE_STOP = 2;
        void onBreathStateChanged(int state);
    }
}
