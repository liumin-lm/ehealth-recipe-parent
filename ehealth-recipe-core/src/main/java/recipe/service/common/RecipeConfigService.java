package recipe.service.common;

import com.ngari.base.BaseAPI;
import com.ngari.base.clientconfig.service.IClientConfigService;
import com.ngari.base.clientconfig.to.ClientConfigBean;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.constant.CacheConstant;
import recipe.constant.RecipeBussConstant;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

/**
 * @author： 0184/yu_yun
 * @date： 2019/5/16
 * @description： 处方配置信息获取
 * @version： 1.0
 */
@RpcBean("recipeConfigService")
public class RecipeConfigService {

    /** logger */
    private static final Logger LOG = LoggerFactory.getLogger(RecipeConfigService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private IConfigurationCenterUtilsService configService;

    private String ZL_YS = "1";

    private String NL_YS = "2";


    /**
     *  根据APPKEY获取处方流转模式
     *  老版本调用
     * @param appKey
     * @return
     */
    @RpcService
    @LogRecord
    public String getRecipeMode(String appKey){
        //配置key:recipeCirculationMode
        String val = RecipeBussConstant.RECIPEMODE_NGARIHEALTH;
        if(StringUtils.isEmpty(appKey)){
            LOG.info("getRecipeMode appKey is null, recipeMode={}", val);
            return val;
        }
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        val = cacheService.getTemporaryParam(CacheConstant.KEY_RECIPEMODE+appKey);
        if(StringUtils.isEmpty(val)){
            val = RecipeBussConstant.RECIPEMODE_NGARIHEALTH;
            try {
                //date 2020 0326
                //根据sdk辨别解析方式
                if(appKey.contains("APP_SDK")){
                    //直接替换
                    appKey = appKey.split("@")[2];
                }
                IClientConfigService ccService = BaseAPI.getService(IClientConfigService.class);
                ClientConfigBean clientConfigDTO = ccService.getByAppKey(appKey);
                LOG.info("getRecipeMode 响应的为端配置：{}", JSONUtils.toString(clientConfigDTO));
                if(null == clientConfigDTO){
                    LOG.warn("getRecipeMode clientConfigDTO is null. appKey={}", appKey);
                    return val;
                }
                Object obj;
                if("PC".equals(clientConfigDTO.getType())){
                    //5---pc端配置  2----app端配置
                    obj = configService.getPropertyOfKey(clientConfigDTO.getId(), "recipeCirculationModeForPC", 5);
                }else {
                    obj = configService.getPropertyOfKey(clientConfigDTO.getId(), "recipeCirculationMode", 2);
                }
                if (null != obj) {
                    val = LocalStringUtil.toString(obj);
                    redisClient.setEX(CacheConstant.KEY_RECIPEMODE + appKey, 24 * 3600L, val);
                }
            } catch (Exception e) {
                LOG.error("getRecipeMode exception! appKey={}", appKey, e);
            }
        }

        LOG.info("getRecipeMode appKey={}, recipeMode={}", appKey, val);
        return val;
    }

    /**
     *  根据APPKEY、organId获取处方流转模式
     * @param appKey
     * @return
     */
    @RpcService
    @LogRecord
    public String getRecipeModeByAppKeyAndOrganId(String appKey,Integer organId) {
        String val = "";
        if (organId == null) {
            return getRecipeMode(appKey);
        }
        String recipeModeRadioConfig = String.valueOf(configService.getConfiguration(organId, "recipeModeRadioConfig"));
        LOG.info("getRecipeModeByAppKeyAndOrganId recipeModeRadioConfig:{}",JSONUtils.toString(recipeModeRadioConfig));
        //"options":{ "0":"无", "1":"浙里平台模式", "2":"纳里平台模式" }
        if (ZL_YS.equals(recipeModeRadioConfig)) {
            val = RecipeBussConstant.RECIPEMODE_ZJJGPT;
            return val;
        } else if (NL_YS.equals(recipeModeRadioConfig)) {
            val = RecipeBussConstant.RECIPEMODE_NGARIHEALTH;
            return val;
        } else {
            return getRecipeMode(appKey);

        }
    }
}
