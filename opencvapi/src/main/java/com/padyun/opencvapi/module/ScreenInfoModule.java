package com.padyun.opencvapi.module;

import com.padyun.YpModule2;
import com.padyun.opencvapi.utils.YpScreencap2;

import java.nio.ByteBuffer;

/**
 * Created by litao on 2018/7/19.
 */
public class ScreenInfoModule extends YpModule2 {
    public int width ;
    public int height ;
    public int timestamp ;
    public ByteBuffer img ;
    public ScreenInfoModule(byte[] data, int offset, int len) throws YpModuleException {
        super(data, offset, len);
    }
    public ScreenInfoModule(){
        super(YpScreencap2.TYPE_IAMGE);
    }

    @Override
    public void initField() {
        try {
            fields.put(YpScreencap2.ATTR_WIDTH, getClass().getField("width")) ;
            fields.put(YpScreencap2.ATTR_HEIGHT, getClass().getField("height")) ;
            fields.put(YpScreencap2.ATTR_IMG, getClass().getField("img")) ;
            fields.put(YpScreencap2.ATTR_TIMESTAMP, getClass().getField("timestamp")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }
}
