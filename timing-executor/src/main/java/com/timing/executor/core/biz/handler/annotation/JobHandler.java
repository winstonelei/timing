package com.timing.executor.core.biz.handler.annotation;

import java.lang.annotation.*;

/**
 * Created by winstone on 2017/8/21.
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JobHandler {

    String value() default "";
}
