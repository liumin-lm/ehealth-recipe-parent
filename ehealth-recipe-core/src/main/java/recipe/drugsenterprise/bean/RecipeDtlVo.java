package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * 以大药品详情
 * @author yinsheng
 * @date 2019\12\13 0013 14:44
 */
public class RecipeDtlVo implements Serializable{
    private static final long serialVersionUID = -8690119515816857990L;

    private String recipedtlno;
    private String classtypeno;
    private String classtypename;
    private String drugcode;
    private String drugname;
    private String prodarea;
    private String factoryname;
    private String drugspec;
    private String freqname;
    private Integer sustaineddays;
    private Double quantity;
    private String drugunit;
    private Double unitprice;
    private String measurement;
    private String measurementunit;
    private String usagename;
    private String dosage;
    private String dosageunit;

    public String getRecipedtlno() {
        return recipedtlno;
    }

    public void setRecipedtlno(String recipedtlno) {
        this.recipedtlno = recipedtlno;
    }

    public String getClasstypeno() {
        return classtypeno;
    }

    public void setClasstypeno(String classtypeno) {
        this.classtypeno = classtypeno;
    }

    public String getClasstypename() {
        return classtypename;
    }

    public void setClasstypename(String classtypename) {
        this.classtypename = classtypename;
    }

    public String getDrugcode() {
        return drugcode;
    }

    public void setDrugcode(String drugcode) {
        this.drugcode = drugcode;
    }

    public String getDrugname() {
        return drugname;
    }

    public void setDrugname(String drugname) {
        this.drugname = drugname;
    }

    public String getProdarea() {
        return prodarea;
    }

    public void setProdarea(String prodarea) {
        this.prodarea = prodarea;
    }

    public String getFactoryname() {
        return factoryname;
    }

    public void setFactoryname(String factoryname) {
        this.factoryname = factoryname;
    }

    public String getDrugspec() {
        return drugspec;
    }

    public void setDrugspec(String drugspec) {
        this.drugspec = drugspec;
    }

    public String getFreqname() {
        return freqname;
    }

    public void setFreqname(String freqname) {
        this.freqname = freqname;
    }

    public Integer getSustaineddays() {
        return sustaineddays;
    }

    public void setSustaineddays(Integer sustaineddays) {
        this.sustaineddays = sustaineddays;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getDrugunit() {
        return drugunit;
    }

    public void setDrugunit(String drugunit) {
        this.drugunit = drugunit;
    }

    public Double getUnitprice() {
        return unitprice;
    }

    public void setUnitprice(Double unitprice) {
        this.unitprice = unitprice;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public String getMeasurementunit() {
        return measurementunit;
    }

    public void setMeasurementunit(String measurementunit) {
        this.measurementunit = measurementunit;
    }

    public String getUsagename() {
        return usagename;
    }

    public void setUsagename(String usagename) {
        this.usagename = usagename;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDosageunit() {
        return dosageunit;
    }

    public void setDosageunit(String dosageunit) {
        this.dosageunit = dosageunit;
    }
}
