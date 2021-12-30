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
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;
import recipe.vo.ResultBean;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 处方校验服务入口类
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
        logger.info("RecipeValidateDoctorAtop validateDetailV1 validateDetailVO ：{}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        try {
            ValidateDetailVO result = recipeDetailService.continueRecipeValidateDrug(validateDetailVO);
            logger.info("RecipeValidateDoctorAtop validateDetailV1 result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop validateDetailV1 error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop validateDetailV1 error e", e);
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
        logger.info("RecipeValidateDoctorAtop useDayValidate validateDetailVO {}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        try {
            List<RecipeDetailBean> result = recipeDetailService.useDayValidate(validateDetailVO);
            logger.info("RecipeValidateDoctorAtop useDayValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop useDayValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop useDayValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 检验处方药品超量
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    @RpcService
    public List<RecipeDetailBean> drugSuperScalarValidate(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO.getOrganId());
        if (RecipeUtil.isTcmType(validateDetailVO.getRecipeType()) || CollectionUtils.isEmpty(validateDetailVO.getRecipeDetails())) {
            return validateDetailVO.getRecipeDetails();
        }
        return recipeDetailService.drugSuperScalarValidate(validateDetailVO);
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
        logger.info("RecipeValidateDoctorAtop entrustValidate recipeDetails = {}，organId= {}", JSON.toJSONString(recipeDetails), organId);
        if (ValidateUtil.integerIsEmpty(organId) || CollectionUtils.isEmpty(recipeDetails)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            List<RecipeDetailBean> result = recipeDetailService.entrustValidate(organId, recipeDetails);
            logger.info("RecipeValidateDoctorAtop entrustValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop entrustValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop entrustValidate error e", e);
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
        logger.info("RecipeValidateDoctorAtop revisitValidate mpiId: {},doctorId :{},organId :{}", mpiId, doctorId, organId);
        validateAtop(mpiId, doctorId, organId);
        Recipe recipe = new Recipe();
        recipe.setMpiid(mpiId);
        recipe.setDoctor(doctorId);
        recipe.setClinicOrgan(organId);
        try {
            Boolean result = iRevisitBusinessService.revisitValidate(recipe);
            logger.info("RecipeValidateDoctorAtop revisitValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("RecipeValidateDoctorAtop revisitValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop revisitValidate error e", e);
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
        logger.info("RecipeValidateDoctorAtop validateRepeatRecipe validateDetailVO ：{}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeBean().getClinicOrgan(), validateDetailVO.getRecipeDetails());
        if (ValidateUtil.integerIsEmpty(validateDetailVO.getRecipeBean().getClinicId())) {
            ResultBean<String> resultBean = new ResultBean<>();
            resultBean.setBool(true);
            return resultBean;
        }
        try {
            ResultBean<String> result = recipeDetailService.validateRepeatRecipe(validateDetailVO);
            logger.info("RecipeValidateDoctorAtop validateRepeatRecipe result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop validateRepeatRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop validateRepeatRecipe error e", e);
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
    public Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId) {
        logger.info("RecipeValidateDoctorAtop validateOpenRecipeNumber clinicId ：{},organId ：{},recipeId ：{}", clinicId, organId, recipeId);
        validateAtop(organId);
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return true;
        }
        try {
            return recipeBusinessService.validateOpenRecipeNumber(clinicId, organId, recipeId);
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop validateOpenRecipeNumber error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop validateOpenRecipeNumber error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
