package recipe.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* @Description: RecipePayTipEnum 类（或接口）是 处方支付提示枚举
* @Author: JRK
* @Date: 2019/9/17
*/
public enum RecipeTipesColorTypeEnum {
    /**
     * 处方进行中
     * 待药师审核, 待支付（待处理的状态）, 待处理, 待取药（待处理/审核通过）, 待配送, 配送中, 待取药（有库存,待处理/审核通过）,待取药（无库存,待处理/审核通过）,准备中(待处理/审核通过)
     */
    Recipe_Ongoing(new ArrayList<>(Arrays.asList(RecipeStatusConstant.READY_CHECK_YS,
            RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS_YS,
            RecipeStatusConstant.WAIT_SEND, RecipeStatusConstant.IN_SEND)), "0"),
    /**
     * 处方已完成
     * 已下载, 已完成
     */
    Recipe_Completed(new ArrayList<>(Arrays.asList(RecipeStatusConstant.RECIPE_DOWNLOADED,
            RecipeStatusConstant.FINISH)), "1"),
    /**
     * 处方处理失败
     * 未支付, 未处理, 药师未审核通过, 医生已撤销, 取药失败
     * date 20191016
     * tab 列表添加展示已删除，已撤销，同步his失败状态的处方
     * 将同步his失败状态，已删除设置为红色
     */
    Recipe_Fail(new ArrayList<>(Arrays.asList(RecipeStatusConstant.NO_PAY,
                    RecipeStatusConstant.NO_OPERATOR, RecipeStatusConstant.CHECK_NOT_PASS_YS,
            RecipeStatusConstant.REVOKE, RecipeStatusConstant.RECIPE_FAIL,
            RecipeStatusConstant.NO_DRUG, RecipeStatusConstant.DELETE, RecipeStatusConstant.HIS_FAIL)), "2");
    /**
     * 处方模式
     */
    private List<Integer> recipeStatusList;
    /**
     * 购药方式
     */
    private String showType;

    RecipeTipesColorTypeEnum(List<Integer> recipeStatusList, String showType) {
        this.recipeStatusList = recipeStatusList;
        this.showType = showType;
    }

    public static RecipeTipesColorTypeEnum fromRecipeStatus(Integer recipeStatus){
        for(RecipeTipesColorTypeEnum ep : RecipeTipesColorTypeEnum.values()){
            if(ep.getRecipeStatusList().contains(recipeStatus)){
                return ep;
            }
        }
        return null;
    }

    public List<Integer> getRecipeStatusList() {
        return recipeStatusList;
    }

    public void setRecipeStatusList(List<Integer> recipeStatusList) {
        this.recipeStatusList = recipeStatusList;
    }

    public String getShowType() {
        return showType;
    }

    public void setShowType(String showType) {
        this.showType = showType;
    }
}