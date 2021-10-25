package recipe.factoryManager.button.impl;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.HisRecipeDAO;
import recipe.factoryManager.button.GiveModeManager;
import recipe.factoryManager.button.IGiveModeBase;

import java.util.List;


/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Service
public class BjGiveModeService extends GiveModeManager implements IGiveModeBase {
    @Autowired
    protected HisRecipeDAO hisRecipeDAO;
    @Autowired
    protected DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Override
    public void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        //处理北京互联网特殊按钮的展示
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();

        if (new Integer(1).equals(recipe.getGiveMode())) {
            //表示配送到家,需要判断是药企配送还是医院配送
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAccount(hisRecipe.getDeliveryCode());
            if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getSendType())) {
                //表示为医院配送
                saveGiveModeData(giveModeButtonBeans, "showSendToHos");
            } else {
                //表示为药企配送
                saveGiveModeData(giveModeButtonBeans, "showSendToEnterprises");
            }
        } else if (new Integer(2).equals(recipe.getGiveMode())) {
            //表示到院取药
            saveGiveModeData(giveModeButtonBeans, "supportToHos");
        } else if (new Integer(3).equals(recipe.getGiveMode())) {
            //表示到店取药
            saveGiveModeData(giveModeButtonBeans, "supportTFDS");
        }
    }
}
