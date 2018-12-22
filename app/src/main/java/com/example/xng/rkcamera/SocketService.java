package com.example.xng.rkcamera;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.example.xng.rkcamera.Map.gps.GpsInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class SocketService{
    static final String TAG = "SocketService";

    public static final String ACTION_DOWNLOAD_FRAGMENT = "ACTION_DOWNLOAD_FRAGMENT";
    public static final String ACTION_IPCAMERA_SETTING = "ACTION_IPCAMERA_SETTING";
    public static final String ACTION_VIDEO_PLAYER_ACTIVITY = "ACTION_VIDEO_PLAYER_ACTIVITY";
    public static final String ACTION_CONNECT_SETTING = "ACTION_CONNECT_SETTING";
    public static final String ACTION_IPCAMERA_DEBUG = "ACTION_IPCAMERA_DEBUG";
    public static final String ACTION_GPS_FILE_LIST = "ACTION_GPS_FILE_LIST";
    //public static final String ACTION_CAMERA_FRAGMENT = "ACTION_CAMERA_FRAGMENT";
    //public static final String ACTION_VIDEO_PLAY = "ACTION_VIDEO_PLAY";

    private Context mContext = null;
    private String mOwner;
    private ArrayList<String> mGpsFileList = new ArrayList<String>();

    private Socket mSocket = null;
    private InputStream mReader = null;
    private OutputStream mWriter = null;
    private boolean mReadDone = false;
    private String mLock = new String("lock");

    private static volatile SocketService mInstance = null;

    public static SocketService getInstance() {
        if(mInstance == null) {
            synchronized(SocketService.class) {
                if(mInstance == null) {
                    mInstance = new SocketService();
                }
            }
        }

        return mInstance;
    }

    public void setContext(Context context) {
        if (mContext == null)
            mContext = context;
    }

    public void clearGpsFileList() {
        mGpsFileList.clear();
    }

    public void setGpsOwner(String owner) {
        mOwner = owner;
    }

    public String getGpsOwner() {
        Log.d(TAG, "mOwner: " + mOwner);
        return mOwner;
    }

    protected SocketService () {
        Log.d(TAG, "SocketService, new socket");
        connect();
    }

    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    if(ConnectIP.IP == null){
                        //connectBuilder();
                        Log.d(TAG, "ConnectIP.IP == null");
                    } else {
                        if (mSocket == null){
                            mSocket = new Socket(ConnectIP.IP, 8888);
                            mReader = mSocket.getInputStream();
                            mWriter = mSocket.getOutputStream();
                            mReadDone = true;
                            revMsg();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void connectBuilder() {
        Looper.prepare();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getString(R.string.connect_confirm_msg));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent =  new Intent(Settings.ACTION_WIFI_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        builder.setCancelable(false);
        builder.create().show();
        Looper.loop();
    }

    private void exitBuilder() {
        mContext.getMainLooper().prepare();
        //Looper.prepare();
        AlertDialog.Builder exit_builder = new AlertDialog.Builder(mContext);
        exit_builder.setMessage(mContext.getString(R.string.wifi_disconnected_hint));
        exit_builder.setPositiveButton(R.string.restart_msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeSocket();
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(intent);

                // 杀掉进程
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });

        exit_builder.setCancelable(false);
        exit_builder.create().show();
        mContext.getMainLooper().loop();
        //Looper.loop();
    }

    private void dealMsg(String msg) {
        //Log.d(TAG, "dealMsg: " + msg);
        Intent intent = new Intent();

        if (msg.startsWith(GpsInfo.CMD_ACK_GET_GPS_LIST)) {
            dealGpsListInfo(msg);
        } else {
            if (msg.startsWith(VideoPlayerActivity.CMD_RECORD_BUSY)
                    || msg.startsWith(VideoPlayerActivity.CMD_RECORD_IDLE)
                    || msg.startsWith(VideoPlayerActivity.CMD_CB_STARTREC)
                    || msg.startsWith(VideoPlayerActivity.CMD_CB_STOPREC)
                    || msg.startsWith(VideoPlayerActivity.CMD_CB_NO_SDCARD)
                    || msg.startsWith(VideoPlayerActivity.CMD_CB_GET_MODE)
                    || msg.startsWith(VideoPlayerActivity.CMD_CB_GPS_UPDATA)) {
                Log.d(TAG, "sendBroadcast to  VideoPlayerActivity");
                intent.setAction(ACTION_VIDEO_PLAYER_ACTIVITY);
            } else if (msg.startsWith(DownloadFragment.CMD_ACK_GETCAMFILE_FINISH)
                    || msg.startsWith(DownloadFragment.CMD_DELSUCCESS)
                    || msg.startsWith(DownloadFragment.CMD_DELFAULT)
                    || msg.startsWith(DownloadFragment.CMD_GETCAMFILENAME)
                    || msg.startsWith(DownloadFragment.CMD_CB_GETCAMFILENAME)
                    || msg.startsWith(DownloadFragment.CMD_CB_DELETE)) {
                Log.d(TAG, "sendBroadcast to DownloadFragment");
                intent.setAction(ACTION_DOWNLOAD_FRAGMENT);
            } else if (msg.startsWith(ConnectSetting.CMD_WIFI_INFOWIFINAME)) {
                Log.d(TAG, "sendBroadcast to  ConnectSetting");
                intent.setAction(ACTION_CONNECT_SETTING);
            } else if (msg.startsWith(IpCameraDebug.CMD_ACK_GET_DEBUG)) {
                Log.d(TAG, "sendBroadcast to IpCameraDebug");
                intent.setAction(ACTION_IPCAMERA_DEBUG);
            } else {
                //Log.d(TAG, "sendBroadcast to IpCameraSetting");
                intent.setAction(ACTION_IPCAMERA_SETTING);
            }

            intent.putExtra("msg", msg);
            mContext.sendBroadcast(intent);
        }
    }

    private void dealGpsListInfo(String msg) {
        String[] tmp = msg.split(":");
        if (tmp[0].equals(GpsInfo.CMD_ACK_GET_GPS_LIST)) {
            mGpsFileList.add(tmp[1]);
            sendMsg("CMD_NEXTFILE", false);
            Log.d(TAG, "gps " + mGpsFileList.size() +" file name: " + tmp[1]);
        } else if (tmp[0].equals(GpsInfo.CMD_ACK_GET_GPS_LIST_END)) {
            Intent intent = new Intent(ACTION_GPS_FILE_LIST);
            intent.putExtra("mapGpsFile", tmp[1]);
            intent.putExtra("gpsFileList", mGpsFileList);
            mContext.sendBroadcast(intent);
        }
    }

    private void revMsg() {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[] mbyte = new byte[1024];
                    int readSize = -1;
                    String info = null;

                    while (mReadDone) {
                        readSize = mReader.read(mbyte);
                        if (readSize > 0) {
                            if (mbyte[readSize - 2] == 13 && mbyte[readSize - 1] == 10) //去除gps数据的\r\n
                                readSize -= 2;

                            byte[] msg = new byte[readSize];
                            System.arraycopy(mbyte, 0, msg, 0, readSize);
                            info = new String(msg, "GB2312");
                            Log.d(TAG, "revMsg: " +info);
                            //Log.d(TAG, "readSize: " + readSize);

                            dealMsg(info);
                            readSize = -1;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    exitBuilder();
                } finally {
                    Log.d(TAG, "exit receive thread");
                }
            }
        }.start();
    }

    public void sendMsg(final String msg, boolean sleep) {
        if (mWriter != null)
            new sendThread(msg, sleep).start();
    }

    private class sendThread extends Thread {
        private String msg;
        private boolean sleep;

        public sendThread(String msg, boolean sleep) {
            this.msg = msg;
            this.sleep = sleep;
        }

        public void run() {
            if (sleep) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //synchronized (mLock) {
                try {
                    Log.d(TAG, "send:" + msg);
                    mWriter.write(msg.getBytes("GB2312"));
                    mWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            //}
        }
    }

    public void closeSocket() {
        Log.d(TAG, "closeSocket");
        mReadDone = false;
        try {
            if (mWriter != null){
                mWriter.close();
                mWriter = null;
            }

            if (mReader != null){
                mReader.close();
                mReader = null;
            }

            if (mSocket != null){
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}