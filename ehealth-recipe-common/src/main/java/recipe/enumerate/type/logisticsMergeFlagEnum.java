package recipe.enumerate.type;

/**
 * 是否支持合并快递单
 * @author yinsheng
 * @date 2023\01\30 12:13
 */
public enum logisticsMergeFlagEnum {
    LOGISTICS_MERGE_NO_SUPPORT(0, "不支持合并"),
    LOGISTICS_MERGE_SUPPORT(1, "支持合并");

    private Integer type;
    private String name;

    logisticsMergeFlagEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }
}
