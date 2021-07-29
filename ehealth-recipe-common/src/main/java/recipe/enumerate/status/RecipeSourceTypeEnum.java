package recipe.enumerate.status;

/**
 * 线下线上处方
 *
 * @author liumin
 */
public enum RecipeSourceTypeEnum {

    ONLINE_RECIPE(1, "线上处方"),
    OFFLINE_RECIPE(2, "线下处方");
    //1表示未缴费 2表示已缴费
    private Integer type;
    private String name;

    RecipeSourceTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }


    public String getName() {
        return name;
    }

    public static String getOfflineToOnlineName(Integer type) {
        for (RecipeSourceTypeEnum e : RecipeSourceTypeEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "未知";
    }

    public static Integer getOfflineToOnlineType(String name) {
        for (RecipeSourceTypeEnum e : RecipeSourceTypeEnum.values()) {
            if (e.name.equals(name)) {
                return e.type;
            }
        }
        return 1;
    }
}
