package recipe.constant;

/**
 * @author： gmw
 * @date： 2019/11/06
 * @description： 处方订单状态同步处方枚举（订单-处方）
 * @version： 1.0
 */
public enum RecipeStatusToOrderEnum {
    //待取药
    WAITDRUG(12, 3),
    //待配送
    WAITSEND(3, 5),
    //配送中
    INSEND(4, 4),
    //已完成
    FINISH(5, 6),
    //取药失败
    FAIL(17, 17);

    private Integer key;
    private Integer value;

    RecipeStatusToOrderEnum(Integer key, Integer value) {
        this.key = key;
        this.value = value;
    }
    public static Integer getKey(Integer value){
        for(RecipeStatusToOrderEnum c:RecipeStatusToOrderEnum.values()){
            if(c.getValue().equals(value)){
                return c.getKey();
            }
        }
        return null;
    }

    public static Integer getValue(Integer key){
        for(RecipeStatusToOrderEnum c:RecipeStatusToOrderEnum.values()){
            if(c.getKey().equals(key)){
                return c.getValue();
            }
        }
        return null;
    }

    public static RecipeStatusToOrderEnum getEnum(Integer value){
        for(RecipeStatusToOrderEnum c:RecipeStatusToOrderEnum.values()){
            if(c.getValue().equals(value)){
                return c;
            }
        }
        return null;
    }
    public Integer getKey() {
        return key;
    }

    public Integer getValue() {
        return value;
    }


}
