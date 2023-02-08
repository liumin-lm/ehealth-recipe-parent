package test.eh.prepay;

import com.ngari.recipe.recipe.model.DispendingPharmacyReportReqTo;
import ctd.util.JSONUtils;
import org.junit.Test;

import java.util.Date;


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
