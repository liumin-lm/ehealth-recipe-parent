package recipe.bussutil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.*;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * 电子处方工具类
 * @author jiangtingfeng
 * @date 2017/6/14.
 */
public class RecipeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeUtil.class);
    /**
     * 获取处方单上药品总价
     *
     * @param recipe
     * @param recipedetails
     */
    public static void getRecipeTotalPriceRange(Recipe recipe, List<Recipedetail> recipedetails) {
        if(CollectionUtils.isEmpty(recipedetails)){
            return;
        }
        List<Integer> drugIds = new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            drugIds.add(recipedetail.getDrugId());
        }
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);

        List<DrugList> drugLists = dao.findByDrugIds(drugIds);

        BigDecimal price1 = new BigDecimal(0);
        BigDecimal price2 = new BigDecimal(0);

        for (DrugList drugList : drugLists) {
            for (Recipedetail recipedetail : recipedetails) {
                if (null != drugList && drugList.getDrugId().equals(recipedetail.getDrugId()) ) {
                    price1 = price1.add(getTatolPrice(BigDecimal.valueOf(drugList.getPrice1()), recipedetail));
                    price2 = price2.add(getTatolPrice(BigDecimal.valueOf(drugList.getPrice2()), recipedetail));
                    break;
                }
            }
        }

        recipe.setPrice1(price1.divide(BigDecimal.ONE, 2, RoundingMode.UP));
        recipe.setPrice2(price2.divide(BigDecimal.ONE, 2, RoundingMode.UP));

    }

    /**
     * 获取药品总价
     *
     * @param price        单价
     * @param recipedetail 获取数量
     * @return
     */
    public static BigDecimal getTatolPrice(BigDecimal price, Recipedetail recipedetail) {
        return price.multiply(new BigDecimal(recipedetail.getUseTotalDose()));

    }

    /**
     * 药品获取医院价格
     *
     * @param dList
     */
    public static void getHospitalPrice(Integer organId, List<DrugListBean> dList) {
        List drugIds = new ArrayList();
        for (DrugListBean drugList : dList) {
            if (null != drugList) {
                drugIds.add(drugList.getDrugId());
            }
        }

        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugList = dao.findByOrganIdAndDrugIds(organId, drugIds);
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, dList.get(0).getDrugType()));
        //药品商品名拼接配置
        Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, dList.get(0).getDrugType()));
        // 设置医院价格
        List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList;
        for (DrugListBean drugList : dList) {
            for (OrganDrugList odlist : organDrugList) {
                if (null != drugList && null != odlist && drugList.getDrugId().equals(odlist.getDrugId())) {
                    drugList.setHospitalPrice(odlist.getSalePrice());
                    drugList.setOrganDrugCode(odlist.getOrganDrugCode());
                    //药品用法用量默认使用机构的，无机构数据则使用平台的，两者都无数据则为空
                    if (StringUtils.isNotEmpty(odlist.getUsePathways())){
                        drugList.setUsePathways(odlist.getUsePathways());
                    }
                    if (StringUtils.isNotEmpty(odlist.getUsingRate())){
                        drugList.setUsingRate(odlist.getUsingRate());
                    }
                    if (StringUtils.isNotEmpty(odlist.getUsePathwaysId())){
                        drugList.setUsePathwaysId(odlist.getUsePathwaysId());
                    }
                    if (StringUtils.isNotEmpty(odlist.getUsingRateId())){
                        drugList.setUsingRateId(odlist.getUsingRateId());
                    }
                    if (StringUtils.isNotEmpty(odlist.getDrugEntrust())){
                        drugList.setDrugEntrust(odlist.getDrugEntrust());
                    }
                    //历史用药入口--默认填充机构的，平台的不填充
                    drugList.setUseDose(odlist.getUseDose());
                    //剂型
                    drugList.setDrugForm(odlist.getDrugForm());
                    drugList.setRecommendedUseDose(odlist.getRecommendedUseDose());
                    drugList.setSmallestUnitUseDose(odlist.getSmallestUnitUseDose());
                    //默认最小单位剂量
                    drugList.setDefaultSmallestUnitUseDose(odlist.getDefaultSmallestUnitUseDose());
                    //剂量单位最小单位
                    drugList.setUseDoseSmallestUnit(odlist.getUseDoseSmallestUnit());
                    //设置医生端每次剂量和剂量单位联动关系
                    useDoseAndUnitRelationList = Lists.newArrayList();
                    //用药单位不为空时才返回给前端
                    if (StringUtils.isNotEmpty(drugList.getUseDoseUnit())){
                        useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getRecommendedUseDose(),drugList.getUseDoseUnit(),drugList.getUseDose()));
                    }
                    if (StringUtils.isNotEmpty(drugList.getUseDoseSmallestUnit())){
                        useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getDefaultSmallestUnitUseDose(),drugList.getUseDoseSmallestUnit(),drugList.getSmallestUnitUseDose()));
                    }
                    drugList.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
                    //前端展示的药品拼接名处理
                    drugList.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(drugList, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(drugList.getDrugType())));
                    //前端展示的药品商品名拼接名处理
                    drugList.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(drugList, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(drugList.getDrugType())));
                    break;
                }
            }
        }
    }

    /**
     * 从机构配置表中获取配置(可根据不同机构做不同配置)
     *
     * @param order
     * @param recipeList
     * @return
     */
    public static Map<String, Object> getParamFromOgainConfig(RecipeOrder order, List<Recipe> recipeList) {
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
        Integer organId = order.getOrganId();
        Map<String, Object> map = Maps.newHashMap();
        if (null != organId) {
            OrganConfigBean organConfig = iOrganConfigService.get(organId);
            if (null != organConfig) {
                map.put("serviceChargeDesc", organConfig.getServiceChargeDesc());
                map.put("serviceChargeRemark", organConfig.getServiceChargeRemark());
            }
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            BigDecimal otherFee = (BigDecimal)configurationService.getConfiguration(organId, "otherFee");
            if (null != otherFee && otherFee.compareTo(BigDecimal.ZERO) == 1 && null != configurationService.getConfiguration(organId, "otherServiceChargeDesc") && null != configurationService.getConfiguration(organId, "otherServiceChargeRemark")) {
                map.put("otherServiceChargeDesc", configurationService.getConfiguration(organId, "otherServiceChargeDesc").toString());
                map.put("otherServiceChargeRemark", configurationService.getConfiguration(organId, "otherServiceChargeRemark").toString());
            }
            if (order.getLogisticsCompany() != null) {
                try{
                    String logComStr = DictionaryController.instance().get("eh.cdr.dictionary.KuaiDiNiaoCode")
                            .getText(order.getLogisticsCompany());
                    map.put("logisticsCompanyPY", logComStr);
                }catch (Exception e){
                    LOGGER.info("getParamFromOgainConfig error msg:{}.", e.getMessage());
                }

            }
        }
        if (order.getEnterpriseId() != null) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipeList.get(0).getGiveMode())){
                //@ItemProperty(alias = "0:不支付药品费用，1:全部支付 【 1线上支付  非1就是线下支付】")
                map.put("storePayFlag",drugsEnterprise.getStorePayFlag());
            }
        }
        return map;
    }

    /**
     * 判断是否中药类处方
     *
     * @return
     */
    public static boolean isTcmType(int recipeType) {
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType) || RecipeBussConstant.RECIPETYPE_HP.equals(recipeType)) {
            return true;
        }

        return false;
    }


    /**
     * 处方单设置默认值
     */
    public static void setDefaultData(Recipe recipe) {
        if (null == recipe.getRecipeType()) {
            recipe.setRecipeId(0);
        }

        //默认为西药
        if (null == recipe.getRecipeType()) {
            recipe.setRecipeType(RecipeBussConstant.RECIPETYPE_WM);
        }

        //默认业务来源为无
        if (null == recipe.getBussSource()) {
            recipe.setBussSource(RecipeBussConstant.BUSS_SOURCE_NONE);
        }

        //默认流转模式为平台模式
        if (null == recipe.getRecipeMode()) {
            recipe.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        }

        //互联网模式默认为审方前置
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())){
            recipe.setReviewType(ReviewTypeConstant.Preposition_Check);
        }else {
            //设置运营平台设置的审方模式
            //互联网设置了默认值，平台没有设置默认值从运营平台取
            if (recipe.getReviewType() == null){
                try {
                    IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                    Integer reviewType = (Integer)configurationService.getConfiguration(recipe.getClinicOrgan(), "reviewType");
                    LOGGER.info("运营平台获取审方方式配置 reviewType[{}]",reviewType);
                    if (reviewType == null){
                        //默认审方后置
                        recipe.setReviewType(ReviewTypeConstant.Postposition_Check);
                    }else {
                        recipe.setReviewType(reviewType);
                    }
                }catch (Exception e){
                    LOGGER.error("获取运营平台审方方式配置异常",e);
                    //默认审方后置
                    recipe.setReviewType(ReviewTypeConstant.Postposition_Check);
                }
            }
        }

        //设置运营平台设置的审方途径
        if (recipe.getCheckMode() == null){
            try {
                IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                Integer checkMode = (Integer)configurationService.getConfiguration(recipe.getClinicOrgan(), "isOpenHisCheckRecipeFlag");
                if (checkMode == null){
                    //默认平台审方
                    recipe.setCheckMode(1);
                }else {
                    recipe.setCheckMode(checkMode);
                }
            }catch (Exception e){
                LOGGER.error("获取运营平台审方途径配置异常",e);
                //默认平台审方
                recipe.setCheckMode(1);
            }
        }
        
        //默认剂数为1
        if (recipe.getRecipeType() == 1 || recipe.getRecipeType() == 2) {
            recipe.setCopyNum(0);
        } else {
            if (null == recipe.getCopyNum() || recipe.getCopyNum() < 1) {
                recipe.setCopyNum(1);
            }
        }

        //默认无法医保支付
        if (null == recipe.getMedicalPayFlag()) {
            recipe.setMedicalPayFlag(0);
        }

        //默认可以医院，药企发药
        if (null == recipe.getDistributionFlag()) {
            recipe.setDistributionFlag(0);
        }

        //设置处方来源类型
        recipe.setRecipeSourceType(1);

        //设置处方支付类型 0 普通支付 1 不选择购药方式直接去支付
        recipe.setRecipePayType(0);

        //默认非外带处方
        recipe.setTakeMedicine(0);
        //监管同步标记
        recipe.setSyncFlag(0);

        //默认来源为纳里APP处方
        if (null == recipe.getFromflag()) {
            recipe.setFromflag(1);
        }

        //默认到院取药
        if (null == recipe.getGiveMode()) {
            recipe.setGiveMode(RecipeBussConstant.GIVEMODE_TO_HOS);
        }

        //默认未签名
        if (null == recipe.getStatus()) {
            recipe.setStatus(RecipeStatusConstant.UNSIGN);
        }

        if (null == recipe.getCreateDate()) {
            Date now = new Date();
            recipe.setCreateDate(now);
            recipe.setLastModify(now);
        }

        //默认有效天数
        if (null == recipe.getValueDays()) {
            recipe.setValueDays(3);
        }

        //判断诊断备注是否为空，若为空则显示“无”
//        if (StringUtils.isEmpty(recipe.getMemo())) {
//            recipe.setMemo("无");
//        }

        if (null == recipe.getPayFlag()) {
            recipe.setPayFlag(PayConstant.PAY_FLAG_NOT_PAY);
        }

        if (null == recipe.getChooseFlag()) {
            recipe.setChooseFlag(0);
        }

        if (null == recipe.getGiveFlag()) {
            recipe.setGiveFlag(0);
        }

        if (null == recipe.getRemindFlag()) {
            recipe.setRemindFlag(0);
        }

        if (null == recipe.getPushFlag()) {
            recipe.setPushFlag(0);
        }

        if (null == recipe.getPatientStatus()) {
            recipe.setPatientStatus(1);
        }

        //date 20191011
        //设置处方审核状态默认值
        if (null == recipe.getCheckStatus()) {
            recipe.setCheckStatus(0);
        }

        //设置抢单的默认状态
        recipe.setGrabOrderStatus(0);

    }

    //将；用|代替
    public static String getCode(String code) {
        return code.replace("；","|");
    }

}
