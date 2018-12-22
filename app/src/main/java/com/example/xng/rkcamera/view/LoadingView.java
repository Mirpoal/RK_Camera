package com.example.xng.rkcamera.view;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.xng.rkcamera.R;

/**
 * 自定义透明的dialog
 */
public class LoadingView extends Dialog {
    private String content;

    public LoadingView(Context context, String content) {
        super(context, R.style.LoadingView);
        this.content = content;
        initView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (LoadingView.this.isShowing())
                    LoadingView.this.dismiss();
                break;
        }
        return true;
    }

    private void initView() {
        setContentView(R.layout.loading_view);
        ((TextView) findViewById(R.id.tvcontent)).setText(content);
        setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.alpha = 0.9f;
        getWindow().setAttributes(attributes);
        setCancelable(false);
    }
}