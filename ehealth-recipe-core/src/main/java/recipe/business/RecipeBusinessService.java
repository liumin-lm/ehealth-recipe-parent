package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.PatientInfoDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.bussutil.RecipeValidateUtil;
import recipe.client.DoctorClient;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.OrderManager;
import recipe.manager.RecipeManager;
import recipe.manager.SignManager;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.ChinaIDNumberUtil;
import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private RecipeDetailDAO recipeDetailDAO;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    @Autowired
    private OfflineRecipeClient offlineRecipeClient;

    @Autowired
    private RemoteRecipeService remoteRecipeService;

    @Autowired
    private PatientClient patientClient;

    @Autowired
    private SignManager signManager;

    @Autowired
    private IRecipeCheckService recipeCheckService;

    @Autowired
    private IRecipeAuditService recipeAuditService;

    @Autowired
    private DoctorClient doctorClient;

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;

    @Autowired
    private RecipeManager recipeManager;


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
     * 复诊处方追溯
     *
     * @param bussSource 处方来源
     * @param clinicId   业务id
     * @return
     */
    @Override
    public List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer bussSource, Integer clinicId) {
        logger.info("RecipeBusinessService revisitRecipeTrace bussSource={},clinicID={}", bussSource, clinicId);
        List<RevisitRecipeTraceVo> revisitRecipeTraceVos = new ArrayList<>();
        List<Recipe> recipes = recipeDAO.getByClinicId(clinicId);
        List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).distinct().collect(Collectors.toList());
        List<String> orderCodes = recipes.stream().map(Recipe::getOrderCode).distinct().collect(Collectors.toList());
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIds(recipeIds);
        List<RecipeOrder> orders = recipeOrderDAO.findByOrderCode(orderCodes);
        Map<Integer, List<Recipedetail>> recipeDetailsMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        Map<String, RecipeOrder> ordersMap = orders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, Function.identity(), (key1, key2) -> key2));
        recipes.forEach(recipe -> {
                    //医生开方
                    RevisitRecipeTraceVo revisitRecipeTraceVo = new RevisitRecipeTraceVo();
                    RevisitRecipeTraceVo.Recipe innerRecipe = new RevisitRecipeTraceVo.Recipe();
                    BeanUtils.copy(recipe, innerRecipe);
                    ApothecaryDTO apothecaryDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getRecipeId());
                    innerRecipe.setDoctorSign(apothecaryDTO.getDoctorSignImg());
                    revisitRecipeTraceVo.setRecipe(innerRecipe);

                    //Rp
                    obtainRevisitTraceRecipeDetailInfo(revisitRecipeTraceVo, recipeDetailsMap, recipe, recipeDetails);
                    //审方药师审核
                    RevisitRecipeTraceVo.AuditCheck innerAudit = new RevisitRecipeTraceVo.AuditCheck();
                    RecipeCheckBean recipeCheck = recipeCheckService.getByRecipeId(recipe.getRecipeId());
                    DoctorDTO doctor = new DoctorDTO();
                    try {
                        doctor = doctorClient.getDoctor(recipeCheck.getChecker());
                        if (doctor != null) {
                            innerAudit.setCheckIdCard(doctor.getIdNumber());
                        }
                    } catch (Exception e) {
                        logger.warn("revisitRecipeTrace get doctor error. doctorId={}", recipeCheck.getChecker(), e);
                    }
                    innerAudit.setCheckSign(apothecaryDTO.getDoctorSignImg());
                    revisitRecipeTraceVo.setAuditCheck(innerAudit);
                    obtainCheckNotPassDetail(revisitRecipeTraceVo, recipe);
                    //发药药师审核
                    RevisitRecipeTraceVo.GiveUser giveUser = new RevisitRecipeTraceVo.GiveUser();
                    //获取运营平台发药药师
                    ApothecaryDTO apothecaryDTO2 = doctorClient.getGiveUserDefault(recipe);
                    BeanUtils.copy(apothecaryDTO2, giveUser);
                    revisitRecipeTraceVo.setGiveUser(giveUser);
                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        //患者购药
                        RevisitRecipeTraceVo.Order order = new RevisitRecipeTraceVo.Order();
                        BeanUtils.copy(ordersMap.get(recipe.getOrderCode()), order);
                        String address = orderManager.getCompleteAddress(ordersMap.get(recipe.getOrderCode()));
                        order.setAddress(address);
                        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
                        if (recipeOrderBill != null) {
                            order.setBillPictureUrl(recipeOrderBill.getBillPictureUrl());
                        }
                        revisitRecipeTraceVo.setOrder(order);
                        //物流 药企发药
                        RevisitRecipeTraceVo.Logistics logistics = new RevisitRecipeTraceVo.Logistics();
                        BeanUtils.copy(ordersMap.get(recipe.getOrderCode()), logistics);
                        revisitRecipeTraceVo.setLogistics(logistics);
                    }
                    //医生撤销
                    RecipeCancel recipeCancel = recipeManager.getCancelReasonForPatient(recipe.getRecipeId());
                    recipe.vo.second.RecipeCancel recipeCancel1 = new recipe.vo.second.RecipeCancel();
                    BeanUtils.copy(recipeCancel, recipeCancel1);
                    revisitRecipeTraceVo.setRecipeCancel(recipeCancel1);
                    revisitRecipeTraceVos.add(revisitRecipeTraceVo);
                }
        );
        logger.info("RecipeBusinessService revisitRecipeTraceVos res:{}", JSONUtils.toString(revisitRecipeTraceVos));
        return revisitRecipeTraceVos;
    }

    /**
     * 获取复诊处方追溯--处方详情
     *
     * @param revisitRecipeTraceVo
     * @param recipeDetailsMap
     * @param recipe
     * @param recipeDetails
     */
    private void obtainRevisitTraceRecipeDetailInfo(RevisitRecipeTraceVo revisitRecipeTraceVo, Map<Integer, List<Recipedetail>> recipeDetailsMap, Recipe recipe, List<Recipedetail> recipeDetails) {
        logger.info("RecipeBusinessService obtainRevisitTraceRecipeDetailInfo param:[{},{},{},{}]", JSONUtils.toString(revisitRecipeTraceVo), JSONUtils.toString(recipeDetailsMap), JSONUtils.toString(recipe), JSONUtils.toString(recipeDetails));
        try {
            List<Recipedetail> recipedetails = recipeDetailsMap.get(recipe.getRecipeId());
            if (CollectionUtils.isNotEmpty(recipeDetails)) {
                recipedetails.forEach(recipedetail -> {
                    try {
                        Integer organId = recipe.getClinicOrgan();
                        Integer drugId = recipedetail.getDrugId();
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
                        if (CollectionUtils.isNotEmpty(organDrugLists)) {
                            recipedetail.setDrugForm(organDrugLists.get(0).getDrugForm());
                        }
                    } catch (Exception e) {
                        logger.info("obtainRevisitTraceRecipeDetailInfo error recipe:{},{}.", JSONUtils.toString(recipe), e.getMessage(), e);
                    }
                });
                revisitRecipeTraceVo.setDetailData(ObjectCopyUtils.convert(recipeDetails, RecipeDetailBean.class));
            }
        } catch (Exception e) {
            logger.error("obtainRevisitTraceRecipeDetailInfo error e:{}", e);
            e.printStackTrace();
        }
        logger.info("RecipeBusinessService obtainRevisitTraceRecipeDetailInfo res:{}", JSONUtils.toString(revisitRecipeTraceVo));
    }

    /**
     * 获取审方不通过详情
     *
     * @param revisitRecipeTraceVo
     * @param recipe
     */
    private void obtainCheckNotPassDetail(RevisitRecipeTraceVo revisitRecipeTraceVo, Recipe recipe) {
        logger.info("RecipeBusinessService obtainCheckNotPassDetail param:[{},{}]", JSONUtils.toString(revisitRecipeTraceVo), JSONUtils.toString(recipe));
        //获取审核不通过详情
        List<Map<String, Object>> mapList = recipeAuditService.getCheckNotPassDetail(recipe.getRecipeId());
        if (!ObjectUtils.isEmpty(mapList)) {
            for (int i = 0; i < mapList.size(); i++) {
                Map<String, Object> notPassMap = mapList.get(i);
                List results = (List) notPassMap.get("checkNotPassDetails");
                List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtil.convert(results, RecipeDetailBean.class);
                try {
                    for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                        RecipeValidateUtil.setUsingRateIdAndUsePathwaysId(recipe, recipeDetailBean);
                    }
                } catch (Exception e) {
                    logger.error("RecipeServiceSub  setUsingRateIdAndUsePathwaysId error", e);
                }
            }
        }
        revisitRecipeTraceVo.setReasonAndDetails(mapList);
        logger.info("RecipeBusinessService obtainCheckNotPassDetail res:{}", JSONUtils.toString(revisitRecipeTraceVo));
    }

}

