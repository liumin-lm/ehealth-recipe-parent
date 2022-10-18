package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * 对接药企 无需脱敏
 * @date 2019\3\14 0014 15:33
 */
@Schema
@Getter
@Setter
public class JztRecipeDTO implements Serializable {
    private static final long serialVersionUID = 2278967962954405876L;

    private String certificateType;

    private String certificate;

    private String patientName;

    private String patientTel;

    private String patientNumber;

    private String patientAddress;

    private String patientSex;

    private String recipeCode;

    private String clinicOrgan;

    private String recipeType;

    private String organId;

    private String organName;

    private String departId;

    private String departName;

    private String doctorNumber;

    private String doctorName;

    private String createDate;

    private String recipeFee;

    private String actualFee;

    private String couponFee;

    private String decoctionFee;

    private String medicalFee;

    private String expressFee;

    private String orderTotalFee;

    private String organDiseaseName;

    private String organDiseaseId;

    private String memo;

    private String payFlag;

    private String giveMode;

    private String giveUser;

    private String status;

    private String medicalPayFlag;

    private String distributionFlag;

    private String distributorCode;

    private String distributorName;

    private String pharmacyCode;

    private String pharmacyName;

    private String recipeMemo;

    private String tcmUsePathways;

    private String tcmUsingRate;

    private String tcmNum;

    //处方笺(base64 图片)
    private String signImg;

    private List<JztDrugDTO> drugList;

}
