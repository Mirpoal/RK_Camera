package com.example.xng.rkcamera;

import java.io.Serializable;

public class DownLoadModel implements Serializable{
    static final String TAG = "DownLoadModel";

    public static final int VIDEO = 0;
    public static final int PHOTO = 1;

    public static final int TYPE_NOCHECKED = 0;
    public static final int TYPE_CHECKED = 1;
    public static final int TYPE_OPEN = 2;

    private String mDownLoadProgress = null;

    private String mFileName;
    private String mXmlFileName;
    private String mThumbnailName;
    private int mFileType;

    private String mDownloadSize;
    private String mDownloadDay;
    private String mDownloadTime;
    private String mDownloadType;
    private String mDownloadForm;

    private String mFileUrl;
    private String mThumbnailUrl;

    private boolean mIsLoading;
    private boolean mFlag;

    private int mFinished;
    private int mId;
    private int mType;

    private String mMapGpsStoragePath = null;

    public DownLoadModel(int type, int id, String directory, int finished,
                         String fileName, String downloadSize,String downloadDay,
                         String downloadTime, String downloadType,String downloadForm) {
        super();
        mDownloadSize = downloadSize;
        mDownloadDay = downloadDay;
        mDownloadTime = downloadTime;
        mDownloadType = downloadType;
        mDownloadForm = downloadForm;
        mId = id;
        mFinished = finished;
        mType = type;
        mIsLoading = false;

        mFileName = fileName;
        String tmp = mFileName.substring(0, mFileName.lastIndexOf("."));
        if (mFileName.endsWith(".MP4") || mFileName.endsWith(".mp4")) {//mp4
            mThumbnailName = tmp + ".jpg";
            mFileType = VIDEO;
        } else {   //jpg
            mThumbnailName = mFileName;
            mFileType = PHOTO;
        }

        mXmlFileName = tmp + ".xml";

        String[] directoryField = directory.split("&");
        mFileUrl = "http://" + ConnectIP.IP + "/" + directoryField[0] + "/" + mFileName;
        //Log.d(TAG, "fileUrl: " + mFileUrl);
        mThumbnailUrl = "http://" + ConnectIP.IP + "/" + directoryField[1] + "/" + mThumbnailName;
        //Log.d(TAG, "thumbUrl: " + mThumbnailUrl);
    }

    public String getFileName() {
        return mFileName;
    }
    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public String getXmlFileName() {
        return mXmlFileName;
    }
    public void setXmlFileName(String xmlFileName) {
        mXmlFileName = xmlFileName;
    }

    public String getDownloadSize() {
        return mDownloadSize;
    }
    public void setDownloadSize(String downloadSize) {
        mDownloadSize = downloadSize;
    }

    public String getDownloadDay() {
        return mDownloadDay;
    }
    public void setDownloadDay(String downloadDay) {
        mDownloadDay = downloadDay;
    }

    public String getDownloadTime() {
        return mDownloadTime;
    }
    public void setDownloadTime(String downloadTime) {
        mDownloadTime = downloadTime;
    }

    public int getId() {
        return mId;
    }
    public void setId(int id) {
        mId = id;
    }

    public int getFileType() {
        return mFileType;
    }
    public void setFileType(int fileType) {
        mFileType = fileType;
    }

    public String getFileUrl() {
        return mFileUrl;
    }
    public void setFileUrl(String url) {
        mFileUrl = url;
    }

    public String getDownLoadProgress() {
        return mDownLoadProgress;
    }
    public void setDownLoadProgress(String progress) {
        mDownLoadProgress = progress;
    }

    public String getThumbUrl() {
        return mThumbnailUrl;
    }
    public void setThumbnailUrl(String url) {
        mThumbnailUrl = url;
    }

    public boolean getLoadStatus() {
        return mIsLoading;
    }
    public void setLoadStatus(boolean status) {
        mIsLoading = status;
    }

    public int getFinished() {
        return mFinished;
    }
    public void setFinished(int finished) {
        mFinished = finished;
    }

    public boolean getFlag() {
        return mFlag;
    }
    public void setFlag(boolean flag)
    {
        mFlag = flag;
    }

    public String getDownloadType() {
        return mDownloadType;
    }
    public void setDownloadType(String type) {
        mDownloadType = type;
    }

    public String getDownloadForm() {
        return mDownloadForm;
    }
    public void setDownloadForm(String form) {
        mDownloadForm = form;
    }

    public int getType() {
        return mType;
    }
    public void setType(int type) {
        mType = type;
    }

    public String getMapGpsStoragePath() {
        return mMapGpsStoragePath;
    }
    public void setMapGpsStoragePath(String path) {
        mMapGpsStoragePath = path;
    }

    @Override
    public String toString() {
        return "Set [fileName=" + mFileName + ", downloadSize=" + mDownloadSize + ", downloadDay=" + mDownloadDay
                + ", downloadTime="+ mDownloadTime + "]" + ", Id="+ mId + "]";
    }
}
