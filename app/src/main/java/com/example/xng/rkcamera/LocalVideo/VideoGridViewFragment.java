package com.example.xng.rkcamera.LocalVideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.xng.rkcamera.ConnectIP;
import com.example.xng.rkcamera.Map.gps.GpsFileDownloadThread;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.SettingFragment;
import com.example.xng.rkcamera.VideoPlay;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xng on 2016/11/16.
 */
public class VideoGridViewFragment extends Fragment{
    static final String TAG = "VideoGridViewFragment";

    private GridView mGridView;
    private VideoAdapter mAdapter;
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater,container,savedInstanceState);
        View view =  inflater.inflate(R.layout.video_gridview,container,false);
        int gridColumns = 4;
        mGridView = (GridView)view.findViewById(R.id.gvVideoMember);
        mGridView.setNumColumns(gridColumns);
        if(null == mAdapter){
            mAdapter = new VideoAdapter(getActivity());
        }
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.cb_checkbox);
                if(checkbox.getVisibility() == View.VISIBLE){
                    checkbox.setChecked(!checkbox.isChecked());
                    List<VideoGridViewModel> list = mAdapter.getMemberList();
                    list.get(position).setFlag(checkbox.isChecked());
                }else {
                    VideoGridViewModel member = (VideoGridViewModel)parent.getItemAtPosition(position);
                    List<VideoGridViewModel> list = mAdapter.getMemberList();
//                    Toast toast = new Toast(getActivity());
//                    toast.makeText(getContext(),"地址："+member.getPath(),Toast.LENGTH_SHORT).show();
                    ConnectIP.mVideoUrl = "file://" + member.getPath();
                    Log.d(TAG, "ConnectIP.mVideoUrl: " + ConnectIP.mVideoUrl);
                    Intent intent = new Intent(getActivity(), VideoPlay.class);
                    intent.putExtra("localposition",position);
                    intent.putExtra("localvideourl", (Serializable) list);
                    startActivity(intent);
                }
            }
        });
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "本地视频刷新！");
        refresh();
    }

    public void reciveSelect(boolean b){
        mAdapter.mClick = b;
        mAdapter.notifyDataSetChanged();
    }
    public void reciveCancle(boolean b){
        mAdapter.mClick = b;
        mAdapter.notifyDataSetChanged();
    }
    public void reciveSelectAll(boolean b){
        List<VideoGridViewModel> list = mAdapter.getMemberList();
        if(b){
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setFlag(true);
            }
            mAdapter.notifyDataSetChanged();
        }else {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getFlag()) {
                    list.get(i).setFlag(false);
                } else {
                    list.get(i).setFlag(true);
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }
    public void reciveDelete(boolean b){
        /**
         * 这里不能直接操作mDownLoadInfoList，需要缓存到一个列表中一起清除，否则会出现错误
         */

        if(b){
            // pb.setVisibility(View.VISIBLE);
            List<VideoGridViewModel> list = mAdapter.getMemberList();
            for (int i = list.size() - 1; i > -1 ; i--) {
                if(list.get(i).getFlag()) {
                    File f = new File(list.get(i).getPath());

                    updateGpsMapVideoCount(f.getName());

                    boolean mm = f.delete();
                    if (mm) {
                        list.remove(i);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
            // pb.setVisibility(View.GONE);
        }
    }

    private void updateGpsMapVideoCount(String videoFileName) {
        String curTime[] = videoFileName.split("_"); //lg: 20160223_210726_A.mp4
        long cTime = Long.parseLong(curTime[0] + curTime[1]);

        File storagePath = new File(GpsFileDownloadThread.mLocalStoragePath);
        File files[] = storagePath.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (file.isDirectory()) {
                        String fileName[] = file.getName().split("-"); //20160223_210726_1-20160223_210826_1
                        String startTime[] = fileName[0].split("_");
                        String endTime[] = fileName[1].split("_");
                        long sTime = Long.parseLong(startTime[0] + startTime[1]);
                        long eTime = Long.parseLong(endTime[0] + endTime[1]);

                        if (cTime >= sTime && cTime <= eTime) {
                            //Log.d(TAG, "file.getName(): " + file.getName());
                            String mapGpsTimeStoragePath = GpsFileDownloadThread.mLocalStoragePath
                                    + File.separator + file.getName();
                            File countFile = new File(mapGpsTimeStoragePath, GpsInfo.COUNT_FILE_NAME);
                            if (countFile.exists()) {
                                int count = 0;
                                String str = null;

                                //read count
                                FileInputStream inputStream = new FileInputStream(countFile);
                                InputStreamReader inReader = new InputStreamReader(inputStream, "UTF-8");
                                BufferedReader bufReader = new BufferedReader(inReader);
                                if ((str = bufReader.readLine()) != null) {
                                    count = Integer.parseInt(str);
                                    //Log.d(TAG, "count: " + count);
                                }
                                inputStream.close();
                                inReader.close();
                                bufReader.close();

                                count--;
                                //Log.d(TAG, "---count: " + count);
                                if (count <= 0) {
                                    SettingFragment.deleteAllFiles(new File(mapGpsTimeStoragePath));
                                } else {
                                    //update count
                                    str = String.valueOf(count);
                                    //Log.d(TAG, "str: " + str);
                                    FileOutputStream out = new FileOutputStream(countFile);
                                    out.write(str.getBytes(), 0, str.getBytes().length);
                                    out.flush();
                                    out.close();
                                }
                            }
                        }

                        break;
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class VideoAdapter extends BaseAdapter{
        private LayoutInflater layoutInflater;
        private List<VideoGridViewModel> memberList;
        public Boolean mClick = false;
        private ImageLoader imageLoader = ImageLoader.getInstance(); // Get singleton instance
        private DisplayImageOptions options;

        public List<VideoGridViewModel> getMemberList() {
            return memberList;
        }

        public VideoAdapter(Context context){
            layoutInflater = LayoutInflater.from(context);
            memberList = new ArrayList<>();
            String path = Environment.getExternalStorageDirectory()+"/RkCamera/RkVideo";
            scanFile(memberList,path);
            imageLoader.init(ImageLoaderConfiguration.createDefault(getActivity()));//int()
            options = new DisplayImageOptions.Builder()
                    .showStubImage(R.drawable.img_nopic_03)
                    // 设置图片下载期间显示的图片
                    .showImageForEmptyUri(R.drawable.img_nopic_03) // 设置图片Uri为空或是错误的时候显示的图片
                    .showImageOnFail(R.drawable.img_nopic_03)
                    // 设置图片加载或解码过程中发生错误显示的图片
                    .cacheInMemory(true)
                    // 设置下载的图片是否缓存在内存中
                    .cacheOnDisc(true)
                    // 设置下载的图片是否缓存在SD卡中
                    // .displayer(new RoundedBitmapDisplayer(20)) // 设置成圆角图片
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                    .build();
            // 创建配置过的DisplayImageOption对象
        }

        private void scanFile(List<VideoGridViewModel> list,String path){
            File file = new File(path);
            if(null != file && file.isFile()){
                if(path.toLowerCase().endsWith(".mp4")
                        || path.toLowerCase().endsWith(".MP4")){
                    list.add(new VideoGridViewModel(path));
                }
            }else if (null != file && file.isDirectory()){
                File[] childFiles = file.listFiles();
                for (File f:childFiles){
                    scanFile(list,f.getAbsolutePath());
                }
            }
        }

        public void freshData(){
            memberList.clear();
            String path = Environment.getExternalStorageDirectory()+"/RkCamera/RkVideo";
            scanFile(memberList,path);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return memberList.size();
        }

        @Override
        public Object getItem(int position) {
            return memberList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = layoutInflater.inflate(R.layout.video_grid_item,parent,false);
            }
            VideoGridViewModel member = memberList.get(position);
            ImageView ivImage = (ImageView)convertView.findViewById(R.id.ivvideoImage);
            CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.cb_checkbox);

//            MyAsyncTask task = new MyAsyncTask(ivImage,member);
//            task.execute(member.getPath());
            imageLoader.displayImage("file://"+member.getPath(),ivImage, options);

            checkbox.setChecked(memberList.get(position).getFlag());
            checkbox.setVisibility(View.VISIBLE);
            if(mClick){
                checkbox.setVisibility(View.VISIBLE);
            }else {
                checkbox.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
    public void refresh(){
        if(null != mAdapter){
            mAdapter.freshData();
        }
    }

    private class MyAsyncTask extends AsyncTask<String,String,Bitmap>{
        private  ImageView mImageView;
        private VideoGridViewModel mModel;

        MyAsyncTask(ImageView imageView,VideoGridViewModel model){
            mImageView = imageView;
            mModel = model;
        }
        @Override
        protected Bitmap doInBackground(String... strings) {
            String path = strings[0];
            return getimage(path,90,90, MediaStore.Images.Thumbnails.MICRO_KIND);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            if (null != bitmap){
                mImageView.setImageBitmap(bitmap);
            }
        }
        private Bitmap getimage(String srcPath,int width, int height,
                                int kind) {
            Bitmap bitmap = null;
            // 获取视频的缩略图
            bitmap = ThumbnailUtils.createVideoThumbnail(srcPath, kind);
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            return bitmap;//压缩好比例大小后再进行质量压缩
        }

    }
}
