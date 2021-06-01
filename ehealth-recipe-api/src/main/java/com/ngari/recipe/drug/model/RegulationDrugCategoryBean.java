package com.ngari.recipe.drug.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 互联网医院药品目录
 */
public class RegulationDrugCategoryBean implements java.io.Serializable{

    private static final long serialVersionUID = -3090605406715918633L;

    private String  unitID;
    private String  organID;
    private String  organName;
    private String platDrugCode;  //监管平台药品ID
    private String platDrugName;  //监管平台药品通用名
    private String hospDrugCode;  //医院药品ID
    private String hospDrugName;  //医院药品通用名
    private String hospTradeName;  //医院药品商品名
    private String hospDrugAlias;  //医院药品别名
    private String hospDrugPacking;  //医院药品包装规格
    private String hospDrugManuf;  //医院药品产地名称
    private String useFlag;  //医院有效标志
    private String drugClass;  //药品分类

    private Date updateTime;//最后更新时间
    private Date createTime; //数据库记录创建时间
    private Date lastModify; //数据库记录最后修改时间

    private String checkRes;//指标结果 null 没执行  1 通过 2不通过
    private String warn; //是否警告
    private String error;//是否违规
    private BigDecimal drugPrice; //医院药品单价

    private String medicalDrugCode;  //项目标准代码
    private String licenseNumber;  //批准文号
    private String drugForm;  //剂型名称
    private String drugFormCode;  //剂型代码
    private String hospitalPreparation;  //院内制剂标志：0.非自制药品；1.自制药品。
    private String baseDrug;  //是否基药1：国基药；2：上海增补基药；0：非前述两种
    private String kssFlag;  //抗生素标识1：抗生素；0：非抗生素
    private String dmjfFlag;  //毒麻精放标识1：是；0：否
    private String noteDesc;  //备注说明
    private String modifyFlag; //:1.正常;2.撤销


    public BigDecimal getDrugPrice() {
        return drugPrice;
    }

    public void setDrugPrice(BigDecimal drugPrice) {
        this.drugPrice = drugPrice;
    }
    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

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

    public String getPlatDrugCode() {
        return platDrugCode;
    }

    public void setPlatDrugCode(String platDrugCode) {
        this.platDrugCode = platDrugCode;
    }

    public String getPlatDrugName() {
        return platDrugName;
    }

    public void setPlatDrugName(String platDrugName) {
        this.platDrugName = platDrugName;
    }

    public String getHospDrugCode() {
        return hospDrugCode;
    }

    public void setHospDrugCode(String hospDrugCode) {
        this.hospDrugCode = hospDrugCode;
    }

    public String getHospDrugName() {
        return hospDrugName;
    }

    public void setHospDrugName(String hospDrugName) {
        this.hospDrugName = hospDrugName;
    }

    public String getHospTradeName() {
        return hospTradeName;
    }

    public void setHospTradeName(String hospTradeName) {
        this.hospTradeName = hospTradeName;
    }

    public String getHospDrugAlias() {
        return hospDrugAlias;
    }

    public void setHospDrugAlias(String hospDrugAlias) {
        this.hospDrugAlias = hospDrugAlias;
    }

    public String getHospDrugPacking() {
        return hospDrugPacking;
    }

    public void setHospDrugPacking(String hospDrugPacking) {
        this.hospDrugPacking = hospDrugPacking;
    }

    public String getHospDrugManuf() {
        return hospDrugManuf;
    }

    public void setHospDrugManuf(String hospDrugManuf) {
        this.hospDrugManuf = hospDrugManuf;
    }

    public String getUseFlag() {
        return useFlag;
    }

    public void setUseFlag(String useFlag) {
        this.useFlag = useFlag;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getCheckRes() {
        return checkRes;
    }

    public void setCheckRes(String checkRes) {
        this.checkRes = checkRes;
    }

    public String getWarn() {
        return warn;
    }

    public void setWarn(String warn) {
        this.warn = warn;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
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

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    @Override
    public String toString() {
        return "RegulationDrugCategoryBean{" +
                "unitID='" + unitID + '\'' +
                ", organID='" + organID + '\'' +
                ", organName='" + organName + '\'' +
                ", platDrugCode='" + platDrugCode + '\'' +
                ", platDrugName='" + platDrugName + '\'' +
                ", hospDrugCode='" + hospDrugCode + '\'' +
                ", hospDrugName='" + hospDrugName + '\'' +
                ", hospTradeName='" + hospTradeName + '\'' +
                ", hospDrugAlias='" + hospDrugAlias + '\'' +
                ", hospDrugPacking='" + hospDrugPacking + '\'' +
                ", hospDrugManuf='" + hospDrugManuf + '\'' +
                ", useFlag='" + useFlag + '\'' +
                ", drugClass='" + drugClass + '\'' +
                ", updateTime=" + updateTime +
                ", createTime=" + createTime +
                ", lastModify=" + lastModify +
                ", checkRes='" + checkRes + '\'' +
                ", warn='" + warn + '\'' +
                ", error='" + error + '\'' +
                ", drugPrice=" + drugPrice +
                ", medicalDrugCode='" + medicalDrugCode + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", drugForm='" + drugForm + '\'' +
                ", drugFormCode='" + drugFormCode + '\'' +
                ", hospitalPreparation='" + hospitalPreparation + '\'' +
                ", baseDrug='" + baseDrug + '\'' +
                ", kssFlag='" + kssFlag + '\'' +
                ", dmjfFlag='" + dmjfFlag + '\'' +
                ", noteDesc='" + noteDesc + '\'' +
                ", modifyFlag='" + modifyFlag + '\'' +
                '}';
    }
}
