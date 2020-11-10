package recipe.constant;

/**
 * @author Created by liuxiaofeng on 2020/11/6.
 * 处方费用枚举
 */
public enum RecipeFeeEnum {
    /**/
    DRUG_FEE(1,"药费"),
    REGISTER_FEE(2,"挂号费"),
    AUDIT_FEE(3,"审方费"),
    EXPRESS_FEE(4,"配送费"),
    PAY_FEE(5,"实际支付费用");

    /**
     * 费用类型
     */
    private Integer feeType;
    /**
     * 描述
     */
    private String desc;

    RecipeFeeEnum(Integer feeType, String desc) {
        this.feeType = feeType;
        this.desc = desc;
    }

    public Integer getFeeType() {
        return feeType;
    }

    public void setFeeType(Integer feeType) {
        this.feeType = feeType;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static RecipeFeeEnum getBytype(Integer type) {
        for (RecipeFeeEnum e : RecipeFeeEnum.values()) {
            if (e.getFeeType().equals(type)) {
                return e;
            }
        }
        return null;
    }
}
