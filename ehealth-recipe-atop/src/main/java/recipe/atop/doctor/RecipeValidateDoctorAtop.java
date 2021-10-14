package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRecipeDetailBusinessService;
import recipe.core.api.IRevisitBusinessService;
import recipe.util.ValidateUtil;
import recipe.vo.ResultBean;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 处方明细服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeDetailAtop")
public class RecipeValidateDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeDetailBusinessService recipeDetailService;
    @Autowired
    private IRevisitBusinessService iRevisitBusinessService;

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 长处方标识 0 不是
     */
    private static final String IS_LONG_RECIPE_FALSE = "0";


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


    /**
     * 校验有效复诊单
     *
     * @param mpiId    患者id
     * @param doctorId 医生id
     * @param organId  机构id
     * @return
     */
    @RpcService
    public Boolean revisitValidate(String mpiId, Integer doctorId, Integer organId) {
        logger.info("RecipeDetailDoctorAtop revisitValidate mpiId: {},doctorId :{},organId :{}", mpiId, doctorId, organId);
        validateAtop(mpiId, doctorId, organId);
        Recipe recipe = new Recipe();
        recipe.setMpiid(mpiId);
        recipe.setDoctor(doctorId);
        recipe.setClinicOrgan(organId);
        try {
            Boolean result = iRevisitBusinessService.revisitValidate(recipe);
            logger.info("RecipeDetailDoctorAtop revisitValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("RecipeDetailDoctorAtop revisitValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailDoctorAtop revisitValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 重复处方规则校验
     *
     * @param validateDetailVO 当前处方药品
     * @return
     */
    @RpcService
    public ResultBean<String> validateRepeatRecipe(ValidateDetailVO validateDetailVO) {
        logger.info("RecipeDetailAtop validateRepeatRecipe validateDetailVO ：{}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeBean().getClinicOrgan(), validateDetailVO.getRecipeDetails());
        if (ValidateUtil.integerIsEmpty(validateDetailVO.getRecipeBean().getClinicId())) {
            ResultBean<String> resultBean = new ResultBean<>();
            resultBean.setBool(true);
            return resultBean;
        }
        try {
            ResultBean<String> result = recipeDetailService.validateRepeatRecipe(validateDetailVO);
            logger.info("RecipeDetailAtop validateRepeatRecipe result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop validateRepeatRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop validateRepeatRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 校验开处方单数限制
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @return true 可开方
     */
    @RpcService
    public Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId) {
        logger.info("RecipeDetailAtop validateRepeatRecipe clinicId ：{},organId ：{}", clinicId, organId);
        validateAtop(organId);
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return true;
        }
        try {
            return recipeBusinessService.validateOpenRecipeNumber(clinicId, organId);
        } catch (DAOException e1) {
            logger.error("RecipeDetailAtop validateRepeatRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop validateRepeatRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
