package recipe.bussutil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.constant.CacheConstant;
import recipe.util.RedisClient;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/6
 * @description： 匹配HOS处方用药频率
 * @version： 1.0
 */
public class UsingRateFilter {

    public static String filter(int organId, String filed) {
        String val = RedisClient.instance().hget(CacheConstant.KEY_ORGAN_USINGRATE + organId, filed);
        //默认 必要时
        return StringUtils.isEmpty(val) ? "prn" : val;
    }
}
