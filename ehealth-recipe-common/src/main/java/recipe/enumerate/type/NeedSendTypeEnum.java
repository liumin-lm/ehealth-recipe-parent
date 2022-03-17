package recipe.enumerate.type;

/**
 * 需要配送的类型
 */
public enum NeedSendTypeEnum {

    NO_NEED_SEND_TYPE(0, "无需配送，直接核销"),
    NEED_SEND_TYPE(1, "需要配送");

    private Integer type;
    private String name;

    NeedSendTypeEnum(Integer type, String name){
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
