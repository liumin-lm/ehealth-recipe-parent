package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipe.constant.RecipeTypeEnum;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import com.ngari.recipe.recipe.model.PatientInfoDTO;
import recipe.manager.HisRecipeManager;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.ChinaIDNumberUtil;

import java.math.BigDecimal;
import java.util.Arrays;
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
    private OfflineRecipeClient offlineRecipeClient;

    @Autowired
    private RemoteRecipeService remoteRecipeService;

    @Autowired
    private PatientClient patientClient;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private HisRecipeManager hisRecipeManager;

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

    /**
     * 获取线下处方详情
     *
     * @param mpiId       患者ID
     * @param clinicOrgan 机构ID
     * @param recipeCode  处方号码
     * @date 2021/8/06
     */
    @Override
    public OffLineRecipeDetailVO getOffLineRecipeDetails(String mpiId, Integer clinicOrgan, String recipeCode) {
        logger.info("RecipeBusinessService getOffLineRecipeDetails mpiId={},clinicOrgan={},recipeCode={}", mpiId, clinicOrgan, recipeCode);
        PatientDTO patient = patientService.getPatientByMpiId(mpiId);
        if (null == patient) {
            throw new DAOException(609, "患者信息不存在");
        }
        //获取线下处方信息
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(clinicOrgan, patient, 6, 2, recipeCode);

        List<QueryHisRecipResTO> data = null;
        if (!ObjectUtils.isEmpty(hisRecipeInfos)) {
            data = hisRecipeInfos.getData();
        } else {
            throw new DAOException(609, "线下处方信息为空");
        }
        QueryHisRecipResTO queryHisRecipResTO = null;
        if (!ObjectUtils.isEmpty(data)) {
            queryHisRecipResTO = data.get(0);
        }
        OffLineRecipeDetailVO offLineRecipeDetailVO = new OffLineRecipeDetailVO();
        //预留字段 后续实现电子病历业务使用
        offLineRecipeDetailVO.setDocIndexId(null);
        //设置返回字段
        if (!ObjectUtils.isEmpty(queryHisRecipResTO)) {
            BeanUtils.copy(queryHisRecipResTO, offLineRecipeDetailVO);
            offLineRecipeDetailVO.setOrganDiseaseName(queryHisRecipResTO.getDiseaseName());
            //根据枚举设置处方类型
            Integer recipeType = queryHisRecipResTO.getRecipeType();
            String recipeTypeText = RecipeTypeEnum.getRecipeType(recipeType);
            offLineRecipeDetailVO.setRecipeTypeText(recipeTypeText);
            //判断是否为医保处方
            Integer medicalType = queryHisRecipResTO.getMedicalType();
            if (medicalType == 2) {
                offLineRecipeDetailVO.setMedicalTypeText("普通医保");
            }

            //判断是否为儿科 设置部门名称
            DepartmentDTO departmentDTO = departmentService.getByCodeAndOrgan(queryHisRecipResTO.getDepartCode(), queryHisRecipResTO.getClinicOrgan());
            if (!ObjectUtils.isEmpty(departmentDTO)) {
                if (departmentDTO.getName().contains("儿科") || departmentDTO.getName().contains("新生儿科")
                        || departmentDTO.getName().contains("儿内科") || departmentDTO.getName().contains("儿外科")) {
                    offLineRecipeDetailVO.setChildRecipeFlag(true);
                    //设置监护人字段
                    if (!ObjectUtils.isEmpty(patient)) {
                        offLineRecipeDetailVO.setGuardianName(patient.getGuardianName());
                        offLineRecipeDetailVO.setGuardianAge(patient.getGuardianAge());
                        offLineRecipeDetailVO.setGuardianSex(patient.getGuardianSex());
                    }
                }
                offLineRecipeDetailVO.setDepartName(departmentDTO.getName());
            }
            //处方药品信息
            List<RecipeDetailTO> drugList = queryHisRecipResTO.getDrugList();
            BigDecimal totalPrice = BigDecimal.valueOf(0);
            //计算药品价格
            if (!ObjectUtils.isEmpty(drugList)) {
                for (RecipeDetailTO recipeDetailTO : drugList) {
                    totalPrice = totalPrice.add(recipeDetailTO.getTotalPrice());
                }
                offLineRecipeDetailVO.setRecipeDetails(drugList);
                offLineRecipeDetailVO.setTotalPrice(totalPrice);
            }
            //患者基本属性
            if (!ObjectUtils.isEmpty(patient)) {
                offLineRecipeDetailVO.setPatientSex(patient.getPatientSex());
                offLineRecipeDetailVO.setPatientBirthday(patient.getBirthday());
            }
        }

        logger.info("RecipeBusinessService getOffLineRecipeDetails result={}", JSONUtils.toString(offLineRecipeDetailVO));
        return offLineRecipeDetailVO;
    }
}
