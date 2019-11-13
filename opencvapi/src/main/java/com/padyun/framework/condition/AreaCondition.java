package com.padyun.framework.condition;

import com.padyun.opencvapi.FindResult;
import com.padyun.opencvapi.LtLog;
import org.opencv.core.Mat;

public class AreaCondition extends ScreenItem {
        AreaInfo areaInfo ;
        public AreaCondition(AreaInfo areaInfo){
            this.setState(STATE_UNCHANGE);
            this.areaInfo = areaInfo ;
        }
        @Override
        public void looping(Mat screenMat) {
            areaInfo.matchResult(screenMat);
        }

    @Override
    public int getState() {
            return areaInfo.changed()?STATE_CHANGE:STATE_UNCHANGE ;
    }

    @Override
    public FindResult getFindResult() {
        LtLog.e("error areainfo get findresult......") ;
        return null;
    }
}