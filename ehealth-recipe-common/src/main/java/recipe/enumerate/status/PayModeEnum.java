package recipe.enumerate.status;

/**
 * 发药方式 枚举
 *
 * @author fuzi
 */
public enum PayModeEnum {

    // 0：线下支付   1：在线支付
    OFFLINE_PAY(0, "到院支付", ""),
    ONLINE_PAY(1, "在线支付", ""),
    ;
    private Integer type;
    private String name;
    private String desc;

    PayModeEnum(Integer type, String name, String desc) {
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

    public static String getPayModeEnumName(Integer type) {
        for (PayModeEnum e : PayModeEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
    public static PayModeEnum getPayModeEnum(Integer type) {
        for (PayModeEnum e : PayModeEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
