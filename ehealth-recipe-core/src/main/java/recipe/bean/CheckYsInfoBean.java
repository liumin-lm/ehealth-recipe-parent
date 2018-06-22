package recipe.bean;

import com.ngari.recipe.entity.RecipeCheckDetail;

import java.util.List;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/6/14
 */
public class CheckYsInfoBean {

    private boolean rs;

    private Integer recipeId;

    private Integer checkResult;

    private String checkFailMemo;

    private Integer checkDoctorId;

    private List<RecipeCheckDetail> checkDetailList;

    public boolean isRs() {
        return rs;
    }

    public void setRs(boolean rs) {
        this.rs = rs;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(Integer checkResult) {
        this.checkResult = checkResult;
    }

    public String getCheckFailMemo() {
        return checkFailMemo;
    }

    public void setCheckFailMemo(String checkFailMemo) {
        this.checkFailMemo = checkFailMemo;
    }

    public List<RecipeCheckDetail> getCheckDetailList() {
        return checkDetailList;
    }

    public void setCheckDetailList(List<RecipeCheckDetail> checkDetailList) {
        this.checkDetailList = checkDetailList;
    }

    public Integer getCheckDoctorId() {
        return checkDoctorId;
    }

    public void setCheckDoctorId(Integer checkDoctorId) {
        this.checkDoctorId = checkDoctorId;
    }
}
