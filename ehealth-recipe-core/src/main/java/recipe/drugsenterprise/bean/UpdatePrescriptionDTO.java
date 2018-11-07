package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企更新处方信息标准数据
 * @version： 1.0
 */
public class UpdatePrescriptionDTO implements Serializable {

    private static final long serialVersionUID = 8963421766834474406L;

    @Verify(desc = "组织机构编码")
    private String organId;

    @Verify(isNotNull = false, desc = "平台机构内码", isInt = true)
    private String clinicOrgan;

    @Verify(desc = "电子处方单号")
    private String recipeCode;

    @Verify(desc = "药企标识", maxLength = 20)
    private String account;

    @Verify(desc = "时间", isDate = true)
    private String date;

    @Verify(isNotNull = false, desc = "处方总价", isMoney = true)
    private String recipeFee;

    @Verify(isNotNull = false, desc = "药品详情")
    private List<StandardRecipeDetailDTO> details;

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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(String recipeFee) {
        this.recipeFee = recipeFee;
    }

    public List<StandardRecipeDetailDTO> getDetails() {
        return details;
    }

    public void setDetails(List<StandardRecipeDetailDTO> details) {
        this.details = details;
    }
}
