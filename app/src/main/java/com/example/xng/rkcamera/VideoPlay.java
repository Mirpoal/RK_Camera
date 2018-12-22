package com.example.xng.rkcamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xng.rkcamera.LocalVideo.VideoGridViewModel;
import com.example.xng.rkcamera.Map.MyMapView;
import com.example.xng.rkcamera.Map.gps.GpsFileDownloadThread;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.example.xng.rkcamera.Map.gps.GpsParseUtil;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoPlay extends Activity implements IVLCVout.OnNewVideoLayoutListener, View.OnTouchListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "VideoPlay";
	
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_SCREEN = 1;
    private static final int SURFACE_FILL = 2;
    private static final int SURFACE_16_9 = 3;
    private static final int SURFACE_4_3 = 4;
    private static final int SURFACE_ORIGINAL = 5;
    private static int CURRENT_SIZE = SURFACE_BEST_FIT;

    private FrameLayout mVideoSurfaceFrame = null;
    private VideoSurfaceView mVideoSurface = null;
    private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private Media mMedia = null;
    private ArrayList<String> mOptions = new ArrayList<>();

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;

    private String mUrl = null;
    private String mVideoFileName = null;
    private boolean mPlayHttpUrl = false;
    private boolean mPlayLocalUrl = false;
    private int mDownloadPosition = -1;
    private int mLocalPosition = -1;
    private List<DownLoadModel> mDownloadList = new ArrayList<DownLoadModel>();
    private List<VideoGridViewModel> mLocalList = new ArrayList<VideoGridViewModel>();

    private ImageButton mVideoLastBtn = null, mVideoPauseBtn = null, mVideoNextBtn = null,
            mPlaybackBtn = null, mScreenStatusBtn;
    private boolean mPlayAnotherUrl = false;
    private boolean mPlayUrlInit = false;

    private TextView mVideoTitle = null;
    private LinearLayout mTitle = null;
    private LinearLayout mVideoControllerLayout = null;

    //progress bar parameters
    private int mVideoLength = 0;
    private String mFormatTotalTime = null;
    private TextView mCurrentTime = null;
    private TextView mTotalTime = null;
    private SeekBar mSeekBar = null;

    //map parameters
    private MyMapView mMapView = null;
    private boolean mStartDraw = false;
    private boolean mGpsGetListEnd = false;
    private int mStardPointId = -1;
    private int mCurrentPoint = -1;
    private ArrayList<GpsInfo> mGpsInfoList = null;
    private GpsFileDownloadThread mGpsFileDownloadThread = null;

    private SocketService mSocketService = SocketService.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player);

        if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
            initMapView(savedInstanceState);

        initPlaylist();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SocketService.ACTION_GPS_FILE_LIST);
        registerReceiver(mReceiver, filter);

        mUrl = ConnectIP.mVideoUrl;
        Log.d(TAG, "mUrl: " + mUrl);
        if (TextUtils.isEmpty(mUrl)) {
            Toast.makeText(this, R.string.uri_invalid_msg, Toast.LENGTH_SHORT).show();
        }

        if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
            startGetGpsList(mUrl);

        setVlcOptions();
        mLibVLC = new LibVLC(this, mOptions);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(mMediaPlayerListener);

        setView();
    }

    @Override
    protected void onStart() {
        super.onStart();

        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);
        vlcVout.attachViews(this);

        mPlayUrlInit = true;
        mMedia = new Media(mLibVLC, Uri.parse(mUrl));
        mMediaPlayer.setMedia(mMedia);
        mMediaPlayer.play();

        if (mOnLayoutChangeListener == null) {
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                private final Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        //Log.d(TAG, "------mOnLayoutChangeListener");
                        updateVideoSurfaces();
                    }
                };
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    //Log.d(TAG, "onLayoutChange");
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.post(mRunnable);
                    }
                }
            };
        }
        mVideoSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMapView != null)
            mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMapView != null)
            mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mMapView != null)
            mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        unregisterReceiver(mReceiver);

        if (mMedia != null)
            mMedia.release();

        if (mMediaPlayer != null)
            mMediaPlayer.release();

        if (mLibVLC != null)
            mLibVLC.release();

        if (mMapView != null)
            mMapView.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
            stopGetGpsList();

        if (mOnLayoutChangeListener != null) {
            mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
            mOnLayoutChangeListener = null;
        }

        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
        updateVideoSurfaces();
        //mapUiControl(true);
    }

    private void initPlaylist() {
        mDownloadPosition = getIntent().getIntExtra("position", -1);
        mLocalPosition = getIntent().getIntExtra("localposition", -1);

        mDownloadList = (List<DownLoadModel>)getIntent().getSerializableExtra("videourl");
        mLocalList = (List<VideoGridViewModel>)getIntent().getSerializableExtra("localvideourl");

        if (mDownloadPosition != -1)
            mPlayHttpUrl = true;

        if (mLocalPosition != -1)
            mPlayLocalUrl = true;
    }

    private final MediaPlayer.EventListener mMediaPlayerListener = new MediaPlayer.EventListener() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.TimeChanged:
                    //Log.d("tiantian", "MediaPlayer.Event.TimeChanged");
                    //Log.d("tiantian", "tiantian mMediaPlayer.getTime(): " + mMediaPlayer.getTime());
                    //Log.d("tiantian", "mMediaPlayer.getLength(): " + mMediaPlayer.getLength());

                    if (mStartDraw)
                        setCurrentMovePoint((int)mMediaPlayer.getTime()/1000 + mStardPointId);

                    mSeekBar.setProgress((int) mMediaPlayer.getTime());
                    break;

                case MediaPlayer.Event.MediaChanged:
                    //Log.d(TAG, "MediaPlayer.Event.MediaChanged");
                    break;

                case MediaPlayer.Event.Opening:
                    //Log.d(TAG, "MediaPlayer.Event.Opening");
                    break;

                case MediaPlayer.Event.PositionChanged:
                    //Log.d(TAG, "MediaPlayer.Event.PositionChanged");
                    //Log.d(TAG, "mMediaPlayer.getPosition(): " + (int)(mMediaPlayer.getPosition() * mMediaPlayer.getLength()));
                    break;

                case MediaPlayer.Event.Playing:
                    //Log.d("tiantian", "MediaPlayer.Event.Playing");
                    //Log.d(TAG, "mMediaPlayer.getLength(): " + mMediaPlayer.getLength());
                    //mVideoSurface.setBackground(new ColorDrawable(0x00000000));
                    mSeekBar.setEnabled(true);
                    mVideoPauseBtn.setBackgroundResource(R.drawable.play_stop);

                    if (mPlayUrlInit) {
                        initSeekBar();
                        mPlayUrlInit = false;
                    }
                    break;

                case MediaPlayer.Event.Paused:
                    //Log.d(TAG, "MediaPlayer.Event.Paused");
                    mVideoPauseBtn.setBackgroundResource(R.drawable.play_play);
                    break;

                case MediaPlayer.Event.Stopped:
                    //Log.d(TAG, "MediaPlayer.Event.Stopped");
                    //Log.d(TAG, "stop, getTime(): " + mMediaPlayer.getTime());
                    mVideoPauseBtn.setBackgroundResource(R.drawable.play_play);

                    mSeekBar.setEnabled(false);
                    if (mPlayAnotherUrl) {
                        mPlayAnotherUrl = false;
                    } else {
                        mSeekBar.setProgress(mVideoLength);
                        playNextVideo();
                    }

                    //mVideoSurface.setBackgroundResource(R.drawable.img_nopic_01);
                    //mVideoSurface.setBackground(new ColorDrawable(0xff000000));
                    break;

                case MediaPlayer.Event.Buffering:
                    //Log.d(TAG, "MediaPlayer.Event.Buffering");
                    break;

                case MediaPlayer.Event.EncounteredError:
                    //Log.d(TAG, "EncounteredError error");
                    mPlayAnotherUrl = true;
                    Toast.makeText(VideoPlay.this, R.string.video_can_not_play, Toast.LENGTH_SHORT).show();
                    finish();
                    /*
                    new AlertDialog.Builder(VideoPlay.this)
                            .setMessage(getString(R.string.video_can_not_play))
                            .setCancelable(false)
                            .setNegativeButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    VideoPlay.this.finish();
                                }
                            }).show();
                    */
                    break;
            }
        }
    };


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //Log.d(TAG, "onTouch");
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        //Log.d(TAG, "onProgressChanged, progress: " + progress);
        //Log.d(TAG, "mMediaPlayer.getTime(): " + mMediaPlayer.getTime());
        int[] time = getMinuteAndSecond(progress);
        //Log.d(TAG, "time[0]: " + time[0] + ", time[1]: " + time[1]);
        mCurrentTime.setText(String.format("%02d:%02d", time[0], time[1]));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //Log.d(TAG, "onStartTrackingTouch");
        mMediaPlayer.pause();
        mVideoPauseBtn.setBackgroundResource(R.drawable.play_play);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //Log.d(TAG, "onStopTrackingTouch");
        //Log.d(TAG, "mSeekBar.getProgress(): " + mSeekBar.getProgress());
        mMediaPlayer.setTime(mSeekBar.getProgress());
        //Log.d(TAG, "getTime(): " + mMediaPlayer.getTime());
        mMediaPlayer.play();
        mVideoPauseBtn.setBackgroundResource(R.drawable.play_stop);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.playbackBtn:
                finish();
                break;

            case R.id.videoPauseBtn:
                Log.d(TAG, "videoPauseBtn");
                //if (mSeekBar.getProgress() == mVideoLength) {
                //    Log.d(TAG, "videoPauseBtn :" + mVideoLength);
                //    playUrl(mUrl);
                //    break;
                //}

                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.pause();
                else
                    mMediaPlayer.play();

                break;

            case R.id.videoLastBtn:
                if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
                    stopGetGpsList();

                playLastVideo();
                break;

            case R.id.videoNextBtn:
                if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
                    stopGetGpsList();

                playNextVideo();
                break;

            case R.id.viewBox:
                if (mTitle.getVisibility() != View.VISIBLE) {
                    showOverlay();
                } else {
                    hideOverlay();
                }
                break;

            case R.id.screen_status:
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT){
                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);	//沉浸式模式的实现
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (orientation == Configuration.ORIENTATION_LANDSCAPE){
                    //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);	//取消沉浸式模式
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mScreenStatusBtn.setBackgroundResource(R.drawable.exit_full_screen);
        } else {
            mScreenStatusBtn.setBackgroundResource(R.drawable.enter_full_screen);
        }

        mVideoSurface.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mVideoSurface.requestLayout();
    }

    private void setVlcOptions() {
        mOptions.add("-vvvv");
        mOptions.add("--network-caching=180"); //网络缓存
    }

    private void setView() {
        mVideoSurfaceFrame = (FrameLayout) findViewById(R.id.viewBox);
        mVideoSurface = (VideoSurfaceView) findViewById(R.id.videoView);
        mVideoSurface.setContext(this);

        mVideoPauseBtn = (ImageButton) findViewById(R.id.videoPauseBtn);
        mVideoLastBtn = (ImageButton) findViewById(R.id.videoLastBtn);
        mScreenStatusBtn = (ImageButton) findViewById(R.id.screen_status);
        mVideoNextBtn = (ImageButton) findViewById(R.id.videoNextBtn);
        mPlaybackBtn = (ImageButton) findViewById(R.id.playbackBtn);
        mTitle = (LinearLayout)findViewById(R.id.rltitle);
        mVideoControllerLayout = (LinearLayout) findViewById(R.id.videoControllerLayout);
        mVideoTitle = (TextView)findViewById(R.id.tvtitle);
        mCurrentTime = (TextView)findViewById(R.id.videoCurTime);
        mTotalTime = (TextView)findViewById(R.id.videoTotalTime);
        mSeekBar = (SeekBar) findViewById(R.id.videoSeekBar);

        mVideoSurfaceFrame.setOnTouchListener(this);
        mVideoSurfaceFrame.setOnClickListener(this);
        mVideoPauseBtn.setOnClickListener(this);
        mVideoLastBtn.setOnClickListener(this);
        mVideoNextBtn.setOnClickListener(this);
        mPlaybackBtn.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mScreenStatusBtn.setOnClickListener(this);
        mSeekBar.setProgress(0);

        mVideoTitle.setText(mUrl);

        pathIsExist();
    }

    private void pathIsExist() {
        File file = new File(BitmapUtils.getSDPath() + "/RkCamera/RkPhoto/");
        if (!file.exists())
            file.mkdirs();

        File file1 = new File(BitmapUtils.getSDPath()+"/RkCamera/RkVideo/") ;
        if(!file1.exists())
            file1.mkdirs();
    }

    private void showOverlay() {
        mTitle.setVisibility(View.VISIBLE);
        mVideoControllerLayout.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        mTitle.setVisibility(View.GONE);
        mVideoControllerLayout.setVisibility(View.GONE);
    }

    private void initSeekBar() {
        //Log.d(TAG, "initSeekBar");
        mVideoLength = (int)mMediaPlayer.getLength();
        //Log.d(TAG, "mVideoLength: " + mVideoLength);
        mSeekBar.setMax(mVideoLength);
        int[] time = getMinuteAndSecond(mVideoLength);
        mFormatTotalTime = String.format("%02d:%02d", time[0], time[1]);
        mTotalTime.setText(mFormatTotalTime);
    }

    private int[] getMinuteAndSecond(int mils) {
        mils /= 1000;
        int[] time = new int[2];
        time[0] = mils / 60;
        time[1] = mils % 60;
        return time;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void playUrl(String url){
        Log.d(TAG, "playUrl: " + url);

        if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
            startGetGpsList(url);

        mSeekBar.setProgress(0);

        if (mMediaPlayer.isPlaying()) {
            //Log.d(TAG, "mMediaPlayer.isPlaying()");
            mPlayAnotherUrl = true;
            mMediaPlayer.stop();
        }

        if (mMedia != null) {
            mMedia.release();
            mMedia = null;
        }

        mVideoTitle.setText(url);
        mPlayUrlInit = true;
        mMedia = new Media(mLibVLC, Uri.parse(url));
        mMediaPlayer.setMedia(mMedia);
        mMediaPlayer.play();
    }

    private void playNextVideo() {
        //Log.d(TAG, "play next video");
        if(mPlayHttpUrl) {
            if(mDownloadPosition < (mDownloadList.size() - 1)){
                mDownloadPosition++;
            }else {
                //Toast.makeText(VideoPlay.this, getString(R.string.last_video_msg), Toast.LENGTH_SHORT).show();
                mDownloadPosition = 0;
            }
            mUrl = mDownloadList.get(mDownloadPosition).getFileUrl();
            playUrl(mUrl);
        } else if (mPlayLocalUrl) {
            if(mLocalPosition < (mLocalList.size() - 1)){
                mLocalPosition++;
            }else {
                mLocalPosition = 0;
                //Toast.makeText(VideoPlay.this, getString(R.string.last_video_msg), Toast.LENGTH_SHORT).show();
            }
            mUrl = "file://" + mLocalList.get(mLocalPosition).getPath();
            playUrl(mUrl);
        }
    }

    private void playLastVideo() {
        //Log.d(TAG, "play last video");
        if(mPlayHttpUrl) {
            if(mDownloadPosition > 0){
                mDownloadPosition--;
            }else {
                //Toast.makeText(VideoPlay.this, getString(R.string.first_video_msg), Toast.LENGTH_SHORT).show();
                mDownloadPosition = mDownloadList.size() - 1;
            }
            mUrl = mDownloadList.get(mDownloadPosition).getFileUrl();
            playUrl(mUrl);
        } else if(mPlayLocalUrl) {
            if(mLocalPosition > 0){
                mLocalPosition--;
            } else {
                //Toast.makeText(VideoPlay.this, getString(R.string.first_video_msg), Toast.LENGTH_SHORT).show();
                mLocalPosition = mLocalList.size() - 1;
            }
            mUrl = "file://" + mLocalList.get(mLocalPosition).getPath();
            playUrl(mUrl);
        }
    }

    private void changeMediaPlayerLayout(int displayW, int displayH) {
        /* Change the video placement using the MediaPlayer API */
        switch (CURRENT_SIZE) {
            case SURFACE_BEST_FIT:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_FIT_SCREEN:
            case SURFACE_FILL: {
                Media.VideoTrack vtrack = mMediaPlayer.getCurrentVideoTrack();
                if (vtrack == null)
                    return;
                final boolean videoSwapped = vtrack.orientation == Media.VideoTrack.Orientation.LeftBottom
                        || vtrack.orientation == Media.VideoTrack.Orientation.RightTop;
                if (CURRENT_SIZE == SURFACE_FIT_SCREEN) {
                    int videoW = vtrack.width;
                    int videoH = vtrack.height;

                    if (videoSwapped) {
                        int swap = videoW;
                        videoW = videoH;
                        videoH = swap;
                    }
                    if (vtrack.sarNum != vtrack.sarDen)
                        videoW = videoW * vtrack.sarNum / vtrack.sarDen;

                    float ar = videoW / (float) videoH;
                    float dar = displayW / (float) displayH;

                    float scale;
                    if (dar >= ar)
                        scale = displayW / (float) videoW; /* horizontal */
                    else
                        scale = displayH / (float) videoH; /* vertical */
                    mMediaPlayer.setScale(scale);
                    mMediaPlayer.setAspectRatio(null);
                } else {
                    mMediaPlayer.setScale(0);
                    mMediaPlayer.setAspectRatio(!videoSwapped ? ""+displayW+":"+displayH
                            : ""+displayH+":"+displayW);
                }
                break;
            }
            case SURFACE_16_9:
                mMediaPlayer.setAspectRatio("16:9");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_4_3:
                mMediaPlayer.setAspectRatio("4:3");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_ORIGINAL:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(1);
                break;
        }
    }

    private void updateVideoSurfaces() {
        //获取整个屏幕高宽，包含状态栏（显示电量、运营商等），标题栏 (1920, 1080)
        //int sw = getWindow().getDecorView().getWidth();
        //int sh = getWindow().getDecorView().getHeight();

        //获取应用区域高宽，包含标题栏 (1776, 1080)
        //Display display = getWindowManager().getDefaultDisplay();
        //int sw  = display.getWidth();
        //int sh  = display.getHeight();

        //获取view绘制区域高宽，不包含标题栏 (1704, 1080)
        Rect outRect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect);
        int sw = outRect.width();
        int sh = outRect.height();

        //Log.d("tiantian", "updateVideoSurfaces");
        //Log.d("tiantian", "sw: " + sw + ", sh: " + sh);

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        //mMediaPlayer.getVLCVout().setWindowSize(sw, sh);
        int orientation = getResources().getConfiguration().orientation;
        if (ConnectIP.mProductType.equals(ConnectIP.mCvr) && orientation == Configuration.ORIENTATION_PORTRAIT && sw < sh) {
            mMediaPlayer.getVLCVout().setWindowSize(sw, sh/2); //tiantian, 开启竖屏surface显示在屏幕上端
            mapUiControl(true);
        } else /*if (orientation == Configuration.ORIENTATION_LANDSCAPE)*/ {
            mMediaPlayer.getVLCVout().setWindowSize(sw, sh);
            mapUiControl(false);
        }

        //Log.d("tiantian", "mVideoWidth: " + mVideoWidth + ", mVideoHeight: " + mVideoHeight);
        ViewGroup.LayoutParams lp = mVideoSurface.getLayoutParams();
        ViewGroup.LayoutParams lp1 = mVideoSurfaceFrame.getLayoutParams();
        if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width  = ViewGroup.LayoutParams.MATCH_PARENT;
            //lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (ConnectIP.mProductType.equals(ConnectIP.mCvr) && orientation == Configuration.ORIENTATION_PORTRAIT && sw < sh) {
                lp.height = lp1.height = sh/2;
            } else /*if (orientation == Configuration.ORIENTATION_LANDSCAPE)*/ {
                lp.height = lp1.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            mVideoSurface.setLayoutParams(lp);
            mVideoSurfaceFrame.setLayoutParams(lp1);

            changeMediaPlayerLayout(sw, sh);

            //mVideoSurface.requestLayout();
            return;
        }

        if (lp.width == lp.height && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            mMediaPlayer.setAspectRatio(null);
            mMediaPlayer.setScale(0);
        }

        double dw = sw, dh = sh;
        final boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mVideoSarNum / mVideoSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        switch (CURRENT_SIZE) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_SCREEN:
                if (dar >= ar)
                    dh = dw / ar; /* horizontal */
                else
                    dw = dh * ar; /* vertical */
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        // set display size
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        mVideoSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = mVideoSurfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        mVideoSurfaceFrame.setLayoutParams(lp);

        mVideoSurface.invalidate();
    }

    private void screenShot() {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String name = df.format(new Date());
            name = BitmapUtils.getSDPath() + "/RkCamera/RkVideo/" + name + ".png";
            File file = new File(name);
            if(!file.exists())
                file.createNewFile();

            if(mMediaPlayer.snapShot(name,1920,1080))
                Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.saved_msg), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.screen_shot_failed_msg), Toast.LENGTH_SHORT).show();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    //地图相关接口
    private void initMapView(Bundle savedInstanceState) {
        mMapView = (MyMapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.setActivity(this);
        mMapView.setVisibility(View.VISIBLE);
        mMapView.init();
    }

    private void mapUiControl(boolean enable) {
        //Log.d("tiantian", "mapUiControl: " + enable);

        if (mMapView != null) {
            mMapView.uiControl(enable);
            //mMapView.setMyLocation(enable); //触发本地定位
        }
    }

    private void setCurrentMovePoint(int currentPoint) {
        if (currentPoint >= mGpsInfoList.size() || currentPoint < 0)
            return;

        if (mCurrentPoint != currentPoint) {
            //Log.d(TAG, "setCurrentMovePoint, currentPoint: " + currentPoint);
            mCurrentPoint = currentPoint;
            if (mGpsInfoList.get(currentPoint).getStatus().equals(GpsParseUtil.VALID_DATA)) {
                mMapView.setMoveLine(mGpsInfoList.get(currentPoint).getMapPointId());
            } else {
                Log.d(TAG, "setCurrentMovePoint, currentPoint: " + currentPoint + ", data incalid");
            }
        }
    }

    private String findMapGpsFile(String videoFileName, ArrayList<String> gpsFileList) {
        String mapGpsFile = null;
        String curTime[] = videoFileName.split("_"); //lg: 20160223_210726_A.mp4
        long cTime = Long.parseLong(curTime[0] + curTime[1]);
        //Log.d(TAG, "videoFileName: " + videoFileName);

        File storagePath = new File(GpsFileDownloadThread.mLocalStoragePath);
        File files[] = storagePath.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (file.isDirectory()) {
                        String fileName[] = file.getName().split("-"); //20160223_210726_1-20160223_210826_1
                        String startTime[] = fileName[0].split("_");
                        String endTime[] = fileName[1].split("_");
                        long sTime = Long.parseLong(startTime[0] + startTime[1]);
                        long eTime = Long.parseLong(endTime[0] + endTime[1]);

                        if (cTime >= sTime && cTime <= eTime) {
                            File fs[] = file.listFiles();
                            if (fs != null) {
                                String preFile = null, curFile = null;
                                for (File f : fs) {
                                    if (!f.getName().equals(GpsInfo.COUNT_FILE_NAME)) { //过滤掉计数文件
                                        if (mapGpsFile == null) {
                                            curFile = f.getName(); //lg: 20160223_210726_1.txt
                                            String cur[] = curFile.split("_");
                                            long curT = Long.parseLong(cur[0] + cur[1]);

                                            if (curT == cTime) {
                                                mapGpsFile = curFile;
                                            } else {
                                                if (preFile != null) {
                                                    String pre[] = preFile.split("_");
                                                    long preT = Long.parseLong(pre[0] + pre[1]);

                                                    if (preT < cTime && curT > cTime)
                                                        mapGpsFile = curFile;
                                                }
                                            }
                                            preFile = curFile;
                                        }

                                        gpsFileList.add(f.getName());
                                        //Log.d(TAG, "f.getName(): " + f.getName());
                                    }
                                }
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return mapGpsFile;
    }

    private void localPlayGpsFileDownload(String videoFileName) {
        ArrayList<String> gpsFileList = new ArrayList<String>();

        String mapGpsFile = findMapGpsFile(videoFileName, gpsFileList);

        Log.d(TAG, "mapGpsFile: " + mapGpsFile);
        Log.d(TAG, "gpsFileList.size(): " + gpsFileList.size());
        if (mapGpsFile != null && gpsFileList.size() >0) {
            mGpsFileDownloadThread = new GpsFileDownloadThread(mHandler, videoFileName,
                    mapGpsFile, gpsFileList, mMapView, true, false, GpsFileDownloadThread.mLocalStoragePath);
            mGpsFileDownloadThread.start();
        } else {
            Toast.makeText(VideoPlay.this, getString(R.string.not_gps), Toast.LENGTH_SHORT).show();
        }
    }

    private void startGetGpsList(String url) {
        mMapView.clear();

        mGpsGetListEnd = false;
        mStartDraw = false;
        mStardPointId = -1;
        mGpsInfoList = null;
        mGpsFileDownloadThread = null;

        mVideoFileName = mUrl.substring(mUrl.lastIndexOf("/") + 1, mUrl.length());
        if (url.startsWith("file://")) { //本地播放
            localPlayGpsFileDownload(mVideoFileName);
        } else {
            mSocketService.clearGpsFileList();
            mSocketService.setGpsOwner("VideoPlay");
            mSocketService.sendMsg(GpsInfo.CMD_GET_GPS_LIST + mVideoFileName, false);
        }
    }

    private void stopGetGpsList() {
        //Log.d("GpsFileDownloadThread", "stopGetGpsList");
        if (!mGpsGetListEnd)
            mSocketService.sendMsg(GpsInfo.CMD_STOP_GET_GPS_LIST, false);

        if (mGpsFileDownloadThread != null) {
            mGpsFileDownloadThread.stopDownload();
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (SocketService.ACTION_GPS_FILE_LIST.equals(intent.getAction())
                    && mSocketService.getGpsOwner().equals("VideoPlay")) {
                mGpsGetListEnd = true;

                String mapGpsFile = intent.getStringExtra("mapGpsFile");
                ArrayList<String> gpsFileList = (ArrayList<String>) intent.getSerializableExtra("gpsFileList");

                if (!mapGpsFile.equals("NULL") && gpsFileList.size() > 0) {
                    mGpsFileDownloadThread = new GpsFileDownloadThread(mHandler, mVideoFileName,
                            mapGpsFile, gpsFileList, mMapView, true, true, GpsFileDownloadThread.mOnlineStoragePath);
                    mGpsFileDownloadThread.start();
                } else {
                    if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
                        Toast.makeText(VideoPlay.this, getString(R.string.not_gps), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GpsInfo.MSG_GPS_START_POINT:
                    mStardPointId = (int) msg.obj;
                    Log.d(TAG, "mStardPointId: " + mStardPointId);
                    break;

                case GpsInfo.MSG_GPS_INFO_LIST:
                    mGpsInfoList = (ArrayList<GpsInfo>) msg.obj;
                    Log.d(TAG, "mGpsInfoList.size(): " + mGpsInfoList.size());
                    mStartDraw = true;
                    break;

                case GpsInfo.MSG_GPS_FILE_DOWNLOAD_FINISH:
                    Log.d(TAG, "MSG_GPS_FILE_DOWNLOAD_FINISH");
                    mGpsFileDownloadThread = null;
                    break;
            }
        }
    };
}