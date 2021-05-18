package recipe.factory.status.constant;

/**
 * 线下转线上查询方式 枚举
 *
 * @author fuzi
 */
public enum OfflineToOnlineEnum {

    OFFLINE_TO_ONLINE_NO_PAY(1, "待缴费", ""),
    OFFLINE_TO_ONLINE_ALREADY_PAY(2, "已缴费", "")
    ;
    private Integer type;
    private String name;
    private String desc;

    OfflineToOnlineEnum(Integer type, String name, String desc) {
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

    public static String getOfflineToOnlineName(Integer type) {
        for (OfflineToOnlineEnum e : OfflineToOnlineEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
}
