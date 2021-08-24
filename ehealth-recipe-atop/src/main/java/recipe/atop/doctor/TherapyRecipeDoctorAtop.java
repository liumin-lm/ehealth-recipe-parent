package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

import java.util.LinkedList;
import java.util.List;

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
        recipeInfoVO.getRecipeBean().setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
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

    /**
     * 提交诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    @RpcService
    public Integer submitTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        Integer recipeId = saveTherapyRecipe(recipeInfoVO);
        //异步推送his
        offlineToOnlineService.pushTherapyRecipeExecute(recipeId, CommonConstant.THERAPY_RECIPE_PUSH_TYPE);
        return recipeId;
    }

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapyVO 诊疗处方对象
     * @param start           页数
     * @param limit           每页条数
     * @return
     */
    @RpcService
    public List<RecipeInfoVO> therapyRecipeList(RecipeTherapyVO recipeTherapyVO, int start, int limit) {
        logger.info("TherapyRecipeDoctorAtop therapyRecipeList  recipeTherapyVO = {},start:{},limit:{}", JSON.toJSONString(recipeTherapyVO), start, limit);
        validateAtop(recipeTherapyVO, recipeTherapyVO.getOrganId());
        if (ValidateUtil.validateObjects(recipeTherapyVO.getMpiId()) && ValidateUtil.validateObjects(recipeTherapyVO.getDoctorId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeTherapyVO, RecipeTherapy.class);
        try {
            List<RecipeInfoDTO> recipeInfoList = therapyRecipeBusinessService.therapyRecipeList(recipeTherapy, start, limit);
            List<RecipeInfoVO> result = new LinkedList<>();
            recipeInfoList.forEach(a -> {
                RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
                recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(a.getPatientBean(), PatientVO.class));
                recipeInfoVO.setRecipeTherapyVO(ObjectCopyUtils.convert(a.getRecipeTherapy(), RecipeTherapyVO.class));
                RecipeBean recipeBean = new RecipeBean();
                recipeBean.setOrganDiseaseName(a.getRecipe().getOrganDiseaseName());
                recipeInfoVO.setRecipeBean(recipeBean);
                List<RecipeDetailBean> recipeDetails = new LinkedList<>();
                a.getRecipeDetails().forEach(b -> {
                    RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
                    recipeDetailBean.setDrugName(b.getDrugName());
                    recipeDetailBean.setType(b.getType());
                });
                recipeInfoVO.setRecipeDetails(recipeDetails);
                result.add(recipeInfoVO);
            });
            logger.info("TherapyRecipeDoctorAtop therapyRecipeList  result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop therapyRecipeList  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop therapyRecipeList  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取诊疗处方明细
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    public RecipeInfoVO therapyRecipeInfo(Integer recipeId) {
        logger.info("TherapyRecipeDoctorAtop therapyRecipeInfo  recipeId = {}", recipeId);
        try {
            RecipeInfoDTO result = therapyRecipeBusinessService.therapyRecipeInfo(recipeId);
            RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
            recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(result.getPatientBean(), PatientVO.class));
            recipeInfoVO.setRecipeBean(ObjectCopyUtils.convert(result.getRecipe(), RecipeBean.class));
            recipeInfoVO.setRecipeExtendBean(ObjectCopyUtils.convert(result.getRecipeExtend(), RecipeExtendBean.class));
            recipeInfoVO.setRecipeDetails(ObjectCopyUtils.convert(result.getRecipeDetails(), RecipeDetailBean.class));
            recipeInfoVO.setRecipeTherapyVO(ObjectCopyUtils.convert(result.getRecipeTherapy(), RecipeTherapyVO.class));
            logger.info("TherapyRecipeDoctorAtop therapyRecipeInfo  recipeInfoVO = {}", JSON.toJSONString(recipeInfoVO));
            return recipeInfoVO;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop therapyRecipeInfo  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop therapyRecipeInfo  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 撤销处方
     *
     * @param recipeTherapyVO 撤销参数
     * @return 撤销结果
     */
    @RpcService
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
     * @param recipeId 处方ID
     */
    @RpcService
    public boolean abolishTherapyRecipe(Integer recipeId) {
        logger.info("TherapyRecipeDoctorAtop abolishTherapyRecipe recipeId:{}.", recipeId);
        validateAtop(recipeId);
        try {
            return therapyRecipeBusinessService.abolishTherapyRecipe(recipeId);
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop abolishTherapyRecipe  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop abolishTherapyRecipe  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
