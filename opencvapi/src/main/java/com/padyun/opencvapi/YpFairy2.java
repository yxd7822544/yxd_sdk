package com.padyun.opencvapi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.padyun.Protocol;
import com.padyun.YpModule2;
import com.padyun.module2.FindResultModule;
import com.padyun.module2.TestFindModule;
import com.padyun.network.ServerNio2;
import com.padyun.network.StickPackageForNio2;
import com.padyun.opencvapi.module.ResultModule;


import com.padyun.opencvapi.utils.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by litao on 2018/2/26.
 */
public abstract class YpFairy2 implements StickPackageForNio2.StickPackageCallback {



    public static final float VERSION = 35.3f ;
//    public static final int TEST_PORT = 42100 ;
    public static final int FAIRYTOOL_PORT = 12100 ;

    public static final String OCR_DATA = "/sdcard/yunpai_files/tessdata/chi_sim.traineddata" ;
    /**
     * fairy类型 监控
     * */
    public static final int FAIRY_TYPE_CHECK =1 ;
    /**
     * fairy类型 任务
     * */
    public static final int FAIRY_TYPE_TASK = 2 ;
    /**
     * fairy正常完成状态,其它状态自定义
     * */
    public static final int TASK_STATE_FINISH = 99 ;
    private static final int CAPTURE_INTERVAL = 50 ;

    private int mWidth = 720;
    private int mHeight = 1280;
    private Context mContext ;
    private String mPackageName ;
    private Handler mHandler ;
    private YpFairyUtils mUtils ;
    private YpControl mControlUtil ;
    private YpOpencvUtils mOpencvUtil ;
    private YpScreencap mScreenUtil ;

    private PaintUtil mPaintUtil ;
    private ScreenInfo mRawScreenInfo ;
    private Object mCaptureLock = new Object() ;
    private ReportThread mReportThread;
    private long mLastCaputreTime = 0 ;
    private boolean mKeepAlive = true ;
    private TessBaseAPI mBaseApi ;
    private FairyService mService ;


    public YpFairy2(Context context,Intent intentResult){
        mHandler = new Handler() ;

        mService = new FairyService(this) ;
        mUtils = new YpFairyUtils(context) ;
        mControlUtil = YpControl.getInstance();
        if(intentResult == null){
            mScreenUtil = new YpScreencap2(context, intentResult) ;
        }else{
            mScreenUtil = new YpScreencap(context, intentResult) ;
        }

        mContext = context ;
        mOpencvUtil = new YpOpencvUtils() ;
        mPaintUtil = new PaintUtil(context) ;
        mReportThread = new ReportThread() ;
        mPackageName = context.getPackageName() ;

        String fileName;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            fileName = mContext.getDataDir().getAbsolutePath() +"/fairy.log" ;
        }else{
            fileName = "/data/sdcard/fairy_log/"+mPackageName+".log" ;
        }
        try {
            LtLog.setLogFile(fileName, 1024 * 1024 * 10);
        }catch (Exception e){}
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long useMemary = mUtils.getPackageMem(mPackageName) ;
                LtLog.i("package:"+mPackageName+" use mem:"+useMemary) ;
                mHandler.postDelayed(this, 5*60*1000) ;
            }
        },5*60*1000) ;
    }

    public void startService(){
        mService.start(getServicePort());
    }


    private void initOcr(){
        File f = new File(OCR_DATA) ;
        if(f.exists()) {
            mBaseApi=new TessBaseAPI();
            String ocrName = f.getName();
            mBaseApi.init(f.getParentFile().getParent(), ocrName.substring(0,ocrName.lastIndexOf(".")));
            mBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        }else{

        }
    }

    /**
     * 脚本重启
     * */
    public  void onRestart() {
        LtLog.i("YpFairy onRestart......") ;
    }
    /**
     * 脚本开始
     * */
    public abstract void onStart() ;
    /**
     * 停止脚本
     * */
    public abstract void onStop() ;
    /**
     * 脚本恢复执行
     */
    public abstract void onResume() ;
    /**
     * 脚本暂停执行
     * */
    public abstract void onPause() ;
    /**
     * 检测任务开始
     * */
    public abstract void onCheckStart() ;
    /**
     * 检测任务停止
     * */
    public abstract void onCheckStop() ;
    /**
     * 用户配置发生变化
     * */
    public abstract void onChangeConfig() ;


    public abstract  boolean onMonitorState(int state) ;


    public int getServicePort(){
        return 0 ;
    }

 /*

    //开始测试服务
    public void startTest(){
        Test test = new Test() ;
        test.start();
    }
*/
    public Context getContext(){
        return  mContext ;
    }

    /**
     * 获取模板图片的数据
     * @param name 图片的名字
     * @return  返回assert中图片的数据
     * */
    public byte[] getTemplateData(String name){
        return mUtils.getTemplateData(name) ;
    }

    /**
     * 获取模板数据的Mat对象
     * @param name 模板图片名字
     * @return 返回模板图片的Mat对象
     * */
    public Mat getTemplateMat(String name){
        return  mOpencvUtil.getTemplateMat(name) ;
    }
    public Mat getTemplateMatFromAssert(String name){
        return mOpencvUtil.getTemplateMatFromAssert(name) ;
    }
    /**
     * 获取模板图片的信息
     * */
    public TemplateInfo getTemplateInfo(String name){
        return  mOpencvUtil.getTemplateInfo(name) ;
    }

   public TemplateInfo getTemplateInfoFromAssert(String name){
        return mOpencvUtil.getTemplateInfoFromAssrt(name) ;
    }
    public TemplateInfo getTemplateInfo(InputStream in){
        return YpOpencvUtils.getTemplateInfo(in) ;
    }

    public void reset(){
        mOpencvUtil.reset();
    }

    /**
     * 根据给定的信息获取当期屏幕的Mat对象
     * @param leftX  屏幕左上角X坐标
     * @param leftY  屏幕左上角Y坐标
     * @param width 获取宽
     * @param height 获取高
     * @param flag Mat 颜色类型
     *             @see YpOpencvUtils#THRESH_FLAG
     *             @see YpOpencvUtils#COLOR_FLAG
     *             @see YpOpencvUtils#GRAY_FLAG
     * @param thresh 二值化阈值
     * @param  maxval 二值化最大值
     * @param  type 二值化类型
     * @return 返回当前屏幕的mat对象
     * */
    public Mat getScreenMat(int leftX, int leftY, int width, int height,
                            int flag, int thresh, int maxval, int type){
        return mOpencvUtil.getScreenMat(leftX, leftY, width, height, flag, captureInterval(), thresh, maxval, type);
    }

    public Mat getScreenMat(int leftX, int leftY, int width, int height,
                            int flag, int thresh, int maxval,int type, ScreenInfo screenInfo){
        return mOpencvUtil.getScreenMat(leftX, leftY, width, height, flag, screenInfo, thresh, maxval, type);
    }
    public Mat getScreenMat(int leftX, int leftY, int width, int height,
                            int flag, int thresh, int maxval,int type, Mat screenMat){

        return mOpencvUtil.getScreenMat(leftX, leftY, width, height, flag, screenMat, thresh, maxval, type);
    }

    public Mat getScreenMat(ScreenInfo screenInfo, TemplateInfo templateInfo){
        return  mOpencvUtil.getScreenMat(screenInfo, templateInfo) ;
    }

    /**
     *  在dst Mat对象上查找src mat
     *  @param leftX  目标对象的左上角X坐标
     *  @param leftY  目标对象的左上角Y坐标
     *  @return  返回查找结果
     * */
    public FindResult matchMat(int leftX, int leftY,Mat src, Mat dst ){
        return  mOpencvUtil.findTemplate(leftX,leftY, src, dst, YpOpencvUtils.DEFAULT_METHOD) ;
    }
    public FindResult matchMat(int leftX, int leftY, Mat src, Mat dst, int method){
        return  mOpencvUtil.findTemplate(leftX,leftY, src, dst, method) ;
    }

    /**
     * 查找图片
     * 此方法会根据图片的信息在图片所在固定位置查找
     * @param name 图片名
     * @return 返回查找结果，如果图片信息不存在则返回null
     * */
    public FindResult findPic2(String name){
        return  mOpencvUtil.findPic(name,captureInterval()) ;
    }
    /**
     * 范围查找图片
     * 此方法根据图片的信息在图片所在位置扩大一定范围查找
     * @param name 图片名
     * @param wide 扩大的范围
     * @return 返回查找结果，如果图片信息不存在则返回null
     * */
    public FindResult findPicRange(String name, int wide){
        return  mOpencvUtil.findPicRange(name, captureInterval(), wide) ;
    }

    public int getColorCount(int r, int g, int b, float sim, Mat mat){
        return  mOpencvUtil.getColorCount(r,g,b,sim,mat) ;
    }
    public int getColorCount(int color , float sim , Mat mat){
        return mOpencvUtil.getColorCount(color, sim, mat) ;
    }


    /**
     * 范围查找图片
     * @param leftX 查找范围的左上角X坐标
     * @param leftY 查找范围的左上角Y坐标
     * @param rightX  查找范围的右下角X坐标
     * @param rightY 查找范围的右下角Y坐标
     * @param name 图片名
     * @return 返回查找结果，如果图片信息不存在则返回null
     * */
    public FindResult findPic2(int leftX,int leftY, int rightX, int rightY, String name){
        return  mOpencvUtil.findPic(leftX, leftY, rightX, rightY, name, captureInterval());
    }



    private byte[] cutpic(int leftX, int leftY, int width, int height,int quality,float scale, RawScreenInfo screenInfo){
        Bitmap bitmap = Bitmap.createBitmap(screenInfo.width, screenInfo.height, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix() ;
        matrix.setScale(scale,scale) ;
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(screenInfo.raw));

        Bitmap cut = Bitmap.createBitmap(bitmap, leftX,leftY,width, height, matrix, true);
        if(cut != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(screenInfo.width*screenInfo.height*4) ;
            cut.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            cut.recycle();
            bitmap.recycle();
            return outputStream.toByteArray();
        }else {
            bitmap.recycle();
        }
        return  null ;
    }


    /**
     * 上报任务状态
     *
     *  @param type 任务类型
     *              @see #FAIRY_TYPE_CHECK
     *              @see #FAIRY_TYPE_TASK
     *  @param taskId 任务Id
     *  @param state 任务状态
     *               @see #TASK_STATE_FINISH
     *  @return 返回服务器结果
     * */
    @Deprecated
    public String reportState(int type, String taskId, int state){
        return mUtils.postState(type,taskId, state) ;
    }

    public void killUserGame(){
        Utils.killApp(mContext,YpFairyConfig.getGamePackage());
    }

    public long getUsergameMem(){
        return  mUtils.getUsrGameMem() ;
    }
    public long getFreeMem(){
        return mUtils.getFreeMem() ;
    }
    public void finish(int type, String taskId, int state){
        if(type == FAIRY_TYPE_TASK){
            if(!TextUtils.isEmpty(YpFairyConfig.getTaskID())){
                YpFairyConfig.clearConfig();
                mReportThread.reportTask(type, taskId, state);
            }else{
                LtLog.e("error !!! task state aready reported.....") ;
            }
        }else{
            if(!mPackageName.equals(YpFairyService.GAME_MONITOR_NAME)){
                LtLog.e("error !!! fairy check aready replaced with " + YpFairyService.GAME_MONITOR_NAME) ;
            }
        }
    }
    public void finish(int type, String taskId){
        finish(type, taskId, TASK_STATE_FINISH) ;
    }

    public boolean keepAvlie(){
        return  mKeepAlive ;
    }
    public void keepalive(boolean keep){
        mKeepAlive = keep ;
    }
    public void playerRestart(){
    }
    public void playerStoped(){
    }


    public void tap(int x, int y) throws Exception {
        mControlUtil.tap(x,y);
    }
    /**
     * 点击查找结果，在查找到范围内随机点击
     * @param result 查找结果
     * */
    public void tapResult(FindResult result) throws Exception {
        int x = new Random().nextInt(result.width) + result.x ;
        int y = new Random().nextInt(result.height) + result.y ;
        tap(x,y);
    }
    public void touchDown(float x, float y){
        mControlUtil.touchDown(x,y);
    }
    public  void touchDown(int id, float x, float y)
    {
      mControlUtil.touchDown(id, x, y);
    }

    public void touchUp(){
        mControlUtil.touchUp();
    }
    public void touchUp(int id){
        mControlUtil.touchUp(id);
    }

    public void touchMove(float x, float y, int duration){
        mControlUtil.touchMove(x,y, duration);
    }
    public void touchMove(int id, float x, float y, int duration )
    {
        mControlUtil.touchMove(id, x, y, duration);
    }

    public void touchMove(int id, float x, float y, int duration, int step) throws Exception {
        mControlUtil.touchMove(id, x, y, duration, step);
    }


    public void showMessage(final String msg){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        }) ;

    }
    public void inputText(String text){
        YpFairyUtils.inputText(text);
    }
    public ScreenInfo captureInterval(){

        long current = System.currentTimeMillis() ;

        int i = 0 ;
        ScreenInfo screenInfo ;
        synchronized (mCaptureLock){
            do{
                if(mRawScreenInfo == null || current - mLastCaputreTime > CAPTURE_INTERVAL){
                    screenInfo = capture() ;
                    mLastCaputreTime = current ;
                }else{
                    screenInfo = mRawScreenInfo ;
                }
                if(screenInfo == null || screenInfo.raw == null){
                    LtLog.i("error capture retry after 100 millis "+i++) ;
                    Utils.sleep(100);
                    if(i > 3 && mRawScreenInfo != null){
                        screenInfo = mRawScreenInfo ;
                        LtLog.i("capture retry end use last screeninfo as return value") ;
                        break;
                    }
                }else{
                    break;
                }
            }while (true) ;
        }
        if(screenInfo == null){
            screenInfo = new ScreenInfo() ;
        }
        mRawScreenInfo = screenInfo ;
        return  mRawScreenInfo ;
    }
    public ScreenInfo capture(){

        ScreenInfo screenInfo = new ScreenInfo() ;

        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        screenInfo.height = metrics.heightPixels;
        screenInfo.width = metrics.widthPixels ;
        screenInfo.raw = null ;
        mScreenUtil.captureRawScreen(screenInfo)  ;
        if(screenInfo.raw != null){
            screenInfo.timestamp = System.currentTimeMillis() ;
        }
        return  screenInfo ;
    }

    /*
    public int cmpColor(int x,int y,int color_r,int color_g,int color_b){
        Utils.sleep(500);
        byte[] rawpic = captureRaw();

        //对比颜色点的坐标（x,y）对比的颜色（G:int(g),B:int(b),R:int(r)）
        int sum = 720*1280*4;
        System.out.println("byte总数="+sum);
        int i = (720*4)*y+(x)*4;
        System.out.println("下标索引值 = "+i);

        //将byte数值转为int类型
        int getcolor_r = rawpic[i]&0xff;
        int getcolor_g = rawpic[i+1]&0xff;
        int getcolor_b = rawpic[i+2]&0xff;
        //开始比较颜色的RGB分量值
        if (color_r != getcolor_r){
            System.out.println("当前像素点颜色不相同0");
            return 0;
        }else if (color_g != getcolor_g){
            System.out.println("color_g = "+ rawpic[i+1]);
            System.out.println("当前像素点颜色不相同1");
            return 0;
        }else if (color_b != getcolor_b){
            System.out.println("当前像素点颜色不相同2");
            return 0;
        }else {
            System.out.println("当前像素点颜色相同");
            return 1;
        }

    }
    */
    public byte[] captureBitmap(){
        return mScreenUtil.captureBitmap();
    }
    public Mat captureMat(){
        return  mScreenUtil.captureMat();
    }
    public String ocr(ScreenInfo screenInfo, int x, int y, int width, int height){
        Bitmap screenBitmap = Bitmap.createBitmap(screenInfo.width, screenInfo.height, Bitmap.Config.ARGB_8888);
        screenBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(screenInfo.raw));
        Bitmap bitmap = Bitmap.createBitmap(screenBitmap, x, y, width, height) ;
        screenBitmap.recycle();
        return ocr(bitmap) ;
    }
    public String ocr(int x, int y, int width, int height){
        return  ocr(captureInterval(), x, y, width, height) ;
    }

    public String ocr(Bitmap bitmap){
        if(mBaseApi == null){
            initOcr();
        }
        if(mBaseApi == null){
            return  null ;
        }
        mBaseApi.setImage(bitmap);
        String text = mBaseApi.getUTF8Text() ;
        mBaseApi.clear();
        return text ;
    }
    public void ocrRelease(){
        if(mBaseApi != null){
            mBaseApi.clear();
            mBaseApi.end();
            mBaseApi = null ;
        }
    }

    @Override
    public void onConnect(ServerNio2.ServerNioObject object) {

    }

    @Override
    public void onData(ServerNio2.ServerNioObject object, byte[] data, int offset, int count) {

    }

    @Override
    public void onDisconnect(ServerNio2.ServerNioObject object) {

    }

    public void sendToRemote(ServerNio2.ServerNioObject object, YpModule2 module){
        mService.sendPackage(object, module);
    }


    public class OpencvResult{
        public int x ;
        public int y ;
        public float sim ;

        @Override
        public String toString() {
            return x+":"+y+" "+sim ;
        }
    }
    @Deprecated
    public class RawScreenInfo{
        public int width;
        public int height;
        public byte[] raw;
    }
    /*
    private class TemplateInfo{
        int quality ;
        float scale ;
        String tmpFile ;
        String md5 ;
    }*/

    /*
    class Test implements StickPackageForNio2.StickPackageCallback{
        private StickPackageForNio2 mServer ;

        public void start(){
            mServer = new StickPackageForNio2();
            mServer.setDataCallback(this);
            mServer.start(FAIRYTOOL_PORT, false);
            LtLog.i("test start......") ;
        }

        @Override
        public void onConnect(ServerNio2.ServerNioObject object) {

        }
        private FindResultModule testFind(TestFindModule module){
            Mat screenMat  ;
            Mat templatMat ;
            ScreenInfo rawScreenInfo = capture() ;

            screenMat = mOpencvUtil.getScreenMat(module.x, module.y, module.width, module.height, module.flag,
                                                rawScreenInfo, module.thresh, module.maxval, module.type) ;
            Mat buf = new MatOfByte(module.pic.array()) ;
            templatMat = Imgcodecs.imdecode(buf,Imgcodecs.IMREAD_UNCHANGED) ;
            buf.release();

            //resultMat.release();screenMat.release(); templatMat.release();
            FindResult findResult = mOpencvUtil.findTemplate(module.x, module.y, templatMat, screenMat, module.method) ;
            Imgcodecs.imwrite("/sdcard/templat.png", templatMat) ;
            Imgcodecs.imwrite("/sdcard/screen.png", screenMat) ;
            screenMat.release();
            templatMat.release();
            FindResultModule resultModule = new FindResultModule();
            resultModule.x = findResult.x ;
            resultModule.y = findResult.y ;
            resultModule.width = findResult.width ;
            resultModule.height = findResult.height ;
            resultModule.timestamp = findResult.timestamp ;
            resultModule.sim = (int) (findResult.sim*100);
            LtLog.i("findResultModule x:"+resultModule.x+" y:"+resultModule.y+" sim:"+resultModule.sim+" time:"+resultModule.timestamp) ;
            return  resultModule ;
        }

        @Override
        public void onData(ServerNio2.ServerNioObject object, byte[] data, int offset, int count) {
            short type = com.padyun.utils.Utils.bytesToShort(data, offset) ;
            offset += Protocol.SIZE_OF_TYPE ;
            count -= Protocol.SIZE_OF_TYPE ;
            LtLog.i("onData:"+type+" count:"+count) ;
            switch (type){
                case Protocol.TYPE_TEST_FIND :
                    TestFindModule module = null;
                    try {
                        module = new TestFindModule(data, offset, count);

                        FindResultModule findResultModule = testFind(module) ;
                        object.sendBuffer = findResultModule.toDataWithLen() ;
                        mServer.sendMessage(object);
                        if(module.showResult) {
                            mPaintUtil.drawRect(findResultModule.x, findResultModule.y, findResultModule.width, findResultModule.height, findResultModule.sim, findResultModule.timestamp,5000);
                        }
                    } catch (YpModule2.YpModuleException e) {
                        e.printStackTrace();
                    }
                    break ;
            }

        }

        @Override
        public void onDisconnect(ServerNio2.ServerNioObject object) {

        }
    }
*/
    class ReportThread implements Runnable {

        private int mMonitorState = -1 ;
        private int mTaskState = -1;
        private String mTaskId ;
        private Object mLock ;
        private int reportSleep = 1000 ;
        private final int sleepMax = 5*60*1000 ;
        private Thread mThread ;

        public ReportThread(){
            mLock = new Object() ;
            mThread = new Thread(this);
            mThread.start();
        }
        private boolean report(int type, String taskId, int state){
            boolean success = false ;
            String result = mUtils.postState(type, taskId, state) ;
            LtLog.i("report state  result "+ result) ;
            if(result != null){
                int code = -1 ;
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    code = jsonObject.getInt("code") ;
                    LtLog.i("report task type:"+type+" id:"+taskId+" state:"+state+" resultcode:"+code) ;
                    success = true ;
                } catch (JSONException e) {
                    LtLog.i("getResultCode error :"+result+" e:"+e.getMessage());
                }
            }else{
                LtLog.i("report task error result null...");
            }
            return  success ;
        }
        @Override
        public void run() {
            while(true){
                while(mMonitorState == -1 && mTaskState == -1){
                    synchronized (mLock){
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                boolean success = true ;

                synchronized (mLock){
                    if(mMonitorState != -1){
                        if(report(YpFairy2.FAIRY_TYPE_CHECK,null,mMonitorState)){
                            mMonitorState = -1 ;
                        }else{
                            success = false ;
                        }
                    }
                    if(mTaskState != -1){
                        if(report(YpFairy2.FAIRY_TYPE_TASK,mTaskId,mTaskState)){
                            mTaskState = -1 ;
                        }else{
                            LtLog.e("report task error .....") ;
                            success = false ;
                        }
                    }
                }
                if(!success){
                    try {
                        Thread.sleep(reportSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(reportSleep >= sleepMax){
                        reportSleep = sleepMax ;
                    }else{
                        reportSleep += 1000 ;
                    }
                }else{
                    reportSleep = 1000 ;
                }


            }
        }
        public void reportTask(int type, String taskId, int state){
            synchronized (mLock) {
                switch (type) {
                    case FAIRY_TYPE_CHECK:
                        mMonitorState = state;
                        break;
                    case FAIRY_TYPE_TASK:
                        mTaskState = state;
                        mTaskId = taskId;
                        break;
                }
                mLock.notify();
            }
        }
    }

}
