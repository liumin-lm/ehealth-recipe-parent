package recipe.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校验工具类
 *
 * @author fuzi
 */
public class ValidateUtil {

    private static final Pattern CHINA_PATTERN = Pattern.compile("^((13[0-9])|(14[0,1,4-9])|(15[0-3,5-9])|(16[2,5,6,7])|(17[0-8])|(18[0-9])|(19[0-3,5-9]))\\d{8}$");
    private static final   String PATTERN_HZ= "[\u4e00-\u9fa5]";
    private static final   String PATTERN_YW= "[A-Za-z]";
    private static final   String  PATTERN_ZF="[`~!@#$%^&●◆▲*\\-+={}':;,\\[\\].<>/?￥%…_+|‘；：”“’。，、？\\s]";

    /**
     * 全类型 参数类型校验工具
     *
     * @param args 校验参数
     * @return
     */
    public static boolean validateObjects(Object... args) {
        if (null == args) {
            return true;
        }
        for (Object i : args) {
            if (validateObject(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 全部为空
     *
     * @param args
     * @return 全部为空 = true
     */
    public static boolean validateObjectsIsEmpty(Object... args) {
        if (null == args) {
            return true;
        }
        for (Object i : args) {
            if (!validateObject(i)) {
                return false;
            }
        }
        return true;
    }


    /**
     * todo 新方法使用  validateObjects
     *
     * @param i
     * @return
     */
    public static boolean integerIsEmpty(Integer i) {
        if (null == i || 0 == i) {
            return true;
        }
        return false;
    }

    public static boolean longIsEmpty(Long l) {
        if (null == l || 0 == l) {
            return true;
        }
        return false;
    }


    /**
     * 判断字符串并返回
     *
     * @param parame
     * @return
     */
    public static String isEmpty(String parame) {
        if (StringUtils.isEmpty(parame)) {
            return "";
        } else {
            return parame;
        }
    }

    /**
     * 判断 StringBuilder 为null
     *
     * @param str
     * @return
     */
    public static Boolean isEmpty(StringBuilder str) {
        if (0 > str.length() || StringUtils.isEmpty(str.toString())) {
            return true;
        } else {
            return false;
        }
    }


    private static boolean doubleIsEmpty(Double d) {
        if (null == d || 0 == d) {
            return true;
        }
        return false;
    }

    private static boolean validateObject(Object args) {
        if (null == args) {
            return true;
        }
        if (args instanceof Collection) {
            return CollectionUtils.isEmpty((Collection) args);
        }
        if (args instanceof Map) {
            return ((Map) args).isEmpty();
        }
        if (args instanceof String) {
            return StringUtils.isEmpty((String) args);
        }
        if (args instanceof Integer) {
            return integerIsEmpty((Integer) args);
        }
        if (args instanceof Long) {
            return longIsEmpty((Long) args);
        }
        if (args instanceof Double) {
            return doubleIsEmpty((Double) args);
        }
        return false;
    }

    public static boolean isPhoneLegal(String mobile) {
        try {
            Matcher m = CHINA_PATTERN.matcher(mobile);
            return m.matches();
        } catch (Exception e) {
            return false;
        }
    }


    public static boolean matchHZ( String keyword) {
        String ss = Pattern.compile(PATTERN_HZ).matcher(keyword).replaceAll("");
        if (!StringUtils.isEmpty(keyword) && StringUtils.isEmpty(ss)) {
            return true;
        }
        return false;
    }

    //匹配纯英文--判断是否为纯英文
    public static boolean matchYW( String keyword) {
        String ss = Pattern.compile(PATTERN_YW).matcher(keyword).replaceAll("");
        if (!StringUtils.isEmpty(keyword) && StringUtils.isEmpty(ss)) {
            return true;
        }
        return false;
    }

    //过滤字符串里面的特殊符号
    public static String filterString(String keyword){
        String ss = "";
        if (!StringUtils.isEmpty(keyword)) {
            ss = Pattern.compile(PATTERN_ZF).matcher(keyword).replaceAll("");
        }
        return ss;
    }
}
