package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.recipe.ChineseMedicineMsgVO;
import com.ngari.recipe.recipe.constant.RecipeTypeEnum;
import com.ngari.recipe.recipe.constant.RecipecCheckStatusConstant;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import eh.cdr.api.vo.MedicalDetailBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.IConfigurationClient;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeStatusConstant;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.*;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.enumerate.type.DrugBelongTypeEnum;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.manager.*;
import recipe.service.*;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.*;
import recipe.vo.doctor.PatientOptionalDrugVO;
import recipe.vo.doctor.PharmacyTcmVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.greenroom.DrugUsageLabelResp;
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
    private EmrRecipeManager emrRecipeManager;
    @Autowired
    private StateManager stateManager;
    @Resource
    private AuditModeContext auditModeContext;
    @Autowired
    private ConsultManager consultManager;
    @Autowired
    private RecipeHisService recipeHisService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private CreatePdfFactory createPdfFactory;


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

    public static void main(String[] args) {
        List<OutPatientRecipeDetailDTO> outPatientRecipeDetails = new ArrayList<>();
        OutPatientRecipeDetailDTO outPatientRecipeDetailDTO = new OutPatientRecipeDetailDTO();
        outPatientRecipeDetailDTO.setType(3);
        outPatientRecipeDetails.add(outPatientRecipeDetailDTO);
        Boolean haveSecrecyDrugFlag = outPatientRecipeDetails.stream().anyMatch(outPatientRecipeDetail -> DrugBelongTypeEnum.SECRECY_DRUG.getType().equals(outPatientRecipeDetail.getType()));
        System.out.println(haveSecrecyDrugFlag);
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
            detail.setRecipeDetailId(1);
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
        Recipe recipe = recipeManager.getRecipeById(recipeId);
        Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy(recipe.getStatus(), recipe, null, null);
        recipe.setShowTip(tipMap.get("cancelReason"));
        return recipe;
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
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        service.splicingBackRecipeData(Collections.singletonList(recipe), request);
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
    public Boolean updateRecipeState(Integer recipeId, RecipeStateEnum processState, RecipeStateEnum subState) {
        return stateManager.updateRecipeState(recipeId, processState, subState);
    }

    @Override
    public Boolean updateCheckerSignState(Integer recipeId, SignEnum checkerSignState) {
        return stateManager.updateCheckerSignState(recipeId, checkerSignState);
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
        recipeList.forEach(a -> {
            if (a.getRecipeCode().contains(",")) {
                String[] codes = a.getRecipeCode().split(",");
                if (Arrays.asList(codes).contains(recipeCode)) {
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

    @Override
    public List<RecipeBean> recipeListByClinicId(Integer clinicId, Integer bussSource) {
        List<Recipe> recipeList = recipeDAO.findRecipeByBussSourceAndClinicId(bussSource, clinicId);
        RecipeListService recipeListService = ApplicationUtils.getRecipeService(RecipeListService.class);
        List<Map<String, Object>> map = recipeListService.findRecipesForRecipeList(recipeList, null);
        return map.stream().map(a -> (RecipeBean) a.get("recipe")).collect(Collectors.toList());
    }

    @Override
    public List<RecipeDetailBean> findRecipeDetailByRecipeId(Integer recipeId) {
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        return ObjectCopyUtils.convert(recipeDetailList, RecipeDetailBean.class);
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


        return byRecipeIdList;
    }

    /**
     * 根据处方单获取药品用法标签列表
     *
     * @param recipeId
     * @return
     */
    @Override
    public DrugUsageLabelResp queryRecipeDrugUsageLabel(Integer recipeId) {
        DrugUsageLabelResp drugUsageLabelResp = new DrugUsageLabelResp();

        RecipeDTO recipeDTO = recipeManager.getRecipeDTOSimple(recipeId);
        Recipe recipe = recipeDTO.getRecipe();
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        RecipeOrder recipeOrder = recipeDTO.getRecipeOrder();
        Integer enterpriseId = recipe.getEnterpriseId();

        if (Objects.isNull(enterpriseId)) {
            throw new DAOException("未查询到对应药企！");
        }
        DrugsEnterprise drugsEnterprise = enterpriseManager.drugsEnterprise(recipe.getEnterpriseId());
        drugUsageLabelResp.setEnterpriseName(drugsEnterprise.getName());

        //患者信息
        PatientDTO patientDTO = patientClient.getPatientDTOByMpiId(recipe.getMpiid());
        if (Objects.nonNull(patientDTO)) {
            drugUsageLabelResp.setPatientName(patientDTO.getPatientName());
            drugUsageLabelResp.setPatientAge(patientDTO.getAgeString());
            drugUsageLabelResp.setPatientSex(patientDTO.getPatientSex());
        }

        drugUsageLabelResp.setRecipeType(recipe.getRecipeType());
        drugUsageLabelResp.setDispensingTime(recipeOrder.getDispensingTime());

        if (RecipeTypeEnum.RECIPETYPE_WM.getType().equals(recipe.getRecipeType()) ||
                RecipeTypeEnum.RECIPETYPE_CPM.getType().equals(recipe.getRecipeType())) {
            //西药，中成药
            // 是否医院结算药企
            Boolean isHosSettle = enterpriseManager.getIsHosSettle(recipeOrder);
            if (CollectionUtils.isNotEmpty(recipeDetails)) {
                List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(recipeDetails, RecipeDetailBean.class);
                for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                    if (Objects.nonNull(recipeDetailBean.getHisReturnSalePrice()) && isHosSettle) {
                        recipeDetailBean.setActualSalePrice(recipeDetailBean.getHisReturnSalePrice());
                    }
                }
                drugUsageLabelResp.setDrugUsageLabelList(recipeDetailBeans);
            }
        } else if (RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType()) ||
                RecipeTypeEnum.RECIPETYPE_HP.getType().equals(recipe.getRecipeType())) {
            //中药, 膏方
            ChineseMedicineMsgVO chineseMedicineMsg = new ChineseMedicineMsgVO();
            chineseMedicineMsg.setCopyNum(recipe.getCopyNum());
            chineseMedicineMsg.setTotalFee(recipeOrder.getTotalFee());
            chineseMedicineMsg.setMakeMethodText(recipeExtend.getMakeMethodText());
            chineseMedicineMsg.setDecoctionText(recipeExtend.getDecoctionText());

            Boolean ecoctionFlag;
            String decoctionDeploy = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "decoctionDeploy", "0");
            if ("0".equals(decoctionDeploy)) {
                ecoctionFlag = false;
            } else if ("1".equals(decoctionDeploy)) {
                ecoctionFlag = "1".equals(recipeExtend.getDoctorIsDecoction());
            } else {
                ecoctionFlag = "1".equals(recipeOrder.getPatientIsDecoction());
            }

            chineseMedicineMsg.setDecoctionFlag(ecoctionFlag);
            chineseMedicineMsg.setJuice(recipeExtend.getJuice());
            if (CollectionUtils.isNotEmpty(recipeDetails)) {
                chineseMedicineMsg.setUsePathways(recipeDetails.get(0).getUsePathways());
                chineseMedicineMsg.setUsingRate(recipeDetails.get(0).getUsingRate());
                chineseMedicineMsg.setMemo(recipe.getRecipeMemo());
            }
            chineseMedicineMsg.setMinor(recipeExtend.getMinor());
            chineseMedicineMsg.setUseDays(recipeDetails.get(0).getUseDays());
            drugUsageLabelResp.setChineseMedicineMsg(chineseMedicineMsg);
        }
        return drugUsageLabelResp;
    }

    @Override
    public List<RecipeInfoVO> findRelatedRecipeRecordByRegisterNo(Integer recipeId, Integer doctorId,
                                                                  List<Integer> recipeTypeList, List<Integer> organIds) {
        List<RecipeInfoVO> recipeInfoVOS = new ArrayList<>();
        Recipe recipe = recipeDAO.get(recipeId);
        String mpiId = recipe.getMpiid();
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        String registerId = recipeExtend.getRegisterID();
        if (StringUtils.isBlank(registerId)) {
            return recipeInfoVOS;
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndRegisterIdAndOrganId(registerId,
                recipe.getClinicOrgan());
        if (CollectionUtils.isNotEmpty(recipeList)) {
            logger.info("findRelatedRecipeRecordByRegisterNo recipeList={}", JSON.toJSONString(recipeList));
            for (Recipe re : recipeList) {
                //只展示有审核权限的处方类型
                boolean recipeTypeFlag = recipeTypeList.contains(re.getRecipeType());
                //只展示平台审核单子
                boolean checkMode = Integer.valueOf(1).equals(re.getCheckMode());
                if (recipeId.equals(re.getRecipeId()) || !mpiId.equals(re.getMpiid()) ||
                        !checkMode || !organIds.contains(re.getClinicOrgan()) || !recipeTypeFlag) {
                    continue;
                }
                List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(re.getRecipeId());
                RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
                recipeInfoVO.setRecipeBean(ObjectCopyUtils.convert(re, RecipeBean.class));
                recipeInfoVO.setRecipeDetails(ObjectCopyUtils.convert(recipeDetailList, RecipeDetailBean.class));
                recipeInfoVOS.add(recipeInfoVO);
            }
        }
        return recipeInfoVOS;
    }

    @Override
    public List<DrugUsageLabelResp> queryRecipeDrugUsageLabelByOrder(Integer orderId) {
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIdList);
        Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, List<RecipeExtend>> recipeExtendsMap = recipeExtends.stream().collect(Collectors.groupingBy(RecipeExtend::getRecipeId));
        Integer enterpriseId = recipeOrder.getEnterpriseId();

        if (Objects.isNull(enterpriseId)) {
            throw new DAOException("未查询到对应药企！");
        }
        DrugsEnterprise drugsEnterprise = enterpriseManager.drugsEnterprise(enterpriseId);
        PatientDTO patientDTO = patientClient.getPatientDTOByMpiId(recipeOrder.getMpiId());
        List<DrugUsageLabelResp> deploy = recipeList.stream().map(recipe -> {
            DrugUsageLabelResp drugUsageLabelResp = new DrugUsageLabelResp();
            drugUsageLabelResp.setEnterpriseName(drugsEnterprise.getName());
            //患者信息
            if (Objects.nonNull(patientDTO)) {
                drugUsageLabelResp.setPatientName(patientDTO.getPatientName());
                drugUsageLabelResp.setPatientAge(patientDTO.getAgeString());
                drugUsageLabelResp.setPatientSex(patientDTO.getPatientSex());
            }
            drugUsageLabelResp.setRecipeType(recipe.getRecipeType());
            drugUsageLabelResp.setDispensingTime(recipeOrder.getDispensingTime());
            if(MapUtils.isEmpty(recipeDetailMap) || CollectionUtils.isEmpty(recipeDetailMap.get(recipe.getRecipeId()))){
                throw new DAOException("药品信息为空！");
            }
            List<Recipedetail> recipedetails = recipeDetailMap.get(recipe.getRecipeId());
            if (RecipeTypeEnum.RECIPETYPE_WM.getType().equals(recipe.getRecipeType()) ||
                    RecipeTypeEnum.RECIPETYPE_CPM.getType().equals(recipe.getRecipeType())) {
                //西药，中成药
                // 是否医院结算药企
                Boolean isHosSettle = enterpriseManager.getIsHosSettle(recipeOrder);
                if (CollectionUtils.isNotEmpty(recipedetails)) {
                    List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);
                    for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                        if (Objects.nonNull(recipeDetailBean.getHisReturnSalePrice()) && isHosSettle) {
                            recipeDetailBean.setActualSalePrice(recipeDetailBean.getHisReturnSalePrice());
                        }
                    }
                    drugUsageLabelResp.setDrugUsageLabelList(recipeDetailBeans);
                }
            } else if (RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType()) ||
                    RecipeTypeEnum.RECIPETYPE_HP.getType().equals(recipe.getRecipeType())) {
                //中药, 膏方
                ChineseMedicineMsgVO chineseMedicineMsg = new ChineseMedicineMsgVO();
                chineseMedicineMsg.setCopyNum(recipe.getCopyNum());
                chineseMedicineMsg.setTotalFee(recipeOrder.getTotalFee());
                if (MapUtils.isNotEmpty(recipeExtendsMap) && CollectionUtils.isNotEmpty(recipeExtendsMap.get(recipe.getRecipeId()))) {
                    List<RecipeExtend> recipeExtends1 = recipeExtendsMap.get(recipe.getRecipeId());
                    chineseMedicineMsg.setMakeMethodText(recipeExtends1.get(0).getMakeMethodText());
                    chineseMedicineMsg.setDecoctionText(recipeExtends1.get(0).getDecoctionText());

                    Boolean ecoctionFlag;
                    String decoctionDeploy = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "decoctionDeploy", "0");
                    if ("0".equals(decoctionDeploy)) {
                        ecoctionFlag = false;
                    } else if ("1".equals(decoctionDeploy)) {
                        ecoctionFlag = "1".equals(recipeExtends1.get(0).getDoctorIsDecoction());
                    } else {
                        ecoctionFlag = "1".equals(recipeOrder.getPatientIsDecoction());
                    }
                    chineseMedicineMsg.setJuice(recipeExtends1.get(0).getJuice());

                    chineseMedicineMsg.setDecoctionFlag(ecoctionFlag);
                    if (CollectionUtils.isNotEmpty(recipedetails)) {
                        chineseMedicineMsg.setUsePathways(recipedetails.get(0).getUsePathways());
                        chineseMedicineMsg.setUsingRate(recipedetails.get(0).getUsingRate());
                        chineseMedicineMsg.setMemo(recipe.getRecipeMemo());
                    }
                    chineseMedicineMsg.setMinor(recipeExtends1.get(0).getMinor());
                    chineseMedicineMsg.setUseDays(recipedetails.get(0).getUseDays());
                    drugUsageLabelResp.setChineseMedicineMsg(chineseMedicineMsg);
                }
            }
            return drugUsageLabelResp;
        }).collect(Collectors.toList());

        return deploy;
    }

    @Override
    public AdvanceWarningResVO getAdvanceWarning(AdvanceWarningReqVO advanceWarningReqVO) {
        AdvanceWarningResDTO advanceWarningResDTO = recipeManager.getAdvanceWarning(ObjectCopyUtils.convert(advanceWarningReqVO, AdvanceWarningReqDTO.class));
        return ObjectCopyUtils.convert(advanceWarningResDTO, AdvanceWarningResVO.class);
    }

    @Override
    public void pharmacyToRecipePDF(Integer recipeId) {
        createPdfFactory.updateCheckNamePdf(recipeId);
    }

    @Override
    public void pharmacyToRecipePDFAndCa(Integer recipeId, Integer checker) {
        createPdfFactory.updateCheckNamePdfESign(recipeId);
    }

    @Override
    public List<Map<String, Object>> findRecipeDetailsByOrderCode(String orderCode) {
        List<Recipe> recipeOrderList = recipeDAO.findRecipeByOrderCode(orderCode);
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(recipeOrderList)) {
            List<Integer> recipeIdList = recipeOrderList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
            recipeIdList.forEach(recipeId -> {
                Map<String, Object> map = remoteRecipeService.findRecipeAndDetailsAndCheckById(recipeId);
                mapList.add(map);
            });

        }
        return mapList;
    }


    @Override
    public List<Recipe> findRecipeByMpiidAndrecipeStatus(String mpiid, List<Integer> recipeStatus, Integer terminalType, Integer organId) {
        return recipeDAO.findRecipeByMpiidAndrecipeStatus(mpiid,recipeStatus,terminalType,organId);
    }
}

