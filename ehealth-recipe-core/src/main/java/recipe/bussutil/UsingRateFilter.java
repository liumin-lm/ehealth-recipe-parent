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

    /**
     * 广东省监管平台转换
     * @param usingRate
     * @return
     */
    public static String transReguation(String usingRate){
        String str;
        switch (usingRate) {
            case "bid":
                str="01";
                break;
            case "biw":
                str="02";
                break;
            case "hs":
                str="03";
                break;
            case "q12h":
                str="04";
                break;
            case "q1h":
                str="05";
                break;
            case "q3h":
                str="06";
                break;
            case "q6h":
                str="07";
                break;
            case "q8h":
                str="08";
                break;
            case "qd":
                str="09";
                break;
            case "qid":
                str="10";
                break;
            case "qod":
                str="11";
                break;
            case "qw":
                str="12";
                break;
            case "st":
                str="13";
                break;
            case "tid":
                str="14";
                break;
            default:
                str = "99";
        }
        return str;
    }
    //江苏监管平台
    public static String transDosageFormReguation(String dosageForm){
        String str;
        switch (dosageForm) {
            case "原料":
                str="0";
                break;
            case "片剂":
            case "素片":
            case "压制片":
            case "浸膏片":
            case "非包衣片":
                str="1";
                break;
            case "阴道片":
            case "外用阴道膜":
            case "阴道用药":
            case "阴道栓片":
                str="10";
                break;
            case "水溶片":
            case "眼药水片":
                str="11";
                break;
            case "分散片":
            case "适应片":
                str="12";
                break;
            case "纸片":
            case "纸型片":
            case "膜片":
            case "薄膜片":
                str="13";
                break;
            case "丸剂":
            case "药丸":
            case "眼丸":
            case "耳丸":
            case "糖丸":
            case "糖衣丸":
            case "浓缩丸":
            case "调释丸":
            case "水丸":
                str="14";
                break;
            case "粉针剂":
            case "冻干粉针剂":
            case "冻干粉":
                str="15";
                break;
            case "注射液":
            case "水针剂":
            case "油针剂":
            case "混悬针剂":
                str="16";
                break;
            case "注射溶媒":
                str="17";
                break;
            case "输液剂":
            case "血浆代用品":
                str="18";
                break;
            case "胶囊剂":
            case "硬胶囊":
                str="19";
                break;
            case "糖衣片":
            case "包衣片":
            case "薄膜衣片":
                str="2";
                break;
            case "软胶囊":
            case "滴丸":
            case "胶丸":
                str="20";
                break;
            case "肠溶胶囊":
            case "肠溶胶丸":
                str="21";
                break;
            case "调释胶囊":
            case "控释胶囊":
            case "缓释胶囊":
                str="22";
                break;
            case "溶液剂":
            case "含漱液":
            case "内服混悬液":
                str="23";
                break;
            case "合剂":
                str="24";
                break;
            case "乳剂":
            case "乳胶":
                str="25";
                break;
            case "凝胶剂":
            case "胶剂":
            case "胶体":
            case "胶冻":
            case "胶体微粒":
                str="26";
                break;
            case "胶浆剂":
                str="27";
                break;
            case "芳香水剂":
            case "露剂":
                str="28";
                break;
            case "滴剂":
                str="29";
                break;
            case "咀嚼片":
            case "糖片":
            case "异型片":
            case "糖胶片":
                str="3";
                break;
            case "糖浆剂":
            case "蜜浆剂":
                str="30";
                break;
            case "口服液":
                str="31";
                break;
            case "浸膏剂":
                str="32";
                break;
            case "流浸膏剂":
                str="33";
                break;
            case "酊剂":
                str="34";
                break;
            case "醑剂":
                str="35";
                break;
            case "酏剂":
                str="36";
                break;
            case "洗剂":
            case "阴道冲洗剂":
                str="37";
                break;
            case "搽剂":
            case "涂剂":
            case "擦剂":
            case "外用混悬液剂":
                str="38";
                break;
            case "油剂":
            case "甘油剂":
                str="39";
                break;
            case "肠溶片":
            case "肠衣片":
                str="4";
                break;
            case "棉胶剂":
            case "火棉胶剂":
                str="40";
                break;
            case "涂膜剂":
                str="41";
                break;
            case "涂布剂":
                str="42";
                break;
            case "滴眼剂":
            case "洗眼剂":
            case "粉剂眼花缭乱药":
                str="43";
                break;
            case "滴鼻剂":
            case "洗鼻剂":
                str="44";
                break;
            case "滴耳剂":
            case "洗耳剂":
                str="45";
                break;
            case "口腔药剂":
            case "口腔用药":
            case "牙科用药":
                str="46";
                break;
            case "灌肠剂":
                str="47";
                break;
            case "软膏剂":
            case "油膏剂":
            case "水膏剂":
                str="48";
                break;
            case "霜剂":
            case "乳膏剂":
                str="49";
                break;
            case "调释片":
            case "缓释片":
            case "控释片":
            case "长效片":
                str="5";
                break;
            case "糊剂":
                str="50";
                break;
            case "硬膏剂":
            case "橡皮膏":
                str="51";
                break;
            case "眼膏剂":
                str="52";
                break;
            case "散剂":
            case "内服散剂":
            case "外用散剂":
            case "粉剂":
            case "撒布粉":
                str="53";
                break;
            case "颗粒剂":
            case "冲剂":
            case "晶剂":
            case "结晶":
            case "晶体":
            case "干糖浆":
                str="54";
                break;
            case "泡腾颗粒剂":
                str="55";
                break;
            case "调释颗粒剂":
            case "缓释颗粒剂":
                str="56";
                break;
            case "气雾剂":
            case "水雾剂":
                str="57";
                break;
            case "喷雾剂":
                str="58";
                break;
            case "混悬雾剂":
                str="59";
                break;
            case "泡腾片":
                str="6";
                break;
            case "吸入药剂":
                str="60";
                break;
            case "膜剂":
                str="61";
                break;
            case "海绵剂":
                str="62";
                break;
            case "栓剂":
            case "痔疮栓":
            case "耳栓":
                str="63";
                break;
            case "植入栓":
                str="64";
                break;
            case "透皮剂":
            case "贴剂":
            case "贴片":
                str="65";
                break;
            case "控释透皮剂":
            case "控释贴片":
            case "控释口颊片":
                str="66";
                break;
            case "划痕剂":
                str="67";
                break;
            case "珠链":
            case "泥珠链":
                str="68";
                break;
            case "糖锭":
            case "锭剂":
                str="69";
                break;
            case "舌下片":
                str="7";
                break;
            case "微囊胶囊":
            case "微丸胶囊":
                str="70";
                break;
            case "干混悬剂":
            case "干悬乳剂":
            case "口服乳干粉":
                str="71";
                break;
            case "吸放剂":
                str="72";
                break;
            case "含片":
            case "嗽口片":
            case "喉症片":
            case "口腔粘附片":
                str="8";
                break;
            case "外用片":
            case "外用膜":
            case "坐药片":
            case "环型片":
                str="9";
                break;
            case "试剂盒":
            case "药盒":
                str="90";
                break;
            default:
                str = "99";
        }
        return str;
    }
}
