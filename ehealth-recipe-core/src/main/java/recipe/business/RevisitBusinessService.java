package recipe.business;

import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.RecipeCancelDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeStatusConstant;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import easypay.entity.po.AccountResult;
import eh.recipeaudit.model.RecipeCheckBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DoctorClient;
import recipe.client.IConfigurationClient;
import recipe.client.RecipeAuditClient;
import recipe.core.api.IRevisitBusinessService;
import recipe.dao.*;
import recipe.easypay.IEasyPayService;
import recipe.manager.OrderManager;
import recipe.manager.RecipeManager;
import recipe.manager.RevisitManager;
import recipe.manager.SignManager;
import recipe.util.ValidateUtil;
import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 处方复诊 核心处理类
 *
 * @Author liumin
 * @Date 2021/8/24 上午11:58
 * @Description
 */
@Service
public class RevisitBusinessService extends BaseService implements IRevisitBusinessService {
    private final String UNDERWAY_REVISIT_NO = "1";
    private final String UNDERWAY_REVISIT_REGISTER_ID = "3";

    @Autowired
    private RevisitManager revisitManager;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private SignManager signManager;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    @Autowired
    private IEasyPayService iEasyPayService;
    @Autowired
    private IConfigurationClient configurationClient;


    @Override
    public List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer recipeId, Integer clinicId) {
        logger.info("RecipeBusinessService revisitRecipeTrace recipeId={},clinicID={}", recipeId, clinicId);
        List<RevisitRecipeTraceVo> revisitRecipeTraceVos = new ArrayList<>();
        List<Recipe> recipes = new ArrayList<>();
        if (null == recipeId) {
            recipes = recipeDAO.findByClinicId(clinicId);
        } else {
            recipes = recipeDAO.findRecipeByRecipeId(recipeId);
        }
        if (CollectionUtils.isEmpty(recipes)) {
            return null;
        }
        List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).distinct().collect(Collectors.toList());
        List<String> orderCodes = recipes.stream().map(Recipe::getOrderCode).distinct().collect(Collectors.toList());
        List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIds(recipeIds);
        List<RecipeOrder> orders = recipeOrderDAO.findByOrderCode(orderCodes);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtends.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, Function.identity(), (key1, key2) -> key2));
        Map<Integer, List<Recipedetail>> recipeDetailsMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        Map<String, RecipeOrder> ordersMap = orders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, Function.identity(), (key1, key2) -> key2));
        recipes.forEach(recipe -> {
                    //医生开方
                    RevisitRecipeTraceVo revisitRecipeTraceVo = new RevisitRecipeTraceVo();
                    RevisitRecipeTraceVo.Recipe innerRecipe = new RevisitRecipeTraceVo.Recipe();
                    BeanUtils.copy(recipe, innerRecipe);
                    if (recipeExtendMap != null && recipeExtendMap.get(recipe.getRecipeId()) != null) {
                        BeanUtils.copy(recipeExtendMap.get(recipe.getRecipeId()), innerRecipe);
                    }
                    ApothecaryDTO apothecaryDTO = signManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getRecipeId());
                    innerRecipe.setDoctorSign(apothecaryDTO.getDoctorSignImg());
                    if (new Integer(2).equals(recipe.getRecipeSourceType())) {
                        innerRecipe.setFromflag(0);
                    } else {
                        innerRecipe.setFromflag(1);
                    }
                    revisitRecipeTraceVo.setRecipe(innerRecipe);
                    //Rp
                    obtainRevisitTraceRecipeDetailInfo(revisitRecipeTraceVo, recipeDetailsMap, recipe, recipeDetails);
                    //审方药师审核
                    RevisitRecipeTraceVo.AuditCheck innerAudit = new RevisitRecipeTraceVo.AuditCheck();
                    RecipeCheckBean recipeCheck = recipeAuditClient.getByRecipeId(recipe.getRecipeId());
                    if (RecipeStatusConstant.READY_CHECK_YS != recipe.getStatus() && recipeCheck != null) {
                        BeanUtils.copy(recipeCheck, innerAudit);
                        DoctorDTO doctor = new DoctorDTO();
                        try {
                            doctor = doctorClient.getDoctor(recipeCheck.getChecker());
                            if (doctor != null) {
                                innerAudit.setCheckIdCard(doctor.getIdNumber());
                            }
                        } catch (Exception e) {
                            logger.warn("revisitRecipeTrace get doctor error. doctorId={}", recipeCheck.getChecker(), e);
                        }
                        innerAudit.setCheckSign(apothecaryDTO.getCheckerSignImg());
                        revisitRecipeTraceVo.setAuditCheck(innerAudit);
                        List<Map<String, Object>> mapList = recipeManager.getCheckNotPassDetail(recipe);
                        revisitRecipeTraceVo.setReasonAndDetails(mapList);
                    }

                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        RecipeOrder recipeOrder = ordersMap.get(recipe.getOrderCode());
                        if (recipeOrder != null) {
                            List<AccountResult> refundFeeNumbers = new ArrayList<>();
                            List<AccountResult> payFeeNumbers = new ArrayList<>();
                            if (StringUtils.isNotEmpty(recipeOrder.getOutTradeNo())) {
                                List<AccountResult> accountResults = iEasyPayService.queryPaymentDetailByApplyNo(recipeOrder.getOutTradeNo());
                                if (CollectionUtils.isNotEmpty(accountResults)) {
                                    refundFeeNumbers = accountResults.stream().filter(AccountResult -> "2".equals(AccountResult.getTradeStatus())).collect(Collectors.toList());
                                    payFeeNumbers = accountResults.stream().filter(AccountResult -> "1".equals(AccountResult.getTradeStatus())).collect(Collectors.toList());
                                }
                            }
                            //患者购药
                            obtainOrder(revisitRecipeTraceVo, recipeOrder, payFeeNumbers);
                            //物流 药企发药
                            RevisitRecipeTraceVo.Logistics logistics = new RevisitRecipeTraceVo.Logistics();
                            BeanUtils.copy(recipeOrder, logistics);
                            revisitRecipeTraceVo.setLogistics(logistics);
                            //发药药师审核
                            RevisitRecipeTraceVo.GiveUser giveUser = new RevisitRecipeTraceVo.GiveUser();
                            ApothecaryDTO apothecaryDTO2 = doctorClient.getGiveUserDefault(recipe);
                            if (apothecaryDTO2 != null) {
                                if (null != recipeOrder.getDispensingTime()) {
                                    BeanUtils.copy(apothecaryDTO2, giveUser);
                                    giveUser.setDispensingTime(recipeOrder.getDispensingTime());
                                    revisitRecipeTraceVo.setGiveUser(giveUser);
                                }
                            }
                            //退费
                            obtainRecipeRefund(revisitRecipeTraceVo, refundFeeNumbers, recipeId, recipeExtendMap);

                        }
                    }

                    //医生撤销
                    RecipeCancelDTO recipeCancel = recipeManager.getCancelReasonForPatient(recipe.getRecipeId());
                    if (recipeCancel != null) {
                        RevisitRecipeTraceVo.RecipeCancel innerRecipeCancel = new RevisitRecipeTraceVo.RecipeCancel();
                        BeanUtils.copy(recipeCancel, innerRecipeCancel);
                        revisitRecipeTraceVo.setRecipeCancel(innerRecipeCancel);
                    }
                    //智能审方 暂时先不做
//                    List<AuditMedicinesBean> auditMedicinesBeans = recipeAuditClient.getAuditMedicineIssuesByRecipeId(recipeId);
                    revisitRecipeTraceVos.add(revisitRecipeTraceVo);
                }
        );
        logger.info("RecipeBusinessService revisitRecipeTraceVos res:{}", JSONUtils.toString(revisitRecipeTraceVos));
        return revisitRecipeTraceVos;
    }


    @Override
    public void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId) {
        logger.info("handDealRevisitTraceRecipe start");
        List<Recipe> recipes = recipeDAO.queryRevisitTrace(startTime, endTime, recipeIds, organId);
        recipes.forEach(recipe -> revisitManager.saveRevisitTracesList(recipe));
        logger.info("handDealRevisitTraceRecipe end");
    }

    @Override
    public Boolean revisitValidate(Recipe recipe) {
        //开具处方时复诊状态判断配置
        String isUnderwayRevisit = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "isUnderwayRevisit", UNDERWAY_REVISIT_NO);
        if (UNDERWAY_REVISIT_NO.equals(isUnderwayRevisit)) {
            return true;
        }
        Integer revisitId = revisitManager.getRevisitId(recipe.getMpiid(), recipe.getDoctor(), UNDERWAY_REVISIT_REGISTER_ID.equals(isUnderwayRevisit));
        return !ValidateUtil.integerIsEmpty(revisitId);
    }

    @Override
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) throws Exception {
        logger.info("findWriteDrugRecipeByRevisitFromHis start");
        List<WriteDrugRecipeDTO> result = null;//revisitManager.findWriteDrugRecipeByRevisitFromHis(mpiId, organId, doctorId);
        logger.info("findWriteDrugRecipeByRevisitFromHis end");
        return result;
    }

    /**
     * 获取追溯平台处方退费信息
     *
     * @param revisitRecipeTraceVo
     * @param refundFeeNumbers
     * @param recipeId
     * @param recipeExtendMap
     */
    private void obtainRecipeRefund(RevisitRecipeTraceVo revisitRecipeTraceVo, List<AccountResult> refundFeeNumbers, Integer recipeId, Map<Integer, RecipeExtend> recipeExtendMap) {
        try {
            List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRefundListByRecipeId(recipeId);
            if (CollectionUtils.isNotEmpty(recipeRefunds)) {
                RecipeRefund recipeRefund = new RecipeRefund();
                recipeRefund = recipeRefunds.get(0);
                if (recipeRefund != null) {
                    RevisitRecipeTraceVo.RecipeRefund innerRecipeRefund = new RevisitRecipeTraceVo.RecipeRefund();
                    BeanUtils.copy(recipeRefund, innerRecipeRefund);
                    innerRecipeRefund.setRefundNodeStatus(recipeExtendMap.get(recipeId).getRefundNodeStatus());
                    if (CollectionUtils.isNotEmpty(refundFeeNumbers)) {
                        AccountResult accountResult = refundFeeNumbers.get(0);
                        if (accountResult != null) {
                            innerRecipeRefund.setTradeNo(accountResult.getRefundId());
                        }
                    }
                    revisitRecipeTraceVo.setRecipeRefund(innerRecipeRefund);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("obtainRecipeRefund error");
        }
    }

    /**
     * 获取患者够药模块数据
     *
     * @param revisitRecipeTraceVo
     * @param recipeOrder
     * @param payFeeNumbers
     */
    private void obtainOrder(RevisitRecipeTraceVo revisitRecipeTraceVo, RecipeOrder recipeOrder, List<AccountResult> payFeeNumbers) {
        RevisitRecipeTraceVo.Order order = new RevisitRecipeTraceVo.Order();
        BeanUtils.copy(recipeOrder, order);
        String address = orderManager.getCompleteAddress(recipeOrder);
        order.setAddress(address);
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipeOrder.getOrderCode());
        if (recipeOrderBill != null) {
            order.setBillPictureUrl(recipeOrderBill.getBillPictureUrl());
        }
        //55074
        if (CollectionUtils.isNotEmpty(payFeeNumbers)) {
            AccountResult accountResult = payFeeNumbers.get(0);
            if (accountResult != null) {
                order.setPreSettleTotalAmount(accountResult.getAmount());
                order.setFundAmount(accountResult.getMedicalAmount());
                try {
                    order.setCashAmount((Double.valueOf(accountResult.getAmount()) - Double.valueOf(accountResult.getMedicalAmount())) + "");
                } catch (NumberFormatException e) {
                    logger.info("obtainOrder e", e);
                    e.printStackTrace();
                }
            }
        }
        revisitRecipeTraceVo.setOrder(order);
        logger.info("obtainOrder revisitRecipeTraceVo:{}", JSONUtils.toString(revisitRecipeTraceVo));

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
}
