package recipe.enumerate.type;

/**
 * @description： 药企类型
 * @author： whf
 * @date： 2021-11-22 16:18
 */
public enum EnterpriseCreateTypeEnum {
    /**
     * 药企类型
     */
    MY_SELF(0,"自建"),
    OTHER_SELF(1,"非自建"),
    ;
    private Integer type;
    private String name;

    EnterpriseCreateTypeEnum(Integer type, String name) {
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
