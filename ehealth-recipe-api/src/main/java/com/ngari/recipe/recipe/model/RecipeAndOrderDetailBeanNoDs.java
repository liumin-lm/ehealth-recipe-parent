package com.ngari.recipe.recipe.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;
import recipe.vo.greenroom.InvoiceRecordVO;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 下载处方签所需实体，无需脱敏
 */
@Data
public class RecipeAndOrderDetailBeanNoDs implements Serializable {
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
    private String communityCode;
    private String communityName;
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
    private String pharmNo;
    private InvoiceRecordVO invoiceRecord;
    @ItemProperty(alias = "药店所属公司编码")
    private String companyCode;
    @ItemProperty(alias = "药店所属公司名称")
    private String companyName;
    /**
     * 出生日期
     */
    @Temporal(TemporalType.DATE)
    private Date birthday;

    /**
     * 病人性别
     */
    //后续病人性别如果要对外不使用这个字段
    private String sexCode;
    //后续病人性别如果要对外使用这个字段
    private String gender;
    private String sexName;


    @ItemProperty(alias = "物流公司")
    private Integer logisticsCompany;

    private String logisticsCompanyName;

    @ItemProperty(alias = "快递单号")
    private String trackingNumber;

    @ItemProperty(alias = "处方剂型类型 1 饮片方 2 颗粒方")
    private Integer recipeDrugForm;

    @ItemProperty(alias = "代煎帖数")
    private Integer decoctionNum;

    @Temporal(TemporalType.DATE)
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Shanghai"
    )
    public Date getBirthday() {
        return this.birthday;
    }
}
