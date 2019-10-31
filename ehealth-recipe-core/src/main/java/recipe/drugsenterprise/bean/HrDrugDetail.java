package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:54
 */
public class HrDrugDetail implements Serializable {
    private static final long serialVersionUID = 439863914111296489L;

    private String ProductId;
    private String MedicineName;
    private String CommonName;
    private String Specs;
    private String Producer;
    private int Quantity;
    private Double Price;
    private String Unit;
    private Double Amount;
    private String Description;

    public String getProductId() {
        return ProductId;
    }

    public void setProductId(String productId) {
        ProductId = productId;
    }

    public String getMedicineName() {
        return MedicineName;
    }

    public void setMedicineName(String medicineName) {
        MedicineName = medicineName;
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

    public String getProducer() {
        return Producer;
    }

    public void setProducer(String producer) {
        Producer = producer;
    }

    public int getQuantity() {
        return Quantity;
    }

    public void setQuantity(int quantity) {
        Quantity = quantity;
    }

    public Double getPrice() {
        return Price;
    }

    public void setPrice(Double price) {
        Price = price;
    }

    public String getUnit() {
        return Unit;
    }

    public void setUnit(String unit) {
        Unit = unit;
    }

    public Double getAmount() {
        return Amount;
    }

    public void setAmount(Double amount) {
        Amount = amount;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }
}
