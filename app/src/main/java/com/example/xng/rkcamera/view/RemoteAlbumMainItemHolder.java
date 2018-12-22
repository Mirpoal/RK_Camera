package com.example.xng.rkcamera.view;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.listener.OnRemoteAlbumMainSelectListener;
import com.example.xng.rkcamera.model.RemoteAlbumMain;
import com.example.xng.rkcamera.utils.AsyncLoadpicTask;
import com.example.xng.rkcamera.utils.BitmapCacheUtils;
import com.example.xng.rkcamera.utils.MediaUtils;

/**
 * Created by waha on 2017/8/7.
 */

public class RemoteAlbumMainItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView txtName;
    private TextView txtNum;
    private ImageView imgPreview;
    private ImageView imgPlay;
    private OnRemoteAlbumMainSelectListener mListener;
    private RemoteAlbumMain mModel;
    private AsyncLoadpicTask mTask;

    public RemoteAlbumMainItemHolder(View itemView, OnRemoteAlbumMainSelectListener listener) {
        super(itemView);

        mListener = listener;
        imgPreview = (ImageView) itemView.findViewById(R.id.img_preview);
        imgPlay = (ImageView) itemView.findViewById(R.id.img_play);
        txtName = (TextView) itemView.findViewById(R.id.txt_name);
        txtNum = (TextView) itemView.findViewById(R.id.txt_num);
        itemView.setOnClickListener(this);
    }

    public void setData(RemoteAlbumMain model) {
        mModel = model;
        txtName.setText(model.getDisplayName());
        txtNum.setText(String.valueOf(model.getMediaNum()));
        if (TextUtils.isEmpty(model.getPreviewPath())) {
            if (null != mTask) {
                mTask.cancel(true);
            }
            imgPreview.setTag("");
            imgPreview.setImageResource(R.drawable.ic_not_pic);
            imgPlay.setVisibility(View.GONE);
            return;
        }
        imgPreview.setTag(model.getPreviewPath());
        if (MediaUtils.isVideoFile(model.getPreviewPath())) {
            imgPlay.setVisibility(View.VISIBLE);
        } else {
            imgPlay.setVisibility(View.GONE);
        }
        if (null != mTask) {
            mTask.cancel(true);
        }
        String displayName = model.getPreviewPath().replace("/", "_");
        Bitmap bitmap = BitmapCacheUtils.getInstance().getBitmapFromCache(displayName);
        if (null != bitmap) {
            if (!bitmap.isRecycled()) {
                imgPreview.setImageBitmap(bitmap);
                return;
            } else {
                Log.v("waha", "bitmap alread recycle");
                BitmapCacheUtils.getInstance().removeImageCache(displayName);
            }
        }
        imgPreview.setImageResource(R.drawable.icon_image_02);
        mTask = new AsyncLoadpicTask(imgPreview);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, model.getPreviewPath(), displayName);
    }

    @Override
    public void onClick(View v) {
        if (null != mListener) {
            mListener.play(mModel);
        }
    }
}
