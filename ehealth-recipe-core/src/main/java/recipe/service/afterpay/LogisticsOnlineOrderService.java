package recipe.service.afterpay;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.infra.logistics.mode.CreateLogisticsOrderDto;
import com.ngari.infra.logistics.mode.WriteBackLogisticsOrderDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.infra.logistics.service.IWaybillService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.bean.ThirdResultBean;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.type.ExpressFeePayWayEnum;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.util.AddressUtils;

import java.util.List;
import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * 物流自动下单业务
 * @author yinsheng
 * @date 2021\4\13 0013 09:15
 */
@Component("logisticsOnlineOrderService")
public class LogisticsOnlineOrderService implements IAfterPayBussService{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsOnlineOrderService.class);

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    /**
     * 根据支付结果进行物流下单
     * @param order        订单信息
     * @param recipes      处方列表
     */
    public void onlineOrder(RecipeOrder order, List<Recipe> recipes) {
        LOGGER.info("LogisticsOnlineOrderService onlineOrder:[{}]", order.getOrderCode());
        // 平台物流对接--物流下单逻辑--且处方购药方式为配送到家
        try {
            if (GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType().equals(recipes.get(0).getGiveMode())) {
                LOGGER.info("基础服务物流下单,支付回调订单信息={}", JSONObject.toJSONString(order));
                createLogisticsOrder(order, recipes);
            }
        } catch (Exception e) {
            LOGGER.error("基础服务物流下单.error=", e);
        }
    }

    /**
     * 包装物流信息，进行下单处理
     * @param order    订单信息
     * @param recipeS  处方信息
     */
    private void createLogisticsOrder(RecipeOrder order, List<Recipe> recipeS) {
        // 获取处方药企物流对接方式-仅平台对接物流方式走基础服务物流下单流程
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
        if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_PLATFORM)) {
            String trackingNumber;
            try {
                ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
                CreateLogisticsOrderDto logisticsOrder = getCreateLogisticsOrderDto(order, recipeS.get(0), enterprise);
                LOGGER.info("基础服务物流下单入参={}", JSONObject.toJSONString(logisticsOrder));
                trackingNumber = logisticsOrderService.addLogisticsOrder(logisticsOrder);
            } catch (Exception e) {
                LOGGER.error("基础服务物流下单异常，发起退款流程 orderId={}，异常=", order.getOrderId(), e);
                return;
            }
            LOGGER.info("基础服务物流下单结果={}", trackingNumber);
            if (StringUtils.isNotBlank(trackingNumber)) {
                //更新支付支付方式为线上支付和快递费用
                updatePayPlatStatus(order, trackingNumber);
                for (int i = 0; i < recipeS.size(); i++) {
                    Recipe recipe = recipeS.get(i);
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "基础服务物流下单成功");
                    // 修改状态为待配送
                    Map<String, Object> paramMap = new HashedMap();
                    paramMap.put("recipeId", recipe.getRecipeId());
                    ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
                    ThirdResultBean resultBean = callService.readyToSend(paramMap);
                    LOGGER.info("基础服务物流下单成功,修改状态为待配送修改参数={},修改结果={}", paramMap, JSONObject.toJSONString(resultBean));
                }

                // 下单成功更新物流单号、物流公司
                Map<String, Object> orderAttrMap = new HashedMap();
                orderAttrMap.put("LogisticsCompany", order.getLogisticsCompany());

                orderAttrMap.put("TrackingNumber", trackingNumber);
                recipeOrderDAO.updateByOrdeCode(order.getOrderCode(), orderAttrMap);
                RecipeMsgService.batchSendMsg(recipeS.get(0).getRecipeId(), RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
                LOGGER.info("基础服务物流下单成功，更新物流单号={},物流公司={},orderId={}", trackingNumber, order.getLogisticsCompany(), order.getOrderId());
            } else {
                // 下单失败发起退款，退款原因=物流下单失败
                LOGGER.info("基础服务物流下单失败，发起退款流程 orderId={}", order.getOrderId());
            }
        } else if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_ENT_HIS)) {
            //药企对接-无回写接口:将处方信息传给基础服务线
            ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
            WriteBackLogisticsOrderDto orderDto = getWriteBackLogisticsOrderDto(order, recipeS.get(0));
            LOGGER.info("基础服务物流下单入参 req={}", JSONUtils.toString(orderDto));
            String res = logisticsOrderService.writeBackLogisticsOrder(orderDto);
            LOGGER.info("基础服务物流下单结果 res={}", res);
        }
    }

    private void updatePayPlatStatus(RecipeOrder order, String trackingNumber) {
        LOGGER.info("基础物流更新 order:{}, trackingNumber:{}.", order.getOrderId(), trackingNumber);
        try {
            //将物流支付状态,物流费同步到基础平台
            IWaybillService waybillService = AppContextHolder.getBean("infra.waybillService", IWaybillService.class);
            if (ExpressFeePayWayEnum.ONLINE.getType().equals(order.getExpressFeePayWay())) {
                LOGGER.info("基础物流更新快递单号：{}的支付支付方式为线上支付和快递费用：{}", trackingNumber, order.getExpressFee());
                waybillService.updatePayplatStatus(trackingNumber, 1, order.getExpressFee());
            } else {
                LOGGER.info("基础物流更新快递单号：{}的支付支付方式为线下支付和快递费用：{}", trackingNumber, order.getExpressFee());
                waybillService.updatePayplatStatus(trackingNumber, 0, order.getExpressFee());
            }
        } catch (Exception e) {
            LOGGER.error("基础物流更新付支付方式和快递费用失败", e);
        }
    }

    /**
     * 物流参数包装
     * @param order   订单信息
     * @param recipe  处方信息
     * @return        包装结果
     */
    private WriteBackLogisticsOrderDto getWriteBackLogisticsOrderDto(RecipeOrder order, Recipe recipe) {
        WriteBackLogisticsOrderDto orderDto = new WriteBackLogisticsOrderDto();
        // 机构id
        orderDto.setOrganId(recipe.getClinicOrgan());
        // 业务类型
        orderDto.setBusinessType(DrugEnterpriseConstant.BUSINESS_TYPE);
        // 业务编码
        orderDto.setBusinessNo(order.getOrderCode());
        // 物流公司编码
        orderDto.setLogisticsCode("1003");
        //纳里收件人主键
        orderDto.setUserId(order.getReceiver());
        // 收件人名称
        orderDto.setAddresseeName(order.getReceiver());
        // 收件人手机号
        orderDto.setAddresseePhone(order.getRecMobile());
        // 收件省份
        orderDto.setAddresseeProvince(AddressUtils.getAddressDic(order.getAddress1()));
        // 收件城市
        orderDto.setAddresseeCity(AddressUtils.getAddressDic(order.getAddress2()));
        // 收件镇/区
        orderDto.setAddresseeDistrict(AddressUtils.getAddressDic(order.getAddress3()));
        // 收件人街道
        orderDto.setAddresseeStreet(AddressUtils.getAddressDic(order.getStreetAddress()));
        // 收件详细地址
        orderDto.setAddresseeAddress(order.getAddress4());
        //寄托物名称
        orderDto.setDepositumName(DrugEnterpriseConstant.DEPOSITUM_NAME);
        //运单号
        orderDto.setWaybillNo(recipe.getRecipeCode());
        //运单费用
        orderDto.setWaybillFee(order.getExpressFee());

        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        //门诊号
        orderDto.setOutpatientNumber(recipeExtend.getRegisterID());
        return orderDto;
    }

    /**
     * 物流信息包装
     * @param order       订单信息
     * @param recipe      处方信息
     * @param enterprise  配送药企信息
     * @return            包装后的物流信息
     */
    private CreateLogisticsOrderDto getCreateLogisticsOrderDto(RecipeOrder order, Recipe recipe, DrugsEnterprise enterprise) {
        CreateLogisticsOrderDto logisticsOrder = new CreateLogisticsOrderDto();
        // 机构id
        logisticsOrder.setOrganId(recipe.getClinicOrgan());
        // 平台用户id
        logisticsOrder.setUserId(recipe.getMpiid());
        // 业务类型
        logisticsOrder.setBusinessType(DrugEnterpriseConstant.BUSINESS_TYPE);
        // 业务编码
        logisticsOrder.setBusinessNo(order.getOrderCode());
        // 快递编码
        logisticsOrder.setLogisticsCode(order.getLogisticsCompany() + "");
        // 寄件人姓名
        logisticsOrder.setConsignorName(enterprise.getConsignorName());
        // 寄件人手机号
        logisticsOrder.setConsignorPhone(enterprise.getConsignorMobile());
        // 寄件人省份
        logisticsOrder.setConsignorProvince(AddressUtils.getAddressDic(enterprise.getConsignorProvince()));
        // 寄件人城市
        logisticsOrder.setConsignorCity(AddressUtils.getAddressDic(enterprise.getConsignorCity()));
        // 寄件人区域
        logisticsOrder.setConsignorDistrict(AddressUtils.getAddressDic(enterprise.getConsignorDistrict()));
        // 寄件人街道
        logisticsOrder.setConsignorStreet(AddressUtils.getAddressDic(enterprise.getConsignorStreet()));
        // 寄件人详细地址
        logisticsOrder.setConsignorAddress(enterprise.getConsignorAddress());
        // 收件人名称
        logisticsOrder.setAddresseeName(order.getReceiver());
        // 收件人手机号
        logisticsOrder.setAddresseePhone(order.getRecMobile());
        // 收件省份
        logisticsOrder.setAddresseeProvince(AddressUtils.getAddressDic(order.getAddress1()));
        // 收件城市
        logisticsOrder.setAddresseeCity(AddressUtils.getAddressDic(order.getAddress2()));
        // 收件镇/区
        logisticsOrder.setAddresseeDistrict(AddressUtils.getAddressDic(order.getAddress3()));
        // 收件人街道
        logisticsOrder.setAddresseeStreet(AddressUtils.getAddressDic(order.getStreetAddress()));
        // 收件详细地址
        logisticsOrder.setAddresseeAddress(order.getAddress4());
        // 寄托物名称
        logisticsOrder.setDepositumName(DrugEnterpriseConstant.DEPOSITUM_NAME);
        // 集揽模式
        logisticsOrder.setCollectMode(enterprise.getCollectMode());
        // 就诊人信息
        try {
            IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
            PatientBean patientBean = iPatientService.get(recipe.getMpiid());
            if (patientBean != null) {
                // 就诊人名称
                logisticsOrder.setPatientName(patientBean.getPatientName());
                // 就诊人手机号
                logisticsOrder.setPatientPhone(patientBean.getMobile());
                // 就诊人身份证
                String cardNo = StringUtils.isNotBlank(patientBean.getIdcard()) ? patientBean.getIdcard() : patientBean.getIdcard2();
                if (StringUtils.isNotBlank(cardNo) && cardNo.length() > 18) {
                    cardNo = null;
                }
                logisticsOrder.setPatientIdentityCardNo(cardNo);

            }
            // 挂号序号
            if (recipe.getClinicId() != null) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                if (consultExDTO != null) {
                    logisticsOrder.setOutpatientNumber(consultExDTO.getRegisterNo());
                }
            }
        } catch (Exception e) {
            LOGGER.error("基础服务物流下单非必填信息获取异常：", e);
        }
        return logisticsOrder;
    }
}
