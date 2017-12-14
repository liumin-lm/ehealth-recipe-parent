package recipe.util;

import ctd.schema.exception.ValidateException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;
import java.util.regex.Pattern;

public class ChinaIDNumberUtil
{
	private static final Pattern pattern = Pattern.compile("[0-9]{17}");
	private static final char[] validateCodes = { '1', '0', 'X', '9', '8', '7',
			'6', '5', '4', '3', '2' };
	private static final int[] wi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10,
			5, 8, 4, 2 };

	public static String convert15To18(String rawIdNumber)
			throws ValidateException {
		//wx2.7 儿童患者需求，身份证标识-前的身份证号需要判断，不整体进行判断
		String idNumber= StringUtils.substringBefore(rawIdNumber, "-");
		//wx2.7 儿童身份证校验后，需要将后缀加回去
		String suffix=StringUtils.substringAfter(rawIdNumber, "-");
		if(!StringUtils.isEmpty(suffix)){
			suffix="-"+suffix;
		}
		int len = idNumber.length();
		if (!(len == 15 || len == 18)) {
			throw new ValidateException("lenth!= 15 or 18");
		}
		String ai = "";
		ai = idNumber.substring(0, 6) + "19" + idNumber.substring(6, 15);
		if (len == 18) {
			ai = idNumber.substring(0, 17);
		} else {
			ai = idNumber.substring(0, 6) + "19" + idNumber.substring(6, 15);
		}
		if (!pattern.matcher(ai).matches()) {
			throw new ValidateException("[0-17] must be number");
		}

		String birth = ai.substring(6, 14);
		try {
			LocalDate birthDay = DateTimeFormat.forPattern("yyyyMMdd")
					.parseLocalDate(birth);
			LocalDate now = new LocalDate();
			int age = Years.yearsBetween(birthDay, now).getYears();
			if (age < 0 || age > 120) {
				throw new ValidateException("BirthdayOverflow[" + birth + "]");
			}
		} catch (RuntimeException e) {
			throw new ValidateException("BirthdayDateInvaid[" + birth + "]");
		}
		int sum = 0;
		for (int i = 0; i < 17; i++) {
			sum += Integer.parseInt(String.valueOf(ai.charAt(i))) * wi[i];
		}
		int mod = sum % 11;
		char c = validateCodes[mod];
		if (idNumber.length() == 18 && idNumber.charAt(17) != c) {
			throw new ValidateException("validatecode was wrong!");

		}
		ai += c;

		return ai+suffix;
	}

	/**
	 * 传入18位身份证,获取出生日期
	 * 
	 * @author zhangx
	 * @date 2016-1-21 上午11:19:42
	 * @param idNumber
	 *            18位身份证号
	 * @throws ValidateException
	 *             校验异常，身份证必须18位
	 * @return yyyyMMdd
	 */
	public static Date getBirthFromIDNumber(String idNumber)
			throws ValidateException {
		
		isValidIDNumber(idNumber);

		String birth = idNumber.substring(6, 14);
		return DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(birth).toDate();
	}

	/**
	 * 传入18位身份证,获取性别
	 * @author zhangx
	 * @date 2016-1-21 上午11:47:36
	 * @param idNumber 18位身份证号
	 * @return 1男；2女
	 * @throws ValidateException  校验异常，身份证必须18位
	 */
	public static String getSexFromIDNumber(String idNumber) throws ValidateException{
		
		isValidIDNumber(idNumber);
		
		int idcardsex = Integer.parseInt(idNumber.substring(
				idNumber.length() - 2, idNumber.length() - 1));
		
		return idcardsex % 2 == 0 ? "2" : "1";
		
	}
	
	/**
	 * 传入18位身份证,获取年龄
	 * @author zhangx
	 * @date 2016-1-21 上午11:47:36
	 * @param idNumber 18位身份证号
	 * @throws ValidateException  校验异常，身份证必须18位
	 */
	public static Integer getAgeFromIDNumber(String idNumber) throws ValidateException{
		isValidIDNumber(idNumber);
		
		String birth = idNumber.substring(6, 14);
		Integer age;
		try {
			LocalDate birthDay = DateTimeFormat.forPattern("yyyyMMdd")
					.parseLocalDate(birth);
			LocalDate now = new LocalDate();
			age = Years.yearsBetween(birthDay, now).getYears();
			if (age < 0 || age > 120) {
				throw new ValidateException("BirthdayOverflow[" + birth + "]");
			}
		} catch (RuntimeException e) {
			throw new ValidateException("BirthdayDateInvaid[" + birth + "]");
		}
		
		return age;
	}
	
	private static void isValidIDNumber(String idNumber) throws ValidateException {
		int len = idNumber.length();
		if (len != 18) {
			throw new ValidateException("lenth!=18");
		}
	}
}
