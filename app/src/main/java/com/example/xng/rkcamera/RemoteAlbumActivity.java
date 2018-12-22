package com.example.xng.rkcamera;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xng.rkcamera.adapter.RemoteAlbumAdapter;
import com.example.xng.rkcamera.listener.OnRemoteAlbumSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumModel;
import com.example.xng.rkcamera.utils.BitmapCacheUtils;
import com.example.xng.rkcamera.view.LoadingView;
import com.example.xng.rkcamera.utils.MediaUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumActivity extends Activity implements
        View.OnClickListener, OnRemoteAlbumSelectListener {
    private static final int MSG_ALBUM_SCAN_START = 0x0001;
    private static final int MSG_ALBUM_SCAN_FINISH = 0x0002;

    private TextView txtAlbumWarn;
    private Button btnback;
    private RecyclerView rvAlbum;
    private RemoteAlbumAdapter mAdapter;
    private ScanThread mScanThread;
    private final int MAX_COLUMN = 3;
    private boolean mViewFinish;
    private List<RemoteAlbumModel> mList = new ArrayList<>();
    private LoadingView mLoadingView;
    private Object mLock = new Object();
    private String mRemotePath;
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
                    if (mAdapter.getItemCount() > 0) {
                        txtAlbumWarn.setVisibility(View.GONE);
                        rvAlbum.setVisibility(View.VISIBLE);
                    } else {
                        rvAlbum.setVisibility(View.GONE);
                        txtAlbumWarn.setVisibility(View.VISIBLE);
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
        setContentView(R.layout.remote_album_activity);

        mLoadingView = new LoadingView(this,
                getResources().getString(R.string.scaning_file));
        mRemotePath = getIntent().getStringExtra("path");
        initView();
    }

    private void initView() {
        txtAlbumWarn = (TextView) findViewById(R.id.txt_album_warn);
        rvAlbum = (RecyclerView) findViewById(R.id.rv_album);
        btnback = (Button) findViewById(R.id.main_back);
        btnback.setOnClickListener(this);

        GridLayoutManager manager = new GridLayoutManager(this, MAX_COLUMN);
        rvAlbum.setHasFixedSize(true);
//        rvAlbum.addItemDecoration(new DividerItemDecoration(getActivity(),
//                LinearLayoutManager.VERTICAL));
        rvAlbum.setLayoutManager(manager);
        mAdapter = new RemoteAlbumAdapter(mList, this);
        rvAlbum.setAdapter(mAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = mList.get(position).getItemType();
                //指跨度，实际上的大小可以理解为 返回值×(1/spanCount)
                return type == RemoteAlbumAdapter.TYPE_HEADER ? MAX_COLUMN : 1;
            }
        });
        refresh();
        mViewFinish = true;
    }

    private void refresh() {
        if (null != mAdapter) {
            rvAlbum.scrollToPosition(0);
        }
        if (null != mScanThread) {
            mScanThread.setCancel(true);
        }
        mScanThread = new ScanThread();
        mScanThread.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_back:
                finish();
                break;
        }
    }

    @Override
    public void play(RemoteAlbumModel model) {
        boolean isPic = MediaUtils.isImageFile(model.getPath());
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("file:///" + model.getPath());
            intent.setDataAndType(uri, isPic ? "image/*" : "video/*");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, isPic ? R.string.not_png_app : R.string.not_video_app,
                    Toast.LENGTH_SHORT).show();
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
            List<RemoteAlbumModel> list = new ArrayList<>();
            if (!mCancel) {
                scanFile2List(mRemotePath, list);
            }
            if (!mCancel) {
                addHeader2List(list);
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

        private void scanFile2List(String path, List<RemoteAlbumModel> list) {
            File root = new File(path);
            String parentname = path.substring(path.lastIndexOf("/") + 1);
            if (root.exists() && root.isDirectory()) {
                File[] childs = root.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    File child = childs[i];
                    if (child.isDirectory()) {
                        scanFile2List(child.getAbsolutePath(), list);
                    } else if (MediaUtils.isVideoFile(child.getName())
                            || MediaUtils.isImageFile(child.getName())) {
                        String displayname = child.getName().substring(
                                child.getName().lastIndexOf("/") + 1/*, child.length() - suffix.length()*/);
                        RemoteAlbumModel model = new RemoteAlbumModel(RemoteAlbumAdapter.TYPE_ITEM);
                        model.setDisplayName(displayname);
                        model.setParentName(parentname);
                        model.setPath(child.getAbsolutePath());
                        model.setFileSize(MediaUtils.getSizeWithUnit(child.length()));
                        list.add(model);
                    }
                }
            }
        }

        private void addHeader2List(List<RemoteAlbumModel> list) {
            if (null == list) {
                return;
            }
            String header = "";
            for (int i = 0; i < list.size(); i++) {
                String curHeader = list.get(i).getParentName();
                if (!header.equals(curHeader)) {
                    header = curHeader;
                    RemoteAlbumModel model = new RemoteAlbumModel(RemoteAlbumAdapter.TYPE_HEADER);
                    model.setDisplayName(curHeader);
                    list.add(i, model);
                    i++;
                }
            }
        }

    }

    @Override
    protected void onDestroy() {
        if (null != mScanThread) {
            mScanThread.setCancel(true);
        }
        BitmapCacheUtils.getInstance().clearCache();
        super.onDestroy();
    }
}

