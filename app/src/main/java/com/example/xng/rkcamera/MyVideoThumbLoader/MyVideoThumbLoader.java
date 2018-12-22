package com.example.xng.rkcamera.MyVideoThumbLoader;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

public class MyVideoThumbLoader {
	private ImageView imgView;
	private String path;
	//创建cache
	private LruCache<String, Bitmap> lruCache;

	private Handler mHandler = new Handler(){

		public void handleMessage(Message msg) {

			if(imgView.getTag().equals(path)){
				Bitmap btp = (Bitmap) msg.obj;
				imgView.setImageBitmap(btp);
			}
		}
	};


	@SuppressLint("NewApi")
	public MyVideoThumbLoader(){
		int maxMemory = (int) Runtime.getRuntime().maxMemory();//获取最大的运行内存
		int maxSize = maxMemory /4;
		lruCache = new LruCache<String, Bitmap>(maxSize){
			@Override
			protected int sizeOf(String key, Bitmap value) {
				//这个方法会在每次存入缓存的时候调用
				return value.getByteCount();
			}
		};
	}

	public void addVideoThumbToCache(String path,Bitmap bitmap){
		if(getVideoThumbToCache(path) == null){
//			当前地址没有缓存时，就添加
//			if(path!=null||bitmap!=null){
//				lruCache.put(path, bitmap);
//			}
		}
	}
	public Bitmap getVideoThumbToCache(String path){

		return lruCache.get(path);

	}
	public void showThumbByAsynctack(String path,ImageView imgview){

		if(getVideoThumbToCache(path) == null){
			//异步加载
			new MyBobAsynctack(imgview, path).execute(path);
		}else{
			imgview.setImageBitmap(getVideoThumbToCache(path));
		}

	}

	class MyBobAsynctack extends AsyncTask<String, Void, Bitmap> {
		private ImageView imgView;
		private String path;

		public MyBobAsynctack(ImageView imageView,String path) {
			this.imgView = imageView;
			this.path = path;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bitmap = VideoUtil.createVideoThumbnail(params[0], 70, 50);
			//加入缓存中
			if(getVideoThumbToCache(params[0]) == null){
				addVideoThumbToCache(path, bitmap);
			}
			return bitmap;
		}
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if(imgView.getTag().equals(path)){
				imgView.setImageBitmap(bitmap);
			}
		}
	}

	public void showDateByThread(ImageView imageview,final String path){
		imgView =imageview;
		this.path = path;
		new Thread(new Runnable() {

			@Override
			public void run() {
				Bitmap bitmap = VideoUtil.createVideoThumbnail(path,70, 50);
				Message msg = new Message();
				msg.obj = bitmap;
				msg.what = 1001;
				mHandler.sendMessage(msg);
			}
		}).start();

	}
}
