package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import eh.base.constant.CardTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OperationClient;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.enumerate.type.*;
import recipe.factoryManager.button.IGiveModeBase;
import recipe.factoryManager.button.impl.BjGiveModeServiceImpl;
import recipe.factoryManager.button.impl.CommonGiveModeServiceImpl;
import recipe.factoryManager.button.impl.FromHisGiveModeServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 按钮 manager
 * @author： whf
 * @date： 2021-08-19 9:38
 */
@Service
public class ButtonManager extends BaseManager {
    private static final String medicalPayConfigKey = "medicalPayConfig";
    private static final String provincialMedicalPayFlagKey = "provincialMedicalPayFlag";

    @Autowired
    private CommonGiveModeServiceImpl commonGiveModeService;
    @Autowired
    private BjGiveModeServiceImpl bjGiveModeService;
    @Autowired
    private FromHisGiveModeServiceImpl fromHisGiveModeServiceImpl;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;

    /**
     * 获取支付按钮 仅杭州市互联网医院使用
     *
     * @param organId  机构id
     * @param cardType 患者卡类型
     * @param isForce  是否强制自费
     * @return
     */
    public Integer getPayButton(Integer organId, String cardType, Boolean isForce) {
        logger.info("ButtonManager.getPayButton req organId={} cardType={} isForce={}", organId, cardType, isForce);

        // 强制自费 + 健康卡 展示 自费支付
        if (isForce) {
            return PayButtonEnum.MY_PAY.getType();
        }
        Boolean valueBooleanCatch = configurationClient.getPropertyByClientId(medicalPayConfigKey);
        Integer valueCatch = configurationClient.getValueCatchReturnInteger(organId, provincialMedicalPayFlagKey, 0);
        logger.info("ButtonManager.getPayButton  valueCatch={} valueBooleanCatch={}", valueCatch, valueBooleanCatch);
        if (valueBooleanCatch && valueCatch > 1 && CardTypeEnum.INSURANCECARD.getValue().equals(cardType)) {
            return PayButtonEnum.MEDICAL_PAY.getType();
        }
        return PayButtonEnum.MY_PAY.getType();
    }


    /**
     * 传入药企信息
     *
     * @param supportDepList
     * @param recipeSupportGiveModeList
     * @param recipeId
     * @param organId
     * @return
     */
    public List<Integer> getGiveModeBuEnterprise(List<DrugsEnterprise> supportDepList, List<Integer> recipeSupportGiveModeList, Integer recipeId, int organId) {
        Set<Integer> sendTypes = new HashSet<>();
        // 获取所有药企支持的购药方式
        if (CollectionUtils.isNotEmpty(supportDepList)) {
            Set<Integer> collect = supportDepList.stream().map(drugsEnterprise -> {
                Integer payModeSupport = drugsEnterprise.getPayModeSupport();
                Integer sendType = drugsEnterprise.getSendType();
                sendTypes.add(sendType);
                if (RecipeDistributionFlagEnum.drugsEnterpriseAll.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE.getType();
                } else if (RecipeDistributionFlagEnum.drugsEnterpriseTo.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE_TO.getType();
                } else if (RecipeDistributionFlagEnum.drugsEnterpriseSend.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE_SEND.getType();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());

            // 是否支持到店自取
            boolean drugHaveTo = collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE_TO.getType());
            // 是否支持配送
            boolean drugHaveSend = collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE_SEND.getType());
            // 根据药企的配送方式获取支持模式
            if (collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE.getType()) || (drugHaveTo && drugHaveSend)) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
                sendTypes(sendTypes, recipeSupportGiveModeList);
            } else if (drugHaveTo) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
            } else if (drugHaveSend) {
                // 根据配送主体区分医院配送还是药企配送
                sendTypes(sendTypes, recipeSupportGiveModeList);
            }
            logger.info("getGiveModeWhenContinueOne  recipeId= {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
            return recipeSupportGiveModeList;
        } else {
            logger.info("getGiveModeWhenContinueOne 药企没有库存 recipeId = {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
            return recipeSupportGiveModeList;
        }
    }

    /**
     * 通过机构ID从运营平台获取购药方式的基本配置项
     *
     * @param organId 机构ID
     * @return 运营平台的配置项
     */
    public GiveModeShowButtonDTO getGiveModeSettingFromYypt(Integer organId) {
        return operationClient.getGiveModeSettingFromYypt(organId);
    }


    /**
     * 校验 药品库存 全部药企的库存数量
     * 根据 按钮配置 获取 药企购药配置-库存对象
     *
     * @param organId 机构id
     * @return 药品信息 一定存在于出参
     */
    public List<EnterpriseStock> enterpriseStockCheck(Integer organId) {
        //获取需要查询库存的药企对象
        List<DrugsEnterprise> enterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        logger.info("ButtonManager enterpriseStockCheck organId:{},enterprises:{}", organId, JSON.toJSONString(enterprises));
        List<EnterpriseStock> list = new LinkedList<>();
        if (CollectionUtils.isEmpty(enterprises)) {
            return list;
        }
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        Map<String, String> configGiveModeMap = new HashMap<>();
        if (null != giveModeButtonBeans) {
            configGiveModeMap = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        }
        List<String> configGiveMode = RecipeSupportGiveModeEnum.checkEnterprise(giveModeButtonBeans);
        for (DrugsEnterprise drugsEnterprise : enterprises) {
            EnterpriseStock enterpriseStock = new EnterpriseStock();
            enterpriseStock.setDrugsEnterprise(drugsEnterprise);
            enterpriseStock.setDrugsEnterpriseId(drugsEnterprise.getId());
            enterpriseStock.setDeliveryName(drugsEnterprise.getName());
            enterpriseStock.setDeliveryCode(drugsEnterprise.getId().toString());
            enterpriseStock.setAppointEnterpriseType(AppointEnterpriseTypeEnum.ENTERPRISE_APPOINT.getType());
            List<GiveModeButtonDTO> giveModeButton = RecipeSupportGiveModeEnum.giveModeButtonList(drugsEnterprise, configGiveMode, configGiveModeMap);
            enterpriseStock.setGiveModeButton(giveModeButton);
            list.add(enterpriseStock);
        }
        logger.info("ButtonManager enterpriseStockCheck list:{}", JSON.toJSONString(list));
        return list;
    }

    public GiveModeShowButtonDTO getShowButton(Recipe recipe) {
        IGiveModeBase giveModeBase = getGiveModeBaseByRecipe(recipe);
        GiveModeShowButtonDTO giveModeShowButtonDTO = giveModeBase.getShowButton(recipe);
        //设置特殊按钮
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        giveModeBase.setSpecialItem(giveModeShowButtonDTO, recipe, recipeExtend);
        return giveModeShowButtonDTO;
    }

    public GiveModeShowButtonDTO getShowButtonV1(Recipe recipe) {
        IGiveModeBase giveModeBase = getGiveModeBaseByRecipe(recipe);
        GiveModeShowButtonDTO giveModeShowButtonDTO = giveModeBase.getShowButtonV1(recipe);
        //设置特殊按钮
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        giveModeBase.setSpecialItem(giveModeShowButtonDTO, recipe, recipeExtend);
        return giveModeShowButtonDTO;
    }

    /**
     * 配送方式文案
     *
     * @param recipe
     * @return
     */
    public String getGiveModeTextByRecipe(Recipe recipe) {
        if (recipe == null) {
            return "";
        }
        String giveModeKey;
        if (new Integer(1).equals(recipe.getGiveMode()) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (null == recipeOrder) {
                return "";
            }
            if (new Integer(1).equals(recipeOrder.getSendType())) {
                //表示医院配送
                giveModeKey = "showSendToHos";
            } else {
                //表示药企配送
                giveModeKey = "showSendToEnterprises";
            }
        } else if (new Integer(2).equals(recipe.getGiveMode())) {
            //表示到院取药
            giveModeKey = "supportToHos";
        } else if (new Integer(3).equals(recipe.getGiveMode())) {
            giveModeKey = "supportTFDS";
        } else if (new Integer(4).equals(recipe.getGiveMode())) {
            giveModeKey = "supportDownload";
        } else {
            giveModeKey = "";
        }
        GiveModeShowButtonDTO giveModeShowButtonVO = getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        Map<String, String> result = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        return result.get(giveModeKey);
    }

    /**
     * 根据配送主体获取购药方式
     *
     * @param sendTypes
     * @param recipeSupportGiveModeList
     */
    private void sendTypes(Set<Integer> sendTypes, List<Integer> recipeSupportGiveModeList) {
        if (CollectionUtils.isEmpty(sendTypes)) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
            return;
        }
        boolean alReadyPay = sendTypes.contains(RecipeSendTypeEnum.ALRAEDY_PAY.getSendType());
        boolean noPay = sendTypes.contains(RecipeSendTypeEnum.NO_PAY.getSendType());
        if (alReadyPay && noPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
            return;
        }
        if (alReadyPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            return;
        }
        if (noPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
        }
    }


    private IGiveModeBase getGiveModeBaseByRecipe(Recipe recipe) {
        if (new Integer(2).equals(recipe.getRecipeSource())) {
            //表示来源于线下转线上的处方单
            HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
            //只有北京互联网医院DeliveryCode是不为空的
            if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getDeliveryCode())) {
                return bjGiveModeService;
            }
        }
        //杭州市互联网用预校验中his返回的deliveryCode作为配送方式的选择
        Integer recipeId = recipe.getRecipeId();
        if (null != recipeId) {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            OrganDTO organDTO = organClient.organDTO(recipe.getClinicOrgan());
            //判断是不是杭州互联网医院
            if (null != organDTO && organDTO.getManageUnit().indexOf("eh3301") != -1 && RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                if (null != recipeExtend && StringUtils.isNotEmpty(recipeExtend.getDeliveryCode())) {
                    return fromHisGiveModeServiceImpl;
                }
            }
        }
        return commonGiveModeService;
    }

}
