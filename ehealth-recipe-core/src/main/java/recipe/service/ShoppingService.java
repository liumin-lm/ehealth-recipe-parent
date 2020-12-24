package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.address.model.AddressBean;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.entity.ShoppingDrug;
import com.ngari.recipe.entity.ShoppingOrder;
import com.ngari.recipe.service.IShoppingService;
import com.ngari.recipe.shoppingorder.model.ShoppingDrugDTO;
import com.ngari.recipe.shoppingorder.model.ShoppingOrderDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.ShoppingDrugDAO;
import recipe.dao.ShoppingOrderDAO;
import recipe.service.common.RecipeCacheService;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.net.URLEncoder;
import java.util.*;

/**
 * 健康商城服务类
 *
 * @author:liuya date:2017/12/6
 */
@RpcBean("shoppingService")
public class ShoppingService implements IShoppingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingService.class);

    private static final Integer SHOPPING_ORDER_PAY_FINISH = 1;

    private static final Integer SHOPPING_ORDER_CANCEL = 2;

    private static final Integer SHOPPING_ORDER_START_TRANSPORT = 3;

    private static final String MPI_ID = "mpiId";
    private static final String ORDER_CODE = "orderCode";
    private static final String SALE_TIME = "saleTime";
    private static final String TOTAL_FEE = "totalFee";
    private static final String ACTUAL_FEE = "actualFee";
    private static final String EXPRESS_FEE = "expressFee";
    private static final String DRUG_FEE = "drugFee";
    private static final String COUPON_FEE = "couponFee";
    private static final String PAY_WAY = "payWay";
    private static final String LOGISTICS = "logistics";
    private static final String RECEIVER = "receiver";
    private static final String REC_MOBILE = "recMobile";
    private static final String ADDRESS1 = "address1";
    private static final String ADDRESS2 = "address2";
    private static final String ADDRESS3 = "address3";
    private static final String ADDRESS4 = "address4";
    private static final String DRUGS_INFO = "drugsInfo";
    private static final String DRUG_NAME = "drugName";
    private static final String DRUG_SPEC = "drugSpec";
    private static final String PRODUCER = "producer";
    private static final String PRICE = "price";
    private static final String QUANTITY = "quantity";
    private static final String LOGISTICS_COMPANY = "logisticsCompany";
    private static final String TRACKING_NUMBER = "trackingNumber";
    private static final String CANCEL_TIME = "cancelTime";
    private static final String CANCEL_REASON = "cancelReason";

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private AddressService iAddressService = ApplicationUtils.getBasicService(AddressService.class);

    /**
     * 获取患者信息
     *
     * @param mpiId
     * @return
     */
    @RpcService
    public RecipeBussResTO getUser(String mpiId) {
        LOGGER.info("getUser request={}", mpiId);
        RecipeBussResTO res = new RecipeBussResTO();
        PatientDTO patient = patientService.get(mpiId);
        if (null == patient) {
            res.setCode(0);
            res.setMsg("患者不存在");
        } else {
            //date 2019/12/25
            //调整处方方法调basic
            List<AddressBean> addressList = ObjectCopyUtils.convert(iAddressService.findByMpiId(mpiId), AddressBean.class);
            Map<String, Object> user = Maps.newHashMap();
            user.put("name", patient.getPatientName());
            user.put("address", addressList);
            res.setCode(200);
            res.setMsg("");
            res.setUser(user);
        }
        LOGGER.info("getUser response={}", res);
        return res;
    }

    /**z
     * 健康商城完成支付后回调接口
     *
     * @param request
     * @return
     */
    @RpcService
    public RecipeBussResTO finishOrder(Map<String, Object> request) {
        LOGGER.info("finishOrder request={}", request);
        RecipeBussResTO res = new RecipeBussResTO();
        if (null == request) {
            res.setCode(0);
            res.setMsg("请填写入参");
            return res;
        }
        //校验入参
        validateOrder(request);
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingDrugDAO drugDAO = DAOFactory.getDAO(ShoppingDrugDAO.class);

        ShoppingOrder order = new ShoppingOrder();
        //解析入参
        order.setMpiId(MapValueUtil.getString(request, MPI_ID));
        PatientDTO patient = patientService.get(order.getMpiId());
        order.setPatientName(patient.getPatientName());
        order.setOrderCode(MapValueUtil.getString(request, ORDER_CODE));
        order.setSaleTime(DateConversion.getCurrentDate(MapValueUtil.getString(request, SALE_TIME), DateConversion.DEFAULT_DATE_TIME));
        order.setTotalFee(MapValueUtil.getBigDecimal(request, TOTAL_FEE));
        order.setActualFee(MapValueUtil.getBigDecimal(request, ACTUAL_FEE));
        order.setExpressFee(MapValueUtil.getBigDecimal(request, EXPRESS_FEE));
        order.setDrugFee(MapValueUtil.getBigDecimal(request, DRUG_FEE));
        order.setCouponFee(MapValueUtil.getBigDecimal(request, COUPON_FEE));
        order.setPayWay(MapValueUtil.getInteger(request, PAY_WAY));
        order.setStatus(SHOPPING_ORDER_PAY_FINISH);
        Map<String, Object> logistics = (Map<String, Object>) MapValueUtil.getObject(request, LOGISTICS);
        String receiver = MapValueUtil.getString(logistics, RECEIVER);
        String recMobile = MapValueUtil.getString(logistics, REC_MOBILE);
        String address1 = MapValueUtil.getString(logistics, ADDRESS1);
        String address2 = MapValueUtil.getString(logistics, ADDRESS2);
        String address3 = MapValueUtil.getString(logistics, ADDRESS3);
        String address4 = MapValueUtil.getString(logistics, ADDRESS4);
        order.setReceiver(receiver);
        order.setRecMobile(recMobile);
        order.setAddress(address1 + address2 + address3 + address4);
        order.setCancelTime(null);
        order.setCreateTime(new Date());
        order.setLastModify(new Date());
        ShoppingOrder order1 = orderDAO.getByMpiIdAndOrderCode(order.getMpiId(), order.getOrderCode());
        if (null != order1) {
            res.setCode(0);
            res.setMsg("订单已存在");
            LOGGER.info("finishOrder 保存订单已存在 orderCode={}", order.getOrderCode());
            return res;
        }

        List<Map<String, Object>> drugsInfo = MapValueUtil.getList(request, DRUGS_INFO);
        List<ShoppingDrug> drugs = Lists.newArrayList();
        //保存订单药品信息
        for (Map<String, Object> drugMap : drugsInfo) {
            ShoppingDrug s = new ShoppingDrug();
            s.setOrderCode(order.getOrderCode());
            s.setDrugName(MapValueUtil.getString(drugMap, DRUG_NAME));
            s.setDrugSpec(MapValueUtil.getString(drugMap, DRUG_SPEC));
            s.setProducer(MapValueUtil.getString(drugMap, PRODUCER));
            s.setPrice(MapValueUtil.getBigDecimal(drugMap, PRICE));
            s.setQuantity(MapValueUtil.getBigDecimal(drugMap, QUANTITY));
            drugs.add(s);
        }
        drugDAO.addShoppingDrugList(drugs);
        orderDAO.save(order);
        res.setCode(200);
        res.setMsg("生成订单成功");
        LOGGER.info("finishOrder 成功 orderCode={}", order.getOrderCode());
        return res;
    }

    /**
     * 用户取消订单接口
     *
     * @param request
     * @return
     */
    @RpcService
    public RecipeBussResTO cancelOrder(Map<String, Object> request) {
        LOGGER.info("cancelOrder request={}", request);
        String mpiId = MapValueUtil.getString(request, MPI_ID);
        String orderCode = MapValueUtil.getString(request, ORDER_CODE);
        Date cancelTime = DateConversion.getCurrentDate(MapValueUtil.getString(request, CANCEL_TIME), DateConversion.DEFAULT_DATE_TIME);
        String cancelReason = MapValueUtil.getString(request, CANCEL_REASON);
        RecipeBussResTO res = new RecipeBussResTO();
        if (StringUtils.isEmpty(mpiId)) {
            res.setCode(0);
            res.setMsg("mpiId is null");
            return res;
        }
        if (StringUtils.isEmpty(orderCode)) {
            res.setCode(0);
            res.setMsg("orderCode is null");
            return res;
        }
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingOrder order = orderDAO.getByMpiIdAndOrderCode(mpiId, orderCode);
        if (null == order) {
            res.setCode(0);
            res.setMsg("订单不存在");
            LOGGER.info("cancelOrder 订单不存在 orderCode={}", orderCode);
            return res;
        }
        if (!SHOPPING_ORDER_PAY_FINISH.equals(order.getStatus())) {
            res.setCode(0);
            res.setMsg("该订单不是已完成支付的订单");
            LOGGER.info("cancelOrder 订单状态不为已完成 order={}", order);
            return res;
        }
        order.setCancelTime(cancelTime);
        order.setCancelReason(cancelReason);
        order.setStatus(SHOPPING_ORDER_CANCEL);
        order.setLastModify(new Date());
        orderDAO.update(order);
        res.setCode(200);
        res.setMsg("取消成功");
        LOGGER.info("cancelOrder成功 orderCode={}", orderCode);
        return res;
    }

    /**
     * 开始配送回调接口
     *
     * @param request
     * @return
     */
    @RpcService
    public RecipeBussResTO startTransport(Map<String, Object> request) {
        LOGGER.info("startTransport request={}", request);
        String mpiId = MapValueUtil.getString(request, MPI_ID);
        String orderCode = MapValueUtil.getString(request, ORDER_CODE);
        String logisticsCompany = MapValueUtil.getString(request, LOGISTICS_COMPANY);
        String trackingNumber = MapValueUtil.getString(request, TRACKING_NUMBER);
        RecipeBussResTO res = new RecipeBussResTO();
        if (StringUtils.isEmpty(mpiId)) {
            res.setCode(0);
            res.setMsg("mpiId is null");
            return res;
        }
        if (StringUtils.isEmpty(orderCode)) {
            res.setCode(0);
            res.setMsg("orderCode is null");
            return res;
        }
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingOrder order = orderDAO.getByMpiIdAndOrderCode(mpiId, orderCode);
        if (null == order) {
            res.setCode(0);
            res.setMsg("订单不存在");
            LOGGER.info("startTransport 订单不存在 orderCode={}", orderCode);
            return res;
        }
        if (!SHOPPING_ORDER_PAY_FINISH.equals(order.getStatus())) {
            res.setCode(0);
            res.setMsg("该订单不是已支付完成的订单");
            LOGGER.info("startTransport 订单状态不为已完成 order={}", order);
            return res;
        }
        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setStatus(SHOPPING_ORDER_START_TRANSPORT);
        order.setLastModify(new Date());
        orderDAO.update(order);
        res.setCode(200);
        res.setMsg("更新配送状态成功");
        LOGGER.info("startTransport 成功 orderCode={}", orderCode);
        return res;
    }

    public void validateOrder(Map<String, Object> conditions) {
        //解析入参
        if (StringUtils.isEmpty(MapValueUtil.getString(conditions, MPI_ID))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "mpiId is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(conditions, ORDER_CODE))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "orderCode is null");
        }
        if (null == conditions.get(SALE_TIME)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "saleTime is null");
        }
        if (null == MapValueUtil.getBigDecimal(conditions, TOTAL_FEE)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "totalFee is null");
        }
        if (null == MapValueUtil.getBigDecimal(conditions, ACTUAL_FEE)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "actualFee is null");
        }
        if (null == MapValueUtil.getBigDecimal(conditions, DRUG_FEE)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "drugFee is null");
        }
        if (null == MapValueUtil.getInteger(conditions, PAY_WAY)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "payWay is null");
        }
        Map<String, Object> logistics = (Map<String, Object>) MapValueUtil.getObject(conditions, LOGISTICS);
        if (null == logistics) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "logistics is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, RECEIVER))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "receiver is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, REC_MOBILE))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recMobile is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, ADDRESS1))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address1 is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, ADDRESS2))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address2 is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, ADDRESS3))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address3 is null");
        }
        if (StringUtils.isEmpty(MapValueUtil.getString(logistics, ADDRESS4))) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address4 is null");
        }
        if (null == MapValueUtil.getList(conditions, DRUGS_INFO) || MapValueUtil.getList(conditions, DRUGS_INFO).size() == 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "drugsInfo is null");
        }

    }

    /**
     * 获取健康商城链接地址并拼接信息返回
     *
     * @param mpiId
     * @return
     */
    @RpcService
    public String getYsqShoppingInfoUrl(String mpiId) {
        PatientDTO patient = patientService.get(mpiId);
        Map<String, String> map = Maps.newHashMap();
        if (null == patient) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "patient is null");
        }
        if (StringUtils.isNotEmpty(patient.getFullHomeArea())) {
            //拆分用户地址 存在只有省，市情况
            String[] areaArgs = patient.getFullHomeArea().split(" ");
            if (areaArgs.length >= 2) {
                map.put("province", areaArgs[0]);
                map.put("city", areaArgs[1]);
                if (3 == areaArgs.length) {
                    //区
                    map.put("county", areaArgs[2]);
                }
            }
        }
        map.put("nickName", patient.getPatientName());
        map.put("mobile", patient.getMobile());
        map.put("sex", patient.getPatientSex());
        // 头像参数"headimgurl" 不传
        map.put("custNo", patient.getMpiId());
        //获取钥匙圈地址
        String ysqUrl = cacheService.getParam(ParameterConstant.KEY_YSQ_SKIP_SHOPPING_URL);
        String backUrl = "";
        if (StringUtils.isEmpty(ysqUrl)) {
            return backUrl;
        }
        try {
            backUrl = ysqUrl + URLEncoder.encode(JSONUtils.toString(map), RecipeSystemConstant.DEFAULT_CHARACTER_ENCODING);
            LOGGER.info("getYsqShoppingInfoUrl backUrl {}", backUrl);
            return backUrl;
        } catch (Exception e) {
            LOGGER.info("getYsqShoppingInfoUrl fail {}", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "getYsqShoppingInfoUrl URLEncoder.encode fail");
        }

    }

    /**
     * 根据条件查询订单列表
     *
     * @param changeAttr
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<ShoppingOrderDTO> findShoppingOrdersWithConditions(Map<String, Object> changeAttr, int start, int limit) {
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        List<ShoppingOrder> list = orderDAO.findShoppingOrdersWithConditions(changeAttr, start, limit);
        return ObjectCopyUtils.convert(list, ShoppingOrderDTO.class);
    }

    /**
     * 根据订单编号获取药品详情列表
     *
     * @param orderCode
     * @return
     */
    @RpcService
    public List<ShoppingDrugDTO> findDrugsByOrderCode(String orderCode) {
        ShoppingDrugDAO drugDAO = DAOFactory.getDAO(ShoppingDrugDAO.class);
        List<ShoppingDrug> list = drugDAO.findByOrderCode(orderCode);
        return ObjectCopyUtils.convert(list, ShoppingDrugDTO.class);
    }

    @RpcService
    public QueryResult<Map<String, Object>> findShoppingOrdersWithInfo(String bDate, String eDate, String mpiId, String orderCode,
                                                                       Integer status, Integer start, Integer limit) {
        //通过条件获取List<ShoppingOrder>
        if (null == start || "".equals(start)) {
            start = 0;
        }
        if (null == limit || "".equals(limit)) {
            limit = 10;
        }
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        QueryResult<ShoppingOrder> queryResult = orderDAO.findShoppingOrdersWithInfo(bDate, eDate, mpiId, orderCode, status, start, limit);
        List<ShoppingOrder> shoppingOrders = new ArrayList<>();
        if (queryResult.getItems() != null && queryResult.getItems().size() > 0) {
            shoppingOrders = queryResult.getItems();
        }
        Set<String> set = new HashSet<>();
        if (shoppingOrders != null && shoppingOrders.size() > 0) {
            for (ShoppingOrder so : shoppingOrders) {
                set.add(so.getMpiId());
            }
        }
        List<String> mpiIdList = new ArrayList<>();
        if (set.size() > 0 && set != null) {
            mpiIdList.addAll(set);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        if (mpiIdList != null && mpiIdList.size() > 0) {
            List<PatientDTO> patientBeanList = patientService.findByMpiIdIn(mpiIdList);
            for (ShoppingOrder so : shoppingOrders) {
                for (PatientDTO patient : patientBeanList) {
                    if (so.getMpiId().equals(patient.getMpiId())) {
                        Map<String, Object> map = Maps.newHashMap();
                        map.put("patient", patient);
                        map.put("ShoppingOrder", ObjectCopyUtils.convert(so, ShoppingOrderDTO.class));
                        list.add(map);
                    }
                }
            }
        }
        return new QueryResult<>(queryResult.getTotal(), start, limit, list);
    }

    /**
     * 获取患者信息:patient对象以及收货人详细信息
     *
     * @param mpiId
     * @return
     */
    @RpcService
    public Map<String, Object> getPatientAndAddressByMpiId(String orderCode, String mpiId) {
        Map<String, Object> map = Maps.newHashMap();
        //获得订单信息
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingOrder byMpiIdAndOrderCode = orderDAO.getByMpiIdAndOrderCode(mpiId, orderCode);
        //获得收获人信息
        //date 2019/12/25
        //调整处方方法调basic
        List<AddressBean> addressList = ObjectCopyUtils.convert(iAddressService.findByMpiId(mpiId), AddressBean.class);
        //获得药品详情
        List<ShoppingDrugDTO> drugsByOrderCode = this.findDrugsByOrderCode(orderCode);
        map.put("shoppingOrder", ObjectCopyUtils.convert(byMpiIdAndOrderCode, ShoppingOrderDTO.class));
        map.put("addressBean", addressList);
        map.put("shoppingDrug", drugsByOrderCode);
        return map;
    }

}
