package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;

import java.util.List;

/**
 * created by shiyuping on 2020/11/10
 * 配送到家获取展示药企列表接口
 */
public interface PayModeOnlineShowDepInterface {


    /**
     * 获取药企展示
     *
     * @param dep 机构支持的药企
     * @return
     */
    void getPayModeOnlineShowDep(DrugsEnterprise dep, List<DepDetailBean> depDetailList, Recipe recipe, List<Integer> recipeIdList);
}
