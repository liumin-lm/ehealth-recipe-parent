package recipe.openapi.business;

import com.ngari.infra.logistics.mode.LogisticsOrderDetailsDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.patient.ds.PatientDS;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.account.UserRoleToken;
import ctd.account.thirdparty.ThirdPartyMappingController;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.openapi.business.bean.RecipeAndRecipeDetailsBean;
import recipe.openapi.business.bean.ThirdRecipeDetailBean;
import recipe.openapi.business.request.*;
import recipe.purchase.PurchaseService;
import recipe.service.RecipeListService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author yinsheng
 * @date 2020\9\18 0018 15:16
 */
@RpcBean(value = "thirdRecipeService", mvc_authentication = false)
public class ThirdRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdRecipeService.class);

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    /**
     * 根据处方状态查询处方信息
     * @param request
     *        appkey 固定值  纳里提供
     *        tid    第三方平台用户主键
     *        tabStatus 状态标识
     *        index  分页起始位置
     *        limit  每页查询量
     * @return 处方和处方详情
     */
    @Deprecated
    @RpcService
    public List<RecipeAndRecipeDetailsBean> findRecipesForPatientAndTabStatus(ThirdGetRecipeDetailRequest request){
        LOGGER.info("ThirdRecipeService findRecipesForPatientAndTabStatus request:{}.", JSONUtils.toString(request));
        List<RecipeAndRecipeDetailsBean> recipeAndRecipeDetailsBeans = new ArrayList<>();
        Assert.hasLength(request.getTid(), "findRecipesForPatientAndTabStatus 用户tid为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        String mpiId = getOwnMpiId();
        RecipeListService recipeListService = ApplicationUtils.getRecipeService(RecipeListService.class);
        List<PatientTabStatusRecipeDTO> patientTabStatusRecipeDTOS = recipeListService.findRecipesForPatientAndTabStatus(request.getTabStatus(), mpiId, request.getIndex(), request.getLimit());

        for (PatientTabStatusRecipeDTO patientTabStatusRecipeDTO : patientTabStatusRecipeDTOS) {
            RecipeAndRecipeDetailsBean recipeAndRecipeDetailsBean = new RecipeAndRecipeDetailsBean();
            recipeAndRecipeDetailsBean.setRecipeId(patientTabStatusRecipeDTO.getRecipeId());
            recipeAndRecipeDetailsBean.setPatientName(patientTabStatusRecipeDTO.getPatientName());
            recipeAndRecipeDetailsBean.setPhoto(patientTabStatusRecipeDTO.getPhoto());
            recipeAndRecipeDetailsBean.setPatientSex(patientTabStatusRecipeDTO.getPatientSex());
            recipeAndRecipeDetailsBean.setOrganDiseaseName(patientTabStatusRecipeDTO.getOrganDiseaseName());
            recipeAndRecipeDetailsBean.setSignDate(patientTabStatusRecipeDTO.getSignDate());
            recipeAndRecipeDetailsBean.setTotalMoney(patientTabStatusRecipeDTO.getTotalMoney().doubleValue());
            recipeAndRecipeDetailsBean.setStatusText(patientTabStatusRecipeDTO.getStatusText());
            recipeAndRecipeDetailsBean.setStatusCode(patientTabStatusRecipeDTO.getStatusCode());
            recipeAndRecipeDetailsBean.setRecipeSurplusHours(patientTabStatusRecipeDTO.getRecipeSurplusHours());
            recipeAndRecipeDetailsBean.setRecipeType(patientTabStatusRecipeDTO.getRecipeType());
            recipeAndRecipeDetailsBean.setLogisticsCompany(patientTabStatusRecipeDTO.getLogisticsCompany());
            recipeAndRecipeDetailsBean.setTrackingNumber(patientTabStatusRecipeDTO.getTrackingNumber());
            List<ThirdRecipeDetailBean> recipeDetailBeans = new ArrayList<>();
            for (RecipeDetailBean recipeDetailBean : patientTabStatusRecipeDTO.getRecipeDetail()) {
                ThirdRecipeDetailBean thirdRecipeDetailBean = ObjectCopyUtils.convert(recipeDetailBean, ThirdRecipeDetailBean.class);
                recipeDetailBeans.add(thirdRecipeDetailBean);
            }
            recipeAndRecipeDetailsBean.setRecipeDetailBeans(recipeDetailBeans);
            recipeAndRecipeDetailsBeans.add(recipeAndRecipeDetailsBean);
        }
        LOGGER.info("ThirdRecipeService findRecipesForPatientAndTabStatus recipeAndRecipeDetailsBeans:{}.", JSONUtils.toString(recipeAndRecipeDetailsBeans));
        return recipeAndRecipeDetailsBeans;
    }

    /**
     * 获取处方单详情信息
     * @param request 请求对象
     * @return 返回处方详情信息
     */
    @RpcService
    public Map<String, Object> getPatientRecipeById(ThirdRecipeDetailRequest request){
        LOGGER.info("ThirdRecipeService getPatientRecipeById request:{}.", JSONUtils.toString(request));
        Assert.hasLength(request.getTid(), "getPatientRecipeById 用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (recipe != null) {
            checkUserHasPermission(recipe.getRecipeId());
        }
        Map<String, Object> result = RecipeServiceSub.getRecipeAndDetailByIdImpl(request.getRecipeId(), false);
        PatientDTO patient = (PatientDTO) result.get("patient");
        result.put("patient", ObjectCopyUtils.convert(patient, PatientDS.class));
        LOGGER.info("ThirdRecipeService getPatientRecipeById result:{}.", JSONUtils.toString(result));
        return result;
    }

    /**
     * 药企列表查询
     * @param request 请求参数
     * @return 返回可供药企列表
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(ThirdGetDepListRequest request) {
        LOGGER.info("ThirdRecipeService filterSupportDepList request:{}.", JSONUtils.toString(request));
        Assert.hasLength(request.getTid(), "filterSupportDepList 用户tid为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        RecipeResultBean  resultBean = purchaseService.filterSupportDepList(Arrays.asList(request.getRecipeId()), Arrays.asList(request.getPayMode()), request.getFilterConditions());
        LOGGER.info("ThirdRecipeService filterSupportDepList resultBean:{}.", JSONUtils.toString(resultBean));
        return resultBean;
    }

    /**
     * 获取收货地址列表
     * @param request 请求参数
     * @return 返回地址列表
     */
    @RpcService
    public List<AddressDTO> findByMpiIdOrderSelf(ThirdBaseRequest request){
        LOGGER.info("ThirdRecipeService findByMpiIdOrderSelf request:{}.", JSONUtils.toString(request));
        List<AddressDTO> addressDTOS = new ArrayList<>();
        Assert.hasLength(request.getTid(), "findByMpiIdOrderSelf 用户tid为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        String mpiId = getOwnMpiId();
        AddressService addressService = BasicAPI.getService(AddressService.class);
        if (StringUtils.isNotEmpty(mpiId)) {
            addressDTOS = addressService.findByMpiIdOrderSelf(mpiId);
        }
        LOGGER.info("ThirdRecipeService findByMpiIdOrderSelf addressDTOS:{}.", JSONUtils.toString(addressDTOS));
        return addressDTOS;
    }

    /**
     * 新增收货地址
     * @param request 地址参数
     * @return 新增地址编号
     */
    @RpcService
    public Integer addAddress(ThirdSetAddressRequest request) {
        LOGGER.info("ThirdRecipeService addAddress request:{}.", JSONUtils.toString(request));
        checkThirdAddressParams(request);
        setUrtToContext(request.getAppkey(), request.getTid());
        String mpiId = getOwnMpiId();
        if (StringUtils.isNotEmpty(mpiId)) {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setMpiId(mpiId);
            addressDTO.setReceiver(request.getReceiver());
            addressDTO.setRecMobile(request.getRecMobile());
            addressDTO.setRecTel(request.getRecTel());
            addressDTO.setAddress1(request.getAddress1());
            addressDTO.setAddress2(request.getAddress2());
            addressDTO.setAddress3(request.getAddress3());
            addressDTO.setStreetAddress(request.getStreetAddress());
            addressDTO.setAddress4(request.getAddress4());
            addressDTO.setZipCode(request.getZipCode());
            addressDTO.setCreateDt(request.getCreateDt()==null?new Date():request.getCreateDt());
            addressDTO.setLastModify(request.getLastModify()==null?new Date():request.getLastModify());
            AddressService addressService = BasicAPI.getService(AddressService.class);
            return addressService.addAddress(addressDTO);
        }
        return -1;
    }

    /**
     * 创建订单
     * @param request 订单请求参数
     * @return 订单ID
     */
    @RpcService
    public Integer createOrder(ThirdSaveOrderRequest request) {
        LOGGER.info("ThirdRecipeService createOrder request:{}.", JSONUtils.toString(request));
        try {
            checkOrderParams(request);
            setUrtToContext(request.getAppkey(), request.getTid());
            String mpiId = getOwnMpiId();
            //查询处方是否存在
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
            if (StringUtils.isEmpty(recipe.getOrderCode())) {
                RecipeOrder order = new RecipeOrder();
                order.setMpiId(mpiId);
                order.setOrganId(recipe.getClinicOrgan());
                order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
                if ("1".equals(request.getRecipeOrder().getSendMethod())) {
                    //设置配送到家的待配送状态
                    order.setStatus(3);
                } else if ("2".equals(request.getRecipeOrder().getSendMethod())) {
                    //设置到院取药的状态
                    order.setStatus(2);
                } else if ("3".equals(request.getRecipeOrder().getSendMethod())) {
                    //设置到店取药的待取药状态
                    order.setStatus(12);
                } else {
                    //设置到院取药的状态
                    order.setStatus(2);
                }
                //设置配送信息
                if (StringUtils.isNotEmpty(request.getRecipeOrder().getAddressId())) {
                    order.setAddressID(Integer.parseInt(request.getRecipeOrder().getAddressId()));
                    AddressService addressService = BasicAPI.getService(AddressService.class);
                    AddressDTO addressDTO = addressService.getByAddressId(Integer.parseInt(request.getRecipeOrder().getAddressId()));
                    if (addressDTO != null) {
                        order.setAddress1(addressDTO.getAddress1());
                        order.setAddress2(addressDTO.getAddress2());
                        order.setAddress3(addressDTO.getAddress3());
                        order.setAddress4(addressDTO.getAddress4());
                        order.setReceiver(addressDTO.getReceiver());
                        order.setRecMobile(addressDTO.getRecMobile());
                    }
                }
                order.setWxPayWay(request.getRecipeOrder().getPayway());
                if (StringUtils.isNotEmpty(request.getRecipeOrder().getDepId())) {
                    order.setEnterpriseId(Integer.parseInt(request.getRecipeOrder().getDepId()));
                }
                if (StringUtils.isNotEmpty(request.getRecipeOrder().getGysCode())) {
                    order.setDrugStoreCode(request.getRecipeOrder().getGysCode());
                }
                order.setEffective(1);
                order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipe.getRecipeId())));
                order.setPayFlag(0);
                //设置订单各个费用
                setOrderFee(order, recipe ,request);
                order.setWxPayWay(request.getRecipeOrder().getPayway());
                order.setCreateTime(new Date());
                order.setPayTime(new Date());
                order.setPushFlag(1);
                order.setSendTime(new Date());
                order.setLastModifyTime(new Date());
                order.setOrderType(0);
                RecipeOrder recipeOrder = recipeOrderDAO.save(order);
                LOGGER.info("ThirdRecipeService createOrder recipeOrder:{}.", JSONUtils.toString(recipeOrder));
                if (recipeOrder != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderCode", recipeOrder.getOrderCode());
                    map.put("enterpriseId", recipeOrder.getEnterpriseId());
                    recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), map);
                    return recipeOrder.getOrderId();
                }
            } else {
                RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                return recipeOrder.getOrderId();
            }
        } catch (NumberFormatException e) {
            LOGGER.info("ThirdRecipeService createOrder NumberFormatException recipeId:{}.", request.getRecipeId(), e);
        } catch (DAOException e) {
            LOGGER.info("ThirdRecipeService createOrder DAOException recipeId:{}.", request.getRecipeId(), e);
        }
        return 0;
    }

    /**
     * 根据订单查询物流信息
     * @param request 物流参数
     * @return 订单物流轨迹
     */
    public LogisticsOrderDetailsDto getLogisticsOrderByOrderId(ThirdLogisticsRequest request){
        LOGGER.info("ThirdRecipeService getLogisticsOrderByOrderId request:{}.", JSONUtils.toString(request));
        checkLogisticsParams(request);
        setUrtToContext(request.getAppkey(), request.getTid());
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(request.getOrderId());
        if (null == recipeOrder) {
            throw new DAOException(609, "订单不存在");
        }
        ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
        LogisticsOrderDetailsDto logisticsOrderDetailsDto = logisticsOrderService.getLogisticsOrderByBizNo(1, recipeOrder.getOrderCode());
        LOGGER.info("ThirdRecipeService getLogisticsOrderByOrderId logisticsOrderDetailsDto:{}.", JSONUtils.toString(logisticsOrderDetailsDto));
        return logisticsOrderDetailsDto;
    }

    private void setOrderFee(RecipeOrder order, Recipe recipe, ThirdSaveOrderRequest request) {
        //设置挂号费
        if (request.getRecipeOrder().getRegisterFee() != null && request.getRecipeOrder().getRegisterFee() > 0.0) {
            order.setRegisterFee(new BigDecimal(request.getRecipeOrder().getRegisterFee()));
        } else {
            order.setRegisterFee(BigDecimal.ZERO);
        }
        //设置快递费
        if (request.getRecipeOrder().getExpressFee() != null && request.getRecipeOrder().getExpressFee() > 0.0) {
            order.setExpressFee(new BigDecimal(request.getRecipeOrder().getExpressFee()));
        } else {
            order.setExpressFee(BigDecimal.ZERO);
        }
        //设置代煎费
        if (request.getRecipeOrder().getDecoctionFee() != null && request.getRecipeOrder().getDecoctionFee() > 0.0) {
            order.setDecoctionFee(new BigDecimal(request.getRecipeOrder().getDecoctionFee()));
        } else {
            order.setDecoctionFee(BigDecimal.ZERO);
        }
        //设置审方费
        if (request.getRecipeOrder().getAuditFee() != null && request.getRecipeOrder().getAuditFee() > 0.0) {
            order.setAuditFee(new BigDecimal(request.getRecipeOrder().getAuditFee()));
        } else {
            order.setAuditFee(BigDecimal.ZERO);
        }
        //设置处方费
        if (request.getRecipeOrder().getRecipeFee() != null && request.getRecipeOrder().getRecipeFee() > 0.0) {
            order.setRecipeFee(new BigDecimal(request.getRecipeOrder().getRecipeFee()));
        } else {
            if (recipe.getTotalMoney() != null) {
                order.setRecipeFee(recipe.getTotalMoney());
            } else {
                order.setRecipeFee(BigDecimal.ZERO);
            }
        }
        //设置优惠费用
        order.setCouponId(0);
        order.setCouponFee(BigDecimal.ZERO);
        //设置总费用
        order.setTotalFee(order.getRegisterFee().add(order.getExpressFee()).add(order.getDecoctionFee()).add(order.getAuditFee().add(order.getRecipeFee())));
        //设置实际支付
        order.setActualPrice(order.getTotalFee().doubleValue());
    }

    /**
     * 第三方处方支付回调
     * @param request 请求参数
     * @return  是否成功
     */
    @RpcService
    public Integer recipePayCallBack(ThirdPayCallBackRequest request){
        LOGGER.info("ThirdRecipeService recipePayCallBack request:{}.", JSONUtils.toString(request));
        checkPayCallBackParams(request);
        setUrtToContext(request.getAppkey(), request.getTid());
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (recipe != null) {
            checkUserHasPermission(recipe.getRecipeId());
        }
        RecipeOrder order = recipeOrderDAO.get(request.getOrderId());
        HashMap<String, Object> attr = new HashMap<>();
        attr.put("payFlag", Integer.parseInt(request.getPayFlag()));
        attr.put("outTradeNo", request.getOutTradeNo());
        attr.put("tradeNo", request.getTradeNo());
        attr.put("wxPayWay", request.getPayway());
        attr.put("actualPrice", request.getTotalAmount());
        if (request.getFundAmount() != null) {
            attr.put("orderType", 1);  //医保支付
            attr.put("fundAmount", new BigDecimal(request.getFundAmount()));
            attr.put("cashAmount", new BigDecimal(request.getCashAmount()));
        }
        RecipeResultBean result = RecipeResultBean.getSuccess();
        orderService.updateOrderInfo(order.getOrderCode(), attr, result);
        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * 更新处方状态
     * @param request 请求参数
     * @return 是否更新成功
     */
    @RpcService
    public Integer updateRecipeStatus(ThirdUpdateRecipeRequest request) {
        LOGGER.info("ThirdRecipeService updateRecipeStatus request:{}.", JSONUtils.toString(request));
        Assert.notNull(request.getTid(), "用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        Assert.notNull(request.getStatus(), "处方状态为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (recipe != null) {
            checkUserHasPermission(recipe.getRecipeId());
            recipe.setStatus(request.getStatus());
            recipeDAO.update(recipe);
            return 1;
        }
        return 0;
    }

    private void checkLogisticsParams(ThirdLogisticsRequest request){
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (request.getOrderId() == null) {
            throw new DAOException(609, "定单ID为空");
        }
    }

    private void checkOrderParams(ThirdSaveOrderRequest request){
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (request.getRecipeId() == null) {
            throw new DAOException(609, "处方单ID为空");
        }
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (null == recipe) {
            throw new DAOException(609, "不存在的处方单");
        }
        if (StringUtils.isEmpty(request.getRecipeOrder().getPayway())) {
            throw new DAOException(609, "支付类型为空");
        }
    }

    /**
     * 校验支付回调入参
     * @param request 入参数据
     */
    private void checkPayCallBackParams(ThirdPayCallBackRequest request){
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (request.getRecipeId() == null) {
            throw new DAOException(609, "处方单ID为空");
        }
        if (request.getOrderId() == null) {
            throw new DAOException(609, "定单号为空");
        }
        if (StringUtils.isEmpty(request.getPayFlag())) {
            throw new DAOException(609, "支付状态为空");
        }
        if (StringUtils.isEmpty(request.getOutTradeNo())) {
            throw new DAOException(609, "平台流水号为空");
        }
        if (StringUtils.isEmpty(request.getTradeNo())) {
            throw new DAOException(609, "支付流水号为空");
        }
        if (request.getTotalAmount() == null) {
            throw new DAOException(609, "支付总金额为空");
        }
        if (StringUtils.isEmpty(request.getPayway())) {
            throw new DAOException(609, "支付类型代码为空");
        }
    }

    /**
     * 校验第三方入参数据
     * @param request 入参数据
     */
    private void checkThirdAddressParams(ThirdSetAddressRequest request) {
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (StringUtils.isEmpty(request.getReceiver())) {
            throw new DAOException(609, "收货人为空");
        }
        if (StringUtils.isEmpty(request.getRecMobile())) {
            throw new DAOException(609, "收货人手机号为空");
        }
        if (StringUtils.isEmpty(request.getAddress1())) {
            throw new DAOException(609, "地址（省）为空");
        }
        if (StringUtils.isEmpty(request.getAddress2())) {
            throw new DAOException(609, "地址（市）为空");
        }
        if (StringUtils.isEmpty(request.getAddress3())) {
            throw new DAOException(609, "地址（区县）为空");
        }
        if (StringUtils.isEmpty(request.getStreetAddress())) {
            throw new DAOException(609, "地址（街道）为空");
        }
        if (StringUtils.isEmpty(request.getAddress4())) {
            throw new DAOException(609, "详细地址为空");
        }
    }

    /**
     * 模拟登录
     * @param thirdParty 第三方标识
     * @param tid 第三方唯一标识
     */
    private void setUrtToContext(String thirdParty, String tid){
        ThirdPartyMappingController.instance().setUrtToContext(thirdParty, tid);
    }

    /**
     * 获取当前登录用户的MPIID
     * @return 当前用户的MPIID
     */
    private String getOwnMpiId(){
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        LOGGER.info("ThirdRecipeService.getOwnMpiId userRoleToken:{}.", JSONUtils.toString(userRoleToken));
        return userRoleToken.getOwnMpiId();
    }

    private void checkUserHasPermission(Integer recipeId){
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        UserRoleToken urt = UserRoleToken.getCurrent();
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (recipe != null){
            if ((urt.isPatient() && patientService.isPatientBelongUser(recipe.getMpiid()))||(urt.isDoctor() && urt.isSelfDoctor(recipe.getDoctor()))) {
                return;
            }else{
                LOGGER.error("当前用户没有权限调用recipeId[{}],methodName[{}]", recipeId ,methodName);
                throw new DAOException("当前登录用户没有权限");
            }
        }
    }
}
