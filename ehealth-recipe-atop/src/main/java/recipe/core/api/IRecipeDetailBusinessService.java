package recipe.core.api;

import com.ngari.recipe.recipe.model.RecipeDetailBean;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 处方明细接口
 *
 * @author fuzi
 */
public interface IRecipeDetailBusinessService {
    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param validateDetailVO 机构id
     * @return
     */
    ValidateDetailVO continueRecipeValidateDrug(ValidateDetailVO validateDetailVO);

    /**
     * 校验处方药品配置时间
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO);

    /**
     * 校验中药嘱托
     *
     * @param organId       机构id
     * @param recipeDetails 处方药品明细
     * @return 处方药品明细
     */
    List<RecipeDetailBean> entrustValidate(Integer organId, List<RecipeDetailBean> recipeDetails);

    /**
     * 患者端处方进行中列表查询药品信息
     *
     * @param orderCode 订单code
     * @return
     */
    String getDrugName(String orderCode);
}
