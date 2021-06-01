package recipe.drugsenterprise.bean.yd.utils;


import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class EncrypterUtils {
    static private byte b[] = {
            79, 62, 93, 38, -125, -45, -70, -104, -99, -85, -33, -70, -122, -71, 109,
            -51, -23, 121, -29, 73, -42, 118, 42, -23};
    static private SecretKey KEY = new SecretKeySpec(b, "DESede");
    static private String CHAR_SET = "UTF-8";

    public EncrypterUtils() {
    }

    /**
     * 加密数据，传入一个字符串，返回加密后的数据
     * @param str
     * @return
     */
    public static String encode(String str) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException, BadPaddingException,
            IllegalBlockSizeException, IllegalStateException {
        if (str == null) {
            return null;
        }

        Cipher cp = Cipher.getInstance("DESede");
        cp.init(Cipher.ENCRYPT_MODE, KEY);

        byte ptext[] = str.getBytes(CHAR_SET);
        byte[] ctext = cp.doFinal(ptext);
        String r = getString(ctext);
        return r;
    }

    /**
     * 解密数据，传入一个字符串，返回解密后的数据
     * @param str
     * @return
     */
    public static String decode(String str) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IllegalStateException,
            UnsupportedEncodingException {
        if (str == null) {
            return null;
        }
        Cipher cp = Cipher.getInstance("DESede");
        cp.init(Cipher.DECRYPT_MODE, KEY);
        byte[] ctext = getByte(str);
        byte strbyte[] = cp.doFinal(ctext);
        String r = new String(strbyte, CHAR_SET);
        return r;

    }

    /**
     * str是以','分割的数字，转换为字符数组
     * @param str
     * @return
     */
    static private byte[] getByte(String str) {
        String strs[] = str.split(",");
        byte[] bytes = new byte[strs.length];
        for (int i = 0; i < strs.length; i++) {
            bytes[i] = (new Byte(strs[i])).byteValue();
        }
        return bytes;
    }

    /**
     * 将bytes数组转换为字符串，由一系列的数字来表示
     * @param bytes
     * @return
     */
    static private String getString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Byte.toString(bytes[i]) + ",");
        }
        return sb.toString();
    }

}