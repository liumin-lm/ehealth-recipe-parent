package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.HisSettleReqDTO;
import com.ngari.his.recipe.mode.HisSettleReqTo;
import com.ngari.his.recipe.mode.HisSettleResTo;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import com.ngari.infra.logistics.mode.LogisticsDistanceDto;
import com.ngari.infra.logistics.mode.OrganLogisticsManageDto;
import com.ngari.infra.logistics.service.IOrganLogisticsManageService;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.InvoiceInfoReqTO;
import com.ngari.platform.recipe.mode.InvoiceInfoResTO;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.platform.recipe.mode.RecipeDetailBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.*;
import recipe.common.OnsConfig;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.enumerate.status.*;
import recipe.enumerate.type.*;
import recipe.util.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private PayClient payClient;
    @Autowired
    private InfraClient infraClient;
    @Autowired
    private IOrganLogisticsManageService organLogisticsManageService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;
    @Autowired
    private RecipeBeforeOrderDAO recipeBeforeOrderDAO;
    @Autowired
    private AddressService addressService;
    @Autowired
    private RecipeParameterDao parameterDao;
    @Autowired
    private RecipeHisClient recipeHisClient;

    /**
     * 合并预下单信息
     *
     * @param beforeOrderList
     * @return
     */
    @LogRecord
    public List<List<RecipeBeforeOrder>> mergeBeforeOrder(List<RecipeBeforeOrder> beforeOrderList) {
        if (CollectionUtils.isEmpty(beforeOrderList)) {
            return new ArrayList<>();
        }
        // 根据购药方式对所有预下单数据分组
        Map<Integer, List<RecipeBeforeOrder>> map = beforeOrderList.stream().collect(Collectors.groupingBy(RecipeBeforeOrder::getGiveMode));
        Set<Integer> keySet = map.keySet();
        List<List<RecipeBeforeOrder>> list = new ArrayList<>();
        keySet.forEach(key -> {
            List<RecipeBeforeOrder> recipeBeforeOrders = map.get(key);
            switch (GiveModeEnum.getGiveModeEnum(key)) {
                case GIVE_MODE_HOME_DELIVERY:
                case GIVE_MODE_PHARMACY_DRUG:
                    Map<String, List<RecipeBeforeOrder>> collect = recipeBeforeOrders.stream().collect(Collectors.groupingBy(beforeOrder -> beforeOrder.getEnterpriseId() + beforeOrder.getDrugStoreCode()));
                    collect.forEach((k, value) -> {
                        list.add(value);
                    });
                    break;
                case GIVE_MODE_HOSPITAL_DRUG:
                default:
                    list.add(recipeBeforeOrders);
                    break;
            }
        });
        return list;
    }

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
        if (Objects.isNull(depId) || StringUtils.isEmpty(addressId) || Objects.isNull(recipeId)) {
            logger.info("orderCanSend have null params");
            return true;
        }
        Recipe recipe = recipeDAO.get(recipeId);
        Boolean logisticsPlaceOrderFlag = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "logisticsPlaceOrderFlag", false);
        if (!logisticsPlaceOrderFlag) {
            return true;
        }
        AddressService addressService = AppContextHolder.getBean("basic.addressService", AddressService.class);
        AddressDTO address = addressService.get(Integer.parseInt(addressId));
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (!RecipeBussConstant.PAYMODE_ONLINE.equals(payMode) && !RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
            return true;
        }
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(depId);
        if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_PLATFORM) && logisticsCompany != null) {
            List<OrganLogisticsManageDto> organLogisticsManageDtos = organLogisticsManageService.findLogisticsManageByOrganIdAndLogisticsCompanyIdAndAccount(enterprise.getId(), logisticsCompany.toString(), DrugEnterpriseConstant.BUSINESS_TYPE, 0);
            logger.info("getCreateLogisticsOrderDto organLogisticsManageDtos:{}", JSONUtils.toString(organLogisticsManageDtos));
            OrganLogisticsManageDto organLogisticsManageDto = new OrganLogisticsManageDto();
            if (CollectionUtils.isNotEmpty(organLogisticsManageDtos) && organLogisticsManageDtos.get(0) != null) {
                organLogisticsManageDto = organLogisticsManageDtos.get(0);
            }
            ControlLogisticsOrderDto controlLogisticsOrderDto = new ControlLogisticsOrderDto();
            controlLogisticsOrderDto.setType(0);
            controlLogisticsOrderDto.setOrganId(enterprise.getId());
            // 寄件人姓名
            controlLogisticsOrderDto.setConsignorName(organLogisticsManageDto.getConsignorName());
            // 寄件人手机号
            controlLogisticsOrderDto.setConsignorPhone(organLogisticsManageDto.getConsignorPhone());
            // 寄件人省份
            controlLogisticsOrderDto.setConsignorProvince(organLogisticsManageDto.getConsignorProvince());
            // 寄件人城市
            controlLogisticsOrderDto.setConsignorCity(organLogisticsManageDto.getConsignorCity());
            // 寄件人区域
            controlLogisticsOrderDto.setConsignorDistrict(organLogisticsManageDto.getConsignorDistrict());
            // 寄件人街道
            controlLogisticsOrderDto.setConsignorStreet(organLogisticsManageDto.getConsignorStreet());
            // 寄件人详细地址
            controlLogisticsOrderDto.setConsignorAddress(organLogisticsManageDto.getConsignorAddress());
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
            logger.info("orderCanSend:{}", orderCanSend);
            if (Objects.isNull(orderCanSend)) {
                return true;
            }
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

    public String getCompleteAddress(String address1, String address2, String address3, String address4, String streetAddress) {
        StringBuilder address = new StringBuilder();
        super.getAddressDic(address, address1);
        super.getAddressDic(address, address2);
        super.getAddressDic(address, address3);
        super.getAddressDic(address, streetAddress);
        address.append(StringUtils.isEmpty(address4) ? "" : address4);
        return address.toString();
    }


    /**
     * 获取订单列表
     *
     * @param orderCodes
     * @return
     */
    public List<RecipeOrder> findEffectiveOrderByOrderCode(Set<String> orderCodes) {
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            return recipeOrderDAO.findEffectiveOrderByOrderCode(orderCodes);
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
        RecipeOrderDto recipeOrderDto = new RecipeOrderDto();
        if (!recipeOrder.getOrderCode().equals(recipeList.get(0).getOrderCode())) {
            recipeOrderDto.setIsInvalidOrder(YesOrNoEnum.YES.getType());
        }
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
        List<Integer> recipeIdList = new ArrayList<>();
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        } else {
            recipeIdList.add(recipeId);
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        String backInfo = payClient.recipeRefund(recipe, details, patientBean, cardBean, recipeList, recipeExtendList);
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
     * 查询订单处方信息
     *
     * @param enterpriseIdList 药企ID列表
     * @param mpiIdList        患者mpiId列表
     * @param beginTime        查询开始时间
     * @param endTime          查询结束时间
     * @return
     */
    public List<DownLoadRecipeOrderDTO> findOrderAndRecipes(List<Integer> enterpriseIdList, List<String> mpiIdList, Date beginTime, Date endTime) {
        List<DownLoadRecipeOrderDTO> downLoadRecipeOrderDTOList = new ArrayList<>();
        //查询订单信息
        List<RecipeOrder> recipeOrderList = recipeOrderDAO.findOrderForEnterprise(enterpriseIdList, mpiIdList, beginTime, endTime);
        if (CollectionUtils.isEmpty(recipeOrderList)) {
            return downLoadRecipeOrderDTOList;
        }
        List<String> recipeOrderCodeList = recipeOrderList.stream().map(RecipeOrder::getOrderCode).collect(Collectors.toList());
        logger.info("findOrderAndRecipes recipeOrderCodeList:{}", JSON.toJSONString(recipeOrderCodeList));
        //根据订单号查询处方
        List<Recipe> recipeList = recipeDAO.findByOrderCode(recipeOrderCodeList);
        List<Integer> recipeIdList = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        logger.info("findOrderAndRecipes recipeIdList:{}", JSON.toJSONString(recipeIdList));
        Map<String, List<Recipe>> recipeListMap = recipeList.stream().collect(Collectors.groupingBy(Recipe::getOrderCode));
        //根据处方查询扩展信息
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
        //根据处方查询明细信息
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIdList);
        //查询药企药品信息
        List<Integer> drugIdList = recipeDetailList.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdsAndDrugIds(enterpriseIdList, drugIdList);
        Map<Integer, List<Recipedetail>> recipeDetailListMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        recipeOrderList.forEach(recipeOrder -> {
            DownLoadRecipeOrderDTO downLoadRecipeOrderDTO = new DownLoadRecipeOrderDTO();
            downLoadRecipeOrderDTO.setRecipeOrder(recipeOrder);
            List<Recipe> recipes = recipeListMap.get(recipeOrder.getOrderCode());
            downLoadRecipeOrderDTO.setRecipeList(recipes);
            List<RecipeExtend> recipeExtends = new ArrayList<>();
            List<Recipedetail> recipeDetails = new ArrayList<>();
            recipes.forEach(recipe -> {
                RecipeExtend recipeExtend = recipeExtendMap.get(recipe.getRecipeId());
                recipeExtends.add(recipeExtend);
                List<Recipedetail> recipeDetailSmall = recipeDetailListMap.get(recipe.getRecipeId());
                recipeDetails.addAll(recipeDetailSmall);
            });
            downLoadRecipeOrderDTO.setRecipeExtendList(recipeExtends);
            downLoadRecipeOrderDTO.setRecipeDetailList(recipeDetails);
            downLoadRecipeOrderDTO.setSaleDrugLists(saleDrugLists);
            downLoadRecipeOrderDTOList.add(downLoadRecipeOrderDTO);
        });
        logger.info("findOrderAndRecipes downLoadRecipeOrderDTOList:{}", JSON.toJSONString(downLoadRecipeOrderDTOList));
        return downLoadRecipeOrderDTOList;
    }

    public QueryResult<RecipeOrder> findRefundRecipeOrder(RecipeOrderRefundReqDTO recipeOrderRefundReqDTO) {
        if (OpRefundBusTypeEnum.BUS_TYPE_ALL_ORDER.getType().equals(recipeOrderRefundReqDTO.getBusType())) {
            return recipeOrderDAO.findRefundRecipeOrder(recipeOrderRefundReqDTO);
        } else if (OpRefundBusTypeEnum.BUS_TYPE_REFUND_ORDER.getType().equals(recipeOrderRefundReqDTO.getBusType())) {
            return recipeOrderDAO.findWaitApplyRefundRecipeOrder(recipeOrderRefundReqDTO);
        } else if (OpRefundBusTypeEnum.BUS_TYPE_FAIL_ORDER.getType().equals(recipeOrderRefundReqDTO.getBusType())) {
            //查询推送药企失败的订单
            return recipeOrderDAO.findPushFailRecipeOrder(recipeOrderRefundReqDTO);
        }
        return new QueryResult<>();
    }

    public void cancelOrder(RecipeOrder order, List<Recipe> recipeList, Boolean canCancelOrderCode, Integer identity) {
        logger.info("RecipeOrderService cancelOrder  order= {}，recipeList={}, canCancelOrderCode= {} ,identity={}", JSON.toJSONString(order), JSON.toJSONString(recipeList), canCancelOrderCode, identity);
        // 邵逸夫手动取消要查看是否有支付审方费
        Boolean syfPayMode = configurationClient.getValueBooleanCatch(order.getOrganId(), "syfPayMode", false);
        if (syfPayMode) {
            //邵逸夫支付
            RecipeOrderPayFlow recipeOrderPayFlow = recipeOrderPayFlowDao.getByOrderIdAndType(order.getOrderId(), PayFlowTypeEnum.RECIPE_AUDIT.getType());
            if (null != recipeOrderPayFlow) {
                if (StringUtils.isEmpty(recipeOrderPayFlow.getOutTradeNo())) {
                    //表示没有实际支付审方或者快递费,只需要更新状态
                    recipeOrderPayFlow.setPayFlag(PayFlagEnum.REFUND_SUCCESS.getType());
                    recipeOrderPayFlowDao.updateNonNullFieldByPrimaryKey(recipeOrderPayFlow);
                } else {
                    //说明需要正常退审方费
                    payClient.refund(order.getOrderId(), PayBusTypeEnum.OTHER_BUS_TYPE.getName());
                }
            }
        }
        recipeList.forEach(recipe -> {
            recipeDAO.updateOrderCodeToNullByOrderCodeAndClearChoose(order.getOrderCode(), recipe, identity, true);
            String decoctionDeploy = configurationClient.getValueCatchReturnArr(recipe.getClinicOrgan(), "decoctionDeploy", "");
            if ("2".equals(decoctionDeploy)) {
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                if (recipeExtend != null) {
                    recipeExtend.setDecoctionText(null);
                    recipeExtend.setDecoctionPrice(null);
                    recipeExtend.setDecoctionId(null);
                    recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
                }
            }
        });
    }

    public List<RecipeOrder> orderListByClinicId(Integer clinicId, Integer bussSource) {
        List<Recipe> recipeList = recipeDAO.findRecipeAllByBussSourceAndClinicId(bussSource, clinicId);
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        List<String> orderCodeList = recipeList.stream().map(Recipe::getOrderCode).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(orderCodeList)) {
            return null;
        }
        return recipeOrderDAO.findByOrderCode(orderCodeList);
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
        return !new Integer(2).equals(expressFeePayWay) && !new Integer(3).equals(expressFeePayWay) && !new Integer(4).equals(expressFeePayWay);
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

    public String queryEinvoiceNumberByRecipeId(Integer recipeId) {
        String einvoiceNumber = "";
        if (null != recipeId) {
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (null != recipeExtend && StringUtils.isNotBlank(recipeExtend.getEinvoiceNumber())) {
                einvoiceNumber = recipeExtend.getEinvoiceNumber();
            } else {
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                if (recipe != null && StringUtils.isNotBlank(recipe.getOrderCode())) {
                    RecipeOrderBillDAO recipeOrderBillDAO = DAOFactory.getDAO(RecipeOrderBillDAO.class);
                    RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
                    if (null != recipeOrderBill) {
                        einvoiceNumber = recipeOrderBill.getBillNumber();
                    }
                }
            }
        }
        return einvoiceNumber;
    }

    public List<ReimbursementDTO> findReimbursementList(ReimbursementListReqDTO reimbursementListReq) {
        List<ReimbursementDTO> reimbursementDTOList = new ArrayList<>();
        //根据机构ID、患者唯一标识查询规定时间内的处方单（orderCode不为空的）
        List<Recipe> recipeList = recipeDAO.findRecipesByClinicOrganAndMpiId(reimbursementListReq.getOrganId(), reimbursementListReq.getMpiId(), reimbursementListReq.getStartTime(), reimbursementListReq.getEndTime());
        logger.info("findReimbursementList recipeList={}", JSONUtils.toString(recipeList));
        if (CollectionUtils.isEmpty(recipeList)) {
            logger.info("findReimbursementList 处方单不存在");
            return null;
        }
        for (Recipe recipe : recipeList) {
            PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(reimbursementListReq.getMpiId());
            if (patientDTO == null) {
                logger.info("findReimbursementList 患者不存在");
                return null;
            }
            //已经开过发票的处方单
            String invoiceNumber = queryEinvoiceNumberByRecipeId(recipe.getRecipeId());
            if (StringUtils.isNotEmpty(invoiceNumber)) {
                RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                //支付成功、未退费的处方单
                if (new Integer(1).equals(recipeOrder.getPayFlag()) && new Integer(0).equals(recipeOrder.getRefundFlag())) {
                    ReimbursementDTO reimbursementListDTO = new ReimbursementDTO();
                    reimbursementListDTO.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.recipe.dto.PatientDTO.class));
                    reimbursementListDTO.setInvoiceNumber(invoiceNumber);
                    reimbursementListDTO.setRecipe(recipe);
                    reimbursementListDTO.setRecipeOrder(recipeOrder);
                    List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                    if (CollectionUtils.isEmpty(recipeDetailList)) {
                        return null;
                    }
                    reimbursementListDTO.setRecipeDetailList(recipeDetailList);
                    reimbursementDTOList.add(reimbursementListDTO);
                }
            }
        }
        logger.info("findReimbursementList reimbursementDTOList={}", JSONUtils.toString(reimbursementDTOList));
        return reimbursementDTOList;
    }

    public ReimbursementDTO findReimbursementDetail(Integer recipeId) {
        ReimbursementDTO reimbursementDetailDTO = new ReimbursementDTO();
        Recipe recipe = recipeDAO.get(recipeId);
        if (recipe == null) {
            logger.info("findReimbursementDetail 处方单不存在");
            return null;
        }
        logger.info("findReimbursementDetail recipe={}", JSONUtils.toString(recipe));
        reimbursementDetailDTO.setRecipe(recipe);
        reimbursementDetailDTO.setInvoiceNumber(queryEinvoiceNumberByRecipeId(recipeId));
        PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
        if (patientDTO == null) {
            logger.info("findReimbursementDetail 患者不存在");
            return null;
        }
        logger.info("findReimbursementDetail patientDTO={}", JSONUtils.toString(patientDTO));
        reimbursementDetailDTO.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.recipe.dto.PatientDTO.class));
        if (StringUtils.isNotBlank(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            reimbursementDetailDTO.setRecipeOrder(recipeOrder);
        }
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeDetailList)) {
            reimbursementDetailDTO.setRecipeDetailList(recipeDetailList);
        }
        logger.info("findReimbursementDetail reimbursementDetailDTO={}", JSONUtils.toString(reimbursementDetailDTO));
        return reimbursementDetailDTO;
    }

    public String getOrderTips(RecipeOrder order) {
        if (OrderStateEnum.PROCESS_STATE_DISPENSING.getType().equals(order.getProcessState())) {
            return "已完成";
        }

        return null;
    }

    public InvoiceInfoResTO makeUpInvoice(String orderCode) {
        logger.info("EleInvoiceService.makeUpInvoice orderCode={}", orderCode);
        InvoiceInfoReqTO invoiceInfoReqTO = new InvoiceInfoReqTO();
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        PatientDTO patientDTO = new PatientDTO();
        Integer organId = recipeList.get(0).getClinicOrgan();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (recipeList.get(0).getBussSource().equals(2)) {
            RevisitBean revisitBean = revisitClient.getRevisitByClinicId(recipeList.get(0).getClinicId());
            if (Objects.nonNull(revisitBean)) {
                //就诊时间
                invoiceInfoReqTO.setVisitTime(df.format(revisitBean.getRequestTime()));
            }
        }
        if (recipeList.size() > 0) {
            patientDTO = patientService.get(recipeList.get(0).getMpiid());
            invoiceInfoReqTO.setRecipeBean(ObjectCopyUtils.convert(recipeList, RecipeBean.class));
            invoiceInfoReqTO.setOrganId(organId);
            invoiceInfoReqTO.setPatientId(recipeList.get(0).getPatientID());
        }
        invoiceInfoReqTO.setMedicalSettleInfo(recipeOrder.getMedicalSettleInfo());
        invoiceInfoReqTO.setHisSettlementNo(recipeOrder.getHisSettlementNo());
        invoiceInfoReqTO.setCashAmount(String.valueOf(recipeOrder.getCashAmount()));
        invoiceInfoReqTO.setTotalFee(String.valueOf(recipeOrder.getTotalFee()));
        invoiceInfoReqTO.setChargingStandard(String.valueOf(recipeOrder.getTotalFee()));
        invoiceInfoReqTO.setPayTime(df.format(recipeOrder.getPayTime()));
        //就诊流水号
        invoiceInfoReqTO.setTradeNo(recipeOrder.getTradeNo());
        if (Objects.nonNull(patientDTO)) {
            invoiceInfoReqTO.setPatientName(patientDTO.getPatientName());
            invoiceInfoReqTO.setSex(patientDTO.getPatientSex());
            invoiceInfoReqTO.setAge(String.valueOf(patientDTO.getAge()));
            invoiceInfoReqTO.setPhone(patientDTO.getMobile());
            //身份证号
            invoiceInfoReqTO.setIdcard(patientDTO.getIdcard());
        }
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIdList);
        recipeDetailList.forEach(recipeDetail -> {
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
            recipeDetail.setDrugForm(organDrugList.getDrugForm());
        });
        invoiceInfoReqTO.setRecipeDetailList(ObjectCopyUtils.convert(recipeDetailList, RecipeDetailBean.class));
        logger.info("EleInvoiceService.makeUpInvoice invoiceInfoReqTO={}", JSONUtils.toString(invoiceInfoReqTO));
        InvoiceInfoResTO invoiceInfoResTO = payClient.makeUpInvoice(invoiceInfoReqTO);
        logger.info("EleInvoiceService.makeUpInvoice invoiceInfoResTO={}", JSONUtils.toString(invoiceInfoResTO));
        RecipeOrderBill recipeOrderBill = new RecipeOrderBill();
        recipeOrderBill.setRecipeOrderCode(orderCode);
        recipeOrderBill.setBillNumber(invoiceInfoResTO.getInvoiceNo());
        recipeOrderBill.setBillBathCode(invoiceInfoResTO.getElectronicReceiptNo());
        recipeOrderBill.setBillQrCode(invoiceInfoResTO.getQrCodeData());
        recipeOrderBill.setBillPictureUrl(invoiceInfoResTO.getElectronicReceiptUrl());
        recipeOrderBill.setCreateTime(new Date());
        recipeOrderBillDAO.save(recipeOrderBill);
        return invoiceInfoResTO;
    }

    /**
     * 若同城速递，用户的收货地址与发件地址，距离超过100KM，则无法下单
     *
     * @param
     * @param extInfo
     * @return true表示可以下单
     */
    @LogRecord
    public boolean controlLogisticsDistance(Map<String, String> extInfo) {
        Integer logisticsCompany = MapValueUtil.getInteger(extInfo, "logisticsCompany");
        if (!new Integer(301).equals(logisticsCompany)) {
            return true;
        }
        Integer addressId = MapValueUtil.getInteger(extInfo, "addressId");
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        if (addressId == null) {
            logger.info("addressId is null");
            return false;
        }
        if (depId == null) {
            logger.info("depId is null");
            return false;
        }
        AddressDTO addressDTO = addressService.getByAddressId(addressId);
        logger.info("controlLogisticsDistance addressDTO:{}", JSONUtils.toString(addressDTO));
        if (addressDTO == null) {
            logger.info("addressDTO is null");
            return false;
        }
        LogisticsDistanceDto logisticsDistanceDto = new LogisticsDistanceDto();
        logisticsDistanceDto.setLatitude(addressDTO.getLatitude());
        logisticsDistanceDto.setLongitude(addressDTO.getLongitude());
        logisticsDistanceDto.setLogisticsCode(String.valueOf(logisticsCompany));
        logisticsDistanceDto.setOrganId(depId);
        logisticsDistanceDto.setBusinessType(1);
        Map<String, String> result = infraClient.controlLogisticsDistance(logisticsDistanceDto);
        if (result != null) {
            return !"1".equals(result.get("distance"));
        }
        return true;
    }

    public void saveRecipeBeforeOrderInfo(ShoppingCartReqDTO shoppingCartReqDTO) {
        //判断是新增到购物车还是在原有的基础上修改购药方式
        Integer recipeId = shoppingCartReqDTO.getRecipeId();
        Recipe recipe = recipeDAO.getByRecipeId(shoppingCartReqDTO.getRecipeId());
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            throw new DAOException(609, "处方已经存在订单信息");
        }
        if (!RecipeStateEnum.PROCESS_STATE_ORDER.getType().equals(recipe.getProcessState())) {
            throw new DAOException(609, "当前处方不是待下单状态");
        }
        if (recipeId != null) {
            //查询有效的预下单信息
            RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(recipeId);
            if (Objects.nonNull(recipeBeforeOrder)) {
                if (YesOrNoEnum.YES.getType().equals(recipeBeforeOrder.getIsLock())) {
                    throw new DAOException(609, "该处方已经锁定购药方式，无法重新选择药企");
                }
                //把原有的删除状态置为1，再新增一条数据
                recipeBeforeOrder.setDeleteFlag(1);
                recipeBeforeOrder.setUpdateTime(new Date());
                recipeBeforeOrderDAO.updateNonNullFieldByPrimaryKey(recipeBeforeOrder);
            }
        }
        RecipeBeforeOrder recipeBeforeOrder = new RecipeBeforeOrder();
        recipeBeforeOrder.setRecipeId(shoppingCartReqDTO.getRecipeId());
        if (recipe != null) {
            recipeBeforeOrder.setOrganId(recipe.getClinicOrgan());
            recipeBeforeOrder.setRecipeCode(recipe.getRecipeCode());
        }
        recipeBeforeOrder.setEnterpriseId(shoppingCartReqDTO.getEnterpriseId());
        recipeBeforeOrder.setGiveMode(shoppingCartReqDTO.getGiveMode());
        //购药方式为到院取药时预下单信息为完善
        if (new Integer(2).equals(shoppingCartReqDTO.getGiveMode())) {
            recipeBeforeOrder.setIsReady(1);
        }
        //购药方式为到店取药时
        else if (new Integer(3).equals(shoppingCartReqDTO.getGiveMode())) {
            //有药店信息则为完善否则为不完善
            if (shoppingCartReqDTO.getDrugStoreName() != null && shoppingCartReqDTO.getDrugStoreCode() != null) {
                recipeBeforeOrder.setIsReady(1);
                recipeBeforeOrder.setDrugStoreName(shoppingCartReqDTO.getDrugStoreName());
                recipeBeforeOrder.setDrugStoreAddr(shoppingCartReqDTO.getDrugStoreAddr());
                recipeBeforeOrder.setDrugStoreCode(shoppingCartReqDTO.getDrugStoreCode());
            } else {
                recipeBeforeOrder.setIsReady(0);
            }
            //配送方式为医院配送或药企配送
        } else if (new Integer(1).equals(shoppingCartReqDTO.getGiveMode())) {
            recipeBeforeOrder.setIsReady(0);
        }
        recipeBeforeOrder.setGiveModeKey(shoppingCartReqDTO.getGiveModeKey());
        recipeBeforeOrder.setGiveModeText(RecipeSupportGiveModeEnum.getNameByText(shoppingCartReqDTO.getGiveModeKey()));
        recipeBeforeOrder.setDeleteFlag(0);
        recipeBeforeOrder.setCreateTime(new Date());
        recipeBeforeOrder.setUpdateTime(new Date());
        recipeBeforeOrder.setOperMpiId(shoppingCartReqDTO.getOperMpiId());
        if (shoppingCartReqDTO.getGiveMode() != null) {
            switch (shoppingCartReqDTO.getGiveMode()) {
                case 1:
                case 2:
                    recipeBeforeOrder.setTakeMedicineWay(0);
                    break;
                case 3:
                    recipeBeforeOrder.setTakeMedicineWay(1);
                    break;
            }
        }
        logger.info("saveRecipeBeforeOrderInfo recipeBeforeOrder={}", JSONUtils.toString(recipeBeforeOrder));
        recipeBeforeOrderDAO.save(recipeBeforeOrder);
    }

    /**
     * 处理购物清单中的地址
     *
     * @param recipeBeforeOrder1
     * @param recipeBeforeOrder2
     * @param recipeOrder
     * @return
     */
    public void processShoppingCartAddress(RecipeBeforeOrderDTO recipeBeforeOrder1, RecipeBeforeOrder recipeBeforeOrder2, RecipeOrder recipeOrder) {
        recipeBeforeOrder1.setAddressId(recipeBeforeOrder2.getAddressId() != null ? recipeBeforeOrder2.getAddressId() : recipeOrder.getAddressID());
        recipeBeforeOrder1.setAddress1(recipeBeforeOrder2.getAddress1() != null ? recipeBeforeOrder2.getAddress1() : recipeOrder.getAddress1());
        recipeBeforeOrder1.setAddress2(recipeBeforeOrder2.getAddress2() != null ? recipeBeforeOrder2.getAddress2() : recipeOrder.getAddress2());
        recipeBeforeOrder1.setAddress3(recipeBeforeOrder2.getAddress3() != null ? recipeBeforeOrder2.getAddress3() : recipeOrder.getAddress3());
        recipeBeforeOrder1.setAddress4(recipeBeforeOrder2.getAddress4() != null ? recipeBeforeOrder2.getAddress4() : recipeOrder.getAddress4());
        recipeBeforeOrder1.setStreetAddress(recipeBeforeOrder2.getStreetAddress() != null ? recipeBeforeOrder2.getStreetAddress() : recipeOrder.getStreetAddress());
        recipeBeforeOrder1.setAddress5(recipeBeforeOrder2.getAddress5() != null ? recipeBeforeOrder2.getAddress5() : recipeOrder.getAddress5());
        recipeBeforeOrder1.setAddress5Text(recipeBeforeOrder2.getAddress5Text() != null ? recipeBeforeOrder2.getAddress5Text() : recipeOrder.getAddress5Text());
        recipeBeforeOrder1.setReceiver(recipeBeforeOrder2.getReceiver() != null ? recipeBeforeOrder2.getReceiver() : recipeOrder.getReceiver());
        recipeBeforeOrder1.setRecMobile(recipeBeforeOrder2.getRecMobile() != null ? recipeBeforeOrder2.getRecMobile() : recipeOrder.getRecMobile());
        recipeBeforeOrder1.setRecTel(recipeBeforeOrder2.getRecTel() != null ? recipeBeforeOrder2.getRecTel() : recipeOrder.getRecTel());
        recipeBeforeOrder1.setZipCode(recipeBeforeOrder2.getZipCode() != null ? recipeBeforeOrder2.getZipCode() : recipeOrder.getZipCode());
    }

    public void recordPayBackLog(Integer orderId, String orderLog) {
        try {
            RecipeParameter recipeParameter = new RecipeParameter();
            recipeParameter.setParamName(orderId + "_PayInfoCallBack");
            recipeParameter.setParamValue(orderLog);
            parameterDao.save(recipeParameter);
        } catch (DAOException e) {
            logger.error("OrderManager recordPayBackLog error", e);
        }
    }

    /**
     * 根据订单信息获取his的结算信息
     *
     * @param orders
     * @return
     */
    public List<String> hisSettleByOrder(List<RecipeOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }
        HisSettleReqTo hisSettleReqTo = new HisSettleReqTo();
        hisSettleReqTo.setOperatorId("NALI");
        hisSettleReqTo.setOrganId(orders.get(0).getOrganId());
        List<HisSettleReqDTO> hisSettleReqDTOs = new ArrayList<>();
        for (RecipeOrder order : orders) {
            if (StringUtils.isEmpty(order.getHisSettlementNo())) {
                continue;
            }
            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
            Recipe recipe = recipeDAO.get(recipeIdList.get(0));
            if (Objects.isNull(recipe)) {
                continue;
            }
            HisSettleReqDTO hisSettleReqDTO = new HisSettleReqDTO();
            if (Objects.nonNull(order.getPreSettletotalAmount())) {
                hisSettleReqDTO.setAmount(BigDecimal.valueOf(order.getPreSettletotalAmount()));
            }
            hisSettleReqDTO.setSettleNo(order.getHisSettlementNo());
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeIdList.get(0));
            if (Objects.nonNull(recipeExtend)) {
                hisSettleReqDTO.setHisRegNo(recipeExtend.getRegisterID());
            }
            String mrnForRecipe = getMrnForRecipe(recipe, recipeExtend);
            hisSettleReqDTO.setCardNo(mrnForRecipe);
            PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
            hisSettleReqDTO.setPatientDTO(patientDTO);
            hisSettleReqDTO.setPatientID(recipe.getPatientID());
            hisSettleReqDTO.setDoctorId(recipe.getDoctor());
            hisSettleReqDTO.setDoctorName(recipe.getDoctorName());
            hisSettleReqDTOs.add(hisSettleReqDTO);
        }
        hisSettleReqTo.setData(hisSettleReqDTOs);
        List<HisSettleResTo> hisSettleResTos = recipeHisClient.queryHisSettle(hisSettleReqTo);
        if (CollectionUtils.isEmpty(hisSettleResTos)) {
            return null;
        }
        Map<String, HisSettleResTo> hisSettleResToMap = hisSettleResTos.stream().filter(k -> StringUtils.isNotEmpty(k.getHisSettlementNo())).collect(Collectors.toMap(k -> k.getHisSettlementNo(), a -> a, (k1, k2) -> k1));
        if (MapUtils.isEmpty(hisSettleResToMap)) {
            return null;
        }
        List<String> list = new ArrayList<>();
        orders.forEach(order -> {
            HisSettleResTo hisSettleResTo = hisSettleResToMap.get(order.getHisSettlementNo());
            if (Objects.nonNull(hisSettleResTo)) {
                setHisSettleResult(order, hisSettleResTo);
                list.add(order.getOrderCode());
            }

        });

        return list;
    }

    /**
     * 合并物流
     * @param order 订单
     */
    @LogRecord
    public String getMergeTrackingNumber(RecipeOrder order) {
        try {
            if (Objects.isNull(order)) {
                return null;
            }
            if (Objects.isNull(order.getEnterpriseId())) {
                return null;
            }
            DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (!DrugEnterpriseConstant.LOGISTICS_PLATFORM.equals(enterprise.getLogisticsType())) {
                return null;
            }
            if (logisticsMergeFlagEnum.LOGISTICS_MERGE_NO_SUPPORT.getType().equals(enterprise.getLogisticsMergeFlag())) {
                return null;
            }
            String logisticsMergeTime = enterprise.getLogisticsMergeTime();
            if (StringUtils.isEmpty(logisticsMergeTime)) {
                return null;
            }
            String designateDateStr = DateConversion.getDesignateDate(logisticsMergeTime);
            //获取查询时间段
            Date designateDate = DateConversion.parseDate(designateDateStr, DateConversion.DEFAULT_DATE_TIME);
            Date startDate;
            if (new Date().getTime() >= designateDate.getTime()) {
                startDate = designateDate;
            } else {
                startDate = DateConversion.getDateAftXDays(designateDate, -1);
            }
            //获取用户mpiId
            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
            List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
            String mpiId = recipes.get(0).getRequestMpiId();
            List<RecipeOrder> canMergeOrder = recipeOrderDAO.findCanMergeRecipeOrder(mpiId, startDate, new Date());
            List<RecipeOrder> mergeOrderList = canMergeOrder.stream().filter(recipeOrder -> getCompleteAddress(recipeOrder).equals(getCompleteAddress(order))
                    && recipeOrder.getRecMobile().equals(order.getRecMobile()) && recipeOrder.getReceiver().equals(order.getReceiver())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(mergeOrderList)) {
                return null;
            }
            return mergeOrderList.get(0).getTrackingNumber();
        } catch (Exception e) {
            logger.error("OrderManager getMergeTrackingNumber recipeId:{}, error ", JSON.toJSONString(order.getRecipeIdList()), e);
        }
        return null;
    }

    /**
     * 保存结算信息
     *
     * @param order
     * @param hisSettleResTo
     */
    private void setHisSettleResult(RecipeOrder order, HisSettleResTo hisSettleResTo) {
        if (Objects.nonNull(hisSettleResTo.getPreSettleTotalAmount())) {
            order.setPreSettletotalAmount(hisSettleResTo.getPreSettleTotalAmount().doubleValue());
        }
        if (Objects.nonNull(hisSettleResTo.getCashAmount())) {
            order.setCashAmount(hisSettleResTo.getCashAmount().doubleValue());
        }
        if (Objects.nonNull(hisSettleResTo.getFundAmount())) {
            order.setFundAmount(hisSettleResTo.getFundAmount().doubleValue());
        }
        if (Objects.nonNull(hisSettleResTo.getSettleMode())) {
            order.setSettleMode(hisSettleResTo.getSettleMode());
        } else {
            order.setSettleMode(1);
        }
        if (Objects.nonNull(hisSettleResTo.getIsMedicalSettle()) && new Integer(1).equals(hisSettleResTo.getIsMedicalSettle())) {
            // RecipeOrderTypeEnum
            order.setOrderType(4);
        }
        if (StringUtils.isNotEmpty(hisSettleResTo.getOutTradeNo())) {
            order.setOutTradeNo(hisSettleResTo.getOutTradeNo());
        }
        if (StringUtils.isNotEmpty(hisSettleResTo.getTradeNo())) {
            order.setOutTradeNo(hisSettleResTo.getTradeNo());
        }
        if (Objects.nonNull(hisSettleResTo.getPayTime())) {
            order.setPayTime(hisSettleResTo.getPayTime());
        } else {
            order.setPayTime(new Date());
        }
        order.setPayFlag(1);
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(order);

    }

    /**
     * 获取就诊卡号
     *
     * @param recipe
     * @param recipeExtend
     * @return
     */
    private String getMrnForRecipe(Recipe recipe, RecipeExtend recipeExtend) {
        String mrn = null;
        if (Objects.nonNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
            mrn = recipeExtend.getCardNo();
        } else {
            //复诊
            if (new Integer(2).equals(recipe.getBussSource())) {
                IRevisitExService revisitExService = RevisitAPI.getService(IRevisitExService.class);
                if (recipe.getClinicId() != null) {
                    RevisitExDTO revisitExDTO = revisitExService.getByConsultId(recipe.getClinicId());
                    if (revisitExDTO != null && revisitExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = revisitExDTO.getCardId();
                    }
                }
                //咨询
            } else if (new Integer(1).equals(recipe.getBussSource())) {
                IConsultExService consultExService = ConsultAPI.getService(IConsultExService.class);
                if (recipe.getClinicId() != null) {
                    ConsultExDTO consultExDTO = consultExService.getByConsultId(recipe.getClinicId());
                    if (consultExDTO != null && consultExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = consultExDTO.getCardId();
                    }
                }
            }
        }

        //-1表示获取不到身份证，默认用身份证获取患者信息
        if (StringUtils.isEmpty(mrn)) {
            mrn = "-1";
        }
        return mrn;
    }


}
