package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.constant.ErrorCode;
import recipe.util.ByteUtils;

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
            return (String) configService.getConfiguration(organId, key);
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatch organId:{}, recipeId:{}", organId, key, e);
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
            return jsonArray.getString(0);
        } catch (Exception e) {
            logger.error("IConfigurationClient getValueCatch organId:{}, recipeId:{}", organId, key, e);
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
