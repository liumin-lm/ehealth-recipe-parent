package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

public class RecipeVerificationIndicatorsReq implements Serializable {

    private static final long serialVersionUID = 2093417445662161811L;

    @NotNull
    private String  unitID;
    @NotNull
    private String  organID;//	机构唯一号
    private String  organName;
    private String  hosCode;//	院区代码
    private String  hosName;//	院区名称
    @NotNull
    private String  bussID;//	互联网医院复诊记录Id
    @NotNull
    private String  recipeUniqueID;//	互联网医院处方唯一号
    @NotNull
    private String  recipeID;//	互联网医院处方号

    @NotNull
    private String  deliveryType;//	处方配送方式 0医院取药 1物流配送 2药店取药
    @NotNull
    private Date    verificationTime;//物流配送、医院药房发药或药店自购时间
    private String  deliveryFirm;//配送药企、医院药房名称、药店名称
    private Date    deliverySTDate;//deliveryType=1是需要传入
    private Date    deliveryENDDate;//deliveryType=1是需要传入
    private String  deliveryFee;
    private Double  totalFee;
    private String  isPay;
    private String  tradeNo;//在线支付流水号、医院结算单号、药店结算单号
    private Date    updateTime;

    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getHosCode() {
        return hosCode;
    }

    public void setHosCode(String hosCode) {
        this.hosCode = hosCode;
    }

    public String getHosName() {
        return hosName;
    }

    public void setHosName(String hosName) {
        this.hosName = hosName;
    }

    public String getBussID() {
        return bussID;
    }

    public void setBussID(String bussID) {
        this.bussID = bussID;
    }

    public String getRecipeUniqueID() {
        return recipeUniqueID;
    }

    public void setRecipeUniqueID(String recipeUniqueID) {
        this.recipeUniqueID = recipeUniqueID;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public Date getVerificationTime() {
        return this.verificationTime;
    }

    public void setVerificationTime(Date verificationTime) {
        this.verificationTime = verificationTime;
    }

    public String getDeliveryFirm() {
        return deliveryFirm;
    }

    public void setDeliveryFirm(String deliveryFirm) {
        this.deliveryFirm = deliveryFirm;
    }

    public Date getDeliverySTDate() {
        return deliverySTDate;
    }

    public void setDeliverySTDate(Date deliverySTDate) {
        this.deliverySTDate = deliverySTDate;
    }

    public Date getDeliveryENDDate() {
        return deliveryENDDate;
    }

    public void setDeliveryENDDate(Date deliveryENDDate) {
        this.deliveryENDDate = deliveryENDDate;
    }

    public String getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(String deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    public String getIsPay() {
        return isPay;
    }

    public void setIsPay(String isPay) {
        this.isPay = isPay;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
