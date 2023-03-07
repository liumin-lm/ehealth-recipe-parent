package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.infra.invoice.mode.InvoiceRecordDto;
import com.ngari.infra.invoice.service.InvoiceRecordService;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.infra.logistics.mode.LogisticsOrderDetailsDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeOrderRefundReqDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderVoNoDS;
import com.ngari.recipe.vo.PatientBeanNoDS;
import ctd.account.UserRoleToken;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.client.IConfigurationClient;
import recipe.client.PatientClient;
import recipe.constant.RecipeRefundRoleConstant;
import recipe.constant.RefundNodeStatusConstant;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.dao.*;
import recipe.enumerate.status.*;
import recipe.enumerate.type.PayFlagEnum;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderFeeManager;
import recipe.manager.OrderManager;
import recipe.manager.RecipeManager;
import recipe.service.RecipeService;
import recipe.util.DateConversion;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.*;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 退费查询接口调用
 *
 * @author ys
 */
@Service
public class OrderFeeService implements IRecipeOrderRefundService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;
    @Autowired
    private OrderFeeManager orderFeeManager;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private InvoiceRecordService invoiceRecordService;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private ILogisticsOrderService logisticsOrderService;


    @Override
    public RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO) {
        RecipeOrderRefundPageVO recipeOrderRefundPageVO = new RecipeOrderRefundPageVO();
        UserRoleToken urt = UserRoleToken.getCurrent();
        String manageUnit = urt.getManageUnit();
        if (!"eh".equals(manageUnit) && !manageUnit.startsWith("yq")) {
            List<Integer> organIds = new ArrayList<>();
            OrganService organService = AppContextHolder.getBean("basic.organService", OrganService.class);
            organIds = organService.queryOrganByManageUnitList(manageUnit, organIds);
            logger.info("RecipeOrderRefundService findRefundRecipeOrder organIds:{}", JSON.toJSONString(organIds));
            recipeOrderRefundReqVO.setOrganIds(organIds);
        }
        if (new Integer("1").equals(recipeOrderRefundReqVO.getDateType())) {
            recipeOrderRefundReqVO.setPayTimeStart(null);
            recipeOrderRefundReqVO.setPayTimeEnd(null);
        } else if (new Integer("2").equals(recipeOrderRefundReqVO.getDateType())) {
            recipeOrderRefundReqVO.setBeginTime(null);
            recipeOrderRefundReqVO.setEndTime(null);
        }
        QueryResult<RecipeOrder> recipeOrderQueryResult =
                orderManager.findRefundRecipeOrder(Objects.requireNonNull(ObjectCopyUtils.convert(recipeOrderRefundReqVO, RecipeOrderRefundReqDTO.class)));
        logger.info("RecipeOrderRefundService findRefundRecipeOrder recipeOrderQueryResult:{}", JSON.toJSONString(recipeOrderQueryResult));
        if (CollectionUtils.isEmpty(recipeOrderQueryResult.getItems())) {
            return recipeOrderRefundPageVO;
        }
        List<RecipeOrder> recipeOrderList = recipeOrderQueryResult.getItems();
        long total = recipeOrderQueryResult.getTotal();
        if (Objects.nonNull(total)) {
            recipeOrderRefundPageVO.setTotal(new Long(total).intValue());
        }
        List<String> orderCodeList = recipeOrderList.stream().map(RecipeOrder::getOrderCode).collect(Collectors.toList());
        List<Integer> depIdList = recipeOrderList.stream().map(RecipeOrder::getEnterpriseId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeDAO.findByOrderCode(orderCodeList);
        Map<String, Recipe> recipeOrderCodeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getOrderCode, a -> a, (k1, k2) -> k1));
        List<Integer> recipeIdList = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(depIdList);
        Map<Integer, DrugsEnterprise> drugsEnterpriseMap = drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId, a -> a, (k1, k2) -> k1));
        List<RecipeOrderRefundVO> recipeOrderRefundVOList = new ArrayList<>();

        recipeOrderList.forEach(recipeOrder -> {
            String appName = configurationClient.getAppName(recipeOrder.getTerminalSource());
            logger.info("findRefundRecipeOrder APP ID={},APP name={}", recipeOrder.getTerminalSource(), appName);
            RecipeOrderRefundVO recipeOrderRefundVO = new RecipeOrderRefundVO();
            recipeOrderRefundVO.setOrderCode(recipeOrder.getOrderCode());
            recipeOrderRefundVO.setActualPrice(recipeOrder.getTotalFee().doubleValue());
            recipeOrderRefundVO.setCreateTime(DateConversion.getDateFormatter(recipeOrder.getCreateTime(), DateConversion.DEFAULT_DATE_TIME));
            if (null != recipeOrder.getEnterpriseId()) {
                if (StringUtils.isNotEmpty(recipeOrder.getDrugStoreName())) {
                    recipeOrderRefundVO.setDepName(recipeOrder.getDrugStoreName());
                } else {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(recipeOrder.getEnterpriseId());
                    if (null != drugsEnterprise) {
                        recipeOrderRefundVO.setDepName(drugsEnterprise.getName());
                        recipeOrderRefundVO.setLogisticsType(drugsEnterprise.getLogisticsType());
                    }
                }
            }
            //发货状态
            recipeOrderRefundVO.setSendStatusText(OrderLogisticsStateEnum.getOrderLogisticsStateName(recipeOrder.getLogisticsState()));
            if (null != recipeOrder.getInvoiceRecordId()) {
                recipeOrderRefundVO.setInvoiceStatus(1);
            } else {
                recipeOrderRefundVO.setInvoiceStatus(0);
            }
            recipeOrderRefundVO.setOrderStatusText(OrderStateEnum.getOrderStateEnum(recipeOrder.getProcessState()).getName());
            recipeOrderRefundVO.setPatientName(recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getPatientName());
            recipeOrderRefundVO.setChannel(appName);
            recipeOrderRefundVO.setPayModeText(PayModeEnum.getPayModeEnumName(recipeOrder.getPayMode()));
            recipeOrderRefundVO.setGiveModeText(recipeOrder.getGiveModeText());
            if (null != recipeOrder.getPayTime()) {
                recipeOrderRefundVO.setPayTime(DateConversion.getDateFormatter(recipeOrder.getPayTime(), DateConversion.DEFAULT_DATE_TIME));
            }
            //订单类型归属
            // 便捷购药订单(=1):便捷购药处方
            // 普通订单(=0)：普通复诊=0/医嘱申请复诊=2/null值/一键续方复诊=3
            Integer fastRecipeFlag = recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getFastRecipeFlag();
            if (Integer.valueOf(1).equals(fastRecipeFlag)) {
                fastRecipeFlag = 1;
            } else {
                fastRecipeFlag = 0;
            }
            recipeOrderRefundVO.setFastRecipeFlag(fastRecipeFlag);


            RecipeExtend recipeExtend = recipeExtendMap.get(recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getRecipeId());
            if (null != recipeExtend) {
                recipeOrderRefundVO.setRefundStatusText(RefundNodeStatusEnum.getRefundStatus(recipeExtend.getRefundNodeStatus()));
            }
            recipeOrderRefundVO.setPrintDrugDistributionListFlag(recipeOrder.getPrintDrugDistributionListFlag());
            recipeOrderRefundVO.setPrintExpressBillFlag(recipeOrder.getPrintExpressBillFlag());
            recipeOrderRefundVO.setLogisticsCompany(recipeOrder.getLogisticsCompany());
            recipeOrderRefundVO.setTrackingNumber(recipeOrder.getTrackingNumber());
            recipeOrderRefundVO.setReceiver(recipeOrder.getReceiver());
            recipeOrderRefundVO.setRecMobile(recipeOrder.getRecMobile());
            recipeOrderRefundVO.setStatus(recipeOrder.getStatus());
            recipeOrderRefundVO.setDispensingApothecaryName(recipeOrder.getDispensingApothecaryName());
            try {
                //查是否可以打印快递面单
                logisticsOrderService.printWaybillByLogisticsOrderNo(1, recipeOrder.getOrderCode());
                recipeOrderRefundVO.setPrintWaybillByLogisticsOrderNo(true);
            } catch (Exception e) {
                recipeOrderRefundVO.setPrintWaybillByLogisticsOrderNo(false);
                logger.error("orderFeeService findRefundRecipeOrder error", e);
            }
            recipeOrderRefundVOList.add(recipeOrderRefundVO);
        });
        recipeOrderRefundPageVO.setRecipeOrderRefundVOList(recipeOrderRefundVOList);
        recipeOrderRefundPageVO.setStart(recipeOrderRefundReqVO.getStart());
        recipeOrderRefundPageVO.setLimit(recipeOrderRefundReqVO.getLimit());
        return recipeOrderRefundPageVO;
    }

    @Override
    public RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode, Integer busType) {
        RecipeOrderRefundDetailVO recipeOrderRefundDetailVO = new RecipeOrderRefundDetailVO();
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (null == recipeOrder) {
            return recipeOrderRefundDetailVO;
        }
        String appName = configurationClient.getAppName(recipeOrder.getTerminalSource());
        RecipeOrderVoNoDS recipeOrderBean = ObjectCopyUtils.convert(recipeOrder, RecipeOrderVoNoDS.class);
        recipeOrderRefundDetailVO.setRecipeOrderBean(recipeOrderBean);
        OrderRefundInfoVO orderRefundInfoVO = new OrderRefundInfoVO();
        if (null != recipeOrderBean.getEnterpriseId()) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrderBean.getEnterpriseId());
            DrugsEnterpriseBean drugsEnterpriseBean = ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
            recipeOrderRefundDetailVO.setDrugsEnterpriseBean(drugsEnterpriseBean);
        }
        PatientDTO patientDTO = patientClient.getPatientDTO(recipeOrder.getMpiId());
        PatientBeanNoDS patientBeanNoDS=ObjectCopyUtils.convert(patientDTO, PatientBeanNoDS.class);
        patientBeanNoDS.setCertificate(null);
        patientBeanNoDS.setIdcard(null);
        patientBeanNoDS.setIdcard2(null);
        patientBeanNoDS.setGuardianCertificate(null);
        patientBeanNoDS.setAddress(null);
        recipeOrderRefundDetailVO.setPatientDTO(patientBeanNoDS);

        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        orderRefundInfoVO.setAuditNodeType(orderFeeManager.getRecipeRefundNode(recipeIdList.get(0), recipeOrder.getOrganId()));
        List<RecipeRefund> recipeRefundList = recipeRefundDAO.findRecipeRefundByRecipeIdAndNodeAndStatus(recipeIdList.get(0), RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_ADMIN);
        List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRefundListByRecipeId(recipeIdList.get(0));
        if (CollectionUtils.isNotEmpty(recipeRefundList)) {
            orderRefundInfoVO.setForceApplyFlag(true);
            orderRefundInfoVO.setAuditNodeType(3);
        }
        if (new Integer(-1).equals(recipeOrder.getPushFlag()) && PayFlagEnum.PAYED.getType().equals(recipeOrder.getPayFlag())) {
            orderRefundInfoVO.setRetryFlag(true);
        }
        List<RecipeRefund> patientRefundList = recipeRefundDAO.findRecipeRefundByRecipeIdAndNodeAndStatus(recipeIdList.get(0), RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_PATIENT);
        if (CollectionUtils.isNotEmpty(patientRefundList)) {
            orderRefundInfoVO.setApplyReason(patientRefundList.get(0).getReason());
            orderRefundInfoVO.setApplyTime(DateConversion.getDateFormatter(patientRefundList.get(0).getApplyTime(), DateConversion.DEFAULT_DATE_TIME));
        }
        if (RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType().equals(recipeOrder.getStatus())) {
            orderRefundInfoVO.setOrderStatusText(RecipeOrderStatusEnum.ORDER_STATUS_READY_GET_DRUG.getName());
        } else {
            orderRefundInfoVO.setOrderStatusText(RecipeOrderStatusEnum.getOrderStatus(recipeOrder.getStatus()));
        }
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
        // 是否医院结算药企
        Boolean isHosSettle = enterpriseManager.getIsHosSettle(recipeOrder);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIdList);
        for (Recipedetail recipedetail : recipeDetailList) {
            if(Objects.nonNull(recipedetail.getHisReturnSalePrice()) && isHosSettle){
                recipedetail.setActualSalePrice(recipedetail.getHisReturnSalePrice());
            }
        }
        if (Objects.nonNull(recipeOrder.getFundAmount()) && recipeOrder.getFundAmount() > 0.0D) {
            recipeOrderBean.setMedicalInsuranceFlag(1);
        } else {
            recipeOrderBean.setMedicalInsuranceFlag(0);
        }
        if (CollectionUtils.isNotEmpty(recipeRefunds)) {
            recipeOrderRefundDetailVO.setRefundOrderFlag(false);
        } else {
            recipeOrderRefundDetailVO.setRefundOrderFlag(recipeOrder.getPayFlag()==1);
        }

        Map<Integer, List<Recipedetail>> detailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        List<RecipeBean> recipeBeanList = new ArrayList<>();
        recipeList.forEach(recipe -> {
            RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
            RecipeExtend recipeExtend = recipeExtendMap.get(recipe.getRecipeId());
            RecipeExtendBean recipeExtendBean = ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class);
            orderRefundInfoVO.setRefundStatusText(RefundNodeStatusEnum.getRefundStatus(recipeExtend.getRefundNodeStatus()));
            orderRefundInfoVO.setRefundNodeStatusText(setRefundNodeStatus(recipeExtend.getRefundNodeStatus()));
            orderRefundInfoVO.setChannel(appName);
            orderRefundInfoVO.setRefundNodeStatus(recipeExtend.getRefundNodeStatus());
            List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(detailMap.get(recipe.getRecipeId()), RecipeDetailBean.class);
            if (new Integer(1).equals(recipeExtend.getRefundNodeStatus()) || new Integer(3).equals(recipeExtend.getRefundNodeStatus())) {
                orderRefundInfoVO.setForceApplyFlag(false);
                orderRefundInfoVO.setAuditNodeType(-1);
            }
            recipeBean.setRecipeExtend(recipeExtendBean);
            recipeBean.setRecipeDetailBeanList(recipeDetailBeans);
            recipeBeanList.add(recipeBean);
        });
        if (CollectionUtils.isEmpty(recipeRefunds)) {
            Map<Integer, List<RecipeRefund>> collect = recipeRefunds.stream().collect(Collectors.groupingBy(RecipeRefund::getStatus));
            List<RecipeRefund> recipeRefunds1 = collect.get(2);
            if (CollectionUtils.isNotEmpty(recipeRefunds1)) {
                orderRefundInfoVO.setRefuseReason(recipeRefunds1.get(0).getReason());
            }
            orderRefundInfoVO.setAuditNodeType(-1);
        }
        if (null != recipeOrder.getInvoiceRecordId()) {
            InvoiceRecordDto invoiceRecordDto = invoiceRecordService.findInvoiceRecordInfo(recipeOrder.getInvoiceRecordId());
            InvoiceRecordVO invoiceRecordVO = new InvoiceRecordVO();
            ObjectCopyUtils.copyProperties(invoiceRecordVO, invoiceRecordDto);
            recipeOrderRefundDetailVO.setInvoiceRecordVO(invoiceRecordVO);
        }
        recipeOrderRefundDetailVO.setOrderRefundInfoVO(orderRefundInfoVO);
        recipeOrderRefundDetailVO.setRecipeBeanList(recipeBeanList);
        //返回发票号
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(orderCode);
        if(recipeOrderBill != null){
            recipeOrderRefundDetailVO.setBillNumber(recipeOrderBill.getBillNumber());
        }
        try {
            //查是否可以打印快递面单
            String logisticsOrderPrintWaybill = logisticsOrderService.printWaybillByLogisticsOrderNo(1, orderCode);
            recipeOrderRefundDetailVO.setLogisticsOrderPrintWaybill(logisticsOrderPrintWaybill);
            recipeOrderRefundDetailVO.setPrintWaybillByLogisticsOrderNo(true);
        }catch (Exception e){
            recipeOrderRefundDetailVO.setPrintWaybillByLogisticsOrderNo(false);
            logger.error("RecipeOrderRefundService getRefundOrderDetail error", e);
        }
        return recipeOrderRefundDetailVO;
    }

    @Override
    public void forceRefund(AuditRefundVO auditRefundVO) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(auditRefundVO.getOrderCode());
        if (null == recipeOrder) {
            throw new DAOException(DAOException.VALUE_NEEDED, "订单不存在");
        }
        Integer refundStatus = 0;
        if (auditRefundVO.getResult() && StringUtils.isNotEmpty(recipeOrder.getOutTradeNo())) {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            recipeService.wxPayRefundForRecipe(4, recipeIdList.get(0), "");
            refundStatus = RefundNodeStatusConstant.REFUND_NODE_SUCCESS_STATUS;
        } else {
            RecipeRefund recipeRefund = new RecipeRefund();
            recipeRefund.setTradeNo(recipeOrder.getTradeNo());
            recipeRefund.setPrice(recipeOrder.getActualPrice());
            recipeRefund.setStatus(2);
            recipeRefund.setNode(RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_THIRD);
            recipeRefund.setReason(auditRefundVO.getReason());
            orderFeeManager.recipeReFundSave(auditRefundVO.getOrderCode(), recipeRefund);
            refundStatus = RefundNodeStatusConstant.REFUND_NODE_NOPASS_AUDIT_STATUS;
        }
        List<Recipe> recipes = orderManager.getRecipesByOrderCode(recipeOrder.getOrderCode());
        recipeManager.updateRecipeRefundStatus(recipes, refundStatus);
    }

    @Override
    public RecipeRefund findApplyRefund(Integer recipeId) {
        List<RecipeRefund> recipeRefundByRecipeIdAndNode = recipeRefundDAO.findRecipeRefundByRecipeIdAndNode(recipeId, -1);
        if (CollectionUtils.isNotEmpty(recipeRefundByRecipeIdAndNode)) {
            return recipeRefundByRecipeIdAndNode.get(0);
        }
        return null;
    }

    @Override
    public void updateRecipePushFlag(List<Integer> recipeIds) {
        recipeDAO.updateRecipePushFlag(recipeIds);
    }

    private String setRefundNodeStatus(Integer status) {
        if (null == status || status == 3 || status == 2) {
            return "未退款";
        }
        if (status == 0 || status == 4) {
            return "退款中";
        }
        return "已退款";
    }
}
