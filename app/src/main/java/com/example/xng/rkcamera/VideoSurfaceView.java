package com.example.xng.rkcamera;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView {
    static final String TAG = "VideoSurfaceView";
    private Activity mContext = null;

    public VideoSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoSurfaceView(Context context) {
        super(context);
    }

    public void setContext(Activity context) {
        if (mContext == null)
            mContext = context;
    }

    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("tiantian", "onMeasure");
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("tiantian", "widthSize: " + widthSize + ", heightSize: " + heightSize);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //setMeasuredDimension(widthSize, heightSize/2);
            setMeasuredDimension(widthSize, heightSize);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }
    */
/*
    @Override
    public void layout(int l, int t, int r, int b) { //view显示区域高宽 (1776, 1008)
        //Log.d("tiantian", "l: " + l + ", t: " + t + ", r: " + r + ", b: " + b);

        //获取应用区域高宽，包含标题栏 (1776, 1080)
        //Display display = mContext.getWindowManager().getDefaultDisplay();
        //r  = display.getWidth();
        //b  = display.getHeight();

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //super.layout(l, t, r, b/2); //tiantian, 开启竖屏surface显示在屏幕上端
            super.layout(l, t, r, b);
        }  else if (orientation == Configuration.ORIENTATION_LANDSCAPE){
            super.layout(l, t, r, b);
        }
    }
*/
}