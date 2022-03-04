package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import com.ngari.infra.logistics.mode.CreateLogisticsOrderDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.dto.RecipeBeanDTO;
import com.ngari.recipe.dto.RecipeFeeDTO;
import com.ngari.recipe.dto.RecipeOrderDto;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.*;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeOrderPayFlowDao;
import recipe.dao.RecipeParameterDao;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeOrderDetailFeeEnum;
import recipe.util.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单
 *
 * @author yinsheng
 * @date 2021\6\30 0030 15:22
 */
@Service
public class OrderManager extends BaseManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private EnterpriseClient enterpriseClient;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RevisitClient revisitClient;
    @Resource
    private RecipeOrderPayFlowDao recipeOrderPayFlowDao;
    @Resource
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private RefundClient refundClient;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private InfraClient infraClient;

    /**
     * 订单能否配送 物流管控
     *
     * @return
     */
    @LogRecord
    public boolean orderCanSend(Map<String, String> extInfo) {
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        String addressId = MapValueUtil.getString(extInfo, "addressId");
        Integer recipeId = MapValueUtil.getInteger(extInfo, "recipeId");
        Integer logisticsCompany = MapValueUtil.getInteger(extInfo, "logisticsCompany");
        if (Objects.isNull(depId) || Objects.isNull(addressId) || Objects.isNull(recipeId)) {
            logger.info("orderCanSend have null params");
            return true;
        }
        Recipe recipe = recipeDAO.get(recipeId);
        AddressService addressService = AppContextHolder.getBean("basic.addressService", AddressService.class);
        AddressDTO address = addressService.get(Integer.parseInt(addressId));
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (!RecipeBussConstant.PAYMODE_ONLINE.equals(payMode) && !RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
            return true;
        }
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(depId);
        if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_PLATFORM)) {
            ControlLogisticsOrderDto controlLogisticsOrderDto = new ControlLogisticsOrderDto();
            String organList = recipeParameterDao.getByName("zhHospitalOrganList");
            if (null != enterprise.getOrganId() && StringUtils.isNotEmpty(organList) && LocalStringUtil.hasOrgan(enterprise.getOrganId().toString(), organList)) {
                // 取药企对应的机构ID
                controlLogisticsOrderDto.setOrganId(enterprise.getOrganId());
            } else {
                // 机构id
                controlLogisticsOrderDto.setOrganId(recipe.getClinicOrgan());
            }
            // 寄件人姓名
            controlLogisticsOrderDto.setConsignorName(enterprise.getConsignorName());
            // 寄件人手机号
            controlLogisticsOrderDto.setConsignorPhone(enterprise.getConsignorMobile());
            // 寄件人省份
            controlLogisticsOrderDto.setConsignorProvince(AddressUtils.getAddressDic(enterprise.getConsignorProvince()));
            // 寄件人城市
            controlLogisticsOrderDto.setConsignorCity(AddressUtils.getAddressDic(enterprise.getConsignorCity()));
            // 寄件人区域
            controlLogisticsOrderDto.setConsignorDistrict(AddressUtils.getAddressDic(enterprise.getConsignorDistrict()));
            // 寄件人街道
            controlLogisticsOrderDto.setConsignorStreet(AddressUtils.getAddressDic(enterprise.getConsignorStreet()));
            // 寄件人详细地址
            controlLogisticsOrderDto.setConsignorAddress(enterprise.getConsignorAddress());
            //省
            String address1 = address.getAddress1();
            //市
            String address2 = address.getAddress2();
            //区
            String address3 = address.getAddress3();
            //详细地址
            String address4 = address.getAddress4();
            // 街道
            String streetAddress = address.getStreetAddress();
            String phone = address.getRecMobile();
            // 收件人名称
            controlLogisticsOrderDto.setAddresseeName(address.getReceiver());
            // 收件人手机号
            controlLogisticsOrderDto.setAddresseePhone(phone);
            // 收件省份
            controlLogisticsOrderDto.setAddresseeProvince(AddressUtils.getAddressDic(address1));
            // 收件城市
            controlLogisticsOrderDto.setAddresseeCity(AddressUtils.getAddressDic(address2));
            // 收件镇/区
            controlLogisticsOrderDto.setAddresseeDistrict(AddressUtils.getAddressDic(address3));
            // 收件人街道
            controlLogisticsOrderDto.setAddresseeStreet(AddressUtils.getAddressDic(streetAddress));
            // 收件详细地址
            controlLogisticsOrderDto.setAddresseeAddress(address4);
            // 物流公司编码
            if (Objects.nonNull(logisticsCompany)) {
                controlLogisticsOrderDto.setLogisticsCode(logisticsCompany.toString());
            }
            logger.info("orderCanSend req controlLogisticsOrderDto={}", controlLogisticsOrderDto);
            String orderCanSend = infraClient.orderCanSend(controlLogisticsOrderDto);
            logger.info("orderCanSend:{}",orderCanSend);
            JSONObject jsonObject = JSONObject.parseObject(orderCanSend);
            String code = jsonObject.getString("code");

            if (!"0".equals(code)) {
                logger.info("物流地址管控!!!!!");
                return false;
            }
        }
        return true;
    }

    /**
     * 邵逸夫模式下 订单没有运费与审方费用的情况下生成一条支付流水
     *
     * @param order
     * @return
     */
    public void saveFlowByOrder(RecipeOrder order) {
        logger.info("RecipeOrderManager saveFlowByOrder order:{}", JSONUtils.toString(order));
        // 邵逸夫模式下 不需要审方物流费需要生成一条流水记录
        Boolean syfPayMode = configurationClient.getValueBooleanCatch(order.getOrganId(), "syfPayMode", false);
        if (syfPayMode) {
            BigDecimal otherFee = Objects.isNull(order.getAuditFee()) ? BigDecimal.ZERO : order.getAuditFee();
            if (Objects.nonNull(order.getEnterpriseId())) {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (checkExpressFeePayWay(drugsEnterprise.getExpressFeePayWay())) {
                    otherFee = otherFee.add(Objects.isNull(order.getExpressFee()) ? BigDecimal.ZERO : order.getExpressFee());
                }
            }
            if (0d >= otherFee.doubleValue()) {
                RecipeOrderPayFlow recipeOrderPayFlow = new RecipeOrderPayFlow();
                recipeOrderPayFlow.setOrderId(order.getOrderId());
                recipeOrderPayFlow.setTotalFee(0d);
                recipeOrderPayFlow.setPayFlowType(2);
                recipeOrderPayFlow.setPayFlag(1);
                recipeOrderPayFlow.setOutTradeNo("");
                recipeOrderPayFlow.setPayOrganId("");
                recipeOrderPayFlow.setTradeNo("");
                recipeOrderPayFlow.setWnPayWay("");
                recipeOrderPayFlow.setWxPayWay("");
                Date date = new Date();
                recipeOrderPayFlow.setCreateTime(date);
                recipeOrderPayFlow.setModifiedTime(date);
                recipeOrderPayFlowDao.save(recipeOrderPayFlow);
            }
        }

    }

    /**
     * 根据处方订单code查询处方费用详情(邵逸夫模式专用)
     *
     * @param orderCode
     * @return
     */
    public List<RecipeFeeDTO> findRecipeOrderDetailFee(String orderCode) {
        logger.info("RecipeOrderManager findRecipeOrderDetailFee orderCode:{}", orderCode);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        Integer payFlag = order.getPayFlag();
        List<RecipeOrderPayFlow> byOrderId = recipeOrderPayFlowDao.findByOrderId(order.getOrderId());
        List<RecipeFeeDTO> list = Lists.newArrayList();
        for (RecipeOrderDetailFeeEnum value : RecipeOrderDetailFeeEnum.values()) {
            addRecipeFeeDTO(list, value, payFlag, byOrderId);
        }
        logger.info("RecipeOrderManager findRecipeOrderDetailFee res :{}", JSONUtils.toString(list));
        return list;
    }


    /**
     * 通过订单号获取该订单下关联的所有处方
     *
     * @param orderCode 订单号
     * @return 处方集合
     */
    public List<Recipe> getRecipesByOrderCode(String orderCode) {
        logger.info("RecipeOrderManager getRecipesByOrderCode orderCode:{}", orderCode);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        logger.info("RecipeOrderManager getRecipesByOrderCode recipes:{}", JSON.toJSONString(recipes));
        return recipes;
    }

    public List<Integer> getRecipeIdsByOrderId(Integer orderId) {
        logger.info("RecipeOrderManager getRecipeIdsByOrderId orderId:{}", orderId);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(orderId);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        logger.info("RecipeOrderManager getRecipeIdsByOrderId recipeIdList:{}", JSON.toJSONString(recipeIdList));
        return recipeIdList;
    }

    /**
     * todo 迁移代码 需要优化 （尹盛）
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @param recipeId
     * @return
     */
    public SkipThirdDTO getThirdUrl(Integer recipeId, Integer giveMode) {
        SkipThirdDTO skipThirdDTO = new SkipThirdDTO();
        if (null == recipeId) {
            return skipThirdDTO;
        }
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            return skipThirdDTO;
        }
        if (recipe.getClinicOrgan() == 1005683) {
            return getUrl(recipe, giveMode);
        }
        if (null == recipe.getEnterpriseId()) {
            return skipThirdDTO;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
        if (drugsEnterprise != null && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
            return getUrl(recipe, giveMode);
        }
        return skipThirdDTO;
    }


    /**
     * 通过订单 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            super.getAddressDic(address, order.getAddress1());
            super.getAddressDic(address, order.getAddress2());
            super.getAddressDic(address, order.getAddress3());
            super.getAddressDic(address, order.getStreetAddress());
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }


    /**
     * 获取订单列表
     *
     * @param orderCodes
     * @return
     */
    public List<RecipeOrder> getRecipeOrderList(Set<String> orderCodes) {
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            return recipeOrderDAO.findByOrderCode(orderCodes);
        }
        return new ArrayList<>();
    }

    public RecipeOrder getRecipeOrderById(Integer orderId) {
        return recipeOrderDAO.getByOrderId(orderId);
    }

    /**
     * 通过商户订单号获取订单
     *
     * @param outTradeNo 商户订单号
     * @return 订单
     */
    public RecipeOrder getByOutTradeNo(String outTradeNo) {
        return recipeOrderDAO.getByOutTradeNo(outTradeNo);
    }

    /**
     * 端 用查询订单信息
     *
     * @param orderId
     * @return
     */
    public RecipeOrderDto getRecipeOrderByBusId(Integer orderId) {
        logger.info("RecipeOrderManager getRecipeOrderByBusId orderId:{}", orderId);
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        String recipeIdList = recipeOrder.getRecipeIdList();

        List<Integer> recipeIds = JSONArray.parseArray(recipeIdList, Integer.class);

        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        // 如果处方中的订单编号与处方中保存的订单编号不一致,则视为订单失效
        if (!recipeOrder.getOrderCode().equals(recipeList.get(0).getOrderCode())) {
            return null;
        }
        RecipeOrderDto recipeOrderDto = new RecipeOrderDto();
        BeanCopyUtils.copy(recipeOrder, recipeOrderDto);
        recipeOrderDto.setStatusText(getStatusText(recipeOrderDto.getStatus()));
        List<RecipeBeanDTO> recipeBeanDTOS = recipeList.stream().map(recipe -> {
            RecipeBeanDTO recipeBeanDTO = new RecipeBeanDTO();
            recipeBeanDTO.setOrganDiseaseName(recipe.getOrganDiseaseName());
            recipeBeanDTO.setOrganName(recipe.getOrganName());
            recipeBeanDTO.setRecipeId(recipe.getRecipeId());
            recipeBeanDTO.setDepart(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", recipe.getDepart()));
            if (null != recipe.getDoctor()) {
                DoctorDTO doctorDTO = doctorService.get(recipe.getDoctor());
                recipeBeanDTO.setDoctor(doctorDTO.getName());
            }
            return recipeBeanDTO;
        }).collect(Collectors.toList());
        recipeOrderDto.setRecipeList(recipeBeanDTOS);
        logger.info("RecipeOrderManager getRecipeOrderByBusId res recipeOrderDto :{}", JSONArray.toJSONString(recipeOrderDto));
        return recipeOrderDto;
    }

    public boolean updateNonNullFieldByPrimaryKey(RecipeOrder recipeOrder) {
        return recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
    }

    /**
     * 处方退款推送his服务
     *
     * @param recipeId
     */
    public String recipeRefundMsg(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return "处方不存在";
        }

        List<String> recipeTypes = configurationClient.getValueListCatch(recipe.getClinicOrgan(), "getRecipeTypeToHis", null);
        if (!recipeTypes.contains(Integer.toString(recipe.getRecipeType())) && RecipeUtil.isTcmType(recipe.getRecipeType())) {
            return "成功";
        }
        if (!configurationClient.isHisEnable(recipe.getClinicOrgan())) {
            recipeLogDAO.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "recipeRefund[RecipeRefundService] HIS未启用");
            logger.warn("recipeRefund 医院HIS未启用[organId:" + recipe.getClinicOrgan() + ",recipeId:" + recipe.getRecipeId() + "]");
            return "成功";
        }
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipeId);
        com.ngari.recipe.dto.PatientDTO patientBean = patientClient.getPatientDTO(recipe.getMpiid());
        HealthCardBean cardBean = patientClient.getCardBean(recipe.getMpiid(), recipe.getClinicOrgan());
        String backInfo = refundClient.recipeRefund(recipe, details, patientBean, cardBean);
        recipeLogDAO.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeStatusEnum.NONE.getType(), "同步HIS退款返回：" + backInfo);
        return backInfo;
    }

    /**
     * 根据 处方ID 或者 订单id 查询订单数据
     *
     * @param recipeId 处方ID
     * @param orderId  订单id
     * @return 订单数据
     */
    public RecipeOrder getRecipeOrder(Integer recipeId, Integer orderId) {
        RecipeOrder recipeOrder = null;
        if (!ValidateUtil.integerIsEmpty(orderId)) {
            recipeOrder = recipeOrderDAO.getByOrderId(orderId);
        }
        if (!ValidateUtil.integerIsEmpty(recipeId)) {
            recipeOrder = recipeOrderDAO.getOrderByRecipeId(recipeId);
        }
        return recipeOrder;
    }

    /**
     * 更新订单的物流信息
     *
     * @param orderId          订单ID
     * @param logisticsCompany 物流公司
     * @param trackingNumber   物流单号
     * @return
     */
    public boolean updateOrderLogisticsInfo(Integer orderId, Integer logisticsCompany, String trackingNumber) {
        if (StringUtils.isEmpty(trackingNumber)) {
            return false;
        }
        RecipeOrder recipeOrder = new RecipeOrder(orderId);
        recipeOrder.setLogisticsCompany(logisticsCompany);
        recipeOrder.setTrackingNumber(trackingNumber);
        return recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
    }

    /**
     * 处理患者信息
     *
     * @param mpiId
     * @return
     */
    private PatientBaseInfo patientBaseInfo(String mpiId) {
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        if (StringUtils.isEmpty(mpiId)) {
            return patientBaseInfo;
        }
        PatientDTO patient = patientClient.getPatientBeanByMpiId(mpiId);
        if (patient != null) {
            patientBaseInfo.setPatientName(patient.getPatientName());
            patientBaseInfo.setCertificate(patient.getCertificate());
            patientBaseInfo.setCertificateType(patient.getCertificateType());
            patientBaseInfo.setMobile(patient.getMobile());
        }
        return patientBaseInfo;
    }

    /**
     * 获取处方详情费用
     *
     * @param list
     * @param feeType
     * @param payFlag
     * @param byOrderId
     */
    private void addRecipeFeeDTO(List<RecipeFeeDTO> list, RecipeOrderDetailFeeEnum feeType, Integer payFlag, List<RecipeOrderPayFlow> byOrderId) {
        RecipeFeeDTO recipeFeeDTO = new RecipeFeeDTO();
        recipeFeeDTO.setFeeType(feeType.getName());
        recipeFeeDTO.setPayFlag(payFlag);
        if (payFlag.equals(PayFlagEnum.NOPAY.getType()) && CollectionUtils.isNotEmpty(byOrderId)) {
            Map<Integer, List<RecipeOrderPayFlow>> collect = byOrderId.stream().collect(Collectors.groupingBy(RecipeOrderPayFlow::getPayFlowType));
            recipeFeeDTO.setPayFlag(getPayFlag(feeType, collect));
        }
        list.add(recipeFeeDTO);
    }

    /**
     * 获取支付状态
     *
     * @param recipeOrderDetailFeeEnum
     * @param collect
     * @return
     */
    private Integer getPayFlag(RecipeOrderDetailFeeEnum recipeOrderDetailFeeEnum, Map<Integer, List<RecipeOrderPayFlow>> collect) {
        Integer payFlag = 0;
        List<RecipeOrderPayFlow> recipeOrderPayFlows = collect.get(recipeOrderDetailFeeEnum.getType());
        if (CollectionUtils.isEmpty(recipeOrderPayFlows)) {
            payFlag = PayFlagEnum.NOPAY.getType();
        } else {
            payFlag = recipeOrderPayFlows.get(0).getPayFlag();
        }
        return payFlag;
    }

    /**
     * 是否需要计算运费
     *
     * @param expressFeePayWay
     * @return
     */
    private Boolean checkExpressFeePayWay(Integer expressFeePayWay) {
        if (new Integer(2).equals(expressFeePayWay) || new Integer(3).equals(expressFeePayWay) || new Integer(4).equals(expressFeePayWay)) {
            return false;
        }
        return true;
    }

    private SkipThirdDTO getUrl(Recipe recipe, Integer giveMode) {
        RecipeThirdUrlReqTO req = new RecipeThirdUrlReqTO();
        if (null != recipe.getEnterpriseId()) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
            if (null != drugsEnterprise && StringUtils.isNotEmpty(drugsEnterprise.getThirdEnterpriseCode())) {
                req.setPatientChannelId(drugsEnterprise.getThirdEnterpriseCode());
            }
        }
        req.setOrganId(recipe.getClinicOrgan());
        req.setRecipeCode(String.valueOf(recipe.getRecipeId()));
        req.setSkipMode(giveMode);
        req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
        req.setUser(patientBaseInfo(recipe.getRequestMpiId()));
        PatientBaseInfo patientBaseInfo = patientBaseInfo(recipe.getMpiid());
        patientBaseInfo.setPatientID(recipe.getPatientID());
        patientBaseInfo.setMpi(recipe.getRequestMpiId());
        patientBaseInfo.setTid(enterpriseClient.getSimpleWxAccount().getTid());
        req.setPatient(patientBaseInfo);
        String openId = patientClient.getOpenId();
        if (StringUtils.isNotEmpty(openId)) {
            req.setOpenId(openId);
        }
        try {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (revisitExDTO != null && StringUtils.isNotEmpty(revisitExDTO.getProjectChannel())) {
                req.setPatientChannelId(revisitExDTO.getProjectChannel());
            }
        } catch (Exception e) {
            logger.error("queryPatientChannelId error:", e);
        }
        return enterpriseClient.skipThird(req);
    }

    /**
     * 获取订单状态
     *
     * @param status
     * @return
     */
    private String getStatusText(Integer status) {
        String statusText = "";
        switch (RecipeOrderStatusEnum.getRecipeOrderStatusEnum(status)) {
            case ORDER_STATUS_HAS_DRUG:
                statusText = "待取药";
                break;
            case ORDER_STATUS_NO_DRUG:
                statusText = "准备中";
                break;
            default:
                statusText = RecipeOrderStatusEnum.getOrderStatus(status);
                break;
        }
        return statusText;
    }
}
