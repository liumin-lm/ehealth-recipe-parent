package recipe.service.common;

import com.ngari.base.BaseAPI;
import com.ngari.base.clientconfig.service.IClientConfigService;
import com.ngari.base.clientconfig.to.ClientConfigBean;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.mvc.weixin.WXApp;
import ctd.mvc.weixin.WXAppManager;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
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

    /**
     *  根据APPKEY获取处方流转模式
     * @param appKey
     * @return
     */
    @RpcService
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
                //date 20200325
                //根据前端传入的appkey和type做查询
                String[] appKeyMsgs = appKey.split("@");
                IClientConfigService ccService = BaseAPI.getService(IClientConfigService.class);
                ClientConfigBean clientConfigDTO = ccService.getByAppKeyAndType(appKeyMsgs[2], appKeyMsgs[0]);
                //date 20200317
                //更新去配置的方式为通过entrance
                //ClientConfigBean clientConfigDTO = ccService.getClientConfigByEntrance(appKey);
                LOG.info("getRecipeMode 响应的为端配置：{}", JSONUtils.toString(clientConfigDTO));
                if(null == clientConfigDTO){
                    LOG.warn("getRecipeMode clientConfigDTO is null. appKey={}", appKey);
                    return val;
                }
                IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
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
                LOG.warn("getRecipeMode exception! appKey={}", appKey, e);
            }
        }

        LOG.info("getRecipeMode appKey={}, recipeMode={}", appKey, val);
        return val;
    }

}
