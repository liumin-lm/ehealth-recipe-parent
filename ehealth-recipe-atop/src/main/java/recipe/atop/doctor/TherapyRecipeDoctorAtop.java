package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.DateConversion;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import recipe.atop.BaseAtop;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.constant.PageInfoConstant;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;
import recipe.vo.doctor.TherapyRecipePageVO;
import recipe.vo.second.OrganVO;

import java.util.Collections;
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
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeBean());
        validateAtop("请添加项目信息", recipeInfoVO.getRecipeDetails());
        RecipeBean recipeBean = recipeInfoVO.getRecipeBean();
        validateAtop(recipeBean.getDoctor(), recipeBean.getMpiid(), recipeBean.getClinicOrgan(), recipeBean.getClinicId(), recipeBean.getDepart());
        recipeBean.setStatus(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType());
        recipeBean.setProcessState(RecipeStateEnum.PROCESS_STATE_SUBMIT.getType());
        recipeBean.setSubState(RecipeStateEnum.SUB_SUBMIT_TEMPORARY.getType());
        recipeBean.setRecipeSourceType(3);
        recipeBean.setSignDate(DateTime.now().toDate());
        recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        recipeBean.setChooseFlag(0);
        recipeBean.setGiveFlag(0);
        recipeBean.setPayFlag(0);
        recipeBean.setPushFlag(0);
        recipeBean.setRemindFlag(0);
        recipeBean.setTakeMedicine(0);
        recipeBean.setPatientStatus(1);
        if (null == recipeInfoVO.getRecipeExtendBean()) {
            recipeInfoVO.setRecipeExtendBean(new RecipeExtendBean());
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
        logger.info("TherapyRecipeDoctorAtop submitTherapyRecipe recipeInfoVO = {}", JSON.toJSONString(recipeInfoVO));
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeDetails());
        Integer recipeId = saveTherapyRecipe(recipeInfoVO);
        //推送his
        RecipeInfoDTO recipeInfoDTO = offlineToOnlineService.pushRecipe(recipeId, CommonConstant.RECIPE_PUSH_TYPE,
                CommonConstant.RECIPE_DOCTOR_TYPE, null, null, null, 1);
        therapyRecipeBusinessService.updatePushTherapyRecipe(recipeId, recipeInfoDTO.getRecipeTherapy(), CommonConstant.RECIPE_PUSH_TYPE);
        return recipeId;
    }

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapyVO 诊疗处方对象
     * @param start           页数
     * @param limit           每页条数
     * @return key 复诊id
     */
    @RpcService
    public TherapyRecipePageVO therapyRecipeList(RecipeTherapyVO recipeTherapyVO, int start, int limit) {
        logger.info("TherapyRecipeDoctorAtop therapyRecipeList  recipeTherapyVO = {},start:{},limit:{}", JSON.toJSONString(recipeTherapyVO), start, limit);
        validateAtop(recipeTherapyVO, recipeTherapyVO.getOrganId());
        if (ValidateUtil.validateObjects(recipeTherapyVO.getMpiId()) && ValidateUtil.validateObjects(recipeTherapyVO.getDoctorId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeTherapyVO, RecipeTherapy.class);
        Integer total;
        try {
            total = therapyRecipeBusinessService.therapyRecipeTotal(recipeTherapy);
            logger.info("TherapyRecipeDoctorAtop therapyRecipeList total = {}", total);
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop therapyRecipeList total error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop therapyRecipeList total error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
        TherapyRecipePageVO therapyRecipePageVO = new TherapyRecipePageVO();
        therapyRecipePageVO.setLimit(limit);
        therapyRecipePageVO.setStart(start);
        therapyRecipePageVO.setTotal(total);
        if (ValidateUtil.validateObjects(total)) {
            therapyRecipePageVO.setRecipeInfoList(Collections.emptyList());
            return therapyRecipePageVO;
        }
        List<RecipeInfoDTO> recipeInfoList;
        try {
            recipeInfoList = therapyRecipeBusinessService.therapyRecipeList(recipeTherapy, start * limit, limit);
            logger.info("TherapyRecipeDoctorAtop therapyRecipeList  recipeInfoList = {}", JSON.toJSONString(recipeInfoList));
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop therapyRecipeList  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop therapyRecipeList  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
        List<RecipeInfoVO> result = new LinkedList<>();

        recipeInfoList.forEach(a -> {
            RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
            recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(a.getPatientBean(), PatientVO.class));
            recipeInfoVO.setRecipeTherapyVO(ObjectCopyUtils.convert(a.getRecipeTherapy(), RecipeTherapyVO.class));

            RecipeBean recipeBean = new RecipeBean();
            recipeBean.setRecipeId(a.getRecipe().getRecipeId());
            recipeBean.setClinicId(a.getRecipe().getClinicId());
            recipeBean.setOrganDiseaseName(a.getRecipe().getOrganDiseaseName());
            recipeBean.setCreateDate(a.getRecipe().getCreateDate());
            if (null != recipeBean.getCreateDate()) {
                recipeBean.setWxDisplayTime(DateConversion.convertRequestDateForBussNew(recipeBean.getCreateDate()));
            }
            recipeInfoVO.setRecipeBean(recipeBean);

            List<RecipeDetailBean> recipeDetails = new LinkedList<>();
            if (!CollectionUtils.isEmpty(a.getRecipeDetails())) {
                a.getRecipeDetails().forEach(b -> {
                    RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
                    recipeDetailBean.setDrugName(b.getDrugName());
                    recipeDetailBean.setType(b.getType());
                    recipeDetails.add(recipeDetailBean);
                });
                recipeInfoVO.setRecipeDetails(recipeDetails);
            }
            result.add(recipeInfoVO);
        });
        therapyRecipePageVO.setRecipeInfoList(result);
        return therapyRecipePageVO;
    }

    /**
     * 获取诊疗处方明细
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    public RecipeInfoVO therapyRecipeInfo(Integer recipeId) {
        logger.info("therapyRecipeInfo = {}", recipeId);

        RecipeInfoDTO result = therapyRecipeBusinessService.therapyRecipeInfo(recipeId);
        //越权校验
        OrganVO organVO=ObjectCopyUtils.convert(result.getOrgan(), OrganVO.class);
        isAuthorisedOrgan(organVO==null?null:organVO.getOrganId());

        RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
        recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(result.getPatientBean(), PatientVO.class));
        recipeInfoVO.setRecipeBean(ObjectCopyUtils.convert(result.getRecipe(), RecipeBean.class));
        recipeInfoVO.setRecipeExtendBean(ObjectCopyUtils.convert(result.getRecipeExtend(), RecipeExtendBean.class));
        recipeInfoVO.setRecipeDetails(ObjectCopyUtils.convert(result.getRecipeDetails(), RecipeDetailBean.class));
        recipeInfoVO.setRecipeTherapyVO(ObjectCopyUtils.convert(result.getRecipeTherapy(), RecipeTherapyVO.class));
        recipeInfoVO.setOrganVO(organVO);

        return recipeInfoVO;
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
        validateAtop(recipeTherapyVO, recipeTherapyVO.getTherapyCancellationType(), recipeTherapyVO.getRecipeId());
        try {
            //异步推送his
            offlineToOnlineService.pushRecipe(recipeTherapyVO.getRecipeId(), CommonConstant.RECIPE_CANCEL_TYPE,
                    CommonConstant.RECIPE_DOCTOR_TYPE, null, null, null, 1);
            RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeTherapyVO, RecipeTherapy.class);
            therapyRecipeBusinessService.updatePushTherapyRecipe(recipeTherapyVO.getRecipeId(), recipeTherapy, CommonConstant.RECIPE_CANCEL_TYPE);
            return true;
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
        validateAtop(recipeId);
        return therapyRecipeBusinessService.abolishTherapyRecipe(recipeId);
    }

    /**
     * 复诊关闭作废诊疗处方
     *
     * @param bussSource 业务类型
     * @param clinicId 复诊ID
     * @return 作废结果
     */
    @RpcService
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId){
        validateAtop(bussSource, clinicId);
        return therapyRecipeBusinessService.abolishTherapyRecipeForRevisitClose(bussSource, clinicId);
    }

    /**
     * 搜索诊疗项目
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO){
        validateAtop(itemListVO, itemListVO.getOrganId());
        //drugName 为空时可以查询默认的  默认第一个分页数据
        if(ValidateUtil.integerIsEmpty(itemListVO.getStart())){
            itemListVO.setStart(PageInfoConstant.PAGE_NO);
        }
        if(ValidateUtil.integerIsEmpty(itemListVO.getLimit())){
            itemListVO.setLimit(PageInfoConstant.PAGE_SIZE);
        }
        List<ItemListVO> result = therapyRecipeBusinessService.searchItemListByKeyWord(itemListVO);
        logger.info("TherapyRecipeDoctorAtop searchItemListByKeyWord result:{}.", JSON.toJSONString(result));
        return result;
    }

}
