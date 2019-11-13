package com.padyun.opencvapi.module;

import com.padyun.YpModule2;
import com.padyun.opencvapi.OpencvProtocol;

/**
 * Created by litao on 2018/2/24.
 */
public class ResultModule extends YpModule2 {

    public int x ;
    public int y ;
    public int sim ;

    public ResultModule(byte[] data, int offset, int len) throws YpModuleException {
        super(data, offset, len);
    }

    @Override
    public void initField() {
        try {
            fields.put(OpencvProtocol.ATTR_X, getClass().getField("x")) ;
            fields.put(OpencvProtocol.ATTR_Y, getClass().getField("y")) ;
            fields.put(OpencvProtocol.ATTR_SIM, getClass().getField("sim")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
