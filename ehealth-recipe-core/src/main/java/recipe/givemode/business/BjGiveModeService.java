package recipe.givemode.business;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import ctd.persistence.DAOFactory;
import org.springframework.stereotype.Component;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.HisRecipeDAO;

import java.util.List;


/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Component("bjGiveModeService")
public class BjGiveModeService extends AbstractGiveModeService implements IGiveModeBase{

    @Override
    public void setSpecialItem(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        //处理北京互联网特殊按钮的展示
        HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();

        if (new Integer(1).equals(recipe.getGiveMode())) {
            //表示配送到家,需要判断是药企配送还是医院配送
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
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
