package recipe.enumerate.type;

/**
 * @description： 处方订单费用类型
 * @author： whf
 * @date： 2022-06-02 13:42
 */
public enum RecipeOrderFeeTypeEnum {
    // 订单费用类型
    REGISTER_FEE("1", "挂号费"),
    TCM_FEE("2", "中医辨证论治费"),
    DECOCTION_FEE("3", "代煎费");

    private String type;
    private String name;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    RecipeOrderFeeTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

}
