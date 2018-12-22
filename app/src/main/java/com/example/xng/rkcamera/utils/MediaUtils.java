package com.example.xng.rkcamera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

/**
 * Created by waha on 2017/8/4.
 */

public class MediaUtils {

    public static String getSizeWithUnit(long b) {
        float kb = (float) (b / 1024);
        if (kb > 1024) {
            float mb = kb / 1024;
            if (mb > 1024) {
                return String.format("%.2f", mb / 1024) + "G";
            }
            return String.format("%.2f", mb) + "M";
        }
        return String.format("%.0f", kb) + "K";
    }

    public static boolean isVideoFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        if (path.endsWith(".mp4")
                || path.endsWith(".mkv")) {
            return true;
        }
        return false;
    }

    public static boolean isImageFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        if (path.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInsampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int caculateInsampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
