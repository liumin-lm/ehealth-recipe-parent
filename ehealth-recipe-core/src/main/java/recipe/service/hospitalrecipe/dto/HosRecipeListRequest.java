package recipe.service.hospitalrecipe.dto;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/10/16
 * 入参 无需脱敏
 */
public class HosRecipeListRequest implements Serializable {

    private static final long serialVersionUID = -8250786987260451176L;

    @Verify(desc = "医生ID")
    private Integer doctorId;

    @Verify(desc = "患者证件类型")
    private String certificateType;

    @Verify(desc = "患者证件号")
    private String certificate;

    @Verify(desc = "患者姓名")
    private String patientName;

    @Verify(desc = "患者电话")
    private String patientTel;

    @Verify(desc = "性别", isInt = true)
    private String patientSex;

    @Verify(desc = "组织结构编码")
    private String organId;

    @Verify(desc = "就诊卡号")
    private String cardNo;

    @Verify(desc = "分页开始下标")
    private Integer start;

    @Verify(desc = "分页单页数量")
    private Integer limit;

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientTel() {
        return patientTel;
    }

    public void setPatientTel(String patientTel) {
        this.patientTel = patientTel;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
