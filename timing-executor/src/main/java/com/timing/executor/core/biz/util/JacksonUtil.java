package com.timing.executor.core.biz.util;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;

/**
 * Created by winstone on 2017/8/18.
 */
public class JacksonUtil {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    public static ObjectMapper getInstance() {
        return objectMapper;
    }

    /**
     * bean、array、List、Map --> json
     *
     * @param obj
     * @return json string
     * @throws Exception
     */
    public static String writeValueAsString(Object obj) {
        try {
            return getInstance().writeValueAsString(obj);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * string --> bean、Map、List(array)
     *
     * @param jsonStr
     * @param clazz
     * @return obj
     * @throws Exception
     */
    public static <T> T readValue(String jsonStr, Class<T> clazz) {
        try {
            return getInstance().readValue(jsonStr, clazz);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static <T> T readValueRefer(String jsonStr, Class<T> clazz) {
        try {
            return getInstance().readValue(jsonStr, new TypeReference<T>() { });
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
