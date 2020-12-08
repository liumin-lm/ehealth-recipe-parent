package recipe.givemode.business;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Component("fromHisGiveModeService")
public class FromHisGiveModeService extends AbstractGiveModeService implements IGiveModeBase {
    @Override
    public void setSpecialItem(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        //浙江省的购药方式按钮显示需要从HIS获取，目前正式环境一条数据都没有
        if ("1".equals(recipeExtend.getGiveModeFormHis())) {
            //只支持配送到家
            saveGiveModeData(giveModeButtonBeans, "showSendToHos");
            saveGiveModeData(giveModeButtonBeans, "showSendToEnterprises");
        } else if ("2".equals(recipeExtend.getGiveModeFormHis())) {
            //只支持到院取药
            saveGiveModeData(giveModeButtonBeans, "supportToHos");
        } else if ("3".equals(recipeExtend.getGiveModeFormHis())) {
            //都支持
        } else {
            //都不支持
            saveGiveModeData(giveModeButtonBeans, "");
        }
    }
}
