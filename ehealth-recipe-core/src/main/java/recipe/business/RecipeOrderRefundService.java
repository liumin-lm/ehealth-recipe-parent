package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeOrderRefundReqDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.client.PatientClient;
import recipe.constant.RecipeRefundRoleConstant;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.dao.*;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.PayModeEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RefundNodeStatusEnum;
import recipe.manager.OrderManager;
import recipe.manager.RecipeRefundManage;
import recipe.service.RecipeService;
import recipe.util.DateConversion;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 退费查询接口调用
 *
 * @author ys
 */
@Service
public class RecipeOrderRefundService implements IRecipeOrderRefundService {
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
    private RecipeRefundManage recipeRefundManage;

    @Override
    public RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO) {
        RecipeOrderRefundPageVO recipeOrderRefundPageVO = new RecipeOrderRefundPageVO();
        Date beginDate = DateConversion.parseDate(recipeOrderRefundReqVO.getBeginTime(), DateConversion.DEFAULT_DATE_TIME);
        Date endDate = DateConversion.parseDate(recipeOrderRefundReqVO.getEndTime(), DateConversion.DEFAULT_DATE_TIME);
        RecipeOrderRefundReqDTO recipeOrderRefundReqDTO = ObjectCopyUtils.convert(recipeOrderRefundReqVO, RecipeOrderRefundReqDTO.class);
        recipeOrderRefundReqDTO.setBeginTime(beginDate);
        recipeOrderRefundReqDTO.setEndTime(endDate);
        QueryResult<RecipeOrder> recipeOrderQueryResult = orderManager.findRefundRecipeOrder(recipeOrderRefundReqDTO);
        logger.info("RecipeOrderRefundService findRefundRecipeOrder recipeOrderQueryResult:{}", JSON.toJSONString(recipeOrderQueryResult));
        if (CollectionUtils.isEmpty(recipeOrderQueryResult.getItems())) {
            return recipeOrderRefundPageVO;
        }
        List<RecipeOrder> recipeOrderList = recipeOrderQueryResult.getItems();
        long total = recipeOrderQueryResult.getTotal();
        if (null != new Long(total)) {
            recipeOrderRefundPageVO.setTotal(new Long(total).intValue());
        }
        List<String> orderCodeList = recipeOrderList.stream().map(RecipeOrder::getOrderCode).collect(Collectors.toList());
        List<Integer> depIdList = recipeOrderList.stream().map(RecipeOrder::getEnterpriseId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeDAO.findByOrderCode(orderCodeList);
        Map<String, Recipe> recipeOrderCodeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getOrderCode,a->a,(k1,k2)->k1));
        List<Integer> recipeIdList = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a->a,(k1, k2)->k1));
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(depIdList);
        Map<Integer, DrugsEnterprise> drugsEnterpriseMap = drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId,a->a,(k1,k2)->k1));
        List<RecipeOrderRefundVO> recipeOrderRefundVOList = new ArrayList<>();

        recipeOrderList.forEach(recipeOrder -> {
            RecipeOrderRefundVO recipeOrderRefundVO = new RecipeOrderRefundVO();
            recipeOrderRefundVO.setOrderCode(recipeOrder.getOrderCode());
            recipeOrderRefundVO.setActualPrice(recipeOrder.getActualPrice());
            recipeOrderRefundVO.setCreateTime(DateConversion.getDateFormatter(recipeOrder.getCreateTime(), DateConversion.DEFAULT_DATE_TIME));
            if (null != recipeOrder.getEnterpriseId()) {
                if (StringUtils.isNotEmpty(recipeOrder.getDrugStoreName())) {
                    recipeOrderRefundVO.setDepName(recipeOrder.getDrugStoreName());
                } else {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(recipeOrder.getEnterpriseId());
                    if (null != drugsEnterprise) {
                        recipeOrderRefundVO.setDepName(drugsEnterprise.getName());
                    }
                }
            }
            recipeOrderRefundVO.setSendStatusText(RecipeOrderStatusEnum.getOrderStatus(recipeOrder.getStatus()));
            recipeOrderRefundVO.setOrderStatusText(OrderStateEnum.getOrderStateEnum(recipeOrder.getProcessState()).getName());
            recipeOrderRefundVO.setPatientName(recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getPatientName());
            recipeOrderRefundVO.setChannel(patientClient.getClientNameById(recipeOrder.getMpiId()));
            recipeOrderRefundVO.setPayModeText(PayModeEnum.getPayModeEnumName(recipeOrder.getPayMode()));
            recipeOrderRefundVO.setGiveModeText(recipeOrder.getGiveModeText());
            RecipeExtend recipeExtend = recipeExtendMap.get(recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getRecipeId());
            if (null != recipeExtend) {
                recipeOrderRefundVO.setRefundStatusText(RefundNodeStatusEnum.getRefundStatus(recipeExtend.getRefundNodeStatus()));
            }
            recipeOrderRefundVOList.add(recipeOrderRefundVO);
        });
        recipeOrderRefundPageVO.setRecipeOrderRefundVOList(recipeOrderRefundVOList);
        recipeOrderRefundPageVO.setStart(recipeOrderRefundReqVO.getStart());
        recipeOrderRefundPageVO.setLimit(recipeOrderRefundReqVO.getLimit());
        return recipeOrderRefundPageVO;
    }

    @Override
    public RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode) {
        RecipeOrderRefundDetailVO recipeOrderRefundDetailVO = new RecipeOrderRefundDetailVO();
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (null == recipeOrder) {
            return recipeOrderRefundDetailVO;
        }
        RecipeOrderBean recipeOrderBean = ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class);
        recipeOrderRefundDetailVO.setRecipeOrderBean(recipeOrderBean);
        OrderRefundInfoVO orderRefundInfoVO = new OrderRefundInfoVO();
        if (null != recipeOrderBean.getEnterpriseId() && StringUtils.isEmpty(recipeOrderBean.getDrugStoreName())) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrderBean.getEnterpriseId());
            DrugsEnterpriseBean drugsEnterpriseBean = ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
            recipeOrderRefundDetailVO.setDrugsEnterpriseBean(drugsEnterpriseBean);
        }
        PatientDTO patientDTO = patientClient.getPatientDTO(recipeOrder.getMpiId());
        recipeOrderRefundDetailVO.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.patient.dto.PatientDTO.class));
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        orderRefundInfoVO.setAuditNodeType(recipeRefundManage.getRecipeRefundNode(recipeIdList.get(0), recipeOrder.getOrganId()));
        List<RecipeRefund> recipeRefundList = recipeRefundDAO.findRecipeRefundByRecipeIdAndNodeAndStatus(recipeIdList.get(0), RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_ADMIN);
        if (CollectionUtils.isNotEmpty(recipeRefundList)) {
            orderRefundInfoVO.setForceApplyFlag(true);
            orderRefundInfoVO.setAuditNodeType(3);
        }
        if (new Integer(1).equals(recipeOrder.getPushFlag())) {
            orderRefundInfoVO.setRetryFlag(true);
        } else if (new Integer(-1).equals(recipeOrder.getPushFlag())) {
            orderRefundInfoVO.setAuditNodeType(4);
        }
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId,a->a,(k1,k2)->k1));
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIdList);
        Map<Integer, List<Recipedetail>> detailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        List<RecipeBean> recipeBeanList = new ArrayList<>();
        recipeList.forEach(recipe -> {
            RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
            RecipeExtendBean recipeExtendBean = ObjectCopyUtils.convert(recipeExtendMap.get(recipe.getRecipeId()), RecipeExtendBean.class);
            orderRefundInfoVO.setRefundStatusText(RefundNodeStatusEnum.getRefundStatus(recipeExtendMap.get(recipe.getRecipeId()).getRefundNodeStatus()));
            orderRefundInfoVO.setRefundNodeStatusText(setRefundNodeStatus(recipeExtendMap.get(recipe.getRecipeId()).getRefundNodeStatus()));
            orderRefundInfoVO.setChannel(patientClient.getClientNameById(recipe.getMpiid()));
            List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(detailMap.get(recipe.getRecipeId()), RecipeDetailBean.class);
            recipeBean.setRecipeExtend(recipeExtendBean);
            recipeBean.setRecipeDetailBeanList(recipeDetailBeans);
            recipeBeanList.add(recipeBean);
        });
        recipeOrderRefundDetailVO.setOrderRefundInfoVO(orderRefundInfoVO);
        recipeOrderRefundDetailVO.setRecipeBeanList(recipeBeanList);
        return recipeOrderRefundDetailVO;
    }

    @Override
    public void forceRefund(AuditRefundVO auditRefundVO) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(auditRefundVO.getOrderCode());
        if (null == recipeOrder) {
            throw new DAOException(DAOException.VALUE_NEEDED, "订单不存在");
        }
        if (auditRefundVO.getResult() && StringUtils.isNotEmpty(recipeOrder.getOutTradeNo())) {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            recipeService.wxPayRefundForRecipe(4, recipeIdList.get(0), "");
        } else {
            RecipeRefund recipeRefund = new RecipeRefund();
            recipeRefund.setTradeNo(recipeOrder.getTradeNo());
            recipeRefund.setPrice(recipeOrder.getActualPrice());
            recipeRefund.setStatus(2);
            recipeRefund.setNode(RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_THIRD);
            recipeRefund.setReason(auditRefundVO.getReason());
            recipeRefundManage.recipeReFundSave(auditRefundVO.getOrderCode(), recipeRefund);
        }
    }

    private String setRefundNodeStatus(Integer status){
        if (null == status) {
            return "未退款";
        }
        if (status == 1) {
            return  "退款中";
        }
        return "已退款";
    }
}
