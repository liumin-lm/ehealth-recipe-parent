package recipe.constant;

/**
 * @author jiangtingfeng
 * date:2017/5/25.
 */
public class RecipeSystemConstant {

    public static final String SUCCESS = "success";

    public static final String FAIL = "fail";

    public static final String CUSTOMER_TEL = "400-613-8688";

    /**
     * 咨询类型： requestMode 1电话咨询，为2图文咨询 3视频咨询 4询医问药   5专家解读   91随访会话消息（健康端区分消息类型）
     */
    public static final int CONSULT_TYPE_POHONE = 1;
    public static final int CONSULT_TYPE_GRAPHIC = 2;
    public static final int CONSULT_TYPE_VIDEO = 3;
    public static final int CONSULT_TYPE_RECIPE = 4;
    public static final int CONSULT_TYPE_PROFESSOR = 5;
    public static final int FOLLOW_MSG_TYPE = 91;

    /**
     * 咨询单状态
     * <item key="0" text="待处理"/>
     * <item key="1" text="处理中"/>
     * <item key="2" text="咨询结束"/>
     * <item key="3" text="拒绝"/>
     * <item key="4" text="待支付"/>
     * <item key="9" text="已取消"/>
     */
    public static final int CONSULT_STATUS_SUBMIT = 0;
    public static final int CONSULT_STATUS_HANDLING = 1;
    public static final int CONSULT_STATUS_FINISH = 2;
    public static final int CONSULT_STATUS_REJECT = 3;
    public static final int CONSULT_STATUS_PENDING = 4;
    public static final int CONSULT_STATUS_HAVING_EVALUATION = 6;
    public static final int CONSULT_STATUS_FOR_FOLLOW = 8;
    public static final int CONSULT_STATUS_CANCEL = 9;

    /** 卫宁系统里针对每个医院的标识*/
    public static final String WEINING_HOSPCODE = "1696";

    /** 诊断类型--IDC10*/
    public static final String IDC10_DIAGNOSE_TYPE = "2";

    /** 就诊类型--普通门诊*/
    public static final String COMMON_ADM_TYPE = "100";

    /** 当前处方标识*/
    public static final String IS_CURRENT_PRESCRIPTION = "1";

    /** 新增处方标识*/
    public static final String IS_NEW_PRESCRIPTION = "1";

    /** 门诊标识*/
    public static final String HOSPFLAG_FOR_OP = "op";

    /** 需要预警分析*/
    public static final String IS_NEED_ALERT = "1";

    /** 时间点:一周前*/
    public static final int ONE_WEEK_AGO = 7;

    /** 时间点：一个月*/
    public static final int ONE_MONTH_AGO = 30;
}
