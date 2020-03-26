package recipe.service.common;

import com.ngari.base.sysparamter.service.ISysParamterService;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.dao.RecipeParameterDao;
import recipe.util.RedisClient;

import java.util.Set;

/**
 * @author： 0184/yu_yun
 * @date： 2019/1/7
 * @description： 缓存查询服务
 * @version： 1.0
 */
@RpcBean(value = "recipeCacheService", mvc_authentication = false)
public class RecipeCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCacheService.class);

    @Autowired
    private RedisClient redisClient;

    private final String RECIPE_CACHE_KEY = "RECIPE_CACHE_KEY";

    @RpcService
    public String getParam(String field) {
        return getParam(field, null);
    }

    /**
     * 获取 base_parameter 表的数据，缓存保持一周
     *
     * @param field
     * @param defaultStr
     * @return
     */
    @RpcService
    public String getParam(String field, String defaultStr) {
        LOGGER.info("recipeCacheService field={}, defaultStr={}", field, defaultStr);
        if (StringUtils.isEmpty(field)) {
            return "";
        }

        //先从缓存获取
        String val = redisClient.hget(RECIPE_CACHE_KEY, field);
        if (StringUtils.isEmpty(val)) {
            //从RPC接口获取
            ISysParamterService iSysParamterService = ApplicationUtils.getBaseService(ISysParamterService.class);
            val = iSysParamterService.getParam(field, null);
            if (StringUtils.isNotEmpty(val)) {
                redisClient.hsetEx(RECIPE_CACHE_KEY, field, val, 7 * 24 * 3600L);
            } else {
                //返回默认值
                val = defaultStr;
            }
        }
        LOGGER.info("recipeCacheService value={}", val);
        return val;
    }

    /**
     * 获取 recipe_parameter 表的数据，缓存保持一周
     * @param field
     * @param defaultStr
     * @return
     */
    @RpcService
    public String getRecipeParam(String field, String defaultStr) {
        LOGGER.info("recipeCacheService getRecipeParam field={}, defaultStr={}", field, defaultStr);
        if (StringUtils.isEmpty(field)) {
            return "";
        }

        //先从缓存获取
        String val = redisClient.hget(RECIPE_CACHE_KEY, field);
        if (StringUtils.isEmpty(val)) {
            RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            val = parameterDao.getByName(field);
            if (StringUtils.isNotEmpty(val)) {
                redisClient.hsetEx(RECIPE_CACHE_KEY, field, val, 7 * 24 * 3600L);
            } else {
                //返回默认值
                val = defaultStr;
            }
        }
        LOGGER.info("recipeCacheService getRecipeParam value={}", val);
        return val;
    }

    /**
     * 获取临时缓存数据，默认一天失效
     * @param key
     * @return
     */
    public String getTemporaryParam(String key){
        LOGGER.info("recipeCacheService getTemporaryParam key={}", key);
        return redisClient.get(key);
    }

    /**
     * 清除缓存
     *
     * @return
     */
    @RpcService
    public Long deleteCacheKey() {
        return redisClient.del(RECIPE_CACHE_KEY);
    }

    @RpcService
    public Set<String> findDrugByDiseaseName(String field) {
        LOGGER.info("recipeCacheService getDiseaseAndDrugParam field={}", field);
        //先从缓存获取
        Set<String> val = redisClient.sMembers(field);
        LOGGER.info("recipeCacheService getDiseaseAndDrugParam value={}", val);
        return val;
    }

}
