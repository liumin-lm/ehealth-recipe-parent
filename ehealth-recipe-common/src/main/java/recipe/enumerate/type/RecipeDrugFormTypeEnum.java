package recipe.enumerate.type;

import org.apache.commons.lang3.StringUtils;
import recipe.util.ValidateUtil;

/**
 * 处方剂型类型 1 中药饮片 2 配方颗粒
 */
public enum RecipeDrugFormTypeEnum {
    TCM_DECOCTION_PIECES(1, "饮片方", "中药饮片"),
    TCM_FORMULA_PIECES(2, "颗粒方", "配方颗粒"),
    TCM_FORMULA_CREAM_FORMULA(3, "膏方", "中药饮片"),
    ;

    private Integer type;
    private String name;
    private String desc;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    RecipeDrugFormTypeEnum(Integer type, String name, String desc){
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public static String getDrugForm(Integer type) {
        if (ValidateUtil.integerIsEmpty(type)) {
            return TCM_DECOCTION_PIECES.desc;
        }
        for (RecipeDrugFormTypeEnum e : RecipeDrugFormTypeEnum.values()) {
            if (e.type.equals(type)) {
                return e.desc;
            }
        }
        return "";
    }

    public static Integer getDrugFormType(String desc) {
        if (StringUtils.isEmpty(desc)) {
            return TCM_DECOCTION_PIECES.type;
        }
        for (RecipeDrugFormTypeEnum e : RecipeDrugFormTypeEnum.values()) {
            if (e.desc.equals(desc)) {
                return e.type;
            }
        }
        return null;
    }

    public static Integer getDrugFormName(String name) {
        for (RecipeDrugFormTypeEnum e : RecipeDrugFormTypeEnum.values()) {
            if (e.name.equals(name) || e.name.contains(name)) {
                return e.type;
            }
        }
        return null;
    }
}
