package com.ngari.recipe.dto;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class HisRecipeExtDTO {
    @ItemProperty(alias = "处方ID")
    private Integer recipeId;

    @ItemProperty(alias = "挂号序号")
    private String registerID;

    @ItemProperty(alias = "主诉")
    private String mainDieaseDescribe;

    @ItemProperty(alias = "现病史")
    private String currentMedical;

    @ItemProperty(alias = "既往史")
    private String histroyMedical;

    @ItemProperty(alias = "过敏史")
    private String allergyMedical;

    @ItemProperty(alias = "发病日期")
    private Date onsetDate;

    /**
     * 以下为互联网医院字段
     */
    @ItemProperty(alias = "现病史")
    private String historyOfPresentIllness;

    @ItemProperty(alias = "处理方法")
    private String handleMethod;

    @ItemProperty(alias = "体格检查")
    private String physicalCheck;
    /**
     * 为互联网医院字段
     */
    @ItemProperty(alias = "HIS处方关联的卡类型")
    private String cardTypeName;

    @ItemProperty(alias = "HIS处方关联的卡类型")
    private Integer cardType;

    @ItemProperty(alias = "HIS处方关联的卡号")
    private String cardNo;

    @ItemProperty(alias = "患者类型 自费 0 商保 1 普通医保 2 慢病医保 3 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A")
    private String patientType;

    @ItemProperty(alias = "his返回的配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "his返回的配送药企名称")
    private String deliveryName;

    @ItemProperty(alias = "医保返回的医院机构编码")
    private String hospOrgCodeFromMedical;

    @ItemProperty(alias = "参保地统筹区")
    private String insuredArea;

    @ItemProperty(alias = "医保结算请求串")
    private String medicalSettleData;

    @ItemProperty(alias = "门诊挂号序号（医保）")
    private String registerNo;

    @ItemProperty(alias = "HIS收据号（医保）")
    private String hisSettlementNo;

    @ItemProperty(alias = "处方预结算返回支付总金额")
    private String preSettleTotalAmount;

    @ItemProperty(alias = "处方预结算返回医保支付金额")
    private String fundAmount;

    @ItemProperty(alias = "处方预结算返回自费金额")
    private String cashAmount;

    @ItemProperty(alias = "开处方页面病种选择开关标识")
    private Integer recipeChooseChronicDisease;
    @ItemProperty(alias = "病种标识")
    @Dictionary(id = "eh.cdr.dictionary.ChronicDiseaseFlag")
    private String chronicDiseaseFlag;
    @ItemProperty(alias = "病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;
    @ItemProperty(alias = "并发症")
    private String complication;

    @ItemProperty(alias = "用药医嘱")
    private String drugEntrustment;

    //用户页面选择
    @ItemProperty(alias = "是否长处方")
    private String isLongRecipe;

    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;

    @ItemProperty(alias = "电子处方监管平台流水号")
    private String superviseRecipecode;

    @ItemProperty(alias = "第三方处方ID")
    private String rxid;
    @ItemProperty(alias = "电子票号")
    private String einvoiceNumber;

    @ItemProperty(alias = "制法")
    private String makeMethod;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每贴次数")
    private String everyTcmNumFre;
    @ItemProperty(alias = "服用要求")
    private String requirementsForTakingId;
    @ItemProperty(alias = "服用要求code")
    private String requirementsForTakingCode;
    @ItemProperty(alias = "服用要求text")
    private String requirementsForTakingText;
    @ItemProperty(alias = "每付取汁")
    private String juice;
    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;
    @ItemProperty(alias = "次量")
    private String minor;
    @ItemProperty(alias = "次量单位")
    private String minorUnit;
    @ItemProperty(alias = "中医症候编码")
    private String symptomCode;
    @ItemProperty(alias = "中医症候名称")
    private String symptomName;
    @ItemProperty(alias = "煎法")
    private String decoctionCode;
    @ItemProperty(alias = "煎法text")
    private String decoctionText;
    @ItemProperty(alias = "医保备案号")
    private String putOnRecordID;

    @ItemProperty(alias = "天猫返回处方编号")
    private String rxNo;

    @ItemProperty(alias = "his返回的取药方式1配送到家 2医院取药 3两者都支持")
    private String giveModeFormHis;

    @ItemProperty(alias = "his返回的处方总金额")
    private String deliveryRecipeFee;

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;
    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

    /**
     * 医生医保编码
     */
    private String doctorMedicalNo;

    @ItemProperty(alias = "制法")
    private String makeMethodId;

    @ItemProperty(alias = "中医症候编码")
    private String symptomId;

    @ItemProperty(alias = "煎法")
    private String decoctionId;

    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;

    @ItemProperty(alias = "病历索引Id")
    private Integer docIndexId;

    @ItemProperty(alias = "皮肤反应测验")
    private String skinTest;

    @ItemProperty(alias = "处方预结算返回应付金额")
    private String payAmount;

    @ItemProperty(alias = "处方来源 0 线下his同步 1 平台处方")
    private Integer fromFlag;

    @ItemProperty(alias = "监管人姓名")
    private String guardianName;

    @ItemProperty(alias = "监管人证件号")
    private String guardianCertificate;

    @ItemProperty(alias = "监管人手机号")
    private String guardianMobile;

    @ItemProperty(alias = "his处方付费序号合集")
    private String recipeCostNumber;

    @ItemProperty(alias = "用药说明")
    private String medicationInstruction;

    @ItemProperty(alias = "ca签名ID")
    private String caUniqueID;

    @ItemProperty(alias = "药师ca签名ID")
    private String checkCAUniqueID;

    @ItemProperty(alias = "诊断序号")
    private String hisDiseaseSerial;

    @ItemProperty(alias = "病历号")
    private String medicalRecordNumber;

    @ItemProperty(alias = "卡类型串")
    private String cardTypeStr;

    @ItemProperty(alias = "大病类型")
    private String illnessType;
    @ItemProperty(alias = "大病类型名称")
    private String illnessName;
}
