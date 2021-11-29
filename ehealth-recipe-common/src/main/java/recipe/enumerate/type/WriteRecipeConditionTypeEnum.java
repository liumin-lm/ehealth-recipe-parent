package recipe.enumerate.type;

/**
 * 开处方条件类型
 */
public enum WriteRecipeConditionTypeEnum {
    NO_CONDITION("1","不判断任何状态"),
    EFFECTIVE_REVISIT("2","只有存在进行中的复诊（复诊单号）才可以开方"),
    EFFECTIVE_REGISTER("3","只有存在进行中的复诊（挂号序号）才可以开方");

    private String type;
    private String name;

    WriteRecipeConditionTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }
}
