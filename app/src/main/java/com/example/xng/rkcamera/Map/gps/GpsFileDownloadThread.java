package com.example.xng.rkcamera.Map.gps;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.xng.rkcamera.ConnectIP;
import com.example.xng.rkcamera.Map.MyMapView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GpsFileDownloadThread extends Thread {
    private String TAG = "GpsFileDownloadThread";

    public static String mLocalStoragePath = Environment.getExternalStorageDirectory() + "/RkCamera/RkGps";
    public static String mOnlineStoragePath = Environment.getExternalStorageDirectory() + "/RkCamera/RkGps/tempdata";
    private String mGpsFilePath = "http://" + ConnectIP.IP + "/" + ".MISC/.GPS/";

    private String mTimeStorageDir = null;
    private String mTimeStoragePath = null;
    private String mStoragePath = null;

    private int mMapPointId = -1;
    private int mStardPointId = -1;
    private String mVideoFileName = null;
    private String mMapGpsFile = null;
    private boolean mStop = false;
    private boolean mParseGpsFileList = false;
    private boolean mDownloadFile = false;

    private MyMapView mMapView = null;
    private Handler mHandler = null;
    private ArrayList<String> mGpsFileList;
    private ArrayList<GpsInfo> mGpsInfoList = new ArrayList<GpsInfo>();

    public GpsFileDownloadThread(Handler handler, String videoFileName, String mapGpsFile, ArrayList<String> gpsFileList,
                                 MyMapView mapView, boolean parseGpsFileList, boolean downloadFile, String storagePath) {
        mHandler = handler;
        mGpsFileList = gpsFileList;
        mVideoFileName = videoFileName;
        mMapGpsFile = mapGpsFile;
        mMapView = mapView;
        mParseGpsFileList = parseGpsFileList;
        mDownloadFile = downloadFile;
        mStoragePath = storagePath;

        Log.d(TAG, "mMapGpsFile: " + mMapGpsFile);
        Log.d(TAG, "mVideoFileName: " + mVideoFileName);
        Log.d(TAG, "mParseGpsFileList: " + mParseGpsFileList);
        Log.d(TAG, "mDownloadFile: " + mDownloadFile);
        Log.d(TAG, "mGpsFileList.size(): " + mGpsFileList.size());

        creatStoragePath(storagePath);
    }

    private void creatStoragePath(String storagePath) {
        String[] startTmp = mGpsFileList.get(0).split("\\.");
        String[] endTmp = mGpsFileList.get(mGpsFileList.size() - 1).split("\\.");

        mTimeStorageDir = startTmp[0] + "-" + endTmp[0];
        mTimeStoragePath = storagePath + File.separator + mTimeStorageDir;
        Log.d(TAG, "mTimeStoragePath: " + mTimeStoragePath);

        File dir = new File(mTimeStoragePath);
        if (!dir.exists())
            dir.mkdirs();
    }

    private void getStartPointId() {
        int hour = 0, videoTime = 0, gpsTime = 0;
        int i,  offset = -1, mapFilePosition = -1;

        //Log.d(TAG, "mGpsFileList.get(0): " + mGpsFileList.get(0));
        //Log.d(TAG, "mVideoFileName: " + mVideoFileName);
        if (mVideoFileName == null || mMapGpsFile == null)
            return;

        String[] videoTmp = mVideoFileName.split("_");
        String[] mapGpsTmp = mMapGpsFile.split("_");

        if (!videoTmp[0].equals(mapGpsTmp[0])) //年月日不相等，说明跨天，计算时差要加上24h
            hour = 24;

        videoTime = (Integer.parseInt(videoTmp[1].substring(0, 2)) + hour) * 3600 //s
                + Integer.parseInt(videoTmp[1].substring(2, 4)) * 60
                + Integer.parseInt(videoTmp[1].substring(4, 6)) ;
        gpsTime = (Integer.parseInt(mapGpsTmp[1].substring(0, 2))) * 3600 //s
                + Integer.parseInt(mapGpsTmp[1].substring(2, 4)) * 60
                + Integer.parseInt(mapGpsTmp[1].substring(4, 6)) ;
        offset = videoTime - gpsTime;

        for (i = 0; i < mGpsFileList.size(); i++) {
            if (mGpsFileList.get(i).equals(mMapGpsFile)) {
                mapFilePosition = i;
                break;
            }
        }
        mStardPointId = mapFilePosition * 60 + offset;

        Message msg = Message.obtain();
        msg.what = GpsInfo.MSG_GPS_START_POINT;
        msg.obj = mStardPointId;
        mHandler.sendMessage(msg);
    }

    @Override
    public void run() {
        if (mDownloadFile) {
            downloadGpsFileList();
        }

        if (mParseGpsFileList && !mStop)
            parseGpsFileList();

        if (!mStop) {
            Message msg = Message.obtain();
            msg.what = GpsInfo.MSG_GPS_FILE_DOWNLOAD_FINISH;
            msg.obj = mTimeStoragePath;
            mHandler.sendMessage(msg);
        }
    }

    public void stopDownload() {
        //Log.d(TAG, "stop");
        mStop = true;
    }

    private boolean copyFile(String gpsFileName, File newFile) {
        String path;
        if (mStoragePath.endsWith("RkGps"))
            path = mOnlineStoragePath;
        else
            path = mLocalStoragePath;

        File oldFile = new File(path + File.separator + mTimeStorageDir + File.separator + gpsFileName);
        if (oldFile.exists()) {
            int len = -1;
            try {
                Log.d(TAG, "oldFile is exists, copy: " + oldFile.getPath());
                //Log.d(TAG, "newFile: " + newFile.getPath());

                newFile.createNewFile();
                InputStream inStream = new FileInputStream(oldFile); //读入原文件
                FileOutputStream fs = new FileOutputStream(newFile);

                byte[] buffer = new byte[8192];
                while ((len = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, len);
                }

                inStream.close();
                fs.flush();
                fs.close();

                return true;
            } catch (Exception e) {
                Log.d(TAG, "copy file error!");
                e.printStackTrace();
            }
        }

        return false;
    }

    private void downloadGpsFileList() {
        try
        {
            for (String gpsFileName : mGpsFileList) {
                if (mStop) {
                    Log.d(TAG, "gps file download stop");
                    break;
                }

                File file = new File(mTimeStoragePath, gpsFileName);
                if (!file.exists()) {
                    if (!copyFile(gpsFileName, file)) {
                        Log.d(TAG, "download gps file: " + gpsFileName);
                        int len = -1;
                        byte buf[] = new byte[8192];

                        URL url = new URL(mGpsFilePath + gpsFileName);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setRequestMethod("GET");
                        //connection.setRequestProperty("Charset", "UTF-8");
                        //int fileLength = connection.getContentLength();
                        //Log.d(TAG, "fileLength: " + fileLength); //128KB

                        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                        InputStream inputStream = connection.getInputStream();
                        while ((len = inputStream.read(buf)) != -1) {
                            //Log.d(TAG, "down file len: " + len);
                            raf.write(buf, 0, len);
                        }
                        httpDeInit(connection, raf, inputStream);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseGpsFileList() {
        mMapPointId = -1;
        mGpsInfoList.clear();

        getStartPointId();

        for (String gpsFileName : mGpsFileList) {
            if (mStop) {
                Log.d(TAG, "gps file parse stop");
                return;
            }

            Log.d(TAG, "parse gps file: " + gpsFileName);
            getGpsDateFromFileName(mTimeStoragePath + File.separator + gpsFileName);
        }
        //Log.d(TAG, "-----mGpsInfoList.size(): " + mGpsInfoList.size());
        mMapView.setLine(mGpsInfoList);

        Message msg = Message.obtain();
        msg.what = GpsInfo.MSG_GPS_INFO_LIST;
        msg.obj = mGpsInfoList;
        mHandler.sendMessage(msg);
    }

    private void getGpsDateFromFileName(String filename) {
        //Log.d("tiantian", "filename: " + filename);
        String[] tmp = null;
        String line = null;
        FileInputStream inputStream = null;
        InputStreamReader inReader = null;
        BufferedReader bufReader = null;

        try {
            inputStream = new FileInputStream(filename);
            inReader = new InputStreamReader(inputStream, "UTF-8");
            bufReader = new BufferedReader(inReader);
            while((line = bufReader.readLine()) != null) {
                if (line.startsWith(GpsParseUtil.GPSENDTIME)) {
                    //Log.d(TAG, "read to the end of the file, break");
                    break;
                } else {
                    tmp = line.split(",");
                    if (tmp[0].endsWith(GpsParseUtil.XXRMC)) {
                        GpsInfo gpsInfo = new GpsInfo();
                        if (GpsParseUtil.nmeaDataParse(gpsInfo, line, GpsParseUtil.RMC, line.length())) {
                            //read 2 line
                            /*
                            if ((line = bufReader.readLine()) != null) {
                                tmp = line.split(",");
                                if (tmp[0].endsWith(GpsParseUtil.XXGGA)) {
                                    if (!GpsParseUtil.nmeaDataParse(gpsInfo, line, GpsParseUtil.GGA, line.length())) {
                                        Log.d(TAG, "xxgga data parse error, break while!");
                                        break;
                                    }
                                }
                            }
                            */

                            if (gpsInfo.getStatus().equals(GpsParseUtil.VALID_DATA))
                                mMapPointId++;

                            gpsInfo.setId(mGpsInfoList.size());
                            gpsInfo.setMapPointId(mMapPointId);
                            mMapView.WGStoGCJ(gpsInfo);
                            //Log.d("tiantian", gpsInfo.toString());
                            mGpsInfoList.add(gpsInfo);
                            //Log.d(TAG, "mGpsInfoList.size(): " + mGpsInfoList.size());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "read " + filename + " error");
        } finally {
            inputReadDeInit(inputStream, inReader, bufReader);
        }
    }

    private void inputReadDeInit(FileInputStream inputStream, InputStreamReader inReader, BufferedReader bufReader)
    {
        try {
            if (bufReader != null)
                bufReader.close();

            if (inReader != null)
                inReader.close();

            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "close file hangder error!");
        }
    }

    private void httpDeInit(HttpURLConnection connection, RandomAccessFile raf, InputStream inputStream)
    {
        try {
            if (connection != null)
                connection.disconnect();

            if (raf != null)
                raf.close();

            if (inputStream != null)
                inputStream.close();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}