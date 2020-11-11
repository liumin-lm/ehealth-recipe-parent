package recipe.factory.status.constant;

public class RecipeStatusEnum {
    ORDER_STATUS_READY_PAY(1,"待支付",""),

    ORDER_STATUS_READY_GET_DRUG(2,"待取药",""),

    ORDER_STATUS_AWAIT_SHIPPING(3,"待配送",""),

    ORDER_STATUS_PROCEED_SHIPPING(4,"配送中",""),

    ORDER_STATUS_DONE(5,"已完成",""),

    ORDER_STATUS_CANCEL_NOT_PASS(6,"审核不通过","已取消"),

    ORDER_STATUS_CANCEL_MANUAL(7,"已取消","手动取消"),

    ORDER_STATUS_CANCEL_AUTO(8,"已取消","处方单自动取消或其他原因导致的订单取消"),

    ORDER_STATUS_READY_CHECK(9,"待审核",""),

    ORDER_STATUS_READY_DRUG(11,"准备中","药店取药（无库存准备中"),

    ORDER_STATUS_DONE_DISPENSING(13,"已发药",""),

    ORDER_STATUS_DECLINE(14,"已拒发",""),

    ORDER_STATUS_DRUG_WITHDRAWAL(15,"已退药","");

    private Integer type;
    private String name;
    private String desc;

    RecipeStatusEnum(Integer type, String name, String desc) {
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
        for (RecipeStatusEnum e : RecipeStatusEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
}
