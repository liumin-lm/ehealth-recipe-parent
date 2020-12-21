package test.eh.prepay;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import recipe.audit.auditmode.AuditPreMode;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class PrePayInHospitalServiceTest {

    @Autowired
    RecipeDetailDAO recipeDetailDAO;

    @Autowired
    RecipeDAO recipeDAO;

    @Test
    public void test() {
        String start = DateConversion.formatDateTimeWithSec(new Date());
        String end = DateConversion.formatDateTimeWithSec(new Date());
        start = "2020-10-01 00:00:00";
        end = "2020-10-31 23:59:59";
        List<Map<String, Object>> recipeByOrderCodegroupByDis = recipeDAO.findRecipeDrugDetialByRecipeId(220160);
        System.out.println(recipeByOrderCodegroupByDis);
    }

}
