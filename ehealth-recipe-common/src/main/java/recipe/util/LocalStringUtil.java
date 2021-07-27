package recipe.util;

import org.apache.commons.lang3.StringUtils;
import recipe.constant.BussTypeConstant;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * @author zhangx
 * @date 2017/2/22.
 */
public class LocalStringUtil {

    private static final String NUMERIC_REGEX = "^\\d+(\\.\\d+)?$";
    private static Pattern p = Pattern.compile("^((13[0-9])|(17[0-9])|(15[0-9])|(18[0-9]))\\d{8}$");

    /**
     * 用“*”遮盖手机号的中间四位数字，如：13811386878  执行结果为：138****6878
     * 此方法并未对手机号做严格的正则校验
     *
     * @param mobile
     * @return
     */
    public static String coverMobile(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
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
        if (StringUtils.isEmpty(model)) {
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
     * 业务类型1位+时间戳后10位+随机码4位
     *
     * @return
     */
    public static String getOrderCode() {
        StringBuilder orderCode = new StringBuilder();
        orderCode.append(BussTypeConstant.RECIPE);
        String time = Long.toString(Calendar.getInstance().getTimeInMillis());
        orderCode.append(time.substring(time.length() - 10));
        orderCode.append(new Random().nextInt(9000) + 1000);
        return orderCode.toString();
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
