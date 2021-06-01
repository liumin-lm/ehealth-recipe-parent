package recipe.util;

import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    public static final String DEFAULT_DATETIME_BUSS = "MM-dd HH:mm";

    public static final String DEFAULT_DATE_WITHOUTYEAR = "MM-dd";

    public static final String DEFAULT_TIME_WITHOUTSECOND = "HH:mm";

	public static final String DEFAULT_DATETIME_WITHSECOND = "yyyy-MM-dd HH:mm:ss";

	public static final String DEFAULT_TIME = "HH:mm:ss";

	/**
	 * 当月第一天
	 * 
	 * @return
	 */
	public static Date firstDayOfThisMonth() {
		LocalDate dt = new LocalDate();
		return dt.minusDays(dt.getDayOfMonth() - 1).toDate();
	}

	/**
	 * 获取指定日期所在月月初
	 *
	 * @return
	 */
	public static Date firstDayOfMonthForDate(Date date) {
		LocalDate dt = new LocalDate(date);
		return dt.dayOfMonth().withMinimumValue().toDate();
	}

	/**
	 * 获取指定日期所在月月末
	 *
	 * @return
	 */
	public static Date lastDayOfMonthForDate(Date date) {
		LocalDate dt = new LocalDate(date);
		return dt.dayOfMonth().withMaximumValue().toDate();
	}


	/**
	 * 下周
	 * 
	 * @return
	 */
	public static Date getDateOfNextWeek() {
		LocalDate dt = new LocalDate();
		return dt.plusWeeks(1).toDate();
	}

	/**
	 * 入参时间的当前一周
	 * 
	 * @author luf
	 * @return
	 */
	public static Date getDateOfWeekNow(Date startTime) {
		Calendar c = Calendar.getInstance();
		c.setTime(startTime);
		c.add(Calendar.DAY_OF_WEEK, 7);
		return c.getTime();
	}

	/**
	 * 几天前
	 * 
	 * @return
	 */
	public static Date getDaysAgo(int days) {
		LocalDate dt = new LocalDate();
		return dt.minusDays(days).toDate();
	}

	/**
	 * 几天前(时分秒)
	 *
	 * @return
	 */
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

	/**
	 * 根据时间点 返回日期
	 * 
	 * @param currentDate
	 * @param timePoint
	 *            eg: 8:10
	 * @return
	 */
	public static Date getDateByTimePoint(Date currentDate, String timePoint) {
		DateFormat df = new SimpleDateFormat(YYYY_MM_DD);
		String date = df.format(currentDate);
		String newDate = date + " " + timePoint;
		return getCurrentDate(newDate, "yyyy-MM-dd HH:mm");

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
	 * 均分时间段
	 * 
	 * @author luf
	 * @param start
	 *            开始时间
	 * @param end
	 *            结束时间
	 * @param avg
	 *            分段数
	 * @return List<Object[]> 均分出的时间段
	 */
	public static List<Object[]> getAverageTime(Date start, Date end, int avg) {
		long diff = end.getTime() - start.getTime();
		long avgM = diff / avg;
		List<Object[]> os = new ArrayList<Object[]>();
		for (int i = 0; i < avg; i++) {
			Date s = new Date(start.getTime() + i * avgM);
			Date e = new Date(end.getTime() - (avg - i - 1) * avgM);
			Date[] d = new Date[2];
			d[0] = s;
			d[1] = e;
			os.add(d);
		}
		return os;
	}

	/**
	 * 获取当前日期是星期几
	 * 
	 * @param dt
	 * @return 当前日期是星期几
	 */
	public static String getWeekOfDate(Date dt) {
		String[] weekDays = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" };
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w < 0){
			w = 0;
		}
		return weekDays[w];
	}


	/**
	 * Date类型转Timestamp
	 * 
	 * @author luf
	 * @param date
	 * @return
	 */
	public static Timestamp convertFromDateToTsp(Date date) {
		String dateString = getDateFormatter(date, "yyyy-MM-dd HH:mm:ss");
		return Timestamp.valueOf(dateString);
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
	 * 根据间隔计算时间段
	 * 
	 * @author luf
	 * @param startTime
	 *            开始时间
	 * @param endTime
	 *            结束时间
	 * @param intervalTime
	 *            间隔时间
	 * @return List<Object[]>
	 */
	public static List<Object[]> getIntervalTimeList(Date startTime,
			Date endTime, Integer intervalTime) {
		List<Object[]> list = new ArrayList<Object[]>();
		if (intervalTime == null || intervalTime <= 0) {
			Date[] d = new Date[2];
			d[0] = startTime;
			d[1] = endTime;
			list.add(d);
			return list;
		}
		long start = startTime.getTime();
		long end = endTime.getTime();
		long interval = intervalTime.longValue() * 60 * 1000;
		long temp = start;
		while (temp < end) {
			Date[] d= new Date[2];
			// startTime
			d[0] = new Date(temp);
			temp = temp + interval;
			if (temp <= end) {
				d[1] = new Date(temp);
			} else {
				d[1] = endTime;
			}
			list.add(d);
		}
		return list;
	}

	/**
	 * 根据日期获取周几
	 * 
	 * @author luf
	 * @param dt
	 *            日期
	 * @return int --周几
	 */
	public static int getWeekOfDateInt(Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w <= 0) {
			w = 7;
		}
		return w;
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
				/*throw new IllegalArgumentException(
						"Can't be born in the future");*/
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
	 * 
	 * @function 根据起始时间 与结束时间及基础时间，计算剩余小时数
	 * @author zhangjr
	 * @date 2016-2-25
	 * @return int
	 */
	public static int getHoursDiffer(Date startDate,Date endDate,int baseNumber){
		if(endDate.before(startDate)){
			return -1;
		}
		long startTime = startDate.getTime();
		long endTime = endDate.getTime();
		long diff = endTime - startTime;
		int hours = (int) diff/(1000*60*60);
		return baseNumber - hours;
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
	 * 获取num天后的日期
	 * @author luf
	 * */
	public static Date getDateAftXMinutes(Date d,int num){
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.set(Calendar.MINUTE, now.get(Calendar.MINUTE) + num);
		return now.getTime();
	}

	/**
	 * 获取当前时间一小时后的日期
	 * @author luf
	 * */
	public static Date getDateAftHour(Date d,int num){
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.add(Calendar.HOUR_OF_DAY, num);
		return now.getTime();
	}

	/**
	 * 获取当前时间几分钟后的时间
	 * @param d
	 * @param num
     * @return
     */
	public static Date getDateAftMinute(Date d, int num){
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.add(Calendar.MINUTE, num);
		return now.getTime();
	}

	/**
	 * 出生日期获取年岁
	 * @param birthDate
	 * @author houxr
	 * @date 2016-06-17 11:01:20
     * @return
     */
	public static int getAgeFromBirthday(String birthDate){
		LocalDate birthDay = DateTimeFormat.forPattern("yyyy-MM-dd").parseLocalDate(birthDate);
		LocalDate now = new LocalDate();
		int age = Years.yearsBetween(birthDay, now).getYears();
		int age0 = 0;
		int age120 = 120;
		if (age < age0 || age > age120) {
			return -1;
		}
		return  age;
	}

	/**
	 * 根据年龄获取出生日期
	 * @param age
	 * @return
     */
	public static Date getBirthdayFromAge(int age) {
		return LocalDate.now().minusYears(age).toDate();
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

	/**
	 * 获取某时间之前几个小时的时间
	 * @param d
	 * @param num
     * @return
     */
	public static Date getDateBFtHour(Date d,int num){
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.add(Calendar.HOUR_OF_DAY, -num);
		return now.getTime();
	}

	/**
	 * 计算时间点
	 * @param startTime
	 * @param endTime
	 * @param intervalTime
     * @return
     */
	public static List<Object[]> getIntervalStartList(Date startTime,
													 Date endTime, long intervalTime) {
		List<Object[]> list = new ArrayList<Object[]>();
		if (intervalTime <= 0) {
			Date[] d = new Date[2];
			d[0] = startTime;
			d[1] = endTime;
			list.add(d);
			return list;
		}
		long start = startTime.getTime();
		long end = endTime.getTime();
		long interval = intervalTime * 60 * 1000;
		long temp = start;
		while (temp+interval <= end) {
			Date[] d = new Date[2];
			temp = temp + interval;
			d[0] = new Date(temp);
			list.add(d);
		}
		return list;
	}

	public static boolean isSameDay(Date d1,Date d2){
		if(d1==null||d2==null){
			return false;
		}
		Date day1 = getFormatDate(d1, "yyyy-MM-dd");
		Date day2 = getFormatDate(d2, "yyyy-MM-dd");
		return day1.compareTo(day2)==0;
	}


	/**
	 * 时间显示规则：
	 * 1、1小时内：按分钟显示  如5分钟前
	 * 2、1-24小时：按小时显示   如：12：31
	 * 3、日期为昨天且距离现在超过24小时：昨天+时间   昨天  12：31
	 * 4、日期为昨天以前：日期+时间  5月20日 12：31
	 * 5、上一年的：年+月+日+时间 2016年5月20日 12：31
	 * @param date
	 * @return
	 */
	public static String handleTimeText(Date date){
		String text = "";
		Date currentDate = new Date();
		long minusMillisSeconds = currentDate.getTime()-date.getTime();
		long oneSecond = 1000;
		long oneMinute = 60 * oneSecond;
		long oneHour = 60 * oneMinute;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		int cD = calendar.get(Calendar.DATE);
		int cM = calendar.get(Calendar.MONTH);
		int cY = calendar.get(Calendar.YEAR);
		calendar.setTime(date);
		int mD = calendar.get(Calendar.DATE);
		int mM = calendar.get(Calendar.MONTH);
		int mY = calendar.get(Calendar.YEAR);

		SimpleDateFormat sdf = new SimpleDateFormat();
		if(cY==mY){
			// 今年内
			if(cM==mM){
				// 一个月内
				if(cD==mD){
					// 今天内
					if(minusMillisSeconds<oneHour){
						// 1小时内 格式： x分钟前
						long x = (long)minusMillisSeconds/oneMinute;
						if(x==0){
							x=1;
						}
						text = x + "分钟前";
					}else{
						// 一小时前  格式：HH:mm
						sdf.applyPattern("HH:mm");
						text = sdf.format(date);
					}
				}else if((cD-mD)==1){
					// 昨天 格式：昨天 HH:mm
					sdf.applyPattern("昨天 HH:mm");
					text = sdf.format(date);
				}else{
					// 昨天以前 格式：x月x日 HH:mm
					sdf.applyPattern("MM月dd日 HH:mm");
					text = sdf.format(date);
				}
			}else{
				// 昨天以前 格式：x月x日 HH:mm
				sdf.applyPattern("MM月dd日 HH:mm");
				text = sdf.format(date);
			}
		}else{
			// 今年以前   格式： x年x月x日 HH:mm
			sdf.applyPattern("yyyy年MM月dd日 HH:mm");
			text = sdf.format(date);
		}
		return text;
	}

	public static int daysBetween(Date smdate,Date bdate) throws ParseException {
		SimpleDateFormat sdf=new SimpleDateFormat(DEFAULT_DATE_TIME);
		smdate=sdf.parse(sdf.format(smdate));
		bdate=sdf.parse(sdf.format(bdate));
		Calendar cal = Calendar.getInstance();
		cal.setTime(smdate);
		long time1 = cal.getTimeInMillis();
		cal.setTime(bdate);
		long time2 = cal.getTimeInMillis();
		long betweenDays=(time2-time1)/(1000*3600*24);
		return Integer.parseInt(String.valueOf(betweenDays));
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

	/**
	 * 计算2个日期相差的月数
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public static int getMonthsBetween(Date startDate,Date endDate) {
		if(null == startDate || null == endDate){
			throw new DAOException(DAOException.VALUE_NEEDED, "startDate and endDate are needed");
		}
		Calendar c1= Calendar.getInstance();
		Calendar c2=Calendar.getInstance();

		c1.setTime(startDate);
		c2.setTime(endDate);
		int year =c2.get(Calendar.YEAR)-c1.get(Calendar.YEAR);
		if (c1.get(Calendar.DATE) <= c2.get(Calendar.DATE)) {
			return year * 12 + c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
		}
		return year * 12 + c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH) - 1;
	}

	/**
	 * 获取一个月的第一天 和最后一天
	 * @param Date
	 * @return
     */
	public static List<Date> getStartAndEndDateByMonth(Date date){
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.DAY_OF_MONTH, 1);
		Date startDate = c.getTime();
		c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
		Date endDate = c.getTime();
		List<Date> list = new ArrayList<>();
		list.add(startDate);
		list.add(endDate);
		return list;
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
