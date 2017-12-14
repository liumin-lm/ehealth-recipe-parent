package recipe.bean;

import java.io.Serializable;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/5/12.
 */
public class HospitalRecipeDetailBean implements Serializable {

    private static final long serialVersionUID = -4237631034878769037L;

    /**
     * HIS药品医嘱ID
     */
    private String hisOrderID;

    /**
     * 药品代码
     */
    private String drcode;

    /**
     * 药品名称
     */
    private String drname;

    /**
     * 组号
     */
    private String drugGroup;

    /**
     * 药品规格
     */
    private String drmodel;

    /**
     * 药品包装
     */
    private String pack;

    /**
     * 药品包装单位
     */
    private String packUnit;

    /**
     * 药品产地代码
     */
    private String manfcode;

    /**
     * 药品用法
     */
    private String admission;

    /**
     * 用品使用频度
     */
    private String frequency;

    /**
     * 每次剂量
     */
    private String dosage;

    /**
     * 规格单位
     */
    private String drunit;

    /**
     * 剂量单位
     */
    private String dosageUnit;

    /**
     * 药品总剂量
     */
    private String dosageTotal;

    /**
     * 药品使用天数
     */
    private String useDays;

    /**
     * 备注
     */
    private String remark;

    public HospitalRecipeDetailBean(){}

    public String getHisOrderID() {
        return hisOrderID;
    }

    public void setHisOrderID(String hisOrderID) {
        this.hisOrderID = hisOrderID;
    }

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

    public String getDrugGroup() {
        return drugGroup;
    }

    public void setDrugGroup(String drugGroup) {
        this.drugGroup = drugGroup;
    }

    public String getDrmodel() {
        return drmodel;
    }

    public void setDrmodel(String drmodel) {
        this.drmodel = drmodel;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public String getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }

    public String getManfcode() {
        return manfcode;
    }

    public void setManfcode(String manfcode) {
        this.manfcode = manfcode;
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

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getDosageTotal() {
        return dosageTotal;
    }

    public void setDosageTotal(String dosageTotal) {
        this.dosageTotal = dosageTotal;
    }

    public String getUseDays() {
        return useDays;
    }

    public void setUseDays(String useDays) {
        this.useDays = useDays;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
