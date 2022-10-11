package recipe.enumerate.type;

/**
 * 处方剂型类型 1 中药饮片 2 配方颗粒
 */
public enum RecipeDrugFormTypeEnum {

    TCM_DECOCTION_PIECES(1, "中药饮片"),
    TCM_FORMULA_PIECES(2, "配方颗粒");

    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    RecipeDrugFormTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public static String getDrugForm(Integer type) {
        for (RecipeDrugFormTypeEnum e : RecipeDrugFormTypeEnum.values()) {
            if (e.type.equals(type)) {
                return e.name;
            }
        }
        return "";
    }
}
