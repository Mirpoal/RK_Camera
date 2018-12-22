package com.example.xng.rkcamera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by Xng on 2016/12/8.
 */
public class IpCameraDebug extends Fragment implements View.OnClickListener{
    public static final String CMD_ACK_GET_DEBUG = "CMD_ACK_GET_DEBUG";

    private static final String TAG = "IpCameraDebug";

    private Switch mreboot_change,mrecovery_change,mawake_change,mstandby_change,mmode_change,mvideo_change,mdeg_end_video_change,
            mphoto_change,mtemp_control_change;
    private TextView mvideo_bit_tv;
    private LinearLayout mvideo_bit_linearlayout;

    private final int MSG_DEBUG_FINISH = 20;
    private int video_bit_index = 0;

    String connect_confirm_msg = null;
    String string_yes = null;

    private SocketService mSocketService = SocketService.getInstance();

    private Handler debugHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_DEBUG_FINISH:
                    debugMsg(msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SocketService.ACTION_IPCAMERA_DEBUG);
        getActivity().registerReceiver(mReceiver, filter);
        mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ipcamera_debug, container,
                false);

        mreboot_change = (Switch)view.findViewById(R.id.debug_reboot_change);
        mrecovery_change = (Switch)view.findViewById(R.id.debug_recover_change);
        mawake_change = (Switch)view.findViewById(R.id.debug_awake_change);
        mstandby_change = (Switch)view.findViewById(R.id.debug_standby_change);
        mmode_change = (Switch)view.findViewById(R.id.debug_mod_change);
        mvideo_change = (Switch)view.findViewById(R.id.debug_video_change);
        mdeg_end_video_change = (Switch)view.findViewById(R.id.begin_end_video);
        mphoto_change = (Switch)view.findViewById(R.id.debug_photo_change);
        mtemp_control_change = (Switch)view.findViewById(R.id.debug_temp_control_change);

        mvideo_bit_tv = (TextView)view.findViewById(R.id.video_bit_choice);

        mvideo_bit_linearlayout = (LinearLayout)view.findViewById(R.id.video_bit_rate_per_pixel);
        mvideo_bit_linearlayout.setOnClickListener(this);

        connect_confirm_msg = getActivity().getString(R.string.connect_confirm_msg);
        string_yes = getActivity().getString(R.string.yes);

        mreboot_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_reboot:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_reboot:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mrecovery_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_recovery:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_recovery:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mawake_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_awake:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_awake:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mstandby_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_standby:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_standby:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mmode_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_mode_change:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_mode_change:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mvideo_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_video:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_video:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mdeg_end_video_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_beg_end_video:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_beg_end_video:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mphoto_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_photo:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_photo:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        mtemp_control_change.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!compoundButton.isPressed()){
                    return;
                }else {
                    if(b){
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_temp_control:on", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }else {
                        if(ConnectIP.IP == null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(connect_confirm_msg);
                            builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                        }else {
                            mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_temp_control:off", false);
                            mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", true);
                        }
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    public void refresh(){
        mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", false);
    }

    @Override
    public void onClick(View view) {
         switch (view.getId()){
             case R.id.video_bit_rate_per_pixel:
                 final String[] video_bit_item = {"1","2","4","6","8","10","12"};
                 if(ConnectIP.IP!=null){
                     AlertDialog.Builder video_bit_builder = new AlertDialog.Builder(getActivity());
                     if(mvideo_bit_tv.getText().toString().equals("1")){
                         video_bit_index = 0;
                     }else if(mvideo_bit_tv.getText().toString().equals("2")){
                         video_bit_index=1;
                     }else if(mvideo_bit_tv.getText().toString().equals("4")){
                         video_bit_index=2;
                     }else if(mvideo_bit_tv.getText().toString().equals("6")){
                         video_bit_index=3;
                     }else if(mvideo_bit_tv.getText().toString().equals("8")){
                         video_bit_index=4;
                     }else if(mvideo_bit_tv.getText().toString().equals("10")){
                         video_bit_index=5;
                     }else if(mvideo_bit_tv.getText().toString().equals("12")){
                         video_bit_index=6;
                     }
                     video_bit_builder.setSingleChoiceItems(video_bit_item, video_bit_index, new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialogInterface, int i) {
                             if(i == 0){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:1", false);
                             }else if(i == 1){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:2", false);
                             }else if(i == 2){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:4", false);
                             }else if(i == 3){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:6", false);
                             }else if(i == 4){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:8", false);
                             }
                             else if(i == 5){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:10", false);
                             }
                             else if(i == 6){
                                 video_bit_index = i;
                                 mSocketService.sendMsg("CMD_DEBUG_ARGSETTINGdebug_bit_rate_per_pixel:12", false);
                             }
                         }
                     });
                     video_bit_builder.setPositiveButton(getActivity().getString(R.string.finish), new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialogInterface, int i) {
                             mSocketService.sendMsg("CMD_GET_DEBUG_ARGSETTING", false);
                         }
                     });
                     video_bit_builder.create().show();
                 }else {
                     AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                     builder.setMessage(connect_confirm_msg);
                     builder.setPositiveButton(string_yes, new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                         }
                     });
                     //添加AlertDialog.Builder对象的setNegativeButton()方法
                     builder.create().show();
                 }
                 break;
         }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (SocketService.ACTION_IPCAMERA_DEBUG.equals(intent.getAction())){
                String info = intent.getStringExtra("msg");
                Message msg = new Message();
                msg.what = MSG_DEBUG_FINISH;
                msg.obj = info;
                debugHandler.sendMessage(msg);
            }
        }
    };

    private void debugMsg(String s){
        if(s.startsWith(CMD_ACK_GET_DEBUG)){
            String[] temp = s.split("reboot:");
            String[] temp1 = temp[1].split("recovery:");
            String reboot = temp1[0];
            String[] temp2 = temp1[1].split("awake:");
            String recovery = temp2[0];
            String[] temp3 = temp2[1].split("standby:");
            String awake = temp3[0];
            String[] temp4 = temp3[1].split("mode_change:");
            String standby = temp4[0];
            String[] temp5 = temp4[1].split("debug_video:");
            String mode_change = temp5[0];
            String[] temp6 = temp5[1].split("begin_end_video:");
            String debug_video = temp6[0];
            String[] temp7 = temp6[1].split("photo:");
            String begin_end_video = temp7[0];
            String[] temp8 = temp7[1].split("temp_control:");
            String photo = temp8[0];
            String[] temp9 = temp8[1].split("temp_video_bit_rate_per_pixel:");
            String temp_control = temp9[0];
            String temp_video_bit_rate_per_pixel = temp9[1];

            if(reboot.equals("on")){
                mreboot_change.setChecked(true);
            }else if(reboot.equals("off")){
                mreboot_change.setChecked(false);
            }

            if(recovery.equals("on")){
                mrecovery_change.setChecked(true);
            }else if(reboot.equals("off")){
                mrecovery_change.setChecked(false);
            }

            if(awake.equals("on")){
                mawake_change.setChecked(true);
            }else if(awake.equals("off")){
                mawake_change.setChecked(false);
            }

            if(standby.equals("on")){
                mstandby_change.setChecked(true);
            }else if(standby.equals("off")){
                mstandby_change.setChecked(false);
            }

            if(mode_change.equals("on")){
                mmode_change.setChecked(true);
            }else if(mode_change.equals("off")){
                mmode_change.setChecked(false);
            }

            if(debug_video.equals("on")){
                mvideo_change.setChecked(true);
            }else if(debug_video.equals("off")){
                mvideo_change.setChecked(false);
            }

            if(begin_end_video.equals("on")){
                mdeg_end_video_change.setChecked(true);
            }else if(begin_end_video.equals("off")){
                mdeg_end_video_change.setChecked(false);
            }

            if(photo.equals("on")){
                mphoto_change.setChecked(true);
            }else if(photo.equals("off")){
                mphoto_change.setChecked(false);
            }

            if(temp_control.equals("on")){
                mtemp_control_change.setChecked(true);
            }else if(temp_control.equals("off")){
                mtemp_control_change.setChecked(false);
            }

            if(temp_video_bit_rate_per_pixel.equals("1")){
                mvideo_bit_tv.setText("1");
            }else if(temp_video_bit_rate_per_pixel.equals("1")){
                mvideo_bit_tv.setText("1");
            }else if(temp_video_bit_rate_per_pixel.equals("2")){
                mvideo_bit_tv.setText("2");
            }else if(temp_video_bit_rate_per_pixel.equals("4")){
                mvideo_bit_tv.setText("4");
            }else if(temp_video_bit_rate_per_pixel.equals("6")){
                mvideo_bit_tv.setText("6");
            }else if(temp_video_bit_rate_per_pixel.equals("8")){
                mvideo_bit_tv.setText("8");
            }else if(temp_video_bit_rate_per_pixel.equals("10")){
                mvideo_bit_tv.setText("10");
            }else if(temp_video_bit_rate_per_pixel.equals("12")){
                mvideo_bit_tv.setText("12");
            }
        }
    }
}
