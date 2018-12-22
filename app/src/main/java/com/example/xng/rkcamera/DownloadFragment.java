package com.example.xng.rkcamera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xng.rkcamera.DownLoadServer.DownloadService;
import com.example.xng.rkcamera.Map.gps.GpsFileDownloadThread;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {
    public static final String CMD_GETCAMFILENAME = "CMD_GETCAMFILENAME"; //app端主动查询文件列表
    public static final String CMD_CB_GETCAMFILENAME = "CMD_CB_GETCAMFILENAME"; //evb新增文件主动上报
    public static final String CMD_ACK_GETCAMFILE_FINISH = "CMD_ACK_GETCAMFILE_FINISH";
    public static final String CMD_DELSUCCESS = "CMD_DELSUCCESS";
    public static final String CMD_DELFAULT = "CMD_DELFAULT";
    public static final String CMD_CB_DELETE = "CMD_CB_Delete";

    static final String TAG = "DownloadFragment";

    private String mStoragePath = Environment.getExternalStorageDirectory() + "/RkCamera/RkVideo";
    private static final int MAX_DOWNLOAD_COUNT = 1; //3;

    private static final int MSG_LOAD_FINISH = 0;
    private static final int MSG_DELETE_FINISH = 1;
    private static final int MSG_IMAGE_FINISH = 2;
    private static final int MSG_FILELOAD_FINISH = 3;
    private static final int MSG_CMD_CB_DELETE = 4;
    private static final int MSG_CMD_CB_GETCAMFILENAME = 5;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_LOCK = 1;
    private static final int TYPE_PICTURE = 2;
    private int mDownLoadType = -1;

    private ArrayList<DownLoadModel> mDownLoadInfoList = new ArrayList<DownLoadModel>();
    private DownLoadAdapter mDownloadAdapter;
    private ListView mListView;
    private ProgressBar mProgressBar, mProgressBarImage;
    private ImageView mImgAlbum;

    private TextView mTotalDelFiles, mCurDelFiles, mSlash;
    private int mDelFiles = 0;

    private int mCurrentDelPosition = -1;
    private DownLoadModel mCurrentDownloadModel = null;
    private int mBulkDownloadCount = 0;
    private ArrayList<DownLoadModel> mBulkDownLoadList = new ArrayList<DownLoadModel>();

    private Bitmap mBitmap;
    private ImageLoader mImageLoader = ImageLoader.getInstance(); // Get singleton instance
    private DisplayImageOptions mOptions;

    //gps parameters
    private GpsFileDownloadThread mGpsFileDownloadThread = null;
    private boolean mGpsGetListEnd = false;

    private SocketService mSocketService = SocketService.getInstance();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_FINISH:
                    parseMsg(msg.obj.toString());
                    mDownloadAdapter.notifyDataSetChanged();
                    mSocketService.sendMsg("CMD_NEXTFILE", false);
                    break;

                case MSG_CMD_CB_GETCAMFILENAME:
                    parseMsg(msg.obj.toString());
                    mDownloadAdapter.notifyDataSetChanged();
                    break;

                case MSG_FILELOAD_FINISH:
                    mDownloadAdapter.notifyDataSetChanged();
                    break;
                /*
                case MSG_DELETE_FINISH:
                    if(ConnectIP.mSelect == true){
                        dealBulkDeleteMsg(); //批量删除处理
                    } else {    //单个文件删除
                        mDownLoadInfoList.remove(mCurrentDelPosition);
                        updateModeId();
                        mCurrentDelPosition = -1;
                        mDownloadAdapter.notifyDataSetInvalidated();
                    }
                    break;
                */
                case MSG_CMD_CB_DELETE:
                    if(ConnectIP.mSelect == true){
                        dealBulkDeleteMsg();
                    } else {
                        dealOneDeleteMsg(msg.obj.toString());
                    }
                    break;
                case MSG_IMAGE_FINISH:
                    mProgressBarImage.setVisibility(View.GONE);
                    mImgAlbum.setImageBitmap((Bitmap) msg.obj);
                    mImgAlbum.setVisibility(View.VISIBLE);
                    break;

                case GpsInfo.MSG_GPS_FILE_DOWNLOAD_FINISH:
                    Log.d(TAG, "MSG_GPS_FILE_DOWNLOAD_FINISH");

                    if (mCurrentDownloadModel != null) {
                        mCurrentDownloadModel.setMapGpsStoragePath((String)msg.obj);
                        download(mCurrentDownloadModel, getActivity());
                        mCurrentDownloadModel = null;
                    }
                    break;
            }
        }
    };

    private void dealOneDeleteMsg(String msg) {
        String tmp = msg.substring(CMD_CB_DELETE.length() + 1); //CMD_CB_Delete:
        String filename[] = tmp.split("TYPE");

        Log.d(TAG, "filename: " + filename[0]);

        for (DownLoadModel fileInfo : mDownLoadInfoList){
            if (filename[0].equals(fileInfo.getFileName())) {
                Log.d(TAG, "id: " + fileInfo.getId());
                mDownLoadInfoList.remove(fileInfo.getId());
                break;
            }
        }

        updateModeId();
        mCurrentDelPosition = -1;
        mDownloadAdapter.notifyDataSetInvalidated();
    }

    private void dealBulkDeleteMsg() {
        removeDelMode();
        updateModeId();
        if (mCurrentDelPosition == 0) {
            //Log.d(TAG, "mCurrentDelPosition == 0");
            mProgressBar.setVisibility(View.GONE);
            mCurDelFiles.setVisibility(View.GONE);
            mTotalDelFiles.setVisibility(View.GONE);
            mSlash.setVisibility(View.GONE);
            mDelFiles = 0;
        } else {
            mDelFiles++;
            mCurDelFiles.setText(Integer.toString(mDelFiles));
            deleteItem();
        }
        mDownloadAdapter.notifyDataSetChanged();
        ConnectIP.mDelFinish = true;
    }

    private void removeDelMode() {
        for (int i = 0; i < mBulkDownLoadList.size(); i++) {
            if(mBulkDownLoadList.get(i).getId() == mCurrentDelPosition) {
                Log.d(TAG, "removeDelMode, mBulkDownLoadList remove");
                mBulkDownLoadList.remove(i);
                break;
            }
        }

        mDownLoadInfoList.remove(mCurrentDelPosition);
    }

    private void updateModeId() {
        for (int i = 0; i < mDownLoadInfoList.size(); i++) {
            DownLoadModel model = mDownLoadInfoList.get(i);
            model.setId(i);
            for (int j = 0; j < mBulkDownLoadList.size(); j++) {
                DownLoadModel blukModel = mBulkDownLoadList.get(j);
                if (blukModel.getFileName().equals(model.getFileName()))
                    blukModel.setId(i);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        initImageLoader();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SocketService.ACTION_DOWNLOAD_FRAGMENT);
        filter.addAction(SocketService.ACTION_GPS_FILE_LIST);
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_PAUSE);
        filter.addAction(DownloadService.ACTION_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);
        mSocketService.sendMsg("CMD_GETFCAMFILETYPE:normal", false);
        mDownLoadType = TYPE_NORMAL;
    }

    private void initImageLoader(){
        mImageLoader.init(ImageLoaderConfiguration.createDefault(getActivity()));//Android-Universal-Image-Loader, 开源图片加载框架
        mOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.img_nopic_03) // 设置图片下载期间显示的图片
                .showImageForEmptyUri(R.drawable.img_nopic_03) // 设置图片Uri为空或是错误的时候显示的图片
                .showImageOnFail(R.drawable.img_nopic_03) // 设置图片加载或解码过程中发生错误显示的图片
                .cacheInMemory(true) // 设置下载的图片是否缓存在内存中
                .cacheOnDisk(true) // 设置下载的图片是否缓存在SD卡中
                .bitmapConfig(Bitmap.Config.ARGB_8888) //设置图片的解码类型
                //.bitmapConfig(Bitmap.Config.RGB_565) //设置图片的解码类型, tiantian: 避免OOM, 使用RGB_565会比使用ARGB_8888少消耗2倍的内存
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT) //设置图片以如何的编码方式显示
                .build();
    }

    private void clearImageLoaderCache() {
        Log.d(TAG, "clear mImageLoader cache");
        mImageLoader.clearMemoryCache();
        mImageLoader.clearDiskCache();
    }

    public class DownLoadAdapter extends BaseAdapter {
        public Boolean mClick = false;

        private Context context;
        private List<DownLoadModel> list;

        public DownLoadAdapter(Context context, List<DownLoadModel> list) {
            super();
            this.context = context;
            this.list = list;

            File dir = new File(mStoragePath);
            if (!dir.exists())
                dir.mkdir();
        }

        @Override
        public int getCount() {
            if (list != null) {
                return list.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
              return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            final DownLoadModel model = list.get(position);

            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.download_item, null);
                initViewHolder(convertView, viewHolder);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            setViewHolder(viewHolder, model);

            //start 设置开始下载点击监听
            viewHolder.startDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewHolder.pauseDownload.setVisibility(View.VISIBLE);
                    viewHolder.startDownload.setVisibility(View.GONE);
                    downloadFile(model, context);
                }
            });

            viewHolder.pauseDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewHolder.pauseDownload.setVisibility(View.GONE);
                    viewHolder.startDownload.setVisibility(View.VISIBLE);

                    if (model.getLoadStatus()) {
                        stopGetGpsList();

                        model.setLoadStatus(false);
                        Intent intent = new Intent(context,DownloadService.class);
                        intent.setAction(DownloadService.ACTION_STOP);
                        intent.putExtra("fileInfo", model);
                        intent.putExtra("isDeleteFile", false);
                        context.startService(intent);
                    } else {
                        removeFromBulkDownloadList(model);
                    }
                    if(isVisible()){
                        model.setType(DownLoadModel.TYPE_NOCHECKED);
                    } else {
                        model.setType(DownLoadModel.TYPE_CHECKED);
                    }
                    //model.setFlag(false);
                }
            });

            viewHolder.open_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openFile(viewHolder, model);
                }
            });

            setKeyVisible(viewHolder, model);

            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 改变CheckBox状态
                    if (viewHolder.checkBox.isEnabled()) {
                        list.get(position).setFlag(viewHolder.checkBox.isChecked());
                    }
                }
            });

            viewHolder.item_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(viewHolder.checkBox.getVisibility() == View.VISIBLE) {
                        // 改变CheckBox状态
                        if (viewHolder.checkBox.isEnabled()) {
                             viewHolder.checkBox.toggle();
                             list.get(position).setFlag(viewHolder.checkBox.isChecked());
                        }
                    } else {
                        openFile(viewHolder, model);
                    }
                }
            });

            viewHolder.item_layout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
                    builder.setMessage(getActivity().getString(R.string.delete_file_msg));
                    builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mCurrentDelPosition = position;
                            delete(model);
                        }
                    });
                    builder.setNegativeButton(getActivity().getString(R.string.no), null);
                    //添加AlertDialog.Builder对象的setNegativeButton()方法
                    builder.create().show();
                    return false;
                }
            });
            return convertView;
        }

        private void removeFromBulkDownloadList(DownLoadModel model)
        {
            for (int i = 0; i < mBulkDownLoadList.size(); i++) {
                if(mBulkDownLoadList.get(i).getId() == model.getId()) {
                    mBulkDownLoadList.remove(i);
                    break;
                }
            }
        }

        private void initViewHolder(View convertView, ViewHolder viewHolder)
        {
            viewHolder.tv_filename = (TextView) convertView.findViewById(R.id.tv_filename);
            viewHolder.tv_fileday = (TextView) convertView.findViewById(R.id.tv_fileday);
            viewHolder.tv_filetime = (TextView) convertView.findViewById(R.id.tv_filetime);
            viewHolder.tv_filesize = (TextView) convertView.findViewById(R.id.tv_filesize);
            viewHolder.startDownload = (Button) convertView.findViewById(R.id.btn_start);
            viewHolder.pauseDownload = (Button)convertView.findViewById(R.id.btn_pause);
            viewHolder.open_btn = (Button)convertView.findViewById(R.id.btn_open);
            viewHolder.item_layout = (LinearLayout) convertView.findViewById(R.id.item_layout);
            viewHolder.iv_fileimage = (ImageView) convertView.findViewById(R.id.iv_fileimage);
            viewHolder.pbProgress = (ProgressBar) convertView.findViewById(R.id.pb_progress_bar);
            viewHolder.checkBox = (CheckBox)convertView.findViewById(R.id.ckb_check);
            convertView.setTag(viewHolder);
        }

        private void setViewHolder(ViewHolder viewHolder, DownLoadModel model)
        {
            viewHolder.pbProgress.setMax(100);
            viewHolder.tv_filename.setText(model.getFileName());

            String s = model.getDownloadSize().toString();
            long i = Integer.parseInt(s);
            int j = 0;
            float size= (float)i/1024;
            while (size > 1024) {
                j++;
                size = size/1024;
            }
            DecimalFormat df = new DecimalFormat("0.00");//格式化小数，不足的补0
            String filesize = df.format(size);//返回的是String类型的
            switch (j){
                case 0:
                    viewHolder.tv_filesize.setText(filesize + "KB");
                    break;
                case 1:
                    viewHolder.tv_filesize.setText(filesize + "MB");
                    break;
                case 2:
                    viewHolder.tv_filesize.setText(filesize + "GB");
                    break;
                case 3:
                    viewHolder.tv_filesize.setText(filesize + "GB");
                    break;
                case 4:
                    viewHolder.tv_filesize.setText(filesize + "TB");
                    break;
            }

            viewHolder.tv_fileday.setText(model.getDownloadDay());
            viewHolder.tv_filetime.setText(model.getDownloadTime());

            viewHolder.checkBox.setChecked(model.getFlag());
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            if(mClick){
                viewHolder.checkBox.setVisibility(View.VISIBLE);
            }else {
                viewHolder.checkBox.setVisibility(View.GONE);
            }

            checkFile(model);

            //获取视频缩略图
            //这句代码的作用是为了解决convertView被重用的时候，图片预设的问题
            viewHolder.iv_fileimage.setImageResource(R.drawable.img_nopic_03);
            mImageLoader.displayImage(model.getThumbUrl(), viewHolder.iv_fileimage, mOptions);
            viewHolder.pbProgress.setProgress(model.getFinished());
        }

        private void setKeyVisible(ViewHolder viewHolder, DownLoadModel model)
        {
            if (model.getType() == DownLoadModel.TYPE_CHECKED) {
                viewHolder.startDownload.setVisibility(View.GONE);
                viewHolder.pauseDownload.setVisibility(View.VISIBLE);
                viewHolder.open_btn.setVisibility(View.GONE);
            } else if (model.getType() == DownLoadModel.TYPE_NOCHECKED) {
                viewHolder.startDownload.setVisibility(View.VISIBLE);
                viewHolder.pauseDownload.setVisibility(View.GONE);
                viewHolder.open_btn.setVisibility(View.GONE);
            } else {
                viewHolder.startDownload.setVisibility(View.GONE);
                viewHolder.pauseDownload.setVisibility(View.GONE);
                viewHolder.open_btn.setVisibility(View.VISIBLE);
            }
        }

        private void openFile(ViewHolder viewHolder, DownLoadModel model) {
            //Log.d(TAG, "model.getFileUrl(): " + model.getFileUrl());
            if(viewHolder.tv_filename.getText().toString().endsWith(".JPG")||viewHolder.tv_filename.getText().toString().endsWith(".jpg")){
                returnBitmap(model.getFileUrl());
                mProgressBarImage.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                mImgAlbum.setVisibility(View.GONE);

                Intent intent = new Intent(MainActivity.DOWNLOAD_FRAMENT_DISABLE_UI);
                getActivity().sendBroadcast(intent);
            } else if (viewHolder.tv_filename.getText().toString().endsWith(".MP4")||viewHolder.tv_filename.getText().toString().endsWith(".mp4")){
                String videourl = model.getFileUrl();
                ConnectIP.mVideoUrl = videourl;
                if (!TextUtils.isEmpty(videourl)) {
                    Intent intent = new Intent(getActivity(),VideoPlay.class);
                    intent.putExtra("position",model.getId());
                    intent.putExtra("videourl", (Serializable) list);
                    startActivity(intent);
                }
            }
        }

        /**
         * 更新列表项中的进度条
         */
        public void updateProgress(DownLoadModel model, int progress)
        {
            int id = model.getId();
            if (id < list.size()) {
                DownLoadModel fileInfo = list.get(id);
                if (model.getFileName().equals(fileInfo.getFileName())) {
                    fileInfo.setFinished(progress);
                    notifyDataSetChanged();
                }
            }
        }

        //下载完成，将文件从tmp目录移到RKVideo目录
        private void moveFile(DownLoadModel fileInfo) {
            try {
                File file = new File(DownloadService.DOWNLOAD_PATH, fileInfo.getFileName());
                if (!file.exists()) {
                    Log.d(TAG, "Error," + fileInfo.getFileName() + " does not exist!");
                    return;
                }

                if (file.renameTo(new File(mStoragePath, file.getName()))) {
                    Log.d(TAG, "File is moved successful!");
                } else {
                    Log.d(TAG, "File is failed to move!");
                }

                deleteFile(DownloadService.DOWNLOAD_PATH, fileInfo.getXmlFileName());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        //一个文件下载完成，状态更新
        public void update(DownLoadModel model)
        {
            int id = model.getId();
            if (id < list.size()) {
                DownLoadModel fileInfo = list.get(id);
                if (model.getFileName().equals(fileInfo.getFileName())) {
                    String url = "file://" + mStoragePath + "/" + fileInfo.getFileName();
                    moveFile(fileInfo);
                    //fileInfo.setFlag(false);
                    fileInfo.setLoadStatus(false);
                    fileInfo.setFileUrl(url);
                    fileInfo.setFinished(100);
                    fileInfo.setType(DownLoadModel.TYPE_OPEN);

                    downloadNextFile(context);
                    notifyDataSetChanged();
                }
            }
        }

        /**
         * 一键暂停所有文件的下载
         */
        public void setStopDownloadAll(){
            //Log.d("tiantian", "setStopDownloadAll");
            mBulkDownLoadList.clear();
            for (DownLoadModel fileInfo : list){
                if (fileInfo.getLoadStatus()) {
                    stopGetGpsList();

                    fileInfo.setLoadStatus(false);
                    fileInfo.setType(DownLoadModel.TYPE_NOCHECKED);

                    Intent intent = new Intent(context, DownloadService.class);
                    intent.putExtra("fileInfo", fileInfo);
                    intent.putExtra("isDeleteFile", false);
                    intent.setAction(DownloadService.ACTION_STOP);
                    context.startService(intent);
                }
            }
            notifyDataSetChanged();
        }

        /**
         * 一键下载所有未完成下载的文件
         */
        public void setStartDownloadAll(){
            for(DownLoadModel fileInfo : list ) {
                notifyDataSetChanged();
                Intent intent = new Intent(context, DownloadService.class);
                intent.putExtra("fileInfo", fileInfo );
                intent.setAction(DownloadService.ACTION_START);
                context.startService(intent);
            }
        }

       private void returnBitmap(final String url) {
           new Thread() {
               @Override
               public void run() {
                   try {
                       URL myFileUrl = new URL(url);
                       InputStream inputStream = null;

                       if (url.startsWith("file://")) {
                           inputStream = new FileInputStream(url.substring("file://".length()));
                       } else {
                           HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                           conn.setDoInput(true);
                           conn.connect();
                           inputStream = conn.getInputStream();
                       }

                       mBitmap = BitmapFactory.decodeStream(inputStream);
                       inputStream.close();
                       Message msg = new Message();
                       msg.what = MSG_IMAGE_FINISH;
                       msg.obj = mBitmap;
                       mHandler.sendMessage(msg);
                   } catch (MalformedURLException e) {
                       e.printStackTrace();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }.start();
       }
    }

    private void checkFile(DownLoadModel model)
    {
        //Log.d("tiantian", "checkFile");
        if (findFiles(mStoragePath, model.getFileName())) {//文件下载完成
            String url = "file://" + mStoragePath + "/" + model.getFileName();
            model.setFileUrl(url);
            model.setFinished(100);
            model.setType(DownLoadModel.TYPE_OPEN);
        } else if (findFiles(DownloadService.DOWNLOAD_PATH, model.getXmlFileName())) {//存在保存下载进度的xml文件
            if (findFiles(DownloadService.DOWNLOAD_PATH, model.getFileName())) {
                //Log.d("tiantian", "find xml and tmp file: " + model.getXmlFileName());
                try {
                    FileReader reader = new FileReader(DownloadService.DOWNLOAD_PATH + "/" + model.getXmlFileName());
                    BufferedReader br = new BufferedReader(reader);
                    String str = br.readLine();
                    br.close();
                    reader.close();
                    //Log.d("tiantian", "str: " + str);
                    if (str != null) {
                        int progress = 0;
                        model.setDownLoadProgress(str);
                        String tmp[] = str.split("&");
                        for (String tmp1 : tmp) {
                            String tmp2[] = tmp1.split(":");
                            progress += Integer.parseInt(tmp2[1]);
                            //Log.d("tiantian", "progress: " + progress);
                        }

                        model.setFinished((int) ((long)progress * 100 / Integer.parseInt(model.getDownloadSize())));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean findFiles(String dirName, String targetFileName)
    {
        //Log.d(TAG, "dirName: " + dirName);
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()){
            //Log.d(TAG, "文件查找失败：" + dirName + "不是一个目录！");
            return false;
        } else {
            String[] fileList = dir.list();
            for (int i = 0; i < fileList.length; i++) {
                File readfile = new File(dirName, fileList[i]);
                if(readfile.exists() && !readfile.isDirectory()) {
                    String tempName =  readfile.getName();

                    if (tempName.equals(targetFileName)) {
                        //Log.d(TAG, "find file: " + targetFileName);
                        return true;
                    }
                } else if(readfile.isDirectory()){
                    findFiles(dirName + "/" + fileList[i], targetFileName);
                }
            }
        }
        return false;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

    	Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.download_layout, container,
                false);
        mListView = (ListView) view.findViewById(R.id.rv_download_list);
        mImgAlbum = (ImageView) view.findViewById(R.id.down_image);
        mImgAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImgAlbum.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);

                Intent intent = new Intent(MainActivity.DOWNLOAD_FRAMENT_ENABLE_UI);
                getActivity().sendBroadcast(intent);
            }
        });
        mDownloadAdapter = new DownLoadAdapter(getActivity(),mDownLoadInfoList);
        mListView.setAdapter(mDownloadAdapter);

        mProgressBar = (ProgressBar) view.findViewById(R.id.pb);
        mProgressBarImage = (ProgressBar)view.findViewById(R.id.pb_image);
        mProgressBarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBarImage.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
                mImgAlbum.setVisibility(View.GONE);

                Intent intent = new Intent(MainActivity.DOWNLOAD_FRAMENT_ENABLE_UI);
                getActivity().sendBroadcast(intent);
            }
        });
        //mProgressBar.setVisibility(View.GONE);
        //mListView.setVisibility(View.GONE);

        mTotalDelFiles = (TextView) view.findViewById(R.id.total_del_files);
        mCurDelFiles = (TextView) view.findViewById(R.id.cur_del_files);
        mSlash = (TextView) view.findViewById(R.id.slash);

        return view;
    }

    @Override
    public void onPause(){
        //mImageLoader.pause();
        mDownloadAdapter.setStopDownloadAll();
        Log.d(TAG, "页面不可见时暂停下载(onPause)");
        super.onPause();
    }

    @Override
    public void onResume(){
    	Log.d(TAG, "onResume");
        super.onResume();
        //if(mProgressBar.getVisibility()!=View.VISIBLE){
        //    refresh();
        //}
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
    	Log.d(TAG, "onHiddenChanged, hidden: " + hidden);
        super.onHiddenChanged(hidden);
        if (hidden) {// 不在最前端界面显示
            if(mImgAlbum != null){
                mImgAlbum.setVisibility(View.GONE);
            }
            //mImageLoader.pause();
            mDownloadAdapter.setStopDownloadAll();
            Log.d(TAG, "页面不可见时暂停下载(hidden)");
//            getActivity().unregisterReceiver(mReceiver);
        } else {// 重新显示到最前端中

        }
    }

    @Override
    public void onDestroy(){
        mDownloadAdapter.setStopDownloadAll();
    	Log.d(TAG, "onDestroy");
        Log.d(TAG, "整个程序停止时暂停下载");
        getActivity().unregisterReceiver(mReceiver);
        //clearImageLoaderCache();
        mImageLoader.destroy();
        mDownLoadInfoList.clear();
        mBulkDownLoadList.clear();
        super.onDestroy();
    }

    /**
     * 更新UI的广播接收器
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Log.d("tiantian", "onReceive, intent.getAction(): " + intent.getAction());
            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
                int finised = intent.getIntExtra("finished", 0);
                //int id = intent.getIntExtra("id", 0);
                DownLoadModel fileInfo = (DownLoadModel) intent.getSerializableExtra("fileInfo");
                mDownloadAdapter.updateProgress(fileInfo, finised);
                //Log.i("tiantian", "ACTION_UPDATE: " + id);
            } else if (DownloadService.ACTION_FINISHED.equals(intent.getAction())) {
                // 下载结束
                Log.d(TAG, "下载结束");
                DownLoadModel fileInfo = (DownLoadModel) intent.getSerializableExtra("fileInfo");
                updateGpsMapVideoCount(fileInfo);
                mDownloadAdapter.update(fileInfo);
                Toast.makeText(getActivity(), mDownLoadInfoList.get(fileInfo.getId()).getFileName() + getActivity().getString(R.string.download_complete_msg), Toast.LENGTH_SHORT).show();
            } else if (DownloadService.ACTION_PAUSE.equals(intent.getAction())) {
                //某一个文件暂停下载
                //Log.d("tiantian", "暂停下载");
                downloadNextFile(getContext());
            } else if (SocketService.ACTION_DOWNLOAD_FRAGMENT.equals(intent.getAction())){
                String info = intent.getStringExtra("msg");
                dealSocketRevMsg(info);
            } else if (SocketService.ACTION_GPS_FILE_LIST.equals(intent.getAction())
                    && mSocketService.getGpsOwner().equals("DownloadFragment")) {
                mGpsGetListEnd = true;

                //String mapGpsFile = intent.getStringExtra("mapGpsFile");
                ArrayList<String> gpsFileList = (ArrayList<String>) intent.getSerializableExtra("gpsFileList");

                if (gpsFileList.size() > 0) {
                    mGpsFileDownloadThread = new GpsFileDownloadThread(mHandler, null, null,
                            gpsFileList, null, false, true, GpsFileDownloadThread.mLocalStoragePath);
                    mGpsFileDownloadThread.start();
                } else {
                    Message msg = Message.obtain();
                    msg.what = GpsInfo.MSG_GPS_FILE_DOWNLOAD_FINISH;
                    mHandler.sendMessage(msg);
                }
            }
        }
    };

    private void dealSocketRevMsg(String info) {
        Message msg = new Message();
        if (info.startsWith(CMD_GETCAMFILENAME)) {
            msg.what = MSG_LOAD_FINISH;
            msg.obj = info;
            mHandler.sendMessage(msg);
        } else if (info.startsWith(CMD_CB_GETCAMFILENAME)) {
            msg.what = MSG_CMD_CB_GETCAMFILENAME;
            msg.obj = info;
            mHandler.sendMessage(msg);
        } /*else if(info.startsWith(CMD_DELSUCCESS)) {
            msg.what = MSG_DELETE_FINISH;
            msg.obj = info;
            mHandler.sendMessage(msg);
        } */else if(info.startsWith(CMD_CB_DELETE)) {
            msg.what = MSG_CMD_CB_DELETE;
            msg.obj = info;
            mHandler.sendMessage(msg);
        } else if(info.startsWith(CMD_ACK_GETCAMFILE_FINISH)) {
            if (mDownLoadType == TYPE_NORMAL) {
                mSocketService.sendMsg("CMD_GETFCAMFILETYPE:lock", false);
                mDownLoadType = TYPE_LOCK;
            } else if (mDownLoadType == TYPE_LOCK) {
                mSocketService.sendMsg("CMD_GETFCAMFILETYPE:picture", false);
                mDownLoadType = TYPE_PICTURE;
            } else {
                mHandler.sendEmptyMessage(MSG_FILELOAD_FINISH);
            }
        } else if (info.startsWith(CMD_DELFAULT)) {
            mProgressBar.setVisibility(View.GONE);
            mCurDelFiles.setVisibility(View.GONE);
            mTotalDelFiles.setVisibility(View.GONE);
            mSlash.setVisibility(View.GONE);
            mDelFiles = 0;

            delFailBuilder();
        }
    }

    private void delFailBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //builder.setCancelable(false);
        builder.setMessage(getActivity().getString(R.string.del_file_failed));
        builder.setPositiveButton(getActivity().getString(R.string.retry), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                reciveDelete(true);
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create().show();
    }

    public void reciveSelect(boolean b){
        mDownloadAdapter.mClick = b;
        mDownloadAdapter.notifyDataSetChanged();
    }

    public void reciveCancle(boolean b){
        for (DownLoadModel model : mDownLoadInfoList) {
            model.setFlag(false);
        }

        mDownloadAdapter.mClick = b;
        mDownloadAdapter.notifyDataSetChanged();
    }

    public void reciveSelectAll(boolean b){
        for (DownLoadModel model : mDownLoadInfoList) {
            if(b)
                model.setFlag(true);
            else
                model.setFlag(false);
        }
        mDownloadAdapter.notifyDataSetChanged();
    }

    public void bulkDownload()
    {
        mCurrentDelPosition = mDownLoadInfoList.size();
        for (int i = mCurrentDelPosition - 1; i > -1; i--) {
            DownLoadModel model = mDownLoadInfoList.get(i);
            if (model.getType() == DownLoadModel.TYPE_NOCHECKED && model.getFlag()) {
                downloadFile(model, getContext());
                mCurrentDelPosition = i;
            }
        }
        mDownloadAdapter.notifyDataSetChanged();
    }

    private void downloadNextFile(Context context)
    {
        mBulkDownloadCount--;
        Log.d("tiantian", "downloadNextFile, mBulkDownloadCount: " + mBulkDownloadCount);
        Log.d("tiantian", "mBulkDownLoadList.size(): " + mBulkDownLoadList.size());
        if (mBulkDownloadCount < MAX_DOWNLOAD_COUNT && mBulkDownLoadList.size() > 0) {
            DownLoadModel model = mBulkDownLoadList.get(0);
            mBulkDownLoadList.remove(0);
            downloadFile(model, context);
        }
    }

    private void downloadFile(DownLoadModel model, Context context)
    {
        //Log.d("tiantian", "downloadFile, model.getFileName(): " + model.getFileName());
        if(isVisible()){
            model.setType(DownLoadModel.TYPE_CHECKED);
        } else {
            model.setType(DownLoadModel.TYPE_NOCHECKED);
        }

        //Log.d("tiantian", "downloadFile, mBulkDownloadCount: " + mBulkDownloadCount);
        if (mBulkDownloadCount < MAX_DOWNLOAD_COUNT) {
            mBulkDownloadCount++;
            model.setLoadStatus(true);

            if (model.getFileType() == DownLoadModel.VIDEO) {
                mCurrentDownloadModel = model;
                startGetGpsList(model.getFileName());
            } else {
                download(model, context);
            }
        } else {
            model.setLoadStatus(false);
            mBulkDownLoadList.add(model);
        }
    }

    private void download(DownLoadModel model, Context context) {
        checkFile(model);

        // 通知Service开始下载
        Log.i(TAG , "downloadFile:" + model.toString());
        Intent intent = new Intent(getActivity(), DownloadService.class);
        intent.setAction(DownloadService.ACTION_START);
        intent.putExtra("fileInfo", model);
        context.startService(intent);

        deleteFile(DownloadService.DOWNLOAD_PATH, model.getXmlFileName());
    }

    private void deleteFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (file.exists()) {
            file.delete();
            //Log.d("tiantian", "delete file: " + fileName);
        }
    }

    public void reciveDelete(boolean b) {
        //Log.d(TAG, "reciveDelete");
        if (b) {
            int totalDelFiles = 0;
            for (DownLoadModel model : mDownLoadInfoList) {
                if (model.getFlag()) {
                    totalDelFiles++;
                }
            }

            if (totalDelFiles > 0) {
                Log.d(TAG, "totalDelFiles: " + totalDelFiles);
                Log.d(TAG, "mDownLoadInfoList.size: " + mDownLoadInfoList.size());
                mTotalDelFiles.setText(Integer.toString(totalDelFiles));
                mCurDelFiles.setText("0");

                mCurrentDelPosition = mDownLoadInfoList.size() ;
                mProgressBar.setVisibility(View.VISIBLE);
                mCurDelFiles.setVisibility(View.VISIBLE);
                mTotalDelFiles.setVisibility(View.VISIBLE);
                mSlash.setVisibility(View.VISIBLE);
                deleteItem();
            }

        }
    }

    private void deleteItem(){
        DownLoadModel model;
        //Log.d(TAG, "mCurrentDelPosition: " + mCurrentDelPosition);
        for (int i = mCurrentDelPosition - 1; i > -1; i--) {
            model = mDownLoadInfoList.get(i);
            if (model.getFlag()) {
                mCurrentDelPosition = i;
                //Log.d("tiantian", "deleteItem, mode: " + model.toString());
                delete(model);
                break;
            } else if(0 == i) {
                //Log.d(TAG, "deleteItem, 0 == i");
                mProgressBar.setVisibility(View.GONE);
                mCurDelFiles.setVisibility(View.GONE);
                mTotalDelFiles.setVisibility(View.GONE);
                mSlash.setVisibility(View.GONE);
                mDelFiles = 0;
                mDownloadAdapter.notifyDataSetChanged();
            }
        }
    }

    private void delete(DownLoadModel model) {
        if (model.getLoadStatus()) {
            Log.d(TAG, "deleteItem, ACTION_STOP");
            stopGetGpsList();

            Intent intent = new Intent(getActivity(),DownloadService.class);
            intent.setAction(DownloadService.ACTION_STOP);
            intent.putExtra("fileInfo", model);
            intent.putExtra("isDeleteFile", true);
            getContext().startService(intent);
        } else {
            deleteFile(DownloadService.DOWNLOAD_PATH, model.getFileName());
            deleteFile(DownloadService.DOWNLOAD_PATH, model.getXmlFileName());
        }

        mSocketService.sendMsg("CMD_DELFCAMFILENAME:"+ model.getFileName()
                + "TYPE:" + model.getDownloadType()
                + "FORM:" + model.getDownloadForm()+ "END", false);

    }

    public void close(){
       Log.d(TAG, "清空列表！！！！！！！！");
       mDownLoadInfoList.clear();
        mBulkDownLoadList.clear();
    }

    public void refresh(){
        Log.d(TAG, "重新刷新！！！！！！！！");
        mDownLoadInfoList.clear();
        mBulkDownLoadList.clear();
        mBulkDownloadCount = 0;
        mHandler.sendEmptyMessage(MSG_FILELOAD_FINISH);
        mSocketService.sendMsg("CMD_GETFCAMFILETYPE:normal", false);
        mDownLoadType = TYPE_NORMAL;
    }

    private void parseMsg(String msg) {
        if (msg.endsWith("END")) {
                String[] field0 = msg.split("NAME:");
                String[] field1 = field0[1].split("TYPE:");
                String filename = field1[0];
                String[] field2 = field1[1].split("PATH:");
                String type = field2[0];
                String[] field3 = field2[1].split("FORM:");
                String directory = field3[0];
                String[] field4 = field3[1].split("SIZE:");
                String form = field4[0];
                String[] field5 = field4[1].split("DAY:");
                String filesize = field5[0];
                String[] field6 = field5[1].split("TIME:");
                String fileday = field6[0];
                String[] field7 = field6[1].split("COUNT:");
                String filetime = field7[0];

                DownLoadModel cmd = new DownLoadModel(0, mDownLoadInfoList.size(), directory,
                        0, filename, filesize, fileday, filetime, type, form);
                mDownLoadInfoList.add(cmd);
        }
    }

    private void startGetGpsList(String fileName) {
        mGpsFileDownloadThread = null;
        mGpsGetListEnd = false;

        mSocketService.clearGpsFileList();
        mSocketService.setGpsOwner("DownloadFragment");
        mSocketService.sendMsg(GpsInfo.CMD_GET_GPS_LIST + fileName, false);
    }

    private void stopGetGpsList() {
        Log.d("GpsFileDownloadThread", "stopGetGpsList");
        if (!mGpsGetListEnd)
            mSocketService.sendMsg(GpsInfo.CMD_STOP_GET_GPS_LIST, false);

        if (mGpsFileDownloadThread != null) {
            mGpsFileDownloadThread.stopDownload();
        }
    }

    //MP4下载完成，统计同一时间段本地GPS文件，对应的本地MP4文件数
    private void updateGpsMapVideoCount(DownLoadModel fileInfo) {
        //Log.d(TAG, "updateGpsMapVideoCount");
        if (fileInfo.getMapGpsStoragePath() != null) {
            try {
                int count = 0;
                String str = null;
                File file = new File(fileInfo.getMapGpsStoragePath(), GpsInfo.COUNT_FILE_NAME);

                if (!file.exists()) {
                    //Log.d(TAG, "creat file");
                    file.createNewFile();
                } else {
                    //read count
                    FileInputStream inputStream = new FileInputStream(file);
                    InputStreamReader inReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufReader = new BufferedReader(inReader);
                    if ((str = bufReader.readLine()) != null) {
                        count = Integer.parseInt(str);
                        Log.d(TAG, "count: " + count);
                    }
                    inputStream.close();
                    inReader.close();
                    bufReader.close();
                }

                //update count
                count++;
                str = String.valueOf(count);
               // Log.d(TAG, "str: " + str);
                FileOutputStream out = new FileOutputStream(file);
                out.write(str.getBytes(), 0, str.getBytes().length);
                out.flush();
                out.close();
            } catch (IOException e) {
                //Log.d(TAG, "IOException");
                e.printStackTrace();
            }
        }
    }
}

