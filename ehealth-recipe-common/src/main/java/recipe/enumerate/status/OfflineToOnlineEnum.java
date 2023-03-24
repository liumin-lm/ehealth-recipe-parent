package recipe.enumerate.status;

/**
 * 线下转线上查询方式 枚举
 * type:[1:去his查未缴费处方 2:去his查未缴费处方]
 * @author liumin
 */
public enum OfflineToOnlineEnum {

    OFFLINE_TO_ONLINE_NO_PAY(1, "onready", "待处理",3),
    OFFLINE_TO_ONLINE_ALREADY_PAY(2, "isover", "已处理",7),
    OFFLINE_TO_ONLINE_ONGOING(1, "ongoing", "进行中",9);
    //1表示未缴费 2表示已缴费
    private Integer type;
    private String name;
    private String desc;
    private Integer processState;

    OfflineToOnlineEnum(Integer type, String name, String desc, Integer processState) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.processState = processState;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getProcessState() {
        return processState;
    }

    public void setProcessState(Integer processState) {
        this.processState = processState;
    }

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

    public static OfflineToOnlineEnum getOfflineToOnlineEnum(Integer processState) {
        for (OfflineToOnlineEnum e : OfflineToOnlineEnum.values()) {
            if (e.processState.equals(processState)) {
                return e;
            }
        }
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY;
    }
}
