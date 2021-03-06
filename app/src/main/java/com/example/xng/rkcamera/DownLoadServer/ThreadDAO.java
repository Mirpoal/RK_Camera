package com.example.xng.rkcamera.DownLoadServer;

import java.util.List;


public interface ThreadDAO
{
	public void insertThread(ThreadInfo threadInfo);

	public void deleteThread(String url);

	public void updateThread(String url, int thread_id, int finished);

	public List<ThreadInfo> getThreads(String url);

	public boolean isExists(String url, int thread_id);
}
