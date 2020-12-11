package recipe.givemode.business;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.purchase.PurchaseService;

import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Component("commonGiveModeService")
public class commonGiveModeService extends AbstractGiveModeService implements IGiveModeBase{

    @Override
    public void setSpecialItem(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        //设置临沭医保例外支付的个性化按钮设置
        if (recipe.getClinicOrgan() == 1002753){
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
            boolean supportMedicalPayment = result.containsKey("supportMedicalPayment");
            if (supportMedicalPayment && !purchaseService.isMedicarePatient(recipe.getClinicOrgan(), recipe.getMpiid())){
                super.removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "supportMedicalPayment");
            }
        }
    }
}
