package com.example.xng.rkcamera.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

/**
 * Created by waha on 2017/5/16.
 */

public class BitmapCacheUtils {
    private LruCache<String, Bitmap> mCache;
    private final int MAX_CACHE_NUM = 15;
    private static BitmapCacheUtils instance;

    public static BitmapCacheUtils getInstance() {
        if (null == instance) {
            instance = new BitmapCacheUtils();
        }

        return instance;
    }

    private BitmapCacheUtils() {
        if (null == mCache) {
            mCache = new LruCache<String, Bitmap>(MAX_CACHE_NUM);
        }
    }

    public synchronized void clearCache() {
        if (mCache != null && mCache.size() > 0) {
            mCache.evictAll();
        }
    }

    public synchronized void addBitmapToCache(String key, Bitmap bitmap) {
        if (!TextUtils.isEmpty(key) && null != bitmap && null == mCache.get(key)) {
            mCache.put(key, bitmap);
        }
    }

    public synchronized Bitmap getBitmapFromCache(String key) {
        if (!TextUtils.isEmpty(key)) {
            return mCache.get(key);
        }
        return null;
    }

    public synchronized void removeImageCache(String key) {
        if (!TextUtils.isEmpty(key) && null != mCache) {
            Bitmap bm = mCache.remove(key);
            if (null != bm && !bm.isRecycled()) {
                bm.recycle();
            }
        }
    }
}
