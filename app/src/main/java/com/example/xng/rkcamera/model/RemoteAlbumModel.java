package com.example.xng.rkcamera.model;

import com.example.xng.rkcamera.adapter.RemoteAlbumAdapter;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumModel {
    private String path;
    private String parentName;
    private String displayName;
    private String fileSize;
    private int ItemType = RemoteAlbumAdapter.TYPE_ITEM;

    public RemoteAlbumModel(int itemType) {
        this.ItemType = itemType;
    }

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

    public int getItemType() {
        return ItemType;
    }

    public void setItemType(int itemType) {
        ItemType = itemType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
}
