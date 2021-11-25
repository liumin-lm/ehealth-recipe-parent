package recipe.core.api.patient;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import recipe.vo.doctor.DrugEnterpriseStockVO;

import java.util.List;
import java.util.Map;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
public interface IDrugEnterpriseBusinessService {
    /**
     * 医生端查询药品列表-查库存
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    List<DrugEnterpriseStockVO> stockList(Integer organId, List<Recipedetail> recipeDetails);

    /**
     * 医生指定药企列表-查库存
     *
     * @param recipe        处方数据
     * @param recipeDetails 处方明细
     * @return
     */
    List<EnterpriseStock> stockList(Recipe recipe, List<Recipedetail> recipeDetails);

    /**
     * 校验 药品库存 在同一个药企下的库存数量
     * todo 兼容老代码 所以出参数用map 非特殊情况 不可使用
     *
     * @param recipeId 处方id
     * @return 是否可以开方
     */
    Map<String, Object> enterpriseStock(Integer recipeId);

    /**
     * 校验 药品库存 的库存数量
     *
     * @param organId       机构id
     * @param recipeDetails 药品信息 drugId，code
     * @param enterpriseId  指定某药企id
     * @return 药品信息 一定存在于出参
     */
    List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails, Integer enterpriseId);
}
