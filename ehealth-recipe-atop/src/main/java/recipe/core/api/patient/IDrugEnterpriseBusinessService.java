package recipe.core.api.patient;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;

import java.util.List;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
public interface IDrugEnterpriseBusinessService {
    /**
     * 校验药企库存
     *
     * @param recipe     处方信息
     * @param detailList 处方明细信息
     * @return 返回药企库存信息
     */
    List<EnterpriseStock> enterpriseStockCheck(Recipe recipe, List<Recipedetail> detailList);

    /**
     * 校验药企库存 ：不判断 药品在同一个 药企下
     *
     * @param organId       处方信息
     * @param recipeDetails 处方明细信息
     * @return 返回药企库存信息
     */
    List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails);
}
