package recipe.enumerate.type;

/**
 * 咨询/复诊 业务类型
 * @author yinsheng
 * @date 2021\9\7 0007 13:58
 */
public enum BussSourceTypeEnum {

    BUSSSOURCE_CONSULT(1, "咨询"),
    BUSSSOURCE_REVISIT(2, "复诊");

    private Integer type;
    private String name;

    BussSourceTypeEnum(Integer type, String name){
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
