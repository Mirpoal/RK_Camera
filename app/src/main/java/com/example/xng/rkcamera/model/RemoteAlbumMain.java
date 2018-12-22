package com.example.xng.rkcamera.model;

/**
 * Created by waha on 2017/8/7.
 */

public class RemoteAlbumMain {
    private String path;
    private String displayName;
    private int mediaNum;
    private String previewPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getMediaNum() {
        return mediaNum;
    }

    public void setMediaNum(int mediaNum) {
        this.mediaNum = mediaNum;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }
}
