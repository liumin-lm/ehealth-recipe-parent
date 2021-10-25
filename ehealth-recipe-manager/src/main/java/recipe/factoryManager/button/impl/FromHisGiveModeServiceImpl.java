package recipe.factoryManager.button.impl;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.springframework.stereotype.Service;
import recipe.factoryManager.button.GiveModeManager;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Service
public class FromHisGiveModeServiceImpl extends GiveModeManager {
    @Override
    public void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        super.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
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
