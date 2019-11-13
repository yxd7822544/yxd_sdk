package com.padyun.framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.padyun.framework.condition.ImageInfo;
import com.padyun.opencvapi.utils.TemplateInfo;

public class ImageRange implements IRange {

    private int w,h ;

    public ImageRange(String image){
        TemplateInfo info = YpTaskFairyImpl.getFairy().getTemplateInfo(image) ;
        w = info.width ;
        h = info.height ;
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }
}
