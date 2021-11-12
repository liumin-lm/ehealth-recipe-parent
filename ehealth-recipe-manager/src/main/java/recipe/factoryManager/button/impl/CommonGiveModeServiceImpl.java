package recipe.factoryManager.button.impl;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.PatientClient;
import recipe.factoryManager.button.GiveModeManager;

import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Service
public class CommonGiveModeServiceImpl extends GiveModeManager {
    @Autowired
    private PatientClient patientClient;

    @Override
    public void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        //设置临沭医保例外支付的个性化按钮设置
        if (recipe.getClinicOrgan() == 1002753) {
            Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
            boolean supportMedicalPayment = result.containsKey("supportMedicalPayment");
            if (supportMedicalPayment && !patientClient.isMedicarePatient(recipe.getClinicOrgan(), recipe.getMpiid())) {
                super.removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "supportMedicalPayment");
            }
        }
    }
}
