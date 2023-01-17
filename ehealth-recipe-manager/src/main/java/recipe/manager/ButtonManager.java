package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.JSONUtils;
import eh.base.constant.CardTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OperationClient;
import recipe.client.RevisitClient;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.enumerate.status.RecipeSourceTypeEnum;
import recipe.enumerate.type.*;
import recipe.factoryManager.button.IGiveModeBase;
import recipe.factoryManager.button.impl.BjGiveModeServiceImpl;
import recipe.factoryManager.button.impl.CommonGiveModeServiceImpl;
import recipe.factoryManager.button.impl.FromHisGiveModeServiceImpl;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import javax.persistence.criteria.CriteriaBuilder;
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
    @Autowired
    private RevisitClient revisitClient;

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
     * @return 药企信息
     */
    public List<EnterpriseStock> enterpriseStockCheck(Integer organId, Integer recipeType, String decoctionId, List<Recipedetail> recipeDetails) {
        /**获取需要查询库存的药企对象 ，通过药企流转关系筛选*/
        List<DrugsEnterprise> enterprises = this.organAndEnterprise(organId, recipeType, decoctionId);
        if (CollectionUtils.isEmpty(enterprises)) {
            return new LinkedList<>();
        }
        // 获取药品的剂型
        List<String> drugIds = recipeDetails.stream().map(Recipedetail::getOrganDrugCode).collect(Collectors.toList());
        Set<Integer> recipeIds = recipeDetails.stream().map(Recipedetail::getRecipeId).collect(Collectors.toSet());
        Set<Integer> medicalFlag = getMedicalFlag(recipeIds);

        List<OrganDrugList> drugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organId, drugIds);
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        Map<String, String> configGiveModeMap = new HashMap<>();
        if (null != giveModeButtonBeans) {
            configGiveModeMap = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        }
        List<String> configGiveMode = RecipeSupportGiveModeEnum.checkEnterprise(giveModeButtonBeans);
        // 到院取药是否采用药企管理模式
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(organId, "drugToHosByEnterprise", false);
        List<OrganAndDrugsepRelation> relation = organAndDrugsepRelationDAO.findByOrganId(organId);

        Map<String, OrganAndDrugsepRelation> drugsDepRelationMap = relation.stream().collect(Collectors.toMap(drugsDepRelation -> drugsDepRelation.getOrganId() + "_" + drugsDepRelation.getDrugsEnterpriseId(), a -> a, (k1, k2) -> k1));
        List<EnterpriseStock> list = new ArrayList<>();
        for (DrugsEnterprise drugsEnterprise : enterprises) {
            EnterpriseStock enterpriseStock = new EnterpriseStock();
            enterpriseStock.setDrugsEnterprise(drugsEnterprise);
            enterpriseStock.setDrugsEnterpriseId(drugsEnterprise.getId());
            enterpriseStock.setDeliveryName(drugsEnterprise.getName());
            enterpriseStock.setDeliveryCode(drugsEnterprise.getId().toString());
            enterpriseStock.setAppointEnterpriseType(AppointEnterpriseTypeEnum.ENTERPRISE_APPOINT.getType());
            OrganAndDrugsepRelation drugsDepRelation = drugsDepRelationMap.get(organId + "_" + drugsEnterprise.getId());
            if (StringUtils.isNotEmpty(drugsDepRelation.getCannotMedicalFlag()) && CollectionUtils.isNotEmpty(medicalFlag)) {
                List<Integer> medicalFlags = JSONUtils.parse((drugsDepRelation.getCannotMedicalFlag()), List.class);
                Set<Integer> finalMedicalFlag = medicalFlag;
                List<Integer> integers = medicalFlags.stream().filter(s -> finalMedicalFlag.contains(s)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(integers)) {
                    continue;
                }
            }
            logger.info("ButtonManager enterpriseStockCheck configGiveMode:{},configGiveModeMap:{},drugToHosByEnterprise:{},drugsDepRelation:{}", JSON.toJSONString(configGiveMode), JSON.toJSONString(configGiveModeMap), drugToHosByEnterprise, JSON.toJSONString(drugsDepRelation));
            List<GiveModeButtonDTO> giveModeButton = RecipeSupportGiveModeEnum.giveModeButtonList(configGiveMode, configGiveModeMap, drugToHosByEnterprise, drugsDepRelation);
            if (!checkSendGiveMode(organId, drugsEnterprise.getId(), drugLists) && CollectionUtils.isNotEmpty(giveModeButton)) {
                giveModeButton = giveModeButton.stream().filter(a -> !RecipeSupportGiveModeEnum.enterpriseSendList.contains(a.getShowButtonKey())).collect(Collectors.toList());
                enterpriseStock.setSendFlag(false);
            }
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
     * 药企流转权限判断
     *
     * @param organId     机构id
     * @param recipeType  处方类型
     * @param decoctionId 煎法id
     * @return 符合流转关系的药企
     */
    public List<DrugsEnterprise> organAndEnterprise(Integer organId, Integer recipeType, String decoctionId) {
        //获取需要查询库存的药企对象
        List<DrugsEnterprise> enterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        logger.info("ButtonManager organAndEnterprise organId:{},enterprises:{}", organId, JSON.toJSONString(enterprises));
        if (CollectionUtils.isEmpty(enterprises)) {
            return null;
        }
        List<Integer> ids = enterprises.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        List<OrganAndDrugsepRelation> organAndEnterpriseList = organAndDrugsepRelationDAO.findByOrganIdEntId(organId, ids);
        logger.info("ButtonManager organAndEnterprise organAndEnterpriseList:{},recipeType={},decoctionId={}", JSON.toJSONString(organAndEnterpriseList), recipeType, decoctionId);
        if (CollectionUtils.isEmpty(organAndEnterpriseList)) {
            return null;
        }
        List<Integer> enterprisesIds = new ArrayList<>();
        organAndEnterpriseList.forEach(a -> {
            //没配置处方类型流转权限
            if (StringUtils.isEmpty(a.getEnterpriseRecipeTypes())) {
                return;
            }
            //不查找处方类型对应权限
            if (ValidateUtil.validateObjects(recipeType)) {
                enterprisesIds.add(a.getDrugsEnterpriseId());
                return;
            }
            //匹配处方类型权限
            List<Integer> recipeTypes = Arrays.stream(a.getEnterpriseRecipeTypes().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            if (!recipeTypes.contains(recipeType)) {
                return;
            }
            //没配置煎法流转权限
            if (RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipeType) && StringUtils.isEmpty(a.getEnterpriseDecoctionIds())) {
                return;
            }
            //不查找煎法对应权限
            if (StringUtils.isEmpty(decoctionId)) {
                enterprisesIds.add(a.getDrugsEnterpriseId());
                return;
            }
            //匹配煎法权限
            List<Integer> decoctionIds = Arrays.stream(a.getEnterpriseDecoctionIds().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            if (!decoctionIds.contains(-1) && !decoctionIds.contains(Integer.valueOf(decoctionId))) {
                return;
            }
            enterprisesIds.add(a.getDrugsEnterpriseId());
        });
        logger.info("ButtonManager organAndEnterprise enterprisesIds:{}", JSON.toJSONString(enterprisesIds));
        if (CollectionUtils.isEmpty(enterprisesIds)) {
            return null;
        }
        return enterprises.stream().filter(a -> enterprisesIds.contains(a.getId())).collect(Collectors.toList());
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


    /**
     * 药品剂型是否可以配送
     *
     * @param organId
     * @param enterpriseId
     * @param drugLists
     * @return
     */
    private Boolean checkSendGiveMode(Integer organId, Integer enterpriseId, List<OrganDrugList> drugLists) {
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, enterpriseId);
        if (StringUtils.isEmpty(relation.getEnterpriseDrugForm()) || "null".equals(relation.getEnterpriseDrugForm())) {
            return true;
        }
        List<String> drugFrom = JSONUtils.parse((relation.getEnterpriseDrugForm()), List.class);
        for (OrganDrugList drugList : drugLists) {
            if (drugFrom.contains(drugList.getDrugForm())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取医保标识
     * @param recipeIds
     * @return
     */
    private Set<Integer> getMedicalFlag(Set<Integer> recipeIds){
        if (CollectionUtils.isEmpty(recipeIds)) {
            return null;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipes)) {
            return null;
        }
        Set<Integer> medicalFlag = recipes.stream().map(Recipe::getMedicalFlag).filter(Objects::nonNull).collect(Collectors.toSet());

        return medicalFlag;
    }
}
