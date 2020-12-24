package test.eh;

import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.mode.PayNotifyResTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.presettle.settle.MedicalSettleService;
import recipe.retry.RecipeRetryService;

import javax.annotation.Resource;

/**
 * created by shiyuping on 2020/12/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class RetryTest {

    @Resource
    RecipeRetryService recipeSettleRetryService;

    @Test
    public void testRetry(){
        PayNotifyResTO payNotifyResTO = recipeSettleRetryService.doRecipeSettle(new MedicalSettleService(), new PayNotifyReqTO());
        System.out.println(JSONUtils.toString(payNotifyResTO));
    }
}
