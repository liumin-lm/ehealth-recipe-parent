package recipe.enumerate.type;

/**
 * @description： 处方订单详情支付类型 枚举
 * @author： whf
 * @date： 2021-09-22 15:10
 */
public enum RecipeOrderDetailFeeEnum {
    /**
     * 药品费用
     */
    DRUG_FEE(1, "drugFee"),
    /**
     * 代煎费
     */
    DECOCTION_FEE(1, "decoctionFee"),
    /**
     * 中医辨证论治费
     */
    TCM_FEE(1, "TCMFee"),
    /**
     * 审方服务费
     */
    SERVICE_FEE(2, "serviceFee"),
    /**
     * 配送费
     */
    FREIGHT_FEE(2, "freightFee"),
    ;

    private Integer type;
    private String name;

    RecipeOrderDetailFeeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }
    public static RecipeOrderDetailFeeEnum getEnumByType(String name){
        RecipeOrderDetailFeeEnum[] enums = RecipeOrderDetailFeeEnum.values();
        for (RecipeOrderDetailFeeEnum configEnum : enums) {
            if (configEnum.getName().equals(name)) {
                return configEnum;
            }
        }
        return null;
    }


    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
