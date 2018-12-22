package com.example.xng.rkcamera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Xng on 2016/8/24.
 */
public class ConnectSetting extends Fragment implements View.OnClickListener{
    public static final String CMD_WIFI_INFOWIFINAME = "CMD_WIFI_INFOWIFINAME";

    static final String TAG = "ConnectSetting";

    private TextView tvChangeMod;
    private EditText tvApName, etApPassword, etStaName, etStaPassword;
    private int modchange_index = 0 ;
    private ProgressBar connectpb;
    private View view;

    private LinearLayout mModChange;
    private final int MSG_LOAD_FINISH = 10;

    private String mApMode = null;
    private String mStaMode = null;

    private SocketService mSocketService = SocketService.getInstance();

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_LOAD_FINISH:
                    dealMsg(msg.obj.toString());
                    connectpb.setVisibility(View.GONE);
                    break;
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SocketService.ACTION_CONNECT_SETTING);
        getActivity().registerReceiver(mReceiver, filter);
        mSocketService.sendMsg("CMD_GETWIFI", false);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, final ViewGroup container,
                              Bundle savedInstanceState){
        view = inflater.inflate(R.layout.connect_setting, container,
                false);
        tvApName = (EditText) view.findViewById(R.id.ap_name);
        etApPassword = (EditText) view.findViewById(R.id.ap_password);
        etStaName = (EditText) view.findViewById(R.id.sta_name);
        etStaPassword = (EditText) view.findViewById(R.id.sta_password);
        mModChange = (LinearLayout)view.findViewById(R.id.mod_change);
        mModChange.setOnClickListener(this);
        tvChangeMod = (TextView)view.findViewById(R.id.change_mod);
        connectpb = (ProgressBar) view.findViewById(R.id.connectpb);
        connectpb.setVisibility(View.VISIBLE);

        mApMode = getActivity().getString(R.string.ap_model);
        mStaMode = getActivity().getString(R.string.sta_model);

        setStaInfo();
        return view;
    }

    public void refresh(){
        mSocketService.sendMsg("CMD_GETWIFI", false);
    }

    private void setStaInfo() {
        String ssid = null;
        String password = null;
        try {
            WifiManager manager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            //拿到getWifiApConfiguration()方法
            Method method = manager.getClass().getDeclaredMethod("getWifiApConfiguration");
            //调用getWifiApConfiguration()方法，获取到 热点的WifiConfiguration
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(manager);
            ssid = configuration.SSID;
            password = configuration.preSharedKey;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "ssid: " + ssid);
        Log.d(TAG, "password: " + password);

        etStaName.setText(ssid);
        etStaPassword.setText(password);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (SocketService.ACTION_CONNECT_SETTING.equals(intent.getAction())){
                String info = intent.getStringExtra("msg");
                Message msg = new Message();
                msg.what = MSG_LOAD_FINISH;
                msg.obj = info;
                mHandler.sendMessage(msg);
                //Thread.sleep(1000); ???
            }
        }
    };

    private void dealMsg(String msg) {
        if (msg.startsWith(CMD_WIFI_INFOWIFINAME)) {
            String[] mInfo = msg.split("WIFIPASSWORD:");
            String[] a = mInfo[0].split(":");
            String ApName = a[1];
            Log.d(TAG, "ApName:" + ApName);
            String[] b = mInfo[1].split("MODE:");
            String ApPassword = b[0];
            Log.d(TAG, "ApPassword:" + ApPassword);
            String Mode = b[1].substring(0, b[1].length() - 3);
            Log.d(TAG, "MODE:" + Mode);
            tvApName.setText(ApName);
            etApPassword.setText(ApPassword);
            if (Mode.equals("AP")) {
                 tvChangeMod.setText(mApMode);
            }
            else if(Mode.equals("STA")){
                tvChangeMod.setText(mStaMode);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mHandler.removeMessages(MSG_LOAD_FINISH);
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mod_change:
                String[] mod_item = {mApMode, mStaMode};
                if(ConnectIP.IP != null){
                    final String ap_set_msg = getActivity().getString(R.string.ap_set_msg);
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    if(tvChangeMod.getText().toString().equals(mApMode)){
                        modchange_index = 0;
                    }else if(tvChangeMod.getText().toString().equals(mStaMode)){
                        modchange_index = 1;
                    }
                    builder.setSingleChoiceItems(mod_item, modchange_index, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface1, int i) {
                           if (i == 0) {
                               modchange_index = i;
                               dialogInterface1.dismiss();
                               if(tvApName.length() > 0 || etApPassword.length() >= 8){
                                   sendBuilder("CMD_SETWIFIWIFINAME:"+tvApName.getText().toString()+"PASSWD:"+etApPassword.getText().toString());
                               }else {
                                   Toast.makeText(getContext(), ap_set_msg, Toast.LENGTH_SHORT).show();
                               }
                           } else if (i == 1) {
                               modchange_index= i;
                               dialogInterface1.dismiss();
                               if (etStaName.length() > 0 || etStaPassword.length() >= 8) {
                                   sendBuilder("CMD_APPEARSSID:" + etStaName.getText().toString() + "PASSWD:" + etStaPassword.getText().toString());
                               }
                               else {
                                   Toast.makeText(getContext(), ap_set_msg, Toast.LENGTH_SHORT).show();
                               }
                           }
                        }
                    }).create().show();
                }
                break;
        }
    }

    private void sendBuilder(final String msg) {
        AlertDialog.Builder exit_builder = new AlertDialog.Builder(getActivity());
        exit_builder.setMessage(getActivity().getString(R.string.exit_hint));
        exit_builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSocketService.sendMsg(msg, false);
                exitBuilder(msg);
            }
        });
        exit_builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        exit_builder.create().show();
    }

    private void exitBuilder(final String msg) {
        AlertDialog.Builder exit_builder = new AlertDialog.Builder(getActivity());
        exit_builder.setMessage(getActivity().getString(R.string.exit_apk_msg));
        exit_builder.setPositiveButton(R.string.exit_msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //getActivity().finish();
                System.exit(0);
            }
        });
        exit_builder.setNegativeButton(R.string.resend_msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSocketService.sendMsg(msg, false);
                exitBuilder(msg);
            }
        });
        exit_builder.setCancelable(false);
        exit_builder.create().show();
    }
}
