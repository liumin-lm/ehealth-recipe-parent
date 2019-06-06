package recipe.drugsenterprise;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.constant.CacheConstant;
import recipe.util.RedisClient;

/**
 * @author yinsheng
 * @date 2019\3\4 0004 21:05
 */
@RpcBean("aldyfRedisService")
public class AldyfRedisService {


    private RedisClient redisClient = RedisClient.instance();

    /**
     * 保存阿里session
     * @param loginId   loginId
     * @param value   session值
     */
    @RpcService
    public void saveTaobaoAccessToken (String loginId, String value, Long expireSecond) {
        redisClient.setEX(CacheConstant.KEY_DEPT_ALI_SESSION + loginId, expireSecond, value);
    }

    /**
     * 获取阿里session
     * @param loginId  loginId
     * @return  session值
     */
    @RpcService
    public String getTaobaoAccessToken (String loginId) {
        return redisClient.get(CacheConstant.KEY_DEPT_ALI_SESSION + loginId);
    }
}
