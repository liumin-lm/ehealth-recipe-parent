package recipe.enumerate.type;

/**
 * 运营平台退费页面查询业务类型
 */
public enum OpRefundBusTypeEnum {
    BUS_TYPE_ALL_ORDER(1, "全部药品订单"),
    BUS_TYPE_REFUND_ORDER(2, "退货待审核列表"),
    BUS_TYPE_FAIL_ORDER(3, "同步失败列表");

    private Integer type;
    private String name;

    OpRefundBusTypeEnum(Integer type, String name){
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
