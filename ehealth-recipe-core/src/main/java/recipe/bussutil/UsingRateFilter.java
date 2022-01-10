package recipe.bussutil;

import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.constant.CacheConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.util.RedisClient;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/6
 * @description： 匹配HOS处方用药频率
 * @version： 1.0
 */
public class UsingRateFilter {

    public static String filter(int organId, String field) {
        if (StringUtils.isEmpty(field)){
            return "";
        }
        String val = RedisClient.instance().hget(CacheConstant.KEY_ORGAN_USINGRATE + organId, field);
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
        String val = RedisClient.instance().hget(CacheConstant.KEY_NGARI_USINGRATE + organId, field);
        /**
         * 查不到的原因
         * 1 因为field有可能在平台没有新增，则返回实际值
         * 2 没有进行字典对照，则返回实际值
         */
        return StringUtils.isEmpty(val) ? field : val;
    }

    /**
     * 根据平台的字典编码，匹配第三方的值，一般用于平台处方写入其他平台使用---杭州市互联网
     * @param organId
     * @param field
     * @return
     */
    public static String filterNgariByMedical(int organId, String field){
        if (StringUtils.isEmpty(field)){
            return "";
        }
        String val = RedisClient.instance().hget(CacheConstant.KEY_MEDICAL_NGARI_USINGRATE + organId, field);
        if (StringUtils.isEmpty(val)){
            OrganAndDrugsepRelationDAO dao = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> enterprises = dao.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
            if (CollectionUtils.isNotEmpty(enterprises)){
                if ("hzInternet".equals(enterprises.get(0).getCallSys())){
                    val = RedisClient.instance().hget(CacheConstant.KEY_MEDICAL_NGARI_USINGRATE + "hzInternet", field);
                    return StringUtils.isEmpty(val) ? field : val;
                }
            }
        }
        /**
         * 查不到的原因
         * 1 因为field有可能在平台没有新增，则返回实际值
         * 2 没有进行字典对照，则返回实际值
         */
        return StringUtils.isEmpty(val) ? field : val;
    }

    /**
     * 对接武昌his--根据用药频次转每日次数
     * @return
     */
    public static Integer transDailyTimes(String usePathway) {
        int dailyTimes;
        switch (usePathway) {
            case "bid":
            case "q12h":
            case "2id":
                dailyTimes = 2;
                break;
            case "q8h":
            case "tid":
            case "3id":
            case "939":
                dailyTimes = 3;
                break;
            case "q6h":
            case "qid":
            case "q5h":
            case "4id":
                dailyTimes = 4;
                break;
            case "q4h":
            case "q8h2":
                dailyTimes = 6;
                break;
            case "q3h":
                dailyTimes = 8;
                break;
            case "q2h":
                dailyTimes = 12;
                break;
            case "qh":
            case "q1h":
                dailyTimes = 24;
                break;
            case "q30m":
            case "q1/2h":
                dailyTimes = 48;
                break;
            default:
                dailyTimes = 1;
        }
        return dailyTimes;
    }

}
