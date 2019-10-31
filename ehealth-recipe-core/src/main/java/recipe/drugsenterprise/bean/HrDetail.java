package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:39
 */
public class HrDetail implements Serializable{
    private static final long serialVersionUID = 5575779677866678724L;

    private String ProductId;
    private String Price;
    private String Quantity;
    private String Unit;
    private String Description;
    private String DrugDoseQuantity;
    private String DrugDoseUnit;
    private String Frequency;
    private String FrequencyQuantity;
    private String FrequencyDescription;
    private String Usage;
    private String UsageDescription;
    private String PerDosageQuantity;
    private String PerDosageUnit;
    private String Days;
    private String Style;
    private String CommonName;
    private String Specs;
    private String MedicineName;
    private String Producer;

    public String getProductId() {
        return ProductId;
    }

    public void setProductId(String productId) {
        ProductId = productId;
    }

    public String getPrice() {
        return Price;
    }

    public void setPrice(String price) {
        Price = price;
    }

    public String getQuantity() {
        return Quantity;
    }

    public void setQuantity(String quantity) {
        Quantity = quantity;
    }

    public String getUnit() {
        return Unit;
    }

    public void setUnit(String unit) {
        Unit = unit;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getDrugDoseQuantity() {
        return DrugDoseQuantity;
    }

    public void setDrugDoseQuantity(String drugDoseQuantity) {
        DrugDoseQuantity = drugDoseQuantity;
    }

    public String getDrugDoseUnit() {
        return DrugDoseUnit;
    }

    public void setDrugDoseUnit(String drugDoseUnit) {
        DrugDoseUnit = drugDoseUnit;
    }

    public String getFrequency() {
        return Frequency;
    }

    public void setFrequency(String frequency) {
        Frequency = frequency;
    }

    public String getFrequencyQuantity() {
        return FrequencyQuantity;
    }

    public void setFrequencyQuantity(String frequencyQuantity) {
        FrequencyQuantity = frequencyQuantity;
    }

    public String getFrequencyDescription() {
        return FrequencyDescription;
    }

    public void setFrequencyDescription(String frequencyDescription) {
        FrequencyDescription = frequencyDescription;
    }

    public String getUsage() {
        return Usage;
    }

    public void setUsage(String usage) {
        Usage = usage;
    }

    public String getUsageDescription() {
        return UsageDescription;
    }

    public void setUsageDescription(String usageDescription) {
        UsageDescription = usageDescription;
    }

    public String getPerDosageQuantity() {
        return PerDosageQuantity;
    }

    public void setPerDosageQuantity(String perDosageQuantity) {
        PerDosageQuantity = perDosageQuantity;
    }

    public String getPerDosageUnit() {
        return PerDosageUnit;
    }

    public void setPerDosageUnit(String perDosageUnit) {
        PerDosageUnit = perDosageUnit;
    }

    public String getDays() {
        return Days;
    }

    public void setDays(String days) {
        Days = days;
    }

    public String getStyle() {
        return Style;
    }

    public void setStyle(String style) {
        Style = style;
    }

    public String getCommonName() {
        return CommonName;
    }

    public void setCommonName(String commonName) {
        CommonName = commonName;
    }

    public String getSpecs() {
        return Specs;
    }

    public void setSpecs(String specs) {
        Specs = specs;
    }

    public String getMedicineName() {
        return MedicineName;
    }

    public void setMedicineName(String medicineName) {
        MedicineName = medicineName;
    }

    public String getProducer() {
        return Producer;
    }

    public void setProducer(String producer) {
        Producer = producer;
    }
}
