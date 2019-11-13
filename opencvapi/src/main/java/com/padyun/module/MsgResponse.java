package com.padyun.module;


import com.padyun.Protocol;
import com.padyun.YpModule;

/**
 * Created by litao on 2018/1/18.
 */
public class MsgResponse extends YpModule {
    public int state ;
    public MsgResponse() {
        super(Protocol.TYPE_RESPONSE);
    }
    public MsgResponse(byte[] data, int offset,int count){
        super(data, offset, count);
    }

    @Override
    public void initField() {
        try {
            fields.put(Protocol.ATTR_STATE, getClass().getField("state")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
