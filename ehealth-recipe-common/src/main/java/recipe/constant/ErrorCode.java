package recipe.constant;

/**
 * 错误代码定义
 *
 * @author wnw
 */
public class ErrorCode {
    /**
     * 服务成功
     */
    public static final int SERVICE_SUCCEED = 200;

    /**
     * 服务异常，提示消息
     */
    public static final int SERVICE_ERROR = 609;

    /**
     * 提示消息，单次确认
     */
    public static final int SERVICE_CONFIRM = 908;

    /**
     * 提示消息，二次确认
     */
    public static final int SERVICE_DOUBLE_CONFIRM = 909;

    /**
     * 有待支付的咨询单，不能发起新咨询
     */
    public static final int CONSULT_PENDING = 611;

    /**
     * 优惠券不能使用时，弹框一秒显示“亲，该优惠券已过期”
     */
    public static final int COUPON_EXPIRED = 612;

    /**
     * 当前患者和医生已存在进行中业务
     */
    public static final int REQUEST_MODE_EXISTS = 613;

    /**
     * 患者注册时身份证号有问题
     */
    public static final int PATIENT_IDCARD_ERROR = 615;

    /**
     * 患者注册时手机号有问题
     */
    public static final int PATIENT_MOBILE_ERROR = 616;

    /**
     * 重复发起支付
     */
    public static final int REPEAT_ORDER = 617;

    /**
     * 合理用药建议
     */
    public static final int RATIONAL_DRUG_USE = 618;
}
