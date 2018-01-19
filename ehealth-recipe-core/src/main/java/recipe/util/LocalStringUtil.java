package recipe.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author zhangx
 * @date 2017/2/22.
 */
public class LocalStringUtil
{
    public static final String DEFAULT_CONNECTOR_CHAR = ",";
    private static final String INTEGER_SEPARATOR_CHAR = "";
    private static final String STRING_SEPARATOR_CHAR = "'";

    /**
     * 将list转换为逗号（,）分割的字符串
     *
     * @param list
     * @return
     */
    public static String listToIntArrayString(List<? extends Integer> list) {
        return listToStringWithSeparator(list, DEFAULT_CONNECTOR_CHAR, INTEGER_SEPARATOR_CHAR);
    }

    public static String listToStringArrayString(List<? extends String> list) {
        return listToStringWithSeparator(list, DEFAULT_CONNECTOR_CHAR, STRING_SEPARATOR_CHAR);
    }

    public static String listToStringWithSeparator(List<?> list, String connector, String separator) {
        if (ValidateUtil.blankList(list)) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (Object obj : list) {
            if (obj != null) {
                sb.append(separator);
                sb.append(obj.toString());
                sb.append(separator);
                sb.append(connector);
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * 四舍五入，保留两位小数
     *
     * @param value
     * @return
     */
    public static String roundDouble(Double value) {
        return roundDouble(value, 2);
    }

    /**
     * 四舍五入，保留给定位数小数
     *
     * @param value
     * @param afterDotBits 小数位数
     * @return
     */
    public static String roundDouble(Double value, int afterDotBits) {
        if (value == null){
            return null;
        }
        char seperator = '.';
        char padChar = '0';
        if(value.toString().indexOf(seperator)>0){
            Long temp = Math.round(value*Math.pow(10, afterDotBits));
            String regex = "(?<=\\d+)\\B(?=\\d{" + afterDotBits + "}$)";
            return temp.toString().replaceFirst(regex, String.valueOf(seperator));
        }else {
            StringBuffer sb = new StringBuffer();
            sb.append(seperator);
            for(int i=0; i<afterDotBits; i++){
                sb.append(padChar);
            }
            return value.toString() + sb.toString();
        }
    }

    /**
     * 将浮点数中多余的0去掉 如： 20.10 执行结果为： 20.1
     * 20.000 执行结果为： 20
     *
     * @param orginValue
     * @return
     */
    public static String removeRedundantZeroForFloatNumber(String orginValue) {
        if (orginValue == null){
            return null;
        }
        String newValue = orginValue.replaceAll("(\\.0+$)|(?<=\\.\\d{0,7})0(?=0+$|\\b)", "");
        return newValue;
    }

    /**
     * 格式化给定数值字符串， 如：12345678.9 执行结果为：12,345,678.9
     * 此方法要求输入字符串为数值字符串
     *
     * @param numeric
     * @return
     */
    public static String formatNumeric(String numeric) {
        if (ValidateUtil.blankString(numeric)) {
            return numeric;
        }
        numeric = numeric.trim();
        String regex1 = "^\\d+(\\.\\d+)?$";
        if (!numeric.matches(regex1)) {
            return numeric;
        }
        String regex = "\\B(?=((\\d{3})+(?!\\d)))";
        return numeric.replaceAll(regex, ",");
    }

    /**
     * 用“*”遮盖手机号的中间四位数字，如：13811386878  执行结果为：138****6878
     * 此方法并未对手机号做严格的正则校验
     *
     * @param mobile
     * @return
     */
    public static String coverMobile(String mobile) {
        if (ValidateUtil.blankString(mobile)) {
            return mobile;
        }
        String regex = "(?<=\\d{3})\\d(?=\\d{4})";
        return mobile.replaceAll(regex, "*");
    }

    /**
     * 用给定参数args依次替换给定model中的标记{}
     *
     * @param model
     * @param args
     * @return
     */
    public static String format(String model, Object... args) {
        if (ValidateUtil.blankString(model)) {
            return model;
        }
        String result = model.replaceAll("\\{\\s\\}", "{}");
        if(args==null){
            return model.replaceFirst("\\{\\}", "");
        }
        if (args.length == 0) {
            return model;
        }
        for (Object arg : args) {
            if (result.indexOf("{}") == -1) {
                break;
            }
            if (arg != null) {
                arg = arg.toString().replaceAll("\\$", "\\\\\\$");
            }
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }

    public static String processTemplate(String tpl, Map<String, ?> params) {
        Iterator<String> it = params.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            Object v = params.get(k);
            String val = v == null ? "" : v.toString();
            tpl = tpl.replace("${" + k + "}", val);
        }
        return tpl;
    }

    /**
     * 处理姓名敏感
     * 1.全为文字没有表情：第一个文字+**
     * 2.名字第一个是表情：表情+**
     * 3.名字中间有表情：第一个文字+**
     * 4.名字最后一个是表情：第一个文字+**
     * 5.只有一个表情为名字：表情+**
     * 6.有多个表情为名字:第一个表情+**
     * @param name
     * @return
     */
    public static String coverName(String name) {
        //处理患者姓名头为emoji表情的情况
        String emoji = name.replaceFirst("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
        if (StringUtils.isEmpty(emoji)) {
            return name + "**";
        } else if (name.length() == emoji.length()) {
            return name.substring(0, 1) + "**";
        } else {
            int emojiIndex = name.indexOf(emoji);
            if(emojiIndex>0){
                return name.substring(0, emojiIndex) + "**";
            }else{
                return name.substring(0, 1) + "**";
            }

        }
    }

    public static String replaceChinese(String variable){
        return variable.replaceAll("[^x00-xff]*", "");
    }

    public static String getSubstringByDiff(String str,String diff){
        if (str.contains(diff)) {
            int index = str.indexOf(diff);
            return str.substring(0, index);
        }else {
            return  str;
        }

    }

    /**
     * 对象toString
     * @param obj
     * @return
     */
    public static String toString(Object obj){
        if(null == obj){
            return "";
        }

        return obj.toString();
    }
}
