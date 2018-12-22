package com.example.xng.rkcamera.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.listener.OnRemoteAlbumSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumModel;
import com.example.xng.rkcamera.view.RemoteAlbumHeaderHolder;
import com.example.xng.rkcamera.view.RemoteAlbumItemHolder;

import java.util.List;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumAdapter extends RecyclerView.Adapter<ViewHolder> {
    public static final int TYPE_ITEM = 0;
    public static final int TYPE_HEADER = 1;

    private List<RemoteAlbumModel> mDatas;
    private OnRemoteAlbumSelectListener mListener;

    public RemoteAlbumAdapter(List<RemoteAlbumModel> datas, OnRemoteAlbumSelectListener listener) {
        mDatas = datas;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            LayoutInflater inflate = LayoutInflater.from(parent.getContext());
            View v = inflate.inflate(R.layout.remote_album_header_layout, parent, false);
            return new RemoteAlbumHeaderHolder(v);
        }
        LayoutInflater inflate = LayoutInflater.from(parent.getContext());
        View v = inflate.inflate(R.layout.remote_album_item_layout, parent, false);
        return new RemoteAlbumItemHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (null != mDatas) {
            if (holder instanceof RemoteAlbumItemHolder) {
                ((RemoteAlbumItemHolder) holder).setData(mDatas.get(position));
            } else if (holder instanceof RemoteAlbumHeaderHolder) {
                ((RemoteAlbumHeaderHolder) holder).setData(mDatas.get(position));
            }
        }
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (null != mDatas) {
            return mDatas.get(position).getItemType();
        }
        return super.getItemViewType(position);
    }

}
