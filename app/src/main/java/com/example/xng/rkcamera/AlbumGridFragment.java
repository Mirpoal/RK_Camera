package com.example.xng.rkcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xng on 2016/8/8.
 */
public class AlbumGridFragment extends Fragment{
    private ZoomImageView mImgAlbum;
    private GridView mGvMember;
    private MemberAdapter mAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.album_gridview, container,
                false);
        int gridColumns = 4;
        mGvMember = (GridView) view.findViewById(R.id.gvPhotoMember);
        mGvMember.setNumColumns(gridColumns);
        mImgAlbum = (ZoomImageView) view. findViewById(R.id.iv_album);
        mImgAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImgAlbum.setVisibility(View.GONE);
                mGvMember.setVisibility(View.VISIBLE);
            }
        });

        if(null == mAdapter){
            mAdapter = new MemberAdapter(getActivity());
        }
        mGvMember.setAdapter(mAdapter);
        mGvMember.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.cb_checkbox);
                if(checkbox.getVisibility() == View.VISIBLE){
                    checkbox.setChecked(!checkbox.isChecked());
                    List<AlbumModel> list = mAdapter.getMemberList();
                    list.get(position).setFlag(checkbox.isChecked());
                }else {
                    AlbumModel member = (AlbumModel) parent.getItemAtPosition(position);

                    Intent intent = new Intent(getActivity(), ShowBigImage.class);
                    String url = member.getPath();
                    Log.d("tiantian", "url: " + url);
                    intent.putExtra("url", url);
                    getActivity().startActivity(intent);
//                    mImgAlbum.setImageBitmap(BitmapFactory.decodeFile(member.getPath()));
//                    mImgAlbum.setVisibility(View.VISIBLE);
//                    mGvMember.setVisibility(View.GONE);
                }

            }
        });
        return view;
    }
    public void reciveSelect(boolean b){
        mAdapter.mClick = b;
        mAdapter.notifyDataSetChanged();
    }

    public void reciveCancle(boolean b){
        mAdapter.mClick = b;
        mAdapter.notifyDataSetChanged();
    }

    public void reciveDelete(boolean b){
        /**
         * 这里不能直接操作mDownLoadInfoList，需要缓存到一个列表中一起清除，否则会出现错误
         */
        if(b){
           // pb.setVisibility(View.VISIBLE);
            List<AlbumModel> list = mAdapter.getMemberList();
            for (int i = list.size() - 1; i > -1 ; i--){
                if(list.get(i).getFlag()){
                    File f = new File(list.get(i).getPath());
                    boolean mm = f.delete();
                    if(mm){
                        list.remove(i);
                  }
                }
                mAdapter.notifyDataSetChanged();
            }
           // pb.setVisibility(View.GONE);
        }
    }

    public void reciveSelectAll(boolean b){
        List<AlbumModel> list = mAdapter.getMemberList();
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

    private class MemberAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;
        private List<AlbumModel> memberList;
        public Boolean mClick = false;
        private ImageLoader imageLoader = ImageLoader.getInstance(); // Get singleton instance
        private DisplayImageOptions options;

        public List<AlbumModel> getMemberList() {
            return memberList;
        }

        public MemberAdapter(Context context) {
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

        public void freshData(){
            memberList.clear();
            String path = Environment.getExternalStorageDirectory()+"/RkCamera/RkVideo";
            scanFile(memberList,path);
            notifyDataSetChanged();
        }

        private void scanFile(List<AlbumModel> list,String path){
            File file = new File(path);
            if(null != file && file.isFile()){
                if(path.toLowerCase().endsWith(".jpg")
                        || path.toLowerCase().endsWith(".jpeg")
                        || path.toLowerCase().endsWith(".bmp")
                        || path.toLowerCase().endsWith(".png")){
                    list.add(new AlbumModel(path));
                }
            }else if (null != file && file.isDirectory()){
                File[] childFiles = file.listFiles();
                for (File f:childFiles){
                    scanFile(list,f.getAbsolutePath());
                }
            }
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
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.album_grid_item, parent, false);
            }

            AlbumModel member = memberList.get(position);
            ImageView ivImage = (ImageView) convertView.findViewById(R.id.ivphotoImage);
            CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.cb_checkbox);
//            ivImage.setImageResource(R.drawable.test01);
//            MyAsyncTask task = new MyAsyncTask(ivImage,member);
//            task.execute(member.getPath());

            imageLoader.displayImage("file://"+member.getPath(),ivImage, options);

            checkbox.setChecked(memberList.get(position).getFlag());
            checkbox.setVisibility(View.VISIBLE);
//            ivImage.setImageResource(member.getImage());
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


    private class MyAsyncTask extends AsyncTask<String ,String ,Bitmap>{
            private ImageView mImageView;
            private AlbumModel mModel;

        MyAsyncTask(ImageView imageView,AlbumModel model){
            mImageView = imageView;
            mModel = model;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String path = strings[0];
            return getimage(path);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(null != bitmap){
                mImageView.setImageBitmap(bitmap);
            }
        }
        private Bitmap getimage(String srcPath) {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            //开始读入图片，此时把options.inJustDecodeBounds 设回true了
            newOpts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);//此时返回bm为空

            newOpts.inJustDecodeBounds = false;
            int w = newOpts.outWidth;
            int h = newOpts.outHeight;
            //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
            float hh = 200;//这里设置高度为800f
            float ww = 120;//这里设置宽度为480f
            //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
            int be = 1;//be=1表示不缩放
            if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
                be = (int) (newOpts.outWidth / ww);
            } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
                be = (int) (newOpts.outHeight / hh);
            }
            if (be <= 0)
                be = 1;
            newOpts.inSampleSize = be;//设置缩放比例
            //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
            bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
            return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
        }

        private Bitmap compressImage(Bitmap image) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            int options = 100;
            while ( baos.toByteArray().length / 1024>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
                baos.reset();//重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
                options -= 10;//每次都减少10
            }
            ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
            Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
            return bitmap;
        }
    }
}