package recipe.enumerate.type;

/**
 * @description： 指定药企类型
 * @author： whf
 * @date： 2021-10-26 15:36
 */
public enum AppointEnterpriseTypeEnum {
    /**
     * 指定药企类型
     */
    DEFAULT(0, "默认,没有指定药企"),
    ORGAN_APPOINT(1, "机构"),
    ENTERPRISE_APPOINT(2,"药企");

    private Integer type;
    private String name;

    AppointEnterpriseTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public static AppointEnterpriseTypeEnum getAppointEnterpriseTypeEnum(Integer type){
        AppointEnterpriseTypeEnum[] values = AppointEnterpriseTypeEnum.values();
        for (AppointEnterpriseTypeEnum value : values) {
            if(value.getType().equals(type)){
                return value;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
