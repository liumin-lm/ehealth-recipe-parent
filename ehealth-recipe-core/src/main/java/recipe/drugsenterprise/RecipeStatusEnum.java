package recipe.drugsenterprise;

/**
 * @author： gmw
 * @date： 2019/9/19
 * @description： 淘宝同步处方状态枚举
 * @version： 1.0
 */
public enum RecipeStatusEnum {
    //可使用
    EFFECTIVE("21", "EFFECTIVE"),
    //使用中
    USING("22", "USING"),
    //已使用
    USED("6", "USED"),
    //已过期
    EXPIRED("20", "EXPIRED"),
    //已退回
    RETURNED("6", "RETURNED");

    private String key;
    private String value;

    RecipeStatusEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public static String getKey(String value){
        for(RecipeStatusEnum c:RecipeStatusEnum.values()){
            if(c.getValue().equals(value)){
                return c.getKey();
            }
        }
        return null;
    }

    public static String getValue(String key){
        for(RecipeStatusEnum c:RecipeStatusEnum.values()){
            if(c.getKey().equals(key)){
                return c.getValue();
            }
        }
        return null;
    }

    public static RecipeStatusEnum getEnum(String value){
        for(RecipeStatusEnum c:RecipeStatusEnum.values()){
            if(c.getValue().equals(value)){
                return c;
            }
        }
        return null;
    }
    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }


}
