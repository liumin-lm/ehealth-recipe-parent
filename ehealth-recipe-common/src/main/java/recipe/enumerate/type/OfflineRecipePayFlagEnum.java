package recipe.enumerate.type;

/**
 * 线下处方支付状态
 */
public enum OfflineRecipePayFlagEnum {

    OFFLINE_RECIPE_NO_PAY(0, 3, "代缴费"),
    OFFLINE_RECIPE_PAY(1, 7, "已缴费"),
    OFFLINE_RECIPE_CANCELLATION(3, 9, "已退费");

    private Integer type;
    private Integer state;
    private String name;

    OfflineRecipePayFlagEnum(Integer type, Integer state, String name){
        this.type = type;
        this.state = state;
        this.name = name;
    }

    public static OfflineRecipePayFlagEnum getByType(Integer type){
        OfflineRecipePayFlagEnum[] enums = OfflineRecipePayFlagEnum.values();
        for (OfflineRecipePayFlagEnum configEnum : enums) {
            if (configEnum.getType().equals(type)) {
                return configEnum;
            }
        }
        return null;
    }

    public static OfflineRecipePayFlagEnum getByState(Integer state){
        OfflineRecipePayFlagEnum[] enums = OfflineRecipePayFlagEnum.values();
        for (OfflineRecipePayFlagEnum configEnum : enums) {
            if (configEnum.getState().equals(state)) {
                return configEnum;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
