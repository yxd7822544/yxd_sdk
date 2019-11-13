package com.padyun.yxd.framework;

import com.padyun.opencvapi.FindResult;

import java.util.Timer;

/**
 * Created by Administrator on 2019/2/19 0019.
 */
public class TimerInit {

    private  long  timer=System.currentTimeMillis();
    private  long  timer_1=System.currentTimeMillis();
    private  int  count=0;

    /**
     *
     *获取到没有进入任何场景的时间
     * @return the timer
     */
    public long getTimer() {
        this.timer_1= this.timer-this.timer_1;
        return this.timer_1;
    }

    /**
     *
     * 设置参数进行计时
     * @param sim    the sim
     * @param result the result
     */
    public void setTimer(float sim,FindResult result) {
        if (result.sim>sim){
            timerInit();
        }else {
            this.timer = System.currentTimeMillis();
        }
    }

    /**
     *
     * 设置参数进行计时
     */
    public void setTimer() {
        this.timer = System.currentTimeMillis();
    }

    /**
     *
     *获取到没有进入任何场景的次数
     * @return the count
     */
    public long getCount() {
        return  this.count;
    }

    /**
     *
     *设置参数进行计次
     * @param sim    the sim
     * @param result the result
     */
    public void setCount(float sim,FindResult result) {
        if (result.sim>sim){
            countInit();
        }else {
            this.count ++;
        }
    }

    /**
     *
     * 设置参数进行计次
     */
    public void setCount() {
         this.count ++;
    }

    /**
     *
     * 初始化时间
     */
    public void timerInit() {
      this.timer=System.currentTimeMillis();
      this.timer_1=System.currentTimeMillis();
    }

    /**
     * 初始化次数
     */
    public void countInit() {
       this.count=0;
    }

}
