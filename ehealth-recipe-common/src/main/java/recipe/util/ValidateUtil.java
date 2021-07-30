package recipe.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 校验工具类
 *
 * @author fuzi
 */
public class ValidateUtil {
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
        if (Objects.isNull(args)) {
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
        if (args instanceof Double) {
            return doubleIsEmpty((Double) args);
        }
        return false;
    }
}
