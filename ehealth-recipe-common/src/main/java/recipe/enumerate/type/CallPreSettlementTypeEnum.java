package recipe.enumerate.type;

/**
 * @description：终端配置--是否调用预结算
 * @author： whf
 * @date： 2022-12-15 10:46
 */
public enum CallPreSettlementTypeEnum {

    ORGAN("1", "以机构为准"),
    NO_CALL("2", "不调用");

    private String type;
    private String name;

    CallPreSettlementTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
