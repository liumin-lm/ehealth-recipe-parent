package recipe.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/30
 * @description： 正则表达式
 * @version： 1.0
 */
public enum RegexEnum {

    /**
     * 国内手机号
     */
    MOBILE("0?(13|14|15|18|17)[0-9]{9}"),
    /**
     * 国内电话号码
     */
    TEL("[0-9-()（）]{7,18}"),

    /**
     * email
     */
    EMAIL("\\w[-\\w.+]*@([A-Za-z0-9][-A-Za-z0-9]+\\.)+[A-Za-z]{2,14}"),

    /**
     * 数字
     */
    NUMBER("[0-9]*");

    private String exp;

    private RegexEnum(String exp) {
        this.exp = exp;
    }

    public String getExp() {
        return exp;
    }


    /**
     * 匹配是否符合正则表达式pattern 匹配返回true
     *
     * @param str   匹配的字符串
     * @param regex 匹配模式
     * @return boolean
     */
    public static boolean regular(String str, RegexEnum regex) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        Pattern p = Pattern.compile(regex.getExp());
        Matcher m = p.matcher(str);
        return m.matches();
    }

}
