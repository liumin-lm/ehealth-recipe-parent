package recipe.serviceprovider.recipeorder.service.constant;

/**
 * @author xlx
 * @Date: 2020/12/15
 * @Description:com.xlx.springbootweb
 * @version:1.0
 */
public enum RecipeTaskEnum {
    RECIPE_TASK_STATUS_UNPAID(-10, "您有一张可购药的处方，请在有效期内购药", "审核完成，待购药", "去下单"),
    RECIPE_TASK_STATUS_PENDING(1, "您有一张处方未支付，请在有效期内支付", "完成下单，待支付", "去支付"),
    RECIPE_TASK_STATUS_SHIPPING(3, "您有一张处方正在备货中，即将发药", "待发药", "去查看"),
    RECIPE_TASK_STATUS_DELIVERY(4, "您有一张处方已发药，请注意查收", "待收药", "去查看"),
    RECIPE_TASK_STATUS_GET_DRUG(2, "您有一张处方等待上门取药，请及时取药", "待取药", "去查看"),
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
