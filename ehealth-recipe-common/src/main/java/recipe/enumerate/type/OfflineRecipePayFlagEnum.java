package recipe.enumerate.type;

/**
 * 线下处方支付状态
 *
 */
public enum OfflineRecipePayFlagEnum {

    OFFLINE_RECIPE_NO_PAY(0, 3, 0, "onready","代缴费"),
    OFFLINE_RECIPE_PAY(1, 7, 1, "isover","已缴费"),
    OFFLINE_RECIPE_CANCELLATION(3, 9, 0, "isover","已退费");
    //TODO 已作废也走的已完成的查询querydata isover

    private Integer type;
    private Integer state;
    private Integer payFlag;
    private String name;
    private String hisState;

    OfflineRecipePayFlagEnum(Integer type, Integer state, Integer payFlag,String hisState, String name){
        this.type = type;
        this.state = state;
        this.payFlag = payFlag;
        this.name = name;
        this.hisState=hisState;
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

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public String getHisState() {
        return hisState;
    }

    public void setHisState(String hisState) {
        this.hisState = hisState;
    }
}
