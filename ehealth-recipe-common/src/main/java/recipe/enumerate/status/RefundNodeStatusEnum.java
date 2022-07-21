package recipe.enumerate.status;

/**
 * 退费状态
 */
public enum RefundNodeStatusEnum {

    NO_APPLY(-1, "未申请"),
    WAIT_AUDIT(0, "申请审核中"),
    DOCTOR_AGREE(4, "医生已审核，待进一步审核"),
    AGREE_SUCCESS(1, "已同意 退款成功"),
    AGREE_FAIL(2, "已同意 退款失败"),
    NO_AGREE(3, "不同意");

    private Integer type;
    private String name;

    RefundNodeStatusEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public static String getRefundStatus(Integer type) {
        for (RefundNodeStatusEnum e : RefundNodeStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return NO_APPLY.getName();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}