package com.jsh.erp.utils;

import com.alibaba.fastjson2.JSON;

/**
 * @author jishenghua qq752718920  2018-10-7 15:26:27
 */
public class ExtJsonUtils {

    public static String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    public interface ExtExtractor {
        String getExt(Object bean);
    }
}
