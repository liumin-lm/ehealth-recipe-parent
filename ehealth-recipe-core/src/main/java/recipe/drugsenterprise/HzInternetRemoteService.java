package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DeliveryList;
import com.ngari.his.recipe.mode.MedicalPreSettleReqTO;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.drugsenterprise.compatible.HzInternetRemoteNewType;
import recipe.drugsenterprise.compatible.HzInternetRemoteOldType;
import recipe.drugsenterprise.compatible.HzInternetRemoteTypeInterface;
import recipe.hisservice.RecipeToHisService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author gmw
 * @description 杭州互联网（金投）对接服务
 * @date 2019/9/11
 */
@RpcBean("hzInternetRemoteService")
public class HzInternetRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HzInternetRemoteService.class);

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HzInternetRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return getRealization(recipeIds).pushRecipeInfo(recipeIds, enterprise);
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    /*
     * @description 处方预结算
     * @author gaomw
     * @date 2019/12/13
     * @param [recipeId]
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    @Deprecated
    public DrugEnterpriseResult recipeMedicalPreSettleO(Integer recipeId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        MedicalPreSettleReqTO medicalPreSettleReqTO = new MedicalPreSettleReqTO();
        medicalPreSettleReqTO.setClinicOrgan(recipe.getClinicOrgan());

        //封装医保信息
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null && recipeExtend.getMedicalSettleData() != null) {
            medicalPreSettleReqTO.setHospOrgCode(recipeExtend.getHospOrgCodeFromMedical());
            medicalPreSettleReqTO.setInsuredArea(recipeExtend.getInsuredArea());
            medicalPreSettleReqTO.setMedicalSettleData(recipeExtend.getMedicalSettleData());
        } else {
            LOGGER.info("杭州互联网虚拟药企-未获取处方医保结算请求串-recipeId={}", JSONUtils.toString(recipe.getRecipeId()));
            result.setMsg("未获取处方医保结算请求串");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        //HisResponseTO hisResult = service.recipeMedicalPreSettle(medicalPreSettleReqTO);
        HisResponseTO hisResult = null;
        if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
            LOGGER.info("杭州互联网虚拟药企-处方预结算成功-his. param={},result={}", JSONUtils.toString(medicalPreSettleReqTO), JSONUtils.toString(hisResult));
            result.setCode(DrugEnterpriseResult.SUCCESS);
        } else {
            LOGGER.error("杭州互联网虚拟药企-处方预结算失败-his. param={},result={}", JSONUtils.toString(medicalPreSettleReqTO), JSONUtils.toString(hisResult));
            if (hisResult != null) {
                result.setMsg(hisResult.getMsg());
            }
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        return result;
    }

    /**
     * 原杭州互联网提交订单 前端会在orderforRecipe之前调用---0928版本去掉这部分逻辑--已废弃
     *
     * @param recipeId
     * @param extInfo
     * @return
     */
    @RpcService
    @Deprecated
    public DrugEnterpriseResult checkMakeOrder(Integer recipeId, Map<String, String> extInfo) {
        LOGGER.info("checkMakeOrder 当前确认订单校验的新流程预结算->同步配送信息, 入参：{}，{}", recipeId, JSONUtils.toString(extInfo));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

        /*result = recipeMedicalPreSettle(recipeId, null == extInfo.get("depId") ? null : Integer.parseInt(extInfo.get("depId").toString()));
        if (DrugEnterpriseResult.FAIL.equals(result.getCode())) {
            LOGGER.info("order 当前处方{}确认订单校验处方信息：预结算失败，结算结果：{}", recipeId, JSONUtils.toString(result));
            return result;
        }*/

        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

        //判断当前确认订单是配送方式
        if (StringUtils.isEmpty(extInfo.get("depId")) || StringUtils.isEmpty(extInfo.get("payMode"))) {
            LOGGER.info("order 当前处方{}确认订单校验处方信息,没有传递配送药企信息，无需同步配送信息，直接返回预结算结果", recipeId);
            return result;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(Integer.parseInt(extInfo.get("depId")));
        AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        return remoteService.sendMsgResultMap(recipeId, extInfo, result);

    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return getRealization(Lists.newArrayList(recipeId)).scanStock(recipeId, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return getRealization(recipeIds).findSupportDep(recipeIds, ext, enterprise);
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_HZ;
    }

    /*
     * @description 推送药企处方状态，由于只是个别药企需要实现，故有默认实现
     * @author gmw
     * @date 2019/9/18
     * @param rxId  recipeCode
     * @param status  status
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    @Override
    public DrugEnterpriseResult updatePrescriptionStatus(String rxId, int status) {
        LOGGER.info("更新处方状态");
        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);

        return drugEnterpriseResult;
    }

    @Override
    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        return getRealization(dbRecipe).scanStock(dbRecipe, dep, drugIds);
    }

    @Override
    public String appEnterprise(RecipeOrder order) {
        return getRealization(order).appEnterprise(order);
    }

    @Override
    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        return getRealization(order).orderToRecipeFee(order, recipeIds, payModeSupport, recipeFee, extInfo);
    }

    @Override
    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {
        getRealization(order).setOrderEnterpriseMsg(extInfo, order);
    }

    @Override
    public void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map) {
        LOGGER.info("checkRecipeGiveDeliveryMsg recipeBean:{}, map:{}", JSONUtils.toString(recipeBean), JSONUtils.toString(map));
        String giveMode = null != map.get("giveMode") ? map.get("giveMode").toString() : null;
        Object deliveryList = map.get("deliveryList");
        if (null != deliveryList && null != giveMode) {

            List<Map> deliveryLists = (List<Map>) deliveryList;
            //暂时按照逻辑只保存展示返回的第一个药企
            DeliveryList nowDeliveryList = JSON.parseObject(JSON.toJSONString(deliveryLists.get(0)), DeliveryList.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            if (null != nowDeliveryList) {
                Map<String, String> updateMap = Maps.newHashMap();
                updateMap.put("deliveryCode", nowDeliveryList.getDeliveryCode());
                updateMap.put("deliveryName", nowDeliveryList.getDeliveryName());
                //存放处方金额
                updateMap.put("deliveryRecipeFee", null != nowDeliveryList.getRecipeFee() ? nowDeliveryList.getRecipeFee().toString() : null);
                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeBean.getRecipeId(), updateMap);
            }
            //date 20200311
            //将his返回的批量药企信息存储下来，将信息分成|分割
            DeliveryList deliveryListNow;
            Map<String, String> updateMap = Maps.newHashMap();
            StringBuffer deliveryCodes = new StringBuffer().append("|");
            StringBuffer deliveryNames = new StringBuffer().append("|");
            StringBuffer deliveryRecipeFees = new StringBuffer().append("|");
            for (Map<String, String> delivery : deliveryLists) {
                deliveryListNow = JSON.parseObject(JSON.toJSONString(delivery), DeliveryList.class);
                deliveryCodes.append(deliveryListNow.getDeliveryCode()).append("|");
                deliveryNames.append(deliveryListNow.getDeliveryName()).append("|");
                deliveryRecipeFees.append(deliveryListNow.getRecipeFee()).append("|");
            }
            updateMap.put("deliveryCode", "|".equals(deliveryCodes) ? null : deliveryCodes.toString());
            updateMap.put("deliveryName", "|".equals(deliveryNames) ? null : deliveryNames.toString());
            //存放处方金额
            updateMap.put("deliveryRecipeFee", "|".equals(deliveryRecipeFees) ? null : deliveryRecipeFees.toString());
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeBean.getRecipeId(), updateMap);
            LOGGER.info("hisRecipeCheck 当前处方{}预校验，配送方式存储成功:{}！", recipeBean.getRecipeId(), JSONUtils.toString(updateMap));

        } else {
            LOGGER.info("hisRecipeCheck 当前处方{}预校验，配送方式没有返回药企信息！", recipeBean.getRecipeId());
        }
    }

    @Override
    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        getRealization(order).setEnterpriseMsgToOrder(order, depId, extInfo);
    }

    @Override
    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe) {
        return getRealization(dbRecipe).specialMakeDepList(drugsEnterprise, dbRecipe);
    }

    @Override
    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult) {
        return getRealization(Lists.newArrayList(recipeId)).sendMsgResultMap(recipeId, extInfo, payResult);
    }

    private HzInternetRemoteTypeInterface getRealization(List<Integer> recipeIds) {
        //判断其对应的模式(旧/新模式)
        //根据当前处方的recipecode
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        HzInternetRemoteTypeInterface result = new HzInternetRemoteOldType();
        if (null != recipeIds && CollectionUtils.isNotEmpty(recipeIds)) {
            Recipe recipe = recipeDAO.get(recipeIds.get(0));
            //当recipe没有关联上纳里平台的处方code说明是his同步过来的新流程
            if (null != recipe && null != recipe.getRecipeCode() && !recipe.getRecipeCode().contains("ngari")) {
                result = new HzInternetRemoteNewType();
            }
        }
        return result;
    }

    private HzInternetRemoteTypeInterface getRealization(RecipeOrder order) {
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        if (null != recipeIdList && CollectionUtils.isNotEmpty(recipeIdList)) {
            return getRealization(Lists.newArrayList(recipeIdList.get(0)));
        }
        return new HzInternetRemoteOldType();
    }

    private HzInternetRemoteTypeInterface getRealization(Recipe recipe) {
        if (null != recipe) {
            return getRealization(Lists.newArrayList(recipe.getRecipeId()));
        }
        return new HzInternetRemoteOldType();
    }

}
