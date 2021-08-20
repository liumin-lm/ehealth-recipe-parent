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
import recipe.core.api.patient.IOfflineRecipeBusinessService;
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
    @Autowired
    private IOfflineRecipeBusinessService offlineToOnlineService;

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


    @RpcService
    public Integer submitTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        Integer recipeId = saveTherapyRecipe(recipeInfoVO);
        //异步推送his
        offlineToOnlineService.pushTherapyRecipeExecute(recipeId, 1);
        return recipeId;
    }


    /**
     * 撤销处方
     *
     * @param recipeTherapyVO 撤销参数
     * @return 撤销结果
     */
    public boolean cancelTherapyRecipe(RecipeTherapyVO recipeTherapyVO) {
        logger.info("TherapyRecipeDoctorAtop cancelRecipe cancelRecipeReqVO:{}.", JSON.toJSONString(recipeTherapyVO));
        validateAtop(recipeTherapyVO, recipeTherapyVO.getTherapyCancellationType(), recipeTherapyVO.getRecipeId(), recipeTherapyVO.getTherapyCancellation());
        try {
            boolean result = therapyRecipeBusinessService.cancelRecipe(recipeTherapyVO);
            logger.info("TherapyRecipeDoctorAtop cancelRecipe  result = {}", JSON.toJSONString("result"));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop cancelRecipe  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop cancelRecipe  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 作废诊疗处方
     *
     * @param therapyId 诊疗处方ID
     */
    public boolean abolishTherapyRecipe(Integer therapyId) {
        logger.info("TherapyRecipeDoctorAtop abolishTherapyRecipe therapyId:{}.", therapyId);
        validateAtop(therapyId);
        try {
            return therapyRecipeBusinessService.abolishTherapyRecipe(therapyId);
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop abolishTherapyRecipe  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop abolishTherapyRecipe  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
