package com.padyun.opencvapi.utils;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;
import com.padyun.opencvapi.LtLog;

import java.util.Random;

/**
 * Created by litao on 2018/3/29.
 */
public class YpControl {

    private static final int MAX_POINTER    = 10 ;
    private static final int DEFAULT_TOUCHID    = 0 ;
    private Instrumentation mInstrumentation ;
    private TouchProtocol.MutilTouchEvent mMTevent ;
    private boolean mEnableControl = true ;
    private static YpControl sInstance ;

    public static YpControl getInstance(){
        if(sInstance == null){
            sInstance = new YpControl() ;
        }
        return  sInstance ;
    }

    private YpControl(){
        mInstrumentation = new Instrumentation() ;
        mMTevent = new TouchProtocol.MutilTouchEvent();
    }

    public void setEnableControl(boolean enable){
        mEnableControl = enable ;
    }

    public void tap(int x, int y)throws Exception{
        if(!mEnableControl){
            LtLog.i("tap return becasue not enabled.....") ;
            return;
        }
        touchDown(x, y);
        Random random = new Random() ;
        int i = random.nextInt(150) ;
        if(i < 40){
            i+= 80 ;
        }

        Thread.sleep(i);
        touchUp();
    }
    public void touchDown(float x, float y){
        if(!mEnableControl){
            return;
        }
        touchDown(DEFAULT_TOUCHID, x, y);
    }
    public  void touchDown(int id, float x, float y)
    {
        if(!mEnableControl){
            return;
        }
        synchronized(mMTevent){
            if(mMTevent.ids == null){
                mMTevent.ids = new int[MAX_POINTER] ;
            }
            if(mMTevent.coords == null){
                mMTevent.coords = new MotionEvent.PointerCoords[MAX_POINTER] ;
                for(int i = 0 ;i < MAX_POINTER ; ++i)
                {
                    mMTevent.coords[i] = new MotionEvent.PointerCoords() ;
                }
            }
            int sit = idSit(id) ;

            //过滤重复按下id
            if(mMTevent.count > 0 && sit >= 0 ){
                //TouchDown 在已存在同样id的前提下 上一次操作只能是这个id的抬起操作
                if(  (mMTevent.action != MotionEvent.ACTION_UP &&  (mMTevent.action & MotionEvent.ACTION_POINTER_UP) != MotionEvent.ACTION_POINTER_UP)  || mMTevent.action >> 8 != sit )
                {
                    return ;
                }
            }
            if(mMTevent.count == 0){
                mMTevent.action = MotionEvent.ACTION_DOWN ;
                mMTevent.count++ ;
            }else{
                if( (mMTevent.action & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP ){
                    mMTevent.action = mMTevent.action ^ MotionEvent.ACTION_POINTER_UP | MotionEvent.ACTION_POINTER_DOWN ;
                }else{
                    mMTevent.action = mMTevent.count << 8 | MotionEvent.ACTION_POINTER_DOWN ;
                    mMTevent.count++ ;
                }
            }
            sit = (mMTevent.action >> 8) ;
            MotionEvent.PointerCoords coords = mMTevent.coords[sit] ;
            mMTevent.ids[sit] = id ;
            coords.x = x ;
            coords.y = y ;
            sendEvent(mMTevent.action) ;
        }
    }

    public void touchUp(){
        touchUp(DEFAULT_TOUCHID);
    }
    public void touchUp(int id){
        synchronized(mMTevent){
            if(mMTevent.count == 0 || id == -1){
                resetEvent() ;
                return ;
            }
            int sit = idSit(id) ;

            if(sit < 0){
                return ;
            }

            if( (mMTevent.action & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP ){
                LtLog.i("touchup remove last up pointer") ;
                removeLastUpPointer();
            }
            if(mMTevent.count == 1){
                mMTevent.action = MotionEvent.ACTION_UP ;
            }else{
                mMTevent.action =  sit << 8 | MotionEvent.ACTION_POINTER_UP ;
            }
            sendEvent(mMTevent.action);
            if(mMTevent.count == 1){
                mMTevent.count = 0 ;
            }
        }

    }

    public void touchMove(float x, float y, int duration){
        if(!mEnableControl){
            return;
        }
        touchMove(DEFAULT_TOUCHID, x, y, duration);
    }
    public void touchMove(int id, float x, float y, int duration, int step)throws Exception{
        if(!mEnableControl){
            return;
        }
        if(mMTevent.ids == null){
            return ;
        }
        if(mMTevent.coords == null){
            return ;
        }
        if(mMTevent.count <=0 ){
            return ;
        }

        if(duration <= 0)
        {
            duration = 1000 ;
        }
        int i = duration / step ;
        if( i <= 0){
            return ;
        }
        int eventSit = idSit(id) ;
        if(eventSit < 0){
            return ;
        }
        MotionEvent.PointerCoords coord = mMTevent.coords[eventSit] ;
        int j = 0;

        float xStep = (x - coord.x)*1.0f / step ;
        float yStep = (y - coord.y)*1.0f / step ;
        while (true) {

                if (j < step) {


                    j++;
                    coord.x = coord.x +  xStep;
                    coord.y = coord.y +  yStep;
                    Thread.sleep(i);
                    //if(coord.x < x && coord.y < y) {
                        sendEvent(MotionEvent.ACTION_MOVE);
                    //}
                }else{
                    coord.x = x ;
                    coord.y = y ;
                    sendEvent(MotionEvent.ACTION_MOVE);
                    break ;
                }
        }

    }
    public void touchMove(int id, float x, float y, int duration )
    {
        if(!mEnableControl){
            return;
        }
        if(mMTevent.ids == null){
            return ;
        }
        if(mMTevent.coords == null){
            return ;
        }
        if(mMTevent.count <=0 ){
            return ;
        }

        if(duration <= 0)
        {
            duration = 1000 ;
        }
        int i = duration / 11 ;
        if( i <= 0){
            return ;
        }
        int eventSit = idSit(id) ;
        if(eventSit < 0){
            return ;
        }
        MotionEvent.PointerCoords coord = mMTevent.coords[eventSit] ;
        int j = 0;

        float xStep = (x - coord.x) / 11 ;
        float yStep = (y - coord.y) / 11 ;
        while (true) {
            try {
                if (j < 11) {
                    j++;
                    coord.x = coord.x +  xStep;
                    coord.y = coord.y +  yStep;
                    Thread.sleep(i);
                    //mMTevent.action = MotionEvent.ACTION_MOVE ;
                    sendEvent(MotionEvent.ACTION_MOVE);
                }else{
                    coord.x = x ;
                    coord.y = y ;
                    sendEvent(MotionEvent.ACTION_MOVE);
                    break ;
                }
            } catch (InterruptedException localInterruptedException) {
                localInterruptedException.printStackTrace();
            }
        }

    }

    public  void sendEvent(int action){
        if(mMTevent.count <=0 )
        {
            return ;
        }

        long time = SystemClock.uptimeMillis() ;
        MotionEvent event = MotionEvent.obtain(time, time,
                action, mMTevent.count, mMTevent.ids, mMTevent.coords,
                0, 1.0f, 1.0f, 0, 0, 0, 0) ;
        mInstrumentation.sendPointerSync(event);
    }

    private  int idSit(int id){
        for(int i = 0 ;i < mMTevent.ids.length ; ++i ){
            if(mMTevent.ids[i] == id){
                return i ;
            }
        }
        return -1 ;
    }
    private void removeLastUpPointer(){
        int upSit = mMTevent.action >> 8 ;
        for(int i = upSit ; i < mMTevent.ids.length -1  ; ++i){
            mMTevent.ids[i] = mMTevent.ids[i+1] ;
            mMTevent.coords[i].x = mMTevent.coords[i+1].x ;
            mMTevent.coords[i].y = mMTevent.coords[i+1].y ;
        }
        mMTevent.count-- ;
    }
    private void resetEvent(){
        while(mMTevent.count > 0){
            touchUp(mMTevent.ids[0]) ;
        }
    }
}
