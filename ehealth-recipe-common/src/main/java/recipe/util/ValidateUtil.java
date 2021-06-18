package recipe.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 0003
 * @date 2016/6/3
 */
public class ValidateUtil {
    private static final String NUMERIC_REGEX = "^\\d+(\\.\\d+)?$";
    private static Pattern p = Pattern.compile("^((13[0-9])|(17[0-9])|(15[0-9])|(18[0-9]))\\d{8}$");

    public static boolean nullOrZeroDouble(Double value){
        if(value==null || value==0) {
            return true;
        }
        return false;
    }

    /**
     * 判断value是否为null或0
     * @param value
     * @return
     */
    public static boolean notNullDouble(Double value){
        if(value == null || value == 0){
            return false;
        } else {
            return true;
        }
    }

    /**
     * 判断value是否为 非0整型
     * @param value
     * @return
     */
    public static boolean notNullAndZeroInteger(Integer value){
        if(value==null){
            return false;
        }
        if(value==0){
            return false;
        }
        return true;
    }

    /**
     * 判断value是否为 null或者0
     * @param value
     * @return
     */
    public static boolean nullOrZeroInteger(Integer value){
        if(value==null){
            return true;
        }
        if(value==0){
            return true;
        }
        return false;
    }

    /**
     * 判断value是否为 非0长整型
     * @param value
     * @return
     */
    public static boolean notNullAndZeroLong(Long value){
        if(value==null){
            return false;
        }
        if(value==0){
            return false;
        }
        return true;
    }
    /**
     * 判断value是否为 null或者0
     * @param value
     * @return
     */
    public static boolean nullOrZeroLong(Long value){
        if(value==null){
            return true;
        }
        if(value==0){
            return true;
        }
        return false;
    }

    /**
     * 是否为 非空字符串
     * @param value
     * @return
     */
    public static boolean notBlankString(String value){
        if(value==null){
            return false;
        }
        if("".equals(value.trim())){
            return false;
        }
        return true;
    }

    /**
     * 是否为 空字符串
     * @param value
     * @return
     */
    public static boolean blankString(String value){
        if(value==null){
            return true;
        }
        if("".equals(value.trim())){
            return true;
        }
        return false;
    }

    /**
     * 判断是否是空集合
     * @param list
     * @return
     */
    public static boolean blankList(List list){
        if(list==null){
            return true;
        }
        if(list.size()==0){
            return true;
        }
        return false;
    }

    /**
     * 判断是否是非空集合
     * @param list
     * @return
     */
    public static boolean notBlankList(List list){
        if(list==null){
            return false;
        }
        if(list.size()==0){
            return false;
        }
        return true;
    }

    /**
     * 判断Boolean是否为true
     * @param arg
     * @return
     */
    public static boolean isTrue(Boolean arg){
        if(arg==null){
            return false;
        }
        return arg;
    }

    /**
     * 判断Boolean是否为false
     * @param arg
     * @return
     */
    public static boolean isNotTrue(Boolean arg){
        if(arg==null){
            return true;
        }
        return !arg;
    }

    /**
     * 判断给定字符串是否为纯数字组合
     * @param value
     * @return
     */
    public static boolean isNum(String value){
        if(blankString(value)){
            return false;
        }
        return isStrictNumeric(value.trim());
    }

    /**
     * 判断给定字符串是否是严格的数值 如：“3.1415”->true;“ 3.1415”->false;
     * @param value
     * @return
     */
    public static boolean isStrictNumeric(String value){
        if(blankString(value)){
            return false;
        }
        return value.matches(NUMERIC_REGEX);
    }

    /**
     * 判断给定的字符串是否不是数值
     * @param value
     * @return
     */
    public static boolean isNotNum(String value){
        if(blankString(value)){
            return true;
        }
        return !isStrictNumeric(value.trim());
    }

    public static boolean isMobile(String mobile) {

        Matcher m = p.matcher(mobile);
        return m.matches();
    }


    public static boolean integerIsEmpty(Integer i) {
        if (null == i || 0 == i) {
            return true;
        }
        return false;
    }

    public static boolean integerIsEmpty(Integer... args) {
        if (null == args) {
            return true;
        }
        for (Integer i : args) {
            if (integerIsEmpty(i)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doubleIsEmpty(Double d) {
        if (null == d || 0 == d) {
            return true;
        }
        return false;
    }

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

    public static boolean validateObject(Object args) {
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
