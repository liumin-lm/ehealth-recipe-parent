package recipe.client;

import com.ngari.base.sysparamter.service.ISysParamterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.RedisClient;

/**
 * 处方缓存获取
 *
 * @author yins
 */
@Service
public class RecipeRedisClient extends BaseClient {

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ISysParamterService iSysParamterService;

    private final String RECIPE_CACHE_KEY = "RECIPE_CACHE_KEY";

    /**
     * 获取 base_parameter 表的数据，缓存保持一周
     *
     * @param field
     * @param defaultStr
     * @return
     */
    public String getParam(String field, String defaultStr) {
        logger.info("RecipeRedisClient field={}, defaultStr={}", field, defaultStr);
        if (StringUtils.isEmpty(field)) {
            return "";
        }
        //先从缓存获取
        String val = redisClient.hget(RECIPE_CACHE_KEY, field);
        if (StringUtils.isEmpty(val)) {
            val = iSysParamterService.getParam(field, null);
            if (StringUtils.isNotEmpty(val)) {
                redisClient.hsetEx(RECIPE_CACHE_KEY, field, val, 7 * 24 * 3600L);
            } else {
                //返回默认值
                val = defaultStr;
            }
        }
        logger.info("recipeCacheService value={}", val);
        return val;
    }
}
