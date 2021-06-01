package recipe.util;

import ctd.schema.exception.ValidateException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author yuyun
 */
public class ChinaIDNumberUtil
{
	private static final Pattern PATTERN = Pattern.compile("[0-9]{17}");
	private static final char[] VALIDATE_CODES = { '1', '0', 'X', '9', '8', '7',
			'6', '5', '4', '3', '2' };
	private static final int[] WI = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10,
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
		int len15 = 15;
		int len17 = 17;
		int len18 = 18;
		int age120 = 120;
		if (!(len == len15 || len == len18)) {
			throw new ValidateException("lenth!= 15 or 18");
		}
		String ai = "";
		ai = idNumber.substring(0, 6) + "19" + idNumber.substring(6, 15);
		if (len == len18) {
			ai = idNumber.substring(0, 17);
		} else {
			ai = idNumber.substring(0, 6) + "19" + idNumber.substring(6, 15);
		}
		if (!PATTERN.matcher(ai).matches()) {
			throw new ValidateException("[0-17] must be number");
		}

		String birth = ai.substring(6, 14);
		try {
			LocalDate birthDay = DateTimeFormat.forPattern("yyyyMMdd")
					.parseLocalDate(birth);
			LocalDate now = new LocalDate();
			int age = Years.yearsBetween(birthDay, now).getYears();
			if (age < 0 || age > age120) {
				throw new ValidateException("BirthdayOverflow[" + birth + "]");
			}
		} catch (RuntimeException e) {
			throw new ValidateException("BirthdayDateInvaid[" + birth + "]");
		}
		int sum = 0;
		for (int i = 0; i < len17; i++) {
			sum += Integer.parseInt(String.valueOf(ai.charAt(i))) * WI[i];
		}
		int mod = sum % 11;
		char c = VALIDATE_CODES[mod];
		if (idNumber.length() == len18 && idNumber.charAt(len17) != c) {
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
		int age120 = 120;
		String birth = idNumber.substring(6, 14);
		Integer age;
		try {
			LocalDate birthDay = DateTimeFormat.forPattern("yyyyMMdd")
					.parseLocalDate(birth);
			LocalDate now = new LocalDate();
			age = Years.yearsBetween(birthDay, now).getYears();
			if (age < 0 || age > age120) {
				throw new ValidateException("BirthdayOverflow[" + birth + "]");
			}
		} catch (RuntimeException e) {
			throw new ValidateException("BirthdayDateInvaid[" + birth + "]");
		}

		return age;
	}

	public static void isValidIDNumber(String idNumber) throws ValidateException {
		if (StringUtils.isEmpty(idNumber)) {
			throw new ValidateException("idNumber is null");
		}
		int len = idNumber.length();
		int len18 = 18;
		if (len != len18) {
			throw new ValidateException("lenth!=18");
		}
	}

	/**
	 * 脱敏身份证号
	 *
	 * @param idCard
	 * @return
	 */
	public static String hideIdCard(String idCard) {
		if (StringUtils.isEmpty(idCard)) {
			return "";
		}
        try {
            //显示前1-3位
            String str1 = idCard.substring(0, 3);
            //显示后15-18位
            String str2 = idCard.substring(14, 18);
            idCard = str1 + "***********" + str2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idCard;
	}

    /**
     * 传入18或15位身份证,获取年龄 小于一岁时传递值xx月xx天 大于一岁直接数值字符串类型
     * @param idNumber 18或15位位身份证号
     * @throws ValidateException  校验异常，身份证必须18位
     */
    public static String getStringAgeFromIDNumber(String idNumber) throws ValidateException{
        int len = idNumber.length();
        int len18 = 18;
        if (len != len18) {
            idNumber = convert15To18(idNumber);
        }
        int age120 = 120;
        String birth = idNumber.substring(6, 14);
        Integer age;
        try {
            LocalDate birthDay = DateTimeFormat.forPattern("yyyyMMdd")
                    .parseLocalDate(birth);
            LocalDate now = new LocalDate();
            age = Years.yearsBetween(birthDay, now).getYears();
            if (age < 0 || age > age120) {
                throw new ValidateException("BirthdayOverflow[" + birth + "]");
            }
            if (age == 0){
                int months = Months.monthsBetween(birthDay, now).getMonths();
                int days = Days.daysBetween(birthDay, now).getDays();
                if (months == 0){
                    return days+"天";
                }else {
                    days = days - months*30;
                    return months+"月"+days+"天";
                }
            }
        } catch (RuntimeException e) {
            throw new ValidateException("BirthdayDateInvaid[" + birth + "]");
        }
        return String.valueOf(age);
    }

	/**
	 * 根据出生日期,获取年龄
	 * @author liumin
	 * @date 2020-7-28 上午11:47:36
	 * @throws
	 */
	public static Integer getAgeFromBirth(String birth) throws ValidateException {
		int age120 = 120;
		Integer age;
		try {
			LocalDate birthDay = DateTimeFormat.forPattern("yyyy-MM-dd")
					.parseLocalDate(birth);
			LocalDate now = new LocalDate();
			age = Years.yearsBetween(birthDay, now).getYears();
			if (age < 0 || age > age120) {
				throw new ValidateException("getAgeFromBirth ageOverflageow[" + birth + "]");
			}
		} catch (RuntimeException | ValidateException e) {
			throw new ValidateException("getAgeFromBirth[" + birth + "]");
		}

		return age;
	}
    public static void main(String[] args) throws Exception{
        System.out.println(getAgeFromBirth("2012-01-01"));
    }
}
