package com.ngari.recipe.entity.sign;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Schema
@Entity
@Table(name = "sign_doctor_ca_info")
public class SignDoctorCaInfo {

    private Integer id;

    /**
     * 医生ID
     */
    private Integer doctorId;

    /**
     * 签名序列号
     */
    private String caSerCode;

    /**
     * 证书信息
     */
    private String cert_voucher;

    /**
     * 证书序列号
     */
    private String certSerial;

    /**
     * ca类型
     */
    private String caType;

    private Date createDate;

    private Date lastmodify;

    /**
     * 证书到期时间
     */
    private Date caEndTime;

    private String name;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String idcard;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Column
    public String getCaSerCode() {
        return caSerCode;
    }

    public void setCaSerCode(String caSerCode) {
        this.caSerCode = caSerCode;
    }

    @Column
    public String getCert_voucher() {
        return cert_voucher;
    }

    public void setCert_voucher(String cert_voucher) {
        this.cert_voucher = cert_voucher;
    }


    @Column
    public String getCaType() {
        return caType;
    }

    public void setCaType(String caType) {
        this.caType = caType;
    }

    @Column
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column
    public Date getLastmodify() {
        return lastmodify;
    }

    public void setLastmodify(Date lastmodify) {
        this.lastmodify = lastmodify;
    }

    @Column
    public Date getCaEndTime() {
        return caEndTime;
    }

    public void setCaEndTime(Date caEndTime) {
        this.caEndTime = caEndTime;
    }

    @Column
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column
    public String getIdcard() {
        return idcard;
    }

    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }

    public String getCertSerial() {
        return certSerial;
    }

    public void setCertSerial(String certSerial) {
        this.certSerial = certSerial;
    }
}
