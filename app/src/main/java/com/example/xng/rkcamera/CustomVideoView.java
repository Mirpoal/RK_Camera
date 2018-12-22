package com.example.xng.rkcamera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;
/**
 * Created by Xng on 2016/8/11.
 */
/**
 * 自定义播放器 ,使VideoView的宽度，高度随xml设置的高度，宽度
 */
public class CustomVideoView extends VideoView {

    /** VideoView的宽度 */
    private int mVideoWidth = 0;
    /** VideoView的高度 */
    private int mVideoHeight = 0;
    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 下面的代码是让视频的播放的长宽是根据你设置的参数来决定
        int  width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
