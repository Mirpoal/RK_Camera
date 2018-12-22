package com.example.xng.rkcamera.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.example.xng.rkcamera.Map.gps.GCJPointer;
import com.example.xng.rkcamera.Map.gps.GpsInfo;
import com.example.xng.rkcamera.Map.gps.GpsParseUtil;
import com.example.xng.rkcamera.Map.gps.WGSPointer;
import com.example.xng.rkcamera.R;

import java.util.ArrayList;

public class MyMapView extends MapView {
    static final String TAG = "MyMapView";

    private Activity mActivity = null;

    private AMap mAMap = null;
    private Marker mMarker = null;
    private UiSettings mUiSettings = null;
    private Polyline mPolyline = null;
    private ArrayList<LatLng> mPointList = new ArrayList<LatLng>();
    private ArrayList<LatLng> mMovePointList = new ArrayList<LatLng>();

    public MyMapView(Context var1) {
        super(var1);
    }

    public MyMapView(Context var1, AttributeSet var2) {
        super(var1, var2);
    }

    public MyMapView(Context var1, AttributeSet var2, int var3) {
        super(var1, var2, var3);
    }

    public MyMapView(Context var1, AMapOptions var2) {
        super(var1, var2);
    }

    public void setActivity(Activity activity) {
            mActivity = activity;
    }

    public void init() {
        if (mAMap == null) {
            mAMap = getMap();
            mUiSettings = mAMap.getUiSettings();
        }

        //mPointList.clear();
        //mMovePointList.clear();
    }

    public void clear() {
        mAMap.clear();
        mMarker = null;
        mPolyline = null;
    }

    public void uiControl(boolean enable) {
        //Log.d(TAG, "uiControl: " + enable);
        if (enable) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            //获取整个屏幕高宽，包含状态栏（显示电量、运营商等），标题栏 (1920, 1080)
            //int height = mActivity.getWindow().getDecorView().getHeight();

            //获取应用区域高宽，包含标题栏 (1776, 1080)
            //int height = mActivity.getWindowManager().getDefaultDisplay().getHeight();

            //获取view绘制区域高宽，不包含标题栏 (1704, 1080)
            Rect outRect = new Rect();
            mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect);
            int height = outRect.height();

            //Log.d(TAG, "height: " + height);

            setVisibility(View.VISIBLE);

            lp.width  = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = height/2;
            setLayoutParams(lp);
            setTranslationY(height/2);

            mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_BUTTOM); //缩放按钮显示右下
        } else {
            setVisibility(View.GONE);
        }

        mUiSettings.setZoomControlsEnabled(enable);//设置地图默认的缩放按钮是否显示
        mUiSettings.setCompassEnabled(enable);    //设置地图默认的指南针是否显示
    }

    public void setMyLocation(boolean enable) {
        //Log.d(TAG, "setMyLocation: " + enable);
        mUiSettings.setMyLocationButtonEnabled(enable); //是否显示默认的定位按钮
        mAMap.setMyLocationEnabled(enable); //是否可触发定位并显示定位层
    }

    //更新实时行车路线
    public void updateRealLine(GpsInfo info) {
        Log.d("VideoPlayerActivity", "updateRealLine");
        if (info.getStatus().equals(GpsParseUtil.VALID_DATA)) {
            LatLng lng = new LatLng(info.getLatitude(), info.getLongitude());
            mPointList.add(lng);

            if (this.getVisibility() == View.VISIBLE) {
                if (mPolyline == null)
                    setStartMarker();

                updateMarker(lng, 16, true);
                mPolyline = mAMap.addPolyline(new PolylineOptions().setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.custtexture)) //setCustomTextureList(bitmapDescriptors)
                        //.setCustomTextureIndex(texIndexList)
                        .addAll(mPointList)
                        //.add(lng)
                        .useGradient(true)
                        .width(18));
            }

        }
    }

    private void updateMarker(LatLng lng, float zoom, boolean isRealLine) {
        if (mMarker == null) {
            //Log.d("VideoPlayerActivity", "mMarker == null");
            mMarker = mAMap.addMarker(new MarkerOptions()
                    .position(lng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

            mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lng, zoom));
            //mAMap.moveCamera(CameraUpdateFactory.zoomTo(zoom));
        } else {
            //Log.d("VideoPlayerActivity", "update mMarker");
            mMarker.setPosition(lng);
        }

        if (isRealLine)
            mAMap.moveCamera(CameraUpdateFactory.changeLatLng(lng));
    }

    //设置车辆移动位置
    public void setMoveLine(int currentPoint) {
        //Log.d(TAG, "currentPoint: " + currentPoint);
        //Log.d("tiantian", "mPointList.size(): " + mPointList.size());

        if (currentPoint > mPointList.size())
            return;
/*
        int i;
        mMovePointList.clear();
        for (i = 0; i <= currentPoint; i++)
            mMovePointList.add(mPointList.get(i));
*/
        if (this.getVisibility() == View.VISIBLE)
            updateMarker(mPointList.get(currentPoint), 13, false);
/*
        if (mPolyline == null) {
            mPolyline = mAMap.addPolyline(new PolylineOptions()
                    .addAll(mMovePointList)
                    //.useGradient(true)
                    .color(Color.RED).width(10));
        } else {
            mPolyline.setPoints(mMovePointList);
        }
*/
    }

    //添加轨迹线
    public void setLine(ArrayList<GpsInfo> gpsInfo) {
        //Log.d(TAG, "setLine");
        //mAMap.clear();

        readLatLngs(gpsInfo);
        if (mPointList.size() <= 0)
            return;

        mPolyline = mAMap.addPolyline(new PolylineOptions().setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.custtexture)) //setCustomTextureList(bitmapDescriptors)
                //.setCustomTextureIndex(texIndexList)
                .addAll(mPointList)
                .useGradient(true)
                .width(18));

        setStartMarker();
        setEndMarker();

        //mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mPointList.get(0), 10));
        //LatLngBounds bounds = new LatLngBounds(mPointList.get(0), mPointList.get(mPointList.size() - 2));
        //mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    //读取坐标点
    private void readLatLngs(ArrayList<GpsInfo> gpsInfo) {
        mPointList.clear();
        for (GpsInfo info : gpsInfo) {
            if (info.getStatus().equals(GpsParseUtil.VALID_DATA))
                mPointList.add(new LatLng(info.getLatitude(), info.getLongitude()));
        }
    }

    private void setStartMarker() {
        if (mPointList.size() > 0) {
            mAMap.addMarker(new MarkerOptions()
                    .position(mPointList.get(0))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
        }
    }

    private void setEndMarker() {
        if (mPointList.size() > 0) {
            mAMap.addMarker(new MarkerOptions()
                    .position(mPointList.get(mPointList.size() - 1))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));
        }
    }

    public void WGStoGCJ(GpsInfo gpsInfo) {
        //Log.d("tiantian", "gps Latitude: " + gpsInfo.getLatitude() + ", gps Longitude: " +  gpsInfo.getLongitude());
        WGSPointer wgs = new WGSPointer(gpsInfo.getLatitude(), gpsInfo.getLongitude());
        GCJPointer gcj = wgs.toGCJPointer();
        gpsInfo.setLatitude(gcj.getLatitude());
        gpsInfo.setLongitude(gcj.getLongitude());
        //Log.d("tiantian", "gcj Latitude: " + gpsInfo.getLatitude() + ", gcj Longitude: " +  gpsInfo.getLongitude());
    }

 /*
    public void fromGpsToAmap(GpsInfo gpsInfo) {
        CoordinateConverter converter = new CoordinateConverter(mActivity);
        converter.from(CoordinateConverter.CoordType.GPS);
        Log.d("tiantian", "gps Latitude: " + gpsInfo.getLatitude() + ", gps Longitude: " +  gpsInfo.getLongitude());
        try {
            converter.coord(new DPoint(gpsInfo.getLatitude(), gpsInfo.getLongitude()));
            DPoint desLatLng = converter.convert();
            gpsInfo.setLatitude(desLatLng.getLatitude());
            gpsInfo.setLongitude(desLatLng.getLongitude());
            Log.d("tiantian", "gcj Latitude: " + gpsInfo.getLatitude() + ", gcj Longitude: " +  gpsInfo.getLongitude());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fromGpsToAmap(GpsInfo gpsInfo) {
        Log.d("tiantian", "gps Latitude: " + gpsInfo.getLatitude() + ", gps Longitude: " +  gpsInfo.getLongitude());
        LatLng latLng = new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude());
        latLng = CoordinateUtil.transformFromWGSToGCJ(latLng);

        gpsInfo.setLatitude(latLng.latitude);
        gpsInfo.setLongitude(latLng.longitude);
        Log.d("tiantian", "gcj Latitude: " + gpsInfo.getLatitude() + ", gcj Longitude: " +  gpsInfo.getLongitude());
    }
    */
}