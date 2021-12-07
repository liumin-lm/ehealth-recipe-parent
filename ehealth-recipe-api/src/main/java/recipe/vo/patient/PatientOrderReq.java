package recipe.vo.patient;


import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @description： 患者端下单入参
 * @author： whf
 * @date： 2021-12-06 10:31
 */
@Getter
@Setter
public class PatientOrderReq implements Serializable {


    @ItemProperty(alias = "处方id")
    private List<Integer> recipeIds;

    @ItemProperty(alias = "支付方式")
    private String payWay;

    @ItemProperty(alias = "当前操作者编号")
    private String operMpiId;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "支付方式")
    private Integer payMode;

    @ItemProperty(alias = "地址编号")
    private Integer addressID;

    @ItemProperty(alias = "煎法")
    private Integer decoctionId;

    @ItemProperty(alias = "1：表示需要制作费，0：不需要")
    private Integer gfFeeFlag;

    @ItemProperty(alias = "药企ID")
    private Integer depId;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "应用ID")
    private String appId;

    @ItemProperty(alias = "购药方式key")
    private String giveModeKey;

    @ItemProperty(alias = "优惠券ID 无优惠券为0")
    private Integer couponId;

    @ItemProperty(alias = "以下为钥世圈字段，跳转链接时需要带上 药店编码")
    private String gysCode;

    @ItemProperty(alias = "以下为钥世圈字段，跳转链接时需要带上 药店名称")
    private String gysName;

    @ItemProperty(alias = "以下为钥世圈字段，跳转链接时需要带上 药店地址")
    private String gysAddr;

    @ItemProperty(alias = "处方费用")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "送货方式")
    private Integer sendMethod;

    @ItemProperty(alias = "payMethod     0：线下支付   1：在线支付  2：两者皆可")
    private Integer payMethod;

    @ItemProperty(alias = "如有药店则使用药店编码，区分 depId")
    private String pharmacyCode;

    @ItemProperty(alias = "药店地址")
    private String address;

    @ItemProperty(alias = "药企名称")
    private String depName;

    @ItemProperty(alias = "物流公司")
    private Integer logisticsCompany;

    @ItemProperty(alias = "是否是还是返回的药企")
    private Boolean hisDep;

    @ItemProperty(alias = "his的药企code")
    private String hisDepCode;

    @ItemProperty(alias = "his的药企处方金额")
    private BigDecimal hisDepFee;

    @ItemProperty(alias = "期望配送日期")
    private String expectSendDate;

    @ItemProperty(alias = "期望配送时间")
    private String expectSendTime;

    @ItemProperty(alias = "预约取药开始时间")
    private String expectStartTakeTime;

    @ItemProperty(alias = "预约取药结束时间")
    private String expectEndTakeTime;

    @ItemProperty(alias = "1表示省医保")
    private Integer orderType;

    @ItemProperty(alias = "诊断疾病名称")
    private String organDiseaseName;

    @ItemProperty(alias = "诊断疾病编码")
    private String organDiseaseId;

    @ItemProperty(alias = "参保地统筹区")
    private String insuredArea;

    @ItemProperty(alias = "参保地统筹区")
    private Integer calculateFee;



}
