package com.example.xng.rkcamera.utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

/**
 * Created by waha on 2017/8/4.
 */

public class AsyncLoadpicTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView mImageView;
    private String mUrl;
    private String mDisplayName;
    private int mPicWidth = 150;
    private int mPicHeight = 100;
    private BitmapCacheUtils mCacheUtils;

    public AsyncLoadpicTask(ImageView imageView) {
        mImageView = imageView;
        mCacheUtils = BitmapCacheUtils.getInstance();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        mUrl = params[0];
        mDisplayName = params[1];
        if (MediaUtils.isVideoFile(mDisplayName)) {
            return loadVideoBitmap(mUrl, mDisplayName);
        }
        return loadPicBitmap(mUrl, mDisplayName);
    }

    private Bitmap loadVideoBitmap(String path, String displayName) {
        Bitmap bitmap = null;
        Bitmap oldBitmap = null;
        try {
            oldBitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
            bitmap = ThumbnailUtils.extractThumbnail(oldBitmap, mPicWidth, mPicHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != oldBitmap && !oldBitmap.isRecycled()) {
                oldBitmap.recycle();
                oldBitmap = null;
            }
        }
        if (null != bitmap) {
            mCacheUtils.addBitmapToCache(displayName, bitmap);
        }
        return bitmap;
    }

    private Bitmap loadPicBitmap(String path, String displayName) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaUtils.decodeSampledBitmapFromPath(path, mPicWidth, mPicHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != bitmap) {
            mCacheUtils.addBitmapToCache(displayName, bitmap);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        super.onPostExecute(bmp);

        if (mImageView != null && null != bmp
                && !isCancelled()
                && null != mImageView.getTag()
                && mUrl.equals(mImageView.getTag().toString())) {
            mImageView.setImageBitmap(bmp);
        }
    }
}
