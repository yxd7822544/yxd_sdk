package com.padyun.opencvapi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.text.TextUtils;

import com.padyun.framework.condition.Brain;
import com.padyun.framework.condition.ImageInfo;
import com.padyun.opencvapi.utils.YpControl;
import com.padyun.utils.Heartbeat;
import com.padyun.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class YpFairyService extends Service {

    public static final int CAPTURE_SRC_LOCATION = 1;
    public static final int CAPTURE_SRC_YPSERVICE = 2;
    public static final int YPSERVICE_CAPTURE_VERSION = 225;

    public static final String GAME_MONITOR_NAME = "com.padyun.gamemonitor";
    public static final String ACTION_STOP = "ypfairy_stop";
    public static final String ACTION_USR_CONNECT = "ypfairy_user_connect";
    public static final String ACTION_USR_DISCONNECT = "ypfairy_user_disconnect";
    public static final String ACTION_RESUME = "ypfairy_resume";
    public static final String ACTION_PAUSE = "ypfairy_pause";
    public static final String ACTION_DEBUG = "ypfairy_debug";
    public static final String ACTION_DEBUG_CANCEL = "ypfairy_debug_cancel";
    public static final String DEBUG_NAME = "image";
    public static final String DEBUG_ID = "id";
    public static final String ACTION_CHANGE_CONFIG = "ypfairy_change_config";
    public static final String FAIRY_ACTION = "fairy_action";
    public static final String KEY_RESULT = "KEY_RESULT";
    private static final int RESULT_CODE = 1;
    private static final int MONITOR_RESULT_CODE = 2;
    private static Class sStarter;
    private YpFairy2 mYpFairy;
    private Handler mHandler;
    private BroadcastReceiver mReceiver;
    private int mUserConnectedCount = 0;
    private ITaskService.Stub mBinder;
    private boolean mIsMonitorPackage = false;
    private boolean mMonitorPackageExist = false;
    private int mCaptureSrc = CAPTURE_SRC_LOCATION;

    public static void setStarterClass(Class starter) {
        sStarter = starter;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, YpFairyService.class);
        context.startService(intent);
    }

    public static void startService(Context context, Intent resultIntent) {
        Intent intent = new Intent(context, YpFairyService.class);
        intent.putExtra(KEY_RESULT, resultIntent);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LtLog.i("YpFairyService onCreate");
        mReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_STOP);
        intentFilter.addAction(ACTION_RESUME);
        intentFilter.addAction(ACTION_USR_CONNECT);
        intentFilter.addAction(ACTION_USR_DISCONNECT);
        intentFilter.addAction(ACTION_CHANGE_CONFIG);
        intentFilter.addAction(ACTION_DEBUG);
        intentFilter.addAction(ACTION_DEBUG_CANCEL);
        registerReceiver(mReceiver, intentFilter);

        mBinder = new ITaskService.Stub() {

            @Override
            public boolean disposeState(int state) throws RemoteException {
                if (mYpFairy != null) {
                    return mYpFairy.onMonitorState(state);
                }
                return false;
            }
        };

        try {
            PackageInfo info = null;
            ;
            info = getPackageManager().getPackageInfo("com.padyun.ypservice", 0);
            if (info.versionCode >= YPSERVICE_CAPTURE_VERSION) {
                mCaptureSrc = CAPTURE_SRC_YPSERVICE;
            } else {
                mCaptureSrc = CAPTURE_SRC_LOCATION;
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {
            //是否存在独立的监控app
            PackageInfo info = null;
            info = getPackageManager().getPackageInfo(GAME_MONITOR_NAME, 0);
            if (info != null) {
                mMonitorPackageExist = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //判断当前应用是否是游戏监控
        mIsMonitorPackage = getPackageName().equals(GAME_MONITOR_NAME);

        LtLog.i("YpFairyService start package:" + getPackageName() + " monitorpackage exist:" + mMonitorPackageExist);

        createNotificationChannel();

    }

    public static boolean isApkInDebug(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveInfo() {
        long curr = System.currentTimeMillis();
        int day = (int) (curr / 1000 / 60 / 24);
        String fileName = "/sdcard/yunpai_files/fairy_" + day + ".lt";
        if (new File(fileName).exists()) {
            return;
        }
        long freeMem = mYpFairy.getFreeMem();
        long gameMem = mYpFairy.getUsergameMem();
        String info = "";
        info += "free mem:";
        if (freeMem < 1024) {
            info += freeMem + " KB";
        } else {
            info += freeMem / 1024 + " MB";
        }
        info += "\n";
        info += "game " + YpFairyConfig.getGamePackage() + " use mem:";
        if (gameMem < 1024) {
            info += gameMem + " KB";
        } else {
            info += gameMem / 1024 + " MB";
        }
        info += "\n";
        Utils.saveFile(fileName, info.getBytes());

    }


    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = "my_channel_01";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = "云派辅助";
//         用户可以看到的通知渠道的描述
        String description = "云派辅助";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(id, name, importance);
            //         配置通知渠道的属性
            mChannel.setDescription(description);
//         设置通知出现时的闪灯（如果 android 设备支持的话）
            mChannel.enableLights(true); mChannel.setLightColor(Color.RED);
//         设置通知出现时的震动（如果 android 设备支持的话）
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
//         最后在notificationmanager中创建该通知渠道 //
            mNotificationManager.createNotificationChannel(mChannel);

            // 为该通知设置一个id
            int notifyID = 1;
            // 通知渠道的id
            String CHANNEL_ID = "my_channel_01";
            // Create a notification and set the notification channel.
            Notification notification = null;
            notification = new Notification.Builder(this)
                    .setContentTitle("云派") .setContentText("辅助运行中....")
                    .setChannelId(CHANNEL_ID)
                    .build();
            startForeground(1,notification);
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, int startId) {
        LtLog.i("on start command flags:" + flags + " startId:" + startId + "mFairy:" + mYpFairy + " Service:" + this);
        if (intent == null) {
            LtLog.i("on start command intent null...... startId:" + startId);
            return super.onStartCommand(intent, flags, startId);
        }
        if (mCaptureSrc == CAPTURE_SRC_LOCATION && intent.getParcelableExtra(KEY_RESULT) == null && mYpFairy == null) {
            LtLog.i("on start command result null...... startId:" + startId);
            RequestPermissionActivity.startActivity(this);
            return super.onStartCommand(intent, flags, startId);
        }
        //游戏监控传进来的action
        final String fairyAction = intent.getStringExtra(FAIRY_ACTION);
        if (mYpFairy == null) {
            if (sStarter != null) {
                Intent result = intent.getParcelableExtra(KEY_RESULT);
                LtLog.i("on start command:" + result);
                if (mCaptureSrc == CAPTURE_SRC_YPSERVICE) {
                    result = null;
                }
                Constructor constructor = null;
                try {
                    constructor = sStarter.getConstructor(Context.class, Intent.class);
                    mYpFairy = (YpFairy2) constructor.newInstance(YpFairyService.this, result);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                //new Thread( () -> mYpFairy.onStart()).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        YpFairyConfig.initConfig();
                        YpControl.getInstance().setEnableControl(true);
                        //默认启动检测任务并设置连接数为0
                        mUserConnectedCount = 0;

                        //被杀后重启
                        if (flags != 0) {
//                                saveInfo();
                            if (!TextUtils.isEmpty(YpFairyConfig.getTaskID())) {
                                LtLog.i("fairy call Restart....");
                                mYpFairy.onRestart();
                            } else {
                                LtLog.i("fairy Restart not execute ....");
                            }
                        }

                        if (mIsMonitorPackage || !mMonitorPackageExist) {
                            if (fairyAction == null || fairyAction.equals(ACTION_USR_DISCONNECT)) {
                                mYpFairy.onCheckStart();
                            }
                        }
                        mYpFairy.startService();


                        if (mYpFairy.keepAvlie()) {
                            Heartbeat heartbeat = new Heartbeat(getPackageName(), getClass().getName());
                            heartbeat.start();
                        }
                        try {
                            mYpFairy.onStart();
                        } catch (Exception e) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            e.printStackTrace(new PrintStream(baos));
                            String exception = baos.toString();
                            LtLog.i("ypfairy onstart exception:" + exception);
                        }

                    }

                }).start();
            } else {
                LtLog.e("error starter class not set.....");
            }
        } else {
            LtLog.i("on start command mYpFairy not null startId:" + startId + " fairyaction:" + fairyAction);
            YpControl.getInstance().setEnableControl(true);
            YpFairyConfig.initConfig();
            long start = System.currentTimeMillis();
            if (fairyAction != null) {
                disposeFairyCmd(fairyAction);
            } else {
                //默认做恢复处理
                disposeFairyCmd(ACTION_RESUME);
            }
            long end = System.currentTimeMillis();
            if (end - start > 1000) {
                LtLog.i("dispose action:" + intent.getAction() + " use:" + (end - start));
            }


        }
        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    private void disposeFairyCmd(String action) {
        switch (action) {
            case ACTION_PAUSE:
                YpControl.getInstance().setEnableControl(false);
                mYpFairy.onPause();
                break;
            case ACTION_RESUME:
                YpControl.getInstance().setEnableControl(true);
                YpFairyConfig.initConfig();
                mYpFairy.onResume();
                break;
            case ACTION_STOP:
                YpControl.getInstance().setEnableControl(false);
                mYpFairy.onStop();
                break;
            case ACTION_CHANGE_CONFIG:
                YpFairyConfig.initConfig();
                mYpFairy.onChangeConfig();
                break;
            case ACTION_USR_CONNECT:
                if (mYpFairy != null) {
                    mYpFairy.playerRestart();
                    mYpFairy.onCheckStop();
                }
                break;
            case ACTION_USR_DISCONNECT:
                if (mYpFairy != null) {
                    if (mIsMonitorPackage || !mMonitorPackageExist) {
                        mYpFairy.onCheckStart();
                    } else {
                        LtLog.i("onCheckStart not called " + getPackageName() + " monitorPackage exist:" + mMonitorPackageExist);
                    }
                }
                break;
        }

    }

    private boolean isMonitorAction(String action) {
        switch (action) {
            case ACTION_USR_CONNECT:
            case ACTION_USR_DISCONNECT:
                return true;
        }
        return false;
    }

    private  boolean isDebugAction(String action){
        switch (action){
            case ACTION_DEBUG :
            case ACTION_DEBUG_CANCEL:
                return true ;
        }
        return false ;
    }

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            LtLog.i("YpFairyService:" + intent.getAction() + " in");
            if (mYpFairy == null) {
                return;
            }
            //监控自己接收监控的事件
            if (mIsMonitorPackage && isMonitorAction(intent.getAction())) {
                return;
            }
            if(isDebugAction(intent.getAction())){
                String image = intent.getStringExtra(DEBUG_NAME) ;
                int id = intent.getIntExtra(DEBUG_ID,-1) ;
                boolean debug = intent.getAction().equals(ACTION_DEBUG) ;
                if(image == null && id == -1){
                    ImageInfo.clearDebug();
                    Brain.clearDebug();
                }else if(id != -1){
                    Brain.setDebug(id, debug);
                }else if(image != null){
                    ImageInfo.setDebugInfo(image,debug);
                }else{
                    LtLog.i("debug error.......id:"+id+" image:"+image) ;
                }
                return;
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    disposeFairyCmd(intent.getAction());
                    long end = System.currentTimeMillis();
                    if (end - start > 1000) {
                        LtLog.i("dispose action:" + intent.getAction() + " use:" + (end - start));
                    }
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LtLog.i("YpFairyService:" + intent.getAction() + " out");
            if (mIsMonitorPackage) {
                setResultCode(MONITOR_RESULT_CODE);
            }

        }
    }
}
