package recipe.serviceprovider.recipeorder.service.constant;

/**
 * @author xlx
 * @Date: 2020/12/15
 * @Description:com.xlx.springbootweb
 * @version:1.0
 */
public enum RecipeTaskEnum {
    RECIPE_TASK_STATUS_UNPAID(1, "您有一条待支付的在线复诊处方", "待支付", "处方支付"),
    RECIPE_TASK_STATUS_PENDING(-10, "您有一条待处理的在线复诊处方", "待处理", "去处理"),
    RECIPE_TASK_STATUS_SHIPPING(3, "您的在线复诊处方正在备货中,即将发药", "待配送", "查看详情"),
    RECIPE_TASK_STATUS_DELIVERY(4, "您的在线复诊处方已发药,请注意查收", "配送中", "查看详情"),
    NONE(-9, "未知", "", ""),
    ;
    private Integer type;
    private String taskName;
    private String busStatusName;
    private String buttonName;

    RecipeTaskEnum(Integer type, String taskName, String busStatusName, String buttonName) {
        this.type = type;
        this.taskName = taskName;
        this.busStatusName = busStatusName;
        this.buttonName = buttonName;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getBusStatusName() {
        return busStatusName;
    }

    public String getButtonName() {
        return buttonName;
    }

    /**
     * 根据类型 获取枚举类型
     *
     * @param type
     * @return
     */
    public static RecipeTaskEnum getRecipeStatusEnum(Integer type) {
        for (RecipeTaskEnum e : RecipeTaskEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }
}
