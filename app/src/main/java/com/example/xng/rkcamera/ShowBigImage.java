package com.example.xng.rkcamera;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Xng on 2016/12/27.
 */
public class ShowBigImage extends Activity{

    private ZoomImageView showImage;
    private Button btn_back;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_big_image);
        showImage = (ZoomImageView) findViewById(R.id.iv_showimage);
        final String url = getIntent().getStringExtra("url");
        showImage.setImageBitmap(BitmapFactory.decodeFile(url));
        btn_back = (Button) findViewById(R.id.showimage_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
