package com.example.xng.rkcamera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Xng on 2016/12/14.
 */
public class LanguageChange extends BaseActivity implements View.OnClickListener {

    static final String TAG = "LanguageChange";
	
    private LinearLayout llenglish;
    private LinearLayout llchinese;
    private Button btn_back;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_change);
        llenglish = (LinearLayout) findViewById(R.id.english);
        llchinese = (LinearLayout) findViewById(R.id.chinese);
        btn_back = (Button) findViewById(R.id.language_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        llchinese.setOnClickListener(this);
        llenglish.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
         switch (view.getId()){
             case R.id.english:
                 switchLanguage("en");
                 break;
             case R.id.chinese:
                 switchLanguage("zh");
                 break;
         }
        /*
        //更新语言后，destroy当前页面，重新绘制
        finish();
        Intent intent = new Intent(LanguageChange.this, MainActivity.class);
        startActivity(intent);
        */

        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);

        // 杀掉进程
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);

    }
}
