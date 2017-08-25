package com.timing.executor.core.biz.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by winstone on 2017/8/21.
 */
public class TimingLogger {

    private static Logger logger = LoggerFactory.getLogger(TimingLogger.class);
    private static SimpleDateFormat loggerFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static void log(String appendLog){

       String logFileName = TimingJobFileAppender.contextHolder.get();//通过threaLocal获取上下文文件名称

       if(logFileName==null || logFileName.trim().length() == 0){
           return;
       }

        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        StackTraceElement callInfo = stackTraceElements[1];

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(loggerFormat.format(new Date())).append(" ")
                .append("["+ callInfo.getClassName() +"]").append("-")
                .append("["+ callInfo.getMethodName() +"]").append("-")
                .append("["+ callInfo.getLineNumber() +"]").append("-")
                .append("["+ Thread.currentThread().getName() +"]").append(" ")
                .append(appendLog!=null?appendLog:"");
        String formatAppendLog = stringBuffer.toString();


       TimingJobFileAppender.appendLog(logFileName,formatAppendLog);

        logger.warn("[{}]: {}", logFileName, formatAppendLog);
    }


    public static void log(String appendLogPattern, Object ... appendLogArguments){

        String appendLog = MessageFormat.format(appendLogPattern, appendLogArguments);
        log(appendLog);

    }

}
