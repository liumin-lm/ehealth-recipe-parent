package recipe.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author yuyun
 */
public class DateConversion
{

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY = "yyyy";
    public static final String YYYY__MM__DD = "yyyy/MM/dd";

    public static final String DEFAULT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

	public static final String PRESCRITION_DATE_TIME = "yyyyMMddHHmmss";

	public static final String DEFAULT_DATETIME_WITHSECOND = "yyyy-MM-dd HH:mm:ss";

	public static Date getDateTimeDaysAgo(int days) {
		DateTime dt = new DateTime();
		return dt.minusDays(days).toDate();
	}

	/**
	 * 获取某一天的0点,
	 * 如2017-04-21 12:09:06的这一天的0点
	 * @param date
	 * @return
	 */
	public static  Date firstSecondsOfDay(Date date){
		return new DateTime(date).withMillisOfDay(0).toDate();
	}

	/**
	 * 获取某一天的23:59:59
	 * 如2017-04-21 12:09:06的这一天的23:59:59
	 * @param date
	 * @return
	 */
	public static  Date lastSecondsOfDay(Date date){
		return new DateTime(date).plusDays(1).withMillisOfDay(0).minusSeconds(1).toDate();
	}

	/**
	 * 
	 * @return
	 */
	public static String getDateFormatter(Date date, String formatter) {
		DateFormat df = new SimpleDateFormat(formatter);
		return df.format(date);
	}

	/**
	 * 根据时间字符串 和指定格式 返回日期
	 * 
	 * @param dateStr
	 * @param format
	 * @return
	 */
	public static Date getCurrentDate(String dateStr, String format) {
		Date currentTime = null;
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		try {
			currentTime = formatter.parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
		return currentTime;
	}

	public static Date getFormatDate(Date date, String format) {
		Date currentTime = null;
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		try {
			String currentTimeStr = formatter.format(date);
			currentTime = formatter.parse(currentTimeStr);
		} catch (ParseException e) {
			return null;
		}
		return currentTime;
	}

	/**
	 * 几月前
	 * 
	 * @author luf
	 * @param months
	 *            月份
	 * @return Date
	 */
	public static Date getMonthsAgo(int months) {
		LocalDate dt = new LocalDate();
		return dt.minusMonths(months).toDate();
	}

	/**
	 * 几年前
	 * 
	 * @author luf
	 * @param years
	 *            年数
	 * @return Date
	 */
	public static Date getYearsAgo(int years) {
		LocalDate dt = new LocalDate();
		return dt.minusYears(years).toDate();
	}

	/**
	 * 几年后
	 * @param years
	 * @return
     */
	public static Date getYearslater(int years) {
		LocalDate dt = new LocalDate();
		return dt.plusYears(years).toDateTimeAtCurrentTime().toDate();
	}

	/**
	 * @function 根据出生日期获取年岁
	 * @author zhangjr
	 * @param birthDate
	 * @date 2015-12-16
	 * @return int
	 */
	public static int getAge(Date birthDate){
		int age = 0;
		Calendar born = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		if (birthDate != null) {
			now.setTime(new Date());
			born.setTime(birthDate);
			if (born.after(now)) {
				return -1;
			}
			age = now.get(Calendar.YEAR) - born.get(Calendar.YEAR);
			if (now.get(Calendar.DAY_OF_YEAR) < born.get(Calendar.DAY_OF_YEAR)) {
				age -= 1;
			}
		}
		return age;
	}
	/**
	 * 获取num天后的日期
	 * zxq
	 * */
	public static Date getDateAftXDays(Date d,int num){
		Calendar now = Calendar.getInstance();  
        now.setTime(d);  
        now.set(Calendar.DATE, now.get(Calendar.DATE) + num);  
        return now.getTime();  
	}

    /**
     * 获取某个时间的小时，24小时制
     * @param date
     * @return
     */
    public static int getHour(Date date){
        Calendar now = Calendar.getInstance();
        now.setTime(date);

        return now.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 计算2个日期相差的天数
     * @param startDate
     * @param endDate
     * @return
     */
    public static int getDaysBetween(Date startDate,Date endDate){
        if(null == startDate || null == endDate){
            return -1;
        }
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
		startCal.set(Calendar.MILLISECOND,0);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
		endCal.set(Calendar.MILLISECOND,0);

        long betweenDays=(endCal.getTimeInMillis()-startCal.getTimeInMillis())/(1000*3600*24);
        return Integer.parseInt(String.valueOf(betweenDays));
    }


	/**
	 * 根据出生日期计算当前年龄
	 * @param birthdayDate
	 * @return
     */
	public static int calculateAge(Date birthdayDate){
		try {

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			int year = calendar.get(Calendar.YEAR);
			calendar.setTime(birthdayDate);
			int birthYear = calendar.get(Calendar.YEAR);
			return year - birthYear;
		}catch (Exception e){
			return 0;
		}
	}

    /**
     * 将字符串格式的时间转成Date对象
     * @param str
     * @param format
     * @return
     */
    public static Date parseDate(String str, String format){
        Date d = null;
        if(StringUtils.isEmpty(str)){
            return d;
        }

        String format1 = DEFAULT_DATE_TIME;
        if(StringUtils.isNotEmpty(format)){
			format1 = format;
        }
        DateFormat df = new SimpleDateFormat(format1);

        try {
            d = df.parse(str);
        } catch (ParseException e) {
            d = null;
        }

        return d;
    }

	public static  String formatDate(Date d)
	{
		String formated = null;
		DateFormat df = new SimpleDateFormat(YYYY_MM_DD);
		try {
			formated = df.format(d);
		} catch (Exception e) {
			formated = null;
		}
		return formated;
	}

	public static  String formatDateTime(Date d)
	{
		String formated = null;
		DateFormat df = new SimpleDateFormat(PRESCRITION_DATE_TIME);
		try {
			formated = df.format(d);
		} catch (Exception e) {
			formated = null;
		}
		return formated;
	}
	public static  String formatDateTimeWithSec(Date d)
	{
		String formated = null;
		DateFormat df = new SimpleDateFormat(DEFAULT_DATETIME_WITHSECOND);
		try {
			formated = df.format(d);
		} catch (Exception e) {
			formated = null;
		}
		return formated;
	}

	public static Date convertFromStringDate(String dateStr)
	{
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(YYYY_MM_DD);
		try {
			date = formatter.parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
		return date;
	}

	/**
	 * 今天的前几天，格式yyyyMMdd
	 * @param past
	 * @return
     */
	public static String getPastDate(int past) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - past);
		Date today = calendar.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String result = format.format(today);
		return result;
	}

	/**
	 * 今天，格式yyyyMMdd
	 * @return
     */
	public static String getToDayDate() {
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String result = format.format(today);
		return result;
	}
}
