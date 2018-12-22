package com.example.xng.rkcamera;

/**
 * Created by Xng on 2016/8/4.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 相机的碎片页面
 * @author fbn
 *
 */
public class CameraFragment extends Fragment {
    static final String TAG = "CameraFragment";

    private Context context;
    private View mView;
    private ListView listView;
    private IpcameraAdapter ipcameraAdapter;
    private List<String> deviceList = new ArrayList<String>();
    private Button mGoSetting= null;

    private SocketService mSocketService = null;

    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // 假数据
//        String  model = new String("RK_CVR");
//        deviceList.add(model);
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
/*
    public void receive(final String msg){
        new Thread() {
            @Override
            public void run() {
                deviceList.add(msg);
                Log.d(TAG,"mdeviceList:" + deviceList);
                ipcameraAdapter.setDevices(deviceList);
                ipcameraAdapter.notifyDataSetChanged();
            }
        }.start();
    }*/
/**/
    public void receive(final String msg){
        getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (String str : deviceList) {
                            if (str.equals(msg))
                                return;
                        }

                        deviceList.add(msg);
                        Log.d(TAG,"mdeviceList:"+deviceList);
                        ipcameraAdapter.setDevices(deviceList);
                        ipcameraAdapter.notifyDataSetChanged();
                    }
                }
        );
    }
/**/

    public List<String> getDeviceList()
    {
        return deviceList;
    }

    public void setBackground()
    {
        mView.setBackgroundResource(R.drawable.settings);
        mGoSetting.setVisibility(View.VISIBLE);
    }

    public void clear(){
        deviceList.clear();
        ipcameraAdapter.notifyDataSetChanged();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.camera_fragment, container,
                false);
        listView=(ListView)mView.findViewById(R.id.lvMember);

        ipcameraAdapter = new IpcameraAdapter(getActivity(),deviceList);
        Log.d(TAG,"getIP:"+ipcameraAdapter);

        listView.setAdapter(ipcameraAdapter);

        mView.setBackgroundResource(R.drawable.settings);
        mGoSetting = (Button)mView.findViewById(R.id.go_setting);
        mGoSetting.setVisibility(View.VISIBLE);
        mGoSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.mAlreadLocalInit = false;
                Intent intent =  new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });

        return mView;
    }

    public class IpcameraAdapter extends BaseAdapter {
        private Context context;
        private List<String> list;

        public IpcameraAdapter(Context context)
        {
            super();
            this.context = context;
        }

        public IpcameraAdapter(Context context,List<String> list)
        {
            super();
            this.context = context;
            this.list=list;
        }

        public void setDevices(List<String> list){
            this.list=list;
        }

        @Override
        public int getCount()
        {
            if (list != null) {
                return list.size();
            }
            return 0;
        }

        @Override
        public String getItem(int position)
        {
            if (list != null) {
                return list.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            final ViewHolder viewHolder;

            if (list.size() <= 0) {
                mGoSetting.setVisibility(View.VISIBLE);
                mView.setBackgroundResource(R.drawable.settings);
            } else {
                mGoSetting.setVisibility(View.GONE);
                mView.setBackgroundResource(0);
            }

            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.ipcamera_item, null);
                viewHolder.videoView = (ImageView) convertView.findViewById(R.id.videoShow);
                viewHolder.mcamera = (ImageView) convertView.findViewById(R.id.image_camera);
                viewHolder.onlinePlay = (ImageView)convertView.findViewById(R.id.online_play);
                viewHolder.cameraname = (TextView) convertView.findViewById(R.id.cameraname);
                viewHolder.msettingbtn = (ImageButton) convertView.findViewById(R.id.image_setting_btn);
                viewHolder.mdownloadbtn = (ImageButton) convertView.findViewById(R.id.image_download_btn);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if(ConnectIP.mFirstStart) {
                try {
                    String path = Environment.getExternalStorageDirectory()+"/RkCamera/RkCache";
                    Bitmap mbitmap = BitmapUtils.getBitmap(path,"RK_CVR.jpg");
					Log.d(TAG, "mbitmap: " + mbitmap);
                    viewHolder.videoView.setImageBitmap(mbitmap);
                    notifyDataSetChanged();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //Log.d(TAG, "getView, not first star");
                viewHolder.videoView.setImageResource(R.drawable.img_nopic_01);
            }

            viewHolder.onlinePlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "ConnectIP.IP: " + ConnectIP.IP);
                    if (mSocketService == null)
                        mSocketService = SocketService.getInstance();

                    //mSocketService.sendMsg("CMD_RTP_TS_TRANS_START", false);    //rtp直播
                    //String url ="rtp://@:20000";

                    mSocketService.sendMsg("CMD_RTSP_TRANS_START", false);   //rtsp直播
                    String url = "rtsp://" + ConnectIP.IP + "/stream0";

                    if (!TextUtils.isEmpty(url)) {
                        startActivity(new Intent(getActivity(), VideoPlayerActivity.class).putExtra("url", url));
                    }
                }
            });
            viewHolder.cameraname.setText(list.get(position).toString());

            viewHolder.msettingbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //响应事件
                    Activity activity = getActivity();
                    if(activity instanceof MainActivity){
                        ((MainActivity) activity).clickTabCameraSetLayout();
                    }
                }
            });

            viewHolder.mdownloadbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity activity = getActivity();
                    if(activity instanceof MainActivity){
                        ((MainActivity) activity).clickTabDownLoadLayout();
                    }
                }
            });
            return convertView;
        }

        private class ViewHolder
        {
            ImageView videoView;
            ImageView mcamera,onlinePlay;
            TextView cameraname;
            ImageButton msettingbtn;
            ImageButton mdownloadbtn;
        }
    }
}