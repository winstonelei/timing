package com.timing.job.admin.core.enums;

/**
 * Created by winstone on 2017/8/23.
 */
public enum ExecutorFailStrategyEnum {


    FAIL_ALARM("失败告警"),

    FAIL_RETRY("失败重试");


    private final String titile;

    private ExecutorFailStrategyEnum(String titile){
        this.titile = titile;
    }

    public String getTitile(){
        return  titile;
    }


    public static ExecutorFailStrategyEnum match(String name,ExecutorFailStrategyEnum defaultItem){
       if(null != name){
           for(ExecutorFailStrategyEnum item : ExecutorFailStrategyEnum.values()){
               if(item.name().equals(name)){
                   return item;
               }
           }
       }
       return defaultItem;
    }


}
