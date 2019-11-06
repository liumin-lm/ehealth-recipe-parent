package recipe.medicationguide.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/10/28
 * 用药指导患者信息数据
 * @author shiyuping
 */
public class PatientInfoDTO implements Serializable {
    private static final long serialVersionUID = -2640113184867316444L;
    /**
     * 患者编号
     */
    @JsonProperty("PatientCode")
    private String patientCode;
    /**
     * 卡号
     */
    @JsonProperty("Card")
    private String card;
    /**
     * 卡类型（1身份证 2 医保卡 3 临时卡）
     */
    @JsonProperty("CardType")
    private String cardType;
    /**
     * 患者姓名
     */
    @JsonProperty("PatientName")
    private String patientName;
    /**
     * 年龄（小于一岁时传递值xx月xx天 大于一岁直接数值字符串类型）
     */
    @JsonProperty("PatientAge")
    private String patientAge;
    /**
     * 性别 1男 2女
     */
    @JsonProperty("Gender")
    private String gender;
    /**
     * 就诊科室名称
     */
    @JsonProperty("DeptName")
    private String deptName;
    /**
     * 就诊号
     */
    @JsonProperty("AdminNo")
    private String adminNo;
    /**
     * 标识字段（暂未定义）默认0
     */
    @JsonProperty("Flag")
    private String flag;
    /**
     * 处方开具时间 精确到秒
     */
    @JsonProperty("DocDate")
    private String docDate;

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getAdminNo() {
        return adminNo;
    }

    public void setAdminNo(String adminNo) {
        this.adminNo = adminNo;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getDocDate() {
        return docDate;
    }

    public void setDocDate(String docDate) {
        this.docDate = docDate;
    }
}
