package recipe.enumerate.type;

/**
 * 医保类型
 */
public enum MedicalTypeEnum {
    SELF_PAY(0, "自费"),
    MEDICAL_PAY(1, "医保支付");

    private Integer type;
    private String name;

    MedicalTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }
}
