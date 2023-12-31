package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ngari.base.clientconfig.service.IClientConfigService;
import com.ngari.base.clientconfig.to.ClientConfigBean;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.patient.service.ClientConfigService;
import com.ngari.patient.service.OrganConfigService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.account.Client;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.entity.base.ClientConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.util.ByteUtils;
import recipe.util.RecipeUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * 获取配置项 交互处理类
 *
 * @author fuzi
 */
@Service
public class IConfigurationClient extends BaseClient {
    @Resource
    private IConfigurationCenterUtilsService configService;
    @Resource
    private OrganConfigService organConfigService;
    @Resource
    private IHisConfigService hisConfigService;
    @Autowired
    private IClientConfigService clientConfigService;

    /**
     * 类加载排序
     *
     * @return
     */
    @Override
    public Integer getSort() {
        return 8;
    }

    /**
     * 获取终端id
     *
     * @return
     */
    public Boolean getPropertyByClientId(String medicalPayConfigKey) {
        try {
            Client client = currentUserInfoService.getCurrentClient();
            logger.info("IConfigurationClient getPropertyByClientId  client:{}", JSONArray.toJSONString(client));
            Boolean valueBooleanCatch = (Boolean) configService.getPropertyByClientId(client.getClientConfigId(), medicalPayConfigKey);
            logger.info("IConfigurationClient getPropertyByClientId  valueBooleanCatch:{}", valueBooleanCatch);
            return valueBooleanCatch;
        } catch (Exception e) {
            logger.error("IConfigurationClient getPropertyByClientId", e);
            return false;
        }
    }

    /**
     * 获取终端配置
     * @param medicalPayConfigKey
     * @return
     */
    public String getPropertyByString(String medicalPayConfigKey) {
        try {
            Client client = currentUserInfoService.getCurrentClient();
            logger.info("IConfigurationClient getPropertyByClientId  client:{}", JSONArray.toJSONString(client));
            String valueBooleanCatch = (String) configService.getPropertyByClientId(client.getClientConfigId(), medicalPayConfigKey);
            logger.info("IConfigurationClient getPropertyByClientId  valueBooleanCatch:{}", valueBooleanCatch);
            return valueBooleanCatch;
        } catch (Exception e) {
            logger.error("IConfigurationClient getPropertyByClientId", e);
            return "";
        }
    }

    /**
     * 获取终端配置
     *
     * @param medicalPayConfigKey      机构id
     * @return list
     */
    public List<String> getPropertyByStringList(String medicalPayConfigKey) {
        try {
            Client client = currentUserInfoService.getCurrentClient();
            logger.info("IConfigurationClient getPropertyByClientId  client:{}", JSONArray.toJSONString(client));
            String[] invalidInfoObject = (String[]) configService.getPropertyByClientId(client.getClientConfigId(), medicalPayConfigKey);
            logger.info("IConfigurationClient getPropertyByClientId  invalidInfoObject:{}", invalidInfoObject);
            if (null == invalidInfoObject || 0 == invalidInfoObject.length) {
                return null;
            }
            return Arrays.asList(invalidInfoObject);
        } catch (Exception e) {
            logger.error("IConfigurationClient getPropertyByClientId", e);
            return null;
        }
    }


    /**
     * 获取多个机构配置
     *
     * @param organId 机构id
     * @param keys    配置key
     * @return
     */
    public Map<String, Object> getConfigurationByKeyList(Integer organId, List<String> keys) {
        if (Objects.isNull(organId) || CollectionUtils.isEmpty(keys)) {
            return null;
        }
        try {
            Map<String, Object> configurations = configService.findConfigurations(organId, keys);
            return configurations;
        } catch (Exception e) {
            logger.error("IConfigurationClient getConfigurationByKeyList organId:{}, keys:{}", organId, JSONArray.toJSONString(keys), e);
            return null;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public String getValueCatch(Integer organId, String key, String defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            String value = (String) configService.getConfiguration(organId, key);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatch organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public BigDecimal getValueCatchReturnBigDecimal(Integer organId, String key, BigDecimal defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            BigDecimal value = (BigDecimal) configService.getConfiguration(organId, key);
            if (Objects.isNull(value)) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatchReturnBigDecimal organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public Double getValueCatchReturnDouble(Integer organId, String key, Double defaultValue){
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            Double value = (Double) configService.getConfiguration(organId, key);
            if (Objects.isNull(value)) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatchReturnDouble organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public Integer getValueCatchReturnInteger(Integer organId, String key, Integer defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            Integer value = (Integer) configService.getConfiguration(organId, key);
            if (Objects.isNull(value)) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatchReturnInteger organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public Integer getValueCatch(Integer organId, String key, Integer defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            String value = (String) configService.getConfiguration(organId, key);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatch Integer organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public String getValueCatchReturnArr(Integer organId, String key, String defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            String[] value = (String[]) configService.getConfiguration(organId, key);
            if (null == value) {
                return defaultValue;
            }
            return value[0];
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueBooleanCatch organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public Boolean getValueBooleanCatch(Integer organId, String key, Boolean defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            Boolean value = (Boolean) configService.getConfiguration(organId, key);
            if (null == value) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueBooleanCatch organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 获取多个机构配置项
     * @param organIds
     * @param key
     * @param defaultValue
     * @return
     */
    public Map<Integer, Boolean> getValueBooleanCatchMap(List<Integer> organIds, String key, Boolean defaultValue) {
        if (null == organIds || StringUtils.isEmpty(key)) {
            return null;
        }
        Map<Integer, Boolean> map = new HashMap<>();
        try {
            organIds.forEach(organId->{
                Boolean value = (Boolean) configService.getConfiguration(organId, key);
                if (null == value) {
                    map.put(organId, defaultValue);
                } else {
                    map.put(organId, value);
                }
            });

            return map;
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueBooleanCatch organId:{}, key:{}", JSON.toJSONString(organIds), key, e);
            return null;
        }
    }


    /**
     * 根据配置获取  枚举类型 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return
     */
    public String getValueEnumCatch(Integer organId, String key, String defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            Object invalidInfoObject = configService.getConfiguration(organId, key);
            JSONArray jsonArray = JSON.parseArray(JSONObject.toJSONString(invalidInfoObject));
            if (null == jsonArray) {
                return defaultValue;
            }
            return jsonArray.getString(0);
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueEnumCatch organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 根据配置获取  数组类型 配置项值，捕获异常时返回默认值
     *
     * @param organId      机构id
     * @param key          配置项建
     * @param defaultValue 配置项默认值报错时返回
     * @return list
     */
    public List<String> getValueListCatch(Integer organId, String key, List<String> defaultValue) {
        if (CollectionUtils.isEmpty(defaultValue)) {
            defaultValue = new LinkedList<>();
        }
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            String[] invalidInfoObject = (String[]) configService.getConfiguration(organId, key);
            if (null == invalidInfoObject || 0 == invalidInfoObject.length) {
                return defaultValue;
            }
            return Arrays.asList(invalidInfoObject);
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueEnumCatch organId:{}, key:{}", organId, key, e);
            return defaultValue;
        }
    }

    /**
     * 获取 检察药企
     *
     * @param organId 机构id
     * @return
     */
    public Integer getCheckEnterpriseByOrganId(Integer organId) {
        return organConfigService.getCheckEnterpriseByOrganId(organId);
    }


    /**
     * 医院HIS 是否 启用 F：未启用 T：启用
     *
     * @param organId
     * @return
     */
    public boolean isHisEnable(Integer organId) {
        return hisConfigService.isHisEnable(organId);
    }

    /**
     * 获取用药天数
     *
     * @param organId      机构id
     * @param recipeType   处方类型
     * @param isLongRecipe 西药-是否勾选长处方
     * @return
     */
    public String[] recipeDay(Integer organId, Integer recipeType, Boolean isLongRecipe) {
        logger.info("IConfigurationClient recipeDay organId= {},organId= {}", organId, recipeType);
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "organId is null");
        }
        String[] recipeDay;
        //中药
        if (RecipeUtil.isTcmType(recipeType)) {
            recipeDay = useDaysRange(organId);
        } else {
            //西药
            Object isCanOpenLongRecipe = configService.getConfiguration(organId, "isCanOpenLongRecipe");
            if (null != isCanOpenLongRecipe && (boolean) isCanOpenLongRecipe) {
                if (isLongRecipe) {
                    Object yesLongRecipe = configService.getConfiguration(organId, "yesLongRecipe");
                    if (null == yesLongRecipe) {
                        throw new DAOException(ErrorCode.SERVICE_ERROR, "yesLongRecipe is null");
                    }
                    recipeDay = yesLongRecipe.toString().split(ByteUtils.COMMA);
                } else {
                    Object noLongRecipe = configService.getConfiguration(organId, "noLongRecipe");
                    if (null == noLongRecipe) {
                        throw new DAOException(ErrorCode.SERVICE_ERROR, "yesLongRecipe is null");
                    }
                    recipeDay = noLongRecipe.toString().split(ByteUtils.COMMA);
                }
            } else {
                recipeDay = useDaysRange(organId);
            }
        }

        if (2 != recipeDay.length) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeDay is error");
        }
        logger.info("IConfigurationClient recipeDay recipeDay= {}", JSON.toJSONString(recipeDay));
        return recipeDay;
    }

    /**
     * 获取机构的购药方式
     *
     * @param organId 机构ID
     * @return 机构配置
     */
    public List<ScratchableBean> getOrganGiveMode(Integer organId) {
        logger.info("IConfigurationClient getOrganGiveMode organId:{}.", organId);
        IScratchableService scratchableService = AppContextHolder.getBean("eh.scratchableService", IScratchableService.class);
        List<ScratchableBean> scratchableBeans = scratchableService.findScratchableByPlatform("myRecipeDetailList", organId + "", 1);
        logger.info("IConfigurationClient getOrganGiveMode scratchableBeans:{}.", JSON.toJSONString(scratchableBeans));
        return scratchableBeans;
    }

    /**
     * 根据配置项获取机构
     *
     * @param boo
     * @return
     */
    public List<Integer> organIdList(String propertyKey, String boo) {
        return configService.findOrganByPropertyKeyAndValue(propertyKey, boo);
    }

    /**
     * 查询关联的监管平台，未关联监管平台时，配置为null
     * 默认未关联，是null，
     关联空，0，
     关联上海监管平台 17，shsjgpt
     * @param organId
     * @return
     */
    public Integer getRelationJgptId(Integer organId) {
        return (Integer)configService.getConfiguration(organId, "relatedJgpt");
    }

    /**
     * 获取线下处方查询相关配置项
     * @param organId
     * @return
     */
    public String getOfflineRecipeQueryConfig(Integer organId) {
        //费用对照项目代码配置 龙华在用
        String decoctionFeeCompareCode = getValueCatch(organId, "decoctionFeeCompareCode", "");
        String tcmFeeCompareCode = getValueCatch(organId, "tcmFeeCompareCode", "");
        String otherTotalFeeCompareCode = getValueCatch(organId, "otherTotalFeeCompareCode", "");
        Map<String, Object> jsonConfigMap = new HashMap<>();
        jsonConfigMap.put("decoctionFeeCompareCode", decoctionFeeCompareCode);
        jsonConfigMap.put("tcmFeeCompareCode", tcmFeeCompareCode);
        jsonConfigMap.put("otherTotalFeeCompareCode", otherTotalFeeCompareCode);
        return JSONUtils.toString(jsonConfigMap);
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipe(Recipe recipe) {
        Boolean isDefaultGiveModeToHos = this.getValueBooleanCatch(recipe.getClinicOrgan(), "isDefaultGiveModeToHos", false);
        if (isDefaultGiveModeToHos && null == recipe.getGiveMode()) {
            //默认到院取药
            recipe.setGiveMode(RecipeBussConstant.GIVEMODE_TO_HOS);
        }
        if (null == recipe.getReviewType()) {
            //互联网模式默认为审方前置
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                recipe.setReviewType(ReviewTypeConstant.Preposition_Check);
            } else {
                //设置运营平台设置的审方模式-互联网设置了默认值，平台没有设置默认值从运营平台取
                Integer reviewType = this.getValueCatchReturnInteger(recipe.getClinicOrgan(), "reviewType", ReviewTypeConstant.Postposition_Check);
                recipe.setReviewType(reviewType);
            }
        }
        //设置运营平台设置的审方途径
        if (null == recipe.getCheckMode()) {
            Integer checkMode = this.getValueCatchReturnInteger(recipe.getClinicOrgan(), "isOpenHisCheckRecipeFlag", 1);
            recipe.setCheckMode(checkMode);
        }
        //设置接方模式
        boolean supportMode = this.getValueBooleanCatch(recipe.getClinicOrgan(), "supportReciveRecipe", false);
        if (supportMode) {
            recipe.setSupportMode(1);
        } else {
            recipe.setSupportMode(2);
        }
    }


    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipeExt(Recipe recipe, RecipeExtend extend) {
        Integer recipeChooseChronicDisease = this.getValueCatchReturnInteger(recipe.getClinicOrgan(), "recipeChooseChronicDisease", 1);
        extend.setRecipeChooseChronicDisease(null == extend.getRecipeChooseChronicDisease() ? recipeChooseChronicDisease : extend.getRecipeChooseChronicDisease());
    }

    /**
     * 获取限制开药天数
     *
     * @param organId
     * @return
     */
    private String[] useDaysRange(Integer organId) {
        Object isLimitUseDays = configService.getConfiguration(organId, "isLimitUseDays");
        if (null == isLimitUseDays || !(boolean) isLimitUseDays) {
            return new String[]{"1", "999"};
        }
        Object useDaysRange = configService.getConfiguration(organId, "useDaysRange");
        if (null == useDaysRange) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "useDaysRange is null");
        }
        return useDaysRange.toString().split(ByteUtils.COMMA);
    }


    /**
     * 获取终端名称
     * @param appId
     * @return
     */
    public String getAppName(String appId) {
        if (StringUtils.isEmpty(appId)) {
            return null;
        }
        ClientConfigBean clientConfigByAppKey = clientConfigService.getClientConfigByAppKey(appId);
        if(Objects.isNull(clientConfigByAppKey)){
            return null;
        }
        return clientConfigByAppKey.getClientName();
    }

    /**
     * 获取APP Type
     * @param appId
     * @return
     */
    public String getAppType(String appId) {
        try {
            if (StringUtils.isEmpty(appId)) {
                return null;
            }
            ClientConfigBean clientConfigByAppKey = clientConfigService.getClientConfigByAppKey(appId);
            if(Objects.isNull(clientConfigByAppKey)){
                return null;
            }
            return clientConfigByAppKey.getType();
        } catch (Exception e) {
            logger.error("getAppType error", e);
        }
        return null;
    }


    /**
     * 根据机构配置可key 和 value 获取打开配置的机构
     * @param key
     * @param value
     * @return
     */
    public List<Integer> findOrganIdByKeyAndValue(String key, String value) {
        List<Integer> organIds = configService.findOrganByPropertyKeyAndValue(key, value);
        return organIds;
    }
}
