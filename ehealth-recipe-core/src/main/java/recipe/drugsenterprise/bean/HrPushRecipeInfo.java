package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:17
 */
public class HrPushRecipeInfo implements Serializable{
    private static final long serialVersionUID = -1739448022029719876L;

    private String OrderId;
    private String StoreId;
    private String Description;
    private Boolean IsNeedInvoice;
    private int PickMode;
    private int SettleMode;
    private int PayFlag;
    private Double Amount;
    private ReceiveAddress ReceiveAddress;
    private List<HrPatient> Patients;
    private List<HrPrescr> Prescrs;
    private List<HrDrugDetail> Details;

    public String getOrderId() {
        return OrderId;
    }

    public void setOrderId(String orderId) {
        OrderId = orderId;
    }

    public String getStoreId() {
        return StoreId;
    }

    public void setStoreId(String storeId) {
        StoreId = storeId;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public boolean getIsNeedInvoice() {
        return IsNeedInvoice;
    }

    public void setIsNeedInvoice(boolean isNeedInvoice) {
        IsNeedInvoice = isNeedInvoice;
    }

    public Integer getPickMode() {
        return PickMode;
    }

    public void setPickMode(Integer pickMode) {
        PickMode = pickMode;
    }

    public Integer getSettleMode() {
        return SettleMode;
    }

    public void setSettleMode(Integer settleMode) {
        SettleMode = settleMode;
    }

    public int getPayFlag() {
        return PayFlag;
    }

    public void setPayFlag(int payFlag) {
        PayFlag = payFlag;
    }

    public Double getAmount() {
        return Amount;
    }

    public void setAmount(Double amount) {
        Amount = amount;
    }

    public recipe.drugsenterprise.bean.ReceiveAddress getReceiveAddress() {
        return ReceiveAddress;
    }

    public void setReceiveAddress(recipe.drugsenterprise.bean.ReceiveAddress receiveAddress) {
        ReceiveAddress = receiveAddress;
    }

    public List<HrPatient> getPatients() {
        return Patients;
    }

    public void setPatients(List<HrPatient> patients) {
        Patients = patients;
    }

    public List<HrPrescr> getPrescrs() {
        return Prescrs;
    }

    public void setPrescrs(List<HrPrescr> prescrs) {
        Prescrs = prescrs;
    }

    public List<HrDrugDetail> getDetails() {
        return Details;
    }

    public void setDetails(List<HrDrugDetail> details) {
        Details = details;
    }
}
