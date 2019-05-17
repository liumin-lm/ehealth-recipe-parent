package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:36
 */
@Schema
public class DrugParamDTO implements Serializable{

    private static final long serialVersionUID = 5673401636633685329L;

    @Verify(isNotNull = true, desc = "药品ID", maxLength = 20)
    private String drugId;

    @Verify(isNotNull = false, desc = "⽤于复诊开⽅算法匹配", maxLength = 20)
    private String spuId;

    @Verify(isNotNull = true, desc = "药品通⽤名", maxLength = 64)
    private String drugCommonName;

    @Verify(isNotNull = true, desc = "药品规格", maxLength = 64)
    private String spec;

    @Verify(isNotNull = true, desc = "开⽅数量")
    private Integer num;

    @Verify(isNotNull = true, desc = "开具单位", maxLength = 10)
    private String doseUnit;

    @Verify(isNotNull = true, desc = "⽤法", maxLength = 20)
    private String doseUsage;

    @Verify(isNotNull = true, desc = "⽤量", maxLength = 64)
    private String doseUsageAdvice;

    @Verify(isNotNull = false, desc = "商品名", maxLength = 64)
    private String drugName;

    @Verify(isNotNull = false, desc = "⽣产⼚家", maxLength = 64)
    private String manufacturer;

    @Verify(isNotNull = false, desc = "是否医保")
    private Boolean medicalInsurance;

    public String getDrugId() {
        return drugId;
    }

    public void setDrugId(String drugId) {
        this.drugId = drugId;
    }

    public String getSpuId() {
        return spuId;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
    }

    public String getDrugCommonName() {
        return drugCommonName;
    }

    public void setDrugCommonName(String drugCommonName) {
        this.drugCommonName = drugCommonName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }

    public String getDoseUsage() {
        return doseUsage;
    }

    public void setDoseUsage(String doseUsage) {
        this.doseUsage = doseUsage;
    }

    public String getDoseUsageAdvice() {
        return doseUsageAdvice;
    }

    public void setDoseUsageAdvice(String doseUsageAdvice) {
        this.doseUsageAdvice = doseUsageAdvice;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Boolean getMedicalInsurance() {
        return medicalInsurance;
    }

    public void setMedicalInsurance(Boolean medicalInsurance) {
        this.medicalInsurance = medicalInsurance;
    }
}
