package com.example.xng.rkcamera.MyVideoThumbLoader;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.io.File;

/**
 * Created by Xng on 2016/12/30.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        Log.d("MainApplication", "onCreate");
        super.onCreate();
        initConfig();
    }
    /**
     * 配置ImageLoader基本属性,最好放在Application中(只能配置一次,如多次配置,则会默认第一次的配置参数)
     */
    private void initConfig() {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .threadPriority(Thread.NORM_PRIORITY - 2)//设置线程优先级
                .threadPoolSize(4)//线程池内加载的数量,推荐范围1-5内。
                .denyCacheImageMultipleSizesInMemory()//当同一个Uri获取不同大小的图片缓存到内存中时只缓存一个。不设置的话默认会缓存多个不同大小的图片
                .memoryCacheExtraOptions(480, 800)//内存缓存文件的最大长度
                .memoryCache(new LruMemoryCache(10 * 1024 * 1024))//内存缓存方式,这里可以换成自己的内存缓存实现。(推荐LruMemoryCache,道理自己懂的)
                .memoryCacheSize(10 * 1024 * 1024)//内存缓存的最大值
                .diskCacheFileCount(500)
                .diskCache(new UnlimitedDiskCache(createSavePath()))//可以自定义缓存路径
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())//对保存的URL进行加密保存
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                .imageDownloader(new BaseImageDownloader(getApplicationContext(), 5 * 1000, 30 * 1000))//设置连接时间5s,超时时间30s
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);
    }

    /**
     * 创建存储缓存的文件夹路径
     *
     * @return
     */
    private File createSavePath() {
        String path;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            path = Environment.getExternalStorageDirectory().getPath() + "/RkCamera/RkCache/";
        } else {
            path = "/RkCamera/RkCache/";
        }
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}
