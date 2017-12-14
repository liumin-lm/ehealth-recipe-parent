package recipe.util;

import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/2.
 */
public class MapValueUtil {

    public static String getString(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return "";
        }

        if (obj instanceof String) {
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

        if (obj instanceof String) {
            try {
                return Integer.valueOf(obj.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public static Date getDate(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }

        if (obj instanceof Date) {
            return (Date) obj;
        }

        return null;
    }

    public static Float getFloat(Map<String, ? extends Object> map, String key) {
        Object obj = getObject(map, key);
        if (null == obj) {
            return null;
        }

        if (obj instanceof Float) {
            return (Float) obj;
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
     * json 转换为 实体对象
     *
     * @param str
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T fromJson(String str, Class<T> type) {
        try {
            T t = JSONUtils.parse(str, type);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * asciicode 转为中文
     *
     * @param asciicode eg:{"code":400002,"msg":"\u7b7e\u540d\u9519\u8bef"}
     * @return eg:{"code":400002,"msg":"签名错误"}
     */
    public static String ascii2native(String asciicode) {
        String[] asciis = asciicode.split("\\\\u");
        String nativeValue = asciis[0];
        try {
            for (int i = 1; i < asciis.length; i++) {
                String code = asciis[i];
                nativeValue += (char) Integer.parseInt(code.substring(0, 4), 16);
                if (code.length() > 4) {
                    nativeValue += code.substring(4, code.length());
                }
            }
        } catch (NumberFormatException e) {
            return asciicode;
        }
        return nativeValue;
    }

    /**
     * 获取当前执行程序的本机地址
     *
     * @return
     */
    public static String getLocalHostIP() {
        String localhostIP = null;
        try {
            localhostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localhostIP;
    }

}
