package com.padyun.opencvapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by litao on 2018/3/16.
 */
public class RequestPermissionActivity extends Activity {

    private int REQUEST_MEDIA_PROJECTION = 1;
    private Intent mIntentResult = null;
    public static void startActivity(Context from){
        Intent intent = new Intent(from, RequestPermissionActivity.class) ;
        if(!(from instanceof Activity)){
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ;
        }
        from.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PackageInfo info = null;;
        int serviceVersion = 0 ;
        try {
            info = getPackageManager().getPackageInfo("com.padyun.ypservice", 0);
            serviceVersion = info.versionCode ;
        } catch (PackageManager.NameNotFoundException e) {

            e.printStackTrace();
        }

        if(mIntentResult == null && serviceVersion < YpFairyService.YPSERVICE_CAPTURE_VERSION){
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }else{
            finish();
            LtLog.i("on create start service........") ;
            startService();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LtLog.i("on activity result requestCode:"+ requestCode) ;
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            mIntentResult = data ;
            startService();
            finish();
        }

    }
    private void startService(){
        YpFairyService.startService(this, mIntentResult);
    }
}
