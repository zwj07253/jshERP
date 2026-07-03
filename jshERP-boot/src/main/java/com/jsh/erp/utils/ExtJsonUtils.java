package com.jsh.erp.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.ValueFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jishenghua qq752718920  2018-10-7 15:26:27
 */
public class ExtJsonUtils {

    private static final String EXT_NAME = "ext";

    static class ExtFilter implements ValueFilter, NameFilter {
        private Map<Object, JSONObject> map = new HashMap<>();
        private Map<Object, Set<String>> ignoredKey = new HashMap<>();

        @Override
        public Object apply(Object object, String name, Object value) {
            if (name.equals(EXT_NAME) && value instanceof String) {
                map.put(object, JSON.parseObject((String) value));
                return value;
            }
            if (!map.containsKey(object)) {
                ignoredKey.put(object, new HashSet<>());
            }
            ignoredKey.get(object).add(name);
            return value;
        }

        @Override
        public String process(Object object, String name, Object value) {
            if (map.containsKey(object)) {
                Set<String> ignoredKeys = ignoredKey.getOrDefault(object, new HashSet<>());
                if (ignoredKeys.contains(name)) {
                    return name;
                }
                JSONObject ext = map.get(object);
                if (ext.containsKey(name)) {
                    return name;
                }
            }
            return name;
        }
    }

    public static String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    public interface ExtExtractor {
        String getExt(Object bean);
    }
}
