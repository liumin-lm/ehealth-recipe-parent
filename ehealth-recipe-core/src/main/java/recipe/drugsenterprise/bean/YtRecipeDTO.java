package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
* @Description: 对接处方推送的中间对象
* @Author: JRK
* @Date: 2019/7/9
*/
@Schema
public class YtRecipeDTO implements Serializable{
    private static final long serialVersionUID = 6146979557492634276L;
    /**
     * 门店机构编码
     *  药店下的code
     */
    private String orgCode;
    /**
     * 处方流水号
     *  处方标识recipecode
     */
    private String sourceSerialNumber;
    /**
     * 外配处方号
     * 非必填
     */
    private String externalNumber;
    /**
     * 是否为杭州市医保
     * 1是2否
     * 默认
     */
    private Integer hzMedicalFlag;
    /**
     * 处方日期
     * 例如："2018-10-26"
     * 处方签名时间
     */
    private String pTime;
    /**
     * 处方有效期
     * 非必填
     * 默认为1，1~3
     * 3天
     */
    private Integer validDay = 1;
    /**
     * 医院编码
     *recipe下clinicOrgan对应的OrganCode
     */
    private String hospitalCode;

    /**
     * 医院编码
     *recipe下clinicOrgan对应的OrganName
     */
    private String hospitalName;
    /**
     * 医生名称
     * recipe下doctor
     */
    private String doctorName;
    /**
     * 病人名称
     * recipe下Mpiid对应的patientName
     */
    private String patientName;
    /**
     * 性别
     * 非必填
     * 1男2女默认男
     * recipe下Mpiid对应的patientSex
     */
    private Integer sex = 1;
    /**
     * 年龄
     * recipe下Mpiid对应的patientAge
     */
    private Integer age;
    /**
     * 电话
     * 非必填
     * recipe下Mpiid对应的patientPhone
     */
    private String phone;
    /**
     * 家庭住址
     * 非必填
     * recipe下Mpiid对应的patientaddress
     */
    private String address;
    /**
     * 症状
     * recipe下Mpiid对应的patient最新病情摘要
     * lastSummary
     */
    private String symptom;
    /**
     * 费别
     * 1自费 2医保
     * 默认
     */
    private Integer costType = 1;

    //医保支付金额
    private Double fundAmount;
    /**
     * 病历号
     * recipe的patientID
     */
    private String recordNo;
    /**
     * 科别
     * recipe的departName科室名称
     */
    private String category;
    /**
     * 总金额
     * recipe.totalMoney
     */
    private Double totalAmount;
    /**
     * 运费
     * 非必填
     */
    private Double transFee;

    //药事服务费
    private Double serviceFree;

    //审方费
    private Double prescriptionChecking;

    private int giveModel;
    /**
     * 是否支付
     * 1是2否
     * 默认1
     */
    private Integer ifPay;
    /**
     * 支付方式
     * 1线上支付
     * 2货到付款
     * 非必填
     */
    private Integer payMode;
    /**
     * 收货人
     * 非必填
     *
     */
    private String recipientName;
    /**
     * 送货地址
     * 非必填
     */
    private String recipientAdd;
    /**
     * 收件人电话
     * 非必填
     */
    private String recipientTel;

    private String province;

    private String city;

    private String district;
    /**
     * 邮编
     * 非必填
     */
    private String zipCode;
    /**
     * 处方笺
     */
    private String image;
    /**
     * 处方平台来源
     * 3.优达康
     * 4.纳里平台
     * 默认4
     */
    private Integer source;

    //处方单id
    private Integer recipeId;

    //诊断名称
    private String diagnose;

    //处方类型
    private int recipeType;

    private String orderNo;

    /**
     * 商品明细
     */
    private List<YtDrugDTO> itemList;

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getSourceSerialNumber() {
        return sourceSerialNumber;
    }

    public void setSourceSerialNumber(String sourceSerialNumber) {
        this.sourceSerialNumber = sourceSerialNumber;
    }

    public String getExternalNumber() {
        return externalNumber;
    }

    public void setExternalNumber(String externalNumber) {
        this.externalNumber = externalNumber;
    }

    public Integer getHzMedicalFlag() {
        return hzMedicalFlag;
    }

    public void setHzMedicalFlag(Integer hzMedicalFlag) {
        this.hzMedicalFlag = hzMedicalFlag;
    }

    public String getpTime() {
        return pTime;
    }

    public void setpTime(String pTime) {
        this.pTime = pTime;
    }

    public Integer getValidDay() {
        return validDay;
    }

    public void setValidDay(Integer validDay) {
        this.validDay = validDay;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public Integer getCostType() {
        return costType;
    }

    public void setCostType(Integer costType) {
        this.costType = costType;
    }

    public Double getFundAmount() {
        return fundAmount;
    }

    public void setFundAmount(Double fundAmount) {
        this.fundAmount = fundAmount;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getTransFee() {
        return transFee;
    }

    public void setTransFee(Double transFee) {
        this.transFee = transFee;
    }

    public Integer getIfPay() {
        return ifPay;
    }

    public void setIfPay(Integer ifPay) {
        this.ifPay = ifPay;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientAdd() {
        return recipientAdd;
    }

    public void setRecipientAdd(String recipientAdd) {
        this.recipientAdd = recipientAdd;
    }

    public String getRecipientTel() {
        return recipientTel;
    }

    public void setRecipientTel(String recipientTel) {
        this.recipientTel = recipientTel;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public List<YtDrugDTO> getItemList() {
        return itemList;
    }

    public void setItemList(List<YtDrugDTO> itemList) {
        this.itemList = itemList;
    }

    public Double getServiceFree() {
        return serviceFree;
    }

    public void setServiceFree(Double serviceFree) {
        this.serviceFree = serviceFree;
    }

    public Double getPrescriptionChecking() {
        return prescriptionChecking;
    }

    public void setPrescriptionChecking(Double prescriptionChecking) {
        this.prescriptionChecking = prescriptionChecking;
    }

    public int getGiveModel() {
        return giveModel;
    }

    public void setGiveModel(int giveModel) {
        this.giveModel = giveModel;
    }

    public String getDiagnose() {
        return diagnose;
    }

    public void setDiagnose(String diagnose) {
        this.diagnose = diagnose;
    }

    public int getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(int recipeType) {
        this.recipeType = recipeType;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    @Override
    public String toString() {
        return "YtRecipeDTO{" +
                "source=" + source +
                ", orgCode='" + orgCode + '\'' +
                ", sourceSerialNumber='" + sourceSerialNumber + '\'' +
                ", externalNumber='" + externalNumber + '\'' +
                ", hzMedicalFlag=" + hzMedicalFlag +
                ", pTime='" + pTime + '\'' +
                ", validDay=" + validDay +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", hospitalName='" + hospitalName + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", patientName='" + patientName + '\'' +
                ", sex=" + sex +
                ", age=" + age +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", symptom='" + symptom + '\'' +
                ", costType=" + costType +
                ", recordNo='" + recordNo + '\'' +
                ", category='" + category + '\'' +
                ", totalAmount=" + totalAmount +
                ", transFee=" + transFee +
                ", giveModel=" + giveModel +
                ", recipeType=" + recipeType +
                ", diagnose=" + diagnose +
                ", giveModel=" + giveModel +
                ", province=" + province +
                ", city=" + city +
                ", district=" + district +
                ", serviceFree=" + serviceFree +
                ", prescriptionChecking=" + prescriptionChecking +
                ", ifPay=" + ifPay +
                ", payMode=" + payMode +
                ", recipientName='" + recipientName + '\'' +
                ", recipientAdd='" + recipientAdd + '\'' +
                ", recipientTel='" + recipientTel + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", itemList=" + itemList +
                '}';
    }
}