package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 对接上海六院易复诊查询开处方接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzAddHospitalPrescriptionDto implements Serializable {
    /**
     * access_token
     */
    private String access_token;
    /**
     * 医院HIS处方ID
     */
    private String hisprescriptionId;
    /**
     * 医生工号或代码等医院唯一标识
     */
    private String employeeCardNo;
    /**
     * 医生姓名
     */
    private String doctorName;
    /**
     * 39
     */
    private String prescriptionType;
    /**
     * 诊断名称
     */
    private String diagnoseName;
    /**
     * 病情描述
     */
    private String symptoms;
    /**
     * 部门ID
     */
    private String departmentId;
    /**
     * 部门名称
     */
    private String departmentName;
    /**
     * 诊疗费
     * 非必填
     */
    private String medicalFee;
    /**
     * 患者就诊卡ID
     * 非必填
     */
    private String patientCard;
    /**
     * 医院病患id
     */
    private String hisPatientId;
    /**
     * 药品列表
     */
    private List<YfzMesDrugDetailDto> mesDrugDetailList;
    /**
     * 病人信息
     */
    private YfzMesPatientDto mesPatient;
    /**
     * 收货人信息
     */
    private YfzTBPrescriptionExtendDto tbPrescriptionExtend;
    private YfzTADrugStoreDto taDrugStore;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getHisprescriptionId() {
        return hisprescriptionId;
    }

    public void setHisprescriptionId(String hisprescriptionId) {
        this.hisprescriptionId = hisprescriptionId;
    }

    public String getEmployeeCardNo() {
        return employeeCardNo;
    }

    public void setEmployeeCardNo(String employeeCardNo) {
        this.employeeCardNo = employeeCardNo;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPrescriptionType() {
        return prescriptionType;
    }

    public void setPrescriptionType(String prescriptionType) {
        this.prescriptionType = prescriptionType;
    }

    public String getDiagnoseName() {
        return diagnoseName;
    }

    public void setDiagnoseName(String diagnoseName) {
        this.diagnoseName = diagnoseName;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(String medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getPatientCard() {
        return patientCard;
    }

    public void setPatientCard(String patientCard) {
        this.patientCard = patientCard;
    }

    public String getHisPatientId() {
        return hisPatientId;
    }

    public void setHisPatientId(String hisPatientId) {
        this.hisPatientId = hisPatientId;
    }

    public List<YfzMesDrugDetailDto> getMesDrugDetailList() {
        return mesDrugDetailList;
    }

    public void setMesDrugDetailList(List<YfzMesDrugDetailDto> mesDrugDetailList) {
        this.mesDrugDetailList = mesDrugDetailList;
    }

    public YfzMesPatientDto getMesPatient() {
        return mesPatient;
    }

    public void setMesPatient(YfzMesPatientDto mesPatient) {
        this.mesPatient = mesPatient;
    }

    public YfzTBPrescriptionExtendDto getTbPrescriptionExtend() {
        return tbPrescriptionExtend;
    }

    public void setTbPrescriptionExtend(YfzTBPrescriptionExtendDto tbPrescriptionExtend) {
        this.tbPrescriptionExtend = tbPrescriptionExtend;
    }

    public YfzTADrugStoreDto getTaDrugStore() {
        return taDrugStore;
    }

    public void setTaDrugStore(YfzTADrugStoreDto taDrugStore) {
        this.taDrugStore = taDrugStore;
    }
}
