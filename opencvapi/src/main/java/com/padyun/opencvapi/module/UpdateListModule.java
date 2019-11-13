package com.padyun.opencvapi.module;


import com.padyun.YpModule2;
import com.padyun.opencvapi.OpencvProtocol;

import java.util.List;

/**
 * Created by litao on 2018/2/27.
 */
public class UpdateListModule extends YpModule2 {
    public List<String> updateList ;
    public UpdateListModule(){
        super(OpencvProtocol.TYPE_UPDATELIST);
    }
    public UpdateListModule(byte[] data, int offset, int len) throws YpModuleException {
        super(data, offset, len);
    }

    @Override
    public void initField() {
        try {
            fields.put(OpencvProtocol.ATTR_LIST, getClass().getField("updateList")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
