package recipe.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/10/17
 */
public class DigestUtil {

    public static String md5For32(String str) {
        byte[] bytes = DigestUtils.md5(str);
        int i;//定义整型
        //声明StringBuffer对象
        StringBuilder buf = new StringBuilder("");
        for (int offset = 0; offset < bytes.length; offset++) {
            i = bytes[offset];//将首个元素赋值给i
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");//前面补0
            buf.append(Integer.toHexString(i));//转换成16进制编码
        }

        return buf.toString();
    }

    public static String md5For16(String str) {
        String s = md5For32(str);
        return s.substring(8, 24);//输出16位16进制字符串
    }
}
