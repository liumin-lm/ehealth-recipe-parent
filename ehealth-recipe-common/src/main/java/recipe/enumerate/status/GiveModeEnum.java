package recipe.enumerate.status;

/**
 * 发药方式 枚举
 *
 * @author fuzi
 */
public enum GiveModeEnum {

    GIVE_MODE_HOME_DELIVERY(1, "配送到家", ""),
    GIVE_MODE_HOSPITAL_DRUG(2, "医院取药", ""),
    GIVE_MODE_PHARMACY_DRUG(3, "药店取药", ""),
    GIVE_MODE_PATIENTS_OPTIONAL(4, "患者自选", ""),
    GIVE_MODE_DOWNLOAD_RECIPE(5, "下载处方签", ""),
    ;
    private Integer type;
    private String name;
    private String desc;

    GiveModeEnum(Integer type, String name, String desc) {
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

    public static String getGiveModeName(Integer type) {
        for (GiveModeEnum e : GiveModeEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }
    public static GiveModeEnum getGiveModeEnum(Integer type) {
        for (GiveModeEnum e : GiveModeEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
