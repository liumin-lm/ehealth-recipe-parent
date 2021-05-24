package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
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
     * todo 过期方法新调用 使用： validateDetailV1
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    @RpcService
    @Deprecated
    public List<RecipeDetailBean> validateDetail(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        logger.info("RecipeDetailAtop validateDetail recipeDetails = {}，organId= {}，recipeType= {}", JSON.toJSONString(recipeDetails), organId, recipeType);
        if (null == organId || null == recipeType || CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.continueRecipeValidateDrug(organId, recipeType, recipeDetails, new RecipeExtendBean());
            logger.info("RecipeDetailAtop validateDetail result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop validateDetail error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop validateDetail error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return 处方明细
     */
    @RpcService
    public List<RecipeDetailBean> validateDetailV1(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails, RecipeExtendBean recipeExtendBean) {
        logger.info("RecipeDetailAtop validateDetailV1 recipeDetails = {}，organId= {}，recipeType= {}，recipeExtendBean= {}"
                , JSON.toJSONString(recipeDetails), organId, recipeType, JSON.toJSONString(recipeExtendBean));

        if (null == recipeExtendBean || null == organId || null == recipeType || CollectionUtils.isEmpty(recipeDetails)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.continueRecipeValidateDrug(organId, recipeType, recipeDetails, recipeExtendBean);
            logger.info("RecipeDetailAtop validateDetailV1 result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop validateDetailV1 error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop validateDetailV1 error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 校验处方药品配置时间
     *
     * @param organId       机构id
     * @param recipeType    处方类型
     * @param recipeDetails 处方药品明细
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> useDayValidate(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        logger.info("RecipeDetailAtop useDayValidate recipeDetails = {}，organId= {}，recipeType= {}", JSON.toJSONString(recipeDetails), organId, recipeType);
        if (null == organId || null == recipeType || CollectionUtils.isEmpty(recipeDetails)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.useDayValidate(organId, recipeType, recipeDetails);
            logger.info("RecipeDetailAtop useDayValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop useDayValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop useDayValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 校验中药嘱托
     *
     * @param organId       机构id
     * @param recipeDetails 处方药品明细
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> entrustValidate(Integer organId, List<RecipeDetailBean> recipeDetails) {
        logger.info("RecipeDetailAtop entrustValidate recipeDetails = {}，organId= {}", JSON.toJSONString(recipeDetails), organId);
        if (null == organId || CollectionUtils.isEmpty(recipeDetails)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.entrustValidate(organId, recipeDetails);
            logger.info("RecipeDetailAtop entrustValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop entrustValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop entrustValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
