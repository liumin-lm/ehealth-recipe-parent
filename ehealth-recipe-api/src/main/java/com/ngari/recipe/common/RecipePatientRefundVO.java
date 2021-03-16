package com.ngari.recipe.common;

import java.util.Date;

public class RecipePatientRefundVO implements java.io.Serializable {

    private static final long serialVersionUID = -5191649339763189617L;

    public RecipePatientRefundVO() {
    }

    public RecipePatientRefundVO(Integer doctorId, Integer refundStatus, String refundReason, Date refundDate, String patientName, String patientMpiid, Integer busId, Double refundPrice, String refundStatusMsg) {
        this.doctorId = doctorId;
        this.refundStatus = refundStatus;
        this.refundReason = refundReason;
        this.refundDate = refundDate;
        this.patientName = patientName;
        this.patientMpiid = patientMpiid;
        this.busId = busId;
        this.refundPrice = refundPrice;
        this.refundStatusMsg = refundStatusMsg;
    }

    private Integer doctorId;
    /**
     *  患者退款状态
     */
    private Integer refundStatus;
    /**
     * 患者退款理由
     */
    private String refundReason;
    /**
     * 患者退款时间
     */
    private Date refundDate;
    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 患者头像
     */
    private String photo;

    /**
     * 患者性别
     */
    private String patientSex;

    /**
     * 患者年龄
     */
    private Integer patientAge;
    /**
     * 患者姓名mpiid
     */
    private String patientMpiid;
    /**
     * 关联的业务id
     */
    private Integer busId;
    /**
     * 关联的复诊金额
     */
    private Double refundPrice;
    /**
     *  患者退款状态文案
     */
    private String refundStatusMsg;
    /**
     *  医生退款不通过原因
     */
    private String doctorNoPassReason;

    public String getDoctorNoPassReason() {
        return doctorNoPassReason;
    }

    public void setDoctorNoPassReason(String doctorNoPassReason) {
        this.doctorNoPassReason = doctorNoPassReason;
    }

    public String getRefundStatusMsg() {
        return refundStatusMsg;
    }

    public void setRefundStatusMsg(String refundStatusMsg) {
        this.refundStatusMsg = refundStatusMsg;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Date getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(Date refundDate) {
        this.refundDate = refundDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientMpiid() {
        return patientMpiid;
    }

    public void setPatientMpiid(String patientMpiid) {
        this.patientMpiid = patientMpiid;
    }

    public Integer getBusId() {
        return busId;
    }

    public void setBusId(Integer busId) {
        this.busId = busId;
    }

    public Double getRefundPrice() {
        return refundPrice;
    }

    public void setRefundPrice(Double refundPrice) {
        this.refundPrice = refundPrice;
    }
}