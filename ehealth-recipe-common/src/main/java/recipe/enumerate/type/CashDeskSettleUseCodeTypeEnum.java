package recipe.enumerate.type;

/**
 * @author yinsheng
 * @date 2022\10\31 0029 19:29
 */
public enum CashDeskSettleUseCodeTypeEnum {

    HIS_RECIPE_CODE(1, "his处方编码"),
    HIS_CASH_CODE(2, "his缴费编码");
    CashDeskSettleUseCodeTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
