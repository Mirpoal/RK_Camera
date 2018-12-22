package com.example.xng.rkcamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("ValidFragment")
public class IpCameraSetting extends Fragment implements View.OnClickListener,
        DateTimeDialog.MyOnDateSetListener, CompoundButton.OnCheckedChangeListener {
    static final String TAG ="IpCameraSetting";

    private final int MSG_LOAD_FINISH = 10;

    private Socket mUpdateSocket = null;
    private OutputStream mUpdateWriter = null;
    private boolean mUpdateFirmware = false;

    private SocketService mSocketService = SocketService.getInstance();
    private String mProductType = null;

    /* CVR unique Settings */
    private LinearLayout mFrontCamera, mBackCamera, m3Dnr, mAdas, mMotionDetection,
            mRecordMode;
    private TextView mRecordModeChoice;
    private Switch m3DnrSwitch, mAdasSwitch, mMotionDetectionSwitch;
    private int mFrontCameraIndex = 0, mBackCameraIndex = 0, mRecordModeIndex = 0;
    /* CVR unique Settings */

    /* SportDV unique Settings */
    private LinearLayout mCarModel, mVideoResolution, mPhotoResolution, mLoopRecord,
            mDvs, mKeySound, mAutoShutdown, mScreensaver, mGyrosensorCalibration;
    private Switch mCarModelSwitch, mLoopRecordSwitch, mDvsSwitch,
            mKeySoundSwitch, mAutoShutdownSwitch, mScreensaverSwitch;
    private int mVideoResolutionIndex = 0, mPhotoResolutionIndex = 0;
    /* SportDV unique Settings */

    /* CVR and SportDV Shared Settings */
    private LinearLayout mWhiteBalance, mExposure, mDateStamp, mRecordAudio, mBootRecord,
            mLanguage, mFrequency, mBright, mFormat, mDateSetup, mRecovery, mFirmwareUpdate,
            mVersion, mConnectSetup, mVideoLength, mDebugTest, mIdc, mTimelapseRecord,
            mLicence, mFlip, mAutoOffScreen, mVideoQuality, mCollisionDetect, mPipLayout;

    private TextView mWhiteBalanceChoice, mExposureChoice, mLanguageChoice, mFrequencyChoice,
            mBrightChoice, mVersionChoice, mVideoLengthChoice, mDateSetupChoice, mTimelapseRecordChoice,
            mLicenceChoice, mVideoQualityChoice, mCollisionDetectChoice;

    private Switch mDateStampSwitch, mRecordAudioSwitch, mBootRecordSwitch, mIdcSwitch,
            mFlipSwitch, mAutoOffScreenSwitch, mPipSwitch;

    private int mWhiteBalanceIndex = 0, mExposureIndex = 0, mLanguageIndex = 0, mFrequencyIndex = 0,
            mBrightIndex = 0, mTimelapseRecordIndex = 0, mVideoQualityIndex = 0, mCollisionDetectIndex = 0,
            mVideoLengthIndex = 0;
    /* CVR and SportDV Shared Settings */

    private AlertDialog mDialog = null;
    private TextView mDialogTv = null;
    private Button mDialogPositiveBtn = null;
    private View mDialogView = null;

    private DateTimeDialog mDateTimeDialog;
    private ProgressBar mProgressBar;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String string_yes = null, string_no = null, string_finish = null;
    private String string_front = null, string_rear = null, string_double = null;
    private String string_english = null, string_chinese = null;
    private String string_low = null, string_middle = null, string_hight = null;
    private String string_1min = null, string_3min = null, string_5min = null;
    private String string_auto = null, string_daylight = null, string_fluocrescence = null,
            string_cloudysky = null, string_tungsten = null;
    private String string_close = null, string_space_1s = null, string_space_5s = null,
            string_space_10s = null, string_space_30s = null, string_space_60s = null;

    public IpCameraSetting(String productType) {
        Log.d(TAG, "new IpCameraSetting, productType: " + productType);
        mProductType = productType;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (SocketService.ACTION_IPCAMERA_SETTING.equals(intent.getAction())){
                Message msg = new Message();
                msg.what = MSG_LOAD_FINISH;
                msg.obj = intent.getStringExtra("msg");
                mHandler.sendMessage(msg);
            }
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_LOAD_FINISH:
                    dealSocketRevMsg(msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SocketService.ACTION_IPCAMERA_SETTING);
        getActivity().registerReceiver(mReceiver, filter);
        mSocketService.sendMsg("CMD_GET_ARGSETTING", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view;
        if (mProductType.equals(ConnectIP.mCvr))
            view = inflater.inflate(R.layout.ipcamera_setting, container, false);
        else
            view = inflater.inflate(R.layout.ipcamera_setting_dv, container, false);

        initMsgString();
        initLinearLayout(view);
        initTextView(view);
        initSwitch(view);

        initDialog();

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mDateTimeDialog = new DateTimeDialog(getContext(),null,this);
        return view;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    public void refresh(){
        mSocketService.sendMsg("CMD_GET_ARGSETTING", false);
    }

    private void initDialog() {
        mDialogView = View.inflate(getContext(), R.layout.dialog_layout, null);
        mDialog = new AlertDialog.Builder(getActivity())
                .setView(mDialogView)
                .setPositiveButton(string_yes, null)
                .setCancelable(false)
                .create();
    }

    private void initMsgString() {
        string_yes = getActivity().getString(R.string.yes);
        string_no = getActivity().getString(R.string.no);
        string_finish = getActivity().getString(R.string.finish);

        string_auto = getActivity().getString(R.string.auto);
        string_daylight = getActivity().getString(R.string.Daylight);
        string_fluocrescence = getActivity().getString(R.string.fluocrescence);
        string_cloudysky = getActivity().getString(R.string.cloudysky);
        string_tungsten = getActivity().getString(R.string.tungsten);

        string_english = getActivity().getString(R.string.english);
        string_chinese = getActivity().getString(R.string.chinese);

        string_front = getActivity().getString(R.string.front_camera);
        string_rear = getActivity().getString(R.string.rear_camera);
        string_double = getActivity().getString(R.string.double_camera);

        string_low = getActivity().getString(R.string.low);
        string_middle = getActivity().getString(R.string.middle);
        string_hight = getActivity().getString(R.string.hight);

        string_1min = getActivity().getString(R.string.one_min);
        string_3min = getActivity().getString(R.string.three_min);
        string_5min = getActivity().getString(R.string.five_min);

        string_close = getActivity().getString(R.string.close);
        string_space_1s = getActivity().getString(R.string.space_1s);
        string_space_5s = getActivity().getString(R.string.space_5s);
        string_space_10s = getActivity().getString(R.string.space_10s);
        string_space_30s = getActivity().getString(R.string.space_30s);
        string_space_60s = getActivity().getString(R.string.space_60s);
    }

    private void settingEnableCvr() {
        mFrontCamera.setEnabled(true);
        mBackCamera.setEnabled(true);
        m3Dnr.setEnabled(true);
        mAdas.setEnabled(true);
        mMotionDetection.setEnabled(true);
        mRecordMode.setEnabled(true);

        m3DnrSwitch.setEnabled(true);
        mAdasSwitch.setEnabled(true);
        mMotionDetectionSwitch.setEnabled(true);
    }

    private void settingEnableSportDv() {
        mCarModel.setEnabled(true);
        mLoopRecord.setEnabled(true);
        mVideoResolution.setEnabled(true);
        mPhotoResolution.setEnabled(true);
        mDvs.setEnabled(true);
        mKeySound.setEnabled(true);
        mAutoShutdown.setEnabled(true);
        mScreensaver.setEnabled(true);
        mGyrosensorCalibration.setEnabled(true);

        mCarModelSwitch.setEnabled(true);
        mLoopRecordSwitch.setEnabled(true);
        mDvsSwitch.setEnabled(true);
        mKeySoundSwitch.setEnabled(true);
        mAutoShutdownSwitch.setEnabled(true);
        mScreensaverSwitch.setEnabled(true);
    }

    private void settingEnableShared() {
        mWhiteBalance.setEnabled(true);
        mExposure.setEnabled(true);
        mDateStamp.setEnabled(true);
        mRecordAudio.setEnabled(true);
        mBootRecord.setEnabled(true);
        mLanguage.setEnabled(true);
        mFrequency.setEnabled(true);
        mBright.setEnabled(true);
        mFormat.setEnabled(true);
        mDateSetup.setEnabled(true);
        mRecovery.setEnabled(true);
        mFirmwareUpdate.setEnabled(true);
        mVersion.setEnabled(true);
        mConnectSetup.setEnabled(true);
        mVideoLength.setEnabled(true);
        mDebugTest.setEnabled(true);
        mIdc.setEnabled(true);
        mFlip.setEnabled(true);
        mAutoOffScreen.setEnabled(true);
        mTimelapseRecord.setEnabled(true);
        mLicence.setEnabled(true);
        mVideoQuality.setEnabled(true);
        mCollisionDetect.setEnabled(true);

        mDateStampSwitch.setEnabled(true);
        mRecordAudioSwitch.setEnabled(true);
        mBootRecordSwitch.setEnabled(true);
        mIdcSwitch.setEnabled(true);
        mFlipSwitch.setEnabled(true);
        mAutoOffScreenSwitch.setEnabled(true);
        mPipSwitch.setEnabled(true);
    }

    private void settingDisableCvr() {
        mFrontCamera.setEnabled(false);
        mBackCamera.setEnabled(false);
        m3Dnr.setEnabled(false);
        mAdas.setEnabled(false);
        mMotionDetection.setEnabled(false);
        mRecordMode.setEnabled(false);

        m3DnrSwitch.setEnabled(false);
        mAdasSwitch.setEnabled(false);
        mMotionDetectionSwitch.setEnabled(false);
    }

    private void settingDisableSportDv() {
        mCarModel.setEnabled(false);
        mLoopRecord.setEnabled(false);
        mVideoResolution.setEnabled(false);
        mPhotoResolution.setEnabled(false);
        mDvs.setEnabled(false);
        mKeySound.setEnabled(false);
        mAutoShutdown.setEnabled(false);
        mScreensaver.setEnabled(false);
        mGyrosensorCalibration.setEnabled(false);

        mCarModelSwitch.setEnabled(false);
        mLoopRecordSwitch.setEnabled(false);
        mDvsSwitch.setEnabled(false);
        mKeySoundSwitch.setEnabled(false);
        mAutoShutdownSwitch.setEnabled(false);
        mScreensaverSwitch.setEnabled(false);
    }

    private void settingDisableShared() {
        mWhiteBalance.setEnabled(false);
        mExposure.setEnabled(false);
        mDateStamp.setEnabled(false);
        mRecordAudio.setEnabled(false);
        mBootRecord.setEnabled(false);
        mLanguage.setEnabled(false);
        mFrequency.setEnabled(false);
        mBright.setEnabled(false);
        mFormat.setEnabled(false);
        mDateSetup.setEnabled(false);
        mRecovery.setEnabled(false);
        mFirmwareUpdate.setEnabled(false);
        mVersion.setEnabled(false);
        mConnectSetup.setEnabled(false);
        mVideoLength.setEnabled(false);
        mDebugTest.setEnabled(false);
        mIdc.setEnabled(false);
        mFlip.setEnabled(false);
        mAutoOffScreen.setEnabled(false);
        mTimelapseRecord.setEnabled(false);
        mLicence.setEnabled(false);
        mVideoQuality.setEnabled(false);
        mCollisionDetect.setEnabled(false);

        mDateStampSwitch.setEnabled(false);
        mRecordAudioSwitch.setEnabled(false);
        mBootRecordSwitch.setEnabled(false);
        mIdcSwitch.setEnabled(false);
        mFlipSwitch.setEnabled(false);
        mAutoOffScreenSwitch.setEnabled(false);
        mPipSwitch.setEnabled(false);
    }

    private void initCvrLinearLayout(View view) {
        mFrontCamera = (LinearLayout)view.findViewById(R.id.front_camera);
        mBackCamera = (LinearLayout)view.findViewById(R.id.back_camera);
        m3Dnr = (LinearLayout)view.findViewById(R.id.weqw);
        mAdas = (LinearLayout)view.findViewById(R.id.adas);
        mMotionDetection = (LinearLayout)view.findViewById(R.id.motion_detection);
        mRecordMode = (LinearLayout)view.findViewById(R.id.recordmode);
        mPipLayout = (LinearLayout) view.findViewById(R.id.layout_pip);

        if(getResources().getBoolean(R.bool.show_pip)){
            mPipLayout.setVisibility(View.VISIBLE);
        }

        mFrontCamera.setOnClickListener(this);
        mBackCamera.setOnClickListener(this);
        m3Dnr.setOnClickListener(this);
        mAdas.setOnClickListener(this);
        mMotionDetection.setOnClickListener(this);
        mRecordMode.setOnClickListener(this);
    }

    private void initSportDvLinearLayout(View view) {
        mCarModel = (LinearLayout)view.findViewById(R.id.car_model);
        mLoopRecord = (LinearLayout)view.findViewById(R.id.loop_record);
        mVideoResolution = (LinearLayout)view.findViewById(R.id.video_resolution);
        mPhotoResolution = (LinearLayout)view.findViewById(R.id.photo_resolution);
        mDvs = (LinearLayout)view.findViewById(R.id.dvs);
        mKeySound  = (LinearLayout)view.findViewById(R.id.key_sound);
        mAutoShutdown = (LinearLayout)view.findViewById(R.id.auto_shutdown);
        mScreensaver = (LinearLayout)view.findViewById(R.id.screensaver);
        mGyrosensorCalibration = (LinearLayout)view.findViewById(R.id.gyrosensor_calibration);

        mCarModel.setOnClickListener(this);
        mLoopRecord.setOnClickListener(this);
        mVideoResolution.setOnClickListener(this);
        mPhotoResolution.setOnClickListener(this);
        mDvs.setOnClickListener(this);
        mKeySound.setOnClickListener(this);
        mAutoShutdown.setOnClickListener(this);
        mScreensaver.setOnClickListener(this);
        mGyrosensorCalibration.setOnClickListener(this);
    }

    private void initSharedLinearLayout(View view) {
        mWhiteBalance = (LinearLayout)view.findViewById(R.id.white_balance);
        mExposure = (LinearLayout)view.findViewById(R.id.exposure);
        mDateStamp = (LinearLayout)view.findViewById(R.id.date_stamp);
        mRecordAudio = (LinearLayout)view.findViewById(R.id.record_audio);
        mBootRecord = (LinearLayout)view.findViewById(R.id.boot_record);
        mLanguage = (LinearLayout)view.findViewById(R.id.language);
        mFrequency = (LinearLayout)view.findViewById(R.id.frequency);
        mBright = (LinearLayout)view.findViewById(R.id.bright);
        mFormat = (LinearLayout)view.findViewById(R.id.format);
        mDateSetup = (LinearLayout)view.findViewById(R.id.date_set);
        mRecovery = (LinearLayout)view.findViewById(R.id.recovery);
        mFirmwareUpdate = (LinearLayout)view.findViewById(R.id.firmware_update);
        mVersion = (LinearLayout)view.findViewById(R.id.version);
        mConnectSetup = (LinearLayout)view.findViewById(R.id.connect_set);
        mVideoLength = (LinearLayout)view.findViewById(R.id.videolength);
        mDebugTest = (LinearLayout)view.findViewById(R.id.debug_test);
        mIdc = (LinearLayout)view.findViewById(R.id.idc);
        mFlip = (LinearLayout)view.findViewById(R.id.flip);
        mAutoOffScreen = (LinearLayout)view.findViewById(R.id.auto_off_screen);
        mTimelapseRecord = (LinearLayout)view.findViewById(R.id.timelapse_record);
        mLicence = (LinearLayout)view.findViewById(R.id.licence);
        mVideoQuality = (LinearLayout)view.findViewById(R.id.video_quality);
        mCollisionDetect = (LinearLayout)view.findViewById(R.id.collision_detect);

        mWhiteBalance.setOnClickListener(this);
        mExposure.setOnClickListener(this);
        mDateStamp.setOnClickListener(this);
        mRecordAudio.setOnClickListener(this);
        mBootRecord.setOnClickListener(this);
        mLanguage.setOnClickListener(this);
        mFrequency.setOnClickListener(this);
        mBright.setOnClickListener(this);
        mVideoLength.setOnClickListener(this);
        mFormat.setOnClickListener(this);
        mDateSetup.setOnClickListener(this);
        mRecovery.setOnClickListener(this);
        mFirmwareUpdate.setOnClickListener(this);
        mVersion.setOnClickListener(this);
        mConnectSetup.setOnClickListener(this);
        mDebugTest.setOnClickListener(this);
        mIdc.setOnClickListener(this);
        mFlip.setOnClickListener(this);
        mAutoOffScreen.setOnClickListener(this);
        mTimelapseRecord.setOnClickListener(this);
        mLicence.setOnClickListener(this);
        mVideoQuality.setOnClickListener(this);
        mCollisionDetect.setOnClickListener(this);
    }

    private void initSportDvSwitch(View view) {
        mCarModelSwitch = (Switch)view.findViewById(R.id.car_model_change);
        mLoopRecordSwitch = (Switch)view.findViewById(R.id.loop_record_change);
        mDvsSwitch  = (Switch)view.findViewById(R.id.dvs_change);
        mKeySoundSwitch  = (Switch)view.findViewById(R.id.key_sound_change);
        mAutoShutdownSwitch  = (Switch)view.findViewById(R.id.auto_shutdown_change);
        mScreensaverSwitch  = (Switch)view.findViewById(R.id.screensaver_change);

        mCarModelSwitch.setOnCheckedChangeListener(this);
        mLoopRecordSwitch.setOnCheckedChangeListener(this);
        mDvsSwitch.setOnCheckedChangeListener(this);
        mKeySoundSwitch.setOnCheckedChangeListener(this);
        mAutoShutdownSwitch.setOnCheckedChangeListener(this);
        mScreensaverSwitch.setOnCheckedChangeListener(this);
    }

    private void initCvrSwitch(View view) {
        m3DnrSwitch =(Switch)view.findViewById(R.id.weqw_change);
        mAdasSwitch = (Switch)view.findViewById(R.id.adas_change);
        mMotionDetectionSwitch = (Switch)view.findViewById(R.id.motion_detection_change);

        m3DnrSwitch.setOnCheckedChangeListener(this);
        mAdasSwitch.setOnCheckedChangeListener(this);
        mMotionDetectionSwitch.setOnCheckedChangeListener(this);
    }

    private void initSharedSwitch(View view) {
        mDateStampSwitch = (Switch)view.findViewById(R.id.date_stamp_change);
        mRecordAudioSwitch = (Switch)view.findViewById(R.id.record_audio_change);
        mBootRecordSwitch = (Switch)view.findViewById(R.id.boot_record_change);
        mIdcSwitch  = (Switch)view.findViewById(R.id.idc_change);
        mFlipSwitch  = (Switch)view.findViewById(R.id.flip_change);
        mAutoOffScreenSwitch  = (Switch)view.findViewById(R.id.auto_off_screen_change);
        mPipSwitch = (Switch) view.findViewById(R.id.pip_switch);

        mDateStampSwitch.setOnCheckedChangeListener(this);
        mRecordAudioSwitch.setOnCheckedChangeListener(this);
        mBootRecordSwitch.setOnCheckedChangeListener(this);
        mIdcSwitch.setOnCheckedChangeListener(this);
        mFlipSwitch.setOnCheckedChangeListener(this);
        mAutoOffScreenSwitch.setOnCheckedChangeListener(this);
        mPipSwitch.setOnCheckedChangeListener(this);
    }

    private void initTextView(View view) {
        mWhiteBalanceChoice = (TextView)view.findViewById(R.id.white_balance_choice);
        mExposureChoice = (TextView)view.findViewById(R.id.exposure_choice);
        mLanguageChoice = (TextView)view.findViewById(R.id.language_choice);
        mFrequencyChoice = (TextView)view.findViewById(R.id.frequency_choice);
        mBrightChoice = (TextView)view.findViewById(R.id.bright_choice);
        mVersionChoice = (TextView)view.findViewById(R.id.version_choice);
        mVideoLengthChoice = (TextView)view.findViewById(R.id.videolength_choice);
        mDateSetupChoice = (TextView)view.findViewById(R.id.date_set_choice);
        mTimelapseRecordChoice = (TextView)view.findViewById(R.id.timelapse_record_choice);
        mLicenceChoice = (TextView)view.findViewById(R.id.licence_choice);
        mVideoQualityChoice = (TextView)view.findViewById(R.id.video_quality_choice);
        mCollisionDetectChoice = (TextView)view.findViewById(R.id.collision_detect_choice);

        if (mProductType.equals(ConnectIP.mCvr))
            mRecordModeChoice = (TextView)view.findViewById(R.id.recordmode_choice);
    }

    private void initLinearLayout(View view) {
        initSharedLinearLayout(view);

        if (mProductType.equals(ConnectIP.mSportDv))
            initSportDvLinearLayout(view);
        else
            initCvrLinearLayout(view);
    }

    private void initSwitch(View view) {
        initSharedSwitch(view);

        if (mProductType.equals(ConnectIP.mSportDv))
            initSportDvSwitch(view);
        else
            initCvrSwitch(view);
    }

    private void settingEnable() {
        settingEnableShared();

        if (mProductType.equals(ConnectIP.mSportDv))
            settingEnableSportDv();
        else
            settingEnableCvr();
    }

    private void settingDisable() {
        settingDisableShared();

        if (mProductType.equals(ConnectIP.mSportDv))
            settingDisableSportDv();
        else
            settingDisableCvr();
    }

    private void dealSocketRevMsg(String msg) {
        if (msg.startsWith("CMD_ACK_OTA_Upload_File")) {
            firmware_update();
        } else if (msg.startsWith("CMD_ACK_OTA_Upload_Success")) {
            mSocketService.sendMsg("CMD_OTA_Update", false);
            firmware_update_finish();
        } else if (msg.startsWith("CMD_ACK_OTA_Upload_Fault")) {
            firmware_update_fault();
        } else if (msg.startsWith("CMD_ACK_GET_ARGSETTING")){
            dealMsg(msg);
        }else if (msg.startsWith("CMD_GET_ACK_FORMAT_STATUS")) {
            dealFormatMsg(msg);
        }  else if (msg.startsWith("CMD_CB_")) {
            dealCmdCallbackMsg(msg);
        } else if (msg.startsWith("CMD_ACK_FRONT_CAMERARESPLUTION")) {
            dealFrontCamMsg(msg);
            dealVideoResolutionMsg(msg);
        } else if (msg.startsWith("CMD_ACK_BACK_CAMERARESPLUTION")){
            dealBackCamMsg(msg);
        } else if (msg.startsWith("CMD_ACK_GET_PHOTO_QUALITY")){
            dealPhotoResolutionMsg(msg);
        }
    }

    private void firmware_update() {
        if (ConnectIP.IP != null){
            new Thread() {
                @Override
                public void run() {
                    try {
                        String filePath = null;
                        int readSize = -1;
                        byte[] data = new byte[4096];

                        if (mUpdateSocket == null){
                            mUpdateSocket = new Socket(ConnectIP.IP, 18890);
                            mUpdateWriter = mUpdateSocket.getOutputStream();
                        }

                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RkCamera/Firmware.img";
                        } else {
                            filePath = "/RkCamera/Firmware.img";
                        }

                        FileInputStream file = new FileInputStream(filePath);
                        while (true){
                            readSize = file.read(data);
                            if (readSize < 0)
                                break;

                            mUpdateWriter.write(data);
                            mUpdateWriter.flush();
                        }
                        file.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Log.d(TAG, "exit firmware update thread, close socket");
                        firmware_update_close_socket();
                    }
                }
            }.start();
        }
    }

    private  void firmware_update_close_socket() {
        try {
            if (mUpdateWriter != null){
                mUpdateWriter.close();
                mUpdateWriter = null;
            }

            if (mUpdateSocket != null){
                mUpdateSocket.close();
                mUpdateSocket = null;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void firmware_update_finish() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.firmware_updateing_msg));
        builder.setPositiveButton(string_finish, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "开始升级固件，退出APK！");
                getActivity().finish();
            }
        });
        builder.create().show();
        mUpdateFirmware = false;
    }

    private String firmware_update_getcrc32() {
        String filePath = null;
        String crc32 = null;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RkCamera/Firmware.img";
        } else {
            filePath = "/RkCamera/Firmware.img";
        }

        try {
            FileInputStream file = new FileInputStream(filePath);
            crc32 = CRC.fileCrc32(file);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return crc32;
    }

    private void firmware_update_fault() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setMessage(getActivity().getString(R.string.again_update_firmware_msg));
        builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "firmware_update again");
                mSocketService.sendMsg("CMD_OTA_Upload_FileCRC:" + firmware_update_getcrc32(), false);
            }
        });
        builder.setNegativeButton(string_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mUpdateFirmware = false;
            }
        });
        builder.create().show();
    }

    private void dealMsg(String msg) {  //tiantian, 新协议更新了setting项，dealMsg要添加相应解析处理
        String[] temp0 = msg.split("Videolength:");
        String[] temp1 = temp0[1].split("RecordMode:");
        String Videolength = temp1[0];
        String[] temp2 = temp1[1].split("3DNR:");
        String RecordMode = temp2[0];
        String[] temp3 = temp2[1].split("ADAS:");
        String m3DNR = temp3[0];
        String[] temp4 = temp3[1].split("WhiteBalance:");
        String ADAS = temp4[0];
        String[] temp5 = temp4[1].split("Exposure:");
        String WhiteBalance = temp5[0];
        String[] temp6 = temp5[1].split("MotionDetection:");
        String Exposure = temp6[0];
        String[] temp7 = temp6[1].split("DataStamp:");
        String MotionDetection = temp7[0];
        String[] temp8 = temp7[1].split("RecordAudio:");
        String DataStamp = temp8[0];
        String[] temp9 = temp8[1].split("BootRecord:");
        String RecordAudio = temp9[0];
        String[] temp10 = temp9[1].split("Language:");
        String BootRecord = temp10[0];
        String[] temp11 = temp10[1].split("Frequency:");
        String Language = temp11[0];
        String[] temp12 = temp11[1].split("Bright:");
        String Frequency = temp12[0];
        String[] temp13 = temp12[1].split("IDC:");
        String Bright = temp13[0];
        String[] temp14 = temp13[1].split("FLIP:");
        String idc = temp14[0];
        String[] temp15 = temp14[1].split("TIME_LAPSE:");
        String flip = temp15[0];
        String[] temp16 = temp15[1].split("AUTOOFF_SCREEN:");
        String timelapse = temp16[0];
        String[] temp17 = temp16[1].split("QUAITY:");
        String autoOffScreen = temp17[0];
        String[] temp18 = temp17[1].split("Collision:");
        String videoQuaity = temp18[0];
        String[] temp19 = temp18[1].split("LICENCE_PLATE:");
        String collision = temp19[0];
        String[] temp20 = temp19[1].split("PIP:");
        String licencePlate = temp20[0];
        String[] temp21 = temp20[1].split("DVS:");
        String pipState = temp21[0];
        String[] temp22 = temp21[1].split("Time:");
        String dvsState = temp22[0];
        String[] temp23 = temp22[1].split("Version:");
        String Time = temp23[0];
        String Version = temp23[1];

        set3Dnr(m3DNR);
        setAdas(ADAS);
        setMotionDetection(MotionDetection);
        setRecordmode(RecordMode);
        setVideolength(Videolength);
        setWhiteBalance(WhiteBalance);
        setLanguage(Language);
        setBright(Bright);
        setDataStamp(DataStamp);
        setRecordAudio(RecordAudio);
        setBootRecord(BootRecord);
        setIdc(idc);
        setFlip(flip);
        setAutoOffScreen(autoOffScreen);
        setTimelapseRecord(timelapse);
        setVideoQuality(videoQuaity);
        setCollisionDetect(collision);
        setPip(pipState);
        setDvs(dvsState);

        mLicenceChoice.setText(licencePlate);
        mExposureChoice.setText(Exposure);
        mFrequencyChoice.setText(Frequency);
        mVersionChoice.setText(Version);
        mDateSetupChoice.setText(Time);
    }

    private void dealCmdCallbackMsg(String msg) {
        Log.d(TAG, "dealCmdCallbackMsg: " + msg);
        String[] tmp = msg.split(":");

        if (tmp == null)
            return;

        if (tmp[0].equals("CMD_CB_3DNR")) {
            set3Dnr(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_ADAS")) {
            setAdas(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_MotionDetection")) {
            setMotionDetection(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_DataStamp")) {
            setDataStamp(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_RecordAudio")) {
            setRecordAudio(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_BootRecord")) {
            setBootRecord(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_WhiteBalance")) {
            setWhiteBalance(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Language")) {
            setLanguage(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Bright")) {
            setBright(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Videolength")) {
            setVideolength(tmp[1]);
        }  else if (tmp[0].equals("CMD_CB_RecordMode")) {
            setRecordmode(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Exposure")) {
            mExposureChoice.setText(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Frequency")) {
            mFrequencyChoice.setText(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Time")) {
            mDateSetupChoice.setText(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Collision")) {
            setCollisionDetect(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_QUAITY")) {
            setVideoQuality(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_TIME_LAPSE")) {
            setTimelapseRecord(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_IDC")) {
            setIdc(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_FLIP")) {
            setFlip(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_AUTOOFF_SCREEN")) {
            setAutoOffScreen(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_LICENCE_PLATE")) {
            mLicenceChoice.setText(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_FrontCamera")) {
            setFrontCamera(tmp[1]);
            setVideoResolution(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_BackCamera")) {
            setBackCamera(tmp[1]);
        }  else if (tmp[0].equals("CMD_CB_Format")) {
            setFormat(tmp[1]);
        }  else if (tmp[0].equals("CMD_CB_PIP")) {
            setPip(tmp[1]);
        }  else if (tmp[0].equals("CMD_CB_DVS")) {
            setDvs(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_DVS_CALIB")) {
            dealDvsCalibration(tmp[1]);
        } else if (tmp[0].equals("CMD_CB_Photo_Quality")) {
            setPhotoQuality(tmp[1]);
        }
    }

    private void setFormat(String formatStatus) {
        if (formatStatus.equals("Start")) {
            mProgressBar.setVisibility(View.VISIBLE);
            settingDisable();
            Intent intent = new Intent(MainActivity.IPCAMERA_SETTING_DISABLE_UI);
            getActivity().sendBroadcast(intent);
        } else if (formatStatus.equals("Finish")) {
            formatBuilder(getActivity().getString(R.string.format_finish_msg));
        } else if (formatStatus.equals("Fault")) {
            formatBuilder(getActivity().getString(R.string.format_fail_msg));
        }
    }

    private void formatBuilder(String msg) {
        AlertDialog.Builder format_builder = new AlertDialog.Builder(getActivity());
        format_builder.setMessage(msg)
                .setPositiveButton(string_finish, null)
                .create().show();

        mProgressBar.setVisibility(View.GONE);
        settingEnable();
        Intent intent = new Intent(MainActivity.IPCAMERA_SETTING_ENABLE_UI);
        getActivity().sendBroadcast(intent);

    }

    private void setFrontCamera(String frontCamera) {
        //if (mProductType.equals(ConnectIP.mCvr)) {
            String index = frontCamera.substring("front".length());
            mFrontCameraIndex = Integer.parseInt(index) - 1;
        //}
    }

    private void setBackCamera(String backCamera) {
        if (mProductType.equals(ConnectIP.mCvr)) {
            String index = backCamera.substring("back".length());
            mBackCameraIndex = Integer.parseInt(index) - 1;
        }
    }

    private void setPhotoQuality(String photoQuality) {
        if (mProductType.equals(ConnectIP.mSportDv)) {
            mPhotoResolutionIndex = Integer.parseInt(photoQuality) - 1;
        }
    }

    private void setVideoResolution(String frontCamera) {
        if (mProductType.equals(ConnectIP.mSportDv)) {
            String index = frontCamera.substring("front".length());
            mVideoResolutionIndex = Integer.parseInt(index) - 1;
        }
    }

    private void set3Dnr(String m3dnr) {
        if (mProductType.equals(ConnectIP.mCvr)) {
            if(m3dnr.equals("ON")){
                m3DnrSwitch.setChecked(true);
            }else if(m3dnr.equals("OFF")){
                m3DnrSwitch.setChecked(false);
            }
        }
    }

    private void setAdas(String adas) {
        if (mProductType.equals(ConnectIP.mCvr)) {
            if(adas.equals("ON")){
                mAdasSwitch.setChecked(true);
            }else if(adas.equals("OFF")){
                mAdasSwitch.setChecked(false);
            }
        }
    }

    private void setMotionDetection(String motionDetection) {
        if (mProductType.equals(ConnectIP.mCvr)) {
            if(motionDetection.equals("ON")){
                mMotionDetectionSwitch.setChecked(true);
            }else if(motionDetection.equals("OFF")){
                mMotionDetectionSwitch.setChecked(false);
            }
        }
    }

    private void setRecordmode(String recordmode) {
        if (mProductType.equals(ConnectIP.mCvr)) {
            if (recordmode.equals("Front")) {
                mRecordModeChoice.setText(string_front);
            } else if(recordmode.equals("Rear")) {
                mRecordModeChoice.setText(string_rear);
            } else if(recordmode.equals("Double")) {
                mRecordModeChoice.setText(string_double);
            }
        }
    }

    private void setDataStamp(String dataStamp) {
        if(dataStamp.equals("ON")){
            mDateStampSwitch.setChecked(true);
        }else if(dataStamp.equals("OFF")){
            mDateStampSwitch.setChecked(false);
        }
    }

    private void setRecordAudio(String recordAudio) {
        if(recordAudio.equals("ON")){
            mRecordAudioSwitch.setChecked(true);
        }else if(recordAudio.equals("OFF")){
            mRecordAudioSwitch.setChecked(false);
        }
    }

    private void setBootRecord(String bootRecord) {
        if(bootRecord.equals("ON")){
            mBootRecordSwitch.setChecked(true);
        }else if(bootRecord.equals("OFF")){
            mBootRecordSwitch.setChecked(false);
        }
    }

    private void setIdc(String idc) {
        if(idc.equals("ON")){
            mIdcSwitch.setChecked(true);
        }else if(idc.equals("OFF")){
            mIdcSwitch.setChecked(false);
        }
    }
    private void setFlip(String flip) {
        if(flip.equals("ON")){
            mFlipSwitch.setChecked(true);
        }else if(flip.equals("OFF")){
            mFlipSwitch.setChecked(false);
        }
    }

    private void setAutoOffScreen(String autoOffScreen) {
        if(autoOffScreen.equals("ON")){
            mAutoOffScreenSwitch.setChecked(true);
        }else if(autoOffScreen.equals("OFF")){
            mAutoOffScreenSwitch.setChecked(false);
        }
    }

    private void setDvs(String state) {
        if (mProductType.equals(ConnectIP.mSportDv)) {
            if ("ON".equals(state)) {
                mDvsSwitch.setChecked(true);
            } else if ("OFF".endsWith(state)) {
                mDvsSwitch.setChecked(false);
            }
        }
    }

    private void setPip(String state) {
        if ("ON".equals(state)) {
            mPipSwitch.setChecked(true);
        } else if ("OFF".endsWith(state)) {
            mPipSwitch.setChecked(false);
        }
    }

    private void setTimelapseRecord(String timelapse) {
        if (timelapse.equals("OFF")) {
            mTimelapseRecordChoice.setText(string_close);
        } else if(timelapse.equals("1")) {
            mTimelapseRecordChoice.setText(string_space_1s);
        } else if(timelapse.equals("5")) {
            mTimelapseRecordChoice.setText(string_space_5s);
        } else if(timelapse.equals("10")) {
            mTimelapseRecordChoice.setText(string_space_10s);
        } else if(timelapse.equals("30")) {
            mTimelapseRecordChoice.setText(string_space_30s);
        } else if(timelapse.equals("60")) {
            mTimelapseRecordChoice.setText(string_space_60s);
        }
    }

    private void setCollisionDetect(String collision) {
        if (collision.equals("CLOSE")) {
            mCollisionDetectChoice.setText(string_close);
        } else if (collision.equals("L")) {
            mCollisionDetectChoice.setText(string_low);
        } else if(collision.equals("M")) {
            mCollisionDetectChoice.setText(string_middle);
        } else if(collision.equals("H")) {
            mCollisionDetectChoice.setText(string_hight);
        }
    }

    private void setVideoQuality(String videoQuality) {
        if (videoQuality.equals("LOW")) {
            mVideoQualityChoice.setText(string_low);
        } else if(videoQuality.equals("MID")) {
            mVideoQualityChoice.setText(string_middle);
        } else if(videoQuality.equals("HIGH")) {
            mVideoQualityChoice.setText(string_hight);
        }
    }

    private void setVideolength(String videolength) {
        if (videolength.equals("1min")) {
            mVideoLengthChoice.setText(string_1min);
        } else if(videolength.equals("3min")) {
            mVideoLengthChoice.setText(string_3min);
        } else if(videolength.equals("5min")) {
            mVideoLengthChoice.setText(string_5min);
        }
    }

    private void setLanguage(String language) {
        if (language.equals("English")) {
            mLanguageChoice.setText(string_english);
        } else if(language.equals("Chinese")) {
            mLanguageChoice.setText(string_chinese);
        }
    }

    private void setBright(String bright) {
        if (bright.equals("low")) {
            mBrightChoice.setText(string_low);
        } else if(bright.equals("middle")) {
            mBrightChoice.setText(string_middle);
        } else if(bright.equals("hight")) {
            mBrightChoice.setText(string_hight);
        }
    }

    private void setWhiteBalance(String whiteBalance) {
        if (whiteBalance.equals("auto")) {
            mWhiteBalanceChoice.setText(string_auto);
        } else if(whiteBalance.equals("Daylight")) {
            mWhiteBalanceChoice.setText(string_daylight);
        } else if(whiteBalance.equals("fluocrescence")) {
            mWhiteBalanceChoice.setText(string_fluocrescence);
        } else if(whiteBalance.equals("cloudysky")) {
            mWhiteBalanceChoice.setText(string_cloudysky);
        } else if(whiteBalance.equals("tungsten")) {
            mWhiteBalanceChoice.setText(string_tungsten);
        }
    }

    private void dealResolutionMsg(String msg, int index, final String sendMsg) {
        String[] temp = msg.split(":");
        final String[] cam_item = new String[temp.length-1];
        for(int i = 0;i<cam_item.length;i++){
            cam_item[i] = temp[i+1];    //1720-960;1920-1080;2440*1440
        }
        if(ConnectIP.IP != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(false)
                    .setPositiveButton(string_finish, null)
                    .setNegativeButton(string_no, null);

            builder.setSingleChoiceItems(cam_item, index, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSocketService.sendMsg(sendMsg + (i + 1), false);
                }
            });

            builder.create().show();
        } else {
            connectBuilder();
        }
    }

    private void dealFrontCamMsg(String msg){
        if (mProductType.equals(ConnectIP.mCvr)) {
            dealResolutionMsg(msg, mFrontCameraIndex, "CMD_ARGSETTINGFront_camera:front");
        }
    }

    private void dealBackCamMsg(String msg){
        if (mProductType.equals(ConnectIP.mCvr)) {
            dealResolutionMsg(msg, mBackCameraIndex, "CMD_ARGSETTINGBack_camera:back");
        }
    }

    private void dealVideoResolutionMsg(String msg){    //tiantian，替换成正确的协议
        if (mProductType.equals(ConnectIP.mSportDv)) {
            dealResolutionMsg(msg, mVideoResolutionIndex, "CMD_ARGSETTINGFront_camera:front");
        }
    }

    private void dealPhotoResolutionMsg(String msg){
        if (mProductType.equals(ConnectIP.mSportDv)) {
            dealResolutionMsg(msg, mPhotoResolutionIndex, "CMD_ARGSETTINGPhotoQuality:");
        }
    }

    private void dealDvsCalibration(String msg) {
        if (msg.equals("START")) {
            mDialog.show();

            mDialogTv = (TextView) mDialog.findViewById(R.id.tv_dialog);
            mDialogTv.setText(R.string.gyrosensor_msg);

            mDialogPositiveBtn = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            mDialogPositiveBtn.setEnabled(false);
        } else if (msg.equals("SUCCESS")) {
            mDialogTv.setText(R.string.Success);
            mDialogPositiveBtn.setEnabled(true);
        } else if (msg.equals("FAULT")) {
            mDialogTv.setText(R.string.Error);
            mDialogPositiveBtn.setEnabled(true);
        }
    }

    private void dealFormatMsg(String msg){
        if(msg.startsWith("CMD_GET_ACK_FORMAT_STATUS_IDLE")){
            final AlertDialog.Builder format_builder = new AlertDialog.Builder(getActivity());
            format_builder.setMessage(getActivity().getString(R.string.format_msg))
                    .setNegativeButton(string_no, null);

            format_builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSocketService.sendMsg("CMD_ARGSETTINGFormat:Format", false);
                }
            });
            format_builder.create().show();
        } else if (msg.startsWith("CMD_GET_ACK_FORMAT_STATUS_BUSY")) {
            AlertDialog.Builder format_builder = new AlertDialog.Builder(getActivity());
            format_builder.setMessage(getActivity().getString(R.string.formating_msg))
                    .setNegativeButton(getActivity().getString(R.string.close), null);
            format_builder.create().show();
        } else if (msg.startsWith("CMD_GET_ACK_FORMAT_STATUS_ERR")){
            AlertDialog.Builder format_builder = new AlertDialog.Builder(getActivity());
            format_builder.setMessage(getActivity().getString(R.string.format_fail_msg))
                 .setPositiveButton(string_finish, null)
                 .create().show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.front_camera:
                mSocketService.sendMsg("CMD_GET_FRONT_SETTING_RESPLUTION", false);
                mSocketService.sendMsg("CMD_GET_FRONT_CAMERARESPLUTION", true);
                break;

            case R.id.back_camera:
                mSocketService.sendMsg("CMD_GET_BACK_SETTING_RESPLUTION", false);
                mSocketService.sendMsg("CMD_GET_BACK_CAMERARESPLUTION", true);
                break;

            case R.id.video_resolution:
                mSocketService.sendMsg("CMD_GET_FRONT_SETTING_RESPLUTION", false);
                mSocketService.sendMsg("CMD_GET_FRONT_CAMERARESPLUTION", true);
                break;

            case R.id.photo_resolution:
                mSocketService.sendMsg("CMD_GET_SETTING_PHOTO_QUALITY", false);
                mSocketService.sendMsg("CMD_GET_PHOTO_QUALITY", true);
                break;

            case R.id.licence:
                licenceClick();
                break;

            case R.id.timelapse_record:
                timelapseRecordClick();
                break;

            case R.id.video_quality:
                videoQualityClick();
                break;

            case R.id.collision_detect:
                collisionDetectClick();
                break;

            case R.id.white_balance:
                whiteBalanceClick();
                break;

            case R.id.exposure:
                exposureClick();
                break;

            case R.id.language:
                languageClick();
                break;

            case R.id.frequency:
                frequencyClick();
                break;

            case R.id.bright:
                brightClick();
                break;

            case R.id.format:
                mSocketService.sendMsg("CMD_GET_FORMAT_STATUS", false);
                break;

            case R.id.date_set:
                mDateTimeDialog.hideOrShow();
                break;

            case R.id.recovery:
                recoveryClick();
                break;

            case R.id.firmware_update:
                firmwareUpdateClick();
                break;

            case R.id.connect_set:
                Activity activity = getActivity();
                if(activity instanceof MainActivity) {
                    ((MainActivity) activity).clickTabCameraConnectLayout();
                }
                break;

            case R.id.videolength:
                videoLengthClick();
                break;

            case R.id.recordmode:
                recordModeClick();
                break;

            case R.id.debug_test:
                Activity activity1 = getActivity();
                if(activity1 instanceof MainActivity) {
                    ((MainActivity) activity1).clickTabCameraDebugLayout();
                }
                break;

            case R.id.gyrosensor_calibration:
                mSocketService.sendMsg("CMD_ARGSETTINGCalibration", false);

            default:
                break;
        }
    }

    private void whiteBalanceClick() {
        final String[] item = {string_auto, string_daylight, string_fluocrescence,
                string_cloudysky, string_tungsten};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (mWhiteBalanceChoice.getText().toString().equals(string_auto)) {
            mWhiteBalanceIndex = 0;
        } else if (mWhiteBalanceChoice.getText().toString().equals(string_daylight)) {
            mWhiteBalanceIndex = 1;
        } else if (mWhiteBalanceChoice.getText().toString().equals(string_fluocrescence)) {
            mWhiteBalanceIndex = 2;
        } else if (mWhiteBalanceChoice.getText().toString().equals(string_cloudysky)) {
            mWhiteBalanceIndex = 3;
        } else if (mWhiteBalanceChoice.getText().toString().equals(string_tungsten)) {
            mWhiteBalanceIndex = 4;
        }

        builder.setSingleChoiceItems(item, mWhiteBalanceIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGWhiteBalance:auto", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGWhiteBalance:Daylight", false);
                } else if(which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGWhiteBalance:fluocrescence", false);
                }else if(which == 3){
                    mSocketService.sendMsg("CMD_ARGSETTINGWhiteBalance:cloudysky", false);
                }else if(which == 4){
                    mSocketService.sendMsg("CMD_ARGSETTINGWhiteBalance:tungsten", false);
                }
            }
        });

        builder.setPositiveButton(string_finish, null);
        builder.create().show();
    }

    private void exposureClick() {
        final String[] exposure_item = {"-3","-2","-1","0","1"};

        AlertDialog.Builder exposure_builder = new AlertDialog.Builder(getActivity());
        if(mExposureChoice.getText().toString().equals("-3")){
            mExposureIndex = 0;
        }else if(mExposureChoice.getText().toString().equals("-2")){
            mExposureIndex=1;
        }else if(mExposureChoice.getText().toString().equals("-1")){
            mExposureIndex=2;
        }else if(mExposureChoice.getText().toString().equals("0")){
            mExposureIndex=3;
        }else if(mExposureChoice.getText().toString().equals("1")){
            mExposureIndex=4;
        }

        exposure_builder.setSingleChoiceItems(exposure_item, mExposureIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGExposure:-3", false);
                }else if(i == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGExposure:-2", false);
                }else if(i == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGExposure:-1", false);
                }else if(i == 3){
                    mSocketService.sendMsg("CMD_ARGSETTINGExposure:0", false);
                }else if(i == 4){
                    mSocketService.sendMsg("CMD_ARGSETTINGExposure:1", false);
                }
            }
        });

        exposure_builder.setPositiveButton(string_finish, null);
        exposure_builder.create().show();
    }

    private void languageClick() {
        final String[] language_item = {string_english, string_chinese};

        AlertDialog.Builder language_builder = new AlertDialog.Builder(getActivity());
        if(mLanguageChoice.getText().toString().equals(string_english)){
            mLanguageIndex= 0;
        }else if(mLanguageChoice.getText().toString().equals(string_chinese)){
            mLanguageIndex =1;
        }

        language_builder.setSingleChoiceItems(language_item, mLanguageIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGLanguage:English", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGLanguage:Chinese", false);
                }
            }
        });

        language_builder.setPositiveButton(string_finish, null);
        language_builder.create().show();
    }

    private void frequencyClick() {
        final String[] frequency_item = {"50HZ","60HZ"};

        AlertDialog.Builder frequency_builder = new AlertDialog.Builder(getActivity());
        if(mFrequencyChoice.getText().toString().equals("50HZ")){
            mFrequencyIndex = 0;
        }else if(mFrequencyChoice.getText().toString().equals("60HZ")){
            mFrequencyIndex=1;
        }

        frequency_builder.setSingleChoiceItems(frequency_item, mFrequencyIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGFrequency:50HZ", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGFrequency:60HZ", false);
                }
            }
        });

        frequency_builder.setPositiveButton(string_finish, null);
        frequency_builder.create().show();
    }

    private void brightClick() {
        final String[] bright_item = {string_low, string_middle, string_hight};

        AlertDialog.Builder bright_builder = new AlertDialog.Builder(getActivity());
        if(mBrightChoice .getText().toString().equals(string_low)){
            mBrightIndex=0;
        }else if(mBrightChoice.getText().toString().equals(string_middle)){
            mBrightIndex=1;
        }else if(mBrightChoice.getText().toString().equals(string_hight)){
            mBrightIndex=2;
        }

        bright_builder.setSingleChoiceItems(bright_item, mBrightIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGBright:low", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGBright:mid", false);
                }else if(which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGBright:high", false);
                }
            }
        });

        bright_builder.setPositiveButton(string_finish, null);
        bright_builder.create().show();
    }

    private void recoveryClick() {
        AlertDialog.Builder recovery_builder = new AlertDialog.Builder(getActivity());
        recovery_builder.setMessage(getActivity().getString(R.string.recovery_msg))
                .setNegativeButton(string_no, null);

        recovery_builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSocketService.sendMsg("CMD_ARGSETTINGRecovery", false);
                exitBuilder();
            }
        });
        recovery_builder.create().show();
    }

    private void firmwareUpdateClick() {
        AlertDialog.Builder firmware_builder = new AlertDialog.Builder(getActivity());

        if (mUpdateFirmware) {
            firmware_builder.setMessage(getActivity().getString(R.string.firmware_ota_update_msg));
            firmware_builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.d(TAG, "firmware_update...");
                }
            });
        } else {
            firmware_builder.setMessage(getActivity().getString(R.string.firmware_update_msg))
                    .setNegativeButton(string_no, null);

            firmware_builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.d(TAG, "firmware_update");
                    mUpdateFirmware = true;
                    mSocketService.sendMsg("CMD_OTA_Upload_FileCRC:"  + firmware_update_getcrc32(), false);
                }
            });
        }
        firmware_builder.create().show();
    }

    private void videoLengthClick() {
        final String[] videolength_item = {string_1min, string_3min, string_5min};

        AlertDialog.Builder videolength_builder = new AlertDialog.Builder(getActivity());
        if(mVideoLengthChoice.getText().toString().equals(string_1min)){
            mVideoLengthIndex = 0;
        }else if(mVideoLengthChoice.getText().toString().equals(string_3min)){
            mVideoLengthIndex = 1;
        }else if(mVideoLengthChoice.getText().toString().equals(string_5min)){
            mVideoLengthIndex = 2;
        }

        videolength_builder.setSingleChoiceItems(videolength_item, mVideoLengthIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGVideolength:1min", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGVideolength:3min", false);
                }else if(which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGVideolength:5min", false);
                }
            }
        });

        videolength_builder.setPositiveButton(string_finish, null);
        videolength_builder.create().show();
    }

    private void recordModeClick() {
        final String[] recordmode_item = {string_front, string_rear,string_double};
        if(ConnectIP.IP != null){
            AlertDialog.Builder recordmode_builder = new AlertDialog.Builder(getActivity());
            if(mRecordModeChoice.getText().toString().equals(string_front)){
                mRecordModeIndex = 0;
            }else if(mRecordModeChoice.getText().toString().equals(string_rear)){
                mRecordModeIndex = 1;
            }else if(mRecordModeChoice.getText().toString().equals(string_double)){
                mRecordModeIndex = 2;
            }
            recordmode_builder.setSingleChoiceItems(recordmode_item, mRecordModeIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    if(which == 0){
                        mSocketService.sendMsg("CMD_ARGSETTINGRecordMode:Front", false);
                    }else if (which == 1){
                        mSocketService.sendMsg("CMD_ARGSETTINGRecordMode:Rear", false);
                    }else if(which == 2){
                        mSocketService.sendMsg("CMD_ARGSETTINGRecordMode:Double", false);
                    }
                }
            });
            recordmode_builder.setPositiveButton(string_finish, null);
            recordmode_builder.create().show();
        }else {
            connectBuilder();
        }
    }

    private void timelapseRecordClick() {
        final String[] item = {string_close, string_space_1s, string_space_5s, string_space_10s,
                string_space_30s, string_space_60s};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (mTimelapseRecordChoice.getText().toString().equals(string_close)) {
            mTimelapseRecordIndex = 0;
        } else if (mTimelapseRecordChoice.getText().toString().equals(string_space_1s)) {
            mTimelapseRecordIndex = 1;
        } else if (mTimelapseRecordChoice.getText().toString().equals(string_space_5s)) {
            mTimelapseRecordIndex = 2;
        } else if (mTimelapseRecordChoice.getText().toString().equals(string_space_10s)) {
            mTimelapseRecordIndex = 3;
        } else if (mTimelapseRecordChoice.getText().toString().equals(string_space_30s)) {
            mTimelapseRecordIndex = 4;
        } else if (mTimelapseRecordChoice.getText().toString().equals(string_space_60s)) {
            mTimelapseRecordIndex = 5;
        }

        builder.setSingleChoiceItems(item, mTimelapseRecordIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:OFF", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:1", false);
                }else if(which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:5", false);
                }else if(which == 3){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:10", false);
                }else if(which == 4){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:30", false);
                }else if(which == 5){
                    mSocketService.sendMsg("CMD_ARGSETTINGTimeLapse:60", false);
                }
            }
        });

        builder.setNegativeButton(string_no, null);
        builder.setPositiveButton(string_finish, null);
        builder.create().show();
    }

    private void videoQualityClick() {
        final String[] bright_item = {string_low, string_middle, string_hight};

        AlertDialog.Builder bright_builder = new AlertDialog.Builder(getActivity());
        if(mVideoQualityChoice .getText().toString().equals(string_low)){
            mVideoQualityIndex=0;
        }else if(mVideoQualityChoice.getText().toString().equals(string_middle)){
            mVideoQualityIndex=1;
        }else if(mVideoQualityChoice.getText().toString().equals(string_hight)){
            mVideoQualityIndex=2;
        }

        bright_builder.setSingleChoiceItems(bright_item, mVideoQualityIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGQuality:LOW", false);
                }else if (which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGQuality:MID", false);
                }else if(which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGQuality:HIGH", false);
                }
            }
        });

        bright_builder.setPositiveButton(string_finish, null);
        bright_builder.create().show();
    }

    private void collisionDetectClick() {
        final String[] bright_item = {string_close, string_low, string_middle, string_hight};

        AlertDialog.Builder bright_builder = new AlertDialog.Builder(getActivity());
        if(mCollisionDetectChoice .getText().toString().equals(string_close)){
            mCollisionDetectIndex=0;
        }else if(mCollisionDetectChoice .getText().toString().equals(string_low)){
            mCollisionDetectIndex=1;
        }else if(mCollisionDetectChoice.getText().toString().equals(string_middle)){
            mCollisionDetectIndex=2;
        }else if(mCollisionDetectChoice.getText().toString().equals(string_hight)){
            mCollisionDetectIndex=3;
        }

        bright_builder.setSingleChoiceItems(bright_item, mCollisionDetectIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which == 0){
                    mSocketService.sendMsg("CMD_ARGSETTINGCollision:CLOSE", false);
                }else if(which == 1){
                    mSocketService.sendMsg("CMD_ARGSETTINGCollision:LOW", false);
                }else if (which == 2){
                    mSocketService.sendMsg("CMD_ARGSETTINGCollision:MID", false);
                }else if(which == 3){
                    mSocketService.sendMsg("CMD_ARGSETTINGCollision:HIGH", false);
                }
            }
        });

        bright_builder.setPositiveButton(string_finish, null);
        bright_builder.create().show();
    }

    private void licenceClick() {
        final EditText editText = new EditText(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.licence_hint))
                .setNegativeButton(string_no, null)
                .setView(editText);

        builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String str = null;
                String licence_plate = editText.getText().toString();
                if (licence_plate != null) {
                    mSocketService.sendMsg("CMD_ARGSETTINGLicencePlate:" + licence_plate, false);
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(!compoundButton.isPressed()){
            return;
        } else {
            if (ConnectIP.IP == null) {
                connectBuilder();
            } else {
                switch (compoundButton.getId()) {
                    case R.id.car_model_change:
                        dealSwitchCheckedChanged(b, "......:ON", "......:OFF");
                        break;

                    case R.id.loop_record_change:
                        dealSwitchCheckedChanged(b, "......:ON", "......:OFF");
                        break;

                    case R.id.dvs_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGDVS:ON", "CMD_ARGSETTINGDVS:OFF");
                        break;

                    case R.id.key_sound_change:
                        dealSwitchCheckedChanged(b, "......:ON", "......:OFF");
                        break;

                    case R.id.idc_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGIDC:ON", "CMD_ARGSETTINGIDC:OFF");
                        break;

                    case R.id.flip_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGFLIP:ON", "CMD_ARGSETTINGFLIP:OFF");
                        break;

                    case R.id.auto_off_screen_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGAutoOffScreen:ON", "CMD_ARGSETTINGAutoOffScreen:OFF");
                        break;

                    case R.id.auto_shutdown_change:
                        dealSwitchCheckedChanged(b, "......:ON", "......:OFF");
                        break;

                    case R.id.screensaver_change:
                        dealSwitchCheckedChanged(b, "......:ON", "......:OFF");
                        break;

                    case R.id.date_stamp_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGDataStamp:ON", "CMD_ARGSETTINGDataStamp:OFF");
                        break;

                    case R.id.record_audio_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGRecordAudio:ON", "CMD_ARGSETTINGRecordAudio:OFF");
                        break;

                    case R.id.boot_record_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGBootRecord:ON", "CMD_ARGSETTINGBootRecord:OFF");
                        break;

                    case R.id.weqw_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTING3DNR:ON", "CMD_ARGSETTING3DNR:OFF");
                        break;

                    case R.id.adas_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGADAS:ON", "CMD_ARGSETTINGADAS:OFF");
                        break;

                    case R.id.motion_detection_change:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGMotionDetection:ON", "CMD_ARGSETTINGMotionDetection:OFF");
                        break;
                    case R.id.pip_switch:
                        dealSwitchCheckedChanged(b, "CMD_ARGSETTINGPIP:ON", "CMD_ARGSETTINGPIP:OFF");
                        break;
                }
            }
        }
    }

    private void dealSwitchCheckedChanged(boolean isChecked, String onMsg, String offMsg) {
        if(isChecked)
            mSocketService.sendMsg(onMsg, false);
        else
            mSocketService.sendMsg(offMsg, false);
    }

    @Override
    public void onDateSet(Date date) {
        //mDateSetupChoice.setText(mSimpleDateFormat.format(date) + "");
        String datetime = mSimpleDateFormat.format(date);
        mSocketService.sendMsg("CMD_ARGSETTINGDateSet:" + datetime, false);
    }

    private void exitBuilder() {
        AlertDialog.Builder exit_builder = new AlertDialog.Builder(getActivity());
        exit_builder.setMessage(getActivity().getString(R.string.exit_apk_msg));
        exit_builder.setPositiveButton(R.string.exit_msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        });
        exit_builder.setNegativeButton(R.string.resend_msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSocketService.sendMsg("CMD_ARGSETTINGRecovery", false);
                exitBuilder();
            }
        });
        //exit_builder.setCancelable(false);
        exit_builder.create().show();
    }

    private void connectBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.connect_confirm_msg));

        builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent =  new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });
        builder.create().show();
    }
}