package recipe.enumerate.status;

/**
 * 处方状态枚举
 *
 * @author fuzi
 */
public enum OrderStateEnum {
    /**
     * 处方父状态
     */
    NONE(0, "默认", ""),
    PROCESS_STATE_SUBMIT(1, "预下单", ""),
    PROCESS_STATE_AUDIT(2, "待支付", ""),
    PROCESS_STATE_ORDER(3, "处理中", ""),
    PROCESS_STATE_DISPENSING(4, "已完成", ""),
    PROCESS_STATE_CANCELLATION(9, "已作废", ""),

    /**
     * 处方子状态:作废
     */
    SUB_CANCELLATION_DOCTOR_REPEAL(91, "医生撤销", ""),
    SUB_CANCELLATION_AUDIT_NOT_PASS(92, "药师审核未通过", "药师审核不通过"),
    SUB_CANCELLATION_REFUSE_ORDER(93, "售药方拒绝订单", "已拒发"),
    SUB_CANCELLATION_RETURN_DRUG(94, "售药方退药", "已退药"),
    SUB_CANCELLATION_USER(95, "用户取消", "患者手动取消"),
    SUB_CANCELLATION_TIMEOUT_NON_PAYMENT(96, "超时未支付", "过期处方（未支付过期 /未处理过期）"),
    SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE(97, "超时未取药", "超时未取药系统取消"),
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
