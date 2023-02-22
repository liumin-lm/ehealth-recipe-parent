package recipe.enumerate.type;

/**
 * 退款途径
 */
public enum OrderRefundWayTypeEnum {
    DEFAULT(0, "默认"),
    PATIENT_APPLY(1, "患者端申请"),
    HIS_SETTLE_FAiL(2, "his结算失败"),
    DRUG_ORDER(3, "药品订单退单处理");
    private Integer type;
    private String name;

    OrderRefundWayTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
