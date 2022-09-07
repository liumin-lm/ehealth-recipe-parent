package recipe.enumerate.status;

/**
 * @description： 订单物流状态
 * @author： whf
 * @date： 2022-09-07 14:25
 */
public enum OrderLogisticsStateEnum {
    /**
     * 订单父状态
     */
    NONE(0, "默认", ""),
    LOGISTICS_STATE_DISPENSING(1, "待发药", ""),
    LOGISTICS_STATE_DISTRIBUTION(2, "配送中", ""),
    LOGISTICS_STATE_MEDICINE(3, "待取药", ""),;

    private Integer type;
    private String name;
    private String desc;

    OrderLogisticsStateEnum(Integer type, String name, String desc) {
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
}