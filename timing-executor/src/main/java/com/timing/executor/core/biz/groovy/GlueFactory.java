package com.timing.executor.core.biz.groovy;

import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import groovy.lang.GroovyClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by winstone on 2017/8/23.
 */
public class GlueFactory {

    private static Logger logger = LoggerFactory.getLogger(GlueFactory.class);


    /**
     * groovy claassLoader
     */

    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    private static GlueFactory glueFactory = new GlueFactory();

    public static  GlueFactory getInstance(){
        return glueFactory;
    }


    /**
     * 实现注册bean的属性
     * @param instance
     */
    private void injectService(Object instance){
       if(instance == null){
           return ;
       }
      Field[] fields = instance.getClass().getDeclaredFields();
      for(Field field : fields){
          if(Modifier.isStatic(field.getModifiers())){
              continue;
          }
       Object fieldBean = null;
       if(AnnotationUtils.getAnnotation(field,Resource.class)!=null){
           try {
               Resource resource = AnnotationUtils.getAnnotation(field,Resource.class);
               if(resource.name()!=null && resource.name().length()>0){ //
                   fieldBean = TimingJobExecutor.getApplicationContext().getBean(resource.name());
               }else{
                   fieldBean = TimingJobExecutor.getApplicationContext().getBean(field.getName());
               }
           } catch (BeansException e) {
               e.printStackTrace();
           }
           if(fieldBean == null){
               fieldBean = TimingJobExecutor.getApplicationContext().getBean(field.getType());
           }
       }else if(AnnotationUtils.getAnnotation(field, Autowired.class)!=null) {
           Qualifier qualifier = AnnotationUtils.getAnnotation(field,Qualifier.class);
           if(qualifier != null && qualifier.value()!=null && qualifier.value().length()>0 ){
                fieldBean = TimingJobExecutor.getApplicationContext().getBean(qualifier.value());
           }else{
               fieldBean = TimingJobExecutor.getApplicationContext().getBean(field.getType());
           }
       }

        if(fieldBean!=null){
            field.setAccessible(true);
            try {
                field.set(instance,fieldBean);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
      }


    }


    /**
     * 记载codesource,动态生成jobHandler
     * @param codeSource
     * @return
     * @throws Exception
     */
    public AbstractJobHandler loadNewInstance(String codeSource) throws Exception{
        if(codeSource!=null && codeSource.trim().length()>0){
           Class<?> clazz = groovyClassLoader.parseClass(codeSource);
           if(clazz!=null){
               Object instance = clazz.newInstance();
               if(null!=instance){
                 if(instance instanceof  AbstractJobHandler){
                   this.injectService(instance);
                   return (AbstractJobHandler)instance;
                 }else{
                     throw new IllegalArgumentException("cannot convert from instance["+ instance.getClass() +"] to IJobHandler");
                 }
               }
           }
        }
        throw new IllegalArgumentException("instance is null");
    }




}
