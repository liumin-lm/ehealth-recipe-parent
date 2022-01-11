package recipe.purchase;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.constant.UpdateSendMsgStatusEnum;
import recipe.core.api.IStockBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.drugsenterprise.paymodeonlineshowdep.PayModeOnlineShowDepServiceProducer;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.hisservice.RecipeToHisService;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.presettle.factory.OrderTypeFactory;
import recipe.presettle.model.OrderTypeCreateConditionRequest;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

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
    @Autowired
    private IStockBusinessService stockBusinessService;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private EnterpriseManager enterpriseManager;

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        //todo---暂时写死上海六院---配送到家判断是否是医保患者
        //非卫宁付
        if (!purchaseService.getPayOnlineConfig(dbRecipe.getClinicOrgan())) {
            if (dbRecipe.getClinicOrgan() == 1000899 && purchaseService.isMedicarePatient(dbRecipe.getClinicOrgan(), dbRecipe.getMpiid())) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("医保患者不支持线上配送，请选择其他取药方式");
                return resultBean;
            }
        }
        Integer recipeId = dbRecipe.getRecipeId();
        //判断是否是慢病医保患者------郑州人民医院
        if (purchaseService.isMedicareSlowDiseasePatient(recipeId)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("抱歉，由于您是慢病医保患者，请到人社平台、医院指定药房或者到医院进行医保支付。");
            return resultBean;
        }
        //药企列表
        List<DepDetailBean> depDetailList = new ArrayList<>();

        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        List<Integer> payModeSupportDoc = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD);
        payModeSupport.addAll(payModeSupportDoc);
        LOG.info("drugsEnterpriseList organId:{}, payModeSupport:{}", dbRecipe.getClinicOrgan(), payModeSupport);
        // 获取药企
        List<DrugsEnterprise> drugsEnterpriseList = enterpriseManager.findEnterpriseByOnLine(extInfo.get("sendType"), payModeSupport, dbRecipe);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            LOG.warn("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }

        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterpriseList.size());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbRecipe, detailList, dep.getId());
            if (null != enterpriseStock && enterpriseStock.getStock()) {
                subDepList.add(dep);
            }
        }

        if (CollectionUtils.isEmpty(subDepList)) {
            LOG.warn("findSupportDepList 该处方无法配送. recipeId=[{}]", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }
        LOG.info("findSupportDepList subDepList:{}", JSONUtils.toString(subDepList));
        //这里是为了同一个药企下能显示在线支付和货到付款
        subDepList = getAllSubDepList(subDepList);
        //处理详情
        //为了计算药品费
        List<Integer> recipeIdList = Arrays.asList(recipeId);
        String recipeIds = MapValueUtil.getString(extInfo, "recipeIds");
        if (StringUtils.isNotEmpty(recipeIds)) {
            List<String> recipeIdString = Splitter.on(",").splitToList(recipeIds);
            recipeIdList = recipeIdString.stream().map(Integer::valueOf).collect(Collectors.toList());
        }
        //这里是获取到可以支持的药企列表
        for (DrugsEnterprise dep : subDepList) {
            //这里有四个途径获取供应商列表分别是有展示药店标识的药企、his管理的药企、正常的配送到家的药企、北京互联网处方流转
            PayModeOnlineShowDepServiceProducer.getShowDepService(dep, dbRecipe).getPayModeOnlineShowDep(dep, depDetailList, dbRecipe, recipeIdList);
        }
        depListBean.setSigle(false);
        if (CollectionUtils.isNotEmpty(depDetailList) && depDetailList.size() == 1) {
            depListBean.setSigle(true);
        }
        //去重---有可能getAllSubDepList接口返回两个相同的药企但是在北京互联网的模式过滤下会出现两个一模一样的药企
        //暂时先放这去重
        depListBean.setList(depDetailList.stream().distinct().collect(Collectors.toList()));
        resultBean.setObject(depListBean);
        LOG.info("findSupportDepList 当前处方{}查询药企列表信息：{}", recipeId, JSONUtils.toString(resultBean));
        return resultBean;
    }

    @Override
    public OrderCreateResult order(List<Recipe> dbRecipes, Map<String, String> extInfo) {
        LOG.info("PayModeOnline order recipes={}", JSONUtils.toString(dbRecipes));
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        return getOrderCreateResult(dbRecipes, extInfo, result);

    }

    //确认订单流程
    private OrderCreateResult getOrderCreateResult(List<Recipe> recipeList, Map<String, String> extInfo, OrderCreateResult result) {
        RecipeOrder order = new RecipeOrder();
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);


        List<Integer> recipeIdLists = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        String payway = MapValueUtil.getString(extInfo, "payway");
        //订单类型-1省医保
        Integer orderType = MapValueUtil.getInteger(extInfo, "orderType");
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        String insuredArea = MapValueUtil.getString(extInfo, "insuredArea");
        Integer logisticsCompany = MapValueUtil.getInteger(extInfo, "logisticsCompany");
        String provinceCode = MapValueUtil.getString(extInfo, "provinceCode");
        String cityCode = MapValueUtil.getString(extInfo, "cityCode");
        String areaCode = MapValueUtil.getString(extInfo, "areaCode");
        Integer takeMedicineWay = MapValueUtil.getInteger(extInfo, "takeMedicineWay");
        if (StringUtils.isNotEmpty(insuredArea)) {
            for (Recipe recipe : recipeList) {
                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("insuredArea", insuredArea));
            }
        }

        if (StringUtils.isEmpty(payway)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("支付信息不全");
            return result;
        }
        order.setWxPayWay(payway);

        //保存站点相关信息
        if (null != takeMedicineWay) {
            String gysName = MapValueUtil.getString(extInfo, "gysName");
            String gysAddr = MapValueUtil.getString(extInfo, "gysAddr");
            String stationCode = MapValueUtil.getString(extInfo, "pharmacyCode");
            String distance = MapValueUtil.getString(extInfo, "distance");
            order.setDrugStoreName(gysName);
            order.setDrugStoreAddr(gysAddr);
            order.setDrugStoreCode(stationCode);
            order.setTakeMedicineWay(takeMedicineWay);
            order.setAddress1(provinceCode);
            order.setAddress2(cityCode);
            order.setAddress3(areaCode);
            order.setAddress4(gysAddr);
            order.setDistance(Double.parseDouble(distance));
        }
        //如果是医保支付前端目前传的orderType都是1,杭州市医保得特殊处理
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeList.get(0).getRecipeMode())
                && RecipeBussConstant.ORDERTYPE_ZJS.equals(orderType)) {
            orderType = RecipeBussConstant.ORDERTYPE_HZS;
        }
        LOG.info("getOrderCreateResult.orderType ={}", orderType);
        order.setOrderType(orderType);
        //设置确认订单页购药方式的key
        String giveModeKey = MapValueUtil.getString(extInfo, "giveModeKey");
        order.setGiveModeKey(giveModeKey);
        order.setGiveModeText(CommonOrder.getGiveModeText(recipeList.get(0).getClinicOrgan(), giveModeKey));
        DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
        //处理详情
        for (Recipe dbRecipe : recipeList) {
            List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());

            order.setRecipeIdList(JSONUtils.toString(recipeIdLists));
            RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                    ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

            AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(dep);

            // 根据药企查询库存
            EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbRecipe, detailList, depId);
            if (Objects.isNull(enterpriseStock) || !enterpriseStock.getStock()) {
                //无法配送
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("药企无法配送");
                return result;
            } else {
                remoteService.setEnterpriseMsgToOrder(order, depId, extInfo);
            }
        }


        if (dep != null) {
            //设置配送费支付方式
            order.setExpressFeePayWay(dep.getExpressFeePayWay());
            order.setSendType(dep.getSendType());
            //设置是否显示期望配送时间,默认否 0:否,1:是
            order.setIsShowExpectSendDate(dep.getIsShowExpectSendDate());
        }

        //设置中药代建费
        Integer decoctionId = MapValueUtil.getInteger(extInfo, "decoctionId");
        if (decoctionId != null) {
            DrugDecoctionWayDao drugDecoctionWayDao = getDAO(DrugDecoctionWayDao.class);
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(decoctionId);
            for (Recipe dbRecipe : recipeList) {
                if (decoctionWay != null) {
                    if (decoctionWay.getDecoctionPrice() != null) {
                        order.setDecoctionUnitPrice(BigDecimal.valueOf(decoctionWay.getDecoctionPrice()));
                    }
                    recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("decoctionId", decoctionId + "", "decoctionText", decoctionWay.getDecoctionText()));

                } else {
                    LOG.error("未获取到对应的代煎费，recipeId={},decoctionId={}", dbRecipe.getRecipeId(), decoctionId);
                }
            }
        }

        // 暂时还是设置成处方单的患者，不然用户历史处方列表不好查找
        order.setMpiId(recipeList.get(0).getMpiid());
        order.setOrganId(recipeList.get(0).getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        String drugStoreCode = MapValueUtil.getString(extInfo, "pharmacyCode");
        if (StringUtils.isNotEmpty(drugStoreCode)) {
            String drugStoreName = MapValueUtil.getString(extInfo, "depName");
            order.setDrugStoreCode(drugStoreCode);
            order.setDrugStoreName(drugStoreName);
        }
        //设置订单各种费用和配送地址
        orderService.setOrderFee(result, order, recipeIdLists, recipeList, payModeSupport, extInfo, 1);

        //判断设置状态
        int reviewType = recipeList.get(0).getReviewType();
        Integer payStatus;
        //判断处方是否免费
        if (0 >= order.getActualPrice()) {
            //免费不需要走支付
            if (ReviewTypeConstant.Postposition_Check == reviewType) {
                payStatus = OrderStatusConstant.READY_CHECK;
            } else {
                payStatus = OrderStatusConstant.READY_SEND;
            }
        } else {
            //走支付，待支付
            payStatus = OrderStatusConstant.READY_PAY;
        }

        order.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        order.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        order.setStatus(payStatus);

        //设置为有效订单
        order.setEffective(1);

        OrderTypeCreateConditionRequest orderTypeCreateConditionRequest = OrderTypeCreateConditionRequest.builder()
                .recipe(recipeList.get(0))
                .drugsEnterprise(dep)
                .recipeOrder(order)
                .recipeExtend(recipeExtendDAO.getByRecipeId(recipeList.get(0).getRecipeId()))
                .build();
        Integer recipeOrderType = OrderTypeFactory.getRecipeOrderType(orderTypeCreateConditionRequest);
        LOG.info("getOrderCreateResult.order recipeID={} recipeOrderType ={}", recipeList.get(0).getRecipeId(), recipeOrderType);
        order.setOrderType(recipeOrderType);

        // 目前paymode传入还是老版本 除线上支付外全都算线下支付,下个版本与前端配合修改
        Integer payModeNew = payMode;
        if (!payMode.equals(1)) {
            payModeNew = 2;
        }
        order.setPayMode(payModeNew);
        // 在订单创建的时候写入物流公司信息
        if (Objects.nonNull(logisticsCompany)) {
            order.setLogisticsCompany(logisticsCompany);
        }
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if (!saveFlag) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("订单保存出错");
            return result;
        }

        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        if (0d >= order.getActualPrice()) {
            //如果不需要支付则不走支付
            orderService.finishOrderPay(order.getOrderCode(), 1, MapValueUtil.getInteger(extInfo, "payMode"));
        } else {
            // 邵逸夫模式下 不需要审方物流费需要生成一条流水记录
            orderManager.saveFlowByOrder(order);

            for (Integer recipeId3 : recipeIdLists) {
                recipeDAO.updateRecipeInfoByRecipeId(recipeId3, ImmutableMap.of("chooseFlag", 1));
            }
        }
        for (Integer recipeId2 : recipeIdLists) {
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            purchaseService.updateRecipeDetail(recipeId2);
            //date 20200318
            //确认订单后同步配送信息接口
            extInfo.put("payMode", "1");
            CommonOrder.updateGoodsReceivingInfoToCreateOrder(recipeId2, extInfo);
        }
        return result;
    }

    public HisResponseTO updateGoodsReceivingInfoToCreateOrder(Integer recipeId, Map<String, String> extInfo) {
        try {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (patient == null) {
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
            if (recipe.getChecker() != null) {
                IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                EmploymentBean primaryEmp = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                if (primaryEmp != null) {
                    updateTakeDrugWayReqTO.setCheckerId(primaryEmp.getJobNumber());
                }
                DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
                if (doctorDTO != null) {
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
            if (order != null) {
                //收货人
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                //联系电话
                updateTakeDrugWayReqTO.setContactTel(order.getRecMobile());
                //收货地址
                //date 20200319
                //修改推送的地址细节：address ：address4,receiveAddress:集合，receiveAddrCode：address3
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                updateTakeDrugWayReqTO.setAddress(order.getAddress4());
                if (order.getStreetAddress() != null) {
                    updateTakeDrugWayReqTO.setReceiveAddrCode(order.getStreetAddress());
                } else {
                    updateTakeDrugWayReqTO.setReceiveAddrCode(order.getAddress3());
                }

                updateTakeDrugWayReqTO.setReceiveAddress(commonRemoteService.getCompleteAddressToSend(order));

                //date 20200314
                //新添字段更新
                updateTakeDrugWayReqTO.setDeliveryCode(order.getHisEnterpriseCode());
                updateTakeDrugWayReqTO.setDeliveryName(order.getHisEnterpriseName());
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
                SimpleDateFormat formatter = new SimpleDateFormat(DateConversion.DEFAULT_DATE_TIME);
                updateTakeDrugWayReqTO.setPlanDate(StringUtils.isNotEmpty(order.getExpectSendDate()) ?
                        order.getExpectSendDate() + " 00:00:00" : null);
                updateTakeDrugWayReqTO.setPlanTime(order.getExpectSendTime());

                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                if (recipeExtend != null && recipeExtend.getDecoctionId() != null) {
                    //煎法Code
                    DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                    DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                    updateTakeDrugWayReqTO.setDecoctionCode(decoctionWay.getDecoctionCode());
                    updateTakeDrugWayReqTO.setDecoctionFee(order.getDecoctionFee());
                }
            } else {
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
            //date 2020-10-15 17:38 修改添加挂号序号
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend nowRecipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (null != nowRecipeExtend) {
                updateTakeDrugWayReqTO.setRegisterId(nowRecipeExtend.getRegisterID());
            } else {
                LOG.info("当前处方{}没有关联的扩展信息", recipeId);
            }
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
        } catch (Exception e) {
            LOG.error("updateGoodsReceivingInfo. error", e);
            HisResponseTO hisResponseTO = new HisResponseTO();
            hisResponseTO.setMsgCode("-1");
            return hisResponseTO;
        }

    }

    //将map中用来生成平台订单的信息组装成订单配送信息
    private RecipeOrder createOrderBySendMap(Map<String, String> extInfo) {
        LOG.info("createOrderBySendMap. extInfo=[{}]", JSONUtils.toString(extInfo));
        RecipeOrder recipeOrder = new RecipeOrder();
        recipeOrder.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        recipeOrder.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        //判断传入的是his的药企code还是平台维护的药企id
        if (StringUtils.isNotEmpty(extInfo.get("hisDepCode"))) {
            recipeOrder.setHisEnterpriseCode(MapValueUtil.getString(extInfo, "hisDepCode"));
        } else {
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
        if (null != address) {
            recipeOrder.setAddress1(address.getAddress1());
            recipeOrder.setAddress2(address.getAddress2());
            recipeOrder.setAddress3(address.getAddress3());
            recipeOrder.setAddress4(address.getAddress4());
            recipeOrder.setStreetAddress(address.getStreetAddress());
        } else {
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
        String tips = "";
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_CHECK_PASS:
                if (StringUtils.isNotEmpty(orderCode)) {
                    int orderStatus = order.getStatus();
                    if (orderStatus == OrderStatusConstant.READY_SEND) {
                        tips = "订单已处理，请耐心等待药品配送";
                    }
                    //退款refundFlag
                    if (orderStatus == OrderStatusConstant.READY_PAY && new Integer(1).equals(order.getRefundFlag())) {
                        tips = "订单结算失败，费用已为您原路返回";
                    }
                }
                break;
            case RECIPE_STATUS_WAIT_SEND:
                tips = "订单已处理，请耐心等待药品配送";
                break;
            case RECIPE_STATUS_CHECK_PASS_YS:
                tips = "处方已审核通过，请耐心等待药品配送";
                break;
            case RECIPE_STATUS_IN_SEND:
                tips = "物流配送中";
                break;
            case RECIPE_STATUS_FINISH:
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

    @Override
    public void setRecipePayWay(RecipeOrder recipeOrder) {
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe recipe = recipeDAO.findRecipeListByOrderCode(recipeOrder.getOrderCode()).get(0);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (new Integer(2).equals(recipeOrder.getPayMode())) {
            recipeOrder.setPayMode(0);
        } else {
            recipeOrder.setPayMode(1);
        }
        recipeOrderDAO.update(recipeOrder);
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

}
