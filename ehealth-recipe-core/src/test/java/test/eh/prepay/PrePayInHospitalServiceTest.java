package test.eh.prepay;

import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/*@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")*/
public class PrePayInHospitalServiceTest {

    /*@Autowired
    RecipeDAO recipeDAO;*/

    @Test
    public void test() {
        /*String start = DateConversion.formatDateTimeWithSec(new Date());
        String end = DateConversion.formatDateTimeWithSec(new Date());
        start = "2020-01-01 00:00:00";
        end = "2020-10-31 23:59:59";
        List<RecipeDrugDetialReportDTO> recipeDrugDetialReport = recipeDAO.findRecipeDrugDetialReport(1, start, end,null, null, null, null, null, "13,14,15", null, null, null, null, 0, 10);
        */
        DispendingPharmacyReportReqTo dispendingPharmacyReportReqTo = new DispendingPharmacyReportReqTo();
        dispendingPharmacyReportReqTo.setOrganId(1);
        dispendingPharmacyReportReqTo.setStartDate(new Date());
        dispendingPharmacyReportReqTo.setEndDate(new Date());
        dispendingPharmacyReportReqTo.setStart(0);
        dispendingPharmacyReportReqTo.setLimit(10);
        dispendingPharmacyReportReqTo.setOrderStatus(1);
        System.out.println(JSONUtils.toString(dispendingPharmacyReportReqTo));
    }

    public static String ageFormat(Date fromDate, String append) {
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

                return age + append;
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
            return "";
        }
    }

    public static void main(String[] args) {
        System.out.println(ageFormat("2021-09-15", "岁"));
    }

    public static String ageFormat(String birthday, String append) {
        if (StringUtils.isEmpty(append)) {
            append = "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        Date date = null;
        try {
            date = sdf.parse(birthday);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (date == null) {
            return "";
        }
        return ageFormat(date, append);
    }

    /**
     * 获取年龄（虚岁or实岁）
     *
     * @param birthDay 生日
     * @return
     * @throws Exception
     */
    public static String getAge(Date birthDay) {
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
