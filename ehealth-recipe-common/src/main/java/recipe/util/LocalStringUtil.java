package recipe.util;

import com.ngari.recipe.entity.Recipe;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.BussTypeConstant;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author zhangx
 * @date 2017/2/22.
 */
public class LocalStringUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalStringUtil.class);
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

    public static boolean hasOrgan(String organ, String args){
        if (StringUtils.isNotEmpty(args)) {
            String[] organs = args.split(",");
            List<String> organList = Arrays.asList(organs);
            return organList.contains(organ);
        }
        return false;
    }

    /**
     * 获取区域文本
     *
     * @param area 区域
     * @return 区域文本
     */
    public static String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
        return "";
    }


    public static String getInvalidTime(Recipe recipe) {
        String invalidTime = "3日";
        try {
            if (null != recipe.getInvalidTime()) {
                Date now = new Date();
                long nd = 1000 * 24 * 60 * 60;
                long nh = 1000 * 60 * 60;
                long nm = 1000 * 60;
                long ns = 1000;
                long diff = recipe.getInvalidTime().getTime() - now.getTime();
                // 处方已到失效时间，失效定时任务未执行（每30分钟执行一次）
                if (diff <= 0) {
                    invalidTime = "30分钟";
                } else {
                    long day = diff / nd;
                    long hour = diff % nd / nh;
                    long min = diff % nd % nh / nm;
                    long sec = diff % nd % nh % nm / ns;
                    if (day <= 0 && hour <= 0 && min <= 0 && sec > 0) {
                        invalidTime = "1分钟";
                    } else {
                        hour = hour + (day * 24);
                        invalidTime = hour > 0 ? (hour + "小时") : "";
                        invalidTime = min > 0 ? (invalidTime + min + "分钟") : (invalidTime + "");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("失效时间倒计时计算异常，recipeid={}", recipe.getRecipeId(), e);
        }
        return invalidTime;
    }
}
