package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.util.ByteUtils;
import recipe.util.RecipeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 获取配置项 交互处理类
 *
 * @author fuzi
 */
@Service
public class IConfigurationClient extends BaseClient {
    @Autowired
    private IConfigurationCenterUtilsService configService;

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
     * 判断是否需要对接HIS----根据运营平台配置处方类型是否跳过his
     *
     * @param recipe
     * @return
     */
    public boolean skipHis(Recipe recipe) {
        try {
            String[] recipeTypes = (String[]) configService.getConfiguration(recipe.getClinicOrgan(), "getRecipeTypeToHis");
            List<String> recipeTypelist = Arrays.asList(recipeTypes);
            if (recipeTypelist.contains(Integer.toString(recipe.getRecipeType()))) {
                return false;
            }
        } catch (Exception e) {
            logger.error("skipHis error " + e.getMessage(), e);
            //按原来流程走-西药中成药默认对接his
            if (!RecipeUtil.isTcmType(recipe.getRecipeType())) {
                return false;
            }
        }
        return true;
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



}
