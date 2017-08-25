package com.timing.executor.core.biz.model;

/**
 * Created by winstone on 2017/8/18.
 */
public class RegistyConfig {


    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{ EXECUTOR, ADMIN }


}
