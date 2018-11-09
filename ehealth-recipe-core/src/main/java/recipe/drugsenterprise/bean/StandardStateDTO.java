package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/10/29
 * @description： 处方状态改变DTO
 * @version： 1.0
 */
public class StandardStateDTO implements Serializable{

    private static final long serialVersionUID = -8948834094204713613L;

    @Verify(desc = "药企标识", maxLength = 20)
    private String account;

    @Verify(desc = "处方状态", isInt = true)
    private String status;

    @Verify(isNotNull = false, desc = "其他信息", maxLength = 100)
    private String reason;

    @Verify(desc = "组织机构编码")
    private String organId;

    @Verify(isNotNull = false, desc = "平台机构内码", isInt = true)
    private String clinicOrgan;

    @Verify(desc = "电子处方单号")
    private String recipeCode;

    @Verify(desc = "时间", isDate = true)
    private String date;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
