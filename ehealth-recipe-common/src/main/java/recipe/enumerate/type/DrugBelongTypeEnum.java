package recipe.enumerate.type;

/**
 * 药品类型：1 药品 2 诊疗项目 3 保密药品
 */
public enum DrugBelongTypeEnum {
    SECRECY_DRUG(3, "保密药品");
    private Integer type;
    private String name;

    DrugBelongTypeEnum(Integer type, String name) {
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
