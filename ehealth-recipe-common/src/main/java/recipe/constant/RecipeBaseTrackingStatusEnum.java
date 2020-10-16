package recipe.constant;


/**
 * @author Created by liuxaiofeng on 2020/10/12.
 *         基础服务-处方  物流状态对应枚举
 */
public enum RecipeBaseTrackingStatusEnum {
    //基础服务-处方  物流状态对应枚举

    READY_STATUS(0, 3, "待配送"),
    SEND_STATUS(1, 4, "配送中"),
    FINISH_STATUS(2, 5, "配送完成");

    /**
     * 基础服务物流状态码
     */
    private Integer baseCode;
    /**
     * 处方业务物流状态码
     */
    private Integer recipeCode;
    /**
     * 描述
     */
    private String desc;

    RecipeBaseTrackingStatusEnum(Integer baseCode, Integer recipeCode, String desc) {
        this.baseCode = baseCode;
        this.recipeCode = recipeCode;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getBaseCode() {
        return baseCode;
    }

    public void setBaseCode(Integer baseCode) {
        this.baseCode = baseCode;
    }

    public Integer getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(Integer recipeCode) {
        this.recipeCode = recipeCode;
    }

    public static RecipeBaseTrackingStatusEnum getByBaseCode(Integer code) {
        for (RecipeBaseTrackingStatusEnum e : RecipeBaseTrackingStatusEnum.values()) {
            if (e.getBaseCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

}
