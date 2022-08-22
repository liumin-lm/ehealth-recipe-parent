package recipe.enumerate.type;

/**
 * 寄付计价方式
 * @author 刘敏
 * @date 2022\8\19
 */
public enum ConsignmentPricingMethodEnum {
    LOGISTICS_COMPANY_PRICE("0", "物流公司预估价格"),
    PLATFORM_PRICE("1", "机构设置物流价格"),
    SHOWFREEPIC("100", "全部");
    private String type;
    private String name;

    ConsignmentPricingMethodEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
