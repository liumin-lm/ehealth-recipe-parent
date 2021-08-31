package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.patient.service.OrganConfigService;
import ctd.account.Client;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.util.ByteUtils;
import recipe.util.RecipeUtil;

import javax.annotation.Resource;
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


    /**
     * 获取终端id
     *
     * @return
     */
    public Integer getPropertyByClientId() {
        try {
            ICurrentUserInfoService userInfoService = AppContextHolder.getBean(
                    "eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
            Client client = userInfoService.getCurrentClient();
            logger.info("IConfigurationClient getPropertyByClientId  client:{}", JSONArray.toJSONString(client));
            return client.getClientConfigId();
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
    public Integer getValueCatch(Integer organId, String key, Integer defaultValue) {
        if (null == organId || StringUtils.isEmpty(key)) {
            return defaultValue;
        }
        try {
            String value = (String) configService.getConfiguration(organId, key);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatch organId:{}, recipeId:{}", organId, key, e);
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
            logger.error("IConfigurationClient getValueCatch organId:{}, recipeId:{}", organId, key, e);
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
            logger.error("IConfigurationClient getValueBooleanCatch organId:{}, recipeId:{}", organId, key, e);
            return defaultValue;
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
            logger.error("IConfigurationClient getValueEnumCatch organId:{}, recipeId:{}", organId, key, e);
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
            logger.error("IConfigurationClient getValueEnumCatch organId:{}, recipeId:{}", organId, key, e);
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
     * 获取限制开药天数
     *
     * @param organId
     * @return
     */
    private String[] useDaysRange(Integer organId) {
        Object isLimitUseDays = configService.getConfiguration(organId, "isLimitUseDays");
        if (null == isLimitUseDays || !(boolean) isLimitUseDays) {
            return new String[]{"1", "99"};
        }
        Object useDaysRange = configService.getConfiguration(organId, "useDaysRange");
        if (null == useDaysRange) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "useDaysRange is null");
        }
        return useDaysRange.toString().split(ByteUtils.COMMA);

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
}
