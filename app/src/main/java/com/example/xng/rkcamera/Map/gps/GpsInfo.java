package com.example.xng.rkcamera.Map.gps;

public class GpsInfo {
    static final String TAG = "GpsInfo";

    /* gps message */
    public static final int MSG_GPS_START_POINT = 97;
    public static final int MSG_GPS_INFO_LIST = 98;
    public static final int MSG_GPS_FILE_DOWNLOAD_FINISH = 99;
    public static final String CMD_ACK_GET_GPS_LIST = "CMD_ACK_GET_GPS_LIST";
    public static final String CMD_ACK_GET_GPS_LIST_END = "CMD_ACK_GET_GPS_LIST_END";
    public static final String CMD_GET_GPS_LIST = "CMD_GET_GPS_LIST:";
    public static final String CMD_STOP_GET_GPS_LIST = "CMD_STOP_GET_GPS_LIST";
    public static final String COUNT_FILE_NAME = "count";

    /* gps info list position */
    private int mId;

    /* Mapping to the point of the route on the map */
    private int mMapPointId;

    /* A: data valid, V: data invalid */
    private String mStatus;

    /* ddmm.mmmmm, 0000.00000~8959.9999, d is degree, m is minute */
    private double mLatitude;
    /* N: north, S: south */
    private String mLatitudeIndicator;

    /* dddmm.mmmmm, 00000.00000~17959.9999, d is degree, m is minute */
    private double mLongitude;
    /* E: east, W: west */
    private String mLongitudeIndicator;

    /* speed over ground, 000.0~999.9(nmi/h) */
    private float mSpeed;

    /* course over ground, 000.0~359.9 */
    private float mDirection;

    /* MSL altitude, -9999.9~99999.9 */
    private float mAltitude;

    /* number of satellites: 00~12 */
    private int mSatellites;

    /* 0 fix not available or invalid
     * 1 GPS SPS mode,fix valid
     * 2 differential GPS,SPS mode,fix valid
     * 3 GPS PPS mode,fix valid
     */
    private int mPositionFixIndicator;

    private DateTime mDateTime = new DateTime();

    public void setId(int id) {
        mId = id;
    }
    public int getId() {
        return mId;
    }

    public void setMapPointId(int id) {
        mMapPointId = id;
    }
    public int getMapPointId() {
        return mMapPointId;
    }

    public void setStatus(String status) {
        mStatus = status;
    }
    public String getStatus() {
        return mStatus;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }
    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitudeIndicator(String latitudeIndicator) {
        mLatitudeIndicator = latitudeIndicator;
    }
    public String getLatitudeIndicator() {
        return mLatitudeIndicator;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }
    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitudeIndicator(String longitudeIndicator) {
        mLongitudeIndicator = longitudeIndicator;
    }
    public String getLongitudeIndicator() {
        return mLongitudeIndicator;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }
    public float getSpeed() {
        return mSpeed;
    }

    public void setDirection(float direction) {
        mDirection = direction;
    }
    public float getDirection() {
        return mDirection;
    }

    public void setAltitude(float altitude) {
        mAltitude = altitude;
    }
    public float getAltitude() {
        return mAltitude;
    }

    public void setSatellites(int satellites) {
        mSatellites = satellites;
    }
    public int getSatellites() {
        return mSatellites;
    }

    public void setPositionFixIndicator(int positionFixIndicator) {
        mPositionFixIndicator = positionFixIndicator;
    }
    public int getPositionFixIndicator() {
        return mPositionFixIndicator;
    }

    public void setDate(int year, int month, int day) {
        mDateTime.year = year;
        mDateTime.month = month;
        mDateTime.day = day;
    }
    public void setTime(int hour, int min, int sec) {
        //Log.d(TAG, "hour: " + hour + ", min: " + min + ", sec: " + sec);
        mDateTime.hour = hour;
        mDateTime.min = min;
        mDateTime.sec = sec;
    }
    public DateTime getDateTime() {
        return mDateTime;
    }

    public class DateTime extends Throwable {
        private int year;
        private int month;
        private int day;
        private int hour;
        private int min;
        private int sec;

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getDay() {
            return day;
        }

        public int getHour() {
            return hour;
        }

        public int getMin() {
            return min;
        }

        public int getSec() {
            return sec;
        }
    }

    public String toString() {
        return "list id: " + mId + ", map id: " + mMapPointId + ", mStatus: " + mStatus + ", " + mLatitudeIndicator + ": " + mLatitude
                +  ", " + mLongitudeIndicator + ": " + mLongitude + ", mSpeed: " + mSpeed + ", mDirection: "
                + mDirection + ", mAltitude: " + mAltitude + ", mSatellites: " + mSatellites
                + ", mPositionFixIndicator: " + mPositionFixIndicator + ", date time: " + mDateTime.getYear()
                + "-" + mDateTime.getMonth() + "-" + mDateTime.getDay() + ":" + mDateTime.getHour() + "-"
                + mDateTime.getMin() + "-" + mDateTime.getSec();
    }
}