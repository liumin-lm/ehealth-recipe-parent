package recipe.util;

import org.springframework.util.StringUtils;

public class ByteUtils {
	public static String COMMA = ",";
	public static String DOT = "\\.";
	public static String SEMI_COLON_EN = ";";
	public static String SEMI_COLON_CH = "；";

	/**
	 * 私有构造函数，不允许本类生成实例
	 */
	private ByteUtils() {

	}

	private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

	/**
	 * 转换字节数组为16进制字串
	 *
	 * @param b 字节数组
	 * @return 16进制字串
	 */
	public static String byteArrayToHexString(byte[] b) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));
		}
		return resultSb.toString();
	}
	
	/**
	 * 字节转为16进制串
	 * @param b
	 * @return
	 */
	public static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n = 256 + n;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
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
	 * 截取 StringBuilder 拼接的最后一个字符 如 "，"
	 *
	 * @param str
	 * @return
	 */
	public static String subString(StringBuilder str) {
		if (StringUtils.isEmpty(str)) {
			return "";
		} else {
			return str.substring(0, str.length() - 1);
		}
	}

	public static String[] split(String str, String regex) {
		if (StringUtils.isEmpty(str)) {
			return new String[0];
		} else {
			return str.split(regex);
		}
	}
}
