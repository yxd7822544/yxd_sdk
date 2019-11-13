package com.padyun.fairy;

import com.padyun.opencvapi.FindResult;
import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.ScreenInfo;
import com.padyun.yxd.framework.YpYxdFairyImpl;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Administrator on 2019/1/24 0024.
 */

public class TaskMain {
    public YpYxdFairyImpl mF;
    public TaskMain(YpYxdFairyImpl ypfairy){

        mF=ypfairy;
        LtLog.e("bbbbbbbb");
    }
     FindResult result;
    static {
        System.out.println("====================TaskMain   loadLibrary  ");
        System.loadLibrary("native-lib");
    }
    public void main(){
        LtLog.e("==============================main");
        ScreenInfo screenInfo = mF.captureInterval();
        Mat img1;
        img1 = new Mat(screenInfo.height, screenInfo.width, CvType.CV_8UC4);
        img1.put(0, 0, screenInfo.raw);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2RGB);
        int[] xy = new int[2];
        String colorStr_start = "101,192,210";
        String colorStr_sub = "54|5|255,255,255&116|1|186,226,232&89|14|250,253,254";

//        String colorStr_start= "252,236,60";
//        String colorStr_sub="30|78|203,24,24";

        mF.multipointFindColor(0, 0, 1280, 720, null, img1.getNativeObjAddr(), colorStr_start, colorStr_sub, 0.90, xy);

        LtLog.e("========================2=========================test" + xy[0] + "," + xy[1]);

        LtLog.e("==============================main");

        LtLog.e("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
