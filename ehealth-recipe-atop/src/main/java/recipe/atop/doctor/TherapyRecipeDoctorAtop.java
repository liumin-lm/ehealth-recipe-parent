package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

/**
 * 诊疗处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("therapyRecipeDoctorAtop")
public class TherapyRecipeDoctorAtop extends BaseAtop {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    /**
     * 保存诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    @RpcService
    public Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        logger.info("TherapyRecipeDoctorAtop saveTherapyRecipe recipeInfoVO = {}", JSON.toJSONString(recipeInfoVO));
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeDetails(), recipeInfoVO.getRecipeBean());
        validateAtop(recipeInfoVO.getRecipeBean().getDoctor(), recipeInfoVO.getRecipeBean().getMpiid());
        recipeInfoVO.getRecipeBean().setStatus(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType());
        recipeInfoVO.getRecipeBean().setRecipeSourceType(3);
        recipeInfoVO.getRecipeBean().setSignDate(DateTime.now().toDate());
        if (null == recipeInfoVO.getRecipeTherapyVO()) {
            recipeInfoVO.setRecipeTherapyVO(new RecipeTherapyVO());
        }
        if (null == recipeInfoVO.getRecipeExtendBean()) {
            recipeInfoVO.setRecipeExtendBean(new RecipeExtendBean());
        }
        try {
            Integer result = therapyRecipeBusinessService.saveTherapyRecipe(recipeInfoVO);
            logger.info("TherapyRecipeDoctorAtop saveTherapyRecipe  result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop saveTherapyRecipe  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop saveTherapyRecipe  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


}
