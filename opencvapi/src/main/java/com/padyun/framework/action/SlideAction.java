package com.padyun.framework.action;

import com.padyun.framework.FairyRect;
import com.padyun.framework.YpTaskFairyImpl;

import java.util.Random;

public class SlideAction implements IAction {

    private int mTapDelay = DEFAULT_DELAY ;
    private FairyRect mStartRect ;
    private FairyRect mEndRect ;
    private int mTimestamp ;
    public  SlideAction(FairyRect startRect, FairyRect endRect, int timestamp){
       setSlide(startRect, endRect, timestamp);
    }
    public void setSlide(FairyRect startRect, FairyRect endRect, int timestamp){
        mStartRect = startRect ;
        mEndRect = endRect ;
        mTimestamp = timestamp ;
    }
    @Override
    public int onAction() {
        int startx,starty,endx,endy ;
        startx = new Random().nextInt(mStartRect.width()) + mStartRect.x() ;
        starty = new Random().nextInt(mStartRect.height()) + mStartRect.y() ;
        endx = new Random().nextInt(mEndRect.width()) + mEndRect.x() ;
        endy = new Random().nextInt(mEndRect.height()) + mEndRect.y() ;
        YpTaskFairyImpl.getFairy().touchDown(startx,starty);
        YpTaskFairyImpl.getFairy().touchMove(endx,endy,mTimestamp);
        YpTaskFairyImpl.getFairy().touchUp();
        return  mTapDelay ;
    }
    public SlideAction setTapDelay(int delay){
        mTapDelay = delay ;
        return  this ;
    }
}
