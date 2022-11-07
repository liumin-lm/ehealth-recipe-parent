package com.ngari.recipe.dto;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RecipeBeforeOrderDTO implements Serializable {
    private static final long serialVersionUID = -4896108391623327201L;

    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "处方编码")
    private String recipeCode;

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "药企ID")
    private Integer enterpriseId;

    @ItemProperty(alias = "机构或药企名称")
    private String organName;

    @ItemProperty(alias = "机构或药企电话")
    private String organPhone;

    @ItemProperty(alias = "取药药店或站点名称")
    private String drugStoreName;

    @ItemProperty(alias = "取药药店或站点编码")
    private String drugStoreCode;

    @ItemProperty(alias = "取药药店或站点地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "取药药店或站点电话号")
    private String drugStorePhone;

    @ItemProperty(alias = "配送地址id")
    private Integer addressId;

    @ItemProperty(alias = "完整地址")
    private String completeAddress;

    @ItemProperty(alias = "收货人")
    @Desensitizations(type = DesensitizationsType.NAME)
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String recMobile;

    @ItemProperty(alias = "收货人电话")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String recTel;

    @ItemProperty(alias = "地址（省）")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    private String address2;

    @ItemProperty(alias = "地址（区）")
    private String address3;

    @ItemProperty(alias = "地址（街道）")
    private String streetAddress;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "社区编码")
    private String address5;

    @ItemProperty(alias = "社区名称")
    private String address5Text;

    @ItemProperty(alias = "当前地址是否可进行配送")
    private Boolean addressCanSend;

    @ItemProperty(alias = "邮政编码")
    private String zipCode;

    @ItemProperty(alias = "是否已完善  0 否，1 是")
    private Integer isReady;

    @ItemProperty(alias = "删除标识，0：正常，1：删除")
    private Integer deleteFlag;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date updateTime;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "支付方式")
    private String payWay;

    @ItemProperty(alias = "操作人mpiId")
    private String operMpiId;

    @ItemProperty(alias = "0 无 1 药店取药，2 站点取药")
    private Integer takeMedicineWay;

    @ItemProperty(alias = "订单所属配送方式")
    private String giveModeKey;

    @ItemProperty(alias = "患者购药方式文本")
    private String giveModeText;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;

    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;

    @ItemProperty(alias = "审方费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "处方费")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "配送费支付方式 1-在线支付 2-线下支付 3-第三方支付 4-上传运费细则标准")
    private Integer expressFeePayWay;

    @ItemProperty(alias = "配送费付款方式ExpressFeePayMethodEnum")
    private Integer expressFeePayMethod;

    @ItemProperty(alias = "是否已锁定  0 否，1 是")
    private Integer isLock;
}
