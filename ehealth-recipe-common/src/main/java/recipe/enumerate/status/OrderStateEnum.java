package recipe.enumerate.status;

/**
 * 订单状态枚举
 *
 * @author fuzi
 */
public enum OrderStateEnum {
    /**
     * 订单父状态
     */
    NONE(0, "默认", ""),
    PROCESS_STATE_READY_PAY(1, "待支付", ""),
    PROCESS_STATE_ORDER_PLACED(2, "已下单", ""),
    PROCESS_STATE_ORDER(3, "处理中", ""),
    PROCESS_STATE_DISPENSING(4, "已完成", ""),
    PROCESS_STATE_CANCELLATION(9, "已作废", ""),

    /**
     * 订单子状态:待支付
     */
    SUB_READY_PAY_NONE(11, "请在invalidTime内完成支付", ""),

    /**
     * 订单子状态:已下单
     */
    SUB_ORDER_PLACED_AUDIT(21, "您已完成支付，耐心等待处方审核", ""),
    SUB_ORDER_ORDER_PLACED(22, "已完成下单", ""),

    /**
     * 订单子状态:处理中
     */
    SUB_ORDER_DELIVERED_MEDICINE(31,"请耐心等待发药",""),
    SUB_ORDER_DELIVERED(32,"药品已送出，请耐心等待",""),
    SUB_ORDER_TAKE_MEDICINE(33,"请尽快前往相关药房取药",""),

    /**
     * 订单子状态:已完成
     */
    SUB_DONE_DOWNLOAD(41,"下载处方笺",""),
    SUB_DONE_OD_PAYMENT(42,"门诊缴费下单",""),
    SUB_DONE_UPLOAD_THIRD(43,"上传到第三方",""),
    SUB_DONE_SELF_TAKE(44,"自取核销",""),
    SUB_DONE_SEND(45,"发药签收",""),


    /**
     * 订单子状态:作废
     */
    SUB_CANCELLATION_SETTLE_FAIL(90, "下单结算失败", "患者下单结算失败"),
    SUB_CANCELLATION_DOCTOR_REPEAL(91, "医生撤销", ""),
    SUB_CANCELLATION_AUDIT_NOT_PASS(92, "药师审核未通过", "药师审核不通过"),
    SUB_CANCELLATION_REFUSE_ORDER(93, "供药方拒绝订单", "已拒发"),
    SUB_CANCELLATION_RETURN_DRUG(94, "供药方退药", "已退药"),
    SUB_CANCELLATION_TIMEOUT_NON_PAYMENT(95, "超时未支付", "过期处方（未支付过期 /未处理过期）"),
    SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE(96, "超时未取药", "超时未取药系统取消"),
    SUB_CANCELLATION_USER(97, "用户取消", "患者手动取消"),
    SUB_CANCELLATION_OTHER(98, "其他原因取消订单", ""),
    SUB_CANCELLATION_REFUND(99, "处方已退费成功", ""),
    ;

    private Integer type;
    private String name;
    private String desc;

    OrderStateEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }


    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据类型 获取枚举类型
     *
     * @param type
     * @return
     */
    public static OrderStateEnum getOrderStateEnum(Integer type) {
        for (OrderStateEnum e : OrderStateEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }

}
