package recipe.enumerate.type;

/**
 * 但复方类型
 * 单复方表示0:无状态，1单方，2复方
 * @author xy
 * @date 2022\1\24 0029 19:29
 */
public enum SingleOrCompoundEnum {
    HOSPITAL_CARD(0, ""),
    MEDICARE_CARD(1, "单方"),
    MEDICAL_RECORD_CARD(2, "复方"),
 ;
    SingleOrCompoundEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据类型 获取名称
     *
     * @param type
     * @return
     */
    public static String getSingleOrCompoundName(Integer type) {
        for (SingleOrCompoundEnum e : SingleOrCompoundEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "";
    }
}
