package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.service.RecipeDetailService;

import java.util.List;

/**
 * 处方明细服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeDetailAtop")
public class RecipeDetailAtop extends BaseAtop {

    @Autowired
    private RecipeDetailService recipeDetailService;

    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> validateDetail(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        logger.info("RecipeDetailAtop validateDetail recipeDetails = {}，organId= {}，recipeType= {}", JSON.toJSONString(recipeDetails), organId, recipeType);
        if (null == organId || null == recipeType || CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.continueRecipeValidateDrug(organId, recipeType, recipeDetails);
            logger.info("RecipeDetailAtop validateDetail result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop validateDetail error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop validateDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
