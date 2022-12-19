package recipe.enumerate.type;

import org.apache.commons.lang3.StringUtils;

/**
 * @description： 处方业务类型
 * @author： whf
 * @date： 2022-03-03 16:01
 */
public enum FastRecipeFlagEnum {
    /**
     * 处方业务类型
     */
    FAST_RECIPE_FLAG_OTHER(0, "其他"),
    FAST_RECIPE_FLAG_QUICK(1, "快捷处方"),
    BUSINESS_RECIPE_REVISIT(2, "医嘱申请复诊"),
    BUSINESS_RECIPE_CONTINUE(3, "一键续方复诊"),
    ;

    private static final String REVISIT_SOURCE_YZSQ = "fz-yzsq-001";

    private static final String REVISIT_SOURCE_YJXF = "fz-yjxf-001";

    private static final String REVISIT_SOURCE_BJGY = "fz-bjgy-001";
    private Integer type;
    private String name;

    FastRecipeFlagEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @param sourceTag 入口来源标记
     * @return
     */
    public static Integer getFastRecipeFlag(String sourceTag) {
        if (StringUtils.isEmpty(sourceTag)) {
            return FAST_RECIPE_FLAG_OTHER.type;
        }
        //设置处方入口类型
        switch (sourceTag) {
            case REVISIT_SOURCE_BJGY:
                return FAST_RECIPE_FLAG_QUICK.type;
            case REVISIT_SOURCE_YZSQ:
                return BUSINESS_RECIPE_REVISIT.type;
            case REVISIT_SOURCE_YJXF:
                return BUSINESS_RECIPE_CONTINUE.type;
            default:
                return FAST_RECIPE_FLAG_OTHER.type;
        }
    }
}
