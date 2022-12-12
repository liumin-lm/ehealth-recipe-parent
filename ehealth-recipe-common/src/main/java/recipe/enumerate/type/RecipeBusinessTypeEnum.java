package recipe.enumerate.type;

/**
 * @description： 处方业务类型
 * @author： whf
 * @date： 2022-03-03 16:01
 */
public enum RecipeBusinessTypeEnum {

    /**
     * 处方业务类型
     */
    BUSINESS_RECIPE_OUTPATIENT(1, "门诊处方"),
    BUSINESS_RECIPE_REVISIT(2, "复诊处方"),
    BUSINESS_RECIPE_OTHER(3, "其他处方"),
    ;

    private Integer type;
    private String name;

    RecipeBusinessTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public static RecipeBusinessTypeEnum getRecipeBusinessTypeEnum(Integer type){
        for (RecipeBusinessTypeEnum e : RecipeBusinessTypeEnum.values()) {
            if (type.equals(e.type)) {
                return e;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Integer getRecipeBusinessType(Integer bussSource) {
        if (null == bussSource) {
            return 3;
        }
        switch (bussSource) {
            case 1:
            case 5:
                //门诊处方
                return 1;
            case 2:
                //复诊处方
                return 2;
            default:
                //其他处方
                return 3;
        }
    }
}
