package com.ngari.recipe.hisprescription.model;


import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.schema.annotation.*;
import recipe.vo.second.EmrDetailValueVO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 浙江互联网医院查询处方详情dto
 * created by shiyuping on 2018/11/30
 */
@Schema
public class QueryRecipeInfoDTO implements Serializable {
    private static final long serialVersionUID = -708693213105234237L;
    @ItemProperty(alias = "院区代码，示例：A001 院区A，A002院区B")
    private String hoscode;
    @ItemProperty(alias = "平台机构id")
    private String organId;
    @ItemProperty(alias = "卡类型（1医院就诊卡  2医保卡3 医院病历号）")
    private String cardType;
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    @ItemProperty(alias = "卡(病历)号码")
    private String cardNo;
    @Desensitizations(type = DesensitizationsType.IDCARD)
    @ItemProperty(alias = "患者身份证")
    private String certID;
    @ItemProperty(alias = "患者姓名")
    private String patientName;
    @ItemProperty(alias = "患者性别")
    private String patientSex;
    @ItemProperty(alias = "手机号码")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String mobile;
    @ItemProperty(alias = "复诊id")
    private String clinicID;
    @ItemProperty(alias = "挂号序号")
    private String registerId;
    @ItemProperty(alias = "处方医生工号")
    private String doctorID;
    @ItemProperty(alias = "医生身份证")
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String doctorIDCard;
    @ItemProperty(alias = "处方医生姓名")
    private String doctorName;
    @ItemProperty(alias = "开单行政科室代码")
    private String deptID;
    @ItemProperty(alias = "开单挂号科室代码")
    private String deptCode;
    @ItemProperty(alias = "开单科室名称")
    private String deptName;
    @ItemProperty(alias = "处方审核医生")
    private String auditDoctor;
    @ItemProperty(alias = "处方类型 1 西药 2 成药 3 草药")
    private String recipeType;
    @ItemProperty(alias = "处方日期")
    private Date datein;
    @ItemProperty(alias = "是否已经支付1支付 0未支付")
    private String isPay;
    @ItemProperty(alias = "配送方式 0医院取药 1物流配送 2药店取药")
    private String deliveryType;
    @ItemProperty(alias = "平台处方号")
    private String recipeID;
    @ItemProperty(alias = "平台处方id")
    private String platRecipeID;
    @ItemProperty(alias = "医院诊断内码")
    private String icdRdn;
    @ItemProperty(alias = "ICD诊断码")
    private String icdCode;
    @ItemProperty(alias = "ICD名称")
    private String icdName;
    @ItemProperty(alias = "简要病史(病史摘要)")
    private String diseasesHistory;
    @ItemProperty(alias = "本处方收费类型 1市医保 2省医保 3自费")
    private String medicalPayFlag;
    @ItemProperty(alias = "处方总金额")
    private String recipeFee;
    @ItemProperty(alias = "处方列表数据")
    private List<OrderItemDTO> orderList;
    @ItemProperty(alias = "既往史结构体数据集")
    private PastHistoryInfoDTO pastHistoryInfo;
    @ItemProperty(alias = "婚育史结构体数据集")
    private MCHistoryInfoDTO mcHistoryInfo;
    @ItemProperty(alias = "家族史结构体数")
    private FamilyHistoryInfoDTO familyHistoryInfo;
    @ItemProperty(alias = "月经史结构体数据集")
    private MenstrualHistoryInfoDTO menstrualHistoryInfo;

    @ItemProperty(alias = "病⼈主诉")
    private String BRZS;
    @ItemProperty(alias = "现病史")
    private String XBS;
    @ItemProperty(alias = "处理⽅法")
    private String CLFF;
    @ItemProperty(alias = "体格检查")
    private String TGJC;

    @ItemProperty(alias = "审核状态")
    private String auditCheckStatus;

    @ItemProperty(alias = "自付比例")
    private String payScale;

    //date 20200222杭州市互联网 诊断信息
    private List<DiseaseInfo> diseaseInfo;

    //date 20200222杭州市互联网 配送信息
    private List<DeliveryInfo> deliveryInfo;

    //date 20200222杭州市互联网 审方时间
    @ItemProperty(alias = "审方时间")
    private Date checkDate;
    private RecipeExtendBean recipeExtendBean;


    @ItemProperty(alias = "患者年龄")
    private Integer patinetAge;

    @ItemProperty(alias = "药品总数量")
    private Double drugTotalNumber;

    @ItemProperty(alias = "药品总金额")
    private BigDecimal drugTotalAmount;

    @ItemProperty(alias = "就诊人信息")
    private UserInfoDTO userInfo;

    @ItemProperty(alias = "门诊患者编号")
    private String patientID;

    @ItemProperty(alias = "父医嘱序号")
    private String parentOrderNo;

    @ItemProperty(alias = "处方备注，医嘱正文")
    private String recipeMemo;

    @ItemProperty(alias = "开方时间,开医嘱日期时间")
    private Date createDate;

    @ItemProperty(alias = "诊断备注，医嘱备注")
    private String memo;

    @ItemProperty(alias = "处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;

    @ItemProperty(alias = "收费项目-处方（药品）")
    private String charge;

    @ItemProperty(alias = "电子病历返回")
    private Map<String, Object> medicalInfoBean;


    @ItemProperty(
            alias = "机构疾病"
    )
    private List<EmrDetailValueVO> diseaseValue;
    @ItemProperty(
            alias = "中医症候"
    )
    private List<EmrDetailValueVO> symptomValue;

    @ItemProperty(
            alias = "证件类型"
    )
    private Integer certificateType;

    public Map<String, Object> getMedicalInfoBean() {
        return medicalInfoBean;
    }

    public void setMedicalInfoBean(Map<String, Object> medicalInfoBean) {
        this.medicalInfoBean = medicalInfoBean;
    }

    public String getCharge() {
        return charge;
    }

    public void setCharge(String charge) {
        this.charge = charge;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    public String getParentOrderNo() {
        return parentOrderNo;
    }

    public void setParentOrderNo(String parentOrderNo) {
        this.parentOrderNo = parentOrderNo;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public UserInfoDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoDTO userInfo) {
        this.userInfo = userInfo;
    }

    public Integer getPatinetAge() {
        return patinetAge;
    }

    public Double getDrugTotalNumber() {
        return drugTotalNumber;
    }

    public void setDrugTotalNumber(Double drugTotalNumber) {
        this.drugTotalNumber = drugTotalNumber;
    }

    public BigDecimal getDrugTotalAmount() {
        return drugTotalAmount;
    }

    public void setDrugTotalAmount(BigDecimal drugTotalAmount) {
        this.drugTotalAmount = drugTotalAmount;
    }

    public void setPatinetAge(Integer patinetAge) {
        this.patinetAge = patinetAge;
    }


    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
    }

    public List<DiseaseInfo> getDiseaseInfo() {
        return diseaseInfo;
    }

    public void setDiseaseInfo(List<DiseaseInfo> diseaseInfo) {
        this.diseaseInfo = diseaseInfo;
    }

    public List<DeliveryInfo> getDeliveryInfo() {
        return deliveryInfo;
    }

    public void setDeliveryInfo(List<DeliveryInfo> deliveryInfo) {
        this.deliveryInfo = deliveryInfo;
    }

    public QueryRecipeInfoDTO() {
    }

    public String getBRZS() {
        return BRZS;
    }

    public void setBRZS(String BRZS) {
        this.BRZS = BRZS;
    }

    public String getXBS() {
        return XBS;
    }

    public void setXBS(String XBS) {
        this.XBS = XBS;
    }

    public String getCLFF() {
        return CLFF;
    }

    public void setCLFF(String CLFF) {
        this.CLFF = CLFF;
    }

    public String getTGJC() {
        return TGJC;
    }

    public void setTGJC(String TGJC) {
        this.TGJC = TGJC;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getHoscode() {
        return hoscode;
    }

    public void setHoscode(String hoscode) {
        this.hoscode = hoscode;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getCertID() {
        return certID;
    }

    public void setCertID(String certID) {
        this.certID = certID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getClinicID() {
        return clinicID;
    }

    public void setClinicID(String clinicID) {
        this.clinicID = clinicID;
    }

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }

    public String getDeptID() {
        return deptID;
    }

    public void setDeptID(String deptID) {
        this.deptID = deptID;
    }

    public String getAuditDoctor() {
        return auditDoctor;
    }

    public void setAuditDoctor(String auditDoctor) {
        this.auditDoctor = auditDoctor;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public Date getDatein() {
        return datein;
    }

    public void setDatein(Date datein) {
        this.datein = datein;
    }

    public String getIsPay() {
        return isPay;
    }

    public void setIsPay(String isPay) {
        this.isPay = isPay;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getIcdRdn() {
        return icdRdn;
    }

    public void setIcdRdn(String icdRdn) {
        this.icdRdn = icdRdn;
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

    public String getDiseasesHistory() {
        return diseasesHistory;
    }

    public void setDiseasesHistory(String diseasesHistory) {
        this.diseasesHistory = diseasesHistory;
    }

    public List<OrderItemDTO> getOrderList() {
        return orderList;
    }

    public void setOrderList(List<OrderItemDTO> orderList) {
        this.orderList = orderList;
    }

    public String getPlatRecipeID() {
        return platRecipeID;
    }

    public void setPlatRecipeID(String platRecipeID) {
        this.platRecipeID = platRecipeID;
    }

    public String getMedicalPayFlag() {
        return medicalPayFlag;
    }

    public void setMedicalPayFlag(String medicalPayFlag) {
        this.medicalPayFlag = medicalPayFlag;
    }

    public String getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(String recipeFee) {
        this.recipeFee = recipeFee;
    }

    public PastHistoryInfoDTO getPastHistoryInfo() {
        return pastHistoryInfo;
    }

    public void setPastHistoryInfo(PastHistoryInfoDTO pastHistoryInfo) {
        this.pastHistoryInfo = pastHistoryInfo;
    }

    public MCHistoryInfoDTO getMcHistoryInfo() {
        return mcHistoryInfo;
    }

    public void setMcHistoryInfo(MCHistoryInfoDTO mcHistoryInfo) {
        this.mcHistoryInfo = mcHistoryInfo;
    }

    public FamilyHistoryInfoDTO getFamilyHistoryInfo() {
        return familyHistoryInfo;
    }

    public void setFamilyHistoryInfo(FamilyHistoryInfoDTO familyHistoryInfo) {
        this.familyHistoryInfo = familyHistoryInfo;
    }

    public MenstrualHistoryInfoDTO getMenstrualHistoryInfo() {
        return menstrualHistoryInfo;
    }

    public void setMenstrualHistoryInfo(MenstrualHistoryInfoDTO menstrualHistoryInfo) {
        this.menstrualHistoryInfo = menstrualHistoryInfo;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getAuditCheckStatus() {
        return auditCheckStatus;
    }

    public void setAuditCheckStatus(String auditCheckStatus) {
        this.auditCheckStatus = auditCheckStatus;
    }

    public String getPayScale() {
        return payScale;
    }

    public void setPayScale(String payScale) {
        this.payScale = payScale;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getDoctorIDCard() {
        return doctorIDCard;
    }

    public void setDoctorIDCard(String doctorIDCard) {
        this.doctorIDCard = doctorIDCard;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public RecipeExtendBean getRecipeExtendBean() {
        return recipeExtendBean;
    }

    public void setRecipeExtendBean(RecipeExtendBean recipeExtendBean) {
        this.recipeExtendBean = recipeExtendBean;
    }

    public List<EmrDetailValueVO> getDiseaseValue() {
        return diseaseValue;
    }

    public void setDiseaseValue(List<EmrDetailValueVO> diseaseValue) {
        this.diseaseValue = diseaseValue;
    }

    public List<EmrDetailValueVO> getSymptomValue() {
        return symptomValue;
    }

    public void setSymptomValue(List<EmrDetailValueVO> symptomValue) {
        this.symptomValue = symptomValue;
    }

    public Integer getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(Integer certificateType) {
        this.certificateType = certificateType;
    }
}
