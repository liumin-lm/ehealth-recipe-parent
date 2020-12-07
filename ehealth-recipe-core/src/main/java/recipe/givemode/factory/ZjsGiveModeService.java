package recipe.givemode.factory;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import ctd.persistence.DAOFactory;
import org.springframework.stereotype.Component;
import recipe.dao.RecipeExtendDAO;
import java.util.Iterator;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Component("zjsGiveModeService")
public class ZjsGiveModeService extends AbstractGiveModeService implements IGiveModeBase{
    @Override
    public void setSpecialItem(PatientTabStatusRecipeDTO record, GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        super.setSpecialItem(record, giveModeShowButtonVO, recipe);
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        //浙江省的购药方式按钮显示需要从HIS获取，目前正式环境一条数据都没有
        RecipeExtendDAO RecipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = RecipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null != recipeExtend && null != recipeExtend.getGiveModeFormHis()) {
            if ("1".equals(recipeExtend.getGiveModeFormHis())) {
                //只支持配送到家
                saveGiveModeData(giveModeButtonBeans, "supportOnline");
            } else if ("2".equals(recipeExtend.getGiveModeFormHis())){
                //只支持到院取药
                saveGiveModeData(giveModeButtonBeans, "supportToHos");
            } else if ("3".equals(recipeExtend.getGiveModeFormHis())) {
                //都支持
            } else {
                //都不支持
                saveGiveModeData(giveModeButtonBeans, "");
            }
        } else {
            //省平台互联网购药方式的配置
            if (1 == recipe.getDistributionFlag()) {
                removeGiveModeData(giveModeButtonBeans, "supportOnline");
            }
        }
    }
}
