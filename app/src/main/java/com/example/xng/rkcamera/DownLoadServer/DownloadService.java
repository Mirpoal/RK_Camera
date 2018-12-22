package com.example.xng.rkcamera.DownLoadServer;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.xng.rkcamera.DownLoadModel;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class DownloadService extends Service
{
	private String TAG = "DownloadService";

	public static final String DOWNLOAD_PATH =
			Environment.getExternalStorageDirectory().getAbsolutePath() + "/RkCamera/RkVideo/tempdata";
	public static final String ACTION_START = "ACTION_START";
	public static final String ACTION_PAUSE = "ACTION_PAUSE";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final String ACTION_FINISHED = "ACTION_FINISHED";
	public static final int MSG_INIT = 0;
	private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<Integer, DownloadTask>();
	
	/**
	 * @see Service#onStartCommand(Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(null == intent){
			return super.onStartCommand(intent, flags, startId);
		}

		File dir = new File(DOWNLOAD_PATH);
		if (!dir.exists())
			dir.mkdirs();

		if (ACTION_START.equals(intent.getAction())) {
			DownLoadModel fileInfo = (DownLoadModel) intent.getSerializableExtra("fileInfo");
			Log.i(TAG , "Start:" + fileInfo.toString());
			mHandler.obtainMessage(MSG_INIT, fileInfo).sendToTarget();
		} else if (ACTION_STOP.equals(intent.getAction())) {
			DownLoadModel fileInfo = (DownLoadModel) intent.getSerializableExtra("fileInfo");
			Log.i(TAG, "Stop:" + fileInfo.toString());
			DownloadTask task = mTasks.get(fileInfo.getId());
			if (task != null) {
				task.isPause = true;
				task.isDeleteFile = (Boolean) intent.getSerializableExtra("isDeleteFile");
			}
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case MSG_INIT:
					DownLoadModel fileInfo = (DownLoadModel) msg.obj;
					DownloadTask task = new DownloadTask(DownloadService.this, fileInfo, 1/*3*/);
					task.downLoad();
                    //把下载任务添加到集合中
					mTasks.put(fileInfo.getId(), task);
					break;
				default:
					break;
			}
		};
	};

	/**
	 * @see Service#onBind(Intent)
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

}
