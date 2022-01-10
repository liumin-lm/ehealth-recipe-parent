package recipe.bussutil;

import org.apache.commons.lang3.StringUtils;
import recipe.constant.CacheConstant;
import recipe.util.RedisClient;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/6
 * @description： 匹配HOS处方用药方式
 * @version： 1.0
 */
public class UsePathwaysFilter {

    public static String filter(int organId, String field) {
        if (StringUtils.isEmpty(field)){
            return "";
        }
        String val =  RedisClient.instance().hget(CacheConstant.KEY_ORGAN_USEPATHWAYS + organId, field);
        /**
         * 根据医院的编码，匹配平台的值，一般用于医院处方写入平台使用
         * 查不到的原因
         * 1 因为field有可能在平台没有新增，则返回实际值
         * 2 没有进行字典对照，则返回实际值
         */
        return StringUtils.isEmpty(val) ? field : val;
    }

    /**
     * 根据平台的字典编码，匹配医院的值，一般用于平台处方写入HIS使用
     * @param organId
     * @param field
     * @return
     */
    public static String filterNgari(int organId, String field){
        if (StringUtils.isEmpty(field)){
            return "";
        }
        String val = RedisClient.instance().hget(CacheConstant.KEY_NGARI_USEPATHWAYS + organId, field);
        /**
         * 查不到的原因
         * 1 因为field有可能在平台没有新增，则返回实际值
         * 2 没有进行字典对照，则返回实际值
         */
        return StringUtils.isEmpty(val) ? field : val;
    }

}
