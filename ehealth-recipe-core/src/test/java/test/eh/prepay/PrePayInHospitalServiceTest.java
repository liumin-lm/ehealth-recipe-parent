package test.eh.prepay;

import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import com.ngari.recipe.recipe.model.PharmacyMonthlyReportDTO;
import com.ngari.recipe.recipe.model.PharmacyTopDTO;
import com.ngari.recipe.recipe.model.RecipeDrugDetialReportDTO;
import ctd.util.JSONUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import recipe.dao.RecipeDAO;
import recipe.util.DateConversion;

import java.util.Date;
import java.util.List;
import java.util.Map;


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

}
