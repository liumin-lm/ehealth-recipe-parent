package recipe.enumerate.type;

/**
 * 医生强制自费标识
 * @author yinsheng
 * @date 2022\3\3 0020 14:13
 */
public enum ForceCashTypeEnum {

    FORCE_CASH_TYPE(1, "强制自费"),
    NO_FORCE_CASH_TYPE(2, "不强制自费");

    ForceCashTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
