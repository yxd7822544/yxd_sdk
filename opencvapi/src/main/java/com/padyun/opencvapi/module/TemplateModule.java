package com.padyun.opencvapi.module;



import com.padyun.YpModule2;
import com.padyun.opencvapi.OpencvProtocol;

import java.nio.ByteBuffer;

/**
 * Created by litao on 2018/2/27.
 */
public class TemplateModule extends YpModule2 {
    public String packageName ;
    public int packageVersion ;
    public ByteBuffer template ;
    public String templateName ;
    public String templateMd5 ;
    public TemplateModule(){
        super(OpencvProtocol.TYPE_TEMPLATE);
    }
    public TemplateModule(byte[] data, int offset, int len) throws YpModuleException {
        super(data, offset, len);
    }

    @Override
    public void initField() {
        try {
            fields.put(OpencvProtocol.ATTR_PACKAGE, getClass().getField("packageName")) ;
            fields.put(OpencvProtocol.ATTR_VERSION, getClass().getField("packageVersion")) ;
            fields.put(OpencvProtocol.ATTR_PIC, getClass().getField("template")) ;
            fields.put(OpencvProtocol.ATTR_NAME, getClass().getField("templateName")) ;
            fields.put(OpencvProtocol.ATTR_MD5, getClass().getField("templateMd5")) ;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
