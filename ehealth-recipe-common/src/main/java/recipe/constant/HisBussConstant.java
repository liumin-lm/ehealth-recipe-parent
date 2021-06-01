package recipe.constant;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/30
 * @description： HIS对接相关常量
 * @version： 1.0
 */
public class HisBussConstant {

    /**
     * 发送医院HIS处方状态-新增
     */
    public static final String TOHIS_RECIPE_STATUS_ADD  = "1";

    /**
     * 发送医院HIS处方状态-撤销
     */
    public static final String TOHIS_RECIPE_STATUS_REVOKE  = "2";

    /**
     * 发送医院HIS处方状态-退处方
     */
    public static final String TOHIS_RECIPE_STATUS_REFUND  = "3";

    /**
     * 发送医院HIS处方状态-处方核销完成
     */
    public static final String TOHIS_RECIPE_STATUS_FINISH  = "4";


    /**
     * 接收医院HIS处方状态-新增成功
     */
    public static final String FROMHIS_RECIPE_STATUS_ADD  = "1";

    /**
     * 接收医院HIS处方状态-已付款
     */
    public static final String FROMHIS_RECIPE_STATUS_PAY  = "2";

    /**
     * 接收医院HIS处方状态-已发药-医院取药
     */
    public static final String FROMHIS_RECIPE_STATUS_FINISH  = "3";

    /**
     * 接收医院HIS处方状态-已退费
     */
    public static final String FROMHIS_RECIPE_STATUS_REFUND  = "4";

    /**
     * 接收医院HIS处方状态-已退药
     */
    public static final String FROMHIS_RECIPE_STATUS_REFUND_EX  = "5";

    /**
     * 接收医院HIS处方状态-处方拒绝接收
     */
    public static final String FROMHIS_RECIPE_STATUS_REJECT = "6";

    /**
     * 接收医院HIS处方状态-已申请配送
     */
    public static final String FROMHIS_RECIPE_STATUS_SENDING = "7";

    /**
     * 接收医院HIS处方状态-已配送
     */
    public static final String FROMHIS_RECIPE_STATUS_SENDED= "8";

}
