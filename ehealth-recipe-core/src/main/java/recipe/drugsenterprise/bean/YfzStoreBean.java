package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\25 0025 10:44
 */
public class YfzStoreBean implements Serializable {
    private static final long serialVersionUID = 8815764246047135200L;

    private String id;
    private String parentId;
    private String number;
    private String linkageId;
    private String integralValue;
    private String name;
    private String drugstoreType;
    private String charge;
    private String telephone;
    private String fax;
    private String mobile;
    private String contactName;
    private String contactMobile;
    private String contactTelephone;
    private String signPassword;
    private String dimensionalPic;
    private String headPic;
    private String provinceId;
    private String cityId;
    private String regionId;
    private String address;
    private String manageAddress;
    private String status;
    private String createdDate;
    private String statusName;
    private String isInjection;
    private boolean isPhysician;
    private boolean isPharmacist;
    private String isTelephoneNotice;
    private String noTelephoneNotice;
    private String signing;
    private String deductionRate;
    private String searchCode;
    private String prescriptionSmsMobile;
    private String dispatchingExpenses;
    private String position;
    private String casherId;
    private boolean isSocialReimburse;
    private String memo;
    private String version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLinkageId() {
        return linkageId;
    }

    public void setLinkageId(String linkageId) {
        this.linkageId = linkageId;
    }

    public String getIntegralValue() {
        return integralValue;
    }

    public void setIntegralValue(String integralValue) {
        this.integralValue = integralValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDrugstoreType() {
        return drugstoreType;
    }

    public void setDrugstoreType(String drugstoreType) {
        this.drugstoreType = drugstoreType;
    }

    public String getCharge() {
        return charge;
    }

    public void setCharge(String charge) {
        this.charge = charge;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public void setContactMobile(String contactMobile) {
        this.contactMobile = contactMobile;
    }

    public String getContactTelephone() {
        return contactTelephone;
    }

    public void setContactTelephone(String contactTelephone) {
        this.contactTelephone = contactTelephone;
    }

    public String getSignPassword() {
        return signPassword;
    }

    public void setSignPassword(String signPassword) {
        this.signPassword = signPassword;
    }

    public String getDimensionalPic() {
        return dimensionalPic;
    }

    public void setDimensionalPic(String dimensionalPic) {
        this.dimensionalPic = dimensionalPic;
    }

    public String getHeadPic() {
        return headPic;
    }

    public void setHeadPic(String headPic) {
        this.headPic = headPic;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getManageAddress() {
        return manageAddress;
    }

    public void setManageAddress(String manageAddress) {
        this.manageAddress = manageAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getIsInjection() {
        return isInjection;
    }

    public void setIsInjection(String isInjection) {
        this.isInjection = isInjection;
    }

    public boolean isPhysician() {
        return isPhysician;
    }

    public void setPhysician(boolean physician) {
        isPhysician = physician;
    }

    public boolean isPharmacist() {
        return isPharmacist;
    }

    public void setPharmacist(boolean pharmacist) {
        isPharmacist = pharmacist;
    }

    public String getIsTelephoneNotice() {
        return isTelephoneNotice;
    }

    public void setIsTelephoneNotice(String isTelephoneNotice) {
        this.isTelephoneNotice = isTelephoneNotice;
    }

    public String getNoTelephoneNotice() {
        return noTelephoneNotice;
    }

    public void setNoTelephoneNotice(String noTelephoneNotice) {
        this.noTelephoneNotice = noTelephoneNotice;
    }

    public String getSigning() {
        return signing;
    }

    public void setSigning(String signing) {
        this.signing = signing;
    }

    public String getDeductionRate() {
        return deductionRate;
    }

    public void setDeductionRate(String deductionRate) {
        this.deductionRate = deductionRate;
    }

    public String getSearchCode() {
        return searchCode;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
    }

    public String getPrescriptionSmsMobile() {
        return prescriptionSmsMobile;
    }

    public void setPrescriptionSmsMobile(String prescriptionSmsMobile) {
        this.prescriptionSmsMobile = prescriptionSmsMobile;
    }

    public String getDispatchingExpenses() {
        return dispatchingExpenses;
    }

    public void setDispatchingExpenses(String dispatchingExpenses) {
        this.dispatchingExpenses = dispatchingExpenses;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getCasherId() {
        return casherId;
    }

    public void setCasherId(String casherId) {
        this.casherId = casherId;
    }

    public boolean isSocialReimburse() {
        return isSocialReimburse;
    }

    public void setSocialReimburse(boolean socialReimburse) {
        isSocialReimburse = socialReimburse;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
