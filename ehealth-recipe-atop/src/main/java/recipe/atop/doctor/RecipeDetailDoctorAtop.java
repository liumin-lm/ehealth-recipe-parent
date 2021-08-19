package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRecipeDetailBusinessService;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 处方明细服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeDetailAtop")
public class RecipeDetailDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeDetailBusinessService recipeDetailService;

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 长处方标识 0 不是
     */
    private static final String IS_LONG_RECIPE_FALSE = "0";

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
        validateAtop(organId, recipeType, recipeDetails);
        ValidateDetailVO validateDetailVO = new ValidateDetailVO(organId, recipeType, recipeDetails, new RecipeExtendBean(), true);
        try {
            List<RecipeDetailBean> result = recipeDetailService.continueRecipeValidateDrug(validateDetailVO).getRecipeDetails();
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
     * @param validateDetailVO 药品数据VO
     * @return 处方明细
     */
    @RpcService
    public ValidateDetailVO validateDetailV1(ValidateDetailVO validateDetailVO) {
        logger.info("RecipeDetailAtop validateDetailV1 validateDetailVO ：{}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        try {
            ValidateDetailVO result = recipeDetailService.continueRecipeValidateDrug(validateDetailVO);
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
     * @param validateDetailVO 药品数据VO
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO) {
        logger.info("RecipeDetailAtop useDayValidate validateDetailVO {}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        try {
            List<RecipeDetailBean> result = recipeDetailService.useDayValidate(validateDetailVO);
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
        if (ValidateUtil.integerIsEmpty(organId) || CollectionUtils.isEmpty(recipeDetails)) {
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
