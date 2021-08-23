package recipe.enumerate.type;

/**
 * opbase配置 处方是否允许退费的类型
 * 运营平台配置项(退费限制 refundPattern)
 * @author yinsheng
 * @date 2021\8\23 0023 09:53
 */
public enum RecipeRefundConfigEnum {

    HAVE_BUSS(1, "开过业务单不退费"),
    HAVE_PAY(2, "有未退费或取消的业务单不允许退费");

    private Integer type;
    private String name;

    RecipeRefundConfigEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
