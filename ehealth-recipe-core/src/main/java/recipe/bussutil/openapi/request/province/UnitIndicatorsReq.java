package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;


/**
 * 机构指标数据导入
 */
public class UnitIndicatorsReq implements java.io.Serializable {

    private static final long serialVersionUID = 2070331011103087301L;

    @NotNull
    private String organID; //	机构唯一序号
    @NotNull
    private String organName; // 机构名称 校验：开展互联网诊疗业务的机构名称与行政审批平台备案的名称是否一致
    private String secondName; // 机构第二名称
    private String introduction;//简介
    @NotNull
    private String internetPermit; // 是否批准为互联网医院  校验：开展互联网诊疗业务的机构是否在卫健委行政审批平台审批通过
    private String registerNumber; // 机构登记号
    private String levelId; //	医院级别Id
    private String categoryId; // 机构类别Id
    private String grade; //医院等次


    private String doctorProtocol;//医生端用户协议
    private String userProtocol;//用户端用户协议

    private String legalRepresentative; //	法定代表人
    private String chargeRepresentative; // 主要负责人
    @NotNull
    private String areaCode; //地区编码
    private String address; // 地址
    private String postcode; // 邮编
    private String areaName; // 医院所在行政区划名称
    private String authority; // 发证机关
    private Date   passDate; // 批准日期
    private String passCode; //批准文号
    private Date   beginDate; //	有效开始日期
    private Date   endDate; // 有效截止日期
    private String telNum; // 电话
    @NotNull
    private Date   updateTime; // 最后更新时间
    private String  unitID;

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getInternetPermit() {
        return internetPermit;
    }

    public void setInternetPermit(String internetPermit) {
        this.internetPermit = internetPermit;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getDoctorProtocol() {
        return doctorProtocol;
    }

    public void setDoctorProtocol(String doctorProtocol) {
        this.doctorProtocol = doctorProtocol;
    }

    public String getUserProtocol() {
        return userProtocol;
    }

    public void setUserProtocol(String userProtocol) {
        this.userProtocol = userProtocol;
    }

    public String getLegalRepresentative() {
        return legalRepresentative;
    }

    public void setLegalRepresentative(String legalRepresentative) {
        this.legalRepresentative = legalRepresentative;
    }

    public String getChargeRepresentative() {
        return chargeRepresentative;
    }

    public void setChargeRepresentative(String chargeRepresentative) {
        this.chargeRepresentative = chargeRepresentative;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Date getPassDate() {
        return passDate;
    }

    public void setPassDate(Date passDate) {
        this.passDate = passDate;
    }

    public String getPassCode() {
        return passCode;
    }

    public void setPassCode(String passCode) {
        this.passCode = passCode;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getTelNum() {
        return telNum;
    }

    public void setTelNum(String telNum) {
        this.telNum = telNum;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }
}
