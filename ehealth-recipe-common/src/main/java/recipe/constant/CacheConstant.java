package recipe.constant;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/6
 * @description： 缓存配置
 * @version： 1.0
 */
public class CacheConstant {

    /**
     * 测试药店测试数据
     */
    public static final String KEY_PHARYACY_TEST_DATA = "RCP_PHARYACY_TEST_DATA";

    /**
     * 跳过人工审核organId列表
     */
    public static final String KEY_SKIP_YSCHECK_LIST = "RCP_SKIP_YSCHECK_LIST";

    /**
     * 武昌organId列表
     */
    public static final String KEY_WUCHANG_ORGAN_LIST = "RCP_WUCHANG_ORGAN_LIST";
    
    /**
     * 支持HIS处方检查功能的机构列表
     */
    public static final String KEY_HIS_CHECK_LIST = "RCP_HIS_CHECK_LIST";


    
    /**************************机构个性化配置**************************/

    /**
     * 阿里-淘宝授权获取到的用户session，后面跟 loginId
     */
    public static final String KEY_DEPT_ALI_SESSION = "RCP_ALISN_";

    /**
     * 机构用药频次前缀，缓存中key实际为 RCP_ORGAN_USINGRATE_100100
     */
    public static final String KEY_ORGAN_USINGRATE = "RCP_ORGAN_USINGRATE_";

    /**
     * 机构用药方式前缀，缓存中key实际为 RCP_ORGAN_USEPATHWAYS_100100
     */
    public static final String KEY_ORGAN_USEPATHWAYS = "RCP_ORGAN_USEPATHWAYS_";

    /**
     * 平台用药频次前缀，缓存中key实际为 RCP_NGARI_USINGRATE_100100
     */
    public static final String KEY_NGARI_USINGRATE = "RCP_NGARI_USINGRATE_";

    /**
     * 平台用药方式前缀，缓存中key实际为 RCP_NGARI_USEPATHWAYS_100100
     */
    public static final String KEY_NGARI_USEPATHWAYS = "RCP_NGARI_USEPATHWAYS_";
    

    
    /********************************以下为开关项内容********************************/
    //开关类值为 true or false

    /**
     * 购药功能关闭, ture:表示能购药
     */
    public static final String KEY_SWITCH_PURCHASE_ON = "RCP_SWITCH_PURCHASE_ON";


    
    /********************************以下为配置项内容********************************/

    /**
     * 缓存配置项一般采用HASH存储，key为以下RCP_CONFIG_XXX,filed为organId，value为具体值
     *
     */

    public static final String KEY_CONFIG_RCP_AUTO_REVIEW = "RCP_CONFIG_AUTO_REVIEW";

}
