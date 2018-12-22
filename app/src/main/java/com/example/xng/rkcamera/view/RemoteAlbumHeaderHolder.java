package com.example.xng.rkcamera.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.xng.rkcamera.R;
import com.example.xng.rkcamera.model.RemoteAlbumModel;

/**
 * Created by waha on 2017/8/4.
 */

public class RemoteAlbumHeaderHolder extends RecyclerView.ViewHolder {
    private TextView txtDate;

    public RemoteAlbumHeaderHolder(View itemView) {
        super(itemView);
        txtDate = (TextView) itemView.findViewById(R.id.txt_date);
    }

    public void setData(RemoteAlbumModel model) {
        txtDate.setText(model.getDisplayName());
    }
}
