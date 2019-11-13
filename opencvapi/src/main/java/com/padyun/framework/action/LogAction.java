package com.padyun.framework.action;

import com.padyun.opencvapi.LtLog;

public class LogAction implements IAction {
    private String log ;
    public LogAction(String log){
        this.log = log ;
    }
    @Override
    public int onAction() {
        LtLog.i(log) ;
        return 0;
    }
}
