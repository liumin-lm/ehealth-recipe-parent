package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;

import java.io.Serializable;
import java.util.Date;

/**
 * created by shiyuping on 2019/11/11
 *
 * @author shiyuping
 */
public class HosPatientDTO implements Serializable {
    private static final long serialVersionUID = -5540046098351163199L;
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String mobile;
    //his病人唯一标识
    private String patientID;
    private String patientName;
    private String patientSex;
    private Date birthday;//出生日期
    //病历号 门诊号 等
    private String clinicID;
    //卡号
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String cardID;
    private String cardType;
    private String cardOrgan;
    private String mpi;
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String certificate;
    //证件类型
    private Integer certificateType;
    //患者类型  0:成人 1:有身份证儿童 2:无身份证儿童
    private Integer patientUserType;
    //陪诊人(监护人)信息
    //陪诊人证件号
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String guardianCertificate;
    //陪诊人件类型
    private Integer guardianCertificateType;
    //陪诊人姓名
    private String guardianPatientName;
    //陪诊人手机
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String guardianMobile;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getClinicID() {
        return clinicID;
    }

    public void setClinicID(String clinicID) {
        this.clinicID = clinicID;
    }

    public String getCardID() {
        return cardID;
    }

    public void setCardID(String cardID) {
        this.cardID = cardID;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardOrgan() {
        return cardOrgan;
    }

    public void setCardOrgan(String cardOrgan) {
        this.cardOrgan = cardOrgan;
    }

    public String getMpi() {
        return mpi;
    }

    public void setMpi(String mpi) {
        this.mpi = mpi;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public Integer getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(Integer certificateType) {
        this.certificateType = certificateType;
    }

    public Integer getPatientUserType() {
        return patientUserType;
    }

    public void setPatientUserType(Integer patientUserType) {
        this.patientUserType = patientUserType;
    }

    public String getGuardianCertificate() {
        return guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    public Integer getGuardianCertificateType() {
        return guardianCertificateType;
    }

    public void setGuardianCertificateType(Integer guardianCertificateType) {
        this.guardianCertificateType = guardianCertificateType;
    }

    public String getGuardianPatientName() {
        return guardianPatientName;
    }

    public void setGuardianPatientName(String guardianPatientName) {
        this.guardianPatientName = guardianPatientName;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }
}
