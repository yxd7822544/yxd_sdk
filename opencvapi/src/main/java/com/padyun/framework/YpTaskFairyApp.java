package com.padyun.framework;

import android.app.Application;
import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.YpFairyService;

/**
 * Created by litao on 2018/8/22.
 */
public class YpTaskFairyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LtLog.i("YPTaskFairyApp onCrate") ;
        YpFairyService.setStarterClass(YpTaskFairyImpl.class);

    }
}
