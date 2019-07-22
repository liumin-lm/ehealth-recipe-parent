package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.util.Date;

/**
 * created by shiyuping on 2019/6/19
 */
public class FirstVisitRecord implements Serializable {

    private static final long serialVersionUID = -3593413244463908529L;
    private String  subjectCode;//	开方医师所属专业代码（诊疗科目代码）
    private String  subjectName;//	开方医师所属专业名称（诊疗科目名称）
    private String  deptID;//	医师所属科室代码
    private String  deptName;//	医师所属科室名称
    private Date visitDatetime;//初诊就诊时间
    private String  icdCode;//	诊断ICD码
    private String  icdName;//	初步诊断名称
    private String patientNumber;//门诊号

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

    public String getDeptID() {
        return deptID;
    }

    public void setDeptID(String deptID) {
        this.deptID = deptID;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Date getVisitDatetime() {
        return visitDatetime;
    }

    public void setVisitDatetime(Date visitDatetime) {
        this.visitDatetime = visitDatetime;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public String getIcdName() {
        return icdName;
    }

    public void setIcdName(String icdName) {
        this.icdName = icdName;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }
}
