package com.example.xng.rkcamera;

import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.VideoView;

/**
 * Created by Xng on 2016/8/4.
 */
public class IpcameraModel {
    private String cameraname;
    private int mdownloadbtn,mdelbtn;
    private int videoView;
    private ImageButton msettingbtn;



    public IpcameraModel(String cameraname) {
        this.cameraname = cameraname;
    }

    public String getCameraname() {return cameraname;}

    public void setCameraname(String cameraname) {
        this.cameraname = cameraname;
    }

    public ImageButton getMsettingbtn() {
        return msettingbtn;
    }

    public void setMsettingbtn(ImageButton msettingbtn) {
        this.msettingbtn = msettingbtn;
    }

    public int getMdownloadbtn(){
        return mdownloadbtn;
    }
    public void setMdownloadbtn(int mdownloadbtn){
        this.mdownloadbtn=mdownloadbtn;
    }

    public int getMdelbtn(){
        return mdelbtn;
    }
    public void setMdelbtn(int mdelbtn){
        this.mdelbtn=mdelbtn;
    }

    public int getVideoView(){
        return videoView;
    }

    public void setVideoView(int videoView){
        this.videoView=videoView;
    }
}
