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

    /**
     * 广东省监管平台转换
     * @param usePathway
     * @return
     */
    public static String transReguation(String usePathway){
        String str;
        switch (usePathway) {
            case "po":
                str="1";
                break;
            case "r":
            case "pr.rect":
                str="2";
                break;
            case "dut":
                str="3";
                break;
            case "cv":
            case "fi":
            case "pi":
            case "ip":
            case "ia":
                str="4";
                break;
            case "ih":
                str="401";
                break;
            case "id":
                str="402";
                break;
            case "im":
                str="403";
                break;
            case "ivdrip":
            case "ivi":
            case "ivp":
                str="404";
                break;
            case "in":
                str="5";
                break;
            case "jg":
                str="605";
                break;
            case "pr.vagin":
            case "v":
                str="606";
                break;
            case "pro.0":
                str="607";
                break;
            case "pro.nar":
                str="608";
                break;
            case "mo":
            case "sl":
                str="608";
                break;
            default:
                str = "9";
        }
        return str;
    }

}
