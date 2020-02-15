package recipe.drugsenterprise.bean;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
* @Description: 对接英特处方下详情药品信息中间对象
* @Author: JRK
* @Date: 2019/7/9
*/
@Schema
public class YtDrugDTO implements Serializable{
    private static final long serialVersionUID = -751031313109423553L;
    /**
     * 处方流水号
     * recipe.recipeCode
     */
    private String sourceSerialNumber;
    /**
     * 医院编码
     * recipe下ClinicOrgan对应的organCode
     */
    private String hospitalCode;
    /**
     * 处方细单流水号
     * recipedetail的id
     */
    private String itemNo;
    /**
     * 商品编码
     * 药店在线的商品编码
     * salrDrug的OrganDrugCode
     */
    private String code;
    /**
     * 数量
     * recipedetail的UseTotalDose
     */
    private Double quantity;
    /**
     * 单价
     * recipedetail的SalePrice
     */
    private Double price;
    /**
     * 金额
     * 单价*数量
     */
    private Double amount;
    /**
     * 用法
     * 非必填
     * detail的UsingRate()
     */
    private String usage;
    /**
     * 用量
     * 非必填??
     *
     */
    private String dosage;

    //detail的UsePathways
    private String peroral;

    public String getSourceSerialNumber() {
        return sourceSerialNumber;
    }

    public void setSourceSerialNumber(String sourceSerialNumber) {
        this.sourceSerialNumber = sourceSerialNumber;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getPeroral() {
        return peroral;
    }

    public void setPeroral(String peroral) {
        this.peroral = peroral;
    }

    @Override
    public String toString() {
        return "YtDrugDTO{" +
                "sourceSerialNumber='" + sourceSerialNumber + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", itemNo='" + itemNo + '\'' +
                ", code='" + code + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", amount=" + amount +
                ", usage='" + usage + '\'' +
                ", dosage='" + dosage + '\'' +
                ", peroral='" + peroral + '\'' +
                '}';
    }
}