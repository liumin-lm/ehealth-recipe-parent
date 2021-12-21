package com.ngari.recipe.basic.ds;

import com.ngari.patient.ds.GuardianDS;
import com.ngari.patient.dto.HealthCardDTO;
import ctd.schema.annotation.*;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/*
 * 医生端脱敏对象
 * @author zhangx
 * @create 2020-07-06 15:00
 * @param null
 * @return
 **/

@Schema
public class PatientVO implements Serializable {
    private static final long serialVersionUID = -6772977197600216684L;

    @ItemProperty(alias = "登陆id")
    private String loginId;

    @ItemProperty(alias = "主索引")
    private String mpiId;

    @ItemProperty(alias = "病人姓名")
    private String patientName;

    @ItemProperty(alias = "病人性别")
    @Dictionary(id = "eh.base.dictionary.Gender")
    private String patientSex;

    @ItemProperty(alias = "出生日期")
    @Temporal(TemporalType.DATE)
    private Date birthday;

    @ItemProperty(alias = "病人类型")
    @Dictionary(id = "eh.mpi.dictionary.PatientType")
    private String patientType;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "18位身份证号-用于业务处理")
    private String idcard;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "18位身份证号，备选")
    private String idcard2;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "手机号")
    private String mobile;

    @Desensitizations(type = DesensitizationsType.ADDRESS)
    @ItemProperty(alias = "家庭地址")
    private String address;

    @ItemProperty(alias = "家庭地址区域")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String homeArea;

    @ItemProperty(alias = "个人照片")
    @FileToken(expires = 3600)
    private String photo;

    @ItemProperty(alias = "建档时间")
    private Date createDate;

    @ItemProperty(alias = "最后更新时间")
    private Date lastModify;

    @ItemProperty(alias = "状态")
    @Dictionary(id = "eh.mpi.dictionary.Status")
    private Integer status;

    @ItemProperty(alias = "监护人姓名")
    private String guardianName;

    @ItemProperty(alias = "监护人标记")
    private Boolean guardianFlag;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "证件号")
    private String certificate;

    @ItemProperty(alias = "证件类型")
    @Dictionary(id = "eh.mpi.dictionary.CertificateType")
    private Integer certificateType;

    @ItemProperty(alias = "地址(省市区)")
    private String fullHomeArea;

    @ItemProperty(alias = "就诊人类型 0:成人 1:有身份证儿童 2:无身份证儿童")
    @Dictionary(id = "eh.mpi.dictionary.PatientUserType")
    private Integer patientUserType;

    @ItemProperty(alias = "认证状态")
    @Dictionary(id = "eh.mpi.dictionary.AuthStatus")
    private Integer authStatus;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "陪诊人（监护人）证件号")
    private String guardianCertificate;

    @ItemProperty(alias = "是否用户自己")
    private Boolean isOwn;

    @ItemProperty(alias = "是否默认就诊人")
    private Boolean isDefaultPatient;

    @Dictionary(id = "eh.mpi.dictionary.Educations")
    private String education;

    @Dictionary(id = "eh.mpi.dictionary.Country")
    private String country;

    @Dictionary(id = "eh.base.dictionary.Marry")
    private String marry;

    @Dictionary(id = "eh.mpi.dictionary.Resident")
    private String resident;

    @Dictionary(id = "eh.mpi.dictionary.Household")
    private String houseHold;

    @Dictionary(id = "eh.mpi.dictionary.Job")
    private String job;

    @Dictionary(id = "eh.mpi.dictionary.PayType", multiple = true)
    private String docPayType;

    @Dictionary(id = "eh.mpi.dictionary.Nation")
    private String nation;

    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String state;

    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String birthPlace;

    private String authMsg;


    private List<HealthCardDTO> healthCards = null;

    private Boolean haveUnfinishedFollow;

    private Boolean ashFlag;

    // 年龄
    private Integer age;
    @ItemProperty(alias = "新版年龄（带单位）")
    private String ageString;

    private Boolean signFlag;

    // 医生是否关注病人标记
    private Boolean relationFlag;

    // 标签列表
    private List<String> labelNames;

    //账号

    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String loginName;

    //用户姓名
//    @Desensitizations(type = DesensitizationsType.NAME)
    private String userName;
    private Integer urt;

    private Integer relationPatientId;

    private String cardId;

    private String userIcon;

    @Dictionary(id = "eh.base.dictionary.ExpectClinicPeriodType")
    private Integer expectClinicPeriodType;

    //监护人信息
    private GuardianDS guardian;

    @ItemProperty(alias = "是否为无身份证患者标识(0:否 1:是)")
    private Integer notIdCardFlag;

    @Desensitizations(type = DesensitizationsType.NAME)
    private String realName;

    private String vaccineCardId;

    private String weight;

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
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

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    public String getIdcard() {
        return idcard;
    }

    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }

    public String getIdcard2() {
        return idcard2;
    }

    public void setIdcard2(String idcard2) {
        this.idcard2 = idcard2;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHomeArea() {
        return homeArea;
    }

    public void setHomeArea(String homeArea) {
        this.homeArea = homeArea;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public Boolean getGuardianFlag() {
        return guardianFlag;
    }

    public void setGuardianFlag(Boolean guardianFlag) {
        this.guardianFlag = guardianFlag;
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

    public String getFullHomeArea() {
        return fullHomeArea;
    }

    public void setFullHomeArea(String fullHomeArea) {
        this.fullHomeArea = fullHomeArea;
    }

    public Integer getPatientUserType() {
        return patientUserType;
    }

    public void setPatientUserType(Integer patientUserType) {
        this.patientUserType = patientUserType;
    }

    public Integer getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(Integer authStatus) {
        this.authStatus = authStatus;
    }

    public String getGuardianCertificate() {
        return guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    public Boolean getIsOwn() {
        return isOwn;
    }

    public void setIsOwn(Boolean own) {
        isOwn = own;
    }

    public List<HealthCardDTO> getHealthCards() {
        return healthCards;
    }

    public void setHealthCards(List<HealthCardDTO> healthCards) {
        this.healthCards = healthCards;
    }

    public Boolean getHaveUnfinishedFollow() {
        return haveUnfinishedFollow;
    }

    public void setHaveUnfinishedFollow(Boolean haveUnfinishedFollow) {
        this.haveUnfinishedFollow = haveUnfinishedFollow;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getSignFlag() {
        return signFlag;
    }

    public void setSignFlag(Boolean signFlag) {
        this.signFlag = signFlag;
    }

    public Boolean getRelationFlag() {
        return relationFlag;
    }

    public void setRelationFlag(Boolean relationFlag) {
        this.relationFlag = relationFlag;
    }

    public List<String> getLabelNames() {
        return labelNames;
    }

    public void setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getUrt() {
        return urt;
    }

    public void setUrt(Integer urt) {
        this.urt = urt;
    }

    public Integer getRelationPatientId() {
        return relationPatientId;
    }

    public void setRelationPatientId(Integer relationPatientId) {
        this.relationPatientId = relationPatientId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public Integer getExpectClinicPeriodType() {
        return expectClinicPeriodType;
    }

    public void setExpectClinicPeriodType(Integer expectClinicPeriodType) {
        this.expectClinicPeriodType = expectClinicPeriodType;
    }

    public GuardianDS getGuardian() {
        return guardian;
    }

    public void setGuardian(GuardianDS guardian) {
        this.guardian = guardian;
    }

    public Integer getNotIdCardFlag() {
        return notIdCardFlag;
    }

    public void setNotIdCardFlag(Integer notIdCardFlag) {
        this.notIdCardFlag = notIdCardFlag;
    }

    public Boolean getDefaultPatient() {
        return isDefaultPatient;
    }

    public void setDefaultPatient(Boolean defaultPatient) {
        isDefaultPatient = defaultPatient;
    }

    public Boolean getAshFlag() {
        return ashFlag;
    }

    public void setAshFlag(Boolean ashFlag) {
        this.ashFlag = ashFlag;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMarry() {
        return marry;
    }

    public void setMarry(String marry) {
        this.marry = marry;
    }

    public String getResident() {
        return resident;
    }

    public void setResident(String resident) {
        this.resident = resident;
    }

    public String getHouseHold() {
        return houseHold;
    }

    public void setHouseHold(String houseHold) {
        this.houseHold = houseHold;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getDocPayType() {
        return docPayType;
    }

    public void setDocPayType(String docPayType) {
        this.docPayType = docPayType;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(String userIcon) {
        this.userIcon = userIcon;
    }

    public String getVaccineCardId() {
        return vaccineCardId;
    }

    public void setVaccineCardId(String vaccineCardId) {
        this.vaccineCardId = vaccineCardId;
    }

    public String getAuthMsg() {
        return authMsg;
    }

    public void setAuthMsg(String authMsg) {
        this.authMsg = authMsg;
    }

    public String getAgeString() {
        return ageString;
    }

    public void setAgeString(String ageString) {
        this.ageString = ageString;
    }

    @Override
    public String toString() {
        return "PatientDS{" +
                "loginId='" + loginId + '\'' +
                ", mpiId='" + mpiId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", birthday=" + birthday +
                ", idcard='" + idcard + '\'' +
                ", mobile='" + mobile + '\'' +
                ", status=" + status +
                ", guardianName='" + guardianName + '\'' +
                ", certificate='" + certificate + '\'' +
                ", certificateType=" + certificateType +
                ", patientUserType=" + patientUserType +
                ", guardianCertificate='" + guardianCertificate + '\'' +
                ", isOwn=" + isOwn +
                ", urt=" + urt +
                '}';
    }
}
