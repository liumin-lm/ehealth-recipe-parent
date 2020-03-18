package recipe.recipecheck;

/**
 * created by shiyuping on 2020/3/17
 */
public enum RecipeCheckEnum {
    HISCHECK(2,"hisCheckRecipeService"),
    PLATCHECK(1,"platRecipeCheckService");
    private Integer checkMode;
    private String serviceName;

    RecipeCheckEnum(Integer checkMode, String serviceName) {
        this.checkMode = checkMode;
        this.serviceName = serviceName;
    }

    public Integer getCheckMode() {
        return checkMode;
    }

    public void setCheckMode(Integer checkMode) {
        this.checkMode = checkMode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
