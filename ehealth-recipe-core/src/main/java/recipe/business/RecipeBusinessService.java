package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import com.ngari.recipe.dto.PatientOptionalDrugDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.PatientInfoDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.IConfigurationClient;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.OrganDrugListManager;
import recipe.manager.RecipeManager;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处方业务核心逻辑处理类
 *
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class RecipeBusinessService extends BaseService implements IRecipeBusinessService {

    //药师审核不通过状态集合 供getUncheckRecipeByClinicId方法使用
    private final List<Integer> UncheckedStatus = Arrays.asList(RecipeStatusEnum.RECIPE_STATUS_UNCHECK.getType(), RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType(),
            RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_PHA.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_NO_CODE_PHA.getType());
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OfflineRecipeClient offlineRecipeClient;
    @Autowired
    private RemoteRecipeService remoteRecipeService;
    @Autowired
    private PatientClient patientClient;
    @Resource
    private IConfigurationClient configurationClient;
    @Resource
    private OrganDrugListManager organDrugListManager;

    /**
     * 获取线下门诊处方诊断信息
     *
     * @param patientInfoVO 患者信息
     * @return 诊断列表
     */
    @Override

    public List<DiseaseInfoDTO> getOutRecipeDisease(PatientInfoVO patientInfoVO) {
        return offlineRecipeClient.queryPatientDisease(patientInfoVO.getOrganId(), patientInfoVO.getPatientName(), patientInfoVO.getRegisterID(), patientInfoVO.getPatientId());
    }

    /**
     * 查询门诊处方信息
     *
     * @param outPatientRecipeReqVO 患者信息
     * @return 门诊处方列表
     */
    @Override
    public List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        OutPatientRecipeReq outPatientRecipeReq = ObjectCopyUtil.convert(outPatientRecipeReqVO, OutPatientRecipeReq.class);
        return offlineRecipeClient.queryOutPatientRecipe(outPatientRecipeReq);
    }

    /**
     * 获取门诊处方详情信息
     *
     * @param outRecipeDetailReqVO 门诊处方信息
     * @return 图片或者PDF链接等
     */
    @Override
    public OutRecipeDetailVO queryOutRecipeDetail(OutRecipeDetailReqVO outRecipeDetailReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe queryOutRecipeDetail:{}.", JSON.toJSONString(outRecipeDetailReqVO));
        OutRecipeDetailReq outRecipeDetailReq = ObjectCopyUtil.convert(outRecipeDetailReqVO, OutRecipeDetailReq.class);
        OutRecipeDetailDTO outRecipeDetailDTO = offlineRecipeClient.queryOutRecipeDetail(outRecipeDetailReq);
        return ObjectCopyUtil.convert(outRecipeDetailDTO, OutRecipeDetailVO.class);
    }

    /**
     * 前端获取用药指导
     *
     * @param medicationGuidanceReqVO 用药指导入参
     * @return 用药指导出参
     */
    @Override
    public MedicationGuideResVO getMedicationGuide(MedicationGuidanceReqVO medicationGuidanceReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe getMedicationGuide:{}.", JSON.toJSONString(medicationGuidanceReqVO));
        //获取患者信息
        PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(medicationGuidanceReqVO.getMpiId());
        PatientInfoDTO patientParam = new PatientInfoDTO();
        //患者编号
        patientParam.setPatientCode(medicationGuidanceReqVO.getPatientID());
        patientParam.setPatientName(patientDTO.getPatientName());
        patientParam.setDeptName(medicationGuidanceReqVO.getDeptName());
        //就诊号
        patientParam.setAdminNo(medicationGuidanceReqVO.getPatientID());
        try {
            patientParam.setPatientAge(String.valueOf(ChinaIDNumberUtil.getStringAgeFromIDNumber(patientDTO.getCertificate())));
        } catch (ValidateException e) {
            logger.error("OutPatientRecipeAtop getMedicationGuide error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者年龄获取失败");
        }
        patientParam.setCardType(1);
        patientParam.setCard(patientDTO.getCertificate());
        patientParam.setGender(Integer.valueOf(patientDTO.getPatientSex()));
        patientParam.setDocDate(medicationGuidanceReqVO.getCreateDate());
        patientParam.setFlag(0);
        //获取处方信息
        RecipeBean recipeBean = new RecipeBean();
        BeanUtils.copy(medicationGuidanceReqVO, recipeBean);
        List<MedicationRecipeDetailVO> recipeDetailVOS = medicationGuidanceReqVO.getRecipeDetails();
        List<RecipeDetailBean> recipeDetailBeans = recipeDetailVOS.stream().map(detail -> {
            RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
            BeanUtils.copy(detail, recipeDetailBean);
            return recipeDetailBean;
        }).collect(Collectors.toList());
        Map<String, Object> linkInfo = remoteRecipeService.getHtml5LinkInfo(patientParam, recipeBean, recipeDetailBeans, medicationGuidanceReqVO.getReqType());
        MedicationGuideResVO result = new MedicationGuideResVO();
        result.setType("h5");
        result.setData(linkInfo.get("url").toString());
        return result;
    }

    /**
     * 根据bussSource和clinicID查询是否存在药师审核未通过的处方
     *
     * @param bussSource 处方来源
     * @param clinicId   复诊ID
     * @return true 存在  false 不存在
     * @date 2021/7/16
     */
    @Override
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicId) {
        logger.info("RecipeBusinessService existUncheckRecipe bussSource={},clinicID={}", bussSource, clinicId);
        //获取处方状态为药师审核不通过的处方个数
        Long recipesCount = recipeDAO.getRecipeCountByBussSourceAndClinicIdAndStatus(bussSource, clinicId, UncheckedStatus);
        int uncheckCount = recipesCount.intValue();
        logger.info("RecipeBusinessService existUncheckRecipe recipesCount={}", recipesCount);
        return uncheckCount != 0;
    }

    @Override
    public Recipe getByRecipeId(Integer recipeId) {
        return recipeManager.getRecipeById(recipeId);
    }

    @Override
    public Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId) {
        logger.info("RecipeBusinessService validateOpenRecipeNumber clinicId: {},organId: {}", clinicId, organId);
        //运营平台没有处方单数限制，默认可以无限进行开处方
        Integer openRecipeNumber = configurationClient.getValueCatch(organId, "openRecipeNumber", 999);
        logger.info("RecipeBusinessService validateOpenRecipeNumber openRecipeNumber={}", openRecipeNumber);
        if (ValidateUtil.integerIsEmpty(openRecipeNumber)) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "开方张数0已超出医院限定范围，不能继续开方。");
        }
        //查询当前复诊存在的有效处方单
        List<Integer> recipeIds = recipeManager.findRecipeByClinicId(clinicId, recipeId, RecipeStatusEnum.RECIPE_REPEAT_COUNT);
        if (CollectionUtils.isEmpty(recipeIds)) {
            return true;
        }
        logger.info("RecipeBusinessService validateOpenRecipeNumber recipeCount={}", recipeIds.size());
        if (recipeIds.size() >= openRecipeNumber) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "开方张数已超出医院限定范围，不能继续开方。");
        }
        return true;
    }

    @Override
    @LogRecord
    public List<Recipe> findRecipesByStatusAndInvalidTime(List<Integer> status, Date invalidTime) {
        return recipeDAO.findRecipesByStatusAndInvalidTime(status, invalidTime);
    }

    @Override
    public List<PatientOptionalDrugDTO> findPatientOptionalDrugDTO(Integer clinicId) {
        return organDrugListManager.findPatientOptionalDrugDTO(clinicId);
    }

}

