package com.timing.executor.core.biz.handler.impl;

import com.timing.executor.core.biz.groovy.GlueTypeEnum;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.log.TimingJobFileAppender;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.util.ScriptUtil;

/**
 * Created by winstone on 2017/8/21.
 */
public class ScriptJobExecuteHandler extends AbstractJobHandler {

    private int jobId;
    private long glueUpdatetime;
    private String gluesource;
    private GlueTypeEnum glueType;

    public ScriptJobExecuteHandler(int jobId, long glueUpdatetime, String gluesource, GlueTypeEnum glueType){
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.glueType = glueType;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public ReturnT<String> execute(String... params) throws Exception {

        //构造脚本执行文件名称
        String cmd = "bash";
        String scriptFileName = null;
        if (GlueTypeEnum.GLUE_SHELL == glueType) {
            cmd = "bash";
            scriptFileName = TimingJobFileAppender.logPath.concat("gluesource/").concat(String.valueOf(jobId)).concat("_").concat(String.valueOf(glueUpdatetime)).concat(".sh");
        } else if (GlueTypeEnum.GLUE_PYTHON == glueType) {
            cmd = "python";
            scriptFileName = TimingJobFileAppender.logPath.concat("gluesource/").concat(String.valueOf(jobId)).concat("_").concat(String.valueOf(glueUpdatetime)).concat(".py");
        }

        //构建执行脚本文件
        ScriptUtil.markScriptFile(scriptFileName,gluesource);

        //构建脚本执行日志文件
        String logFileName = TimingJobFileAppender.logPath.concat(TimingJobFileAppender.contextHolder.get());

        //执行脚本文件
        int exitValue = ScriptUtil.execToFile(cmd,scriptFileName,logFileName,params);
        ReturnT<String> result = (exitValue==0)?ReturnT.SUCCESS:new ReturnT<String>(ReturnT.FAIL_CODE, "script exit value("+exitValue+") is failed");
        return result;
    }
}
