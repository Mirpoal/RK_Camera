package com.example.xng.rkcamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.xng.rkcamera.Map.MyMapView;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.example.xng.rkcamera.Map.gps.GpsParseUtil;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VideoPlayerActivity extends Activity implements IVLCVout.OnNewVideoLayoutListener, View.OnClickListener {
	public static final String CMD_RECORD_IDLE = "CMD_ACK_GET_Control_Recording_IDLE";
	public static final String CMD_RECORD_BUSY = "CMD_ACK_GET_Control_Recording_BUSY";
	public static final String CMD_CB_STARTREC = "CMD_CB_StartRec";
	public static final String CMD_CB_NO_SDCARD = "CMD_CB_NoSDCard";
	public static final String CMD_CB_STOPREC = "CMD_CB_StopRec";
	public static final String CMD_CB_GET_MODE = "CMD_CB_GET_MODE";
	public static final String CMD_CB_GPS_UPDATA = "CMD_CB_GPS_UPDATA";

	private static final String TAG = "VideoPlayerActivity";
	private static final int SURFACE_BEST_FIT = 0;
	private static final int SURFACE_FIT_SCREEN = 1;
	private static final int SURFACE_FILL = 2;
	private static final int SURFACE_16_9 = 3;
	private static final int SURFACE_4_3 = 4;
	private static final int SURFACE_ORIGINAL = 5;
	private static int CURRENT_SIZE = SURFACE_BEST_FIT;
	//private static int CURRENT_SIZE = SURFACE_FIT_SCREEN;

	private static final int MSG_RECORD_FINISH = 0;

	private FrameLayout mVideoSurfaceFrame = null, mModeChangeFrame;
	private VideoSurfaceView mVideoSurface = null;
	private LinearLayout mVideoControlLayout = null, mBurstBtnLayout = null, mTimeLapseBtnLayout = null;
	private LinearLayout mTitle = null;

	private ImageButton mScreenShotBtn, mPlayControlBtn, mPhotographBtn, mPlayBackBtn,
			mScreenStatusBtn, mModeChangeBtn, mVideoModeBtn, mPhotoModeBtn, mChangeCameraBtn;

	//录像模式，缩时录影按键
	private ImageButton mTimeLapseOffBtn = null, mTimeLapse1Btn = null, mTimeLapse5Btn = null,
			mTimeLapse10Btn = null, mTimeLapse30Btn = null, mTimeLapse60Btn = null;

	//拍照模式，连拍按键
	private ImageButton mBurstOffBtn = null, mBurst3Btn = null, mBurst4Btn = null, mBurst5Btn = null;

	//缩时录影或连拍类型
	private String mType = null;
	private String mModeType = null;

	private static final int RECORD_STATUS_WAIT = 0;
	private static final int RECORD_STATUS_START = 1;
	private static final int RECORD_STATUS_STOP = 2;
	private int mRecordStatus = RECORD_STATUS_STOP;

	private final Handler mHandler = new Handler();
	private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

	private String mUrl = null;
	private LibVLC mLibVLC = null;
	private MediaPlayer mMediaPlayer = null;
	private Media mMedia = null;
	private ArrayList<String> mOptions = new ArrayList<>();

	private int mVideoHeight = 0, mVideoWidth = 0;
	private int mVideoVisibleHeight = 0, mVideoVisibleWidth = 0;
	private int mVideoSarNum = 0, mVideoSarDen = 0;

	private SocketService mSocketService = SocketService.getInstance();

	//map parameters
	private MyMapView mMapView = null;

	//test
	private ArrayList<GpsInfo> mGpsInfoList = new ArrayList<GpsInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_player_activity);

		if (ConnectIP.mProductType.equals(ConnectIP.mCvr))
			initMapView(savedInstanceState);

		IntentFilter filter = new IntentFilter();
		filter.addAction(SocketService.ACTION_VIDEO_PLAYER_ACTIVITY);
		registerReceiver(mReceiver, filter);
		mSocketService.sendMsg("CMD_GET_MODE", false);
		//mSocketService.sendMsg("CMD_GET_Control_Recording", false);

		mUrl = getIntent().getStringExtra("url");
		Log.d(TAG, "mUrl: " + mUrl);
		if (TextUtils.isEmpty(mUrl)) {
			Toast.makeText(this, R.string.uri_invalid_msg, Toast.LENGTH_SHORT).show();
		}

		setVlcOptions();
		mLibVLC = new LibVLC(this, mOptions);
		mMediaPlayer = new MediaPlayer(mLibVLC);
		mMediaPlayer.setEventListener(mMediaPlayerListener);

		setupView();

		//test
		//getGpsDateFromFileName(GpsFileDownloadThread.mLocalStoragePath
		//		+ "/20160223_210726_1-20160223_215744_1/20160223_215644_1.txt");
	}

	@Override
	protected void onStart() {
		super.onStart();

		final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
		vlcVout.setVideoView(mVideoSurface);
		vlcVout.attachViews(this);

		mMedia = new Media(mLibVLC, Uri.parse(mUrl));
		mMediaPlayer.setMedia(mMedia);
		mMediaPlayer.play();

		if (mOnLayoutChangeListener == null) {
			mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
				private final Runnable mRunnable = new Runnable() {
					@Override
					public void run() {
						//Log.d("tiantian", "mRunnable");
						updateVideoSurfaces();
					}
				};
				@Override
				public void onLayoutChange(View v, int left, int top, int right,
										   int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
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
		super.onStop();

		if (mOnLayoutChangeListener != null) {
			mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
			mOnLayoutChangeListener = null;
		}

		mMediaPlayer.stop();
		mMediaPlayer.getVLCVout().detachViews();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
		//Log.d(TAG, "onNewVideoLayout");
		mVideoWidth = width;
		mVideoHeight = height;
		mVideoVisibleWidth = visibleWidth;
		mVideoVisibleHeight = visibleHeight;
		mVideoSarNum = sarNum;
		mVideoSarDen = sarDen;
		updateVideoSurfaces();
		mapUiControl(true);
	}

	private void initMapView(Bundle savedInstanceState) {
		mMapView = (MyMapView) findViewById(R.id.map);
		mMapView.onCreate(savedInstanceState);

		mMapView.setActivity(this);
		mMapView.setVisibility(View.VISIBLE);
		mMapView.init();
	}

	private void mapUiControl(boolean enable) {
		//Log.d(TAG, "mapUiControl: " + enable);

		if (mMapView != null) {
			mMapView.uiControl(enable);
			//mMapView.setMyLocation(enable); //触发本地定位
		}
	}

	private final MediaPlayer.EventListener mMediaPlayerListener = new MediaPlayer.EventListener() {
		@Override
		public void onEvent(MediaPlayer.Event event) {
			switch (event.type) {
				case MediaPlayer.Event.Playing:
					Log.d(TAG, "MediaPlayer.Event.Playing");
					break;

				case MediaPlayer.Event.Paused:
					Log.d(TAG, "MediaPlayer.Event.Paused");
					break;

				case MediaPlayer.Event.Stopped:
					Log.d(TAG, "MediaPlayer.Event.Stopped");
					break;

				case MediaPlayer.Event.Buffering:
					Log.d(TAG, "MediaPlayer.Event.Buffering");
					break;
			}
		}
	};

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			/*
			case R.id.ib_sreenshot:
				Log.d(TAG, "ib_sreenshot");
				screenShot();
				break;
			*/
			case R.id.ib_photograph:
				mSocketService.sendMsg("CMD_Control_Photograph", false);
				//Toast.makeText(this, this.getString(R.string.successed_camera_msg), Toast.LENGTH_SHORT).show();
				break;

			case R.id.ib_play_control:
				if (mRecordStatus == RECORD_STATUS_START) {
					mSocketService.sendMsg("CMD_Control_Recording:off", false);
					Log.d(TAG, "send CMD_Control_Recording:off");
					mRecordStatus = RECORD_STATUS_WAIT;
				} else if (mRecordStatus == RECORD_STATUS_STOP) {
					mSocketService.sendMsg("CMD_Control_Recording:on", false);
					Log.d(TAG, "send CMD_Control_Recording:on");
					mRecordStatus = RECORD_STATUS_WAIT;
				} else if (mRecordStatus == RECORD_STATUS_WAIT) {
					//mPlayControlBtn.setEnabled(false);
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(getString(R.string.record_status_wait))
							.setCancelable(false);

					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							//mPlayControlBtn.setEnabled(true);
						}
					});
					builder.create().show();
				}

				break;

			case R.id.play_back:
				//screenShot();
				mSocketService.sendMsg("CMD_LIVE_STOP", false);	//tiantian
				//mSocketService.sendMsg("CMD_RTP_TS_TRANS_STOP", false);
				finish();
				break;

			case R.id.video_player_layout:
				if (mTitle.getVisibility() != View.VISIBLE) {
					showOverlay(true);
				} else {
					showOverlay(false);
				}
				break;

			case R.id.screen_status:
				//Log.d("tiantian", "onClick screen_status");
				int orientation = getResources().getConfiguration().orientation;
				if (orientation == Configuration.ORIENTATION_PORTRAIT){
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);	//沉浸式模式的实现
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else if (orientation == Configuration.ORIENTATION_LANDSCAPE){
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);	//取消沉浸式模式
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
				break;

			case R.id.mode_change_overlay:
				frameLayoutChange(true);
				break;

			case R.id.mode_change:
				frameLayoutChange(false);
				break;

			case R.id.video_mode:
				mSocketService.sendMsg("CMD_CHANGE_MODE:RECORDING", false);
				frameLayoutChange(true);
				break;

			case R.id.photo_mode:
				mSocketService.sendMsg("CMD_CHANGE_MODE:PHOTO", false);
				frameLayoutChange(true);
				break;
				
			case R.id.ib_camera_change:
				if(null != mSocketService) {
					mSocketService.sendMsg("CMD_CAMERA_CHANGE", false);
				}
				break;

			case R.id.timelapse_1:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:1", false);
				break;

			case R.id.timelapse_5:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:5", false);
				break;

			case R.id.timelapse_10:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:10", false);
				break;

			case R.id.timelapse_30:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:30", false);
				break;

			case R.id.timelapse_60:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:60", false);
				break;

			case R.id.timelapse_off:
				mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:OFF", false);
				break;

			case R.id.burst_3:
				mSocketService.sendMsg("CMD_ARGSETTINGBurst:3", false);
				break;

			case R.id.burst_4:
				mSocketService.sendMsg("CMD_ARGSETTINGBurst:4", false);
				break;

			case R.id.burst_5:
				mSocketService.sendMsg("CMD_ARGSETTINGBurst:5", false);
				break;

			case R.id.burst_off:
				mSocketService.sendMsg("CMD_ARGSETTINGBurst:OFF", false);
				break;
		}
	}

	private void frameLayoutChange(boolean flag) {
		if (flag) {
			mModeChangeFrame.setVisibility(View.GONE);
			mVideoSurfaceFrame.setVisibility(View.VISIBLE);
		} else {
			mVideoSurfaceFrame.setVisibility(View.GONE);
			mModeChangeFrame.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		//Log.d("tiantian", "onConfigurationChanged");
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
		mOptions.add("--rtsp-tcp"); //tiantian, 直播使用rtsp
		mOptions.add("--clock-synchro=1");
		mOptions.add("--clock-jitter=-2147483647");
	}

	private void setupView() {
		mModeChangeFrame = (FrameLayout) findViewById(R.id.mode_change_overlay);
		mModeChangeFrame.setOnClickListener(this);

		mVideoSurfaceFrame = (FrameLayout) findViewById(R.id.video_player_layout);
		mVideoSurfaceFrame.setOnClickListener(this);
		mVideoSurface = (VideoSurfaceView) findViewById(R.id.main_surface);
		mVideoSurface.setContext(this);

		mVideoControlLayout = (LinearLayout) findViewById(R.id.video_control_layout);
		mTimeLapseBtnLayout = (LinearLayout) findViewById(R.id.timelapse_btn_layout);
		mBurstBtnLayout = (LinearLayout) findViewById(R.id.burst_btn_layout);
		mTitle = (LinearLayout) findViewById(R.id.rl_title);

		//mScreenShotBtn = (ImageButton) findViewById(R.id.ib_sreenshot);
		//mScreenShotBtn.setOnClickListener(this);
		mPlayControlBtn = (ImageButton) findViewById(R.id.ib_play_control);
		mPlayControlBtn.setOnClickListener(this);
		mPhotographBtn = (ImageButton) findViewById(R.id.ib_photograph);
		mPhotographBtn.setOnClickListener(this);
		mPlayBackBtn = (ImageButton) findViewById(R.id.play_back);
		mPlayBackBtn.setOnClickListener(this);
		mScreenStatusBtn = (ImageButton) findViewById(R.id.screen_status);
		mScreenStatusBtn.setOnClickListener(this);

		mModeChangeBtn = (ImageButton) findViewById(R.id.mode_change);
		mModeChangeBtn.setOnClickListener(this);
		mVideoModeBtn = (ImageButton) findViewById(R.id.video_mode);
		mVideoModeBtn.setOnClickListener(this);
		mPhotoModeBtn = (ImageButton) findViewById(R.id.photo_mode);
		mPhotoModeBtn.setOnClickListener(this);

		mTimeLapse1Btn = (ImageButton) findViewById(R.id.timelapse_1);
		mTimeLapse1Btn.setOnClickListener(this);
		mTimeLapse5Btn = (ImageButton) findViewById(R.id.timelapse_5);
		mTimeLapse5Btn.setOnClickListener(this);
		mTimeLapse10Btn = (ImageButton) findViewById(R.id.timelapse_10);
		mTimeLapse10Btn.setOnClickListener(this);
		mTimeLapse30Btn = (ImageButton) findViewById(R.id.timelapse_30);
		mTimeLapse30Btn.setOnClickListener(this);
		mTimeLapse60Btn = (ImageButton) findViewById(R.id.timelapse_60);
		mTimeLapse60Btn.setOnClickListener(this);
		mTimeLapseOffBtn = (ImageButton) findViewById(R.id.timelapse_off);
		mTimeLapseOffBtn.setOnClickListener(this);

		mBurst3Btn = (ImageButton) findViewById(R.id.burst_3);
		mBurst3Btn.setOnClickListener(this);
		mBurst4Btn = (ImageButton) findViewById(R.id.burst_4);
		mBurst4Btn.setOnClickListener(this);
		mBurst5Btn = (ImageButton) findViewById(R.id.burst_5);
		mBurst5Btn.setOnClickListener(this);
		mBurstOffBtn = (ImageButton) findViewById(R.id.burst_off);
		mBurstOffBtn.setOnClickListener(this);

		mChangeCameraBtn = (ImageButton) findViewById(R.id.ib_camera_change);
		if(getResources().getBoolean(R.bool.can_change_camera)){
			mChangeCameraBtn.setVisibility(View.VISIBLE);
			mChangeCameraBtn.setOnClickListener(this);
		}

		pathIsExist();
	}

	private void showOverlay(boolean isShow) {
		if (isShow) {
			mTitle.setVisibility(View.VISIBLE);
			mVideoControlLayout.setVisibility(View.VISIBLE);
			mModeChangeBtn.setVisibility(View.VISIBLE);

			if (mModeType.equals("RECORDING") || mModeType.equals("LAPSE")) {
				mTimeLapseBtnLayout.setVisibility(View.VISIBLE);
				mBurstBtnLayout.setVisibility(View.GONE);
			} else if (mModeType.equals("PHOTO") || mModeType.equals("BURST")) {
				mTimeLapseBtnLayout.setVisibility(View.GONE);
				mBurstBtnLayout.setVisibility(View.VISIBLE);
			} else if (mModeType.equals("PREVIEW")) {
				mTimeLapseBtnLayout.setVisibility(View.GONE);
				mBurstBtnLayout.setVisibility(View.GONE);
			}
		} else {
			mTitle.setVisibility(View.GONE);
			mVideoControlLayout.setVisibility(View.GONE);
			mModeChangeBtn.setVisibility(View.GONE);
			mTimeLapseBtnLayout.setVisibility(View.GONE);
			mBurstBtnLayout.setVisibility(View.GONE);
		}
	}

	private void timeLapseUiChange() {
		if (mType.equals("1")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.pressed_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.normal_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.normal_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.normal_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.normal_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("5")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.normal_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.pressed_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.normal_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.normal_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.normal_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("10")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.normal_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.normal_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.pressed_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.normal_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.normal_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("30")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.normal_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.normal_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.normal_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.pressed_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.normal_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("60")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.normal_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.normal_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.normal_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.normal_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.pressed_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("OFF")) {
			mTimeLapse1Btn.setBackgroundResource(R.drawable.normal_1);
			mTimeLapse5Btn.setBackgroundResource(R.drawable.normal_5);
			mTimeLapse10Btn.setBackgroundResource(R.drawable.normal_10);
			mTimeLapse30Btn.setBackgroundResource(R.drawable.normal_30);
			mTimeLapse60Btn.setBackgroundResource(R.drawable.normal_60);
			mTimeLapseOffBtn.setBackgroundResource(R.drawable.pressed_close);
		}
	}

	private void burstUiChange() {
		if (mType.equals("3")) {
			mBurst3Btn.setBackgroundResource(R.drawable.pressed_3);
			mBurst4Btn.setBackgroundResource(R.drawable.normal_4);
			mBurst5Btn.setBackgroundResource(R.drawable.normal_5);
			mBurstOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("4")) {
			mBurst3Btn.setBackgroundResource(R.drawable.normal_3);
			mBurst4Btn.setBackgroundResource(R.drawable.pressed_4);
			mBurst5Btn.setBackgroundResource(R.drawable.normal_5);
			mBurstOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("5")) {
			mBurst3Btn.setBackgroundResource(R.drawable.normal_3);
			mBurst4Btn.setBackgroundResource(R.drawable.normal_4);
			mBurst5Btn.setBackgroundResource(R.drawable.pressed_5);
			mBurstOffBtn.setBackgroundResource(R.drawable.normal_close);
		} else if (mType.equals("OFF")) {
			mBurst3Btn.setBackgroundResource(R.drawable.normal_3);
			mBurst4Btn.setBackgroundResource(R.drawable.normal_4);
			mBurst5Btn.setBackgroundResource(R.drawable.normal_5);
			mBurstOffBtn.setBackgroundResource(R.drawable.pressed_close);
		}
	}

	private void modeUiChange(String mode) {
		Log.d(TAG, "modeUiChange, mode: " + mode);
		mSocketService.sendMsg("CMD_LIVE_STOP", false);
		mSocketService.sendMsg("CMD_RTSP_TRANS_START", true);

		if (mode.startsWith("RECORDING")) {
			mSocketService.sendMsg("CMD_GET_Control_Recording", true);

			mPlayControlBtn.setVisibility(View.VISIBLE);
			mScreenStatusBtn.setVisibility(View.VISIBLE);
			mPhotographBtn.setVisibility(View.VISIBLE);

			mTimeLapseBtnLayout.setVisibility(View.VISIBLE);
			mBurstBtnLayout.setVisibility(View.GONE);

			timeLapseUiChange();
		} else if (mode.startsWith("PHOTO")) {
			mScreenStatusBtn.setVisibility(View.VISIBLE);
			mPhotographBtn.setVisibility(View.VISIBLE);
			mPlayControlBtn.setVisibility(View.GONE);

			mTimeLapseBtnLayout.setVisibility(View.GONE);
			mBurstBtnLayout.setVisibility(View.VISIBLE);

			burstUiChange();
		} else if (mode.startsWith("PREVIEW")) {
			mPlayControlBtn.setVisibility(View.GONE);
			mScreenStatusBtn.setVisibility(View.GONE);
			mPhotographBtn.setVisibility(View.GONE);

			mTimeLapseBtnLayout.setVisibility(View.GONE);
			mBurstBtnLayout.setVisibility(View.GONE);
		}
	}

	private void showModeUi(String mode) {
		Log.d(TAG, "mode: " + mode);

		String[] tmp = mode.split("&");
		if (mode.startsWith("PREVIEW")) {
			mModeType = "PREVIEW";
		} else {
			if (tmp != null) {
				mModeType = tmp[0];
				mType = tmp[1];
			}
		}

		if (mode.startsWith("LAPSE")) {
			timeLapseUiChange();
		} else if (mode.startsWith("BURST")) {
			burstUiChange();
		} else {
			modeUiChange(mode);
		}
	}

	private void pathIsExist()
	{
		File file = new File(BitmapUtils.getSDPath()+"/RkCamera/RkPhoto/") ;
		if(!file.exists())
			file.mkdirs();

		File file1 = new File(BitmapUtils.getSDPath()+"/RkCamera/RkVideo/") ;
		if(!file1.exists())
			file1.mkdirs();
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

		int orientation = getResources().getConfiguration().orientation;
		//Log.d(TAG, "orientation: " + orientation);
		if (ConnectIP.mProductType.equals(ConnectIP.mCvr) && orientation == Configuration.ORIENTATION_PORTRAIT && sw < sh) {
			mMediaPlayer.getVLCVout().setWindowSize(sw, sh/2); //tiantian, 开启竖屏surface显示在屏幕上端
			mapUiControl(true);
		} else /*if (orientation == Configuration.ORIENTATION_LANDSCAPE) */{
			mMediaPlayer.getVLCVout().setWindowSize(sw, sh);
			mapUiControl(false);
		}

		//Log.d("tiantian", "mVideoWidth: " + mVideoWidth + ", mVideoHeight: " + mVideoHeight);
		ViewGroup.LayoutParams lp = mVideoSurface.getLayoutParams();
		ViewGroup.LayoutParams lp1 = mVideoSurfaceFrame.getLayoutParams();
		ViewGroup.LayoutParams lp2 = mModeChangeFrame.getLayoutParams();
		if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
			lp.width  = ViewGroup.LayoutParams.MATCH_PARENT;
			if (ConnectIP.mProductType.equals(ConnectIP.mCvr) && orientation == Configuration.ORIENTATION_PORTRAIT && sw < sh) {
				lp.height = lp1.height = lp2.height = sh/2;
			} else /*if (orientation == Configuration.ORIENTATION_LANDSCAPE) */{
				lp.height = lp1.height = lp2.height = ViewGroup.LayoutParams.MATCH_PARENT;
			}
			mVideoSurface.setLayoutParams(lp);
			mVideoSurfaceFrame.setLayoutParams(lp1);
			mModeChangeFrame.setLayoutParams(lp2);

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

	private Handler recHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch (msg.what){
				case MSG_RECORD_FINISH:
					dealRecMsg(msg.obj.toString());
					break;
			}
		}
	};

	private void screenShot()
	{
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			String name = df.format(new Date());
			name = BitmapUtils.getSDPath() + "/RkCamera/RkVideo/" + name + ".png";
			File file = new File(name);
			if(!file.exists())
				file.createNewFile();

			if(mMediaPlayer.snapShot(name,1920,1080))
			{
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.saved_msg), Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.screen_shot_failed_msg), Toast.LENGTH_SHORT).show();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	private void screenShot(){
		Log.d(TAG, "screenShot");
		FileOutputStream fos = null;

		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			String filename = df.format(new Date()) + ".jpg";
			String path = BitmapUtils.getSDPath() + "/RkCamera/RkVideo/";
			File file = new File(path + filename);
			if(!file.exists())
				file.createNewFile();

			fos = new FileOutputStream(file);
			byte[] data = VLCUtil.getThumbnail(mMedia, 640, 368);
			Log.d(TAG, "bytes.length: " + data.length);
			if (data.length > 0) {
				fos.write(data);
			} else {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try{
				if (fos != null)
					fos.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
*/

	private void dealRecMsg(String msg){
		Log.d(TAG, "dealRecMsg, msg: " + msg);
		mPlayControlBtn.setEnabled(true);
		if (msg.startsWith(CMD_RECORD_IDLE) || msg.startsWith(CMD_CB_STOPREC) || msg.startsWith(CMD_CB_NO_SDCARD)) {
			mRecordStatus = RECORD_STATUS_STOP;
			mPlayControlBtn.setBackgroundResource(R.drawable.play_play);

			if (msg.startsWith(CMD_CB_NO_SDCARD)){
				Toast.makeText(this, this.getString(R.string.no_sdcard), Toast.LENGTH_SHORT).show();
			}
		} else if (msg.startsWith(CMD_RECORD_BUSY) || msg.startsWith(CMD_CB_STARTREC)){
			mRecordStatus = RECORD_STATUS_START;
			mPlayControlBtn.setBackgroundResource(R.drawable.play_stop);
		} else if (msg.startsWith(CMD_CB_GET_MODE)) {
			String[] tmp = msg.split(":");
			showModeUi(tmp[1]);
		} else if (msg.startsWith(CMD_CB_GPS_UPDATA)) {
			dealGpsInfo(msg);
		}
	}

	//lg: CMD_CB_GPS_UPDATA:$GPRMC,001534.00,A,2606.33201,N,11916.59335,E,30.531,321.17,010817,,,A*56
	//lg: CMD_CB_GPS_UPDATA:$GPGGA,001534.00,2606.33201,N,11916.59335,E,1,06,1.60,-13.3,M,10.1,M,,*44
	//private int test = 0;
	private void dealGpsInfo(String msg) {
		//Log.d(TAG, "dealGpsInfo: " + msg);
		String temp[] = msg.split(":");
		if (mMapView != null && temp != null) {
			String str[] = temp[1].split(",");
			if (str != null && str[0].endsWith(GpsParseUtil.XXRMC)) {
				GpsInfo gpsInfo = new GpsInfo();
				if (GpsParseUtil.nmeaDataParse(gpsInfo, temp[1], GpsParseUtil.RMC, temp[1].length())) {
					mMapView.WGStoGCJ(gpsInfo);
					mMapView.updateRealLine(gpsInfo);
					//Log.d(TAG, "mGpsInfoList.size(): " + mGpsInfoList.size());
					
					//test
					//mMapView.updateRealLine(mGpsInfoList.get(test));
					//test++;
					//if (test == mGpsInfoList.size())
					//	test = 0;
				}
			}
		}
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (SocketService.ACTION_VIDEO_PLAYER_ACTIVITY.equals(intent.getAction())){
				String info = intent.getStringExtra("msg");
				Message msg = new Message();
				msg.what = MSG_RECORD_FINISH;
				msg.obj = info;
				recHandler.sendMessage(msg);

				//dealRecMsg(info);
			}
		}
	};

	/*-----test-----*/
	private void getGpsDateFromFileName(String filename) {
		Log.d(TAG, "filename: " + filename);
		String[] tmp = null;
		String line = null;
		FileInputStream inputStream = null;
		InputStreamReader inReader = null;
		BufferedReader bufReader = null;

		try {
			inputStream = new FileInputStream(filename);
			inReader = new InputStreamReader(inputStream, "UTF-8");
			bufReader = new BufferedReader(inReader);
			while((line = bufReader.readLine()) != null) {
				if (line.startsWith(GpsParseUtil.GPSENDTIME)) {
					//Log.d(TAG, "read to the end of the file, break");
					break;
				} else {
					tmp = line.split(",");
					if (tmp[0].endsWith(GpsParseUtil.XXRMC)) {
						GpsInfo gpsInfo = new GpsInfo();
						if (GpsParseUtil.nmeaDataParse(gpsInfo, line, GpsParseUtil.RMC, line.length())) {
							mMapView.WGStoGCJ(gpsInfo);
							//Log.d("tiantian", gpsInfo.toString());
							mGpsInfoList.add(gpsInfo);
							//Log.d(TAG, "mGpsInfoList.size(): " + mGpsInfoList.size());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "read " + filename + " error");
		} finally {
			inputReadDeInit(inputStream, inReader, bufReader);
		}
	}

	private void inputReadDeInit(FileInputStream inputStream, InputStreamReader inReader, BufferedReader bufReader)
	{
		try {
			if (bufReader != null)
				bufReader.close();

			if (inReader != null)
				inReader.close();

			if (inputStream != null)
				inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(TAG, "close file hangder error!");
		}
	}
	/*-----test-----*/
}