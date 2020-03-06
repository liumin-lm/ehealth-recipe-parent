package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 诊疗后 满意度、评分、评价内容、投诉建议 上传接口参数
 */
public class BussAfterInfoReq implements java.io.Serializable{

    private static final long serialVersionUID = 7332917850630884890L;

    @NotNull
    private String  organID;//机构
    @NotNull
    private String  organName;
    @NotNull
    private String  businessType;//业务类型 1图文咨询 2电话咨询 3视频咨询 4在线复诊 5在线处方 6在线签约7检查预约 8远程门诊9远程会诊 10住院预约
    @NotNull
    private String  bussID;//业务ID
    @NotNull
    private String  evaluateID;//评价记录ID

    private String  deptName;
    private String  doctorId;
    @NotNull
    private String  doctorCertID;
    private String  doctorName;

    private Integer  satisfaction; //满意度
    private Double  scoring;//评分
    private String  evaluation;//评价内容
    private String  complaints;//投诉建议
    private String evaluationPeople; // 评价人
    @NotNull
    private Date   evaluationTime;

    @NotNull
    private Date updateTime; // 最后更新时间
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

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBussID() {
        return bussID;
    }

    public void setBussID(String bussID) {
        this.bussID = bussID;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
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

    public Integer getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(Integer satisfaction) {
        this.satisfaction = satisfaction;
    }

    public Double getScoring() {
        return scoring;
    }

    public void setScoring(Double scoring) {
        this.scoring = scoring;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getComplaints() {
        return complaints;
    }

    public void setComplaints(String complaints) {
        this.complaints = complaints;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getEvaluateID() {
        return evaluateID;
    }

    public void setEvaluateID(String evaluateID) {
        this.evaluateID = evaluateID;
    }

    public String getEvaluationPeople() {
        return evaluationPeople;
    }

    public void setEvaluationPeople(String evaluationPeople) {
        this.evaluationPeople = evaluationPeople;
    }

    public Date getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(Date evaluationTime) {
        this.evaluationTime = evaluationTime;
    }
}
