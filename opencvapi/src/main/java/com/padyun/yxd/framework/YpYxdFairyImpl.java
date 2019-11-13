package com.padyun.yxd.framework;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.padyun.opencvapi.FindResult;
import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.ScreenInfo;
import com.padyun.opencvapi.YpFairy2;
import com.padyun.opencvapi.YpFairyConfig;
import com.padyun.opencvapi.YpFairyService;
import com.padyun.opencvapi.utils.TemplateInfo;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Administrator on 2019/1/23 0023.
 */
public class YpYxdFairyImpl extends YpFairy2 {
    /**
     * arr版本号
     */
    public static final float VERSION = 21.19f;
    /**
     * 任务线程
     */
    private Thread mTaskThread;
    /**
     * 异常线程
     */
    private Thread mErroThread;
    /**
     * 判断线程是否开启
     */
    private boolean mTaskRunning = false;
    /**
     * 反射指定的类名
     */
    private static final String GAME_TASK_MAINCLASS = "com.padyun.fairy.TaskMain";
    /**
     * 反射指定的方法名
     */
    private static final String GAME_TASK_MAINFUNC = "main";
    /**
     * 异常线程指定的类名
     */
    private static final String GAME_TASK_ERROCLASS = "com.padyun.fairy.Abnormal";

    /**
     * 异常线程指定的方法名
     */
    private static final String GAME_TASK_ERROFUNC = "erro";
    /**
     * 线程反射需要用到的
     */
    private Object mMainObj;
    private Method method;
    private Constructor c;
    private Object mMainObjErro;
    private Method methodErro;
    private Constructor cErro;
    /**
     * 脚本版本号
     */
    private int gameVersion = 1;
    /**
     * 脚本名称
     */
    private String gameName = "game";
    /**
     * 截图对象
     */
    ScreenInfo screenInfo;
    TessBaseAPI tessBaseAPI;

    /**
     * Instantiates a new Yp yxd fairy.
     *
     * @param context      the context
     * @param intentResult the intent result
     */
    public YpYxdFairyImpl(Context context, Intent intentResult) {
        super(context, intentResult);
        if (YpFairyService.isApkInDebug(getContext())) {
            keepalive(false);
        }

        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.setDebug(true);
        tessBaseAPI.init("/sdcard/yunpai_files/", "eng"); //eng为识别语言*/
        //ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789,/"); // 识别白名单
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+=-[]}{;:'\"\\|~`.<>?"); // 识别黑名单
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        LtLog.e("当前版本号：" + VERSION);
        LtLog.e("尹    哥      出      品");
    }

    /**
     * 设置脚本名称
     *
     * @param gameName 脚本名称
     */
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    /**
     * 设置脚本版本号
     *
     * @param gameVersion 脚本版本号
     */
    public void setGameVersion(int gameVersion) {
        this.gameVersion = gameVersion;
    }


    /**
     * 用户第一次进入设备并启动任务时调用
     */
    @Override
    public void onStart() {
        synchronized (this) {
            LtLog.e("-----------------------onstart.....");
            mTaskRunning = start();
        }
    }

    /**
     * 内存满时服务器会调用杀掉游戏
     */
    @Override
    public void onRestart() {
        LtLog.e("------------+++--------- onRestart.....");
        killUserGame();
    }

    /**
     * 用户停止任务时调用
     */
    @Override
    public void onStop() {

        synchronized (this) {
            LtLog.e("------------+++--------- onStop.....");
            stop();
            mTaskRunning = false;
        }
    }

    /**
     * 用户启动任务时调用
     */
    @Override
    public void onResume() {

        synchronized (this) {
            LtLog.e("----------onResume------");
            mTaskRunning = start();
        }
    }

    /**
     * 用户暂停任务时调用
     */
    @Override
    public void onPause() {

        synchronized (this) {
            LtLog.e("----------onPause------");
            stop();
            mTaskRunning = false;
        }
    }

    @Override
    public void onChangeConfig() {

    }

    /**
     * 上报状态时调用
     *
     * @return false 不拦截上报
     */
    @Override
    public boolean onMonitorState(int i) {
        LtLog.e("-------上报的状态为i=" + i);
        return false;
    }

    @Override
    public void onCheckStop() {

    }

    @Override
    public void onCheckStart() {

    }


    /**
     * 启动任务时调用
     * 如果tasdID为空则先调用反射类的构造再启动两条线程
     * 如果不为空则先调用反射类的构造再唤醒两条线程
     *
     * @return 启动或唤醒成功返回true
     */
    private boolean start() {

        synchronized (this) {
            if (!YpFairyConfig.getTaskID().equals("")) {
                if (mErroThread == null || !mErroThread.isAlive()) {
                    try {
                        Class cls1 = Class.forName(GAME_TASK_ERROCLASS);
                        cErro = cls1.getConstructor(this.getClass());//获取有参构造
                        mMainObjErro = cErro.newInstance(this);
                        methodErro = cls1.getMethod(GAME_TASK_ERROFUNC);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    mErroThread = new Thread(new ErroLoop());
                    mErroThread.start();
                    LtLog.e("启动异常线程");
                }

                if (mTaskThread == null || !mTaskThread.isAlive()) {
                    try {
                        Class cls = Class.forName(GAME_TASK_MAINCLASS);
                        c = cls.getConstructor(this.getClass());//获取有参构造
                        mMainObj = c.newInstance(this);
                        method = cls.getMethod(GAME_TASK_MAINFUNC);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    mTaskThread = new Thread(new TaskLoop());
                    mTaskThread.start();
                    LtLog.e("启动任务线程");
                    return true;
                }

            }
            if (YpFairyConfig.getTaskID().equals("")) {
                LtLog.e("start task null........");
            } else {
                try {
                    mMainObj = c.newInstance(this);
                    mMainObjErro = cErro.newInstance(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                this.notify();
                this.notify();
                //     LtLog.e("线程唤醒成功") ;
                return true;
            }
        }
        return false;
    }

    /**
     * 停止任务调用
     * 线程阻塞状态下会抛出异常InterruptedException
     * 若线程当前未阻塞则继续运行到下一个阻塞点抛出此异常
     * 由throws Exception一层一层抛出，最后由我处理
     */
    private void stop() {
        synchronized (this) {
            if (mTaskThread != null) {
                LtLog.e("------------+++--------- stop.....");
                mTaskThread.interrupt();
                mErroThread.interrupt();
            }
        }
    }

    /**
     * 任务线程
     */
    class TaskLoop implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (YpYxdFairyImpl.this) {
                    while (YpFairyConfig.getTaskID().equals("") || !mTaskRunning) {
                        try {
                            LtLog.e("任务线程等待中.....");
                            YpYxdFairyImpl.this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                screenInfo = capture();
                if (screenInfo == null) {
                    LtLog.e("game monitor error capture screen info null....");
                    continue;
                }
                try {
                    method.invoke(mMainObj);
                } catch (Exception e) {
                    LtLog.e(e.toString());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw, true));
                    String str = sw.toString();
                    LtLog.e(str);
                    try {
                        condit();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    continue;
                }
            }
        }

    }

    /**
     * 异常线程
     * 每隔45秒打印一次时间
     */
    class ErroLoop implements Runnable {
        /**
         * The Time.
         */
        long time = 0;
        /**
         * The Now time 2.
         */
        String nowTime2 = "没有开启任务";
        /**
         * The Now date 2.
         */
        String nowDate2 = "";
        /**
         * The Format 1.
         */
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss");
        /**
         * The Df 1.
         */
        DateFormat df1 = DateFormat.getDateInstance();//日期格式，精确到日

        @Override
        public void run() {
            nowTime2 = format1.format(new Date());
            nowDate2 = df1.format(new Date());
            while (true) {
                synchronized (YpYxdFairyImpl.this) {
                    while (YpFairyConfig.getTaskID().equals("") || !mTaskRunning) {
                        try {
                            LtLog.e("异常线程等待中.....");
                            YpYxdFairyImpl.this.wait();
                            nowTime2 = format1.format(new Date());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                if (System.currentTimeMillis() - time > 45000) {
                    LtLog.e(gameName + "版本号--" + gameVersion + "--用户开启任务的时间===" + nowDate2 + ":" + nowTime2);
                    LtLog.e("当前时间===" + df1.format(new Date()) + ":" + format1.format(new Date()));
                    time = System.currentTimeMillis();
                }
                if (screenInfo == null) {
                    LtLog.e("game monitor error capture screen info null....");
                    continue;
                }
                try {
                    methodErro.invoke(mMainObjErro);
                } catch (Exception e) {
                    LtLog.e(e.toString());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw, true));
                    String str = sw.toString();
                    LtLog.e(str);
                    try {
                        condit();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    continue;
                }
            }
        }

    }

    /**
     * 完成任务时调用
     *
     * @param taskId
     * @param state  99为任务正常完成上报
     */
    public void finish(String taskId, int state) {
        super.finish(FAIRY_TYPE_TASK, taskId, state);
        stop();
    }

    /**
     * 脚本while中使用每次循环截取一张图片
     *
     * @return true
     * @throws Exception the exception
     */
    public boolean condit() throws Exception {
        getScreenMat();
        return true;
    }


    /**
     * 完成任务时调用
     *
     * @param type   两个参数  任务的上报FAIRY_TYPE_TASK    监控上报FAIRY_TYPE_CHECK已废弃
     * @param taskId
     * @param state  99为任务正常完成上报
     */
    @Override
    public void finish(int type, String taskId, int state) {
        super.finish(type, taskId, state);
        stop();
    }

    /**
     * 指定范围找图
     *
     * @param leftX   the left x
     * @param leftY   the left y
     * @param rightX  the right x
     * @param rightY  the right y
     * @param picName the pic name
     * @return the find result
     * @throws Exception the exception
     */
    public FindResult findPic(int leftX, int leftY, int rightX, int rightY, String picName) throws Exception {
        FindResult result = new FindResult();
        Mat screenMat = new Mat(screenInfo.height, screenInfo.width, CvType.CV_8UC4);
        if (screenInfo.raw == null || screenInfo.raw.length == 0) {
            result.sim = 0.1f;
            return result;
        }
        screenMat.put(0, 0, screenInfo.raw);
        TemplateInfo mInfo = getTemplateInfo(picName);
        Mat mMat = getTemplateMat(picName);
        //   LtLog.i("leftx=" + leftX + " lefty=" + leftY+ " rightX=" +rightX+ " rightY=" + rightY);
        if (screenMat == null) {
            result.sim = 0.1f;
            return result;
        } else {
            int width = rightX - leftX;
            if (width > screenMat.width()) {
                width = screenMat.width();
            }

            int height = rightY - leftY;
            if (height > screenMat.height()) {
                height = screenMat.height();
            }
            if (leftX + width > screenInfo.width || leftY + height > screenInfo.height) {
             /*   LtLog.i("getScreenMat error " +
                        " screen width:" + screenInfo.width + "rightX:" + (leftX + width) +
                        " screen height:" + screenInfo.height + " rightY:" + (leftY + height));*/
                result.sim = 0.1f;
                return result;
            }
            if (leftX == -1 && leftY == -1) {
                LtLog.e("error rect coord x:" + leftX + " y:" + leftY);
            } else {
                //  LtLog.i("leftx=" + leftX + " lefty=" + leftY+ " width=" +width+ " height=" + height);
                Mat rectMat = getScreenMat(leftX, leftY, width, height, mInfo.flag, mInfo.thresh, mInfo.maxval, mInfo.type, screenMat);
                if (rectMat.width() >= mMat.width() && rectMat.height() >= mMat.height()) {
                   /* try {*/
                    result = matchMat(leftX, leftY, mMat, rectMat);
               /*     } catch (Exception e) {
                        LtLog.e(e.getMessage());
                        LtLog.e("match exception:image info:" + mMat.width() + ":" + mMat.height() +
                                "screen info: " + rectMat.width() + ":" + rectMat.height() + " image:" + picName);
                    }*/
                } else {
                    LtLog.e("match err:image info:" + mMat.width() + ":" + mMat.height() +
                            "screen info: " + rectMat.width() + ":" + rectMat.height() + " image:" + picName);
                }
                rectMat.release();
            }

        }
        if (screenMat != null) {
            screenMat.release();
        }
        return result;
    }

    /**
     * 截图的位置找图(范围是图片的原位置)
     *
     * @param picName the pic name
     * @return the find result
     * @throws Exception the exception
     */
    public FindResult findPic(String picName) throws Exception {
        TemplateInfo mInfo = getTemplateInfo(picName);
        return this.findPic(mInfo.x, mInfo.y, mInfo.x + mInfo.width, mInfo.y + mInfo.height, picName);
    }

    /**
     * 指定区域找多图返回最相似的那一张
     *
     * @param x_1     the x 1
     * @param y_1     the y 1
     * @param x_2     the x 2
     * @param y_2     the y 2
     * @param picName new String[]{"test.png","test1.png",.......}
     * @return the find result
     * @throws Exception the exception
     */
    public FindResult findPic(int x_1, int y_1, int x_2, int y_2, String[] picName) throws Exception {
        FindResult result;
        FindResult result1;
        result = findPic(x_1, y_1, x_2, y_2, picName[0]);
        for (int i = 1; i < picName.length; i++) {
            result1 = findPic(x_1, y_1, x_2, y_2, picName[i]);
            if (result1.sim > result.sim) {
                result = result1;
            }
        }
        return result;
    }

    /**
     * 指定区域找多图找到就点图片
     *
     * @param x_1     the x 1
     * @param y_1     the y 1
     * @param x_2     the x 2
     * @param y_2     the y 2
     * @param sim     the sim
     * @param picName new String[]{"test.png","test1.png",.......}
     * @throws Exception the exception
     */
    public void findPic(int x_1, int y_1, int x_2, int y_2, float sim, String[] picName) throws Exception {
        FindResult result;
        for (int i = 0; i < picName.length; i++) {
            result = findPic(x_1, y_1, x_2, y_2, picName[i]);
            onTap(sim, result, picName[i], 1000);
        }
    }

    /**
     * 找多图返回最相似的那张图片
     *
     * @param picName new String[]{"test.png","test1.png",.......}
     * @return the find result
     * @throws Exception the exception
     */
    public FindResult findPic(String[] picName) throws Exception {
        FindResult result;
        FindResult result1;
        result = findPic(picName[0]);
        for (int i = 1; i < picName.length; i++) {
            result1 = findPic(picName[i]);
            if (result1.sim > result.sim) {
                result = result1;
            }
        }
        return result;
    }

    /**
     * 指定相似度找多图找到就点图片
     *
     * @param sim     the sim
     * @param picName new String[]{"test.png","test1.png",.......}
     * @throws Exception the exception
     */
    public void findPic(float sim, String[] picName) throws Exception {
        FindResult result;
        for (int i = 0; i < picName.length; i++) {
            result = findPic(picName[i]);
            onTap(sim, result, picName[i], 1000);
        }
    }


    /**
     * 截一张图
     *
     * @throws Exception the exception
     */
    public void getScreenMat() throws Exception {
        while (true) {
            Thread.sleep(1);
            screenInfo = capture();
            if (screenInfo == null) {
                LtLog.e("game monitor error capture screen info null....");
                continue;
            } else {
                break;
            }
        }
    }


    /**
     * 找到图片后在图片大小的范围内随机点
     *
     * @param sim    the sim
     * @param result the result
     * @param string 图片的名称
     * @param time   点击完成后延时多少秒
     * @throws Exception the exception
     */
    public void onTap(float sim, FindResult result, String string, long time) throws Exception {
        if (result.sim >= sim) {
            int x = new Random().nextInt(result.width) + result.x;
            int y = new Random().nextInt(result.height) + result.y;
            this.tap(x, y);
            StackTraceElement ste = new Throwable().getStackTrace()[1];
            LtLog.e(ste.getFileName() + ": Line " + ste.getLineNumber() + ":sim=" + result.sim + ": IntX=" + x + ": IntY=" + y + ":String=" + string);
            Thread.sleep(time);
            getScreenMat();
        }
    }

    /**
     * 找到图片后在指定范围内随机点击
     *
     * @param sim    the sim
     * @param result the result
     * @param leftX  the left x
     * @param leftY  the left y
     * @param rightX the right x
     * @param rightY the right y
     * @param string 图片的名称
     * @param time   点击完成后延时多少秒
     * @throws Exception the exception
     */
    public void onTap(float sim, FindResult result, int leftX, int leftY, int rightX, int rightY, String string, long time) throws Exception {
        if (result.sim >= sim) {
            int x = new Random().nextInt(rightX - leftX) + leftX;
            int y = new Random().nextInt(rightY - leftY) + leftY;
            this.tap(x, y);
            StackTraceElement ste = new Throwable().getStackTrace()[1];
            LtLog.e(ste.getFileName() + ": Line " + ste.getLineNumber() + ":sim=" + result.sim + ": IntX=" + x + ": IntY=" + y + ":String=" + string);
            Thread.sleep(time);

            getScreenMat();
        }
    }

    /**
     * 在指定范围内随机点击
     *
     * @param leftX  the left x
     * @param leftY  the left y
     * @param rightX the right x
     * @param rightY the right y
     * @param string 图片的名称
     * @param time   点击完成后延时多少秒
     * @throws Exception the exception
     */
    public void onTap(int leftX, int leftY, int rightX, int rightY, String string, long time) throws Exception {
        int x = new Random().nextInt(rightX - leftX) + leftX;
        int y = new Random().nextInt(rightY - leftY) + leftY;
        this.tap(x, y);
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        LtLog.e(ste.getFileName() + ": Line " + ste.getLineNumber() + ": IntX=" + x + ": IntY=" + y + ":String=" + string);
        Thread.sleep(time);

        getScreenMat();
    }

    /**
     * 找到图片后打印出当前图片的名称行号以及类，例如 YxdUtil.java: Line 185
     * @param usim   the usim
     * @param result the result
     * @param str    the str
     * @return the line info
     * @throws Exception the exception
     */
    public String getLineInfo(float usim, FindResult result, String str) throws Exception {
        int x1, y1;
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        if (result.sim >= usim) {
            x1 = result.x;
            y1 = result.y;
            return ste.getFileName() + ": Line " + ste.getLineNumber() + ":sim=" + result.sim + ": IntX=" + x1 + ": IntY=" + y1 + ":img=" + str;
        }
        return "     ";
    }

    /**
     * 打印当前行号以及类
     * 例如 YxdUtil.java: Line 185
     *
     * @param str the str
     * @return the line info
     * @throws Exception the exception
     */
    public String getLineInfo(String str) throws Exception {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        return ste.getFileName() + ": Line " + ste.getLineNumber() + ":other==" + str;
    }

    /**
     * 返回区域内颜色数量一般sim使用0.9 ，    此方法只能用于横屏  竖屏请用重写的
     *
     * @param x         the x
     * @param y         the y
     * @param x_1       the x 1
     * @param y_1       the y 1
     * @param strColor  the str color   "RGB"
     * @param simDouble the sim double
     * @return the color num
     * @throws Exception the exception
     *                   <p>
     *
     */
    public int getColorNum(int x, int y, int x_1, int y_1, String strColor, double simDouble) throws Exception {
        if (screenInfo.height > 720) {
            return 0;
        }
        double[] match = new double[3];
        match[0] = Double.parseDouble(strColor.split(",")[2]);
        match[1] = Double.parseDouble(strColor.split(",")[1]);
        match[2] = Double.parseDouble(strColor.split(",")[0]);
        double simValue = 255 * (1 - simDouble);
        double min_r = match[0] - simValue;
        double min_g = match[1] - simValue;
        double min_b = match[2] - simValue;
        double max_r = match[0] + simValue;
        double max_g = match[1] + simValue;
        double max_b = match[2] + simValue;
        if (min_r < 0) {
            min_r = 0;
        }
        if (min_g < 0) {
            min_g = 0;
        }
        if (min_b < 0) {
            min_b = 0;
        }
        if (max_r > 255) {
            max_r = 255;
        }
        if (max_g > 255) {
            max_g = 255;
        }
        if (max_b > 255) {
            max_b = 255;
        }

        Mat mat;

        while (true) {
            mat = this.getScreenMat(x, y, x_1 - x, y_1 - y, 1, 0, 0, 1, screenInfo);
            if (mat != null) {
                break;
            }
        }
        Scalar minValues = new Scalar(min_r, min_g, min_b);
        Scalar maxValues = new Scalar(max_r, max_g, max_b);
//        System.out.println("------------" + min_r + "," + min_g + "," + min_b + "," + max_r + "," + max_g + "," + max_b + ",mat=" + mat);
        Core.inRange(mat, minValues, maxValues, mat);
        int number = Core.countNonZero(mat);
        mat.release();

        return number;
    }

    /**
     * 返回区域内颜色数量一般sim使用0.9，横竖屏都可用
     *
     * @param str   the str   X1，Y1，X2，Y2范围
     * @param color the color 颜色RGB值
     * @param sim   the sim
     * @param type  the type 等于1的时候是竖屏 等于0的时候是横屏
     * @return the color num
     * @throws Exception the exception
     */
    public int getColorNum(String str, String color, double sim, int type) throws Exception {
        int Nownum = 0;
        byte[] rawpic = screenInfo.raw;
        //这是获取给的范围的
        String[] arr = str.split(",");
        String[] btt = color.split(",");
        String setr = btt[0];
        String setg = btt[1];
        String setb = btt[2];
        int setir = Integer.parseInt(setr);
        int setig = Integer.parseInt(setg);
        int setib = Integer.parseInt(setb);
        double maxrRange = (1 - sim) * setir;
        double maxgRange = (1 - sim) * setig;
        double maxbRange = (1 - sim) * setib;
        int minw = Integer.parseInt(arr[0]);
        int maxw = Integer.parseInt(arr[2]);
        int minh = Integer.parseInt(arr[1]);
        int maxh = Integer.parseInt(arr[3]);
        int Totalspot = (maxw - minw) * (maxh - minh);
        //  LtLog.e("总点数是" + Totalspot);
        int w = 1280;
        if (type == 1) {
            w = 736;
        }
        int i = (w * 4) * Integer.parseInt(arr[1]) + Integer.parseInt(arr[0]) * 4;
        int proit = i;
        int eachrow = maxw - minw;
        int nowspot = 0;
        int begin = 0;
        do {
            nowspot = nowspot + 1;
            String sr = String.valueOf(rawpic[i] & 0xff);
            String sg = String.valueOf(rawpic[i + 1] & 0xff);
            String sb = String.valueOf(rawpic[i + 2] & 0xff);
//            LtLog.e("匹配第"+nowspot+"个点");
//            LtLog.e("---------------------------R=======" + String.valueOf(rawpic[i] & 0xff));
//            LtLog.e("---------------------------G=======" + String.valueOf(rawpic[i + 1] & 0xff));
//            LtLog.e("---------------------------B=======" + String.valueOf(rawpic[i + 2] & 0xff));
            int ir = Integer.parseInt(sr);
            int ig = Integer.parseInt(sg);
            int ib = Integer.parseInt(sb);
            if (setir - maxrRange <= ir && ir <= setir + maxrRange && setig - maxgRange <= ig && ig <= setig + maxgRange && ib >= setib - maxbRange && ib <= setib + maxbRange) {
                Nownum = Nownum + 1;
            }
            begin = begin + 1;
            if (begin >= eachrow) {
                proit = w * 4 + proit;
                i = proit;
                begin = 0;
            } else {
                i = i + 4;
            }
        } while (nowspot < Totalspot);
        //  LtLog.e("匹配成功的个数是" + Nownum);
        return Nownum;
    }


    /**
     * 滑动
     * @param x         滑动起点
     * @param y         滑动起点
     * @param x1        滑动终点
     * @param y1        滑动终点
     * @param moveSleep 滑动起点到终点的时间
     * @param stopSleep 滑动结束后延迟多少秒截图
     * @throws Exception
     */
    public void ranSwipe(int x, int y, int x1, int y1, int moveSleep, long stopSleep) throws Exception {
        touchDown(2, x, y);
        touchMove(2, x1, y1, moveSleep);
        touchUp(2);
        Thread.sleep(stopSleep);
        condit();
    }

    /**
     * 滑动，弃用
     *
     * @param x     the x
     * @param y     the y
     * @param x1    the x 1
     * @param y1    the y 1
     * @param dir   the dir ir = 0从上往下滑动，dir = 1从左往右滑动，dir = 2从下往上滑动，dir = 3从右往左滑动
     * @param sleep the sleep 滑动延时
     * @throws Exception the exception
     *                   <p>
     *                   此方法有问题可能导致滑动不稳定
     */
    public void ranSwipe(int x, int y, int x1, int y1, int dir, int sleep, long sleep1) throws Exception {
        if (dir == 0) {
            int result = x + (int) (Math.random() * ((x1 - x) + 1));
            this.touchDown(result, y);
            this.touchMove(result, y1, sleep);
            this.touchUp();
        } else if (dir == 1) {
            int result = y + (int) (Math.random() * ((y1 - y) + 1));
            this.touchDown(x, result);
            this.touchMove(x1, result, sleep);
            this.touchUp();
        } else if (dir == 2) {
            int result = x + (int) (Math.random() * ((x1 - x) + 1));
            this.touchDown(result, y1);
            this.touchMove(result, y, sleep);
            this.touchUp();
        } else if (dir == 3) {
            int result = y + (int) (Math.random() * ((y1 - y) + 1));
            this.touchDown(x1, result);
            this.touchMove(x, result, sleep);
            this.touchUp();
        }
        Thread.sleep(sleep1);
        condit();
    }

    /**
     * 返回一个区域在很短的时间内有没有变化
     *
     * @param x1 the x 1
     * @param y1 the y 1
     * @param x2 the x 2
     * @param y2 the y 2
     * @return the string  没有变化返回0，0  有变化返回变化的坐标
     * @throws Exception the exception
     */
    public String change(int x1, int y1, int x2, int y2) throws Exception {

        Core.MinMaxLocResult mmr;
        int width;
        int height;
        width = x2 - x1;
        height = y2 - y1;
        Mat mat1 = this.getScreenMat(x1, y1, width, height, 1, 0, 0, 1);


        for (int i = 0; i < 10; i++) {
            Mat mat2 = this.getScreenMat(x1, y1, width, height, 1, 0, 0, 1);
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------mat2=" +mat2.size()+ "," + mat2.channels());
//        Imgcodecs.imwrite("/sdcard/2.png",mat2);

            Mat dst = new Mat();
            Core.absdiff(mat1, mat2, dst);
            Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2GRAY);
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------answerStr=" +dst.size()+ "," + dst.channels());
            Imgproc.threshold(dst, dst, 0, 0, Imgproc.THRESH_TOZERO);

//            LtLog.i(publicFunction.getLineInfo() + "----------------------------answerStr=" +dst.size()+ "," + dst.channels());

            mmr = Core.minMaxLoc(dst);
            if (mmr.maxLoc.x > 0) {
                return ((int) mmr.maxLoc.x + x1) + "," + ((int) mmr.maxLoc.y + y1);
            }
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------answerStr=" + mmr.maxVal  + "," + mmr.maxLoc.x + "," + mmr.maxLoc.y);
        }


        return "0,0";

    }


    /**
     * 返回一个区域没有变化的时间，有变化返回0
     *
     * @param x_1    the x 1
     * @param y_1    the y 1
     * @param width  the width
     * @param height the height
     * @param sim    the sim
     * @return the long  单位是秒
     * @throws Exception the exception
     */
    private Mat mat1, mat2;
    private long timex, time;

    public long mMatTime(int x_1, int y_1, int width, int height, double sim) throws Exception {
        /*
         返回两个图片相等的时间
         */
        boolean matSim = false;
        //ScreenInfo screenInfo=this.capture();
        if (screenInfo.height > 720) {
            LtLog.e("----screenInfo error ---");
            return 0;
        }
        if (mat1 != null) {
            mat1.release();
        }
        mat1 = this.getScreenMat(x_1, y_1, width, height, 1, 0, 0, 1, screenInfo);
        if (mat2 != null && mat1 != null) {
            // LtLog.e(getLineInfo(  "----------------------------mat1.rows=>" + mat1.rows()  + ",mat2.rows="+ mat2.rows()));
           /* try {*/
            matSim = judgeMatAndMatChange(sim, mat1, mat2);

         /*   } catch (Exception e) {
//                LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + e.toString());
            }*/
            mat1.release();
            //判断两个矩阵的相似度大于 sim 则返回 true;
        }
        if (matSim) {
            // LtLog.e(getLineInfo("目前区域没有变化"));
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + matSim + ",timex=" + timex + ",time=" + time);
        } else {
            //如果两个矩阵不相等 重置时间
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + matSim );
            time = System.currentTimeMillis() / 1000;
            if (mat2 != null) {
                mat2.release();
            }
            mat2 = this.getScreenMat(x_1, y_1, width, height, 1, 0, 0, 1);
            return 0;
        }
        timex = System.currentTimeMillis() / 1000 - time;
        return timex;
    }

    /**
     * 初始化MatTime的时间
     */
    public void initMatTime() {
        if (mat2 != null) {
            mat2.release();
        }
        mat2 = null;
    }

    /**
     * 判断两个矩阵的相似度大于 sim 则返回 true;
     */
    private boolean judgeMatAndMatChange(double sim, Mat mat, Mat tempMat) throws Exception {
        //判断两个矩阵的相似度大于 sim 则返回 true;
        boolean state = false;
        Mat dstMat = new Mat(), dst1 = new Mat(), dst2 = new Mat();
        if (mat.channels() == 3 || tempMat.channels() == 3) {
            Imgproc.cvtColor(mat, dst1, Imgproc.COLOR_RGB2HLS);
            Imgproc.cvtColor(tempMat, dst2, Imgproc.COLOR_RGB2HLS);
        }
        Imgproc.matchTemplate(dst1, dst2, dstMat, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr;
        mmr = Core.minMaxLoc(dstMat);
        if (mmr.maxVal >= sim) {
            state = true;
        }
        dstMat.release();
        dst1.release();
        dst2.release();
        return state;
    }

    /**
     * 有几张相同的图片返回第一个先找到的
     * x,x_1,y,y_1   初始范围
     * @param x         the x
     * @param y         the y
     * @param x_1       the x 1
     * @param y_1       the y 1
     * @param img       the img
     * @param sim       the sim
     * @param step      the step 步长  每次范围扩大多少
     * @param max       the max  大于多少次没找到图片结束
     * @param direction the direction 1代表x方向查找  2代表Y方向
     * @return the find result
     * @throws Exception the exception
     */
    public FindResult findPic(int x, int y, int x_1, int y_1, String img, float sim, int step, int max, int direction) throws Exception {
        FindResult result;
        Mat mat = getScreenMat(0, 0, 1280, 720, 1, 0, 0, 1, screenInfo);
        Mat mat1 = getTemplateMat(img);
        TemplateInfo templateInfo = getTemplateInfo(img);
        int js_1 = 0;
        while (direction == 1) {
            //    LtLog.e("范围是=" + x+","+y+","+x_1+","+y_1);
            if ((x_1 - x) > templateInfo.width) {
                Rect rect = new Rect(0, 0, x_1 - x, y_1 - y);
                Mat mat2 = new Mat(mat, rect);
                result = matchMat(x, y, mat1, mat2);
                if (result.sim > sim) {
                    result.x = x + result.x;
                    result.y = y + result.y;
                    //     LtLog.e("result.w=" + result.width+",result.h"+result.height);
                    return result;
                }
            }
            x_1 = x_1 + step;
            if (x_1 >= 1280) {
                return null;
            }
            js_1++;
            if (js_1 > max) {
                return null;
            }
        }
        while (direction == 2) {
            //  LtLog.e("范围是=" + x+","+y+","+x_1+","+y_1);
            if ((y_1 - y) > templateInfo.height) {
                Rect rect = new Rect(x, y, x_1 - x, y_1 - y);
                Mat mat2 = new Mat(mat, rect);
                result = matchMat(0, 0, mat1, mat2);
                if (result.sim > sim) {
                    //    LtLog.e("result.w=" + result.width+",result.h"+result.height);
                    result.x = x + result.x;
                    result.y = y + result.y;
                    return result;
                }
            }
            y_1 = y_1 + step;
            if (y_1 >= 720) {
                return null;
            }
            js_1++;
            if (js_1 > max) {
                return null;
            }
        }
        return null;
    }

    /**
     * 返回星期
     * @return the int
     */
    public int week() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == 1) {
            return 7;
        } else {
            return (cal.get(Calendar.DAY_OF_WEEK)) - 1;
        }
    }

    /**
     * 返回小时
     * @return the int
     */
    public int dateHour() {
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss");
        String nowTime1 = format1.format(new Date());
        int hour = Integer.parseInt(nowTime1.split(":")[0]);
        return hour;
    }

    /**
     * 返回分钟
     * @return the int
     */
    public int dateMinute() {
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss");
        String nowTime1 = format1.format(new Date());
        int minute = Integer.parseInt(nowTime1.split(":")[1]);
        return minute;
    }

    /**
     * 延迟时间
     * @param time the time
     * @throws Exception the exception
     */
    public void sleep(long time) throws Exception {
        Thread.sleep(time);
    }


    Map<Integer, Integer> mMap = new HashMap<>();

    /**
     * 生成一个map  Map<Integer,Integer> mMap= new HashMap<>();已弃用
     * @param number the number  生成key的数量  value全部为0
     * @throws Exception the exception
     */
    public void initmMap(int number) throws Exception {
        mMap.clear();
        for (int i = 1; i <= number; i++) {
            mMap.put(i, 0);
        }
    }

    /**
     * 获取指定key的value, 已弃用
     * @param mkey the mkey
     * @param str  the str
     * @return the map value
     * @throws Exception the exception
     */
    public int getmMapValue(int mkey, String str) throws Exception {
        LtLog.e(str + "=" + mMap.get(mkey));
        return mMap.get(mkey);
    }

    /**
     * 设置指定key的value,已弃用
     * @param mkey   the mkey
     * @param mvalue the mvalue
     * @param str    the str
     * @throws Exception the exception
     */
    public void setmMapValue(int mkey, int mvalue, String str) throws Exception {
        mMap.put(mkey, mvalue);
        LtLog.e(str + "=" + mMap.get(mkey));
    }


    private long fad = 0;
    private long fadTime = 0;
    private int numcolor1 = 0;

    /**
     * 通过颜色数量来进行发呆判断 只能用于横屏
     * @param x
     * @param y
     * @param x_1
     * @param y_1
     * @param strColor
     * @param simDouble
     * @return
     * @throws Exception
     */
    public long dazeTime(int x, int y, int x_1, int y_1, String strColor, double simDouble) throws Exception {
        if (fad == 0) {
            numcolor1 = getColorNum(x, y, x_1, y_1, strColor, simDouble);
            fadTime = System.currentTimeMillis() / 1000;
            fad = 1;
            return 0;
        } else {
            int numcolor2 = getColorNum(x, y, x_1, y_1, strColor, simDouble);
            if (numcolor1 == numcolor2) {
                return (System.currentTimeMillis() / 1000) - fadTime;
            } else {
                fad = 0;
                return 0;
            }
        }
    }//发呆判断


    /**
     * 通过颜色数量来进行发呆判断  可用于横竖屏
     * @param str
     * @param color
     * @param sim
     * @param type
     * @return
     * @throws Exception
     */
    public long dazeTime(String str, String color, double sim, int type) throws Exception {
        if (fad == 0) {
            numcolor1 = getColorNum(str, color, sim, type);
            fadTime = System.currentTimeMillis() / 1000;
            fad = 1;
            return 0;
        } else {
            int numcolor2 = getColorNum(str, color, sim, type);
            if (numcolor1 == numcolor2) {
                return (System.currentTimeMillis() / 1000) - fadTime;
            } else {
                fad = 0;
                return 0;
            }
        }
    }//发呆判断


    /**
     * 颜色发呆判断的初始化
     * @throws Exception
     */
    public void initDaze() throws Exception {
        fad = 0;
        fadTime = 0;
        numcolor1 = 0;
    }


    /**
     * 反复上下滑动一般使用于任务列表
     * @param err           由taskCount中的err来控制
     * @param count         第一个参数必须是初始化的参数
     * @param initSlidCount 初始化滑动的次数 如果为0则不进行
     * @param x             滑动起点
     * @param y             滑动起点
     * @param x1            滑动终点
     * @param y1            滑动终点
     * @param moveSleep     滑动的时间
     * @param stopSleep     滑动停止后多少时间截一张图
     * @throws Exception
     */
    public void taskSlid(int err, int[] count, int initSlidCount, int x, int y, int x1, int y1, int moveSleep, long stopSleep) throws Exception {
        if (initSlidCount != 0 && err == count[0]) {
            for (int i = 0; i < initSlidCount; i++) {
                LtLog.e(getLineInfo("taskSlid初始化滑动>>>"));
                ranSwipe(x1, y1, x, y, moveSleep, stopSleep);
            }
        }
        for (int i = 1; i < count.length; i++) {
            if (err == count[i]) {
                LtLog.e(getLineInfo("taskSlid滑动一下>>>"));
                ranSwipe(x, y, x1, y1, moveSleep, stopSleep);
                return;
            }
        }
    }

    /**
     * 好爱答题
     * @param leftX    需要答题的范围
     * @param leftY    需要答题的范围
     * @param width    需要答题的范围
     * @param height   需要答题的范围
     * @param haoAiNum 上传题目的编号  需要坐标是8006   返回答案是5001
     * @return 没有答案返回“”
     * @throws Exception
     */
    public String haoAi(int leftX, int leftY, int width, int height, String haoAiNum) throws Exception {
        //开始截图
        Mat mat2 = getScreenMat(leftX, leftY, width, height, 1, 0, 0, 1, screenInfo);
        //将图片存入路径
        //Mat转byte[]
        Imgcodecs.imwrite("/sdcard/yunpai_files/111.png", mat2);
        File f1 = new File("/sdcard/yunpai_files/111.png");
        //这里获取好爱HOST
        LtLog.i(getLineInfo("开始获取好爱的HOST"));
        String host = getHtml("http://3.haoi23.net/svlist.html").substring(3, 23);
        if (host == null) {
            host = getHtml("http://3.haoi23.net/svlist.html").substring(3, 23);
        }
        LtLog.i(getLineInfo("请求数据"));
        String a = String.valueOf((int) (1 + Math.random() * 9));
        String b = String.valueOf((int) (1 + Math.random() * 9));
        String c = String.valueOf((int) (1 + Math.random() * 9));
        String d = String.valueOf((int) (1 + Math.random() * 9));
        String e = String.valueOf((int) (1 + Math.random() * 9));
        String f = String.valueOf((int) (1 + Math.random() * 9));
        String g = String.valueOf((int) (1 + Math.random() * 9));
        String h = String.valueOf((int) (1 + Math.random() * 9));
        String ii = String.valueOf((int) (1 + Math.random() * 9));
        String j = String.valueOf((int) (1 + Math.random() * 9));
        String suiji = a + b + c + d + e + f + g + h + ii + j;
        String fanhui = httpPost(host, suiji, haoAiNum);
        // LtLog.i(getLineInfo("请求完成,开始请求TID,TID为" + fanhui));
        a = String.valueOf((int) (1 + Math.random() * 9));
        b = String.valueOf((int) (1 + Math.random() * 9));
        c = String.valueOf((int) (1 + Math.random() * 9));
        d = String.valueOf((int) (1 + Math.random() * 9));
        e = String.valueOf((int) (1 + Math.random() * 9));
        f = String.valueOf((int) (1 + Math.random() * 9));
        g = String.valueOf((int) (1 + Math.random() * 9));
        h = String.valueOf((int) (1 + Math.random() * 9));
        ii = String.valueOf((int) (1 + Math.random() * 9));
        j = String.valueOf((int) (1 + Math.random() * 9));
        suiji = a + b + c + d + e + f + g + h + ii + j;
        for (int i = 0; i < 15; i++) {
            String answerhui = TIDhttpPost(host, fanhui, suiji);
            if (answerhui == "#编号不存在" || answerhui == "#超时") {
                if (f1.exists()) {
                    f1.delete();
                }
                return "";
            }
            if (answerhui.contains(",")) {
                String[] aa = answerhui.split(",");
                sendTongTask(aa[0] + "_" + aa[1]);
            } else {
                sendTongTask(answerhui);
            }
            LtLog.i(getLineInfo("答案是" + answerhui));
            if (f1.exists()) {
                f1.delete();
            }
            return answerhui;
        }
        if (f1.exists()) {
            f1.delete();
        }
        return "";
    }

    /**
     * 获取html
     * @param path
     * @return
     * @throws Exception
     */
    public static String getHtml(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == 200) {
            InputStream inStream = conn.getInputStream();
            byte[] data = readInputStream(inStream);
            String html = new String(data, "UTF-8");
            return html;
        }
        return null;
    }


    /**
     * 读流
     * @param inStream
     * @return
     * @throws Exception
     */
    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }


    /**
     * 请求好爱post
     * @param host
     * @param suiji
     * @param haoAinum
     * @return
     * @throws Exception
     */
    public String httpPost(String host, String suiji, String haoAinum) throws Exception {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userstr", "yunpai|ACMXGAHOAZNDCEED")
                .addFormDataPart("gameid", haoAinum)
                .addFormDataPart("timeout", "60")
                .addFormDataPart("rebate", "3739|6A1962CC9E02B5B9")
                .addFormDataPart("daiLi", "haoi")
                .addFormDataPart("kou", "0")
                .addFormDataPart("beizhu", "2222")
                .addFormDataPart("ver", "web2")
                .addFormDataPart("key", suiji)
                .addFormDataPart("img", GetImageStr("/sdcard/yunpai_files/111.png"))
                .build();
        Request request = new Request.Builder()
                .url("http://" + host + "/UploadBase64.aspx")
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
//            LtLog.i(publicFunction.getLineInfo() + "-------" + "-------------response....." + response.body().string() + "---" + response.toString());
            String str = response.body().string();
            return str;
        } catch (IOException e) {
            LtLog.i(getLineInfo("") + "-------" + "-------------response.....");
            e.printStackTrace();
        }
        return "error";
    }

    /**
     * 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
     * @param imgpath
     * @return
     */
    public String GetImageStr(String imgpath) {
        InputStream in = null;
        byte[] data = null;
        //读取图片字节数组
        try {
            in = new FileInputStream(imgpath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //对字节数组Base64编码
        return new String(android.util.Base64.encode(data, android.util.Base64.DEFAULT));//返回Base64编码过的字节数组字符串
    }

    /**
     * 请求TID
     * @param host
     * @param TID
     * @param suiji
     * @return
     */
    public String TIDhttpPost(String host, String TID, String suiji) {
        System.out.println(TID);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", TID)
                .addFormDataPart("r", suiji)
                .build();
        Request request = new Request.Builder()
                .url("http://" + host + "/GetAnswer.aspx")
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String result = response.body().string();
            try {
                Thread.sleep(3000);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }


    /**
     * 七牛云上传图片
     * @param answer 如果是坐标按照x+"_"+y这个格式写
     * @throws Exception
     */
    public void sendTongTask(String answer) throws Exception {
        Mat mat3 = getScreenMat(0, 0, 1280, 720, 1, 0, 0, 1, screenInfo);
        Imgcodecs.imwrite("/sdcard/screen.png", mat3);

        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd|HHmmss");
        String date = sDateFormat.format(new java.util.Date());
        // LtLog.i(getLineInfo("") + "-------" + "-------------....." + date.split("\\|"));
        String[] dataTime = date.split("\\|");
        //String keyStr = dataTime[0] + "_"+ YpFairyConfig.getGameID()+"_"+x+"_"+y+"_"+ dataTime[1] + ".png";
        String keyStr = dataTime[0] + "_" + YpFairyConfig.getGameID() + "_" + answer + "_" + dataTime[1] + ".png";
        LtLog.i(getLineInfo("七牛云上传" + keyStr));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://api.padyun.com/ws/serverTools.php?act=getUpToken").build();
        String filePath = "/sdcard/screen.png";
        httpPost(filePath, keyStr, client.newCall(request).execute().body().string());
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        return;
    }


    /**
     * 只能识别数字字符串
     * @param x
     * @param y
     * @param width
     * @param height
     * @param minValue
     * @param maxValue
     * @return
     * @throws Exception 示例 getNumber(363, 393, 167, 35, new Scalar(0, 0, 100), new Scalar(100, 100, 150));
     */
    public String getNumber(int x, int y, int width, int height, Scalar minValue, Scalar maxValue) throws Exception {
        if (screenInfo.height > 720) {
            System.out.println("error Screen height >720");
            return null;
        }
        Mat mat = getScreenMat(x, y, width, height, 1, 0, 0, 1, screenInfo);

        Scalar minValues = minValue;
        Scalar maxValues = maxValue;
        Core.inRange(mat, minValues, maxValues, mat);
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(mat, bitmap);
        mat.release();
        tessBaseAPI.setImage(bitmap);
        String number = tessBaseAPI.getUTF8Text();
        tessBaseAPI.clear();
        return number;
    }


    /**
     * 获取下标
     * @param x
     * @param y
     * @param type
     * @return
     * @throws Exception
     */
    public int getIndex(int x, int y, int type) throws Exception {
        int index = -1;

        if (type==0){
            if (x < 0 || y < 0 || x > 1279 || y > 719) {
                return index;
            }
            index = x +(y * 1280);
        }else {
            if (x < 0 || y < 0 || x > 719 || y > 1279) {
                return index;
            }
            index = x +(y * 720);
        }
        return index;
    }

    /**
     * 通过下标获取x
     * @param index
     * @param type
     * @return
     * @throws Exception
     */
    public int IndexX(int index, int type) throws Exception {
        int x;
        if (type == 0) {
            x =(index-4)%1280;
        } else {
            x = (index-4)%720;
        }
        return x;
    }

    /**
     * 通过下标获取y
     * @param index
     * @param type
     * @return
     * @throws Exception
     */
    public int IndexY(int index, int type) throws Exception {
        int y;
        if (type == 0) {
            y =((index-4) /1280);
        } else {
            y =((index-4) / 720);
        }
        return y;
    }

    /**
     * 多点找色
     * @param leftX   范围
     * @param leftY   范围
     * @param rightX   范围
     * @param rightY   范围
     * @param setSim   设置的相似度
     * @param type     0是横屏 1是竖屏
     * @param color     颜色
     * @return
     * @throws Exception
     * 示例   findMultiColor(0,0,1280,720,0.9f,0,new String[]{"92,140,215","29|12|95,171,214","49|-21|83,83,204"});
     */
    public FindResult  findMultiColor(int leftX, int leftY, int rightX, int rightY, float setSim, int type, String[] color) throws Exception {
        byte[] pic;
        FindResult result = new FindResult();
        result.sim=0.1f;
        pic=screenInfo.raw;
        while (pic==null){
            pic=captureBitmap();
            Thread.sleep(1);
        }
        rightX = rightX - 1;
        rightY = rightY - 1;
        int colorNum = color.length;
        int[] anArrayX = new int[colorNum];
        int[] anArrayY = new int[colorNum];
        int[] anArrayR = new int[colorNum];
        int[] anArrayG = new int[colorNum];
        int[] anArrayB = new int[colorNum];
        for (int i = 0; i < colorNum; i++) {
            String[] oneColorarr = color[i].split("\\|");
            if (i == 0) {
                String[] setColorRGB = oneColorarr[0].split(",");
                anArrayX[0] = 0;
                anArrayY[0] = 0;
                anArrayR[0] = Integer.parseInt(setColorRGB[0]);
                anArrayG[0] = Integer.parseInt(setColorRGB[1]);
                anArrayB[0] = Integer.parseInt(setColorRGB[2]);
            } else {
                String[] setColorRGB = oneColorarr[2].split(",");
                anArrayX[i] = Integer.parseInt(oneColorarr[0]);
                anArrayY[i] = Integer.parseInt(oneColorarr[1]);
                anArrayR[i] = Integer.parseInt(setColorRGB[0]);
                anArrayG[i] = Integer.parseInt(setColorRGB[1]);
                anArrayB[i] = Integer.parseInt(setColorRGB[2]);
            }
        }
        int startIndex = getIndex(leftX, leftY, type) * 4 + 16;
        int endIndex = getIndex(rightX, rightY, type) * 4 + 16;
        int R, G, B, oneR = anArrayR[0], oneB = anArrayB[0], oneG = anArrayG[0];
        if (endIndex>=3686400){
            endIndex=3686396;
        }
        float sim;
        for (int i = startIndex; i < endIndex; i += 4) {
            R = (pic[i] & 0xff);
            G = (pic[i + 1] & 0xff);
            B= (pic[i + 2] & 0xff);
            sim = (float) (765 - (Math.abs(R - oneR) + Math.abs(G - oneG) + Math.abs(B - oneB))) / 765;
            if (sim >= setSim) {
                int oneX = IndexX(i / 4, type);
                int oneY = IndexY(i / 4, type);
                int next = 1;
                do {
                    int nextClolorIndex = getIndex(oneX + anArrayX[next], oneY + anArrayY[next], type) * 4 + 16;
                    if (nextClolorIndex < 0) {
                        break;
                    }
                    System.out.println();
                    R = (pic[nextClolorIndex] & 0xff);
                    G = (pic[nextClolorIndex + 1] & 0xff);
                    B = (pic[nextClolorIndex + 2] & 0xff);
                    sim = (float) (765 - (Math.abs(R - anArrayR[next]) + Math.abs(G - anArrayG[next]) + Math.abs(B - anArrayB[next]))) / 765;
                    if (sim >= setSim) {
                    } else {
                        break;
                    }
                    next++;
                    if (next == colorNum) {
                        result.sim=setSim;
                        result.x=oneX;
                        result.y=oneY;
                        return result;
                    }
                } while (next < colorNum);
            }
        }
        return result;
    }

    public int getColorNum(int leftX, int leftY, int rightX, int rightY, double setSim, int type,String color) throws Exception {
        byte[] pic;
        int nowNum = 0;
        pic=screenInfo.raw;
        while (pic==null){
            pic= captureBitmap();
            Thread.sleep(1);
        }
        String[] arr = color.split(",");
        int setr =  Integer.parseInt(arr[0]);
        int setg  =Integer.parseInt(arr[1]);
        int setb =  Integer.parseInt(arr[2]);
        int   width = rightX - leftX;
        int  height = rightY - leftY;
        int setx=leftX,sety=leftY;
        for (int i=0;i<=height;i++){
            for (int j=0;j<=width;j++){
                int startIndex = getIndex(setx, sety, type) * 4 + 16;
                int    R = (pic[startIndex] & 0xff);
                int   G = (pic[startIndex + 1] & 0xff);
                int   B = (pic[startIndex + 2] & 0xff);
                float sim = (float) (765 - (Math.abs(R - setr) + Math.abs(G - setg) + Math.abs(B - setb))) / 765;
                if (sim >= setSim) {
                    nowNum++;
                }
                setx=setx+1;
            }
            setx=leftX;
            sety=sety+1;
        }
        return nowNum;
    }




    public native void multipointFindColor(int x1, int y1, int x2, int y2, byte[] imgByte, long mat, String colorStr_start, String colorStr_sub, double sim, int[] xy);
}
