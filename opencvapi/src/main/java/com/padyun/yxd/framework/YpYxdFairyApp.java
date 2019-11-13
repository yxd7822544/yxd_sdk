package com.padyun.yxd.framework;

import android.app.Application;

import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.YpFairyService;

/**
 * Created by Administrator on 2019/1/23 0023.
 */

public class YpYxdFairyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LtLog.i("YPTaskFairyApp onCrate") ;
        YpFairyService.setStarterClass(YpYxdFairyImpl.class);
    }
}