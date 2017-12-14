package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.base.address.model.AddressBean;
import com.ngari.base.address.service.IAddressService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.entity.ShoppingDrug;
import com.ngari.recipe.entity.ShoppingOrder;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.ErrorCode;
import recipe.dao.ShoppingDrugDAO;
import recipe.dao.ShoppingOrderDAO;
import recipe.util.ApplicationUtils;
import recipe.util.MapValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 健康商城服务类
 * @author:liuya
 * date:2017/12/6
 */
@RpcBean("shoppingService")
public class ShoppingService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingService.class);

    private static final Integer SHOPPING_ORDER_PAY_FINISH = 1;

    private static final Integer SHOPPING_ORDER_CANCEL = 2;

    private static final Integer SHOPPING_ORDER_START_TRANSPORT = 3;

    private IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

    /**
     * 获取患者信息
     * @param mpiId
     * @return
     */
    @RpcService
    public RecipeBussResTO getUser(String mpiId){
        IAddressService addressService = ApplicationUtils.getBaseService(IAddressService.class);
        RecipeBussResTO  res = new RecipeBussResTO();
        PatientBean patient = iPatientService.get(mpiId);
        if (null == patient) {
            res.setCode(0);
            res.setMsg("患者不存在");
            return res;
        }
        List<AddressBean> addressList = addressService.findByMpiId(mpiId);
        Map<String, Object> user = Maps.newHashMap();
        user.put("name", patient.getPatientName());
        user.put("address", addressList);
        res.setCode(200);
        res.setMsg("");
        res.setUser(user);
        return res;
    }

    /**
     * 健康商城完成支付后回调接口
     * @param request
     * @return
     */
    @RpcService
    public RecipeBussResTO finishOrder(RecipeBussReqTO request){
        RecipeBussResTO res = new RecipeBussResTO();
        Map<String, Object> conditions = request.getConditions();
        if(null == conditions){
            res.setCode(0);
            res.setMsg("请填写入参");
            return res;
        }
        //校验入参
        validateOrder(conditions);
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingDrugDAO drugDAO = DAOFactory.getDAO(ShoppingDrugDAO.class);

        ShoppingOrder order = new ShoppingOrder();
        //解析入参
        order.setMpiId(MapValueUtil.getString(conditions, "mpiId"));
        PatientBean patient = iPatientService.get(order.getMpiId());
        order.setPatientName(patient.getPatientName());
        order.setOrderCode(MapValueUtil.getString(conditions, "orderCode"));
        order.setSaleTime(MapValueUtil.getDate(conditions, "saleTime"));
        order.setTotalFee(MapValueUtil.getBigDecimal(conditions, "totalFee"));
        order.setActualFee(MapValueUtil.getBigDecimal(conditions, "actualFee"));
        order.setExpressFee(MapValueUtil.getBigDecimal(conditions, "expressFee"));
        order.setDrugFee(MapValueUtil.getBigDecimal(conditions, "drugFee"));
        order.setCouponFee(MapValueUtil.getBigDecimal(conditions, "couponFee"));
        order.setPayWay(MapValueUtil.getInteger(conditions, "payWay"));
        order.setStatus(SHOPPING_ORDER_PAY_FINISH);
        Map<String,Object> logistics = (Map<String, Object>) MapValueUtil.getObject(conditions, "logistics");
        String receiver = MapValueUtil.getString(logistics, "receiver");
        String recMobile = MapValueUtil.getString(logistics, "recMobile");
        String address1 = MapValueUtil.getString(logistics, "address1");
        String address2 = MapValueUtil.getString(logistics, "address2");
        String address3 = MapValueUtil.getString(logistics, "address3");
        String address4 = MapValueUtil.getString(logistics, "address4");
        order.setCancelTime(null);
        order.setReceiver(receiver);
        order.setRecMobile(recMobile);
        order.setAddress(address1 + address2 + address3 + address4);
        order.setCreateTime(new Date());
        order.setLastModify(new Date());
        ShoppingOrder order1 = orderDAO.getByMpiIdAndOrderCode(order.getMpiId(), order.getOrderCode());
        if(null != order1){
            res.setCode(0);
            res.setMsg("订单已存在");
            return res;
        }
        orderDAO.save(order);
        List<ShoppingDrug> drugsInfo = MapValueUtil.getList(conditions, "drugsInfo");
        for(ShoppingDrug s : drugsInfo){
            s.setOrderCode(order.getOrderCode());
            s.setCreateTime(new Date());
            s.setLastModify(new Date());
            drugDAO.save(s);
        }
        res.setCode(200);
        res.setMsg("生成订单成功");
        return res;
    }

    /**
     * 用户取消订单接口
     * @param mpiId
     * @param orderCode
     * @param cancelTime
     * @param cancelReason
     * @return
     */
    @RpcService
    public RecipeBussResTO cancelOrder(String mpiId, String orderCode, Date cancelTime, String cancelReason){
        RecipeBussResTO res = new RecipeBussResTO();
        if(StringUtils.isEmpty(mpiId)){
            res.setCode(0);
            res.setMsg("mpiId is null");
            return res;
        }
        if(StringUtils.isEmpty(orderCode)){
            res.setCode(0);
            res.setMsg("orderCode is null");
            return res;
        }
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingOrder order = orderDAO.getByMpiIdAndOrderCode(mpiId, orderCode);
        if (null == order) {
            res.setCode(0);
            res.setMsg("订单不存在");
            return res;
        }
        if(!SHOPPING_ORDER_PAY_FINISH.equals(order.getStatus())){
            res.setCode(0);
            res.setMsg("该订单不是已完成支付的订单");
            return res;
        }
        order.setCancelTime(cancelTime);
        order.setCancelReason(cancelReason);
        order.setStatus(SHOPPING_ORDER_CANCEL);
        order.setLastModify(new Date());
        orderDAO.update(order);
        res.setCode(200);
        res.setMsg("取消成功");
        return res;
    }

    /**
     * 开始配送回调接口
     * @param mpiId
     * @param orderCode
     * @param logisticsCompany
     * @param trackingNumber
     * @return
     */
    @RpcService
    public RecipeBussResTO startTransport(String mpiId, String orderCode, String logisticsCompany, String trackingNumber){
        RecipeBussResTO res = new RecipeBussResTO();
        ShoppingOrderDAO orderDAO = DAOFactory.getDAO(ShoppingOrderDAO.class);
        ShoppingOrder order = orderDAO.getByMpiIdAndOrderCode(mpiId, orderCode);
        if(null == order){
            res.setCode(0);
            res.setMsg("订单不存在");
            return res;
        }
        if(!SHOPPING_ORDER_PAY_FINISH.equals(order.getStatus())){
            res.setCode(0);
            res.setMsg("该订单不是已支付完成的订单");
            return res;
        }
        order.setLogisticsCompany(logisticsCompany);
        order.setTrackingNumber(trackingNumber);
        order.setStatus(SHOPPING_ORDER_START_TRANSPORT);
        order.setLastModify(new Date());
        orderDAO.update(order);
        res.setCode(200);
        res.setMsg("");
        return res;
    }

    public void validateOrder(Map<String, Object> conditions){
        //解析入参
        if(StringUtils.isEmpty(MapValueUtil.getString(conditions, "mpiId"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "mpiId is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(conditions, "orderCode"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "orderCode is null");
        }
        if(null == MapValueUtil.getDate(conditions, "saleTime")){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "saleTime is null");
        }
        if(null == MapValueUtil.getBigDecimal(conditions, "totalFee")){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "totalFee is null");
        }
        if(null == MapValueUtil.getBigDecimal(conditions, "actualFee")){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "actualFee is null");
        }
        if(null == MapValueUtil.getBigDecimal(conditions, "drugFee")){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "drugFee is null");
        }
        if(null == MapValueUtil.getInteger(conditions, "payWay")){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "payWay is null");
        }
        Map<String,Object> logistics = (Map<String, Object>) MapValueUtil.getObject(conditions, "logistics");
        if(null == logistics){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "logistics is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "receiver"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "receiver is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "recMobile"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recMobile is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "address1"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address1 is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "address2"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address2 is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "address3"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address3 is null");
        }
        if(StringUtils.isEmpty(MapValueUtil.getString(logistics, "address4"))){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "address4 is null");
        }
        if(null == MapValueUtil.getList(conditions, "drugsInfo") || MapValueUtil.getList(conditions, "drugsInfo").size() == 0){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "drugsInfo is null");
        }

    }


}
