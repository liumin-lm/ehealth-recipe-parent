package recipe.drugsenterprise.bean;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
* @Description: 对接华东处方下详情药品信息中间对象
* @Author: JRK
* @Date: 2019/7/24
*/
@Schema
public class HdDrugDTO implements Serializable{
    private static final long serialVersionUID = 4707266252754404930L;

    /**
     * 商品编码
     * 药店在线的商品编码
     * salrDrug的OrganDrugCode
     */
    private String drugCode;
    /**
     * 商品名称
     * 药店在线的商品名称
     * salrDrug的name
     */
    private String drugName;
    /**
     * 药品规格
     * 药店在线的药品规格
     * detail的drugSpec
     */
    private String specification;
    /**
     * 批准文号
     * 非必填
     */
    private String licenseNumber;
    /**
     * 药品本位码
     * 非必填
     */
    private String standardCode;
    /**
     * 药品生产厂家
     * 非必填
     */
    private String producer;
    /**
     * 开药总数
     * recipedetail的UseTotalDose
     */
    private String total;
    /**
     * 药品单价(数字）
     * recipedetail的SalePrice
     */
    private String drugFee;
    /**
     * 医保报销金额
     * 非必填
     */
    private String medicalFee;
    /**
     * 金额
     * 单价*数量
     */
    private String drugTotalFee;
    /**
     * 用药天数
     * detail的useDays
     */
    private String usingDays;

    /**
     * 用法
     * detail的UsingRate()
     */
    private String usingRate;

    /**
     * 用药方式[参考用药方式]
     * detail的usePathways
     */
    private String usePathways;
    /**
     * 药品使用备注
     * 非必填
     */
    private String memo;

    /**
     * 风险级别
     * 非必填
     */
    private String aiLevel;
    /**
     * 风险级别
     * 非必填
     */
    private String aiIssue;
    /**
     * 审核详情
     * 非必填
     */
    private String aiDetail;
    /**
     * 每次使用剂量
     * 非必填??
     *
     */
    private String useDose;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getDrugFee() {
        return drugFee;
    }

    public void setDrugFee(String drugFee) {
        this.drugFee = drugFee;
    }

    public String getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(String medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getDrugTotalFee() {
        return drugTotalFee;
    }

    public void setDrugTotalFee(String drugTotalFee) {
        this.drugTotalFee = drugTotalFee;
    }

    public String getUsingDays() {
        return usingDays;
    }

    public void setUsingDays(String usingDays) {
        this.usingDays = usingDays;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getAiLevel() {
        return aiLevel;
    }

    public void setAiLevel(String aiLevel) {
        this.aiLevel = aiLevel;
    }

    public String getAiIssue() {
        return aiIssue;
    }

    public void setAiIssue(String aiIssue) {
        this.aiIssue = aiIssue;
    }

    public String getAiDetail() {
        return aiDetail;
    }

    public void setAiDetail(String aiDetail) {
        this.aiDetail = aiDetail;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }
}