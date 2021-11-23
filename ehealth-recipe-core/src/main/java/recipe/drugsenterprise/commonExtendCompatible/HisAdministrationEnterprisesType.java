package recipe.drugsenterprise.commonExtendCompatible;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DeliveryList;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.HisDeliveryConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.purchase.PayModeOnline;
import recipe.purchase.PurchaseService;
import recipe.service.RecipeLogService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("hisAdministrationEnterprisesType")
public class HisAdministrationEnterprisesType implements CommonExtendEnterprisesInterface{
    private static final Logger LOGGER = LoggerFactory.getLogger(HisAdministrationEnterprisesType.class);

    @Override
    //医院管理药企需要强实现(Y)
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        LOGGER.info("getDrugInventory-【his管理的药企】-获取库存");
        return "暂不支持库存查询";
    }

    @Override
    //医院管理药企需要强实现(Y)
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        LOGGER.info("getDrugInventoryForApp-【his管理的药企】-获取库存toAPP");
        return null;
    }

    @Override
    //逻辑上来说push推送，his管理的药企，由his进行推送，平台不需要手动的推送
    //医院管理药企需要强实现(Y)
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("pushRecipeInfo-【his管理的药企】-更新取药信息至处方流转平台开始，处方ID：{}.", JSONUtils.toString(recipeIds));

        //虚拟药企推送，修改配送信息的逻辑调整到前面确认订单
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        return result;
    }

    @Override
    //his管理的药企：药品，库存，价格全都由；医院返回的信息提供，判断库存现阶段由预校验的结果判断(Y)
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        //查询库存通过his预校验的返回判断库存是否足够
        LOGGER.info("scanStock-【his管理的药企】-虚拟药企库存入参为：{}，{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if(!valiScanStock(recipeId, drugsEnterprise, result)){
            return result;
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null != extend) {
            //获取当前his返回的药企信息，以及价格信息
            String deliveryRecipeFees = extend.getDeliveryRecipeFee();
            String deliveryCodes = extend.getDeliveryCode();
            String deliveryNames = extend.getDeliveryName();
            if (StringUtils.isNotEmpty(deliveryRecipeFees) &&
                    StringUtils.isNotEmpty(deliveryCodes) && StringUtils.isNotEmpty(deliveryNames)) {
                //只有杭州是互联网医院返回的是库存足够
                result.setCode(DrugEnterpriseResult.SUCCESS);
                result.setMsg("调用[" + drugsEnterprise.getName() + "][ scanStock ]结果返回成功,有库存,处方单ID:" + recipeId + ".");
                return result;
            }
        }
        return result;
    }

    private boolean valiScanStock(Integer recipeId, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result) {
        if (null == recipeId) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("传入的处方id为空！");
            return false;
        }
        return true;
    }

    @Override
    //医院管理药企，查询药企列表，按his返回信息展示，现阶段是预校验的时候返回的信息(Y)
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        LOGGER.info("findSupportDep-【his管理的药企】-虚拟药企导出入参为：{}，{}，{}", JSONUtils.toString(recipeIds), JSONUtils.toString(ext), JSONUtils.toString(enterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //校验入参
        if(!valiRequestDate(recipeIds, ext, result)){
            return result;
        }
        //date 20200311
        //修改逻辑：将his返回的药企列表信息回传成
        Integer recipeId = recipeIds.get(0);

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        if(null == recipe){
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("当前处方" + recipeIds.get(0) + "不存在！");
            return result;
        }
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        DrugsEnterprise drugsEnterprise;
        if(CollectionUtils.isNotEmpty(drugsEnterprises)){
            //这里杭州市互联网医院只配置一个虚拟药企
            drugsEnterprise = drugsEnterprises.get(0);
        }else{
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("当前医院"+ recipe.getClinicOrgan() +"没有设置关联药企！");
            return result;
        }

        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        List<DepDetailBean> depDetailList = new ArrayList<>();
        if(null != extend){
            //获取当前his返回的药企信息，以及价格信息
            String deliveryRecipeFees = extend.getDeliveryRecipeFee();
            String deliveryCodes = extend.getDeliveryCode();
            String deliveryNames = extend.getDeliveryName();
            DepDetailBean depDetailBean;
            if(StringUtils.isNotEmpty(deliveryRecipeFees) &&
                    StringUtils.isNotEmpty(deliveryCodes) && StringUtils.isNotEmpty(deliveryNames)){
                LOGGER.info("findSupportDepList-【his管理的药企】-当前处方{}的药企信息为his预校验返回信息：{}", recipeId, JSONUtils.toString(extend));
                String[] deliveryRecipeFeeList = deliveryRecipeFees.split("\\|");
                String[] deliveryCodeList = deliveryCodes.split("\\|");
                String[] deliveryNameList = deliveryNames.split("\\|");

                for(int i = 1; i < deliveryRecipeFeeList.length ; i++){
                    //过滤支持药柜配送药企
                    if(HisDeliveryConstant.YG_HIS_DELIVERY_CODE.equals(deliveryCodeList[i])) {
                        continue;
                    }
                    depDetailBean = new DepDetailBean();
                    //标识选择的药企是his推过来的
                    depDetailBean.setDepId(drugsEnterprise.getId());
                    depDetailBean.setDepName(deliveryNameList[i]);
                    depDetailBean.setRecipeFee(new BigDecimal(deliveryRecipeFeeList[i]));
                    depDetailBean.setBelongDepName(deliveryNameList[i]);
                    depDetailBean.setOrderType(1);
                    depDetailBean.setPayModeText("在线支付");
                    depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
                    //预留字段标识是医院推送给过来的
                    depDetailBean.setHisDep(true);
                    depDetailBean.setHisDepCode(deliveryCodeList[i]);
                    //date 20200311
                    //医院返回的药企处方金额
                    depDetailBean.setHisDepFee(new BigDecimal(deliveryRecipeFeeList[i]));
                    depDetailBean.setType(2); //药店
                    depDetailList.add(depDetailBean);
                }
            }
        }
        LOGGER.info("findSupportDepList-【his管理的药企】-虚拟药企处方{}查询his药企列表展示信息：{}", recipeId, JSONUtils.toString(depDetailList));
        result.setObject(depDetailList);
        return result;
    }

    private Boolean valiRequestDate(List<Integer> recipeIds, Map ext, DrugEnterpriseResult result) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setError("传入的处方id为空！");
            return false;
        }
        return true;
    }

    /**
     * 返回调用信息
     *
     * @param result DrugEnterpriseResult
     * @param msg    提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        LOGGER.info("getDrugEnterpriseResult-【his管理的药企】-提示信息：{}.", msg);
        return result;
    }

    @Override
    //his管理的药企：药品，库存，价格全都由；医院返回的信息提供，判断库存现阶段由预校验的结果判断(Y/+1)
    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        LOGGER.info("scanStock-【his管理的药企】- recipeId:{},dep:{},drugIds:{}", dbRecipe.getRecipeId(), JSONUtils.toString(dep), JSONUtils.toString(drugIds));
        DrugEnterpriseResult result = scanStock(dbRecipe.getRecipeId(), dep);
        LOGGER.info("scanStock-【his管理的药企】- recipeId:{}, result:{}", dbRecipe.getRecipeId(), JSONUtils.toString(result));
        boolean equals = result.getCode().equals(DrugEnterpriseResult.SUCCESS);
        LOGGER.info("scanStock-【his管理的药企】- 请求虚拟药企返回：{}", equals);
        return equals;
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails, List<SaleDrugList> saleDrugLists) {
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        drugStockAmountDTO.setResult(true);
        return drugStockAmountDTO;
    }

    @Override
    //his管理的药企：设置app端的额外药企信息；医院返回的信息提供，现阶段由预校验结果展示(Y/+1)
    public String appEnterprise(RecipeOrder order) {
        LOGGER.info("appEnterprise-【his管理的药企】-order:{}", JSONUtils.toString(order));
        String hisEnterpriseName = null;
        if (null != order) {

            hisEnterpriseName = order.getHisEnterpriseName();
        }
        LOGGER.info("appEnterprise-【his管理的药企】-请求虚拟药企返回：{}", hisEnterpriseName);
        return hisEnterpriseName;
    }

    @Override
    //his管理的药企：设置订单时候产生的金额；医院返回的信息提供，现阶段由预校验结果存储(Y/+1)
    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo) {
        LOGGER.info("orderToRecipeFee-【his管理的药企】- order:{}, recipeIds:{}, payModeSupport:{}, recipeFee:{}, extInfo:{}",
                JSONUtils.toString(order), JSONUtils.toString(recipeIds), JSONUtils.toString(payModeSupport), recipeFee, JSONUtils.toString(extInfo));
        BigDecimal depFee = recipeFee;
        String hisDepFee = extInfo.get("hisDepFee");
        if(StringUtils.isNotEmpty(hisDepFee)){
            depFee = new BigDecimal(hisDepFee);
        }
        LOGGER.info("orderToRecipeFee-【his管理的药企】- 请求虚拟药企返回：{}", depFee);
        return depFee;
    }

    @Override
    //his管理的药企：确认订单时候展示药企信息；医院返回的信息提供，现阶段由预校验结果存储(Y/+1)
    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order) {
        LOGGER.info("setOrderEnterpriseMsg-【his管理的药企】- extInfo：{}，order：{}", JSONUtils.toString(extInfo), JSONUtils.toString(order));
        if(null != extInfo){
            //date 20200312
            //设置患者选中his回传信息
            order.setHisEnterpriseName(extInfo.get("depName"));
            order.setHisEnterpriseCode(extInfo.get("hisDepCode"));
        }
        LOGGER.info("setOrderEnterpriseMsg-【his管理的药企】- 当前虚拟药企组装的订单：{}", JSONUtils.toString(order));
    }

    @Override
    //his管理的药企：将处方对应支持的药企信息关联处方；现阶段在【预校验】的时候，将his产生的药企信息放入扩展表中(Y/+1)
    public void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map) {
        LOGGER.info("checkRecipeGiveDeliveryMsg-【his管理的药企】- recipeBean:{}, map:{}", JSONUtils.toString(recipeBean), JSONUtils.toString(map));
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
            LOGGER.info("hisRecipeCheck-【his管理的药企】- 当前处方{}预校验，配送方式存储成功:{}！", recipeBean.getRecipeId(), JSONUtils.toString(updateMap));

        } else {
            LOGGER.info("hisRecipeCheck-【his管理的药企】- 当前处方{}预校验，配送方式没有返回药企信息！", recipeBean.getRecipeId());
        }
    }

    @Override
    //his管理的药企：订单生成的时候设置药企信息；将预校验产生的药企信息关联到订单上(Y/+1)
    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo) {
        LOGGER.info("setEnterpriseMsgToOrder-【his管理的药企】- order:{}, depId:{}，extInfo:{} ", JSONUtils.toString(order), depId, JSONUtils.toString(extInfo));
        order.setEnterpriseId(depId);
        if(null != extInfo){
            //date 20200312
            //设置患者选中his回传信息
            order.setHisEnterpriseName(extInfo.get("depName"));
            order.setHisEnterpriseCode(extInfo.get("hisDepCode"));
        }
        LOGGER.info("setEnterpriseMsgToOrder-【his管理的药企】- 当前虚拟药企组装的订单：{}", JSONUtils.toString(order));
    }

    @Override
    //his管理的药企：药企筛选列表；his维护的药企不在平台维护，需要去除不用的虚拟药企(Y/+1)
    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe) {
        LOGGER.info("【his管理的药企】- 药企个性化展示药企列表 specialMakeDepList drugsEnterprise:{}，dbRecipe:{} "
                , JSONUtils.toString(drugsEnterprise), JSONUtils.toString(dbRecipe));
        return true;
    }

    @Override
    //his管理的药企：配送信息同步到his，现阶段是his去做的同步，之后的版本可能会改动(Y/+1)
    public void sendDeliveryMsgToHis(Integer recipeId) {
        LOGGER.info("【his管理的药企】-确认订单后同步配送信息 sendDeliveryMsgToHis recipeId:{}", recipeId);
        return;
    }

    @Override
    //his管理的药企：在支付前，进行【预结算】成功之后，处方的推送配送信息给医院的行为；
    //**这个接口之所以放在这里是因为，现阶段是将同步配送信息放置在【预结算】之后，可以根据之后的实现调整(Y/+1)
    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult) {
        LOGGER.info("sendMsgResultMap-【his管理的药企】-杭州互联网虚拟药企确认订单前检验订单信息同步配送信息，入参：dbRecipe:{},extInfo:{},payResult:{]",
                recipeId, JSONUtils.toString(extInfo), JSONUtils.toString(payResult));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        PayModeOnline service = (PayModeOnline)purchaseService.getService(1);
        HisResponseTO resultSave = service.updateGoodsReceivingInfoToCreateOrder(recipeId, extInfo);

        if(null != resultSave) {
            if(!resultSave.isSuccess()){
                payResult.setCode(DrugEnterpriseResult.FAIL);
                payResult.setMsg("同步配送信息失败");
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                        recipe.getStatus(), "处方" + recipe.getRecipeId() + "同步配送信息给his失败");
                LOGGER.info("order 当前处方确认订单的his同步配送信息失败，返回：{}", JSONUtils.toString(resultSave));
                return payResult;
            }
            return payResult;
        }else {
            LOGGER.info("order 当前处方{}没有对接同步配送信息，默认成功！", recipeId);
            return payResult;
        }
    }
}