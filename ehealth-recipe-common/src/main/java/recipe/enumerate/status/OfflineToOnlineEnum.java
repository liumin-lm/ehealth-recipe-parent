package recipe.enumerate.status;

/**
 * 线下转线上查询方式 枚举
 * type:[1:去his查未缴费处方 2:去his查未缴费处方]
 * @author liumin
 */
public enum OfflineToOnlineEnum {

    OFFLINE_TO_ONLINE_NO_PAY(1, "onready", "待处理"),
    OFFLINE_TO_ONLINE_ALREADY_PAY(2, "isover", "已处理"),
    OFFLINE_TO_ONLINE_ONGOING(1, "ongoing", "进行中");
    //1表示未缴费 2表示已缴费
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

    public static Integer getOfflineToOnlineType(String name) {
        for (OfflineToOnlineEnum e : OfflineToOnlineEnum.values()) {
            if (e.name.equals(name)) {
                return e.type;
            }
        }
        return 1;
    }
}
