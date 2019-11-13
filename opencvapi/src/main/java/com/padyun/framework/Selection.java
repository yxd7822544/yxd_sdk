package com.padyun.framework;


import com.padyun.framework.action.IAction;
import com.padyun.framework.condition.Brain;
import com.padyun.framework.condition.IConditionItem;
import com.padyun.opencvapi.LtLog;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建一个选择器
 * */
public class Selection implements IConditionItem {
    public Brain.CounterCondition mCondition ;
    public List<IConditionItem> list ;

    /**
     * 通过计数器构造一个选择器
     * @param counterCondition   可通过brain拿到一个计数器
     * */
    public Selection(Brain.CounterCondition counterCondition){
        list =new ArrayList<>();
        mCondition = counterCondition ;
    }

    /**
     * 添加一个conditionitem ;
     * */
    public void addCondtion(IConditionItem t){
        list.add(t) ;
    }

    @Override
    public boolean result(Mat screenMat) {
        IConditionItem t = list.get(mCondition.getCount()) ;
        return t.result(screenMat) ;
    }
}
