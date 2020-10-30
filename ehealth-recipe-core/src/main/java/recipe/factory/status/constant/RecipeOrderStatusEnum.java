package recipe.factory.status.constant;

/**
 * 订单状态枚举
 *
 * @author fuzi
 */
public enum RecipeOrderStatusEnum {
    ORDER_STATUS_AWAIT_SHIPPING(3, "待配送", ""),
    ORDER_STATUS_PROCEED_SHIPPING(4, "配送中", ""),
    ORDER_STATUS_DONE(5, "已完成", ""),
    ORDER_STATUS_DONE_DISPENSING(13, "已发药", ""),
    ORDER_STATUS_DECLINE(14, "已拒发", ""),
    ORDER_STATUS_DRUG_WITHDRAWAL(15, "已退药", "");
    private Integer type;
    private String name;
    private String desc;

    private RecipeOrderStatusEnum(Integer type, String name, String desc) {
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

    public static String getOrderStatus(Integer type) {
        for (RecipeOrderStatusEnum e : RecipeOrderStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
}
