package recipe.enumerate.status;

/**
 * 处方状态枚举
 *
 * @author fuzi
 */
public enum RecipeStateEnum {
    /**
     * 处方父状态
     */
    NONE(0, "默认", ""),
    PROCESS_STATE_SUBMIT(1, "待提交", ""),
    PROCESS_STATE_AUDIT(2, "待审核", ""),
    PROCESS_STATE_ORDER(3, "待下单", ""),
    PROCESS_STATE_DISPENSING(4, "待发药", ""),
    PROCESS_STATE_DISTRIBUTION(5, "配送中", ""),
    PROCESS_STATE_MEDICINE(6, "待取药", ""),
    PROCESS_STATE_DONE(7, "已完成", ""),
    PROCESS_STATE_DELETED(8, "已删除", ""),
    PROCESS_STATE_CANCELLATION(9, "已作废", ""),

    /**
     * 处方子状态:删除
     */
    SUB_DELETED_REVISIT_END(81, "复诊结束", ""),
    SUB_DELETED_DOCTOR_NOT_SUBMIT(82, "医生未提交删除", ""),

    /**
     * 处方子状态:作废
     */
    SUB_CANCELLATION_DOCTOR(91, "医生未提交删除", ""),
    SUB_CANCELLATION_AUDIT_NOT_PASS(92, "药师审核未通过", "药师不双签，审核不通过"),
    SUB_CANCELLATION_REFUSE_ORDER(93, "售药方拒绝订单", "已拒发"),
    SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE(94, "患者超时未取药", "患者未取药"),
    SUB_CANCELLATION_TIMEOUT_NOT_ORDER(95, "已过有效期未下单", "过期处方（未支付过期 /未处理过期）"),
    ;

    private Integer type;
    private String name;
    private String desc;

    RecipeStateEnum(Integer type, String name, String desc) {
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
    public static RecipeStateEnum getRecipeStateEnum(Integer type) {
        for (RecipeStateEnum e : RecipeStateEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }

}
