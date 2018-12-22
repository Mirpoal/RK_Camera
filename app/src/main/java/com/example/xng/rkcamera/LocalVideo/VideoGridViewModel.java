package com.example.xng.rkcamera.LocalVideo;

import java.io.Serializable;

/**
 * Created by Xng on 2016/11/16.
 */
public class VideoGridViewModel implements Serializable{
    private String path;
    private boolean flag;

    public VideoGridViewModel(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }
    public void setPath() {
        this.path = path;
    }

    public boolean getFlag() {
        return flag;
    }
    public void setFlag(boolean flag) {this.flag = flag;}
}
