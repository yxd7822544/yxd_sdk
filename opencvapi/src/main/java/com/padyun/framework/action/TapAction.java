package com.padyun.framework.action;

import com.padyun.framework.FairyRect;
import com.padyun.framework.YpTaskFairyImpl;

import java.util.Random;

public class TapAction implements IAction {
    private FairyRect mRect ;
    private int mTapDelay = DEFAULT_DELAY;
    public TapAction(FairyRect rect){
        mRect = rect ;
    }
    @Override
    public int onAction(){
        int x = new Random().nextInt(mRect.width()) + mRect.x() ;
        int y = new Random().nextInt(mRect.height()) + mRect.y() ;

        try {
            YpTaskFairyImpl.getFairy().tap(x,y) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  mTapDelay ;
    }
    /**
     * 设置点击后到延迟
     * */
    public TapAction setTapDelay(int delay){
        mTapDelay = delay ;
        return this ;
    }
}
