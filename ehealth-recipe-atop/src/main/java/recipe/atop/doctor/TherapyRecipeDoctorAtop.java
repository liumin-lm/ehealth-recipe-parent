package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.CancelRecipeReqVO;
import com.ngari.recipe.recipe.model.CancelRecipeResultVO;
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

/**
 * 诊疗处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("therapyRecipeDoctorAtop")
public class TherapyRecipeDoctorAtop extends BaseAtop {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    @RpcService
    public void saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        logger.info("TherapyRecipeDoctorAtop saveTherapyRecipe recipeInfoVO = {}", JSON.toJSONString(recipeInfoVO));
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeDetails(), recipeInfoVO.getRecipeBean(), recipeInfoVO.getRecipeTherapyVO());
        validateAtop(recipeInfoVO.getRecipeBean().getDoctor(), recipeInfoVO.getRecipeBean().getMpiid());
        recipeInfoVO.getRecipeBean().setStatus(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType());
        recipeInfoVO.getRecipeBean().setRecipeSourceType(3);
        recipeInfoVO.getRecipeBean().setSignDate(DateTime.now().toDate());
        recipeInfoVO.getRecipeTherapyVO().setDoctorId(recipeInfoVO.getRecipeBean().getDoctor());
        recipeInfoVO.getRecipeTherapyVO().setMpiId(recipeInfoVO.getRecipeBean().getMpiid());
        try {
            therapyRecipeBusinessService.saveTherapyRecipe(recipeInfoVO);
            logger.info("TherapyRecipeDoctorAtop saveTherapyRecipe  result = {}", JSON.toJSONString("result"));
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop saveTherapyRecipe  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop saveTherapyRecipe  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    /**
     * 撤销处方
     * @param cancelRecipeReqVO 撤销参数
     * @return 撤销结果
     */
    public CancelRecipeResultVO cancelTherapyRecipe(CancelRecipeReqVO cancelRecipeReqVO){
        logger.info("TherapyRecipeDoctorAtop cancelRecipe cancelRecipeReqVO:{}.", JSON.toJSONString(cancelRecipeReqVO));
        validateAtop(cancelRecipeReqVO, cancelRecipeReqVO.getBusId(), cancelRecipeReqVO.getReason());
        try {
            CancelRecipeResultVO result = therapyRecipeBusinessService.cancelRecipe(cancelRecipeReqVO);
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
     * @param therapyId 诊疗处方ID
     */
    public boolean abolishTherapyRecipe(Integer therapyId){
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
