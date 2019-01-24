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




    /**************************局部缓存**************************/

    /**
     * 机构用药频次前缀，缓存中key实际为 RCP_ORGAN_USINGRATE_100100
     */
    public static final String KEY_ORGAN_USINGRATE = "RCP_ORGAN_USINGRATE_";

    /**
     * 机构用药方式前缀，缓存中key实际为 RCP_ORGAN_USEPATHWAYS_100100
     */
    public static final String KEY_ORGAN_USEPATHWAYS = "RCP_ORGAN_USEPATHWAYS_";


}
