package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.recipe.constant.RecipecCheckStatusConstant;
import com.ngari.recipe.recipe.model.PatientInfoDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.*;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import eh.cdr.api.vo.MedicalDetailBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.IConfigurationClient;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeStatusConstant;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.manager.*;
import recipe.service.RecipeHisService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeService;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.*;
import recipe.vo.doctor.PatientOptionalDrugVO;
import recipe.vo.doctor.PharmacyTcmVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.EmrConfigVO;
import recipe.vo.second.MedicalDetailVO;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方业务核心逻辑处理类
 *
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class RecipeBusinessService extends BaseService implements IRecipeBusinessService {
    /**
     * 操作类型 1：查看，2：copy
     */
    private static final Integer DOC_ACTION_TYPE_INFO = 1;
    private static final Integer DOC_ACTION_TYPE_COPY = 2;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
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
    @Autowired
    private SymptomDAO symptomDAO;
    @Resource
    private IConfigurationClient configurationClient;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private PatientOptionalDrugDAO patientOptionalDrugDAO;
    @Autowired
    private DrugListDAO drugListDAO;
    @Autowired
    protected OrganDrugListDAO organDrugListDAO;
    @Autowired
    protected RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private HisRecipeManager hisRecipeManager;
    @Autowired
    private HisSyncSupervisionService hisSyncSupervisionService;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Autowired
    private StateManager stateManager;
    @Resource
    private AuditModeContext auditModeContext;
    @Autowired
    private ConsultManager consultManager;
    @Autowired
    private RecipeHisService recipeHisService;
    
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
        Long recipesCount = recipeDAO.getRecipeCountByBussSourceAndClinicIdAndStatus(bussSource, clinicId, RecipeStatusEnum.UncheckedStatus);
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
    public List<Recipe> findRecipesByStatusAndInvalidTime(List<Integer> status, Date invalidTime) {
        return recipeDAO.findRecipesByStatusAndInvalidTime(status, invalidTime);
    }

    @Override
    public List<PatientOptionalDrugVO> findPatientOptionalDrugDTO(Integer clinicId) {
        logger.info("RecipeBusinessService findPatientOptionalDrugDTO req clinicId= {}", JSON.toJSONString(clinicId));
        List<PatientOptionalDrug> patientOptionalDrugs = patientOptionalDrugDAO.findPatientOptionalDrugByClinicId(clinicId);
        if (CollectionUtils.isEmpty(patientOptionalDrugs)) {
            logger.info("RecipeBusinessService findPatientOptionalDrugDTO 返回值为空 patientOptionalDrugs= {}", JSON.toJSONString(patientOptionalDrugs));
            return Lists.newArrayList();
        }
        Set<Integer> drugIds = patientOptionalDrugs.stream().collect(Collectors.groupingBy(PatientOptionalDrug::getDrugId)).keySet();
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIdsAndStatus(patientOptionalDrugs.get(0).getOrganId(), drugIds);
        List<DrugList> byDrugIds = drugListDAO.findByDrugIdsAndStatus(drugIds);
        if (CollectionUtils.isEmpty(organDrugList) || CollectionUtils.isEmpty(byDrugIds)) {
            logger.info("药品信息不存在");
            return Lists.newArrayList();
        }
        Map<String, OrganDrugList> organDrugListMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), v -> v));
        Map<Integer, List<DrugList>> drugListMap = byDrugIds.stream().collect(Collectors.groupingBy(DrugList::getDrugId));
        // 获取当前复诊下 的有效处方
        List<Recipedetail> detail = findEffectiveRecipeDetailByClinicId(clinicId);
        Map<String, List<Recipedetail>> collect = detail.stream().collect(Collectors.groupingBy(k -> k.getDrugId() + k.getOrganDrugCode()));

        List<PatientOptionalDrugVO> patientOptionalDrugDTOS = patientOptionalDrugs.stream().map(patientOptionalDrug -> {
            PatientOptionalDrugVO patientOptionalDrugDTO = new PatientOptionalDrugVO();
            org.springframework.beans.BeanUtils.copyProperties(patientOptionalDrug, patientOptionalDrugDTO);
            List<DrugList> drugLists = drugListMap.get(patientOptionalDrug.getDrugId());
            if (CollectionUtils.isNotEmpty(drugLists) && Objects.nonNull(drugLists.get(0))) {
                DrugList drugList = drugLists.get(0);
                org.springframework.beans.BeanUtils.copyProperties(drugList, patientOptionalDrugDTO);
            }
            Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(patientOptionalDrug.getOrganId(), patientOptionalDrugDTO.getDrugType()));
            OrganDrugList organDrugLists = organDrugListMap.get(patientOptionalDrug.getDrugId() + patientOptionalDrug.getOrganDrugCode());
            patientOptionalDrugDTO.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(organDrugLists, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(patientOptionalDrugDTO.getDrugType())));
            if (Objects.isNull(organDrugLists)) {
                return null;
            }
            patientOptionalDrugDTO.setDrugName(organDrugLists.getDrugName());
            patientOptionalDrugDTO.setDrugSpec(organDrugLists.getDrugSpec());
            patientOptionalDrugDTO.setDrugUnit(organDrugLists.getUnit());
            org.springframework.beans.BeanUtils.copyProperties(organDrugLists, patientOptionalDrugDTO);
            patientOptionalDrugDTO.setUseDoseAndUnitRelation(RecipeUtil.defaultUseDose(organDrugLists));
            String pharmacy = organDrugLists.getPharmacy();
            if (StringUtils.isNotEmpty(pharmacy)) {
                String[] pharmacyId = pharmacy.split(",");
                Set pharmaIds = new HashSet();
                for (String s : pharmacyId) {
                    pharmaIds.add(Integer.valueOf(s));
                }
                List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
                if (CollectionUtils.isNotEmpty(pharmacyTcmByIds)) {
                    List<PharmacyTcmVO> pharmacyTcmVOS = pharmacyTcmByIds.stream().map(pharmacyTcm -> {
                        PharmacyTcmVO pharmacyTcmVO = new PharmacyTcmVO();
                        BeanUtils.copy(pharmacyTcm, pharmacyTcmVO);
                        return pharmacyTcmVO;
                    }).collect(Collectors.toList());
                    patientOptionalDrugDTO.setPharmacyTcms(pharmacyTcmVOS);
                }
            }

            List<Recipedetail> recipedetails = collect.get(patientOptionalDrug.getDrugId() + patientOptionalDrug.getOrganDrugCode());

            Double num = 0.00;
            if (CollectionUtils.isNotEmpty(recipedetails)) {
                for (Recipedetail recipedetail : recipedetails) {
                    num = num + recipedetail.getUseTotalDose();
                }
            }
            patientOptionalDrugDTO.setOpenDrugNum(num.intValue());
            return patientOptionalDrugDTO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        logger.info("RecipeBusinessService findPatientOptionalDrugDTO res patientOptionalDrugDTOS= {}", JSON.toJSONString(patientOptionalDrugDTOS));
        return patientOptionalDrugDTOS;
    }

    @Override
    public void savePatientDrug(PatientOptionalDrugVo patientOptionalDrugVo) {
        PatientOptionalDrug patientOptionalDrug = new PatientOptionalDrug();
        BeanUtils.copy(patientOptionalDrugVo, patientOptionalDrug);
        patientOptionalDrugDAO.save(patientOptionalDrug);
    }

    @Override
    public RegulationRecipeIndicatorsDTO regulationRecipe(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>();
        hisSyncSupervisionService.splicingBackRecipeData(Collections.singletonList(recipe), request);
        return ObjectCopyUtils.convert(request.get(0), RegulationRecipeIndicatorsDTO.class);
    }

    @Override
    public MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        MedicalDetailBean medicalDetailBean = null;
        //查看
        if (DOC_ACTION_TYPE_INFO.equals(caseHistoryVO.getActionType())) {
            MedicalDetailBean emrDetails = emrRecipeManager.getEmrDetailsByClinicId(caseHistoryVO.getClinicId(), caseHistoryVO.getBussSource());
            if (!org.springframework.util.StringUtils.isEmpty(emrDetails)) {
                medicalDetailBean = emrDetails;
            } else {
                medicalDetailBean = emrRecipeManager.getEmrDetails(caseHistoryVO.getDocIndexId());
            }
        }
        //copy
        if (DOC_ACTION_TYPE_COPY.equals(caseHistoryVO.getActionType())) {
            if (ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())) {
                medicalDetailBean = emrRecipeManager.getEmrDetailsByClinicId(caseHistoryVO.getClinicId(), caseHistoryVO.getBussSource());
            } else {
                medicalDetailBean = emrRecipeManager.copyEmrDetails(caseHistoryVO.getRecipeId(), caseHistoryVO.getClinicId(), caseHistoryVO.getBussSource());
            }
        }
        if (null == medicalDetailBean) {
            return null;
        }
        MedicalDetailVO medicalDetailVO = new MedicalDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(medicalDetailBean, medicalDetailVO);
        List<EmrConfigVO> detailList = com.ngari.patient.utils.ObjectCopyUtils.convert(medicalDetailBean.getDetailList(), EmrConfigVO.class);
        medicalDetailVO.setDetailList(detailList);
        return medicalDetailVO;
    }


    @Override
    public Boolean confirmAgain(Integer recipeId) {
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        //HIS消息发送
        //审核不通过 往his更新状态（已取消）
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        hisService.recipeStatusUpdateWithOrganId(recipeId, null, null);
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        updateRecipe.setAuditState(RecipeAuditStateEnum.FAIL.getType());
        updateRecipe.setCheckStatus(RecipecCheckStatusConstant.Check_Normal);
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        if (StringUtils.isNotEmpty(dbRecipe.getOrderCode())) {
            RecipeOrder order = recipeOrderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (null != order) {
                stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
            }
        }
        //根据审方模式改变--审核未通过处理
        auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterCheckNotPassYs(dbRecipe);
        //添加发送不通过消息
        RecipeMsgService.batchSendMsg(dbRecipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
        return stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
    }

    @Override
    public Boolean updateAuditState(Integer recipeId, RecipeAuditStateEnum recipeAuditStateEnum) {
        return stateManager.updateAuditState(recipeId, recipeAuditStateEnum);
    }

    @Override
    public RecipeBean getByRecipeCodeAndRegisterIdAndOrganId(String recipeCode, String registerId, int organId) {
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
        logger.info("RecipeBusinessService getByRecipeCodeAndRegisterIdAndOrganId recipe:{}", JSON.toJSONString(recipe));
        if (null != recipe) {
            return ObjectCopyUtils.convert(recipe, RecipeBean.class);
        }
        List<Recipe> recipeList;
        if (StringUtils.isNotEmpty(registerId)) {
            //根据挂号序号查询处方列表
            recipeList = recipeDAO.findByRecipeCodeAndRegisterIdAndOrganId(registerId, organId);
        } else {
            //获取当前一个月的时间段
            Date lastMonthDate = DateConversion.getMonthsAgo(1);
            recipeList = recipeDAO.findRecipeCodesByOrderIdAndTime(organId, lastMonthDate, new Date());
        }
        logger.info("RecipeBusinessService getByRecipeCodeAndRegisterIdAndOrganId recipeList:{}", JSON.toJSONString(recipeList));
        //查看recipeCode是否在recipeCodeList中，这里可能存在这种数据["1212","1222,1211","2312"]
        List<Recipe> result = new ArrayList<>();
        recipeList.forEach(a->{
            if (a.getRecipeCode().contains(",")) {
                String[] codes = a.getRecipeCode().split(",");
                if (Arrays.asList(codes).contains(recipeCode)){
                    result.add(a);
                    return;
                }
            } else {
               if (recipeCode.equals(a.getRecipeCode())) {
                   result.add(a);
                   return;
               }
            }
        });
        if (CollectionUtils.isNotEmpty(result)) {
            return ObjectCopyUtils.convert(result.get(0), RecipeBean.class);
        }
        return null;
    }

    @Override
    public OutPatientRecordResDTO findOutPatientRecordFromHis(String mpiId, Integer organId, Integer doctorId) {
        return consultManager.findOutPatientRecordFromHis(mpiId, organId, doctorId);
    }

    @Override
    public Symptom symptomId(Integer id) {
        return symptomDAO.get(id);
    }

    @Override
    public void recipeListQuery(Integer organId, List<String> recipeCodes) {
        recipeHisService.recipeListQuery(recipeCodes, organId);
    }

    @Override
    public String splitDrugRecipe(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        List<RecipeDetailBean> targetDrugList = detailBeanList.stream().filter(a -> Integer.valueOf(1).equals(a.getTargetedDrugType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(targetDrugList)) {
            return "";
        }
        Integer recipeId = recipeBean.getRecipeId();
        recipeBean.setRecipeId(null);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        //靶向药
        targetDrugList.forEach(a -> recipeService.saveRecipeData(recipeBean, Collections.singletonList(a)));
        //非靶向药
        List<RecipeDetailBean> details = detailBeanList.stream().filter(a -> Integer.valueOf(0).equals(a.getTargetedDrugType())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(details)) {
            recipeBean.setTargetedDrugType(0);
            recipeService.saveRecipeData(recipeBean, details);
        }
        if (!ValidateUtil.integerIsEmpty(recipeId)) {
            recipeDAO.deleteByRecipeIds(Collections.singletonList(recipeId));
        }
        return recipeBean.getGroupCode();
    }

    @Override
    public List<Integer> recipeByGroupCode(String groupCode, Integer type) {
        List<Recipe> list = recipeManager.recipeByGroupCode(groupCode, type);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
    }

    /**
     * 根据复诊id 获取线上线下处方详情
     *
     * @param clinicId
     * @return
     */
    private List<Recipedetail> findEffectiveRecipeDetailByClinicId(Integer clinicId) {
        //线上的有效处方
        List<Recipe> effectiveRecipeByBussSourceAndClinicId = recipeManager.findEffectiveRecipeByBussSourceAndClinicId(BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType(), clinicId);
        List<Integer> recipeIds = effectiveRecipeByBussSourceAndClinicId.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<Recipedetail> byRecipeIdList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            byRecipeIdList = recipeDetailDAO.findByRecipeIdList(recipeIds);
        }

        // 线下有效处方
        RevisitExDTO revisitExDTO = revisitClient.getByClinicId(clinicId);
        if (null == revisitExDTO || StringUtils.isEmpty(revisitExDTO.getRegisterNo())) {
            return byRecipeIdList;
        }
        RevisitBean revisitBean = revisitClient.getRevisitByClinicId(clinicId);
        logger.info("getOfflineEffectiveRecipeFlag revisitBean:{}.", JSONUtils.toString(revisitBean));
        com.ngari.patient.dto.PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(revisitBean.getMpiid());
        List<QueryHisRecipResTO> totalHisRecipe = new ArrayList<>();
        //查询待缴费处方
        HisResponseTO<List<QueryHisRecipResTO>> noPayRecipe = hisRecipeManager.queryData(revisitBean.getConsultOrgan(), patientDTO, null, 1, "");
        //查询已缴费处方
        HisResponseTO<List<QueryHisRecipResTO>> havePayRecipe = hisRecipeManager.queryData(revisitBean.getConsultOrgan(), patientDTO, null, 2, "");
        if (null != noPayRecipe && null != noPayRecipe.getData()) {
            totalHisRecipe.addAll(noPayRecipe.getData());
        }
        if (null != havePayRecipe && null != havePayRecipe.getData()) {
            totalHisRecipe.addAll(havePayRecipe.getData());
        }
        if (CollectionUtils.isEmpty(totalHisRecipe)) {
            return byRecipeIdList;
        }
        List<Recipedetail> finalByRecipeIdList = byRecipeIdList;
        totalHisRecipe.forEach(queryHisRecipResTO -> {
            List<RecipeDetailTO> drugList = queryHisRecipResTO.getDrugList();
            drugList.forEach(recipeDetailTO -> {
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(queryHisRecipResTO.getClinicOrgan(), Arrays.asList(recipeDetailTO.getDrugCode()));
                Recipedetail recipedetail = new Recipedetail();
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugId(organDrugLists.get(0).getDrugId());
                    recipedetail.setOrganDrugCode(recipeDetailTO.getDrugCode());
                }
                if (recipeDetailTO.getUseTotalDose() != null) {
                    recipedetail.setUseTotalDose(recipeDetailTO.getUseTotalDose().doubleValue());
                }
                finalByRecipeIdList.add(recipedetail);
            });
        });

        return finalByRecipeIdList;
    }

}

