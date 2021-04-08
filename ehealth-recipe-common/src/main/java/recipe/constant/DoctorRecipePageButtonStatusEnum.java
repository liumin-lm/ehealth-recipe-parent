package recipe.constant;

import java.util.Arrays;
import java.util.List;

/**
 * created by shiyuping on 2020/7/23
 * 医生端处方详情页面按钮
 */
public enum DoctorRecipePageButtonStatusEnum {
    /**
     * 继续签名--在 [ 待审核、待处理 ] 的处方单详情页显示 [ 继续开方 ] 按钮
     */
    continueOpenRecipeFlag("continueOpenRecipeFlag", Arrays.asList(RecipeStatusConstant.CHECKING_HOS, RecipeStatusConstant.READY_CHECK_YS, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.SIGN_ING_CODE_DOC)),

    /**
     * 重新签名--医生签名失败时
     */
    retrySignFlag("retrySignFlag", Arrays.asList(RecipeStatusConstant.SIGN_ERROR_CODE_DOC)),
    /**
     * 重新开具--药师平台审核未通过 15
     */
    recipeOpenRecipeFlag("recipeOpenRecipeFlag", Arrays.asList(RecipeStatusConstant.CHECK_NOT_PASS_YS)),
    /**
     * 重试--HIS写入失败 11/处方医保上传失败 19
     */
    retryUploadRecipeFlag("retryUploadRecipeFlag", Arrays.asList(RecipeStatusConstant.HIS_FAIL,
            RecipeStatusConstant.RECIPE_MEDICAL_FAIL)),
    /**
     * 重新编辑--His写入失败 11
     */
    reOpenRecipeEditor("reOpenRecipeEditor",Arrays.asList(RecipeStatusConstant.HIS_FAIL)),
    /**续方--
     * 已完成 6
     * 未处理  14
     * 未支付  13
     * 失败  17
     * 已撤销  9
     * 超过3天未取药 12*/
    continueRecipeFlag("continueRecipeFlag",
            Arrays.asList(RecipeStatusConstant.FINISH,
            RecipeStatusConstant.NO_OPERATOR,
            RecipeStatusConstant.NO_PAY,
            RecipeStatusConstant.RECIPE_FAIL,
            RecipeStatusConstant.REVOKE,
            RecipeStatusConstant.NO_DRUG));


    private String buttonName;
    private List<Integer> statusList;

    DoctorRecipePageButtonStatusEnum(String buttonName, List<Integer> statusList) {
        this.buttonName = buttonName;
        this.statusList = statusList;
    }

    public String getButtonName() {
        return buttonName;
    }

    public void setButtonName(String buttonName) {
        this.buttonName = buttonName;
    }

    public List<Integer> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<Integer> statusList) {
        this.statusList = statusList;
    }
}
