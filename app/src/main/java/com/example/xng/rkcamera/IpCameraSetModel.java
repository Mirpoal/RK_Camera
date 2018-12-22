package com.example.xng.rkcamera;

import android.widget.Switch;

/**
 * Created by Xng on 2016/8/23.
 */
public class IpCameraSetModel {
    private String title;
    private int setimage;

    public IpCameraSetModel(String title,int setimage) {
        super();
        this.title = title;
        this.setimage = setimage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSetimage(){return  setimage;}

    public void setSetimage(int setimage){this.setimage = setimage;}

}

