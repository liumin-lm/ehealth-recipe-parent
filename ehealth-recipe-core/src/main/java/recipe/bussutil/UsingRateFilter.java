package recipe.bussutil;

import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.CacheConstant;
import recipe.util.RedisClient;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/6
 * @description： 匹配HOS处方用药频率
 * @version： 1.0
 */
public class UsingRateFilter {

    @Autowired
    private static RedisClient redisClient;

    public static String filter(int organId, String filed) {
        return redisClient.hget(CacheConstant.KEY_ORGAN_USINGRATE + organId, filed);
    }
}
