package recipe.constant;

/**
 * @author yinsheng
 * @date 2020\11\20 0020 09:55
 */
public class RefundNodeStatusConstant {

    //审核中；
    public static final Integer REFUND_NODE_READY_AUDIT_STATUS = 0;
    //1-审核通过，退款成功；
    public static final Integer REFUND_NODE_SUCCESS_STATUS = 1;
    //2-审核通过，退款失败；
    public static final Integer REFUND_NODE_FAIL_AUDIT_STATUS = 2;
    //3-审核不通过
    public static final Integer REFUND_NODE_NOPASS_AUDIT_STATUS = 3;
}
