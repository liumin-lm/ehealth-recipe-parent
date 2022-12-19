package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@Entity
@Schema
@Table(name = "cdr_fast_recipe")
@Access(AccessType.PROPERTY)
public class FastRecipe {

    @ItemProperty(alias = "药方Id, 主键")
    private Integer id;

    @ItemProperty(alias = "排序序号")
    private Integer orderNum;

    @ItemProperty(alias = "药方说明")
    private String introduce;

    @ItemProperty(alias = "药方名称")
    private String title;

    @ItemProperty(alias = "药方图片")
    private String backgroundImg;

    @ItemProperty(alias = "处方类型")
    private Integer recipeType;

    @ItemProperty(alias = "销售数量上限")
    private Integer maxNum;

    @ItemProperty(alias = "销售数量下限")
    private Integer minNum;

    @ItemProperty(alias = "0-删除，1-上架，2-下架")
    private Integer status;

    @ItemProperty(alias = "最后需支付费用")
    private BigDecimal actualPrice;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    @ItemProperty(alias = "来源标志")
    private Integer fromFlag;

    @ItemProperty(alias = "诊断备注")
    private String memo;

    @ItemProperty(alias = "中药嘱托")
    private String recipeMemo;

    @ItemProperty(alias = "制法")
    private String makeMethodId;

    @ItemProperty(alias = "制法text")
    private String makeMethodText;

    @ItemProperty(alias = "每付取汁")
    private String juice;

    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;

    @ItemProperty(alias = "次量")
    private String minor;

    @ItemProperty(alias = "次量单位")
    private String minorUnit;

    @ItemProperty(alias = "中医症候编码")
    private String symptomId;

    @ItemProperty(alias = "中医症候名称")
    private String symptomName;

    @ItemProperty(alias = "煎法")
    private String decoctionId;

    @ItemProperty(alias = "煎法text")
    private String decoctionText;

    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;

    @ItemProperty(alias = "线下处方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "电子病历文本")
    private String docText;

    @ItemProperty(alias = "是否需要问卷，0不需要，1需要")
    private Integer needQuestionnaire;

    @ItemProperty(alias = "问卷链接")
    private String questionnaireUrl;

    @ItemProperty(alias = "医生选择是否代煎（ 0:否 1：是）")
    private String doctorIsDecoction;

    @ItemProperty(alias = "每贴次数")
    private String everyTcmNumFre;

    @ItemProperty(alias = "服用要求")
    private String requirementsForTakingId;

    @ItemProperty(alias = "服用要求code")
    private String requirementsForTakingCode;

    @ItemProperty(alias = "服用要求text")
    private String requirementsForTakingText;

    @ItemProperty(alias = "处方剂型类型 1 饮片方 2 颗粒方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeDrugForm")
    private Integer recipeDrugForm;

    @ItemProperty(alias = "单复方表示：0无状态，1单方，2复方")
    private Integer singleOrCompoundRecipe;

    @ItemProperty(alias = "代煎帖数")
    private Integer decoctionNum;

    @ItemProperty(alias = "处方支持的购药方式,逗号分隔")
    private String recipeSupportGiveMode;

    @ItemProperty(alias = "配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "配送药企名称")
    private String deliveryName;

    @ItemProperty(alias = "处方指定药企类型: 1医院, 2药企, 默认0")
    private Integer appointEnterpriseType;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "order_num")
    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    @Column
    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    @Column
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(name = "background_img")
    public String getBackgroundImg() {
        return backgroundImg;
    }

    public void setBackgroundImg(String backgroundImg) {
        this.backgroundImg = backgroundImg;
    }

    @Column(name = "recipe_type")
    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    @Column(name = "max_num")
    public Integer getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(Integer maxNum) {
        this.maxNum = maxNum;
    }

    @Column(name = "min_num")
    public Integer getMinNum() {
        return minNum;
    }

    public void setMinNum(Integer minNum) {
        this.minNum = minNum;
    }

    @Column
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "actual_price")
    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }

    @Column(name = "clinic_organ")
    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    @Column(name = "organ_name")
    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    @Column(name = "copy_num")
    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    @Column(name = "total_money")
    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    @Column(name = "give_mode")
    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    @Column(name = "from_flag")
    public Integer getFromFlag() {
        return fromFlag;
    }

    public void setFromFlag(Integer fromFlag) {
        this.fromFlag = fromFlag;
    }

    @Column
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "make_method_id")
    public String getMakeMethodId() {
        return makeMethodId;
    }

    public void setMakeMethodId(String makeMethodId) {
        this.makeMethodId = makeMethodId;
    }

    @Column(name = "make_Method_text")
    public String getMakeMethodText() {
        return makeMethodText;
    }

    public void setMakeMethodText(String makeMethodText) {
        this.makeMethodText = makeMethodText;
    }

    @Column
    public String getJuice() {
        return juice;
    }

    public void setJuice(String juice) {
        this.juice = juice;
    }

    @Column(name = "juice_unit")
    public String getJuiceUnit() {
        return juiceUnit;
    }

    public void setJuiceUnit(String juiceUnit) {
        this.juiceUnit = juiceUnit;
    }

    @Column
    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    @Column(name = "minor_unit")
    public String getMinorUnit() {
        return minorUnit;
    }

    public void setMinorUnit(String minorUnit) {
        this.minorUnit = minorUnit;
    }

    @Column(name = "symptom_id")
    public String getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(String symptomId) {
        this.symptomId = symptomId;
    }

    @Column(name = "symptom_name")
    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    @Column(name = "decoction_id")
    public String getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(String decoctionId) {
        this.decoctionId = decoctionId;
    }

    @Column(name = "decoction_text")
    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    @Column(name = "decoction_price")
    public Double getDecoctionPrice() {
        return decoctionPrice;
    }

    public void setDecoctionPrice(Double decoctionPrice) {
        this.decoctionPrice = decoctionPrice;
    }

    @Column(name = "offline_recipe_name")
    public String getOfflineRecipeName() {
        return offlineRecipeName;
    }

    public void setOfflineRecipeName(String offlineRecipeName) {
        this.offlineRecipeName = offlineRecipeName;
    }

    @Column(name = "doc_text")
    public String getDocText() {
        return docText;
    }

    public void setDocText(String docText) {
        this.docText = docText;
    }

    @Column(name = "need_questionnaire")
    public Integer getNeedQuestionnaire() {
        return needQuestionnaire;
    }

    public void setNeedQuestionnaire(Integer needQuestionnaire) {
        this.needQuestionnaire = needQuestionnaire;
    }
    @Column(name = "questionnaire_url")
    public String getQuestionnaireUrl() {
        return questionnaireUrl;
    }

    public void setQuestionnaireUrl(String questionnaireUrl) {
        this.questionnaireUrl = questionnaireUrl;
    }

    @Column(name = "doctor_is_decoction")
    public String getDoctorIsDecoction() {
        return doctorIsDecoction;
    }

    public void setDoctorIsDecoction(String doctorIsDecoction) {
        this.doctorIsDecoction = doctorIsDecoction;
    }

    @Column(name = "every_tcm_num_fre")
    public String getEveryTcmNumFre() {
        return everyTcmNumFre;
    }

    public void setEveryTcmNumFre(String everyTcmNumFre) {
        this.everyTcmNumFre = everyTcmNumFre;
    }

    @Column(name = "recipe_memo")
    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    @Column(name = "requirements_for_taking_id")
    public String getRequirementsForTakingId() {
        return requirementsForTakingId;
    }

    public void setRequirementsForTakingId(String requirementsForTakingId) {
        this.requirementsForTakingId = requirementsForTakingId;
    }

    @Column(name = "requirements_for_taking_code")
    public String getRequirementsForTakingCode() {
        return requirementsForTakingCode;
    }

    public void setRequirementsForTakingCode(String requirementsForTakingCode) {
        this.requirementsForTakingCode = requirementsForTakingCode;
    }

    @Column(name = "requirements_for_taking_text")
    public String getRequirementsForTakingText() {
        return requirementsForTakingText;
    }

    public void setRequirementsForTakingText(String requirementsForTakingText) {
        this.requirementsForTakingText = requirementsForTakingText;
    }

    @Column(name = "recipe_drug_form")
    public Integer getRecipeDrugForm() {
        return recipeDrugForm;
    }

    public void setRecipeDrugForm(Integer recipeDrugForm) {
        this.recipeDrugForm = recipeDrugForm;
    }

    @Column(name = "single_or_compound_recipe")
    public Integer getSingleOrCompoundRecipe() {
        return singleOrCompoundRecipe;
    }

    public void setSingleOrCompoundRecipe(Integer singleOrCompoundRecipe) {
        this.singleOrCompoundRecipe = singleOrCompoundRecipe;
    }

    @Column(name = "decoction_num")
    public Integer getDecoctionNum() {
        return decoctionNum;
    }

    public void setDecoctionNum(Integer decoctionNum) {
        this.decoctionNum = decoctionNum;
    }

    @Column(name = "recipe_support_give_mode")
    public String getRecipeSupportGiveMode() {
        return recipeSupportGiveMode;
    }

    public void setRecipeSupportGiveMode(String recipeSupportGiveMode) {
        this.recipeSupportGiveMode = recipeSupportGiveMode;
    }

    @Column(name = "delivery_code")
    public String getDeliveryCode() {
        return deliveryCode;
    }

    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }

    @Column(name = "delivery_name")
    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    @Column(name = "appoint_enterprise_type")
    public Integer getAppointEnterpriseType() {
        return appointEnterpriseType;
    }

    public void setAppointEnterpriseType(Integer appointEnterpriseType) {
        this.appointEnterpriseType = appointEnterpriseType;
    }
}
