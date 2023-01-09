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

    /**
     * 根据类型 获取枚举类型
     *
     * @param type
     * @return
     */
    public static OrderLogisticsStateEnum getOrderLogisticsStateEnum(Integer type) {
        for (OrderLogisticsStateEnum e : OrderLogisticsStateEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }

    /**
     * 根据类型 获取名称(为了type为null的时候不取默认)
     *
     * @param type
     * @return
     */
    public static String getOrderLogisticsStateName(Integer type) {
        for (OrderLogisticsStateEnum e : OrderLogisticsStateEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return null;
    }
}
