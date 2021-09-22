package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RecipeAndOrderDetailBean implements Serializable{
    private static final long serialVersionUID = -1543466696121633673L;

    private String msg;
    private String certificateType;
    private String certificate;
    private String patientName;
    private String patientTel;
    private String patientAddress;
    private String patientNumber;
    private String recipeCode;
    private String recipeId;
    private String clinicOrgan;
    private String organId;
    private String organName;
    private String recipeType;
    private String departId;
    private String departName;
    private String doctorNumber;
    private String doctorName;
    private String createDate;
    private String recipeFee;
    private String actualFee;
    private String couponFee;
    private String decoctionFee;
    private String tcmFee;
    private String auditFee;
    private String registerFee;
    private String medicalFee;
    private String province;
    private String city;
    private String district;
    private String street;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String streetCode;
    private String expressFee;
    private String receiver;
    private String recMobile;
    private String recAddress;
    private String orderTotalFee;
    private String outTradeNo;
    private String tradeNo;
    private String organDiseaseName;
    private String organDiseaseId;
    private String memo;
    private String payMode;
    private String payFlag;
    private String giveMode;
    private String status;
    private String medicalPayFlag;
    private String distributionFlag;
    private String recipeMemo;
    private String tcmUsePathways;
    private String tcmUsingRate;
    private String tcmNum;
    private String decoctionId;
    private String decoctionText;
    private String pharmacyCode;
    private String pharmacyName;
    private String recipeSignImg;
    private String recipeSignImgUrl;
    private String decoctionFlag;
    private List<DrugListForThreeBean> drugList;

}
