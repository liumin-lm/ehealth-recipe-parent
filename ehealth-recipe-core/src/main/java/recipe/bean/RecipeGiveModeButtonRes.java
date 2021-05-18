package recipe.bean;

import lombok.Data;

import java.util.List;

/**
 * @description： 合并支付购药方式按钮出参
 * @author： whf
 * @date： 2021-05-18 13:40
 */
@Data
public class RecipeGiveModeButtonRes {
    /**
     * 购药方式key
     */
    private String giveModeKey;

    /**
     * 购药方式文案
     */
    private String giveModeText;

    /**
     * 购药方式支持的处方id列表
     */
    private List<Integer> recipeIds;

    /**
     * 按钮是否可以点击
     */
    private Boolean buttonFlag;

    /**
     * 页面跳转类型 1：正常跳转，2：跳转门诊缴费，3：第三方页面
     */
    private Integer jumpType;
}
