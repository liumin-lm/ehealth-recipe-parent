package recipe.bean;





import com.ngari.recipe.entity.Recipedetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审核通过HIS返回数据对象
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2016/6/6.
 */
public class RecipeCheckPassResult {
    /**
     * BASE平台处方ID
     */
    private Integer recipeId;

    /**
     * HIS平台处方ID
     */
    private String recipeCode;

    /**
     * 病人医院病历号
     */
    private String patientID;

    /**
     * 病人挂号序号
     */
    private String registerID;

    /**
     * 处方总金额
     */
    private BigDecimal totalMoney;

    /**
     * 处方详情数据
     */
    private List<Recipedetail> detailList;

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public List<Recipedetail> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<Recipedetail> detailList) {
        this.detailList = detailList;
    }

    public String getRegisterID() {
        return registerID;
    }

    public void setRegisterID(String registerID) {
        this.registerID = registerID;
    }
}
