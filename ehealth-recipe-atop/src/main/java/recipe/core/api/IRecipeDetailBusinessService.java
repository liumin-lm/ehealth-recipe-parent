package recipe.core.api;

import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.RecipeSkipVO;
import recipe.vo.ResultBean;
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
     * 检验处方药品超量
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    List<RecipeDetailBean> drugSuperScalarValidate(ValidateDetailVO validateDetailVO);

    /**
     * 患者端处方进行中列表查询药品信息
     *
     * @param orderCode 订单code
     * @return
     */
    String getDrugName(String orderCode, Integer orderId);

    /**
     * 校验复诊下重复处方/药品
     *
     * @param validateDetailVO 当前处方药品
     * @return
     */
    ResultBean<String> validateRepeatRecipe(ValidateDetailVO validateDetailVO);

    /**
     * 跳转到第三方
     *
     * @param organId    机构ID
     * @param recipeCode his处方号
     * @return
     */
    RecipeSkipVO getRecipeSkipUrl(Integer organId, String recipeCode);

    /**
     * 校验his 药品规则，靶向药，大病医保等
     *
     * @param recipe          处方信息
     * @param recipeDetailDTO 药品信息
     * @return
     */
    List<RecipeDetailDTO> validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetailDTO, String registerId, String dbType);

    List<RecipeDetailBean> findRecipeDetailsByRecipeId(Integer recipeId);
}
