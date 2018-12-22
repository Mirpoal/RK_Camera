package com.example.xng.rkcamera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xng.rkcamera.LocalVideo.VideoGridViewFragment;
import com.example.xng.rkcamera.Map.gps.GpsFileDownloadThread;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainActivity extends BaseActivity implements OnClickListener, LocalSocketClient.LocalSocketListener {
    static final String TAG = "MainActivity";

    public static final String IPCAMERA_SETTING_DISABLE_UI = "IPCAMERASETTING_DISABLE_UI";
    public static final String IPCAMERA_SETTING_ENABLE_UI = "IPCAMERASETTING_ENABLE_UI";
    public static final String DOWNLOAD_FRAMENT_DISABLE_UI = "DOWNLOAD_FRAMENT_DISABLE_UI";
    public static final String DOWNLOAD_FRAMENT_ENABLE_UI = "DOWNLOAD_FRAMENT_ENABLE_UI";

    // 三个tab布局
    private RelativeLayout settingLayout, cameraLayout, albumLayout,delete,download;

    // 底部标签切换的Fragment
    private CameraFragment cameraFragment = null;
    private IpCameraSetting ipCameraSetting = null;
    private DownloadFragment downloadFragment = null;
    private AlbumGridFragment albumGridFragment = null;
    private VideoGridViewFragment videoGridViewFragment = null;
    private IpCameraDebug ipCameraDebug = null;
    private ConnectSetting connectSetting = null;
    private Fragment settingFragment = null, albumFragment = null,
            remoteAlbumFragment, currentFragment = null;
    // 底部标签图片
    private ImageView settingImg = null, cameraImg = null, albumImg = null;
    // 底部标签的文本
    private TextView settingTv = null, cameraTv = null, albumTv = null;
    private Button mbackBtn = null, mrefreshBtn = null, mbtnEdit = null,mbtnSelect = null,
            mbtnSelectAll = null, mCancelBtn = null, mbtnUnSelectAll = null;
    private int mType = -1;//1:设置 2：相机 3：相册 4 ：相册内页 5:视频内页 6:相机设置 7:连接设置 8:下载页面 10:远程文件管理页面
    private boolean mChange = false, mChange_video = false, mChange_ipcamset = false,
            mChange_dowenload = false, mChange_contset = false, listenStatus = false;
    private int UDP_PORT_SEND = 18889;
    private int UDP_PORT_RECEIVE = 18888;
    private DatagramSocket udpSendSocket = null;

    private long exitTime = 0;
    private boolean mIsDelFileUI = false, mIsSelectAllUI = false;
    // private CameraFragment mcamerafragment = new CameraFragment();

    private SocketService mSocketService = null;
    private boolean mNeedLocalSocket;
    public static boolean mAlreadLocalInit;
    private LocalSocketClient mLocalSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //startService(new Intent(this, SocketService.class));

        IntentFilter filter = new IntentFilter();
        filter.addAction(IPCAMERA_SETTING_ENABLE_UI);
        filter.addAction(IPCAMERA_SETTING_DISABLE_UI);
        filter.addAction(DOWNLOAD_FRAMENT_ENABLE_UI);
        filter.addAction(DOWNLOAD_FRAMENT_DISABLE_UI);
        registerReceiver(mReceiver, filter);

        initUI();
        initTab();
        creatPath();
        mNeedLocalSocket = getResources().getBoolean(R.bool.refresh_with_local_socket);
        if(mNeedLocalSocket) {
            mAlreadLocalInit = false;
        } else {
            sendUdp();
        }

        SettingFragment.deleteAllFiles(new File(GpsFileDownloadThread.mOnlineStoragePath));
        //mSocketService = SocketService.getInstance();
        //mSocketService.setContext(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "-----onResume");
        if (cameraFragment != null) {
            cameraFragment.clear();
            cameraFragment.setBackground();
            if(mNeedLocalSocket){
                if(null == mLocalSocket){
                    mLocalSocket = new LocalSocketClient(this);
                    mLocalSocket.startSocketServer();
                } else if(!mAlreadLocalInit) {
                    mLocalSocket.sendMsgInThread(LocalSocketClient.SEND_START);
                } else {
                    sendUdp();
                }
            } else {
                sendUdp();
            }
        }
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        unregisterReceiver(mReceiver);

        if(mSocketService != null)
            mSocketService.closeSocket();

        SettingFragment.deleteAllFiles(new File(GpsFileDownloadThread.mOnlineStoragePath));

        if(null != mLocalSocket){
            mLocalSocket.stopSocketServer();
            mLocalSocket = null;
        }

        Log.d(TAG, "System.exit(0)");
        System.exit(0);
    }

    private void downloadFramentDisableUI() {
        if (mbackBtn.isShown()) {
            mIsDelFileUI = false;
            mbtnSelect.setVisibility(View.GONE);
            mbackBtn.setVisibility(View.GONE);
        } else if (mCancelBtn.isShown()) {
            mIsDelFileUI = true;
            if(mbtnSelectAll.isShown()) {
                mIsSelectAllUI = true;
                mbtnSelectAll.setVisibility(View.GONE);
            } else {
                mIsSelectAllUI = false;
                mbtnUnSelectAll.setVisibility(View.GONE);
            }

            mCancelBtn.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
            download.setVisibility(View.GONE);
        }
    }

    private void downloadFramentEnableUI() {
        if (!mIsDelFileUI) {
            mbtnSelect.setVisibility(View.VISIBLE);
            mbackBtn.setVisibility(View.VISIBLE);
        } else {
            mCancelBtn.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
            download.setVisibility(View.VISIBLE);

            if (mIsSelectAllUI)
                mbtnSelectAll.setVisibility(View.VISIBLE);
            else
                mbtnUnSelectAll.setVisibility(View.VISIBLE);
        }
    }

    private void ipCameraSettingDisableUI() {
        settingLayout.setEnabled(false);
        cameraLayout.setEnabled(false);
        albumLayout.setEnabled(false);
        mbackBtn.setEnabled(false);
    }

    private void ipCameraSettingEnableUI() {
        settingLayout.setEnabled(true);
        cameraLayout.setEnabled(true);
        albumLayout.setEnabled(true);
        mbackBtn.setEnabled(true);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Log.d("tiantian", "onReceive, intent.getAction(): " + intent.getAction());
            if (IPCAMERA_SETTING_DISABLE_UI.equals(intent.getAction()))
                ipCameraSettingDisableUI();
            else if (IPCAMERA_SETTING_ENABLE_UI.equals(intent.getAction()))
                ipCameraSettingEnableUI();
            else if (DOWNLOAD_FRAMENT_DISABLE_UI.equals(intent.getAction()))
                downloadFramentDisableUI();
            else if (DOWNLOAD_FRAMENT_ENABLE_UI.equals(intent.getAction()))
                downloadFramentEnableUI();
        }
    };

    /**
     * 初始化UI
     */
    private void initUI() {
        settingLayout = (RelativeLayout) findViewById(R.id.rl_setting);
        cameraLayout = (RelativeLayout) findViewById(R.id.rl_camera);
        albumLayout = (RelativeLayout) findViewById(R.id.rl_album);
        delete = (RelativeLayout) findViewById(R.id.rl_delete);
        download = (RelativeLayout) findViewById(R.id.rl_download);

        mbackBtn = (Button) findViewById(R.id.main_back);
        mrefreshBtn = (Button) findViewById(R.id.main_refresh);
        mbtnEdit = (Button) findViewById(R.id.btn_edit);
        mbtnSelect = (Button) findViewById(R.id.btn_select);
        mbtnSelectAll = (Button) findViewById(R.id.btn_selectall);
        mCancelBtn = (Button)findViewById(R.id.main_cancle);
        mbtnUnSelectAll = (Button)findViewById(R.id.btn_unselectall);

        settingLayout.setOnClickListener(this);
        cameraLayout.setOnClickListener(this);
        albumLayout.setOnClickListener(this);
        delete.setOnClickListener(this);
        download.setOnClickListener(this);

        mbackBtn.setOnClickListener(this);
        mrefreshBtn.setOnClickListener(this);
        mbtnEdit.setOnClickListener(this);
        mbtnSelect.setOnClickListener(this);
        mbtnSelectAll.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        mbtnUnSelectAll.setOnClickListener(this);

        settingImg = (ImageView) findViewById(R.id.iv_setting);
        cameraImg = (ImageView) findViewById(R.id.iv_camera);
        albumImg = (ImageView) findViewById(R.id.iv_album);
        settingTv = (TextView) findViewById(R.id.tv_setting);
        cameraTv = (TextView) findViewById(R.id.tv_camera);
        albumTv = (TextView) findViewById(R.id.tv_album);
        if(needShowRemoteAlbum()){
            albumTv.setText(R.string.main_remote);
        }
    }

    /**
     * 初始化底部标签
     */
    private void initTab() {
        if (cameraFragment == null) {
            cameraFragment = new CameraFragment();
        }

        if (!cameraFragment.isAdded()) {
            // 提交事务
            getSupportFragmentManager().beginTransaction().add(R.id.content_layout, cameraFragment).commit();
            // 记录当前Fragment
            currentFragment = cameraFragment;
            // 设置图片文本的变化
            cameraImg.setImageResource(R.drawable.icon_home_02);
            cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
            settingImg.setImageResource(R.drawable.icon_set_01);
            settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
            albumImg.setImageResource(R.drawable.icon_local_01);
            albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        }

    }

    @Override
    public void onClick(View view) {
        if(10 == mType && view.getId() != R.id.rl_album && null != mLocalSocket){
            mLocalSocket.sendMsgInThread(LocalSocketClient.SEND_NFS_UMOUNT);
        }
        switch (view.getId()) {
            case R.id.rl_setting: // 设置
                delete.setVisibility(View.GONE);
                download.setVisibility(View.GONE);
                clickTabSetLayout();
                break;
            case R.id.rl_camera: // 相机
                delete.setVisibility(View.GONE);
                download.setVisibility(View.GONE);
                if(mChange_ipcamset){
                    clickTabCameraSetLayout();
                }
                else if(mChange_dowenload){
                    clickTabDownLoadLayout();
                }else {
                    clickTabMainLayout(false);
                }
                break;
            case R.id.rl_album: // 相册
                delete.setVisibility(View.GONE);
                download.setVisibility(View.GONE);

                if(needShowRemoteAlbum()){
                    clickTabRemoteAlbumLayout();
                    return;
                }

                if (mChange) {
                    clickTabLocalPhotoLayout();
                } else if(mChange_video){
                    clickTabLocalVideoLayout();
                }else {
                    clickTabLocalLayout(false);
                }
                break;
            case R.id.rl_delete://全选删除
                deleteClick();
                break;
            case R.id.rl_download://全选下载
                if(mType == 8){
                    downloadFragment.bulkDownload();
                    //mbtnUnSelectAll.setVisibility(View.GONE);
                    //mbtnSelectAll.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.main_back:
                if (mType == 4) {
                    clickTabLocalLayout(false);
                } else if (mType == 5) {
                    clickTabLocalLayout(false);
                }
                else if(mType == 6){
                    clickTabMainLayout(false);
                }
                else if(mType == 7){
                    clickTabCameraSetLayout();
                }
                else if(mType == 8){
                    //downloadFragment.close();
                    clickTabMainLayout(false);
                }else if(mType == 9){
                    clickTabCameraSetLayout();
                }
                break;
            case R.id.main_refresh://刷新连接socket,发送UDP广播包并接收广播包
                cameraFragment.clear();
                if(mNeedLocalSocket){
                    if(null != mLocalSocket){
                        mLocalSocket.sendMsgInThread(LocalSocketClient.SEND_START);
                    }
                } else {
                    sendUdp();
                }
                break;
            case R.id.btn_select:
                if(mType == 8){
                    ConnectIP.mSelect = true;
                    downloadFragment.reciveSelect(ConnectIP.mSelect);
                    mCancelBtn.setVisibility(View.VISIBLE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                    mbackBtn.setVisibility(View.GONE);
                    mbtnSelect.setVisibility(View.GONE);

                    delete.setVisibility(View.VISIBLE);
                    download.setVisibility(View.VISIBLE);
                    settingLayout.setVisibility(View.GONE);
                    cameraLayout.setVisibility(View.GONE);
                    albumLayout.setVisibility(View.GONE);
                }else if(mType == 4){
                    ConnectIP.mSelect = true;
                    albumGridFragment.reciveSelect(ConnectIP.mSelect);
                    mCancelBtn.setVisibility(View.VISIBLE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                    mbackBtn.setVisibility(View.GONE);
                    mbtnSelect.setVisibility(View.GONE);
                    delete.setVisibility(View.VISIBLE);
                    download.setVisibility(View.GONE);
                    settingLayout.setVisibility(View.GONE);
                    cameraLayout.setVisibility(View.GONE);
                    albumLayout.setVisibility(View.GONE);
                }else if(mType == 5){
                    ConnectIP.mSelect = true;
                    videoGridViewFragment.reciveSelect(ConnectIP.mSelect);
                    mCancelBtn.setVisibility(View.VISIBLE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                    mbackBtn.setVisibility(View.GONE);
                    mbtnSelect.setVisibility(View.GONE);
                    delete.setVisibility(View.VISIBLE);
                    download.setVisibility(View.GONE);
                    settingLayout.setVisibility(View.GONE);
                    cameraLayout.setVisibility(View.GONE);
                    albumLayout.setVisibility(View.GONE);
                }
                break;
            case R.id.main_cancle:
                if(mType == 8){
                    ConnectIP.mCancle = false;
                    ConnectIP.mSelect = false;
                    ConnectIP.mSelectAll = false;
                    ConnectIP.mUnSelectAll = false;
                    ConnectIP.mDeleteAll = false;
                    downloadFragment.reciveCancle(ConnectIP.mCancle);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mCancelBtn.setVisibility(View.GONE);
                    mbackBtn.setVisibility(View.VISIBLE);
                    mbtnSelect.setVisibility(View.VISIBLE);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    download.setVisibility(View.GONE);
                    settingLayout.setVisibility(View.VISIBLE);
                    cameraLayout.setVisibility(View.VISIBLE);
                    albumLayout.setVisibility(View.VISIBLE);
                }else if(mType ==4){
                    ConnectIP.mCancle = false;
                    ConnectIP.mSelect = false;
                    ConnectIP.mSelectAll = false;
                    ConnectIP.mUnSelectAll = false;
                    ConnectIP.mDeleteAll = false;
                    albumGridFragment.reciveCancle(ConnectIP.mCancle);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mCancelBtn.setVisibility(View.GONE);
                    mbackBtn.setVisibility(View.VISIBLE);
                    mbtnSelect.setVisibility(View.VISIBLE);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    download.setVisibility(View.GONE);
                    settingLayout.setVisibility(View.VISIBLE);
                    cameraLayout.setVisibility(View.VISIBLE);
                    albumLayout.setVisibility(View.VISIBLE);
                }else if(mType == 5){
                    ConnectIP.mCancle = false;
                    ConnectIP.mSelect = false;
                    ConnectIP.mSelectAll = false;
                    ConnectIP.mUnSelectAll = false;
                    ConnectIP.mDeleteAll = false;
                    videoGridViewFragment.reciveCancle(ConnectIP.mCancle);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mCancelBtn.setVisibility(View.GONE);
                    mbackBtn.setVisibility(View.VISIBLE);
                    mbtnSelect.setVisibility(View.VISIBLE);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    download.setVisibility(View.GONE);
                    settingLayout.setVisibility(View.VISIBLE);
                    cameraLayout.setVisibility(View.VISIBLE);
                    albumLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.btn_selectall:
                if(mType == 8){
                    ConnectIP.mSelectAll = true;
                    downloadFragment.reciveSelectAll(ConnectIP.mSelectAll);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mbtnUnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 4){
                    ConnectIP.mSelectAll = true;
                    albumGridFragment.reciveSelectAll(ConnectIP.mSelectAll);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mbtnUnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 5){
                    ConnectIP.mSelectAll = true;
                    videoGridViewFragment.reciveSelectAll(ConnectIP.mSelectAll);
                    mbtnSelectAll.setVisibility(View.GONE);
                    mbtnUnSelectAll.setVisibility(View.VISIBLE);
                }
                break;
            case  R.id.btn_unselectall:
                if(mType == 8){
                    ConnectIP.mUnSelectAll = false;
                    downloadFragment.reciveSelectAll(ConnectIP.mUnSelectAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 4){
                    ConnectIP.mUnSelectAll = false;
                    albumGridFragment.reciveSelectAll(ConnectIP.mUnSelectAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 5){
                    ConnectIP.mUnSelectAll = false;
                    videoGridViewFragment.reciveSelectAll(ConnectIP.mUnSelectAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }
                break;
            default:
                break;
        }
    }

    private void deleteClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_file_msg))
                .setNegativeButton(getString(R.string.no), null);

        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(mType == 8) {
                    ConnectIP.mDeleteAll = true;
                    downloadFragment.reciveDelete(ConnectIP.mDeleteAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 4){
                    ConnectIP.mDeleteAll = true;
                    albumGridFragment.reciveDelete(ConnectIP.mDeleteAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }else if(mType == 5){
                    ConnectIP.mDeleteAll = true;
                    videoGridViewFragment.reciveDelete(ConnectIP.mDeleteAll);
                    mbtnUnSelectAll.setVisibility(View.GONE);
                    mbtnSelectAll.setVisibility(View.VISIBLE);
                }
            }
        });

        builder.create().show();
    }

    //获取局域网的广播地址
    public static String getBroadcast() throws SocketException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if (interfaceAddress.getBroadcast() != null) {
                        return interfaceAddress.getBroadcast().toString().substring(1);
                    }
                }
            }
        }
        return null;
        //return "192.168.100.255"; //tiantian, okl
        //return "192.168.1.255";
    }

    //Udp发送线程
    public void sendUdp(){
        Log.d(TAG, "-----sendUdp");
        new Thread(){
            @Override
            public void run(){
                String mbroadcast = null;
                try {
                    mbroadcast = getBroadcast();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                Log.d(TAG,"getBroadcast:"+mbroadcast);
                InetAddress broadcastAddr = null;
                try {
                    broadcastAddr = InetAddress.getByName(mbroadcast);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                try {
//                    udpSendSocket = new DatagramSocket(UDP_PORT_RECEIVE);
                    if(udpSendSocket==null){
                        udpSendSocket = new DatagramSocket(null);
                        udpSendSocket.setReuseAddress(true);
                        udpSendSocket.bind(new InetSocketAddress(UDP_PORT_RECEIVE));
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                DatagramPacket sendPacket = new DatagramPacket("CMD_DISCOVER".getBytes(), "CMD_DISCOVER".getBytes().length, broadcastAddr,UDP_PORT_SEND);
                try {
                    Log.d(TAG, "udpSendSocket.send");
                    udpSendSocket.send(sendPacket);
                    recUdp();
//                    udpSendSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void recUdp(){
        Log.d(TAG, "recUdp");
        new Thread(){
            public void run(){
                byte data[] = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                try {
                    udpSendSocket.receive(receivePacket);

                    InetAddress ip = receivePacket.getAddress();
                    String mGetIp = ip.getHostAddress();
                    ConnectIP.IP = mGetIp;
                    Log.d(TAG,"GETIP:"+mGetIp);
                    Log.d(TAG,"ip:"+ip);

                    mSocketService = SocketService.getInstance();
                    mSocketService.setContext(MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String ss = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if(ss.endsWith("END")) {
                    String[] temp = ss.split("WIFINAME:");
                    String[] temp1 = temp[1].split("SID:");
                    String wifiname = temp1[0];
                    String[] temp2 = temp1[1].split("PRODUCT:");
                    String ssid = temp2[0];
                    ConnectIP.mProductType = temp2[1].substring(0,temp2[1].length()-3);
                    Log.d(TAG, "设备信息：WIFINAME:"+ wifiname + ", SSID：" + ssid + ", PRODUCT: " + ConnectIP.mProductType);
                    if (null != wifiname) {
                        cameraFragment.receive(wifiname);
                    }
                }
            }
        }.start();
    }

    /**
     * 点击第一个tab
     */
    private void clickTabSetLayout() {
        mType = 1;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.GONE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if (settingFragment == null) {
            settingFragment = new SettingFragment();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), settingFragment);

        // 设置底部tab变化
        cameraImg.setImageResource(R.drawable.icon_home_01);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        settingImg.setImageResource(R.drawable.icon_set_02);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    /**
     * 点击第二个tab
     */
    private void clickTabMainLayout(boolean second) {
        mType = 2;
        mChange_dowenload = false;
        mChange_ipcamset =false;
        mChange_contset =false;
        mrefreshBtn.setVisibility(View.VISIBLE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.GONE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if(second || null != ipCameraSetting){
//            ipCameraSetting = new IpCameraSetting();
            addOrShowFragment(getSupportFragmentManager().beginTransaction(), ipCameraSetting);
        } else if(second || null != downloadFragment){
//            downloadFragment = new DownloadFragment();
            addOrShowFragment(getSupportFragmentManager().beginTransaction(), downloadFragment);
        }
        if (cameraFragment == null) {
            cameraFragment = new CameraFragment();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), cameraFragment);
//
        cameraImg.setImageResource(R.drawable.icon_home_02);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    /**
     * `````````````
     */
    public void clickTabLocalLayout(boolean second) {
        mType = 3;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.GONE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        mChange = false;
        mChange_video = false;
        if(second || null != albumGridFragment){
            albumGridFragment = new AlbumGridFragment();
            addOrShowFragment(getSupportFragmentManager().beginTransaction(), albumGridFragment);
        }else if(second||null != videoGridViewFragment){
            videoGridViewFragment = new VideoGridViewFragment();
            addOrShowFragment(getSupportFragmentManager().beginTransaction(), videoGridViewFragment);
        }else {
            if (albumFragment == null) {
                albumFragment = new AlbumFragment();
            }
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), albumFragment);

        cameraImg.setImageResource(R.drawable.icon_home_01);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_02);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_press));

    }

    //本地相册
    public void clickTabLocalPhotoLayout() {
        mType = 4;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.VISIBLE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        mChange = true;
        if(null == albumGridFragment){
            albumGridFragment = new AlbumGridFragment();
        }else {
            albumGridFragment.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), albumGridFragment);
        cameraImg.setImageResource(R.drawable.icon_home_01);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_02);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
    }

    //本地视频
    public void  clickTabLocalVideoLayout(){
        mType= 5;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.VISIBLE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        mChange_video = true;
        if (null == videoGridViewFragment){
            videoGridViewFragment = new VideoGridViewFragment();
        }else {
            videoGridViewFragment.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), videoGridViewFragment);
        cameraImg.setImageResource(R.drawable.icon_home_01);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_02);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
    }

    //相机设置页面
    public void clickTabCameraSetLayout(){
        mType= 6;
        mChange_ipcamset = true;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if (null == ipCameraSetting){
            ipCameraSetting = new IpCameraSetting(ConnectIP.mProductType);
        }else {
            ipCameraSetting.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), ipCameraSetting);
        cameraImg.setImageResource(R.drawable.icon_home_02);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    //连接设置页面
    public void clickTabCameraConnectLayout(){
        mType= 7;
        mChange_contset = true;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if (null == connectSetting){
            connectSetting = new ConnectSetting();
        }else {
           connectSetting.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), connectSetting);
        cameraImg.setImageResource(R.drawable.icon_home_02);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    //下载列表页面
    public void clickTabDownLoadLayout(){
    	Log.d(TAG, "clickTabDownLoadLayout");
        mType= 8;
        mChange_dowenload = true;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.VISIBLE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if (null == downloadFragment){
            downloadFragment = new DownloadFragment();
        }else {
            downloadFragment.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), downloadFragment);
        cameraImg.setImageResource(R.drawable.icon_home_02);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    //相机Debug页面
    public void clickTabCameraDebugLayout(){
        mType= 9;
        mChange_contset = true;
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.VISIBLE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        if (null == ipCameraDebug){
            ipCameraDebug = new IpCameraDebug();
        }else {
            ipCameraDebug.refresh();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), ipCameraDebug);
        cameraImg.setImageResource(R.drawable.icon_home_02);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_press));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_01);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
    }

    //远程图片页面
    public void clickTabRemoteAlbumLayout() {
        mType = 10;
        if(mNeedLocalSocket && null != mLocalSocket) {
            mLocalSocket.sendMsgInThread(LocalSocketClient.SEND_NFS_MOUNT);
        }
        mrefreshBtn.setVisibility(View.GONE);
        mbtnEdit.setVisibility(View.GONE);
        mbtnSelect.setVisibility(View.GONE);
        mbackBtn.setVisibility(View.GONE);
        mCancelBtn.setVisibility(View.GONE);
        mbtnSelectAll.setVisibility(View.GONE);
        mbtnUnSelectAll.setVisibility(View.GONE);
        mChange = false;
        mChange_video = false;
        if (null == remoteAlbumFragment ) {
            remoteAlbumFragment = new RemoteAlbumFragment();
        }
        addOrShowFragment(getSupportFragmentManager().beginTransaction(), remoteAlbumFragment);

        cameraImg.setImageResource(R.drawable.icon_home_01);
        cameraTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        settingImg.setImageResource(R.drawable.icon_set_01);
        settingTv.setTextColor(getResources().getColor(R.color.bottomtab_normal));
        albumImg.setImageResource(R.drawable.icon_local_02);
        albumTv.setTextColor(getResources().getColor(R.color.bottomtab_press));

    }

    /**
     * 添加或者显示碎片
     *
     * @param transaction
     * @param fragment
     */
    private void addOrShowFragment(FragmentTransaction transaction,
                                   Fragment fragment) {
        if (currentFragment == fragment)
            return;

        if (!fragment.isAdded()) { // 如果当前fragment未被添加，则添加到Fragment管理器中
            transaction.hide(currentFragment)
                    .add(R.id.content_layout, fragment).commit();
        } else {
            transaction.hide(currentFragment).show(fragment).commit();
        }

        currentFragment = fragment;
    }
    //    public void receiver(String msg) {
//          if(null != cameraFragment){
//              cameraFragment.receiveMsg(msg);
//           }
//    }
    private void creatPath(){
        //文件夹目录"/sdcard/FirstFolder/SecondFolder"，多级目录必须逐一创建

        String FirstFolder="RkCamera";//一级目录

        String SecondFolder="RkPhoto";//二级目录

        String SecondFolder1="RkVideo";

        String SecondFolder2="RkCache";

     /*ALBUM_PATH取得机器的SD卡位置，File.separator为分隔符“/”*/

        String ALBUM_PATH= Environment.getExternalStorageDirectory()+File.separator+FirstFolder+File.separator;

        String Second_PATH=ALBUM_PATH+SecondFolder+ File.separator;
        String Second_PATH1=ALBUM_PATH+SecondFolder1+ File.separator;
        String Second_PATH2=ALBUM_PATH+SecondFolder2+ File.separator;
        //检查手机上是否有外部存储卡

        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if(!sdCardExist)

        {//如果不存在SD卡，进行提示

            Toast.makeText(MainActivity.this, this.getString(R.string.creat_folder_failed_msg), Toast.LENGTH_SHORT).show();

        }else{//如果存在SD卡，判断文件夹目录是否存在

            //一级目录和二级目录必须分开创建

            File dirFirstFile=new File(ALBUM_PATH);//新建一级主目录

            if(!dirFirstFile.exists()){//判断文件夹目录是否存在

                dirFirstFile.mkdir();//如果不存在则创建

            }

            File dirSecondFile=new File(Second_PATH);//新建二级主目录
            File dirSecondFile1=new File(Second_PATH1);//新建二级主目录
            File dirSecondFile2=new File(Second_PATH2);//新建二级主目录
            if(!dirSecondFile.exists()||!dirSecondFile1.exists()||!dirSecondFile2.exists()){//判断文件夹目录是否存在
                dirSecondFile.mkdir();//如果不存在则创建
                dirSecondFile1.mkdir();
                dirSecondFile2.mkdir();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(getApplicationContext(), this.getString(R.string.press_again_exit_msg), Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                Log.d(TAG, "onKeyDown");
                finish();
                //System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean needShowRemoteAlbum(){
        String remotePath = getResources().getString(R.string.remote_album_path);
        if(!TextUtils.isEmpty(remotePath)){
            return true;
        }
        return false;
    }

    @Override
    public void receiverLocalMsg(String msg) {
        if(LocalSocketClient.RECEIVE_START_OK.equals(msg)){
            mAlreadLocalInit = true;
            sendUdp();
        }
    }
}
