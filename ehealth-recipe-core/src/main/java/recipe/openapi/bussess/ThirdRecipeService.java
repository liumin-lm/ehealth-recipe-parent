package recipe.openapi.bussess;

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
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.openapi.bussess.bean.RecipeAndRecipeDetailsBean;
import recipe.openapi.bussess.bean.ThirdRecipeDetailBean;
import recipe.openapi.bussess.request.*;
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
    @RpcService
    public List<RecipeAndRecipeDetailsBean> findRecipesForPatientAndTabStatus(ThirdGetRecipeDetailRequest request){
        LOGGER.info("ThirdRecipeService.findRecipesForPatientAndTabStatus request:{}.", JSONUtils.toString(request));
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
        return recipeAndRecipeDetailsBeans;
    }

    /**
     * 获取处方单详情信息
     * @param request 请求对象
     * @return 返回处方详情信息
     */
    @RpcService
    public Map<String, Object> getPatientRecipeById(ThirdRecipeDetailRequest request){
        LOGGER.info("ThirdRecipeService.getPatientRecipeById request:{}.", JSONUtils.toString(request));
        Assert.hasLength(request.getTid(), "getPatientRecipeById 用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        Map<String, Object> result = RecipeServiceSub.getRecipeAndDetailByIdImpl(request.getRecipeId(), false);
        PatientDTO patient = (PatientDTO) result.get("patient");
        result.put("patient", ObjectCopyUtils.convert(patient, PatientDS.class));
        return result;
    }

    /**
     * 药企列表查询
     * @param request 请求参数
     * @return 返回可供药企列表
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(ThirdGetDepListRequest request) {
        LOGGER.info("ThirdRecipeService.filterSupportDepList request:{}.", JSONUtils.toString(request));
        Assert.hasLength(request.getTid(), "filterSupportDepList 用户tid为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        return purchaseService.filterSupportDepList(request.getRecipeId(), Arrays.asList(request.getPayMode()), request.getFilterConditions());
    }

    /**
     * 获取收货地址列表
     * @param request 请求参数
     * @return 返回地址列表
     */
    @RpcService
    public List<AddressDTO> findByMpiIdOrderSelf(ThirdBaseRequest request){
        LOGGER.info("ThirdRecipeService.findByMpiIdOrderSelf request:{}.", JSONUtils.toString(request));
        Assert.hasLength(request.getTid(), "findByMpiIdOrderSelf 用户tid为空!");
        setUrtToContext(request.getAppkey(), request.getTid());
        String mpiId = getOwnMpiId();
        AddressService addressService = BasicAPI.getService(AddressService.class);
        if (StringUtils.isNotEmpty(mpiId)) {
            return addressService.findByMpiIdOrderSelf(mpiId);
        }
        return new ArrayList<>();
    }

    /**
     * 新增收货地址
     * @param request 地址参数
     * @return 新增地址编号
     */
    @RpcService
    public Integer addAddress(ThirdSetAddressRequest request) {
        LOGGER.info("ThirdRecipeService.addAddress request:{}.", JSONUtils.toString(request));
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
        LOGGER.info("ThirdRecipeService.createOrder request:{}.", JSONUtils.toString(request));
        checkOrderParams(request);
        setUrtToContext(request.getAppkey(), request.getTid());
        String mpiId = getOwnMpiId();
        //查询处方是否存在
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (recipe != null) {
            RecipeOrder order = new RecipeOrder();
            order.setMpiId(mpiId);
            order.setOrganId(recipe.getClinicOrgan());
            order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
            order.setAddressID(Integer.parseInt(request.getRecipeOrder().getAddressId()));
            order.setWxPayWay(request.getRecipeOrder().getPayway());
            if (StringUtils.isNotEmpty(request.getRecipeOrder().getDepId())) {
                order.setEnterpriseId(Integer.parseInt(request.getRecipeOrder().getDepId()));
            }
            if (request.getRecipeOrder().getExpressFee() != null) {
                order.setExpressFee(new BigDecimal(request.getRecipeOrder().getExpressFee()));
            }
            if (StringUtils.isNotEmpty(request.getRecipeOrder().getGysCode())) {
                order.setDrugStoreCode(request.getRecipeOrder().getGysCode());
            }
            return recipeOrderDAO.save(order).getOrderId();
        }
        return 0;
    }

    /**
     * 第三方处方支付回调
     * @param request 请求参数
     * @return  是否成功
     */
    @RpcService
    public Integer recipePayCallBack(ThirdPayCallBackRequest request){
        LOGGER.info("ThirdRecipeService.recipePayCallBack request:{}.", JSONUtils.toString(request));
        checkPayCallBackParams(request);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.get(request.getOrderId());
        HashMap<String, Object> attr = new HashMap<>();
        attr.put("payFlag", request.getPayFlag());
        attr.put("outTradeNo", request.getOutTradeNo());
        attr.put("tradeNo", request.getTradeNo());
        attr.put("payway", request.getPayway());
        attr.put("actualPrice", new BigDecimal(request.getTotalAmount()));
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
        LOGGER.info("ThirdRecipeService.updateRecipeStatus request:{}.", JSONUtils.toString(request));
        Assert.notNull(request.getTid(), "用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        Assert.notNull(request.getStatus(), "处方状态为空!");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(request.getRecipeId());
        if (recipe != null) {
            recipe.setStatus(recipe.getStatus());
            recipe.setLastModify(new Date());
            recipeDAO.update(recipe);
            return 1;
        }
        return 0;
    }

    private void checkOrderParams(ThirdSaveOrderRequest request){
        Assert.notNull(request.getTid(), "用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        Assert.hasLength(request.getRecipeOrder().getAddressId(), "收货地址为空!");
        Assert.hasLength(request.getRecipeOrder().getPayway(), "支付类型为空!");
        Assert.hasLength(request.getRecipeOrder().getDecoctionFlag(), "代煎方式为空!");
        Assert.hasLength(request.getRecipeOrder().getDepId(), "药企ID为空!");
    }

    /**
     * 校验支付回调入参
     * @param request 入参数据
     */
    private void checkPayCallBackParams(ThirdPayCallBackRequest request){
        Assert.notNull(request.getTid(), "用户tid为空!");
        Assert.notNull(request.getRecipeId(), "处方单ID为空!");
        Assert.notNull(request.getOrderId(), "定单号为空!");
        Assert.hasLength(request.getPayFlag(), "支付状态为空!");
        Assert.hasLength(request.getOutTradeNo(), "平台流水号为空!");
        Assert.hasLength(request.getTradeNo(), "支付流水号为空!");
        Assert.notNull(request.getTotalAmount(), "支付总金额为空!");
        Assert.hasLength(request.getPayway(), "支付类型代码为空!");
    }

    /**
     * 校验第三方入参数据
     * @param request 入参数据
     */
    private void checkThirdAddressParams(ThirdSetAddressRequest request) {
        Assert.notNull(request.getTid(), "用户tid为空!");
        Assert.hasLength(request.getReceiver(), "收货人为空!");
        Assert.hasLength(request.getRecMobile(), "收货人手机号为空!");
        Assert.hasLength(request.getAddress1(), "地址（省）为空!");
        Assert.hasLength(request.getAddress2(), "地址（市）为空!");
        Assert.hasLength(request.getAddress3(), "地址（区县）为空!");
        Assert.hasLength(request.getStreetAddress(), "地址（街道）为空!");
        Assert.hasLength(request.getAddress4(), "详细地址为空!");
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
}
