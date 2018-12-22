package com.example.xng.rkcamera;

/**
 * Created by Xng on 2016/8/4.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.xng.rkcamera.DownLoadServer.DownloadService;
import com.example.xng.rkcamera.Map.offlinemap.OfflineMapActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingFragment extends Fragment{
    private ListView mLvMember;
    private SettingAdapter mAdapter;
    private ImageLoader mImageLoader = ImageLoader.getInstance(); // Get singleton instance
    private DisplayImageOptions mOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initImageLoader();
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.setting_fragment, container,
                    false);
            mLvMember = (ListView)view.findViewById(R.id.lvSettingMember);
            if(null == mAdapter){
                mAdapter = new SettingAdapter(getActivity());
            }
            mLvMember.setAdapter(mAdapter);

            mLvMember.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    switch (position){ //存储管理
                        case 0:
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getActivity().getString(R.string.clear_cache_msg))
                                   .setNegativeButton(getActivity().getString(R.string.no), null);

                            builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    clearImageLoaderCache();
                                    deleteAllFiles(new File(Environment.getExternalStorageDirectory() + "/RkCamera/RkCache"));
                                    deleteAllFiles(new File(DownloadService.DOWNLOAD_PATH));
                                }
                            });
                            //添加AlertDialog.Builder对象的setNegativeButton()方法
                            builder.create().show();
                            break;

                        case 1: //语言
                            startActivity(new Intent(getActivity(),LanguageChange.class));
                            break;

                        case 2: //离线地图
                            startActivity(new Intent(getActivity(), OfflineMapActivity.class));
                            break;
                    }
                }
            });

            return view;
        }

    public static void deleteAllFiles(File root){
        if (root.exists()) {
            File files[] = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    try {
                        if (f.isDirectory()) {
                            deleteAllFiles(f);
                        } else if (f.isFile()) {
                            if (f.exists())
                                f.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            root.delete();
        }
    }


    public void switchLanguage(Locale locale) {
        Resources resources = getResources();// 获得res资源对象
        Configuration config = resources.getConfiguration();// 获得设置对象
        DisplayMetrics dm = resources.getDisplayMetrics();// 获得屏幕参数：主要是分辨率，像素等。
        config.locale = locale; // 简体中文
        resources.updateConfiguration(config, dm);
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
        //Log.d("tiantian", "clear mImageLoader cache");
        if (mImageLoader != null) {
            mImageLoader.clearMemoryCache();
            mImageLoader.clearDiskCache();
        }
    }

    private class SettingAdapter extends BaseAdapter{
        private LayoutInflater layoutInflater;
        private List<SettingModel> memberList;

        public SettingAdapter(Context context){
            layoutInflater = LayoutInflater.from(context);
            memberList=new ArrayList<>();
            memberList.add(new SettingModel(R.drawable.manger,R.string.storage_management,R.drawable.setting_next_img));
            memberList.add(new SettingModel(R.drawable.language,R.string.language,R.drawable.setting_next_img));
            memberList.add(new SettingModel(R.drawable.language,R.string.offline_map,R.drawable.setting_next_img));
        }
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
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.setting_item, parent, false);
            }

            SettingModel member = memberList.get(position);
            ImageView ivImage = (ImageView) convertView
                    .findViewById(R.id.ivSetImage);
            ivImage.setImageResource(member.getImage());

            TextView tvName = (TextView) convertView
                    .findViewById(R.id.tvSet);
            tvName.setText(member.getTitle());

            ImageView setimageView = (ImageView) convertView.findViewById(R.id.ivSetNext);
            setimageView.setImageResource(member.getSetimage());
            return convertView;
        }
    }
}