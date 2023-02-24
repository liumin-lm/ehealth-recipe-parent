package recipe.purchase;

import com.alibaba.fastjson.JSON;
import com.ngari.his.recipe.mode.TakeMedicineByToHos;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.client.IConfigurationClient;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IStockBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.status.PayModeEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.FastRecipeFlagEnum;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.manager.*;
import recipe.presettle.factory.OrderTypeFactory;
import recipe.presettle.model.OrderTypeCreateConditionRequest;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;
import recipe.util.DistanceUtil;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/20
 * @description： 到院取药方式实现
 * @version： 1.0
 */
public class PayModeToHos implements IPurchaseService {

    private static final String searchMapLatitude = "latitude";
    private static final String searchMapLongitude = "longitude";

    @Autowired
    private OrderManager orderManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Autowired
    private OrganDrugsSaleConfigDAO organDrugsSaleConfigDAO;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private IStockBusinessService stockBusinessService;
    @Autowired
    private PharmacyDAO pharmacyDAO;
    @Autowired
    private RecipeOrderService recipeOrderService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeManager recipeManager;

    private static final Logger LOG = LoggerFactory.getLogger(PayModeToHos.class);

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        //判断是否是慢病医保患者------郑州人民医院
        if (purchaseService.isMedicareSlowDiseasePatient(dbRecipe.getRecipeId())) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("抱歉，由于您是慢病医保患者，请到人社平台、医院指定药房或者到医院进行医保支付。");
            return resultBean;
        }

        // 到院自取是否采用药企管理模式
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(dbRecipe.getClinicOrgan(), "drugToHosByEnterprise", false);
        if (drugToHosByEnterprise) {
            resultBean = newModeFindSupportDepList(dbRecipe, extInfo);
        } else {
            resultBean = oldModeFindSupportDepList(dbRecipe);
        }
        return resultBean;
    }


    @Override
    public OrderCreateResult order(List<Recipe> dbRecipes, Map<String, String> extInfo) {
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        //定义处方订单
        RecipeOrder order = new RecipeOrder();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        Integer patientIsDecoction = MapValueUtil.getInteger(extInfo, "patientIsDecoction");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        List<Integer> recipeIdLists = dbRecipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        // 到院自取是否采用药企管理模式
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(dbRecipes.get(0).getClinicOrgan(), "drugToHosByEnterprise", false);
        if (drugToHosByEnterprise) {
            Integer depId = MapValueUtil.getInteger(extInfo, "depId");
            DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
            enterpriseManager.checkSupportDecoction(dbRecipes, depId, patientIsDecoction, GiveModeTextEnum.SUPPORTTOHIS.getGiveMode());
            //处理详情
            for (Recipe dbRecipe : dbRecipes) {
                List<Recipedetail> detailList = recipeDetailDAO.findByRecipeId(dbRecipe.getRecipeId());

                order.setRecipeIdList(JSONUtils.toString(recipeIdLists));
                RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                        ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

                AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(dep);

                // 根据药企查询库存
                EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbRecipe, detailList, depId, StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                if (Objects.isNull(enterpriseStock) || !enterpriseStock.getStock()) {
                    //无法配送
                    result.setCode(RecipeResultBean.FAIL);
                    result.setMsg("药企无法配送");
                    return result;
                } else {
                    remoteService.setEnterpriseMsgToOrder(order, depId, extInfo);
                }
            }
            OrderTypeCreateConditionRequest orderTypeCreateConditionRequest = OrderTypeCreateConditionRequest.builder()
                    .recipe(dbRecipes.get(0))
                    .drugsEnterprise(dep)
                    .recipeOrder(order)
                    .recipeExtend(recipeExtendDAO.getByRecipeId(dbRecipes.get(0).getRecipeId()))
                    .build();
            Integer recipeOrderType = OrderTypeFactory.getRecipeOrderType(orderTypeCreateConditionRequest);
            LOG.info("getOrderCreateResult.order recipeID={} recipeOrderType ={}", dbRecipes.get(0).getRecipeId(), recipeOrderType);
            order.setOrderType(recipeOrderType);
        } else {
            // 到院取药校验机构库存
            List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIdLists);
            EnterpriseStock organStock = organDrugListManager.organStock(dbRecipes.get(0), recipeDetails);
            if (Objects.isNull(organStock) || !organStock.getStock()) {
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("抱歉，医院没有库存，无法到医院取药，请选择其他购药方式。");
                return result;
            }
        }
        order.setMpiId(dbRecipes.get(0).getMpiid());
        order.setOrganId(dbRecipes.get(0).getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        order.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        order.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        //设置预约取药开始和结束时间
        order.setExpectEndTakeTime(MapValueUtil.getString(extInfo, "expectEndTakeTime"));
        order.setExpectStartTakeTime(MapValueUtil.getString(extInfo, "expectStartTakeTime"));
        //订单的状态统一到finishOrderPayWithoutPay中设置
        order.setStatus(OrderStatusConstant.READY_GET_DRUG);
        order.setRecipeIdList(JSONUtils.toString(recipeIdLists));
        //保存开票记录
        Integer templateId = MapValueUtil.getInteger(extInfo, "invoiceRecordId");
        Integer invoiceRecordId = CommonOrder.addInvoiceRecord(templateId, recipeIdLists);
        if (!ObjectUtils.isEmpty(invoiceRecordId)) {
            order.setInvoiceRecordId(invoiceRecordId);
        }
        Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
        //设置中药代建费
        Integer decoctionId = MapValueUtil.getInteger(extInfo, "decoctionId");
        if (decoctionId != null) {
            DrugDecoctionWayDao drugDecoctionWayDao = getDAO(DrugDecoctionWayDao.class);
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(decoctionId);
            if (decoctionWay != null) {
                if (decoctionWay.getDecoctionPrice() != null) {
                    calculateFee = 1;
                    order.setDecoctionUnitPrice(BigDecimal.valueOf(decoctionWay.getDecoctionPrice()));
                }
            } else {
                LOG.error("未获取到对应的代煎费,decoctionId={}", decoctionId);
            }
        }
        //线下处方和线上PatientIsDecoction处理成一样
        //在患者没有选择的情况下：前端会根据医生是否选择字段传入patientIsDecoction  对于线下处方而言，线下转线上的时候医生是否选择已经赋值
        //在患者选择的情况下：前端会根据患者自己选择传入patientIsDecoction
        order.setPatientIsDecoction(MapValueUtil.getString(extInfo, "patientIsDecoction"));
        CommonOrder.createDefaultOrder(extInfo, result, order, payModeSupport, dbRecipes, calculateFee);
        //设置为有效订单
        order.setEffective(1);
        // 目前paymode传入还是老版本 除线上支付外全都算线下支付,下个版本与前端配合修改
        Integer payModeNew = payMode;
        // 到院取药是否支持线上支付
        Integer storePayFlag = eh.utils.MapValueUtil.getInteger(extInfo, "storePayFlag");
        if (storePayFlag == 1) {
            payModeNew = RecipeBussConstant.PAYMODE_ONLINE;
        }
        order.setPayMode(payModeNew);
        if(StringUtils.isNotEmpty(MapValueUtil.getString(extInfo, "revisitRemindTime")))   {
            order.setRevisitRemindTime(DateConversion.parseDate(MapValueUtil.getString(extInfo, "revisitRemindTime"),DateConversion.DEFAULT_DATE_TIME));
        }
        boolean saveFlag = orderService.saveOrderToDB(order, dbRecipes, payMode, result, recipeDAO, orderDAO);
        if (!saveFlag) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("提交失败，请重新提交。");
            return result;
        }
        // 到院自取也需要更新药品实际销售价格
        recipeIdLists.forEach(recipeId -> purchaseService.updateRecipeDetail(recipeId,null));
        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        //更新处方信息
        if (0d >= order.getActualPrice()) {
            //如果不需要支付则不走支付,直接掉支付后的逻辑
            orderService.finishOrderPay(order.getOrderCode(), 1, MapValueUtil.getInteger(extInfo, "payMode"));
        } else {
            // 邵逸夫模式下 不需要审方物流费需要生成一条流水记录
            orderManager.saveFlowByOrder(order);

            //需要支付则走支付前的逻辑
            orderService.finishOrderPayWithoutPay(order.getOrderCode(), payMode);
        }

        return result;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TO_HOS;
    }

    @Override
    public String getServiceName() {
        return "payModeToHosService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payFlag = recipe.getPayFlag();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_CHECK_PASS:
                //date 20190930
                //先判断是否需要支付，再判断有没有支付
                if (StringUtils.isNotEmpty(orderCode)) {
                    //上海马陆医院线下转线上处方直接去支付文案特殊化处理
                    if (new Integer(1).equals(recipe.getRecipePayType()) && order.getPayFlag() == 1) {
                        tips = "处方已支付，具体配送情况请咨询您的开方医生。";
                    } else {
                        if (0d >= order.getActualPrice()) {
                            tips = "订单已处理，请到院取药";
                        } else if (0d < order.getActualPrice() && payFlag == 1) {
                            tips = "订单已处理，请到院取药";
                        }
                    }
                }
                break;
            case RECIPE_STATUS_CHECK_PASS_YS:
                tips = "处方已审核通过，请到院取药";
                break;
            case RECIPE_STATUS_RECIPE_FAIL:
                tips = "到院取药失败";
                break;
            case RECIPE_STATUS_FINISH:
                tips = "到院取药成功，订单完成";
                break;
            case RECIPE_STATUS_HAVE_PAY:
                tips = "订单已支付，请到院取药";
            default:
        }
        return tips;
    }

    @Override
    public Integer getOrderStatus(Recipe recipe) {
        return OrderStatusConstant.READY_GET_DRUG;
    }

    @Override
    public void setRecipePayWay(RecipeOrder recipeOrder) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        recipeOrder.setPayMode(2);
        recipeOrderDAO.update(recipeOrder);
    }

    /**
     * 新模式获取到院取药支持的药企
     *
     * @param dbRecipe
     * @return
     */
    private RecipeResultBean newModeFindSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        Integer recipeId = dbRecipe.getRecipeId();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        String sort = extInfo.get("sort");
        final String range = MapValueUtil.getString(extInfo, "range");
        // 库存判断
        List<OrganAndDrugsepRelation> relation = organAndDrugsepRelationDAO.getRelationByOrganIdAndGiveMode(dbRecipe.getClinicOrgan(), RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
        if (CollectionUtils.isEmpty(relation)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("抱歉，该机构无支持到院取药的药企，无法到医院取药，请选择其他购药方式。");
            return resultBean;
        }
        List<Integer> depIds = relation.stream().map(OrganAndDrugsepRelation::getDrugsEnterpriseId).collect(Collectors.toList());
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByIds(depIds);
        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterprises.size());
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        //如果当前处方为快捷购药并开启开关
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(dbRecipe.getClinicOrgan(), "fastRecipeUsePlatStock", false);
        if (!(FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(dbRecipe.getFastRecipeFlag()))
                || (FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(dbRecipe.getFastRecipeFlag()) && !fastRecipeUsePlatStock)) {
            for (DrugsEnterprise dep : drugsEnterprises) {
                EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbRecipe, detailList, dep.getId(), StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                if (null != enterpriseStock && enterpriseStock.getStock()) {
                    subDepList.add(dep);
                }
            }
        } else {
            subDepList.addAll(drugsEnterprises);
        }
        drugsEnterprises = enterpriseManager.enterprisePriorityLevel(dbRecipe.getClinicOrgan(), subDepList);
        //判断药企是否不展示药店
        boolean showStoreFlag = drugsEnterprises.stream().anyMatch(drugsEnterprise -> 0 == drugsEnterprise.getShowStoreFlag());
        List<DepDetailBean> depDetailBeans = new ArrayList<>();
        List<DrugsEnterprise> noShowStoreEnterprises;
        if (showStoreFlag) {
            noShowStoreEnterprises = drugsEnterprises.stream().filter(drugsEnterprise -> 0 == drugsEnterprise.getShowStoreFlag()).collect(Collectors.toList());
            List<Integer> depIdList = noShowStoreEnterprises.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
            Map<Integer, List<OrganDrugsSaleConfig>> saleMap = getIntegerListMap(depIdList);
            depDetailBeans = setEnterpriseToStore(dbRecipe, noShowStoreEnterprises, saleMap, extInfo);
        }
        LOG.info("newModeFindSupportDepList depDetailBeans:{}", JSONUtils.toString(depDetailBeans));
        // 调his获取取药点
        List<TakeMedicineByToHos> takeMedicineByToHosList = enterpriseManager.getTakeMedicineByToHosList(dbRecipe.getClinicOrgan(), dbRecipe);
        LOG.info("newModeFindSupportDepList subDepList:{}", JSONUtils.toString(takeMedicineByToHosList));
        List<Integer> saleDepIds = takeMedicineByToHosList.stream().map(TakeMedicineByToHos::getEnterpriseId).collect(Collectors.toList());
        Map<Integer, List<OrganDrugsSaleConfig>> saleMap = getIntegerListMap(saleDepIds);
        List<DepDetailBean> result = getDepDetailList(takeMedicineByToHosList, saleMap);
        result.addAll(depDetailBeans);
        if ("1".equals(sort)) {
            //价格优先
            result = result.stream().filter(depDetailBean -> depDetailBean.getDistance() <= (StringUtils.isNotEmpty(range)?Double.parseDouble(range):10L)).sorted(Comparator.comparing(DepDetailBean::getRecipeFee)).collect(Collectors.toList());
        } else {
            //距离优先
            result = result.stream().filter(depDetailBean -> depDetailBean.getDistance() <= (StringUtils.isNotEmpty(range)?Double.parseDouble(range):10L)).sorted(Comparator.comparing(DepDetailBean::getDistance)).collect(Collectors.toList());
        }
        depListBean.setList(result);
        depListBean.setSigle(false);
        if (CollectionUtils.isNotEmpty(result) && result.size() == 1) {
            depListBean.setSigle(true);
        }
        resultBean.setObject(depListBean);
        LOG.info("findSupportDepList 当前处方{}查询药企列表信息：{}", recipeId, JSONUtils.toString(resultBean));
        return resultBean;
    }

    private Map<Integer, List<OrganDrugsSaleConfig>> getIntegerListMap(List<Integer> saleDepIds) {
        LOG.info("getIntegerListMap saleDepIds:{}", JSON.toJSONString(saleDepIds));
        if (CollectionUtils.isEmpty(saleDepIds)) {
            return null;
        }
        List<OrganDrugsSaleConfig> organDrugsSaleConfigs = organDrugsSaleConfigDAO.findSaleConfigs(saleDepIds);
        Map<Integer, List<OrganDrugsSaleConfig>> saleMap = null;
        if (CollectionUtils.isNotEmpty(organDrugsSaleConfigs)) {
            saleMap = organDrugsSaleConfigs.stream().collect(Collectors.groupingBy(OrganDrugsSaleConfig::getDrugsEnterpriseId));
        }
        LOG.info("getIntegerListMap saleMap:{}.", JSON.toJSONString(saleMap));
        return saleMap;
    }

    private List<DepDetailBean> setEnterpriseToStore(Recipe recipe, List<DrugsEnterprise> noShowStoreEnterprises, Map<Integer, List<OrganDrugsSaleConfig>> saleMap, Map<String, String> extInfo) {
        LOG.info("setEnterpriseToStore recipe:{},noShowStoreEnterprises:{},extInfo:{}", JSONUtils.toString(recipe), JSONUtils.toString(noShowStoreEnterprises), JSONUtils.toString(extInfo));
        String longitude = MapValueUtil.getString(extInfo, searchMapLongitude);
        String latitude = MapValueUtil.getString(extInfo, searchMapLatitude);
        return noShowStoreEnterprises.stream().map(enterprise -> {
            List<Pharmacy> pharmacyList = pharmacyDAO.findByDepId(enterprise.getId());
            if(CollectionUtils.isEmpty(pharmacyList)){
                LOG.info("setEnterpriseToStore pharmacyList is null enterpriseId:{}",enterprise.getId());
                return null;
            }
            DepDetailBean depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(enterprise.getId());
            depDetailBean.setDepName(enterprise.getName());
            depDetailBean.setBelongDepName(enterprise.getName());
            //重置药企处方价格
            depDetailBean.setRecipeFee(recipeOrderService.reCalculateRecipeFee(enterprise.getId(), Arrays.asList(recipe.getRecipeId()), null));
            if (MapUtils.isNotEmpty(saleMap) && CollectionUtils.isNotEmpty(saleMap.get(enterprise.getId()))) {
                depDetailBean.setPayModeText(PayModeEnum.getPayModeEnumName(saleMap.get(enterprise.getId()).get(0).getTakeOneselfPayment()));
            }
            Pharmacy pharmacy = pharmacyList.get(0);
            Position position = new Position();
            position.setLatitude(Double.valueOf(pharmacy.getPharmacyLatitude()));
            position.setLongitude(Double.valueOf(pharmacy.getPharmacyLongitude()));
            depDetailBean.setPharmacyName(pharmacy.getPharmacyName());
            depDetailBean.setPharmacyCode(pharmacy.getPharmacyCode());
            depDetailBean.setAddress(pharmacy.getPharmacyAddress());
            depDetailBean.setPosition(position);
            if (StringUtils.isNotEmpty(pharmacy.getPharmacyLatitude()) && StringUtils.isNotEmpty(pharmacy.getPharmacyLongitude())
                    && StringUtils.isNotEmpty(latitude) && StringUtils.isNotEmpty(longitude)) {
                Double distance = DistanceUtil.getDistance(Double.parseDouble(pharmacy.getPharmacyLatitude()), Double.parseDouble(pharmacy.getPharmacyLongitude()),
                        Double.parseDouble(latitude), Double.parseDouble(longitude));
                depDetailBean.setDistance(Double.parseDouble(String.format("%.2f", distance)));
            } else {
                depDetailBean.setDistance(0D);
            }
            return depDetailBean;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<DepDetailBean> getDepDetailList(List<TakeMedicineByToHos> takeMedicineByToHosList, Map<Integer, List<OrganDrugsSaleConfig>> saleMap) {
        return takeMedicineByToHosList.stream().map(takeMedicineByToHos -> {
            DepDetailBean depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(takeMedicineByToHos.getEnterpriseId());
            depDetailBean.setDepName(takeMedicineByToHos.getPharmacyName());
            depDetailBean.setBelongDepName(takeMedicineByToHos.getEnterpriseName());
            depDetailBean.setPharmacyName(takeMedicineByToHos.getPharmacyName());
            depDetailBean.setPharmacyCode(takeMedicineByToHos.getPharmacyCode());
            depDetailBean.setAddress(takeMedicineByToHos.getPharmacyAddress());
            if (Objects.isNull(takeMedicineByToHos.getDistance())) {
                depDetailBean.setDistance(0.0);
            } else {
                depDetailBean.setDistance(takeMedicineByToHos.getDistance());
            }
            depDetailBean.setRecipeFee(takeMedicineByToHos.getRecipeTotalPrice());
            depDetailBean.setPayMethod(takeMedicineByToHos.getPayWay().toString());
            if (MapUtils.isNotEmpty(saleMap) && CollectionUtils.isNotEmpty(saleMap.get(takeMedicineByToHos.getEnterpriseId()))) {
                depDetailBean.setPayModeText(PayModeEnum.getPayModeEnumName(saleMap.get(takeMedicineByToHos.getEnterpriseId()).get(0).getTakeOneselfPayment()));
            }
            Position position = new Position();
            position.setLatitude(Double.valueOf(takeMedicineByToHos.getLat()));
            position.setLongitude(Double.valueOf(takeMedicineByToHos.getLng()));
            position.setRange(takeMedicineByToHos.getRange());
            depDetailBean.setPosition(position);
            return depDetailBean;
        }).collect(Collectors.toList());
    }

    /**
     * 老模式到院取药获取药企列表
     *
     * @param dbRecipe
     * @return
     */
    private RecipeResultBean oldModeFindSupportDepList(Recipe dbRecipe) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = dbRecipe.getRecipeId();
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        StringBuilder sb = new StringBuilder();
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(dbRecipe.getClinicOrgan(), "fastRecipeUsePlatStock", false);
        if (!(FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(dbRecipe.getFastRecipeFlag()) && fastRecipeUsePlatStock)) {
            //点击到院取药再次判断库存--防止之前开方的时候有库存流转到此无库存
            // 到院取药校验机构库存
            EnterpriseStock organStock = organDrugListManager.organStock(recipe, detailList);
            if (Objects.isNull(organStock) || !organStock.getStock()) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("抱歉，医院没有库存，无法到医院取药，请选择其他购药方式。");
                return resultBean;
            }
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getPharmNo())) {
            String pharmNo = recipeExtend.getPharmNo();
            if (StringUtils.isNotEmpty(pharmNo)) {
                sb.append("选择到院自取后需去医院取药窗口取药：[" + organDTO.getName() + pharmNo + "取药窗口]");
            } else {
                sb.append("选择到院自取后，需去医院取药窗口取药");
            }
        }
        resultBean.setMsg(sb.toString());
        return resultBean;

    }
}
