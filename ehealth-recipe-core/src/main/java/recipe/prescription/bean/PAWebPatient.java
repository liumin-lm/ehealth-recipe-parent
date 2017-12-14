package recipe.prescription.bean;

import java.util.List;

/**
 * Created by jiangtingfeng on 2017/11/15.
 */
public class PAWebPatient {

    private String admNo;

    private String name;

    private String age;

    private String gender;

    private String medicareType;

    // 过敏信息
    private List<AuditAllergy> allergies;

    // 诊断信息
    private List<AuditDiagnose> diagnoses;

    // 检查单信息
    private String prescription;

    public String getAdmNo() {
        return admNo;
    }

    public void setAdmNo(String admNo) {
        this.admNo = admNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMedicareType() {
        return medicareType;
    }

    public void setMedicareType(String medicareType) {
        this.medicareType = medicareType;
    }

    public List<AuditAllergy> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<AuditAllergy> allergies) {
        this.allergies = allergies;
    }

    public List<AuditDiagnose> getDiagnoses() {
        return diagnoses;
    }

    public void setDiagnoses(List<AuditDiagnose> diagnoses) {
        this.diagnoses = diagnoses;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }
}
