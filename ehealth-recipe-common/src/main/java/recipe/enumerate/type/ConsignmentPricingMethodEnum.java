package recipe.enumerate.type;

/**
 * 寄付计价方式
 * @author 刘敏
 * @date 2022\8\19
 */
public enum ConsignmentPricingMethodEnum {
    LOGISTICS_COMPANY_PRICE(0, "物流公司预估价格"),
    PLATFORM_PRICE(1, "机构设置物流价格"),
    SHOWFREEPIC(100, "全部");
    private Integer type;
    private String name;

    ConsignmentPricingMethodEnum(Integer type, String name){
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
