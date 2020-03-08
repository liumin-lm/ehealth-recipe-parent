package recipe.purchase;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.QueryRecipeReqHisDTO;
import com.ngari.platform.recipe.mode.QueryRecipeResultHisDTO;
import com.ngari.platform.recipe.service.IRecipePlatformServiceNew;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.QueryRecipeReqDTO;
import com.ngari.recipe.hisprescription.model.QueryRecipeResultDTO;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.QueryRecipeService;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 在线支付-配送到家购药方式
 * @version： 1.0
 */
public class PayModeOnline implements IPurchaseService {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PayModeOnline.class);

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        Integer recipeId = dbRecipe.getRecipeId();

        //药企列表
        List<DepDetailBean> depDetailList = new ArrayList<>();

        //date 20200308
        //获取ext表里存的药企信息以及药企费用，使用此药企展示信息
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        if(null != extend){
            String deliveryRecipeFee = extend.getDeliveryRecipeFee();
            String deliveryCode = extend.getDeliveryCode();
            String deliveryName = extend.getDeliveryName();
            DepDetailBean depDetailBean;
            if(StringUtils.isNotEmpty(deliveryRecipeFee) &&
                    StringUtils.isNotEmpty(deliveryCode) && StringUtils.isNotEmpty(deliveryName)){
                LOG.info("findSupportDepList 当前处方{}的药企信息为his预校验返回信息：{}", recipeId, JSONUtils.toString(extend));
                depDetailBean = new DepDetailBean();
                //标识选择的药企是his推过来的
                depDetailBean.setDepId(-1);
                depDetailBean.setDepName(deliveryName);
                depDetailBean.setRecipeFee(new BigDecimal(deliveryRecipeFee));
                depDetailBean.setBelongDepName(deliveryName);
                depDetailBean.setOrderType(1);
                depDetailBean.setPayModeText("在线支付");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
                //预留字段标识是医院推送给过来的
                depDetailBean.setHisDep(true);

                depDetailList.add(depDetailBean);
                depListBean.setSigle(true);
                depListBean.setList(depDetailList);
                resultBean.setObject(depListBean);
                LOG.info("findSupportDepList 当前处方{}查询his药企列表展示信息：{}", recipeId, JSONUtils.toString(resultBean));
                return resultBean;
            }

        }

        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        List<Integer> payModeSupportDoc = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD);
        payModeSupport.addAll(payModeSupportDoc);
        if (CollectionUtils.isEmpty(payModeSupport)) {
            LOG.warn("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, getPayMode());
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("配送模式配置有误");
            return resultBean;
        }
        LOG.info("drugsEnterpriseList organId:{}, payModeSupport:{}", dbRecipe.getClinicOrgan(), payModeSupport);
        //筛选出来的数据已经去掉不支持任何方式配送的药企
        List<DrugsEnterprise> drugsEnterpriseList =
                drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(dbRecipe.getClinicOrgan(), payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            LOG.warn("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }

        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        Map<Integer, Double> drugIdCountMap = Maps.newHashMap();
        for (Recipedetail detail : detailList) {
            drugIds.add(detail.getDrugId());
            drugIdCountMap.put(detail.getDrugId(), detail.getUseTotalDose());
        }

        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterpriseList.size());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //药品匹配成功标识
            boolean stockFlag = scanStock(dbRecipe, dep, drugIds);
            if (stockFlag) {
                subDepList.add(dep);
            }
        }

        if (CollectionUtils.isEmpty(subDepList)) {
            LOG.warn("findSupportDepList 该处方无法配送. recipeId=[{}]", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }
        subDepList = getAllSubDepList(subDepList);
        DepDetailBean depDetailBean;
        for (DrugsEnterprise dep : subDepList) {
            depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(dep.getId());
            depDetailBean.setDepName(dep.getName());
            depDetailBean.setRecipeFee(dbRecipe.getTotalMoney());
            depDetailBean.setBelongDepName(dep.getName());
            depDetailBean.setOrderType(dep.getOrderType());
            if (RecipeBussConstant.PAYMODE_ONLINE.equals(dep.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(dep.getPayModeSupport())) {
                depDetailBean.setPayModeText("在线支付");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
            } else {
                depDetailBean.setPayModeText("货到付款");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
            }

            //如果是价格自定义的药企，则需要设置单独价格
            if (Integer.valueOf(0).equals(dep.getSettlementMode())) {
                List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(dep.getId(), drugIds);
                if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                    BigDecimal total = BigDecimal.ZERO;
                    try {
                        for (SaleDrugList saleDrug : saleDrugLists) {
                            //保留3位小数
                            total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountMap.get(saleDrug.getDrugId())))
                                    .divide(BigDecimal.ONE, 3, RoundingMode.UP));
                        }
                    } catch (Exception e) {
                        LOG.warn("findSupportDepList 重新计算药企ID为[{}]的结算价格出错. drugIds={}", dep.getId(),
                                JSONUtils.toString(drugIds), e);
                        continue;
                    }

                    //重置药企处方价格
                    depDetailBean.setRecipeFee(total);
                }
            }

            depDetailList.add(depDetailBean);
        }
        //此处校验是否存在支持药店配送的药企
        checkStoreForSendToHom(dbRecipe, depDetailList);
        depListBean.setSigle(false);
        if (CollectionUtils.isNotEmpty(depDetailList) && depDetailList.size() == 1) {
            depListBean.setSigle(true);
        }

        depListBean.setList(depDetailList);
        resultBean.setObject(depListBean);
        LOG.info("findSupportDepList 当前处方{}查询药企列表信息：{}", recipeId, JSONUtils.toString(resultBean));
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
        LOG.info("PayModeOnline order recipeId={}",dbRecipe.getRecipeId());
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        if(null != dbRecipe.getRecipeId()){
            RecipeExtend extend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
            if(null != extend && StringUtils.isNotEmpty(extend.getDeliveryRecipeFee())){
                LOG.info("order 当前处方{}是杭州市互联网处方走新流程", dbRecipe.getRecipeId());
                result = getOrderCreateResultNew(dbRecipe, extInfo, result, recipeDAO, recipeExtendDAO, extend);

            }else{
                LOG.info("order 当前处方{}不是杭州市互联网处方走老流程", dbRecipe.getRecipeId());
                result = getOrderCreateResult(dbRecipe, extInfo, result);
            }
        }else{
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("order 当前处方信息不全！");
        }
        return result;

    }

    private synchronized OrderCreateResult getOrderCreateResultNew(Recipe dbRecipe, Map<String, String> extInfo, OrderCreateResult result, RecipeDAO recipeDAO, RecipeExtendDAO recipeExtendDAO, RecipeExtend extend) {
        String orderMakeStatus = extend.getOrderMakeStatus();
        if(StringUtils.isNotEmpty(orderMakeStatus)){
            switch (orderMakeStatus){
                case "0":
                    result = getOrderCreateResult(dbRecipe, extInfo, result);
                    if(RecipeResultBean.SUCCESS.equals(result.getCode())){
                        recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(),
                                ImmutableMap.of("orderMakeStatus", "1"));
                        LOG.info("order 当前处方{}确认订单流程：{}->{}",
                                dbRecipe.getRecipeId(), orderMakeStatus, "1");
                    }else{
                        break;
                    }
                case "1":

                    HisResponseTO resultSave = updateGoodsReceivingInfo(dbRecipe.getRecipeId());

                    if(null != resultSave && resultSave.isSuccess() && null != resultSave.getData()){
                        Map<String, Object> data =(Map<String, Object>)resultSave.getData();

                        if(null != data.get("recipeCode")){
                            //新增成功更新his处方code
                            recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(),
                                    ImmutableMap.of("recipeCode", data.get("recipeCode").toString()));
                            recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(),
                                    ImmutableMap.of("orderMakeStatus", "2"));
                            LOG.info("order 当前处方{}确认订单流程：{}->{}",
                                    dbRecipe.getRecipeId(), orderMakeStatus, "2");
                        }else{
                            result.setCode(RecipeResultBean.FAIL);
                            result.setMsg("order 当前处方确认订单的新增处方流程，没有返回his处方code");
                            LOG.info("order 当前处方确认订单的新增处方流程，没有返回his处方code：{}" , JSONUtils.toString(resultSave));
                            break;
                        }
                    }else{
                        result.setCode(RecipeResultBean.FAIL);
                        result.setMsg("order 当前处方确认订单的新增处方流程，未新增成功");
                        LOG.info("order 当前处方确认订单的新增处方流程未新增成功，返回：{}" , JSONUtils.toString(resultSave));
                        break;
                    }

                case "2":
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                    Map<String, Object> resultMap = hisService.provincialMedicalPreSettle(dbRecipe.getRecipeId());
                    if(null != resultMap && "200".equals(resultMap.get("code"))){
                        recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(),
                                ImmutableMap.of("orderMakeStatus", "3"));
                        LOG.info("order 当前处方{}确认订单流程：{}->{}",
                                dbRecipe.getRecipeId(), orderMakeStatus, "3");
                    }else{
                        result.setCode(RecipeResultBean.FAIL);
                        result.setMsg("order 当前处方确认订单的预结算处方流程，未预结算成功");
                        LOG.info("order 当前处方确认订单的预结算处方流程未成功，返回：{}" , JSONUtils.toString(resultMap));
                        break;
                    }
                case "3":
                    break;
                default:
                    result.setCode(RecipeResultBean.FAIL);
                    result.setMsg("order 当前处方确认订单流程节点状态无法识别：" + orderMakeStatus);
                    LOG.info("order 当前处方确认订单流程节点状态无法识别：：{}" , orderMakeStatus);
                    break;
            }
        }else{
            LOG.info("order 当前处方{}确认订单的流程信息为空!", dbRecipe.getRecipeId());
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("order 当前处方确认订单的流程信息为空！");
        }
        return result;
    }

    //确认订单流程
    private OrderCreateResult getOrderCreateResult(Recipe dbRecipe, Map<String, String> extInfo, OrderCreateResult result) {
        RecipeOrder order = new RecipeOrder();
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

        Integer recipeId = dbRecipe.getRecipeId();
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        String payway = MapValueUtil.getString(extInfo, "payway");
        //订单类型-1省医保
        Integer orderType = MapValueUtil.getInteger(extInfo, "orderType");

        if (StringUtils.isEmpty(payway)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("支付信息不全");
            return result;
        }
        order.setWxPayWay(payway);
        order.setOrderType(orderType);
        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        //date 20200309
        //当前处方为杭州市互联网处方 通过ext校验库存
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        if(null != extend && StringUtils.isNotEmpty(extend.getDeliveryRecipeFee())){
            if(StringUtils.isNotEmpty(extend.getDeliveryCode())
                    && StringUtils.isNotEmpty(extend.getDeliveryName())){
                LOG.info("当前处方{}有his回传的药企信息，his药品库存足够！", recipeId);
                order.setEnterpriseId(depId);

            }else{
                LOG.info("当前处方{}有his无回传的药企信息，his药品库存不够！", recipeId);
                //无法配送
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("药企无法配送");
                return result;
            }
        }else{

            DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
            boolean stockFlag = scanStock(dbRecipe, dep, drugIds);
            if (!stockFlag) {
                //无法配送
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("药企无法配送");
                return result;
            } else {
                order.setEnterpriseId(depId);
            }
        }
        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipeId)));

        // 暂时还是设置成处方单的患者，不然用户历史处方列表不好查找
        order.setMpiId(dbRecipe.getMpiid());
        order.setOrganId(dbRecipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        String drugStoreCode = MapValueUtil.getString(extInfo, "pharmacyCode");
        if (StringUtils.isNotEmpty(drugStoreCode)) {
            String drugStoreName = MapValueUtil.getString(extInfo, "depName");
            //String DrugStoreAddr = MapValueUtil.
            order.setDrugStoreCode(drugStoreCode);
            order.setDrugStoreName(drugStoreName);
        }
        //设置订单各种费用和配送地址
        List<Recipe> recipeList = Arrays.asList(dbRecipe);
        orderService.setOrderFee(result, order, Arrays.asList(recipeId), recipeList, payModeSupport, extInfo, 1);

        //判断设置状态
        int reviewType = dbRecipe.getReviewType();
        Integer giveMode = dbRecipe.getGiveMode();
        Integer payStatus = null;
        //判断处方是否免费
        if(0 >= order.getActualPrice()){
            //免费不需要走支付
            if(ReviewTypeConstant.Postposition_Check == reviewType){
                payStatus = OrderStatusConstant.READY_CHECK;
            }else{
                payStatus = OrderStatusConstant.READY_SEND;
            }
        }else{
            //走支付，待支付
            payStatus = OrderStatusConstant.READY_PAY;
        }

        order.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        order.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        order.setStatus(payStatus);
        order.setStatus(payStatus);

        //设置为有效订单
        order.setEffective(1);
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if (!saveFlag) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("订单保存出错");
            return result;
        }

        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        if(0d >= order.getActualPrice()){
            //如果不需要支付则不走支付
            orderService.finishOrderPay(order.getOrderCode(), 1, MapValueUtil.getInteger(extInfo, "payMode"));
        }else{
            Recipe nowRecipe = recipeDAO.get(recipeId);
            //处方需要支付，需要在确认订单将购药方式绑定上
            if(null == nowRecipe){
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("当前处方" + recipeId + "不存在！");
                return result;
            }
            nowRecipe.setChooseFlag(1);
            recipeDAO.update(nowRecipe);
        }
        //date 20200309
        //当前处方不为杭州市互联网处方
        if(!(null != extend && StringUtils.isNotEmpty(extend.getDeliveryRecipeFee()))){

            //选择配送到家后调用更新取药方式-配送信息
            RecipeBusiThreadPool.submit(()->{
                updateGoodsReceivingInfo(dbRecipe.getRecipeId());
                return null;
            });
        }
        return result;
    }

    private HisResponseTO updateGoodsReceivingInfo(Integer recipeId) {
        try{

            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            //杭州市三除外
            if (StringUtils.isNotEmpty(recipe.getOrganName())&&recipe.getOrganName().contains("杭州市第三人民医院")){
                return new HisResponseTO().setSuccess();
            }
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (patient == null){
                throw new DAOException(ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
            }
            //患者信息
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            patientBaseInfo.setCertificateType(patient.getCertificateType());
            patientBaseInfo.setCertificate(patient.getCertificate());
            patientBaseInfo.setPatientName(patient.getPatientName());
            patientBaseInfo.setPatientID(recipe.getPatientID());

            UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
            updateTakeDrugWayReqTO.setPatientBaseInfo(patientBaseInfo);
            updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
            //医院处方号
            updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
            //审方药师工号和姓名
            if (recipe.getChecker()!=null){
                IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                EmploymentBean primaryEmp = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                if (primaryEmp != null){
                    updateTakeDrugWayReqTO.setCheckerId(primaryEmp.getJobNumber());
                }
                DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
                if (doctorDTO!=null){
                    updateTakeDrugWayReqTO.setCheckerName(doctorDTO.getName());
                }
            }
            //处方总金额
            updateTakeDrugWayReqTO.setPayment(recipe.getActualPrice());
            //支付状态
            updateTakeDrugWayReqTO.setPayFlag(recipe.getPayFlag());
            //支付方式
            updateTakeDrugWayReqTO.setPayMode("1");
            if (StringUtils.isNotEmpty(recipe.getOrderCode())){
                    RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
                    RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
                    if (order!=null){
                        //收货人
                        updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                        //联系电话
                        updateTakeDrugWayReqTO.setContactTel(order.getRecMobile());
                        //收货地址
                        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                        updateTakeDrugWayReqTO.setAddress(commonRemoteService.getCompleteAddress(order));
                    }
                }
            if (recipe.getClinicId() != null) {
                updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
            }
            //流转到这里来的属于物流配送
            updateTakeDrugWayReqTO.setDeliveryType("1");

            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);

            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend nowRecipeExtend = recipeExtendDAO.getByRecipeId(recipeId);

            if(null != nowRecipeExtend){
                String deliveryRecipeFee = nowRecipeExtend.getDeliveryRecipeFee();
                if(StringUtils.isNotEmpty(deliveryRecipeFee)){

                    //date 20200305
                    IRecipePlatformServiceNew platformService = AppDomainContext.getBean("his.recipePlatformService",IRecipePlatformServiceNew.class);
                    QueryRecipeReqHisDTO queryRecipeReqDTO = new QueryRecipeReqHisDTO();
                    queryRecipeReqDTO.setOrganId(null != recipe.getClinicOrgan() ? recipe.getClinicOrgan().toString() :  "");
                    queryRecipeReqDTO.setRecipeID(recipeId.toString());
                    QueryRecipeResultHisDTO queryRecipeResultHisDTO = platformService.queryRecipeInfo(queryRecipeReqDTO);
                    updateTakeDrugWayReqTO.setQueryRecipeResultHisDTO(queryRecipeResultHisDTO);

                }

            }else{
                LOG.info("当前处方{}没有关联的扩展信息", recipeId);
            }
            LOG.info("收货信息更新通知his. req={}", JSONUtils.toString(updateTakeDrugWayReqTO));
            HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
            LOG.info("收货信息更新通知his. res={}", JSONUtils.toString(hisResult));
            return hisResult;
        }catch (Exception e){
            LOG.error("updateGoodsReceivingInfo. error", e);
            HisResponseTO hisResponseTO = new HisResponseTO();
            hisResponseTO.setMsgCode("-1");
            return hisResponseTO;
        }

    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_ONLINE;
    }

    @Override
    public String getServiceName() {
        return "payModeOnlineService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        String orderCode = recipe.getOrderCode();
        int orderStatus = order.getStatus();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.CHECK_PASS:
                if (StringUtils.isNotEmpty(orderCode)) {
                    if (orderStatus == OrderStatusConstant.READY_SEND) {
                        tips = "订单已处理，请耐心等待药品配送";
                    }
                }
                break;
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.CHECK_PASS_YS:
                tips = "处方已审核通过，请耐心等待药品配送";
                break;
            case RecipeStatusConstant.IN_SEND:
                tips = "药企正在配送";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "药企配送完成，订单完成";
                break;
                default:
        }
        return tips;
    }

    @Override
    public Integer getOrderStatus(Recipe recipe) {
        return OrderStatusConstant.READY_SEND;
    }

    private List<DrugsEnterprise> getAllSubDepList(List<DrugsEnterprise> subDepList) {
        List<DrugsEnterprise> returnSubDepList = new ArrayList<>();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        for (DrugsEnterprise drugsEnterprise : subDepList) {
            returnSubDepList.add(drugsEnterprise);
            if (drugsEnterprise.getPayModeSupport() == 9) {
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(drugsEnterprise.getId());
                enterprise.setPayModeSupport(1);
                returnSubDepList.add(enterprise);
            }
        }
        //对货到付款和在线支付进行排序
        Collections.sort(returnSubDepList, new SubDepListComparator());
        return returnSubDepList;
    }

    /**
     * 判断药企库存，包含平台内权限及药企实时库存
     *
     * @param dbRecipe
     * @param dep
     * @param drugIds
     * @return
     */
    private boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        boolean succFlag = false;
        if (null == dep || CollectionUtils.isEmpty(drugIds)) {
            return succFlag;
        }

        //判断药企平台内药品权限，此处简单判断数量是否一致
        Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(dep.getId(), drugIds);
        if (null != count && count > 0) {
            if (count == drugIds.size()) {
                succFlag = true;
            }
        }

        if (!succFlag) {
            LOG.warn("scanStock 存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}], drugIds={}",
                    dbRecipe.getRecipeId(), dep.getId(), dep.getName(), JSONUtils.toString(drugIds));
        } else {
            //通过查询该药企库存，最终确定能否配送
            succFlag = remoteDrugService.scanStock(dbRecipe.getRecipeId(), dep);
            if (!succFlag) {
                LOG.warn("scanStock 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                        dbRecipe.getRecipeId(), dep.getId(), dep.getName());
            }
        }

        return succFlag;
    }

    class SubDepListComparator implements Comparator<DrugsEnterprise> {
        int cp = 0;
        @Override
        public int compare(DrugsEnterprise drugsEnterpriseOne, DrugsEnterprise drugsEnterpriseTwo) {
            int compare = drugsEnterpriseOne.getPayModeSupport() - drugsEnterpriseTwo.getPayModeSupport();
            if (compare != 0) {
                cp = compare > 0 ? 1 : -1;
            }
            return cp;
        }
    }

    private void checkStoreForSendToHom(Recipe dbRecipe, List<DepDetailBean> depDetailList) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByOrganId(dbRecipe.getClinicOrgan());
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            if (DrugEnterpriseConstant.COMPANY_HR.equals(drugsEnterprise.getCallSys())) {
                //将药店配送的药企移除
                BigDecimal recipeFree = BigDecimal.ZERO;
                for (DepDetailBean depDetailBean : depDetailList) {
                    if (depDetailBean.getDepId() == drugsEnterprise.getId()) {
                        recipeFree = depDetailBean.getRecipeFee();
                        depDetailList.remove(depDetailBean);
                        break;
                    }
                }
                //特殊处理,对华润药企特殊处理,包含华润药企,需要将华润药企替换成药店
                RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                //需要从接口获取药店列表
                DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(Arrays.asList(dbRecipe.getRecipeId()), null, drugsEnterprise);

                if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
                    Object result = drugEnterpriseResult.getObject();
                    if (result != null && result instanceof List) {
                        List<DepDetailBean> hrList = (List) result;
                        for (DepDetailBean depDetailBean : hrList) {
                            depDetailBean.setDepId(drugsEnterprise.getId());
                            depDetailBean.setBelongDepName(depDetailBean.getDepName());
                            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
                            depDetailBean.setPayModeText("在线支付");
                            depDetailBean.setRecipeFee(recipeFree);
                        }
                        depDetailList.addAll(hrList);
                        LOG.info("获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
                    }
                }
            }
        }
    }
}
