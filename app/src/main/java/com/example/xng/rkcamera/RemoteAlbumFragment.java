package com.example.xng.rkcamera;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xng.rkcamera.adapter.RemoteAlbumMainAdapter;
import com.example.xng.rkcamera.listener.OnRemoteAlbumMainSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumMain;
import com.example.xng.rkcamera.utils.BitmapCacheUtils;
import com.example.xng.rkcamera.utils.MediaUtils;
import com.example.xng.rkcamera.view.LoadingView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumFragment extends Fragment implements OnRemoteAlbumMainSelectListener {
    private static final int MSG_ALBUM_SCAN_START = 0x0001;
    private static final int MSG_ALBUM_SCAN_FINISH = 0x0002;

    private LoadingView mLoadingView;
    private RecyclerView rvAlbumMain;
    private RemoteAlbumMainAdapter mAdapter;
    private List<RemoteAlbumMain> mList = new ArrayList<>();
    private String mRemotePath;
    private ScanThread mScanThread;
    private Object mLock = new Object();
    private boolean mViewFinish = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ALBUM_SCAN_START:
                    if (null != mLoadingView && !mLoadingView.isShowing()) {
                        mLoadingView.show();
                    }
                    break;
                case MSG_ALBUM_SCAN_FINISH:
                    if (null != mAdapter) {
                        mAdapter.notifyDataSetChanged();
                    }
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRemotePath = getContext().getResources().getString(R.string.remote_album_path);
        mLoadingView = new LoadingView(getActivity(),
                getResources().getString(R.string.scaning_file));
        mHandler.sendEmptyMessage(MSG_ALBUM_SCAN_START);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.remote_album_fragment, null);
        rvAlbumMain = (RecyclerView) view.findViewById(R.id.rv_album_main);
        rvAlbumMain.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvAlbumMain.setLayoutManager(layoutManager);
        mAdapter = new RemoteAlbumMainAdapter(mList, this);
        rvAlbumMain.setAdapter(mAdapter);
        refresh();
        mViewFinish = true;
        return view;
    }

    private void refresh() {
        if (null != mAdapter) {
            rvAlbumMain.scrollToPosition(0);
        }
        if (null != mScanThread) {
            mScanThread.setCancel(true);
        }
        mScanThread = new ScanThread();
        mScanThread.start();
    }

    @Override
    public void onDestroyView() {
        if (null != mScanThread) {
            mScanThread.setCancel(true);
        }
        BitmapCacheUtils.getInstance().clearCache();
        mViewFinish = false;
        super.onDestroyView();
    }

    @Override
    public void play(RemoteAlbumMain model) {
        Intent intent = new Intent(getContext(), RemoteAlbumActivity.class);
        intent.putExtra("path", model.getPath());
        startActivity(intent);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden && mViewFinish){
            if (null != mScanThread) {
                mScanThread.setCancel(true);
            }
            mScanThread = new ScanThread();
            mScanThread.start();
        }
    }

    private class ScanThread extends Thread {
        private boolean mCancel;

        public ScanThread() {
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
        }

        @Override
        public void run() {
            mHandler.sendEmptyMessage(MSG_ALBUM_SCAN_START);
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<RemoteAlbumMain> list = new ArrayList<>();
            if (!mCancel) {
                scanFile2List(mRemotePath, list);
            }
            if (null != mAdapter && null != list && !mCancel) {
                synchronized (mLock) {
                    mList.clear();
                    mList.addAll(list);
                }
            }
            if (!mCancel) {
                mHandler.sendEmptyMessage(MSG_ALBUM_SCAN_FINISH);
            }
        }

        private void scanFile2List(String path, List<RemoteAlbumMain> list) {
            File root = new File(path);
            if (root.exists() && root.isDirectory()) {
                File[] childs = root.listFiles();
                if(null == childs){
                    return;
                }
                for (int i = 0; i < childs.length; i++) {
                    File child = childs[i];
                    if (child.isDirectory()) {
                        RemoteAlbumMain model = new RemoteAlbumMain();
                        model.setPath(child.getAbsolutePath());
                        String displayname = child.getName().substring(
                                child.getName().lastIndexOf("/") + 1/*, child.length() - suffix.length()*/);
                        model.setDisplayName(displayname);
                        int mediaNum = setMediaNumAndPrepareFromPath(child.getAbsolutePath(), model);
                        model.setMediaNum(mediaNum);
                        list.add(model);
                    }
                }
            }
        }

        private int setMediaNumAndPrepareFromPath(String path, RemoteAlbumMain model) {
            int num = 0;
            File root = new File(path);
            if (root.exists() && root.isDirectory()) {
                File[] childs = root.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    File child = childs[i];
                    if (child.isDirectory()) {
                        num += setMediaNumAndPrepareFromPath(child.getAbsolutePath(), model);
                    } else if (MediaUtils.isVideoFile(child.getName())
                            || MediaUtils.isImageFile(child.getName())) {
                        if (TextUtils.isEmpty(model.getPreviewPath())) {
                            model.setPreviewPath(child.getAbsolutePath());
                        }
                        num++;
                    }
                }
            }
            return num;
        }
    }

}
