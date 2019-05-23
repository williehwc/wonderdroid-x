
package com.atelieryl.wonderdroid;

import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.WindowManager;

public class BaseActivity extends AppCompatActivity {

    WonderDroid getWonderDroidApplication() {
        return (WonderDroid)getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
