package com.ngari.recipe.hisprescription.model;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/20
 * @description： HIS处方状态更新请求对象
 * @version： 1.0
 */
public class HospitalStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 8940816216542982369L;

    @Verify(desc = "医院的处方编号")
    private String recipeCode;

    @Verify(isNotNull = false, desc = "纳里平台配置的机构 ID", isInt = true)
    private String clinicOrgan;

    @Verify(desc = "组织机构编码", maxLength = 30)
    private String organId;

    @Verify(desc = "处方状态", isInt = true)
    private String status;

    @Verify(isNotNull = false,desc = "平台处方ID")
    private String platRecipeID;

    @Verify(isNotNull = false,desc = "是否更新掉his返回的处方号")
    private Boolean updateRecipeCodeFlag;

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlatRecipeID() {
        return platRecipeID;
    }

    public void setPlatRecipeID(String platRecipeID) {
        this.platRecipeID = platRecipeID;
    }

    public Boolean getUpdateRecipeCodeFlag() {
        return updateRecipeCodeFlag;
    }

    public void setUpdateRecipeCodeFlag(Boolean updateRecipeCodeFlag) {
        this.updateRecipeCodeFlag = updateRecipeCodeFlag;
    }
}
