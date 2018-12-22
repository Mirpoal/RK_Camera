package com.example.xng.rkcamera.DownLoadServer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.xng.rkcamera.DownLoadModel;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.example.xng.rkcamera.SettingFragment;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class DownloadTask
{
	static final String TAG = "DownloadTask";

	private Context mContext = null;
	private DownLoadModel mFileInfo = null;
	private ThreadDAO mDao = null;
	private int mFinised = 0;
	public boolean isPause = false;
	public boolean isDeleteFile = false;
	private int mThreadCount = 1;
	private List<DownloadThread> mDownloadThreadList = new ArrayList<DownloadThread>();;

	private int mWriteCount = 0;
	private String mWriteString = null;
	
	/** 
	 *@param mContext
	 *@param mFileInfo
	 */
	public DownloadTask(Context mContext, DownLoadModel mFileInfo, int count)
	{
		this.mContext = mContext;
		this.mFileInfo = mFileInfo;
		this.mThreadCount = count;
		//mDao = new ThreadDAOImpl(mContext);
	}
	
	public void downLoad()
	{
		String str[] = null;
		//List<ThreadInfo> threads = mDao.getThreads(mFileInfo.getFileUrl());
		List<ThreadInfo> threads = new ArrayList<ThreadInfo>();

		ThreadInfo threadInfo = null;

		if (mFileInfo.getDownLoadProgress() != null) {
			str = mFileInfo.getDownLoadProgress().split("&");
		}
		//Log.d("tiantian", "downLoad, mFileInfo.getDownLoadProgress(): " + mFileInfo.getDownLoadProgress());

		//if (0 == threads.size()) {
            //获得每个线程下载的长度
			//Log.d("tiantian", "mThreadCount: " + mThreadCount);
			int len = Integer.parseInt(mFileInfo.getDownloadSize()) / mThreadCount;
			for (int i = 0; i < mThreadCount; i++) {
                //创建线程信息
				threadInfo = new ThreadInfo(i,mFileInfo.getFileUrl(),
						len * i, (i + 1) * len - 1, 0);
				
				if (mThreadCount - 1 == i) {
					threadInfo.setEnd(Integer.parseInt(mFileInfo.getDownloadSize()));
				}
                //添加到线程信息集合中
				threads.add(threadInfo);
				//mDao.insertThread(threadInfo);
			}
		//}

		int threadIndex = 0;
		for (ThreadInfo info : threads) {
			if (str != null) {
				for (String str1 : str) {
					String tmp[] = str1.split(":");
					if (Integer.parseInt(tmp[0]) == threadIndex) {
						info.setFinished(Integer.parseInt(tmp[1]));
						break;
					}
				}
			}

			//Log.d("tiantian", "threadIndex: " + threadIndex);
			DownloadThread thread = new DownloadThread(info);
			thread.start();
			mDownloadThreadList.add(thread);
			threadIndex++;
		}
	}

	private class DownloadThread extends Thread
	{
		private ThreadInfo mThreadInfo = null;
		public boolean isFinished = false;//线程是否下载完毕

		/** 
		 *@param mInfo
		 */
		public DownloadThread(ThreadInfo mInfo)
		{
			this.mThreadInfo = mInfo;
			//Log.d("tiantian", "new DownloadThread");
		}
		
		/**
		 * @see Thread#run()
		 */
		@Override
		public void run()
		{
			HttpURLConnection connection = null;
			RandomAccessFile raf = null;
			InputStream inputStream = null;
			
			try
			{
				//Log.d(TAG, "mThreadInfo.getUrl(): " + mThreadInfo.getUrl());
				URL url = new URL(mThreadInfo.getUrl());
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(5000);
				connection.setRequestMethod("GET");

				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				//Log.d("tiantian", mFileInfo.getXmlFileName() + "---" + mThreadInfo.getId() + "---" + mThreadInfo.getFinished());
				connection.setRequestProperty("Range", "bytes=" + start + "-" + mThreadInfo.getEnd());
				File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
				//Log.d(TAG, mFileInfo.getFileName() + " download");
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);

				Intent intent = new Intent();
				intent.setAction(DownloadService.ACTION_UPDATE);
				mFinised += mThreadInfo.getFinished();
				//Log.i(TAG, mThreadInfo.getId() + "finished = " + mThreadInfo.getFinished());
				if (connection.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT) {
					inputStream = connection.getInputStream();
					byte buf[] = new byte[1024 << 2];
					int len = -1;
					long time = System.currentTimeMillis();
					while ((len = inputStream.read(buf)) != -1) {
						raf.write(buf, 0, len);
						mFinised += len;
						mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
						if (System.currentTimeMillis() - time > 1000) {
							time = System.currentTimeMillis();
							int f = (int) ((long)mFinised  * 100 / Integer.parseInt(mFileInfo.getDownloadSize()));
							if (f > mFileInfo.getFinished()) {
								intent.putExtra("finished", f);
								intent.putExtra("fileInfo", mFileInfo);
								//intent.putExtra("id", mFileInfo.getId());
								mContext.sendBroadcast(intent);
							}
						}

						//Log.d("tiantian", mFileInfo.getXmlFileName() + "---" + mThreadInfo.getId() + "---isPause: " + isPause);
						if (isPause) {
							//Log.d("tiantian", "isPause");
							//mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
							//Log.d("tiantian", mFileInfo.getXmlFileName() + "---" + mThreadInfo.getId());
							//Log.i("tiantian", mFileInfo.getXmlFileName() + "---" + mThreadInfo.getId() + "---finished = " + mThreadInfo.getFinished());
							//mDao.deleteThread(mFileInfo.getFileUrl());

							saveDownloadProgress(mThreadInfo);
							httpDeInit(connection, raf, inputStream);
							return;
						}
					}
					
					//标识线程执行完毕
					isFinished = true;
					checkAllThreadFinished();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				httpDeInit(connection, raf, inputStream);
			}
		}
	}

	private void saveDownloadProgress(ThreadInfo threadInfo)
	{
		//Log.d("tiantian", "saveDownloadProgress");
		mWriteCount += 1;

		try {
			if (mWriteCount == mThreadCount) {
				if (!isDeleteFile) {
					if (mWriteString != null)
						mWriteString += Integer.toString(threadInfo.getId())  + ":" + Integer.toString(threadInfo.getFinished()) + "&";
					else
						mWriteString = Integer.toString(threadInfo.getId())  + ":" + Integer.toString(threadInfo.getFinished()) + "&";

					File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getXmlFileName());
					if (!file.exists())
						file.createNewFile();

					byte bt[] = new byte[100];
					bt = mWriteString.getBytes();

					Log.d(TAG, mFileInfo.getXmlFileName() + ", " + "mWriteString: " + mWriteString);
					FileOutputStream out = new FileOutputStream(file);
					out.write(bt, 0, bt.length);
					out.flush();
					out.close();
				} else {
					File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
					if (file.exists()) {
						//Log.d(TAG, mFileInfo.getFileName() + " exists, delete");
						file.delete();
					}

					if (mFileInfo.getMapGpsStoragePath() != null) {
						File gpsFile = new File(mFileInfo.getMapGpsStoragePath(), GpsInfo.COUNT_FILE_NAME);
						if (!gpsFile.exists()) { //count 文件不存在，证明只有正在下载的MP4匹配这个时间段的GPS文件
							Log.d(TAG, "count file not exists, delete map gps dir");
							SettingFragment.deleteAllFiles(new File(mFileInfo.getMapGpsStoragePath()));
						}
					}
				}

				Intent intent = new Intent(DownloadService.ACTION_PAUSE);
				mContext.sendBroadcast(intent);
			}
		} catch (IOException e) {
			e.printStackTrace();
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

    //判断是否所有线程都执行完毕
	private synchronized void checkAllThreadFinished()
	{
		boolean allFinished = true;

        //遍历线程集合，判断线程是否都执行完毕
		for (DownloadThread thread : mDownloadThreadList) {
			if (!thread.isFinished) {
				allFinished = false;
				break;
			}
		}
		
		if (allFinished) {
			//mDao.deleteThread(mFileInfo.getFileUrl());
			Intent intent = new Intent(DownloadService.ACTION_FINISHED);
			intent.putExtra("fileInfo", mFileInfo);
			mContext.sendBroadcast(intent);
		}
	}
}
