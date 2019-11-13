package com.padyun.utils;

import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.YpFairyConfig;
import com.padyun.opencvapi.module.HeartBeatModule;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by litao on 2018/9/30.
 */
public class Heartbeat {
    private Timer mTimer ;
    private HeartBeatTask mTask ;
    private byte[] mHeartBeatData ;
    private byte[] mRecvData ;
    private int mPort ;
    private String mIp = "127.0.0.1" ;
    public Heartbeat(String packageName, String serviceName){
        LtLog.i("heart beat package name:"+packageName+" service name:" + serviceName ) ;
        mTimer = new Timer() ;
        mTask = new HeartBeatTask() ;
        HeartBeatModule module = new HeartBeatModule() ;
        module.packageName = packageName ;
        module.serviceName = serviceName ;
        mHeartBeatData = module.toDataWithLength().array() ;
        mRecvData = new byte[10] ;
        try {
            mPort = Integer.parseInt(YpFairyConfig.getASPort()) ;
        }catch (Exception e){}
    }
    public void start(){
        if(mPort > 0) {
            mTimer.schedule(mTask, 0, 5000);
        }else{
            LtLog.e("start heart beat error port :"+mPort) ;
        }
    }
    private void sendHeart(){
        LtLog.i("send heart ......") ;
        try {
            Socket socket = new Socket(mIp,mPort) ;
            socket.getOutputStream().write(mHeartBeatData);
            socket.getInputStream().read(mRecvData) ;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class HeartBeatTask extends TimerTask{

        @Override
        public void run() {
            sendHeart() ;
        }
    }
}
