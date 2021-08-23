package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.dto.RecipeInfoDTO;
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
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.ItemListVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

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

    /**
     * 搜索诊疗项目
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO){
        logger.info("TherapyRecipeDoctorAtop searchItemListByKeyWord itemListVO:{}.", JSON.toJSONString(itemListVO));
        validateAtop(itemListVO, itemListVO.getOrganID(),itemListVO.getItemName(), itemListVO.getLimit());
        try {
            List<ItemListVO> result = therapyRecipeBusinessService.searchItemListByKeyWord(itemListVO);
            logger.info("TherapyRecipeDoctorAtop searchItemListByKeyWord result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop searchItemListByKeyWord  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop searchItemListByKeyWord  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
