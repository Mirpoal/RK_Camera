package com.example.xng.rkcamera.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.listener.OnRemoteAlbumMainSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumMain;
import com.example.xng.rkcamera.view.RemoteAlbumHeaderHolder;
import com.example.xng.rkcamera.view.RemoteAlbumItemHolder;
import com.example.xng.rkcamera.view.RemoteAlbumMainItemHolder;

import java.util.List;

/**
 * Created by waha on 2017/8/7.
 */

public class RemoteAlbumMainAdapter extends RecyclerView.Adapter<ViewHolder> {
    private List<RemoteAlbumMain> mDatas;
    private OnRemoteAlbumMainSelectListener mListener;

    public RemoteAlbumMainAdapter(List<RemoteAlbumMain> datas, OnRemoteAlbumMainSelectListener listener) {
        mDatas = datas;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflate = LayoutInflater.from(parent.getContext());
        View v = inflate.inflate(R.layout.remote_album_fragment_item, parent, false);
        return new RemoteAlbumMainItemHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (null != mDatas) {
            ((RemoteAlbumMainItemHolder) holder).setData(mDatas.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }

}
