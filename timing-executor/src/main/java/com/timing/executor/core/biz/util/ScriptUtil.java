package com.timing.executor.core.biz.util;

import com.timing.executor.core.biz.log.TimingJobFileAppender;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by winstone on 2017/8/18.
 */
public class ScriptUtil {

    /**
     * 构建脚本执行文件
     * @param scriptFileName
     * @param context
     * @throws Exception
     */
    public static void markScriptFile(String scriptFileName,String context)throws  Exception{

        File filePathDir = new File(TimingJobFileAppender.logPath);
        if(!filePathDir.exists()){
            filePathDir.mkdirs();
        }

        File filePathSourceDir = new File(filePathDir,"gluesource");
        if(!filePathSourceDir.exists()){
            filePathSourceDir.mkdirs();
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(context.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
           if(fileOutputStream!=null){
               fileOutputStream.close();
           }
        }
    }


    /**
     * 执行文件
     * @param Command
     * @param scriptFile
     * @param logFile
     * @param params
     * @return
     * @throws Exception
     */
    public static int execToFile(String Command,String scriptFile,String logFile,String... params)throws Exception{
        //构建输入流
        FileOutputStream fileOutputStream = new FileOutputStream(logFile,true);
        PumpStreamHandler streamHandler = new PumpStreamHandler(fileOutputStream,fileOutputStream,null);

        //构建command
        CommandLine command = new CommandLine(Command);
        command.addArgument(scriptFile);
        if(params!=null && params.length>0){
            command.addArguments(params);
        }

        //执行命令
        DefaultExecutor exec = new DefaultExecutor();
        exec.setExitValues(null);
        exec.setStreamHandler(streamHandler);
        int exitValue = exec.execute(command);
        return exitValue;
    }



}
