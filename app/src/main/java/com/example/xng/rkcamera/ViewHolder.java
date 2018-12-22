package com.example.xng.rkcamera;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Xng on 2016/11/1.
 */
public class ViewHolder {
    public TextView tv_filename, tv_fileday, tv_filetime, tv_filesize, tv_total;
    public LinearLayout item_layout;
    public Button startDownload,pauseDownload, open_btn;
    public ImageView iv_fileimage;
    public ProgressBar pbProgress;
    public CheckBox checkBox;
}
