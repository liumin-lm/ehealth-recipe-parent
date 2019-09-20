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
    USED("6", "USED");

    private String key;
    private String value;

    RecipeStatusEnum(String name, String value) {
        this.key = name;
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
