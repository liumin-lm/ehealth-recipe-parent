package recipe.enumerate.type;

/**
 * 开处方条件类型
 */
public enum WriteRecipeConditionTypeEnum {
    NO_CONDITION(0,"不判断任何状态"),
    EFFECTIVE_REVISIT(1,"只有存在进行中的复诊（复诊单号）才可以开方"),
    EFFECTIVE_REGISTER(2,"只有存在进行中的复诊（挂号序号）才可以开方");

    private Integer type;
    private String name;

    WriteRecipeConditionTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }
}
