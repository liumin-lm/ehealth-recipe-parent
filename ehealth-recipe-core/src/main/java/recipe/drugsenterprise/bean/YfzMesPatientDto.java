package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @Description: 对接上海六院易复诊开处方接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzMesPatientDto implements Serializable {
    /**
     * 性别  1：男 2：女
     */
    private String patientSex;
    /**
     * 生日  例如：1984/11/11
     */
    private String birthday;
    /**
     * 手机号
     */
    private String phoneNo;
    /**
     * 病患姓名
     */
    private String patientName;
    /**
     * 身份证号
     */
    private String certificateId;
    /**
     * +86
     */
    private String areaCode;

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }
}
