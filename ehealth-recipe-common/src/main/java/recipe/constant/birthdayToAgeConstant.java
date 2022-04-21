package recipe.constant;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

public class birthdayToAgeConstant {
    public static final Logger logger = LoggerFactory.getLogger(birthdayToAgeConstant.class);


    public static String ageFormat(Date fromDate) {
        logger.info("birthdayToAgeConstant ageFormat fromDate ={} ",JSON.toJSONString(fromDate));
        try {
            Calendar cal = Calendar.getInstance();
            int yearNow = cal.get(Calendar.YEAR);
            int monthNow = cal.get(Calendar.MONTH) + 1;
            int dayNow = cal.get(Calendar.DAY_OF_MONTH);
            cal.add(Calendar.MONTH, -1);
            int maxdaysOfMonthNowLastMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.setTime(fromDate);

            int yearFrom = cal.get(Calendar.YEAR);
            int monthFrom = cal.get(Calendar.MONTH) + 1;
            int dayFrom = cal.get(Calendar.DAY_OF_MONTH);
            int age = Integer.parseInt(getAge(fromDate));

            if (age >= 14) {

                return age + "岁";
            } else {

                int resultDay = 0;
                int resultMonth = 0;
                int resultYear = 0;

                if ((dayNow - dayFrom) < 0) {
                    monthNow--;
                    dayNow += maxdaysOfMonthNowLastMonth;
                }
                resultDay = dayNow - dayFrom;

                if ((monthNow - monthFrom) < 0) {
                    yearNow--;
                    monthNow += 12;

                }
                resultMonth = monthNow - monthFrom;

                resultYear = yearNow - yearFrom;

                String result = "";
                if (age >= 2) {
                    result = resultYear + "岁" + (resultMonth != 0 ? (resultMonth + "个月") : "");
                } else {
                    String monthPre = "";
                    int monthInt = resultYear * 12 + resultMonth;
                    if (monthInt != 0) {
                        monthPre = monthInt + "个月";
                    }
                    result = monthPre + (resultDay != 0 ? (resultDay + "天") : "");
                }
                return result;
            }
        } catch (Exception e) {
            logger.error("birthdayToAgeConstant ageFormat error fromDate ={} ",JSON.toJSONString(fromDate) ,e);
            return "";
        }
    }

    /**
     * 获取年龄（虚岁or实岁）
     *
     * @param birthDay 生日
     * @return
     * @throws Exception
     */
    private static String getAge(Date birthDay) {
        if (birthDay == null) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        int yearNow = cal.get(Calendar.YEAR);
        int dayOfYearNow = cal.get(Calendar.DAY_OF_YEAR);
        cal.setTime(birthDay);
        int yearBirth = cal.get(Calendar.YEAR);
        int dayofYearBirth = cal.get(Calendar.DAY_OF_YEAR);
        int age = yearNow - yearBirth;
        if (dayOfYearNow < dayofYearBirth - 1) {
            age--;
        }
        if (age < 0) {
            age = 0;
        }
        return age + "";
    }
}
