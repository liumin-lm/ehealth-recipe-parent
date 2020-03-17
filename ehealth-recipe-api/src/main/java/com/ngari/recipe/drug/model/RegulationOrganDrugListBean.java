package com.ngari.recipe.drug.model;

/**
 * 互联网医院药品目录
 */
public class RegulationOrganDrugListBean implements java.io.Serializable{

    private static final long serialVersionUID = 4094560187613498732L;

    private String  organID;

    private String  organName;

    private String useFlag;  //医院有效标志

    private String hospDrugName;  //医院药品通用名

    private String medicalDrugFormCode;  //剂型编码

    private String approvalNumber;  //批准文号

    private String drugForm;  //剂型名称

    private String hospitalPreparation;  //院内制剂标志：0.非自制药品；1.自制药品。

    private String baseDrug;  //是否基药1：国基药；2：上海增补基药；0：非前述两种

    private String kssFlag;  //抗生素标识1：抗生素；0：非抗生素

    private String dmjfFlag;  //毒麻精放标识1：是；0：否

    private String noteDesc;  //备注说明

    private String modifyFlag; //:1.正常;2.撤销

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getUseFlag() {
        return useFlag;
    }

    public void setUseFlag(String useFlag) {
        this.useFlag = useFlag;
    }

    public String getHospDrugName() {
        return hospDrugName;
    }

    public void setHospDrugName(String hospDrugName) {
        this.hospDrugName = hospDrugName;
    }

    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public String getHospitalPreparation() {
        return hospitalPreparation;
    }

    public void setHospitalPreparation(String hospitalPreparation) {
        this.hospitalPreparation = hospitalPreparation;
    }

    public String getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(String baseDrug) {
        this.baseDrug = baseDrug;
    }

    public String getKssFlag() {
        return kssFlag;
    }

    public void setKssFlag(String kssFlag) {
        this.kssFlag = kssFlag;
    }

    public String getDmjfFlag() {
        return dmjfFlag;
    }

    public void setDmjfFlag(String dmjfFlag) {
        this.dmjfFlag = dmjfFlag;
    }

    public String getNoteDesc() {
        return noteDesc;
    }

    public void setNoteDesc(String noteDesc) {
        this.noteDesc = noteDesc;
    }

    public String getModifyFlag() {
        return modifyFlag;
    }

    public void setModifyFlag(String modifyFlag) {
        this.modifyFlag = modifyFlag;
    }
}
