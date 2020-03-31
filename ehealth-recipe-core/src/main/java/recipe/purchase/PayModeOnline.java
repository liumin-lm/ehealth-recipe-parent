package recipe.purchase;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
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
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.QueryRecipeService;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
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
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        //todo---暂时写死上海六院---配送到家判断是否是医保患者
        //非卫宁付
        if (!purchaseService.getPayOnlineConfig(dbRecipe.getClinicOrgan())){
            if (dbRecipe.getClinicOrgan() == 1000899 && purchaseService.isMedicarePatient(dbRecipe.getClinicOrgan(),dbRecipe.getMpiid())){
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("医保患者不支持线上配送，请选择其他取药方式");
                return resultBean;
            }
        }
        Integer recipeId = dbRecipe.getRecipeId();

        //药企列表
        List<DepDetailBean> depDetailList = new ArrayList<>();

//        //date 20200308
//        //获取ext表里存的药企信息以及药企费用，使用此药企展示信息
//        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
//        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
//        if(null != extend){
//            String deliveryRecipeFee = extend.getDeliveryRecipeFee();
//            String deliveryCode = extend.getDeliveryCode();
//            String deliveryName = extend.getDeliveryName();
//            DepDetailBean depDetailBean;
//            if(StringUtils.isNotEmpty(deliveryRecipeFee) &&
//                    StringUtils.isNotEmpty(deliveryCode) && StringUtils.isNotEmpty(deliveryName)){
//                LOG.info("findSupportDepList 当前处方{}的药企信息为his预校验返回信息：{}", recipeId, JSONUtils.toString(extend));
//                depDetailBean = new DepDetailBean();
//                //标识选择的药企是his推过来的
//                depDetailBean.setDepId(-1);
//                depDetailBean.setDepName(deliveryName);
//                depDetailBean.setRecipeFee(new BigDecimal(deliveryRecipeFee));
//                depDetailBean.setBelongDepName(deliveryName);
//                depDetailBean.setOrderType(1);
//                depDetailBean.setPayModeText("在线支付");
//                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
//                //预留字段标识是医院推送给过来的
//                depDetailBean.setHisDep(true);
//
//                depDetailList.add(depDetailBean);
//                depListBean.setSigle(true);
//                depListBean.setList(depDetailList);
//                resultBean.setObject(depListBean);
//                LOG.info("findSupportDepList 当前处方{}查询his药企列表展示信息：{}", recipeId, JSONUtils.toString(resultBean));
//                return resultBean;
//            }
//
//        }

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
        List<DrugsEnterprise> drugsEnterpriseList;
        if (Integer.valueOf(1).equals(dbRecipe.getRecipeSource())) {
            drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndOther(dbRecipe.getClinicOrgan(), payModeSupport);
        } else {
            drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(dbRecipe.getClinicOrgan(), payModeSupport);
        }
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

        RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        AccessDrugEnterpriseService remoteService;
        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterpriseList.size());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            remoteService = remoteDrugEnterpriseService.getServiceByDep(dep);
            //药品匹配成功标识
            boolean stockFlag = remoteService.scanStock(dbRecipe, dep, drugIds);
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
            depDetailBean.setMemo(dep.getMemo());
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
            result = getOrderCreateResult(dbRecipe, extInfo, result);
        }else{
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("order 当前处方信息不全！");
        }
        return result;

    }


    //date 20200318
    //确认订单前校验处方信息
    private Map<String,Object> checkMakeOrder(Recipe dbRecipe, Map<String, String> extInfo) {
        //首先校验：预结算
        //再校验：同步配送信息

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //修改逻辑成：事务1 -> 平台新增，his新增
        //事务2 -> 预交付

        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        Map<String, Object> payResult = hisService.provincialMedicalPreSettle(dbRecipe.getRecipeId());
        if ("-1".equals(payResult.get("code"))) {
            LOG.info("order 当前处方{}确认订单校验处方信息：预结算失败，结算结果：{}",
                    dbRecipe.getRecipeId(), JSONUtils.toString(payResult));
            return payResult;
        }


        HisResponseTO resultSave = updateGoodsReceivingInfoToCreateOrder(dbRecipe.getRecipeId(), extInfo);

        if (null != resultSave) {
            if (resultSave.isSuccess() && null != resultSave.getData()) {

                Map<String, Object> data = (Map<String, Object>) resultSave.getData();

                if (null != data.get("recipeCode")) {
                    //新增成功更新his处方code
                    recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(),
                            ImmutableMap.of("recipeCode", data.get("recipeCode").toString()));
                    LOG.info("order 当前处方{}确认订单流程：his新增成功",
                            dbRecipe.getRecipeId());
                    return payResult;
                } else {
                    payResult.put("code", "-1");
                    payResult.put("msg", "订单信息校验失败");
                    LOG.info("order 当前处方确认订单的his同步配送信息，没有返回his处方code：{}", JSONUtils.toString(resultSave));
                    return payResult;
                }
            } else {
                payResult.put("code", "-1");
                payResult.put("msg", "订单信息校验失败");
                LOG.info("order 当前处方确认订单的his同步配送信息失败，返回：{}", JSONUtils.toString(resultSave));
                return payResult;
            }
        } else {
            LOG.info("order 当前处方{}没有对接同步配送信息，默认成功！", dbRecipe.getRecipeId());
            return payResult;
        }
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
//        if(null != extend && StringUtils.isNotEmpty(extend.getDeliveryRecipeFee())){
//            if(StringUtils.isNotEmpty(extend.getDeliveryCode())
//                    && StringUtils.isNotEmpty(extend.getDeliveryName())){
//                LOG.info("当前处方{}有his回传的药企信息，his药品库存足够！", recipeId);
//                order.setEnterpriseId(depId);
//
//            }else{
//                LOG.info("当前处方{}有his无回传的药企信息，his药品库存不够！", recipeId);
//                //无法配送
//                result.setCode(RecipeResultBean.FAIL);
//                result.setMsg("药企无法配送");
//                return result;
//            }
//        }else{

        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipeId)));
        RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                    ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
        AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(dep);;
        boolean stockFlag = remoteService.scanStock(dbRecipe, dep, drugIds);
        if (!stockFlag) {
                //无法配送
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("药企无法配送");
                return result;
            } else {
                remoteService.setEnterpriseMsgToOrder(order, depId, extInfo);
            }
        //}

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

        // 根据咨询单特殊来源标识和处方单特殊来源标识设置处方订单orderType为省中，邵逸夫医保小程序
        if (null != dbRecipe.getClinicId()) {
            IConsultService consultService = ConsultAPI.getService(IConsultService.class);
            ConsultBean consultBean = consultService.getById(dbRecipe.getClinicId());
            if (null != consultBean) {
                if (Integer.valueOf(1).equals(consultBean.getConsultSource()) && (Integer.valueOf(1).equals(dbRecipe.getRecipeSource()))) {
                    order.setOrderType(3);
                }
            }
        }

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
        updateRecipeDetail(recipeId);
        //date 20200318
        //确认订单后同步配送信息接口
        remoteService.sendDeliveryMsgToHis(dbRecipe.getRecipeId());
        return result;
    }

    private void updateRecipeDetail(Integer recipeId) {
        try{
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            if (recipe.getEnterpriseId() != null) {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
                for (Recipedetail recipedetail : recipedetails) {
                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
                    LOG.info("PayModeOnline.updateRecipeDetail recipeId:{},saleDrugList:{}.", recipeId, JSONUtils.toString(saleDrugList));
                    if (saleDrugList != null) {
                        recipedetail.setActualSalePrice(saleDrugList.getPrice());
                        if (StringUtils.isEmpty(saleDrugList.getOrganDrugCode())) {
                            recipedetail.setSaleDrugCode(saleDrugList.getDrugId()+"");
                        } else {
                            recipedetail.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                        }
                    }
                    recipeDetailDAO.update(recipedetail);
                }
            }
        }catch(Exception e){
            LOG.info("PayModeOnline.updateRecipeDetail error recipeId:{}.", recipeId);
        }
    }

    public HisResponseTO updateGoodsReceivingInfo(Integer recipeId) {
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
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (null != recipeExtend){
                updateTakeDrugWayReqTO.setRegisterId(recipeExtend.getRegisterID());
            }
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
//
//            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
//
//            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
//            RecipeExtend nowRecipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
//
//            if(null != nowRecipeExtend){
//                String deliveryRecipeFee = nowRecipeExtend.getDeliveryRecipeFee();
//                if(StringUtils.isNotEmpty(deliveryRecipeFee)){
//
//                    //date 20200305
//                    IRecipePlatformServiceNew platformService = AppDomainContext.getBean("his.recipePlatformService",IRecipePlatformServiceNew.class);
//                    QueryRecipeReqHisDTO queryRecipeReqDTO = new QueryRecipeReqHisDTO();
//                    queryRecipeReqDTO.setOrganId(null != recipe.getClinicOrgan() ? recipe.getClinicOrgan().toString() :  "");
//                    queryRecipeReqDTO.setRecipeID(recipeId.toString());
//                    QueryRecipeResultHisDTO queryRecipeResultHisDTO = platformService.queryRecipeInfo(queryRecipeReqDTO);
//                    updateTakeDrugWayReqTO.setQueryRecipeResultHisDTO(queryRecipeResultHisDTO);
//
//                }
//
//            }else{
//                LOG.info("当前处方{}没有关联的扩展信息", recipeId);
//            }
            //date 20200312
            //将配送信息同步过来
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
//            updateTakeDrugWayReqTO.setCheckOrgan(null != recipe.getCheckOrgan() ? recipe.getCheckOrgan().toString() : null);
//            updateTakeDrugWayReqTO.setCheckDate(recipe.getCheckDate());
//            updateTakeDrugWayReqTO.setCheckerTel(recipe.getCheckerTel());
//            updateTakeDrugWayReqTO.setCheckMemo(recipe.getCheckFailMemo());
//            updateTakeDrugWayReqTO.setSupplementaryMemo(recipe.getSupplementaryMemo());
//            //设置当前更新
//            updateTakeDrugWayReqTO.setGiveMode(UpdateSendMsgStatusEnum.LOGISTIC_SEND.getSendType());

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

    public HisResponseTO updateGoodsReceivingInfoToCreateOrder(Integer recipeId, Map<String, String> extInfo) {
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
            updateTakeDrugWayReqTO.setNgarRecipeId(recipe.getRecipeId().toString());
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
            RecipeOrder order = createOrderBySendMap(extInfo);
            LOG.info("组装的order信息：{}", JSONUtils.toString(order));
            if (order!=null){
                //收货人
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                //联系电话
                updateTakeDrugWayReqTO.setContactTel(order.getRecMobile());
                //收货地址
                //date 20200319
                //修改推送的地址细节：address ：address4,receiveAddress:集合，receiveAddrCode：address3
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                updateTakeDrugWayReqTO.setAddress(order.getAddress4());
                updateTakeDrugWayReqTO.setReceiveAddrCode(order.getAddress3());
                updateTakeDrugWayReqTO.setReceiveAddress(commonRemoteService.getCompleteAddressToSend(order));

                //date 20200314
                //新添字段更新
                updateTakeDrugWayReqTO.setDeliveryCode(order.getHisEnterpriseCode());
                updateTakeDrugWayReqTO.setDeliveryName(order.getHisEnterpriseName());
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
                SimpleDateFormat formatter = new SimpleDateFormat(DateConversion.DEFAULT_DATE_TIME);
                updateTakeDrugWayReqTO.setPlanDate(StringUtils.isNotEmpty(order.getExpectSendDate())?
                        order.getExpectSendDate() + " 00:00:00" : null);
                updateTakeDrugWayReqTO.setPlanTime(order.getExpectSendTime());
            }else{
                LOG.info("同步配送信息，组装配送订单失败！");
                HisResponseTO hisResponseTO = new HisResponseTO();
                hisResponseTO.setMsgCode("-1");
                return hisResponseTO;
            }
            if (recipe.getClinicId() != null) {
                updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
            }
            //流转到这里来的属于物流配送
            updateTakeDrugWayReqTO.setDeliveryType("1");
//
//            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
//
//            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
//            RecipeExtend nowRecipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
//
//            if(null != nowRecipeExtend){
//                String deliveryRecipeFee = nowRecipeExtend.getDeliveryRecipeFee();
//                if(StringUtils.isNotEmpty(deliveryRecipeFee)){
//
//                    //date 20200305
//                    IRecipePlatformServiceNew platformService = AppDomainContext.getBean("his.recipePlatformService",IRecipePlatformServiceNew.class);
//                    QueryRecipeReqHisDTO queryRecipeReqDTO = new QueryRecipeReqHisDTO();
//                    queryRecipeReqDTO.setOrganId(null != recipe.getClinicOrgan() ? recipe.getClinicOrgan().toString() :  "");
//                    queryRecipeReqDTO.setRecipeID(recipeId.toString());
//                    QueryRecipeResultHisDTO queryRecipeResultHisDTO = platformService.queryRecipeInfo(queryRecipeReqDTO);
//                    updateTakeDrugWayReqTO.setQueryRecipeResultHisDTO(queryRecipeResultHisDTO);
//
//                }
//
//            }else{
//                LOG.info("当前处方{}没有关联的扩展信息", recipeId);
//            }
            //date 20200312
            //将配送信息同步过来
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            updateTakeDrugWayReqTO.setCheckOrgan(null != recipe.getCheckOrgan() ? recipe.getCheckOrgan().toString() : null);
            updateTakeDrugWayReqTO.setCheckDate(recipe.getCheckDateYs());
            updateTakeDrugWayReqTO.setCheckerTel(recipe.getCheckerTel());
            updateTakeDrugWayReqTO.setCheckMemo(recipe.getCheckFailMemo());
            updateTakeDrugWayReqTO.setSupplementaryMemo(recipe.getSupplementaryMemo());
            //设置当前更新
            updateTakeDrugWayReqTO.setGiveMode(
                    UpdateSendMsgStatusEnum.fromGiveType(null == extInfo.get("payMode") ? null : Integer.parseInt(extInfo.get("payMode").toString())).getSendType());

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
    //将map中用来生成平台订单的信息组装成订单配送信息
    private RecipeOrder createOrderBySendMap(Map<String, String> extInfo) {
        RecipeOrder recipeOrder = new RecipeOrder();
        recipeOrder.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        recipeOrder.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        //判断传入的是his的药企code还是平台维护的药企id
        if(StringUtils.isNotEmpty(extInfo.get("hisDepCode"))){
            recipeOrder.setHisEnterpriseCode(MapValueUtil.getString(extInfo, "hisDepCode"));
        }else{
            recipeOrder.setHisEnterpriseCode(MapValueUtil.getString(extInfo, "depId"));
        }

        recipeOrder.setHisEnterpriseName(MapValueUtil.getString(extInfo, "depName"));
        String operMpiId = MapValueUtil.getString(extInfo, "operMpiId");
        String operAddressId = MapValueUtil.getString(extInfo, "addressId");
        AddressDTO address = null;
        AddressService addressService = ApplicationUtils.getBasicService(AddressService.class);
        if (StringUtils.isNotEmpty(operAddressId)) {
            address = addressService.get(Integer.parseInt(operAddressId));
        } else {
            address = addressService.getLastAddressByMpiId(operMpiId);
        }
        if(null != address){
            recipeOrder.setAddress1(address.getAddress1());
            recipeOrder.setAddress2(address.getAddress2());
            recipeOrder.setAddress3(address.getAddress3());
            recipeOrder.setAddress4(address.getAddress4());
        }else{
            LOG.warn("当前确认订单推送没有设置配送地址");
            return null;
        }
        recipeOrder.setReceiver(address.getReceiver());
        recipeOrder.setRecMobile(address.getRecMobile());
        recipeOrder.setRecTel(address.getRecMobile());
        return recipeOrder;
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
                    //退款refundFlag
                    if (orderStatus == OrderStatusConstant.READY_PAY && new Integer(1).equals(order.getRefundFlag())) {
                        tips = "订单结算失败，费用已为您原路返回";
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
        try{
            LOG.info("PayModeOnline.checkStoreForSendToHom:{}.", JSONUtils.toString(depDetailList));
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                    ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByOrganId(dbRecipe.getClinicOrgan());
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
            List<Integer> drugIds = new ArrayList<>(detailList.size());
            Map<Integer, Double> drugIdCountMap = Maps.newHashMap();
            for (Recipedetail detail : detailList) {
                drugIds.add(detail.getDrugId());
                drugIdCountMap.put(detail.getDrugId(), detail.getUseTotalDose());
            }
            // 处方单特殊来源标识：1省中，邵逸夫医保小程序
            // 如果标识是1，则需要支持省直医保；drugsEnterprises 必须和 depDetailList取交集
            if (Integer.valueOf(1).equals(dbRecipe.getRecipeSource())) {
                List<DrugsEnterprise> drugsEnterprisesFilter = new ArrayList<>();
                for (DepDetailBean depDetailBean : depDetailList) {
                    for (DrugsEnterprise drugsEnterpris : drugsEnterprises) {
                        if (depDetailBean.getDepId().equals(drugsEnterpris.getId())) {
                            drugsEnterprisesFilter.add(drugsEnterpris);
                            continue;
                        }
                    }
                }
                drugsEnterprises = drugsEnterprisesFilter;
            }
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                //date 20200316
                //特殊处理的时候判断要不要走药企自己的展示
                AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);;
                boolean specialMake = remoteService.specialMakeDepList(drugsEnterprise, dbRecipe);
                if (DrugEnterpriseConstant.COMPANY_HR.equals(drugsEnterprise.getCallSys()) || DrugEnterpriseConstant.COMPANY_BY.equals(drugsEnterprise.getCallSys())
                        || DrugEnterpriseConstant.COMPANY_YSQ.equals(drugsEnterprise.getCallSys())|| specialMake || DrugEnterpriseConstant.COMPANY_LY.equals(drugsEnterprise.getCallSys())) {
                    //将药店配送的药企移除
                    for (DepDetailBean depDetailBean : depDetailList) {
                        if (drugsEnterprise.getId().equals(depDetailBean.getDepId())) {
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
                                //如果是价格自定义的药企，则需要设置单独价格
                                if (Integer.valueOf(0).equals(drugsEnterprise.getSettlementMode())) {
                                    List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
                                    if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                                        BigDecimal total = BigDecimal.ZERO;
                                        try {
                                            for (SaleDrugList saleDrug : saleDrugLists) {
                                                //保留3位小数
                                                total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountMap.get(saleDrug.getDrugId())))
                                                        .divide(BigDecimal.ONE, 3, RoundingMode.UP));
                                            }
                                        } catch (Exception e) {
                                            LOG.warn("findSupportDepList 重新计算药企ID为[{}]的结算价格出错. drugIds={}", drugsEnterprise.getId(),
                                                    JSONUtils.toString(drugIds), e);
                                            continue;
                                        }
                                        //重置药企处方价格
                                        depDetailBean.setRecipeFee(total);
                                    }
                                }
                            }
                            depDetailList.addAll(hrList);
                            LOG.info("获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
                        }
                    }
                }
            }
        }catch (Exception e){
            LOG.info("PayModeOnline.checkStoreForSendToHom:{},{}.", JSONUtils.toString(dbRecipe), e.getMessage());
        }
    }
}
