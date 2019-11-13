package com.padyun.framework.condition;

import org.opencv.core.Mat;

/**
 * Created by litao on 2018-12-05.
 */
public interface  IConditionItem {
    public  boolean  result(Mat screenMat) ;
}
