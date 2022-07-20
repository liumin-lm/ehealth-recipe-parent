package recipe.enumerate.type;

/**
 * 接方模式
 */
public enum SupportModeTypeEnum {
    SUPPORT_MODE_ACCEPT(1, "接方模式");
    private Integer type;
    private String name;

    SupportModeTypeEnum(Integer type, String name){
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
