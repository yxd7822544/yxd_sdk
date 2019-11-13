package com.padyun.opencvapi.module;

import com.padyun.YpModule2;
import com.padyun.opencvapi.OpencvProtocol;

import java.nio.ByteBuffer;

/**
 * Created by litao on 2018/2/24.
 */
public class PicModule extends YpModule2 {

    public ByteBuffer pic ;
    public String templateName ;
    public String templateMd5 ;
    public String packageName ;
    public int packageVersion ;
    public int flag ;

    public PicModule() {
        super(OpencvProtocol.TYPE_PIC);
    }

    @Override
    public void initField() {
        try {
            fields.put(OpencvProtocol.ATTR_PIC, getClass().getField("pic")) ;
            fields.put(OpencvProtocol.ATTR_NAME, getClass().getField("templateName")) ;
            fields.put(OpencvProtocol.ATTR_MD5, getClass().getField("templateMd5")) ;
            fields.put(OpencvProtocol.ATTR_FLAG, getClass().getField("flag")) ;
            fields.put(OpencvProtocol.ATTR_PACKAGE, getClass().getField("packageName")) ;
            fields.put(OpencvProtocol.ATTR_VERSION, getClass().getField("packageVersion")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
