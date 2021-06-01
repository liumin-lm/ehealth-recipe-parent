package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;


/**
 * 图文咨询监管指标
 */
public class ZiXunIndicatorsReq implements java.io.Serializable{

    private static final long serialVersionUID = 8876344836573681661L;

    @NotNull
    private String organID;//	机构唯一号
    @NotNull
    private String  organName;
    @NotNull
    private String bussID;//	咨询记录Id
    @NotNull
    private String subjectCode;//	咨询医师所属专业代码（诊疗科目代码）
    @NotNull
    private String subjectName;//	咨询医师所属专业名称（诊疗科目名称）

    private String doctorId;//	医生ID
    @NotNull
    private String doctorCertID;//	医师身份证号
    private String doctorName;//	医生姓名
    private String patientCertType;//	患者证件
//    @NotNull
    private String patientCertID;//	患者证件号
    private String patientName;//	患者姓名
    private Integer age;//	患者年龄
    private String sex;//	患者性别
    private String mobile;//	患者联系电话
    private String consultationType;//	咨询类别
    private String onsultationAttribute;// 	咨询属性
    private Date   applyDate;//	咨询申请时间
    private Date   startDate;//	咨询开始时间
    private Date   endDate;//	咨询结束时间
    private String paymentChannel;// 	支付渠道
    private Double consultationPrice;//	咨询价格
    private String content;//	用户咨询内容
    @NotNull
    private String answerFlag;//	咨询是否回复
    private Date refuseTime;//	咨询拒绝/取消时间
    private String refuseReason;//	咨询拒绝/取消原因
    private String refuseType;//	咨询拒绝类别
    private String processDataURL;//	咨询过程数据查询地址
    @NotNull
    private Date   updateTime; // 最后更新时间
//    private String satisfaction; //满意度
//    private String scoring;//评分
//    private String evaluation;//评价内容
//    private String complaints;//投诉建议

    @NotNull
    private String  unitID;



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

    public String getBussID() {
        return bussID;
    }

    public void setBussID(String bussID) {
        this.bussID = bussID;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorCertID() {
        return doctorCertID;
    }

    public void setDoctorCertID(String doctorCertID) {
        this.doctorCertID = doctorCertID;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientCertType() {
        return patientCertType;
    }

    public void setPatientCertType(String patientCertType) {
        this.patientCertType = patientCertType;
    }

    public String getPatientCertID() {
        return patientCertID;
    }

    public void setPatientCertID(String patientCertID) {
        this.patientCertID = patientCertID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public String getOnsultationAttribute() {
        return onsultationAttribute;
    }

    public void setOnsultationAttribute(String onsultationAttribute) {
        this.onsultationAttribute = onsultationAttribute;
    }

    public Date getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(Date applyDate) {
        this.applyDate = applyDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public Double getConsultationPrice() {
        return consultationPrice;
    }

    public void setConsultationPrice(Double consultationPrice) {
        this.consultationPrice = consultationPrice;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAnswerFlag() {
        return answerFlag;
    }

    public void setAnswerFlag(String answerFlag) {
        this.answerFlag = answerFlag;
    }

    public Date getRefuseTime() {
        return refuseTime;
    }

    public void setRefuseTime(Date refuseTime) {
        this.refuseTime = refuseTime;
    }

    public String getRefuseReason() {
        return refuseReason;
    }

    public void setRefuseReason(String refuseReason) {
        this.refuseReason = refuseReason;
    }

    public String getRefuseType() {
        return refuseType;
    }

    public void setRefuseType(String refuseType) {
        this.refuseType = refuseType;
    }

    public String getProcessDataURL() {
        return processDataURL;
    }

    public void setProcessDataURL(String processDataURL) {
        this.processDataURL = processDataURL;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }
}
