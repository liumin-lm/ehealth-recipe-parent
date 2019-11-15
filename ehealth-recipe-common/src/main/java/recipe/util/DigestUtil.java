package recipe.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    /**
     * 转16进制
     *
     * @param bytes
     * @return
     */
    public static String toHex(byte[] bytes) {
        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
        }
        return ret.toString();
    }


    /**
     * 利用MD5进行加密
     *
     * @param str 待加密的字符串
     * @return 加密后的字符串
     */
    public static String encodeMD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes("utf-8"));
            return toHex(bytes);
        } catch (Exception e) {
            return null;
        }
    }


    static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    public static String getMessageDigest(String str, String encName) {
        byte[] digest = null;
        if (StringUtils.isBlank(encName)) {
            encName = "SHA-1";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(encName);
            md.update(str.getBytes(StandardCharsets.UTF_8));
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byteArrayToHex(digest);
    }
}
