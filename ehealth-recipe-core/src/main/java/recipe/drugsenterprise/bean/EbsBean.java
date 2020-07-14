package recipe.drugsenterprise.bean;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\4\15 0015 14:52
 */
@XmlRootElement(name="params")
public class EbsBean implements Serializable{
    private static final long serialVersionUID = 598564582027934109L;

    private String prescripNo;
    private long prescribeDate;
    private String originalPrescripNo;
    private String hospitalCode;
    private String hospitalName;
    private String department;
    private String doctorName;
    private String name;
    private int sex;
    private int age;
    private String mobile;
    private String idCard;
    private String socialSecurityCard;
    private String address;
    private int feeType;
    private BigDecimal totalAmount;
    private String diagnoseResult;
    private String receiver;
    private String receiverMobile;
    private String provinceName;
    private String cityName;
    private String districtName;
    private String shippingAddress;
    private String remark;
    private List<EbsDetail> details;

    public String getPrescripNo() {
        return prescripNo;
    }

    public void setPrescripNo(String prescripNo) {
        this.prescripNo = prescripNo;
    }

    public long getPrescribeDate() {
        return prescribeDate;
    }

    public void setPrescribeDate(long prescribeDate) {
        this.prescribeDate = prescribeDate;
    }

    public String getOriginalPrescripNo() {
        return originalPrescripNo;
    }

    public void setOriginalPrescripNo(String originalPrescripNo) {
        this.originalPrescripNo = originalPrescripNo;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getSocialSecurityCard() {
        return socialSecurityCard;
    }

    public void setSocialSecurityCard(String socialSecurityCard) {
        this.socialSecurityCard = socialSecurityCard;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getFeeType() {
        return feeType;
    }

    public void setFeeType(int feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDiagnoseResult() {
        return diagnoseResult;
    }

    public void setDiagnoseResult(String diagnoseResult) {
        this.diagnoseResult = diagnoseResult;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getReceiverMobile() {
        return receiverMobile;
    }

    public void setReceiverMobile(String receiverMobile) {
        this.receiverMobile = receiverMobile;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<EbsDetail> getDetails() {
        return details;
    }

    public void setDetails(List<EbsDetail> details) {
        this.details = details;
    }
}
