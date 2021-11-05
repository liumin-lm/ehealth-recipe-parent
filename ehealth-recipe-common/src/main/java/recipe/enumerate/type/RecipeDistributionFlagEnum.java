package recipe.enumerate.type;


import recipe.constant.RecipeBussConstant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @description：
 * @author： whf
 * @date： 2021-04-13 11:44
 */
public enum RecipeDistributionFlagEnum {


    /**
     * 默认值
     */
    DEFAULT("默认值", 0),

    /**
     *
     * 药企有库存
     */
    DRUGS_HAVE("药企有库存", 1),

    /**
     * 医院有库存
     */
    HOS_HAVE("医院有库存", 2),

    /**
     * 药企库存的情况下药企只支持到店自取
     */
    DRUGS_HAVE_TO("药企库存的情况下药企只支持到店自取", 11),

    /**
     * 药企库存的情况下药企只支持配送
     */
    DRUGS_HAVE_SEND("药企库存的情况下药企只支持配送", 12),
    /**
     * 药企配送
     */
    DRUGS_HAVE_SEND_TFDS("药企配送", 121),
    /**
     * 医院配送
     */
    DRUGS_HAVE_SEND_HOS("医院配送", 122),
    ;

    /**
     * 配送文案
     */
    private String text;
    /**
     * 配送状态
     */
    private Integer type;

    RecipeDistributionFlagEnum(String text, Integer type) {
        this.text = text;
        this.type = type;
    }


    public String getText() {
        return text;
    }

    public Integer getType() {
        return type;
    }

    /**
     * 药企支持 所有购药方式
     */
    public static final List<Integer> drugsEnterpriseAll = Arrays.asList(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS, RecipeBussConstant.DEP_SUPPORT_COD_TFDS, RecipeBussConstant.DEP_SUPPORT_ALL);
    /**
     * 药企支持 到店取药
     */
    public static final List<Integer> drugsEnterpriseTo = Collections.singletonList(RecipeBussConstant.DEP_SUPPORT_TFDS);
    /**
     * 药企支持 配送
     */
    public static final List<Integer> drugsEnterpriseSend = Arrays.asList(RecipeBussConstant.DEP_SUPPORT_ONLINE, RecipeBussConstant.DEP_SUPPORT_COD);

}
