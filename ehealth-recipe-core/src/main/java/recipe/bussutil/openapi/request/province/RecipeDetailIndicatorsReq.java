package recipe.bussutil.openapi.request.province;

import java.io.Serializable;

public class RecipeDetailIndicatorsReq  implements Serializable {
    private static final long serialVersionUID = -2198296880051047781L;
    private String  drcode;//	药品代码
    private String  drname;//	药品名称
    private String  drmodel;//	药品规格
    private Integer pack;//	药品包装
    private String  packUnit;//	药品包装单位
    private String  drugManf;//	药品产地名称
    private String  admission;//	药品用法
    private String  frequency;//	用品使用频度
    private String  dosage;//	每次剂量
    private String  drunit;//	剂量单位
    private String  dosageTotal;//	药品药量
    private Integer useDays;//	用药天数
    private String  remark;//	备注

    public String getDrcode() {
        return drcode;
    }

    public void setDrcode(String drcode) {
        this.drcode = drcode;
    }

    public String getDrname() {
        return drname;
    }

    public void setDrname(String drname) {
        this.drname = drname;
    }

    public String getDrmodel() {
        return drmodel;
    }

    public void setDrmodel(String drmodel) {
        this.drmodel = drmodel;
    }

    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    public String getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }

    public String getDrugManf() {
        return drugManf;
    }

    public void setDrugManf(String drugManf) {
        this.drugManf = drugManf;
    }

    public String getAdmission() {
        return admission;
    }

    public void setAdmission(String admission) {
        this.admission = admission;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDrunit() {
        return drunit;
    }

    public void setDrunit(String drunit) {
        this.drunit = drunit;
    }

    public String getDosageTotal() {
        return dosageTotal;
    }

    public void setDosageTotal(String dosageTotal) {
        this.dosageTotal = dosageTotal;
    }

    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
