package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @Description: 对接上海六院易复诊同步药品中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzHospDrugDto  implements Serializable {
    /**
     * 医院药品ID
     */
    private String hospDrugId;
    /**
     * 价格
     */
    private String hospDrugPrice;
    /**
     * 通用名
     */
    private String hospDrugGenericName;
    /**
     * 商品名（请尽量提供）
     * 非必填
     */
    private String hospDrugTradeName;
    /**
     * 规格
     */
    private String hospDrugSpec;
    /**
     * 剂型（请尽量提供）
     * 非必填
     */
    private String hospDrugDosage;
    /**
     * 单位（请尽量提供）
     * 非必填
     */
    private String hospDrugUnit;
    /**
     * 生产厂家
     */
    private String hospDrugCompanyName;
    /**
     * 批准文号（请尽量提供）
     * 非必填
     */
    private String hospDrugApproveNumber;

    public String getHospDrugId() {
        return hospDrugId;
    }

    public String getHospDrugPrice() {
        return hospDrugPrice;
    }

    public String getHospDrugGenericName() {
        return hospDrugGenericName;
    }

    public String getHospDrugTradeName() {
        return hospDrugTradeName;
    }

    public String getHospDrugSpec() {
        return hospDrugSpec;
    }

    public String getHospDrugDosage() {
        return hospDrugDosage;
    }

    public String getHospDrugUnit() {
        return hospDrugUnit;
    }

    public String getHospDrugCompanyName() {
        return hospDrugCompanyName;
    }

    public String getHospDrugApproveNumber() {
        return hospDrugApproveNumber;
    }

    public void setHospDrugId(String hospDrugId) {
        this.hospDrugId = hospDrugId;
    }

    public void setHospDrugPrice(String hospDrugPrice) {
        this.hospDrugPrice = hospDrugPrice;
    }

    public void setHospDrugGenericName(String hospDrugGenericName) {
        this.hospDrugGenericName = hospDrugGenericName;
    }

    public void setHospDrugTradeName(String hospDrugTradeName) {
        this.hospDrugTradeName = hospDrugTradeName;
    }

    public void setHospDrugSpec(String hospDrugSpec) {
        this.hospDrugSpec = hospDrugSpec;
    }

    public void setHospDrugDosage(String hospDrugDosage) {
        this.hospDrugDosage = hospDrugDosage;
    }

    public void setHospDrugUnit(String hospDrugUnit) {
        this.hospDrugUnit = hospDrugUnit;
    }

    public void setHospDrugCompanyName(String hospDrugCompanyName) {
        this.hospDrugCompanyName = hospDrugCompanyName;
    }

    public void setHospDrugApproveNumber(String hospDrugApproveNumber) {
        this.hospDrugApproveNumber = hospDrugApproveNumber;
    }
}
