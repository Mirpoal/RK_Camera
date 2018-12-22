package com.example.xng.rkcamera;

/**
 * Created by Xng on 2016/8/8.
 */
public class AlbumModel {
    private String path;
    private String file;
    private boolean flag;

    public AlbumModel() {
        super();
    }

    public AlbumModel(int image) {
        super();
    }

    public AlbumModel(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getFlag() {
        return flag;
    }
    public void setFlag(boolean flag) {this.flag = flag;}

}
