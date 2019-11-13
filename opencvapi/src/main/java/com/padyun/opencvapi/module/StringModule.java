package com.padyun.opencvapi.module;


import com.padyun.YpModule2;
import com.padyun.opencvapi.OpencvProtocol;

/**
 * Created by litao on 2018/2/27.
 */
public class StringModule extends YpModule2 {
    public String str ;
    public StringModule(short type){
        super(type);
    }
    public StringModule(byte[] data, int offset, int len) throws YpModuleException {
        super(data, offset, len);
    }

    @Override
    public void initField() {
        try {
            fields.put(OpencvProtocol.ATTR_STR, getClass().getField("str")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
