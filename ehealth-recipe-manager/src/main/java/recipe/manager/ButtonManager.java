package recipe.manager;

import com.alibaba.fastjson.JSONArray;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.platform.recipe.mode.RecipeResultBean;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.base.constant.CardTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeOrderDAO;
import recipe.enumerate.type.PayButtonEnum;
import recipe.enumerate.type.RecipeDistributionFlagEnum;
import recipe.enumerate.type.RecipeSendTypeEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.factoryManager.button.IGiveModeBase;
import recipe.factoryManager.button.impl.BjGiveModeServiceImpl;
import recipe.factoryManager.button.impl.CommonGiveModeServiceImpl;
import recipe.factoryManager.button.impl.FromHisDeliveryCodeServiceImpl;

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
    private FromHisDeliveryCodeServiceImpl fromHisDeliveryCodeService;
//    @Autowired
//    private GiveModeManager giveModeManager;


    /**
     * 获取支付按钮 仅杭州市互联网医院使用
     *
     * @param organId  机构id
     * @param cardType 患者卡类型
     * @param isForce  是否强制自费
     * @return
     */
    public Integer getPayButton(Integer organId, String cardType, Boolean isForce) {
        logger.info("ButtonManager.getPayButton req organId={} cardType={} isForce={}",organId,cardType,isForce);

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
     * 传入库存信息,获取处方的购药方式
     *
     * @param scanResult
     * @param supportDepList
     * @param checkFlag
     * @param recipeId
     * @param organId
     * @return
     */
    public List<Integer> getRecipeGiveMode(com.ngari.platform.recipe.mode.RecipeResultBean scanResult, List<DrugsEnterprise> supportDepList, int checkFlag, Integer recipeId, int organId, List<String> configurations) {
        logger.info("getRecipeGiveMode scanResult = {} supportDepList= {} checkFlag={} recipeId={} organId={} configurations = {}", JSONArray.toJSONString(scanResult), JSONArray.toJSONString(supportDepList), checkFlag, recipeId, organId, JSONArray.toJSONString(configurations));
        List<Integer> recipeSupportGiveModeList = new ArrayList<>();
        switch (checkFlag) {
            case 1:
                if (RecipeResultBean.SUCCESS.equals(scanResult.getCode())) {
                    recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
                }
                break;
            case 2:
                recipeSupportGiveModeList = getGiveModeBuEnterprise(supportDepList, recipeSupportGiveModeList, recipeId, organId);
                break;
            case 3:
                recipeSupportGiveModeList = getGiveModeBuEnterprise(supportDepList, recipeSupportGiveModeList, recipeId, organId);
                if (RecipeResultBean.SUCCESS.equals(scanResult.getCode())) {
                    recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
                }
                break;
            default:
                break;
        }
        setOtherGiveMode(configurations, recipeId, organId, recipeSupportGiveModeList);
        logger.info("getRecipeGiveMode  recipeId= {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
        return recipeSupportGiveModeList;
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
//    public GiveModeShowButtonDTO getGiveModeSettingFromYypt(Integer organId) {
//        return getGiveModeSettingFromYypt(organId);
//    }

    @Autowired
    private IScratchableService scratchableService;

    public GiveModeShowButtonDTO getGiveModeSettingFromYypt(Integer organId) {
        List<GiveModeButtonDTO> giveModeButtonBeans = new ArrayList<>();
        GiveModeShowButtonDTO giveModeShowButtonVO = new GiveModeShowButtonDTO();
        List<ScratchableBean> scratchableBeans = scratchableService.findScratchableByPlatform("myRecipeDetailList", organId + "", 1);
        scratchableBeans.forEach(giveModeButton -> {
            GiveModeButtonDTO giveModeButtonBean = new GiveModeButtonDTO();
            giveModeButtonBean.setShowButtonKey(giveModeButton.getBoxLink());
            giveModeButtonBean.setShowButtonName(giveModeButton.getBoxTxt());
            giveModeButtonBean.setButtonSkipType(giveModeButton.getRecipeskip());
            if (!"listItem".equals(giveModeButtonBean.getShowButtonKey())) {
                giveModeButtonBeans.add(giveModeButtonBean);
            } else {
                giveModeShowButtonVO.setListItem(giveModeButtonBean);
            }
        });
        giveModeShowButtonVO.setGiveModeButtons(giveModeButtonBeans);
        if (giveModeShowButtonVO.getListItem() == null) {
            //说明运营平台没有配置列表
            GiveModeButtonDTO giveModeButtonBean = new GiveModeButtonDTO();
            giveModeButtonBean.setShowButtonKey("listItem");
            giveModeButtonBean.setShowButtonName("列表项");
            giveModeButtonBean.setButtonSkipType("1");
            giveModeShowButtonVO.setListItem(giveModeButtonBean);
        }
        return giveModeShowButtonVO;
    }


    /**
     * 获取按钮
     *
     * @return
     */
    public List<String> getGiveMode(Integer recipeId, Integer organId) {
        logger.info("DrugStockBusinessService.configurations recipeId={} organId={}", recipeId, organId);
        //添加按钮配置项key
        GiveModeShowButtonDTO giveModeShowButtonVO = getGiveModeSettingFromYypt(organId);
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        if (null == giveModeButtonBeans) {
            return null;
        }
        List<String> configurations = giveModeButtonBeans.stream().map(GiveModeButtonDTO::getShowButtonKey).collect(Collectors.toList());
        //收集按钮信息用于判断校验哪边库存 0是什么都没有，1是指配置了到院取药，2是配置到药企相关，3是医院药企都配置了
        if (CollectionUtils.isEmpty(configurations)) {
            return null;
        }
        logger.info("DrugStockBusinessService.configurations res={}", JSONArray.toJSONString(configurations));
        return configurations;
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
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
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


    /**
     * 例外支付下载处方
     *
     * @param configurations
     * @param recipeId
     * @param organId
     * @param recipeSupportGiveModeList
     * @return
     */
    private List<Integer> setOtherGiveMode(List<String> configurations, Integer recipeId, int organId, List<Integer> recipeSupportGiveModeList) {
        // 查询药品是否不支持下载处方
        if (configurations.contains(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText())) {
            Integer integer = organDrugListDAO.countIsSupperDownloadRecipe(organId, recipeId);
            if (integer == 0) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType());
            }
        }
        // 例外支付 只要机构配置了就支持
        if (configurations.contains(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getType());
        }
        return recipeSupportGiveModeList;
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
                    return fromHisDeliveryCodeService;
                }
            }
        }
        return commonGiveModeService;
    }

}
