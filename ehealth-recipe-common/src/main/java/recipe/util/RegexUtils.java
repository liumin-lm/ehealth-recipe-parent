package recipe.util;

import recipe.constant.RegexEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/30
 * @description： 正则校验工具类
 * @version： 1.0
 */
public class RegexUtils {

    public static void main(String[] args) {
        System.out.println(RegexUtils.regular("18058735530", RegexEnum.MOBILE));
    }

    /**
     * 匹配是否符合正则表达式pattern 匹配返回true
     *
     * @param str     匹配的字符串
     * @param pattern 匹配模式
     * @return boolean
     */
    public static boolean regular(String str, RegexEnum regex) {
        if (null == str || str.trim().length() <= 0)
            return false;
        Pattern p = Pattern.compile(regex.getExp());
        Matcher m = p.matcher(str);
        return m.matches();
    }
}
