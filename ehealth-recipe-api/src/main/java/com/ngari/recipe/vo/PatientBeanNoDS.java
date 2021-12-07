package com.ngari.recipe.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.RelationLabelBean;
import com.ngari.bus.housekeeper.model.ActionHabitBean;
import com.ngari.bus.housekeeper.model.HealthLogBean;
import com.ngari.bus.housekeeper.model.LifeHabitBean;
import com.ngari.emergency.model.FamilyDoctorBean;
import com.ngari.emergency.model.HealthInformationBean;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/12/7 下午2:53
 * @Description
 */
public class PatientBeanNoDS {
    @ItemProperty(
            alias = "登陆id"
    )
    private String loginId;
    @ItemProperty(
            alias = "主索引"
    )
    private String mpiId;
    @ItemProperty(
            alias = "病人姓名"
    )
    private String patientName;
    @ItemProperty(
            alias = "病人性别"
    )
    @Dictionary(
            id = "eh.base.dictionary.Gender"
    )
    private String patientSex;
    @ItemProperty(
            alias = "出生日期"
    )
    @Temporal(TemporalType.DATE)
    private Date birthday;
    @ItemProperty(
            alias = "病人类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.PatientType"
    )
    private String patientType;
    @ItemProperty(
            alias = "18位身份证号-用于业务处理"
    )
    private String idcard;
    @ItemProperty(
            alias = "18位身份证号，备选"
    )
    private String idcard2;
    @ItemProperty(
            alias = "手机号"
    )
    private String mobile;
    @ItemProperty(
            alias = "家庭地址"
    )
    private String address;
    @ItemProperty(
            alias = "家庭地址经度"
    )
    private Double longituder;
    @ItemProperty(
            alias = "家庭地址纬度"
    )
    private Double latitude;
    @ItemProperty(
            alias = "家庭地址区域"
    )
    @Dictionary(
            id = "eh.base.dictionary.AddrArea"
    )
    private String homeArea;
    @ItemProperty(
            alias = "邮政编码"
    )
    private String zipCode;
    @ItemProperty(
            alias = "婚姻"
    )
    @Dictionary(
            id = "eh.base.dictionary.Marry"
    )
    private String marry;
    @ItemProperty(
            alias = "职业"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Job"
    )
    private String job;
    @ItemProperty(
            alias = "民族"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Nation"
    )
    private String nation;
    @ItemProperty(
            alias = "学历"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Educations"
    )
    private String education;
    @ItemProperty(
            alias = "国籍"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Country"
    )
    private String country;
    @ItemProperty(
            alias = "省份"
    )
    @Dictionary(
            id = "eh.base.dictionary.AddrArea"
    )
    private String state;
    @ItemProperty(
            alias = "籍贯"
    )
    @Dictionary(
            id = "eh.base.dictionary.AddrArea"
    )
    private String birthPlace;
    @ItemProperty(
            alias = "联系人名"
    )
    private String linkMan;
    @ItemProperty(
            alias = "联系人关系"
    )
    @Dictionary(
            id = "eh.base.dictionary.LinkRation"
    )
    private String linkRation;
    @ItemProperty(
            alias = "联系人电话"
    )
    private String linkTel;
    @ItemProperty(
            alias = "联系人证件号"
    )
    private String linkCertificate;
    @ItemProperty(
            alias = "工作单位"
    )
    private String company;
    @ItemProperty(
            alias = "担保人名"
    )
    private String surety;
    @ItemProperty(
            alias = "担保人关系"
    )
    private String suretyRation;
    @ItemProperty(
            alias = "担保人电话"
    )
    private String suretyTel;
    @ItemProperty(
            alias = "电子邮箱"
    )
    private String email;
    @ItemProperty(
            alias = "微信号"
    )
    private String weiXin;
    @ItemProperty(
            alias = "血型"
    )
    @Dictionary(
            id = "eh.base.dictionary.Blood"
    )
    private String blood;
    @ItemProperty(
            alias = "个人照片"
    )
    private String photo;
    @ItemProperty(
            alias = "建档机构"
    )
    private String createUnit;
    @ItemProperty(
            alias = "建档时间"
    )
    private Date createDate;
    @ItemProperty(
            alias = "最新诊断编码"
    )
    private String lastDiseaseId;
    @ItemProperty(
            alias = "最新诊断名称"
    )
    private String lastDiseaseName;
    @ItemProperty(
            alias = "最新病情摘要"
    )
    private String lastSummary;
    @ItemProperty(
            alias = "最后更新机构"
    )
    private String lastUpdateUnit;
    @ItemProperty(
            alias = "最后更新时间"
    )
    private Date lastModify;
    @ItemProperty(
            alias = "前端输入的原始数据"
    )
    private String rawIdcard;
    @ItemProperty(
            alias = "状态"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Status"
    )
    private Integer status;
    @ItemProperty(
            alias = "监护人姓名"
    )
    private String guardianName;
    @ItemProperty(
            alias = "监护人标记"
    )
    private Boolean guardianFlag;
    @ItemProperty(
            alias = "身高"
    )
    private String height;
    @ItemProperty(
            alias = "体重"
    )
    private String weight;
    @ItemProperty(
            alias = "是否创建健康档案"
    )
    private Boolean healthProfileFlag;
    @ItemProperty(
            alias = "紧急联系人电话"
    )
    private String emergencyContactPhone;
    @ItemProperty(
            alias = "是否需要同步到公卫"
    )
    private Boolean synHealthRecordFlag;
    @ItemProperty(
            alias = "户别"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Household"
    )
    private String houseHold;
    @ItemProperty(
            alias = "医疗费支付类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.PayType",
            multiple = true
    )
    private String docPayType;
    @ItemProperty(
            alias = "常住类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.Resident"
    )
    private String resident;
    @ItemProperty(
            alias = "户口所在详细地址"
    )
    private String birthPlaceDetail;
    @ItemProperty(
            alias = "户口地址(省市区)"
    )
    private String fullBirthPlace;
    @ItemProperty(
            alias = "国籍详情（非中国）"
    )
    private String countryItem;
    @ItemProperty(
            alias = "证件号"
    )
    private String certificate;
    @ItemProperty(
            alias = "证件类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.CertificateType"
    )
    private Integer certificateType;
    @ItemProperty(
            alias = "地址(省市区)"
    )
    private String fullHomeArea;
    @ItemProperty(
            alias = "就诊人类型 0:成人 1:有身份证儿童 2:无身份证儿童"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.PatientUserType"
    )
    private Integer patientUserType;
    private List<HealthCardBean> healthCards = null;
    private Boolean haveUnfinishedFollow;
    private String userIcon;
    private String note;
    private Integer age;
    private Boolean signFlag;
    private Boolean relationFlag;
    private List<RelationLabelBean> labels;
    private List<String> labelNames;
    private String loginName;
    private String userName;
    private Integer urt;
    private Integer relationPatientId;
    @Dictionary(
            id = "eh.mpi.dictionary.AuthStatus"
    )
    private Integer authStatus;
    private String realName;
    @ItemProperty(
            alias = "实名认证信息"
    )
    private String authMsg;
    @ItemProperty(
            alias = "认证证件类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.CertificateType"
    )
    private Integer cardType;
    private String vaccineCardId;
    @ItemProperty(
            alias = "陪诊人（监护人）证件号"
    )
    private String guardianCertificate;
    private HealthLogBean healthLogBean;
    private ActionHabitBean actionHabitBean;
    private LifeHabitBean lifeHabitBean;
    private String cardId;
    private HealthInformationBean healthInformationBean;
    private FamilyDoctorBean familyDoctorBean;
    private Integer notIdCardFlag;
    private Integer servicePackFlag;
    @ItemProperty(
            alias = "入口端配置表主键"
    )
    private Integer clientConfigId;
    private String clientName;
    private String clientType;
    @ItemProperty(
            alias = "陪诊人（监护人）证件类型"
    )
    @Dictionary(
            id = "eh.mpi.dictionary.CertificateType"
    )
    private Integer guardianCertificateType;
    @ItemProperty(
            alias = "名"
    )
    private String firstName;
    @ItemProperty(
            alias = "姓"
    )
    private String lastName;
    @ItemProperty(
            alias = "陪诊人性别"
    )
    private String guardianSex;
    @ItemProperty(
            alias = "陪诊人年龄"
    )
    private Integer guardianAge;
    @ItemProperty(
            alias = "新版年龄（带单位）"
    )
    private String ageString;
    @ItemProperty(
            alias = "陪诊人年龄（带单位）"
    )
    private String guardianAgeString;

    public PatientBeanNoDS() {
    }

    public String getGuardianCertificate() {
        return this.guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    public Integer getServicePackFlag() {
        return this.servicePackFlag;
    }

    public void setServicePackFlag(Integer servicePackFlag) {
        this.servicePackFlag = servicePackFlag;
    }

    public Integer getNotIdCardFlag() {
        return this.notIdCardFlag;
    }

    public void setNotIdCardFlag(Integer notIdCardFlag) {
        this.notIdCardFlag = notIdCardFlag;
    }

    public String getLoginId() {
        return this.loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getMpiId() {
        return this.mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getPatientName() {
        return this.patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientSex() {
        return this.patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "GMT+8"
    )
    public Date getBirthday() {
        return this.birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getPatientType() {
        return this.patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    public String getIdcard() {
        return this.idcard;
    }

    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }

    public String getIdcard2() {
        return this.idcard2;
    }

    public void setIdcard2(String idcard2) {
        this.idcard2 = idcard2;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLongituder() {
        return this.longituder;
    }

    public void setLongituder(Double longituder) {
        this.longituder = longituder;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getHomeArea() {
        return this.homeArea;
    }

    public void setHomeArea(String homeArea) {
        this.homeArea = homeArea;
    }

    public String getZipCode() {
        return this.zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getMarry() {
        return this.marry;
    }

    public void setMarry(String marry) {
        this.marry = marry;
    }

    public String getJob() {
        return this.job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getNation() {
        return this.nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getEducation() {
        return this.education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBirthPlace() {
        return this.birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getLinkMan() {
        return this.linkMan;
    }

    public void setLinkMan(String linkMan) {
        this.linkMan = linkMan;
    }

    public String getLinkRation() {
        return this.linkRation;
    }

    public void setLinkRation(String linkRation) {
        this.linkRation = linkRation;
    }

    public String getLinkTel() {
        return this.linkTel;
    }

    public void setLinkTel(String linkTel) {
        this.linkTel = linkTel;
    }

    public String getSurety() {
        return this.surety;
    }

    public void setSurety(String surety) {
        this.surety = surety;
    }

    public String getSuretyRation() {
        return this.suretyRation;
    }

    public void setSuretyRation(String suretyRation) {
        this.suretyRation = suretyRation;
    }

    public String getSuretyTel() {
        return this.suretyTel;
    }

    public void setSuretyTel(String suretyTel) {
        this.suretyTel = suretyTel;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWeiXin() {
        return this.weiXin;
    }

    public void setWeiXin(String weiXin) {
        this.weiXin = weiXin;
    }

    public String getBlood() {
        return this.blood;
    }

    public void setBlood(String blood) {
        this.blood = blood;
    }

    public String getPhoto() {
        return this.photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getCreateUnit() {
        return this.createUnit;
    }

    public void setCreateUnit(String createUnit) {
        this.createUnit = createUnit;
    }

    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getLastDiseaseId() {
        return this.lastDiseaseId;
    }

    public void setLastDiseaseId(String lastDiseaseId) {
        this.lastDiseaseId = lastDiseaseId;
    }

    public String getLastDiseaseName() {
        return this.lastDiseaseName;
    }

    public void setLastDiseaseName(String lastDiseaseName) {
        this.lastDiseaseName = lastDiseaseName;
    }

    public String getLastSummary() {
        return this.lastSummary;
    }

    public void setLastSummary(String lastSummary) {
        this.lastSummary = lastSummary;
    }

    public String getLastUpdateUnit() {
        return this.lastUpdateUnit;
    }

    public void setLastUpdateUnit(String lastUpdateUnit) {
        this.lastUpdateUnit = lastUpdateUnit;
    }

    public Date getLastModify() {
        return this.lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getRawIdcard() {
        return this.rawIdcard;
    }

    public void setRawIdcard(String rawIdcard) {
        this.rawIdcard = rawIdcard;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getGuardianName() {
        return this.guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public Boolean getGuardianFlag() {
        return this.guardianFlag;
    }

    public void setGuardianFlag(Boolean guardianFlag) {
        this.guardianFlag = guardianFlag;
    }

    public String getHeight() {
        return this.height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return this.weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public Boolean getHealthProfileFlag() {
        return this.healthProfileFlag;
    }

    public void setHealthProfileFlag(Boolean healthProfileFlag) {
        this.healthProfileFlag = healthProfileFlag;
    }

    public String getEmergencyContactPhone() {
        return this.emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public Boolean getSynHealthRecordFlag() {
        return this.synHealthRecordFlag;
    }

    public void setSynHealthRecordFlag(Boolean synHealthRecordFlag) {
        this.synHealthRecordFlag = synHealthRecordFlag;
    }

    public String getHouseHold() {
        return this.houseHold;
    }

    public void setHouseHold(String houseHold) {
        this.houseHold = houseHold;
    }

    public String getDocPayType() {
        return this.docPayType;
    }

    public void setDocPayType(String docPayType) {
        this.docPayType = docPayType;
    }

    public String getResident() {
        return this.resident;
    }

    public void setResident(String resident) {
        this.resident = resident;
    }

    public String getBirthPlaceDetail() {
        return this.birthPlaceDetail;
    }

    public void setBirthPlaceDetail(String birthPlaceDetail) {
        this.birthPlaceDetail = birthPlaceDetail;
    }

    public String getFullBirthPlace() {
        return this.fullBirthPlace;
    }

    public void setFullBirthPlace(String fullBirthPlace) {
        this.fullBirthPlace = fullBirthPlace;
    }

    public String getCountryItem() {
        return this.countryItem;
    }

    public void setCountryItem(String countryItem) {
        this.countryItem = countryItem;
    }

    public String getFullHomeArea() {
        return this.fullHomeArea;
    }

    public void setFullHomeArea(String fullHomeArea) {
        this.fullHomeArea = fullHomeArea;
    }

    public Integer getPatientUserType() {
        return this.patientUserType;
    }

    public void setPatientUserType(Integer patientUserType) {
        this.patientUserType = patientUserType;
    }

    public List<HealthCardBean> getHealthCards() {
        return this.healthCards;
    }

    public void setHealthCards(List<HealthCardBean> healthCards) {
        this.healthCards = healthCards;
    }

    public Integer getAge() {
        return this.age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getSignFlag() {
        return this.signFlag;
    }

    public void setSignFlag(Boolean signFlag) {
        this.signFlag = signFlag;
    }

    public Boolean getRelationFlag() {
        return this.relationFlag;
    }

    public void setRelationFlag(Boolean relationFlag) {
        this.relationFlag = relationFlag;
    }

    public List<String> getLabelNames() {
        return this.labelNames;
    }

    public void setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
    }

    public Integer getUrt() {
        return this.urt;
    }

    public void setUrt(Integer urt) {
        this.urt = urt;
    }

    public String getLoginName() {
        return this.loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getCertificate() {
        return this.certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public Integer getCertificateType() {
        return this.certificateType;
    }

    public void setCertificateType(Integer certificateType) {
        this.certificateType = certificateType;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Boolean getHaveUnfinishedFollow() {
        return this.haveUnfinishedFollow;
    }

    public void setHaveUnfinishedFollow(Boolean haveUnfinishedFollow) {
        this.haveUnfinishedFollow = haveUnfinishedFollow;
    }

    public Integer getRelationPatientId() {
        return this.relationPatientId;
    }

    public void setRelationPatientId(Integer relationPatientId) {
        this.relationPatientId = relationPatientId;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getAuthStatus() {
        return this.authStatus;
    }

    public void setAuthStatus(Integer authStatus) {
        this.authStatus = authStatus;
    }

    public String getRealName() {
        return this.realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getAuthMsg() {
        return this.authMsg;
    }

    public void setAuthMsg(String authMsg) {
        this.authMsg = authMsg;
    }

    public String getUserIcon() {
        return this.userIcon;
    }

    public void setUserIcon(String userIcon) {
        this.userIcon = userIcon;
    }

    public Integer getCardType() {
        return this.cardType;
    }

    public void setCardType(Integer cardType) {
        this.cardType = cardType;
    }

    public HealthLogBean getHealthLogBean() {
        return this.healthLogBean;
    }

    public void setHealthLogBean(HealthLogBean healthLogBean) {
        this.healthLogBean = healthLogBean;
    }

    public ActionHabitBean getActionHabitBean() {
        return this.actionHabitBean;
    }

    public void setActionHabitBean(ActionHabitBean actionHabitBean) {
        this.actionHabitBean = actionHabitBean;
    }

    public LifeHabitBean getLifeHabitBean() {
        return this.lifeHabitBean;
    }

    public void setLifeHabitBean(LifeHabitBean lifeHabitBean) {
        this.lifeHabitBean = lifeHabitBean;
    }

    public String getCardId() {
        return this.cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public HealthInformationBean getHealthInformationBean() {
        return this.healthInformationBean;
    }

    public void setHealthInformationBean(HealthInformationBean healthInformationBean) {
        this.healthInformationBean = healthInformationBean;
    }

    public FamilyDoctorBean getFamilyDoctorBean() {
        return this.familyDoctorBean;
    }

    public void setFamilyDoctorBean(FamilyDoctorBean familyDoctorBean) {
        this.familyDoctorBean = familyDoctorBean;
    }

    public List<RelationLabelBean> getLabels() {
        return this.labels;
    }

    public void setLabels(List<RelationLabelBean> labels) {
        this.labels = labels;
    }

    public String getVaccineCardId() {
        return this.vaccineCardId;
    }

    public void setVaccineCardId(String vaccineCardId) {
        this.vaccineCardId = vaccineCardId;
    }

    public String getLinkCertificate() {
        return this.linkCertificate;
    }

    public void setLinkCertificate(String linkCertificate) {
        this.linkCertificate = linkCertificate;
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Integer getClientConfigId() {
        return this.clientConfigId;
    }

    public void setClientConfigId(Integer clientConfigId) {
        this.clientConfigId = clientConfigId;
    }

    public String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientType() {
        return this.clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public Integer getGuardianCertificateType() {
        return this.guardianCertificateType;
    }

    public void setGuardianCertificateType(Integer guardianCertificateType) {
        this.guardianCertificateType = guardianCertificateType;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGuardianSex() {
        return this.guardianSex;
    }

    public void setGuardianSex(String guardianSex) {
        this.guardianSex = guardianSex;
    }

    public Integer getGuardianAge() {
        return this.guardianAge;
    }

    public void setGuardianAge(Integer guardianAge) {
        this.guardianAge = guardianAge;
    }

    public String getAgeString() {
        return this.ageString;
    }

    public void setAgeString(String ageString) {
        this.ageString = ageString;
    }

    public String getGuardianAgeString() {
        return this.guardianAgeString;
    }

    public void setGuardianAgeString(String guardianAgeString) {
        this.guardianAgeString = guardianAgeString;
    }
}
