package com.timing.executor.core.biz.log;

import com.timing.executor.core.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by winstone on 2017/8/21.
 */
public class TimingJobFileAppender {

    private static Logger logger = LoggerFactory.getLogger(TimingJobFileAppender.class);
    public static final InheritableThreadLocal<String> contextHolder = new InheritableThreadLocal<>();
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static String logPath = "F:/tmp/data/applogs/job/jobhandler/";



   public static String makeLogFileName(Date triggerDate,int logId){

      File filePathDir = new File(logPath);
      if(!filePathDir.exists()){
          filePathDir.mkdirs();
      }

      String nowFormat = sdf.format(triggerDate);

      File filePathDateDir = new File(filePathDir,nowFormat);

      if(!filePathDateDir.exists()){
          filePathDateDir.mkdirs();
      }

      String fileName = TimingJobFileAppender.sdf.format(triggerDate).concat("/").concat(String.valueOf(logId)).concat(".log");

      return fileName;
   }




  public static void appendLog(String logFileName,String appendLog){

     if(appendLog == null){
         appendLog = "";
     }

     appendLog += "\r\n";

     if(logFileName == null || logFileName.trim().length() == 0){
         return ;
     }

     File file = new File(logPath,logFileName);

     if(!file.exists()){
        try{
            file.createNewFile();
        }catch (Exception e){
            logger.error(e.getMessage());
            return;
        }
     }

      FileOutputStream fos = null;
      try {
          fos = new FileOutputStream(file,true);
          fos.write(appendLog.getBytes("utf-8"));
          fos.flush();
      } catch (Exception e) {
          e.printStackTrace();
      }finally {
          if(fos!=null){
              try {
                  fos.close();
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
      }
  }


    public static LogResult readLog(String logFileName,int fromLineNum){

      if(logFileName == null || logFileName.trim().length() ==0 ){
          return new LogResult(fromLineNum,0,"read fail",true);
      }

      File logFile = new File(logPath,logFileName);

      if(!logFile.exists()){
         return new LogResult(fromLineNum,0,"file is not exists",true);
      }

      StringBuffer sb = new StringBuffer();

      int toLineNum = 0;
      LineNumberReader lineReader = null;
      try {
        lineReader = new LineNumberReader(new InputStreamReader(new FileInputStream(logFile),"utf-8"));
        String line = null;

        while((line = lineReader.readLine())!=null){
           toLineNum = lineReader.getLineNumber();
           if(toLineNum>=fromLineNum){
               sb.append(line).append("\n");
           }
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if(lineReader!=null){
            try {
                lineReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
      }
      LogResult  result = new LogResult(fromLineNum,toLineNum,sb.toString(),true);
      return result;
    }


    public static String readAllLines(File logFile){
      BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile),"utf-8"));
            if(reader != null){
                StringBuffer sb = new StringBuffer();
                String line = null;
                while((line = reader.readLine())!=null){
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
      return null;
    }


}
