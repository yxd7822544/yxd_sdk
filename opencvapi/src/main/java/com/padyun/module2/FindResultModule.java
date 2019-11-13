package com.padyun.module2;

import com.padyun.Protocol;
import com.padyun.YpModule2;
import com.padyun.opencvapi.FindResult;

/**
 * Created by litao on 2018/3/29.
 */
public class FindResultModule extends YpModule2 {
    public int x ;
    public int y ;
    public int width ;
    public int height ;
    public int sim ;
    public int timestamp ;

    public FindResultModule() {
        super(Protocol.TYPE_FIND_RESULT);
    }

    public FindResultModule(byte []data, int offset, int count) throws YpModuleException {
        super(data, offset, count);
    }
    @Override
    public void initField() {
        try {
            fields.put(Protocol.ATTR_X, getClass().getField("x")) ;
            fields.put(Protocol.ATTR_Y, getClass().getField("y")) ;
            fields.put(Protocol.ATTR_SIM, getClass().getField("sim")) ;
            fields.put(Protocol.ATTR_TIMESTAMP, getClass().getField("timestamp")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
