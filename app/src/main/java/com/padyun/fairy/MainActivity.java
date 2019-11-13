package com.padyun.fairy;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.padyun.opencvapi.RequestPermissionActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);
        //  YpFairyService.startService(this);
         RequestPermissionActivity.startActivity(this);
         finish();
    }
}
