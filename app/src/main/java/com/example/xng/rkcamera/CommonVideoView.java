package com.example.xng.rkcamera;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


/**
 * qiangyu on 1/26/16 15:33
 * csdn博客:http://blog.csdn.net/yissan
 */
public class CommonVideoView extends FrameLayout implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, View.OnTouchListener, View.OnClickListener, Animator.AnimatorListener, SeekBar.OnSeekBarChangeListener {
    static final String TAG = "CommonVideoView";
    private final int UPDATE_VIDEO_SEEKBAR = 1000;
    private final int IMAGE_SHOOT_FINISH = 999;

    private Context context;
    private FrameLayout viewBox;
    private MyVideoView videoView;
    private LinearLayout videoPauseBtn;
    private ImageView videoNextImg;
    private ImageView videoLastImg;
    private LinearLayout videoShootBtn;
    private LinearLayout screenSwitchBtn;
    private LinearLayout touchStatusView;
    private LinearLayout videoControllerLayout;
    private LinearLayout rltitile;
    private ImageView touchStatusImg;
    private ImageView videoPauseImg;
    private ImageView videoShootImg;
    private ImageView videoDelImg;
    private ImageView playback;
    private TextView touchStatusTime;
    private TextView videoCurTimeText;
    private TextView videoTotalTimeText;
    private TextView tvTitle;
    private SeekBar videoSeekBar;

    private ProgressBar progressBar;

    private int duration;
    private String formatTotalTime;

    private Timer timer = new Timer();

    private float touchLastX;
    //定义用seekBar当前的位置，触摸快进的时候显示时间
    private int position;
    private int touchStep = 1000;//快进的时间，1秒
    private int touchPosition = -1;

    private boolean rlTitleShow = true;
    private boolean videoControllerShow = true;//底部状态栏的显示状态
    private boolean animation = false;

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            videoHandler.sendEmptyMessage(UPDATE_VIDEO_SEEKBAR);
            timer.cancel();
        }
    };

    private Handler videoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_VIDEO_SEEKBAR:
                    if (videoView.isPlaying()) {
                        videoSeekBar.setProgress(videoView.getCurrentPosition());
                    }
                    break;
                case IMAGE_SHOOT_FINISH:
                    Bitmap bitmap = (Bitmap)msg.obj;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                    String fname = "/sdcard/RkCamera/RkVideo/" + sdf.format(new Date()) + ".png";
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(fname);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(getContext(), getContext().getString(R.string.screen_shot_success_msg), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "截取成功！！！！！！！！");
                    break;
            }
        }
    };

    public CommonVideoView(Context context) {
        this(context,null);
    }

    public CommonVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommonVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void start(String url){
        Log.d(TAG, "start, url: " + url);
        videoPauseBtn.setEnabled(false);
        videoSeekBar.setEnabled(false);
        videoView.setVideoURI(Uri.parse(url));
        tvTitle.setText(url);
        videoView.start();
        Log.d(TAG, "有运行到这里");
    }

    public void setFullScreen(){
        touchStatusImg.setImageResource(R.mipmap.iconfont_exit);
        this.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        videoView.requestLayout();
    }

    public void setNormalScreen(){
        touchStatusImg.setImageResource(R.mipmap.iconfont_enter_32);
        this.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,400));
        videoView.requestLayout();
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "onFinishInflate");
        super.onFinishInflate();
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(context).inflate(R.layout.common_video_view,null);
        viewBox = (FrameLayout) view.findViewById(R.id.viewBox);
        videoView = (MyVideoView) view.findViewById(R.id.videoView);
        videoPauseBtn = (LinearLayout) view.findViewById(R.id.videoPauseBtn);
        videoNextImg = (ImageView)view.findViewById(R.id.videoNextImg);
        videoLastImg = (ImageView)view.findViewById(R.id.videoLastImg);
        videoShootBtn = (LinearLayout)view.findViewById(R.id.videoShootBtn);
        videoDelImg = (ImageView) view.findViewById(R.id.videoDelImg);
        screenSwitchBtn = (LinearLayout) view.findViewById(R.id.screen_status_btn);
        videoControllerLayout = (LinearLayout) view.findViewById(R.id.videoControllerLayout);
        rltitile = (LinearLayout)view.findViewById(R.id.rltitle);
        touchStatusView = (LinearLayout) view.findViewById(R.id.touch_view);
        touchStatusImg = (ImageView) view.findViewById(R.id.touchStatusImg);
        touchStatusTime = (TextView) view.findViewById(R.id.touch_time);
        videoCurTimeText = (TextView) view.findViewById(R.id.videoCurTime);
        videoTotalTimeText = (TextView) view.findViewById(R.id.videoTotalTime);
        tvTitle = (TextView)view.findViewById(R.id.tvtitle);
        videoSeekBar = (SeekBar) view.findViewById(R.id.videoSeekBar);
        videoPauseImg = (ImageView) view.findViewById(R.id.videoPauseImg);
        playback = (ImageView)view.findViewById(R.id.playback);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        videoPauseBtn.setOnClickListener(this);
        videoShootBtn.setOnClickListener(this);
        videoSeekBar.setOnSeekBarChangeListener(this);
        videoPauseBtn.setOnClickListener(this);
        //playback.setOnClickListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        screenSwitchBtn.setOnClickListener(this);
        //注册在设置或播放过程中发生错误时调用的回调函数。如果未指定回调函数，或回调函数返回false，VideoView 会通知用户发生了错误。
        videoView.setOnErrorListener(this);
        viewBox.setOnTouchListener(this);
        viewBox.setOnClickListener(this);

        addView(view);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        duration = videoView.getDuration();
        int[] time = getMinuteAndSecond(duration);
        videoTotalTimeText.setText(String.format("%02d:%02d", time[0], time[1]));
        formatTotalTime = String.format("%02d:%02d", time[0], time[1]);
        videoSeekBar.setMax(duration);
        progressBar.setVisibility(View.GONE);
        mp.start();
        videoPauseBtn.setEnabled(true);
        videoSeekBar.setEnabled(true);
        videoShootBtn.setEnabled(true);
//        playback.setEnabled(true);
        videoPauseImg.setImageResource(R.drawable.play_stop);
        if(timer != null){
            if(timerTask!=null){
                timerTask.cancel();
            }
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    videoHandler.sendEmptyMessage(UPDATE_VIDEO_SEEKBAR);
                }
            };
            timer.schedule(timerTask, 0, 1000);//这个bug要解!
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        videoView.seekTo(0);
        videoSeekBar.setProgress(0);
        videoPauseImg.setImageResource(R.drawable.play_play);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError");
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (!videoView.isPlaying()){
                    return false;
                }
                float downX =  event.getRawX();
                touchLastX = downX;
                Log.d("FilmDetailActivity", "downX" + downX);
                this.position = videoView.getCurrentPosition();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!videoView.isPlaying()){
                    return false;
                }
                float currentX =  event.getRawX();
                float deltaX = currentX - touchLastX;
                float deltaXAbs  =  Math.abs(deltaX);
                if (deltaXAbs>1){
                    if (touchStatusView.getVisibility()!= View.VISIBLE){
                        touchStatusView.setVisibility(View.VISIBLE);
                    }
                    touchLastX = currentX;
                    Log.d("FilmDetailActivity","deltaX"+deltaX);
                    if (deltaX > 1) {
                        position += touchStep;
                        if (position > duration) {
                            position = duration;
                        }
                        touchPosition = position;
                        touchStatusImg.setImageResource(R.mipmap.ic_fast_forward_white_24dp);
                        int[] time = getMinuteAndSecond(position);
                        touchStatusTime.setText(String.format("%02d:%02d/%s", time[0], time[1],formatTotalTime));
                    } else if (deltaX < -1) {
                        position -= touchStep;
                        if (position < 0) {
                            position = 0;
                        }
                        touchPosition = position;
                        touchStatusImg.setImageResource(R.mipmap.ic_fast_rewind_white_24dp);
                        int[] time = getMinuteAndSecond(position);
                        touchStatusTime.setText(String.format("%02d:%02d/%s", time[0], time[1],formatTotalTime));
                        //mVideoView.seekTo(position);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (touchPosition!=-1){
                    videoView.seekTo(touchPosition);
                    touchStatusView.setVisibility(View.GONE);
                    touchPosition = -1;
                    if (videoControllerShow){
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    private int[] getMinuteAndSecond(int mils) {
        mils /= 1000;
        int[] time = new int[2];
        time[0] = mils / 60;
        time[1] = mils % 60;
        return time;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.videoPauseBtn:
                if (videoView.isPlaying()) {
                    videoView.pause();
                    videoPauseImg.setImageResource(R.drawable.play_play);

                } else {
                    videoView.start();
                    videoPauseImg.setImageResource(R.drawable.play_stop);

                }
                break;
            case R.id.viewBox:
                if (rltitile.getVisibility() != View.VISIBLE) {
                    showOverlay();
                } else {
                    hideOverlay();
                }
                break;
            case R.id.screen_status_btn:
                int  i = getResources().getConfiguration().orientation;
                if (i== Configuration.ORIENTATION_PORTRAIT){
                    ((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }else if (i== Configuration.ORIENTATION_LANDSCAPE){
                    ((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;
            case R.id.videoShootBtn:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                String fname = "/sdcard/RkCamera/RkVideo/" + sdf.format(new Date()) + ".png";
                if(tvTitle.getText().toString().startsWith("file")){
                    MediaMetadataRetriever rev = new MediaMetadataRetriever();
                    String path1 =  Environment.getExternalStorageDirectory()+"/RkCamera/RkVideo/";
                    String[] temp1 = tvTitle.getText().toString().split("/RkVideo/");
                    String filename1 = temp1[1];
                    Log.d(TAG, "path: " + path1 + filename1);
                    Uri uri = Uri.parse(path1+filename1);
                    rev.setDataSource(getContext(),uri); //这里第一个参数需要Context，传this指针
                    Bitmap bitmap = rev.getFrameAtTime(videoView.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if(null != bitmap){
                        Toast.makeText(getContext(), getContext().getString(R.string.screen_shot_success_msg), Toast.LENGTH_SHORT).show();
                        try{
                            FileOutputStream out = new FileOutputStream(fname);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else if(tvTitle.getText().toString().startsWith("http")) {
                    Toast.makeText(getContext(), getContext().getString(R.string.screen_shot_msg), Toast.LENGTH_SHORT).show();
                    new Thread(){
                        @Override
                        public void run(){
                            Bitmap bitmap1 = getCurrentVideoBitmap(tvTitle.getText().toString(), videoView);
                            Message msg = new Message();
                            msg.what = IMAGE_SHOOT_FINISH;
                            msg.obj = bitmap1;
                            videoHandler.sendMessage(msg);
                        }
                    }.start();
                }
                break;
        }
    }

    private void showOverlay() {
        rltitile.setVisibility(View.VISIBLE);
        videoControllerLayout.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        rltitile.setVisibility(View.GONE);
        videoControllerLayout.setVisibility(View.GONE);
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        this.animation = false;
        this.videoControllerShow = !this.videoControllerShow;
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int[] time = getMinuteAndSecond(progress);
        videoCurTimeText.setText(String.format("%02d:%02d", time[0], time[1]));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        videoView.pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        videoView.seekTo(videoSeekBar.getProgress());
        videoView.start();
        videoPauseImg.setImageResource(R.drawable.play_stop);
    }

    public ImageView getPlayBack(){
        return playback;
    }

    public ImageView getVideoNextImg(){
        return videoNextImg;
    }

    public ImageView getVideoLastImg(){
        return videoLastImg;
    }

    public ImageView getVideoDelImg(){
        return videoDelImg;
      }

    public void stopplay(){
        videoView.stopPlayback();
    }

    public void  requestfocus(){
        videoView.requestFocus();
    }

    public TextView getTvTitle(){
        return tvTitle;
    }
    private Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {// MODE_CAPTURE_FRAME_ONLY
//          retriever
//                  .setMode(android.media.MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
//          retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
            retriever.setDataSource(filePath);
//          bitmap = retriever.captureFrame();
            String timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long time = Long.parseLong(timeString) * 1000;
            Log.i("TAG","time = " + time);
            bitmap = retriever.getFrameAtTime(time*31/160); //按视频长度比例选择帧
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return bitmap;
    }

    public static Bitmap getCurrentVideoBitmap(String url,MyVideoView videoView){
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        try {
            mediaMetadataRetriever.setDataSource(url,new HashMap<String, String>());
            //取得指定时间的Bitmap，即可以实现抓图（缩略图）功能
            Log.d(TAG, "截图时间："+ videoView.getCurrentPosition()*1000);
            bitmap = mediaMetadataRetriever.getFrameAtTime(videoView.getCurrentPosition()*1000,MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                mediaMetadataRetriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        if (bitmap == null) {
            return null;
        }

        //bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
        bitmap = Bitmap.createBitmap(bitmap);
        return bitmap;
    }

//    public void setAC( aa) {
//        a = aa;
//    }
//
//    public interface click{
//        public void mm();
//    }
//    private click a;
}
