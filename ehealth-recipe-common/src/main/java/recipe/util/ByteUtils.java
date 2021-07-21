package recipe.util;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ByteUtils {
	public static String COMMA = ",";
	public static String DOT = "\\.";
	public static String DOT_EN = ".";
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

	/**
	 * 截取 str 数据按照regex截取获取数组
	 *
	 * @param str   字符串
	 * @param regex 截取标示
	 * @return
	 */
	public static String[] split(String str, String regex) {
		if (StringUtils.isEmpty(str)) {
			return null;
		} else {
			return str.split(regex);
		}
	}

	/**
	 * 判断 StringBuilder 为null
	 *
	 * @param str
	 * @return
	 */
	public static Boolean isEmpty(StringBuilder str) {
		if (0 > str.length() || StringUtils.isEmpty(str.toString())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 格式化时间
	 *
	 * @param date
	 * @return
	 */
	public static String dateToSting(Date date) {
		return dateToSting(date, "yyyy-MM-dd HH:mm");
	}

	/**
	 * 格式化时间
	 *
	 * @param date
	 * @param pattern
	 * @return
	 */
	public static String dateToSting(Date date, String pattern) {
		if (null == date || StringUtils.isEmpty(pattern)) {
			return "";
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			return sdf.format(date);
		} catch (Exception e) {
			return "";
		}
	}


	public static String hideIdCard(String idCard) {
		if (org.apache.commons.lang3.StringUtils.isEmpty(idCard)) {
			return "";
		}
		try {
			//显示前1-3位
			String str1 = idCard.substring(0, 3);
			//显示后15-18位
			String str2 = idCard.substring(14, 18);
			idCard = str1 + "***********" + str2;
			return idCard;
		} catch (Exception e) {
			return "";
		}
	}

	public static Integer strValueOf(String str) {
		try {
			return Integer.valueOf(str);
		} catch (Exception e) {
			return 0;
		}
	}

	public static String objValueOf(Object str) {
		if (ObjectUtils.isEmpty(str)) {
			return null;
		}
		try {
			return String.valueOf(str);
		} catch (Exception e) {
			return null;
		}
	}

	public static String objValueOfString(Object str) {
		if (ObjectUtils.isEmpty(str)) {
			return "";
		}
		try {
			return String.valueOf(str);
		} catch (Exception e) {
			return "";
		}
	}
}
