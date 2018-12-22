package com.example.xng.rkcamera.view;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.listener.OnRemoteAlbumSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumModel;
import com.example.xng.rkcamera.utils.AsyncLoadpicTask;
import com.example.xng.rkcamera.utils.BitmapCacheUtils;
import com.example.xng.rkcamera.utils.MediaUtils;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView txtName;
    private TextView txtSize;
    private ImageView imgAlbum;
    private ImageView imgPlay;
    private LinearLayout layoutName;
    private OnRemoteAlbumSelectListener mListener;
    private RemoteAlbumModel mModel;
    private AsyncLoadpicTask mTask;

    public RemoteAlbumItemHolder(View itemView, OnRemoteAlbumSelectListener listener) {
        super(itemView);

        mListener = listener;
        imgAlbum = (ImageView) itemView.findViewById(R.id.img_album);
        txtName = (TextView) itemView.findViewById(R.id.txt_name);
        txtSize = (TextView) itemView.findViewById(R.id.txt_size);
        imgPlay = (ImageView) itemView.findViewById(R.id.img_play);
        layoutName = (LinearLayout) itemView.findViewById(R.id.layout_name);
        layoutName.bringToFront();
        itemView.setOnClickListener(this);
    }

    public void setData(RemoteAlbumModel model) {
        mModel = model;
        txtName.setText(model.getDisplayName());
        txtSize.setText(model.getFileSize());
        imgAlbum.setTag(model.getPath());
        if (MediaUtils.isVideoFile(model.getDisplayName())) {
            imgPlay.setVisibility(View.VISIBLE);
        } else {
            imgPlay.setVisibility(View.GONE);
        }
        if (null != mTask) {
            mTask.cancel(true);
        }
        String displayName = model.getPath().replace("/", "_");
        Bitmap bitmap = BitmapCacheUtils.getInstance().getBitmapFromCache(displayName);
        if (null != bitmap) {
            if (!bitmap.isRecycled()) {
                imgAlbum.setImageBitmap(bitmap);
                return;
            } else {
                Log.v("waha", "bitmap alread recycle");
                BitmapCacheUtils.getInstance().removeImageCache(displayName);
            }
        }
        imgAlbum.setImageResource(R.drawable.icon_image_02);
        mTask = new AsyncLoadpicTask(imgAlbum);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, model.getPath(), displayName);
    }

    @Override
    public void onClick(View v) {
        if (null != mListener) {
            mListener.play(mModel);
        }
    }
}
