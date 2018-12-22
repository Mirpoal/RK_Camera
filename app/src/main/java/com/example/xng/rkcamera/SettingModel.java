package com.example.xng.rkcamera;

/**
 * Created by Xng on 2016/8/12.
 */
public class SettingModel {
    private int image;
    private int setimage;
    private int title;

    public SettingModel(int image, int title, int setimage) {
        super();
        this.image = image;
        this.title = title;
        this.setimage = setimage;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(int title) {
        this.title = title;
    }

    public int getSetimage(){return  setimage;}

    public void getSetimage(int setimage){this.setimage = setimage;}

}
