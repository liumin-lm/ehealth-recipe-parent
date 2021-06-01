package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 医生指标数据
 */
public class DoctorIndicatorsReq implements java.io.Serializable{

    private static final long serialVersionUID = -8389740151192131443L;
    @NotNull
    private String organID;             // 主要执业机构ID 平台分配
    @NotNull
    private String  organName;
    @NotNull
    private String doctorID;            // 医生内部ID
    @NotNull
    private String doctorName;          // 姓名
    @NotNull
    private String doctorCertID;        // 身份证号码
    private String mobile;              //电话
    private String sex;                 // 性别
    private Date   birthday;            //出生年月
    private String national;            //民族

    private String doctorTileID;        //医师职称ID
    private String doctorTile;          //医师职称
    @NotNull
    private String subjectCode;         // 诊疗科目代码
    @NotNull
    private String subjectName;         // 诊疗科目名称

    private String doctorCertCode;      // 资格证书编码
    private String doctorWorkCode;      // 执业证书编码
    @NotNull
    private String doctorUnitName;      // 主要执业机构
    private String doctorAreaName;      // 执业地点名称

    private String firstWorkUnitId;     //第一执业机构ID
    private String firstUnitName;       //第一执业机构名称
    private String firstAreaName;       //第一执业地点名称
    private String secondWorkUnitId;    //第二执业机构ID
    private String secondWordName;      //第二执业机构名称
    private String secondAreaName;      //第二执业地点名称
    private String thirdWorkUnitId;     //第三执业机构ID
    private String thirdWordName;       //第三执业机构名称
    private String thirdAreaName;       //第三执业地点名称

    private String doctorOrganName;     //	发证机关
    private Date   doctorWorkDate;      // 发证日期
    private String doctorWorkType;      // 1医师 2药师 3护师
    private Integer doctorYears;         //独立临床工作经验年限
    private String policyNo ;           //医疗责任保险编号
    private Date   policyStartDate;     //医疗责任险生效日期
    private Date   policyEndDate;       //医疗责任险到期日期
    @NotNull
    private Date   updateTime;          // 最后更新时间
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

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorCertID() {
        return doctorCertID;
    }

    public void setDoctorCertID(String doctorCertID) {
        this.doctorCertID = doctorCertID;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getNational() {
        return national;
    }

    public void setNational(String national) {
        this.national = national;
    }

    public String getDoctorTileID() {
        return doctorTileID;
    }

    public void setDoctorTileID(String doctorTileID) {
        this.doctorTileID = doctorTileID;
    }

    public String getDoctorTile() {
        return doctorTile;
    }

    public void setDoctorTile(String doctorTile) {
        this.doctorTile = doctorTile;
    }

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

    public String getDoctorCertCode() {
        return doctorCertCode;
    }

    public void setDoctorCertCode(String doctorCertCode) {
        this.doctorCertCode = doctorCertCode;
    }

    public String getDoctorWorkCode() {
        return doctorWorkCode;
    }

    public void setDoctorWorkCode(String doctorWorkCode) {
        this.doctorWorkCode = doctorWorkCode;
    }

    public String getDoctorUnitName() {
        return doctorUnitName;
    }

    public void setDoctorUnitName(String doctorUnitName) {
        this.doctorUnitName = doctorUnitName;
    }

    public String getDoctorAreaName() {
        return doctorAreaName;
    }

    public void setDoctorAreaName(String doctorAreaName) {
        this.doctorAreaName = doctorAreaName;
    }

    public String getFirstWorkUnitId() {
        return firstWorkUnitId;
    }

    public void setFirstWorkUnitId(String firstWorkUnitId) {
        this.firstWorkUnitId = firstWorkUnitId;
    }

    public String getFirstUnitName() {
        return firstUnitName;
    }

    public void setFirstUnitName(String firstUnitName) {
        this.firstUnitName = firstUnitName;
    }

    public String getFirstAreaName() {
        return firstAreaName;
    }

    public void setFirstAreaName(String firstAreaName) {
        this.firstAreaName = firstAreaName;
    }

    public String getSecondWorkUnitId() {
        return secondWorkUnitId;
    }

    public void setSecondWorkUnitId(String secondWorkUnitId) {
        this.secondWorkUnitId = secondWorkUnitId;
    }

    public String getSecondWordName() {
        return secondWordName;
    }

    public void setSecondWordName(String secondWordName) {
        this.secondWordName = secondWordName;
    }

    public String getSecondAreaName() {
        return secondAreaName;
    }

    public void setSecondAreaName(String secondAreaName) {
        this.secondAreaName = secondAreaName;
    }

    public String getThirdWorkUnitId() {
        return thirdWorkUnitId;
    }

    public void setThirdWorkUnitId(String thirdWorkUnitId) {
        this.thirdWorkUnitId = thirdWorkUnitId;
    }

    public String getThirdWordName() {
        return thirdWordName;
    }

    public void setThirdWordName(String thirdWordName) {
        this.thirdWordName = thirdWordName;
    }

    public String getThirdAreaName() {
        return thirdAreaName;
    }

    public void setThirdAreaName(String thirdAreaName) {
        this.thirdAreaName = thirdAreaName;
    }

    public String getDoctorOrganName() {
        return doctorOrganName;
    }

    public void setDoctorOrganName(String doctorOrganName) {
        this.doctorOrganName = doctorOrganName;
    }

    public Date getDoctorWorkDate() {
        return doctorWorkDate;
    }

    public void setDoctorWorkDate(Date doctorWorkDate) {
        this.doctorWorkDate = doctorWorkDate;
    }

    public String getDoctorWorkType() {
        return doctorWorkType;
    }

    public void setDoctorWorkType(String doctorWorkType) {
        this.doctorWorkType = doctorWorkType;
    }

    public Integer getDoctorYears() {
        return doctorYears;
    }

    public void setDoctorYears(Integer doctorYears) {
        this.doctorYears = doctorYears;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public Date getPolicyStartDate() {
        return policyStartDate;
    }

    public void setPolicyStartDate(Date policyStartDate) {
        this.policyStartDate = policyStartDate;
    }

    public Date getPolicyEndDate() {
        return policyEndDate;
    }

    public void setPolicyEndDate(Date policyEndDate) {
        this.policyEndDate = policyEndDate;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }
}
