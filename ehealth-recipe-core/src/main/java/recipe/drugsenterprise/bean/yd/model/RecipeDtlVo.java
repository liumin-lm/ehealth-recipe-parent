package recipe.drugsenterprise.bean.yd.model;

import java.math.BigDecimal;

public class RecipeDtlVo {

    private String recipedtlno;
    private String drugcode;
    private String drugname;
    private String prodarea;
    private String factoryname;
    private String drugspec;
    private String freqname;
    private String sustaineddays;
    private String classtypeno;
    private String classtypename;
    private BigDecimal quantity;
    private String minunit;
    private String usagename;
    private String dosage;
    private String dosageunit;
    private BigDecimal unitprice;
    private String measurement;
    private String measurementunit;
    private String drugunit;

    private String doctor_note;

    public String getDoctor_note() {
        return doctor_note;
    }

    public void setDoctor_note(String doctor_note) {
        this.doctor_note = doctor_note;
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

	public BigDecimal getUnitprice() {
		return unitprice;
	}

	public void setUnitprice(BigDecimal unitprice) {
		this.unitprice = unitprice;
	}

	public String getRecipedtlno() {
        return recipedtlno;
    }

    public void setRecipedtlno(String recipedtlno) {
        this.recipedtlno = recipedtlno;
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

    public String getSustaineddays() {
        return sustaineddays;
    }

    public void setSustaineddays(String sustaineddays) {
        this.sustaineddays = sustaineddays;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getMinunit() {
        return minunit;
    }

    public void setMinunit(String minunit) {
        this.minunit = minunit;
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

    public String getDrugunit() {
        return drugunit;
    }

    public void setDrugunit(String drugunit) {
        this.drugunit = drugunit;
    }
}
