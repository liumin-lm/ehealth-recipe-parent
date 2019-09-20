package recipe.bussutil;

import com.google.common.collect.Maps;
import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 电子处方工具类
 * @author jiangtingfeng
 * @date 2017/6/14.
 */
public class RecipeUtil {

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
    public static void getHospitalPrice(Integer organId, List<DrugList> dList) {
        List drugIds = new ArrayList();
        for (DrugList drugList : dList) {
            if (null != drugList) {
                drugIds.add(drugList.getDrugId());
            }
        }

        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugList = dao.findByOrganIdAndDrugIds(organId, drugIds);
        // 设置医院价格
        for (DrugList drugList : dList) {
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
                    //历史用药入口--默认填充机构的，平台的不填充
                    drugList.setUseDose(odlist.getUseDose());
                    break;
                }
            }
        }
    }

    /**
     * 从机构配置表中获取配置(可根据不同机构做不同配置)
     *
     * @param order
     * @return
     */
    public static Map<String, String> getParamFromOgainConfig(RecipeOrder order) {
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
        Integer organId = order.getOrganId();
        Map<String, String> map = Maps.newHashMap();
        if (null != organId) {
            OrganConfigBean organConfig = iOrganConfigService.get(organId);
            if (null != organConfig) {
                map.put("serviceChargeDesc", organConfig.getServiceChargeDesc());
                map.put("serviceChargeRemark", organConfig.getServiceChargeRemark());
            }
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            BigDecimal otherFee = (BigDecimal)configurationService.getConfiguration(organId, "otherFee");
            if (otherFee.compareTo(BigDecimal.ZERO) == 1) {
                map.put("otherServiceChargeDesc", configurationService.getConfiguration(organId, "otherServiceChargeDesc").toString());
                map.put("otherServiceChargeRemark", configurationService.getConfiguration(organId, "otherServiceChargeRemark").toString());
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
     * 是否为取消处方，这些状态的处方 chooseflag也是1
     *
     * @return
     */
    public static boolean isCanncelRecipe(int status) {
        if (RecipeStatusConstant.REVOKE == status || RecipeStatusConstant.NO_PAY == status
                || RecipeStatusConstant.NO_OPERATOR == status) {
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

        //默认流转模式为平台模式
        if (null == recipe.getRecipeMode()) {
            recipe.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        }

        //互联网模式默认为审方前置
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())){
            recipe.setReviewType(RecipeBussConstant.AUDIT_PRE);
        }
        
        //默认剂数为1
        if (null == recipe.getCopyNum() || recipe.getCopyNum() < 1) {
            recipe.setCopyNum(1);
        }

        //默认无法医保支付
        if (null == recipe.getMedicalPayFlag()) {
            recipe.setMedicalPayFlag(0);
        }

        //默认可以医院，药企发药
        if (null == recipe.getDistributionFlag()) {
            recipe.setDistributionFlag(0);
        }

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
        if (StringUtils.isEmpty(recipe.getMemo())) {
            recipe.setMemo("无");
        }

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

    }

    //将；用|代替
    public static String getCode(String code) {
        return code.replace("；","|");
    }

}
