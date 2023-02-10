package recipe.core.api;

import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.DrugForGiveModeListVO;
import recipe.vo.doctor.DrugQueryVO;

import java.util.List;
import java.util.Map;

/**
 * 库存处理实现类
 *
 * @author fuzi
 */
public interface IStockBusinessService {
    /**
     * 医生端查询 药品列表-查库存
     *
     * @param drugQueryVO   入参字段
     * @param recipeDetails 处方明细
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return
     */
    List<DrugEnterpriseStockVO> stockList(DrugQueryVO drugQueryVO, List<Recipedetail> recipeDetails, Integer stockCheckType);

    /**
     * 医生指定药企列表-查库存
     *
     * @param recipeDTO 处方数据
     * @return
     */
    List<EnterpriseStock> stockList(RecipeDTO recipeDTO);

    /**
     * 校验 药品库存 在同一个药企下的库存数量
     * todo 兼容老代码 所以出参数用map 非特殊情况 不可使用
     *
     * @param recipeId 处方id
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return 是否可以开方
     */
    @Deprecated
    Map<String, Object> enterpriseStockMap(Integer recipeId, Integer stockCheckType);

    /**
     * 校验 某个药企下 药品库存 的库存数量
     *
     * @param recipe        机构id
     * @param recipeDetails 药品信息 drugId，code
     * @param enterpriseId  指定某药企id
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return 药品信息 一定存在于出参
     */
    EnterpriseStock enterpriseStockCheck(Recipe recipe, List<Recipedetail> recipeDetails, Integer enterpriseId, Integer stockCheckType);

    /**
     * 校验 某个药企下 药品库存 的库存数量
     *
     * @param recipeDTO 药品信息 drugId，code
     * @param id        指定某药企id
     * @param type      0默认，1查询医院，2查询药企
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return 药品信息 一定存在于出参
     */
    EnterpriseStock enterpriseStockCheckV1(RecipeDTO recipeDTO, Integer id, Integer type, Integer stockCheckType);

    /**
     * 医生端 查询购药方式下有库存的药品
     *
     * @param recipeDTO 药品信息
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return
     */
    List<DrugForGiveModeListVO> drugForGiveModeV1(RecipeDTO recipeDTO, Integer stockCheckType);

    /**
     * 查询药品能否开在一张处方上
     *
     * @param recipeDTO
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验
     * @return
     */
    List<EnterpriseStock> drugRecipeStock(RecipeDTO recipeDTO, Integer stockCheckType);


    /**
     * 查询药品能支持的够药方式
     *
     * @param recipe
     * @return
     */
    DoSignRecipeDTO validateRecipeGiveMode(RecipeDTO recipe);

    /**
     * 获取药品库存
     *
     * @param recipeIds
     * @param enterpriseId
     * @return
     */
    Boolean getOrderStockFlag(List<Integer> recipeIds, Integer enterpriseId, String giveModeKey);

    /**
     * 查询药品是否用库存
     *
     * @param recipeDTO
     * @return
     */
    List<EnterpriseStock> drugsStock(RecipeDTO recipeDTO, Integer stockCheckType);

    /**
     * 算法拆方，拆分可下单处方
     * 穷举最优处方==最小拆分组数
     *
     * @param recipeDTO
     * @return
     */
    List<List<RecipeDetailBean>> retailsSplitList(RecipeDTO recipeDTO);
}
