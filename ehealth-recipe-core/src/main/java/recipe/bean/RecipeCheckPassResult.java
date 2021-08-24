package recipe.bean;


import com.ngari.recipe.entity.Recipedetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审核通过HIS返回数据对象
 * company: ngarihealth
 *
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

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;

    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

    /**
     * his处方付费序号合集
     */
    private String recipeCostNumber;

    /**
     * 取药窗口
     */
    private String pharmNo;

    /**
     * 诊断序号
     */
    private String diseaseSerial;

    public String getDiseaseSerial() {
        return diseaseSerial;
    }

    public void setDiseaseSerial(String diseaseSerial) {
        this.diseaseSerial = diseaseSerial;
    }

    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }


    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
    }

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

    public String getMedicalType() {
        return medicalType;
    }

    public void setMedicalType(String medicalType) {
        this.medicalType = medicalType;
    }

    public String getMedicalTypeText() {
        return medicalTypeText;
    }

    public void setMedicalTypeText(String medicalTypeText) {
        this.medicalTypeText = medicalTypeText;
    }
}
