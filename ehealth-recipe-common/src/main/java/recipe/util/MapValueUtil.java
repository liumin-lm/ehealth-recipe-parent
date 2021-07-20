package recipe.util;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.beans.BeanMap;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/2.
 */
public class MapValueUtil {
    private static final Logger logger = LoggerFactory.getLogger(MapValueUtil.class);
    public static String getString(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return "";
        }

        if (obj instanceof String) {
            return obj.toString();
        }

        if (obj instanceof Integer) {
            return obj.toString();
        }

        return "";
    }

    public static Integer getInteger(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }

        if (obj instanceof Integer) {
            return (Integer) obj;
        }

        if (obj instanceof String && StringUtils.isNotEmpty(obj.toString())) {
            try {
                return Integer.valueOf(obj.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public static Double getDouble(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }

        if (obj instanceof Double) {
            return (Double) obj;
        }

        try {
            if (obj instanceof Float) {
                return Double.parseDouble(obj.toString());
            }

            if (obj instanceof Integer) {
                return Double.parseDouble(obj.toString());
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    public static BigDecimal getBigDecimal(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }

        try {
            if(obj instanceof Double){
                return new BigDecimal(obj.toString());
            }

            if(obj instanceof Float){
                return new BigDecimal(obj.toString());
            }

            if(obj instanceof Integer){
                return new BigDecimal(obj.toString());
            }

            if(obj instanceof String){
                return new BigDecimal(obj.toString());
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public static List getList(Map<String, Object> map, String key){
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }

        if (obj instanceof List) {
            return (List) obj;
        }

        return null;
    }

    public static Object getObject(Map<String, ? extends Object> map, String key) {
        if (null == map || StringUtils.isEmpty(key)) {
            return null;
        }

        return map.get(key);
    }


    /**
     * 根据字段名获取 对象中的get值
     *
     * @param fieldName 字段名
     * @param o         对象
     * @return
     */
    public static String getFieldValueByName(String fieldName, Object o) {
        if (StringUtils.isEmpty(fieldName) || null == o) {
            logger.info("getFieldValueByName fieldName ={} o ={}", fieldName, JSONUtils.toString(o));
            return "";
        }
        try {
            String getter = "get" + captureName(fieldName.trim());
            Method method = o.getClass().getMethod(getter);
            Object value = method.invoke(o);
            if (null == value) {
                return "";
            }
            if (value instanceof Date) {
                return ByteUtils.dateToSting((Date) value);
            }
            return value.toString();
        } catch (Exception e) {
            logger.warn("getFieldValueByName error fieldName ={}，o ={}", fieldName, o.getClass().toString(), e);
            return "";
        }
    }

    /**
     * 首字母转大写，性能比java自带工具类转大写方法略好
     *
     * @param str
     * @return
     */
    public static String captureName(String str) {
        char[] cs = str.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }

    /**
     * 将string数组根据下标转成map
     *
     * @param strArray
     * @return
     */
    public static Map<String, Integer> strArraytoMap(String[] strArray) {
        if (strArray == null) {
            return null;
        }
        Map<String, Integer> map = new HashMap<>(strArray.length);
        for (int i = 0; i < strArray.length; i++) {
            map.put(strArray[i], i);
        }
        return map;
    }

    /**
     * 对象转map
     *
     * @param bean
     * @param <T>
     * @return
     */
    public static <T> Map<String, Object> beanToMap(T bean) {
        logger.info("MapValueUtil beanToMap bean :{}", JSON.toJSONString(bean));
        Map<String, Object> map = Maps.newHashMap();
        if (bean != null) {
            BeanMap beanMap = BeanMap.create(bean);
            for (Object key : beanMap.keySet()) {
                map.put(key.toString(), beanMap.get(key));
            }
        }
        return map;
    }

}
