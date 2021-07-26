package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\11 0011 10:03
 */
@Schema
@Data
public class HisRecipeVO implements Serializable{
    private static final long serialVersionUID = 1496069628741110154L;
    @ItemProperty(alias = "处方序号")
    private Integer hisRecipeID; // int(11) NOT NULL AUTO_INCREMENT,
    @ItemProperty(alias = "挂号序号")
    private String registeredId; // varchar(30) DEFAULT NULL COMMENT '挂号序号',
    @ItemProperty(alias = "mpiId")
    private String mpiId;
    @ItemProperty(alias = "证件类型 1 身份证")
    private Integer certificateType; // tinyint(1) DEFAULT NULL COMMENT '证件类型 1 身份证',
    @ItemProperty(alias = "证件号")
    private String certificate; // varchar(18) DEFAULT NULL COMMENT '证件号',
    @ItemProperty(alias = "患者姓名")
    private String patientName; // varchar(32) NOT NULL COMMENT '患者姓名',
    @ItemProperty(alias = "患者手机号")
    private String patientTel; // varchar(20) DEFAULT NULL COMMENT '患者手机号',
    @ItemProperty(alias = "患者地址")
    private String patientAddress; // varchar(250) DEFAULT NULL COMMENT '患者地址',
    @ItemProperty(alias = "患者手机号")
    private String patientNumber; // varchar(20) DEFAULT NULL COMMENT '患者手机号',
    @ItemProperty(alias = "His处方单号")
    private String recipeCode; // varchar(50) DEFAULT NULL COMMENT 'His处方单号',
    @ItemProperty(alias = "处方金额")
    private BigDecimal recipeFee; // decimal(10,2) DEFAULT NULL COMMENT '处方金额',
    @ItemProperty(alias = "开方机构序号")
    private  Integer clinicOrgan; // int(11) DEFAULT NULL COMMENT '开方机构序号',
    @ItemProperty(alias = "机构名称")
    private String organName; // varchar(50) DEFAULT NULL COMMENT '机构名称',
    @ItemProperty(alias = "1西药  2中成药 3 草药")
    private Integer recipeType; // tinyint(1) DEFAULT NULL COMMENT '1西药  2中成药 3 草药',
    @ItemProperty(alias = "科室编码")
    private String departCode; // varchar(10) DEFAULT NULL COMMENT '科室编码',
    @ItemProperty(alias = "科室名称")
    private String departName; // varchar(20) DEFAULT NULL COMMENT '科室名称',
    @ItemProperty(alias = "开方医生工号")
    private String doctorCode; // varchar(20) DEFAULT NULL COMMENT '开方医生工号',
    @ItemProperty(alias = "开方医生姓名")
    private String doctorName; // varchar(50) DEFAULT NULL COMMENT '开方医生姓名',
    @ItemProperty(alias = "创建时间")
    private Date createDate; // datetime DEFAULT NULL,
    @ItemProperty(alias = "诊断编码")
    private String disease; // varchar(50) DEFAULT NULL COMMENT '诊断编码',
    @ItemProperty(alias = "诊断编码")
    private String diseaseName; // varchar(50) DEFAULT NULL COMMENT '诊断名称',
    @ItemProperty(alias = "诊断备注")
    private String memo; // varchar(255) DEFAULT NULL COMMENT '诊断备注',
    @ItemProperty(alias = "处方备注")
    private String recipeMemo; // varchar(250) DEFAULT NULL COMMENT '处方备注',
    @ItemProperty(alias = "1 未处理 2已处理")
    private Integer status; //tinyint(4) NOT NULL DEFAULT '0' COMMENT '1 未处理 2已处理',
    @ItemProperty(alias = "中药处方用法")
    private String tcmUsePathways; //varchar(30) DEFAULT NULL COMMENT '中药处方用法',
    @ItemProperty(alias = "中药处方用量")
    private String tcmUsingRate; // varchar(20) DEFAULT NULL COMMENT '中药处方用量',
    @ItemProperty(alias = "贴数")
    private String tcmNum; // int(11) DEFAULT '0' COMMENT '贴数',
    @ItemProperty(alias = "1 自费  2 医保")
    private Integer medicalType; // tinyint(4) NOT NULL DEFAULT '1' COMMENT '1 自费  2 医保',
    @ItemProperty(alias = "提示文本")
    private String showText; // varchar(250) DEFAULT NULL COMMENT '提示文本',
    @ItemProperty(alias = "是否外延处方")
    private Integer extensionFlag; // tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否外延处方 0 否  1 是',
    @ItemProperty(alias = "处方流水号")
    private String tradeNo; // varchar(50) DEFAULT NULL COMMENT '处方流水号',
    @ItemProperty(alias = "医保报销金额")
    private BigDecimal medicalAmount; // decimal(10,2) DEFAULT NULL COMMENT '医保报销金额',
    @ItemProperty(alias = "自费金额")
    private BigDecimal cashAmount; // decimal(10,2) DEFAULT NULL COMMENT '自费金额',
    @ItemProperty(alias = "总金额")
    private BigDecimal totalAmount; // decimal(10,2) DEFAULT NULL COMMENT '总金额',
    @ItemProperty(alias = "订单状态")
    private Integer orderStatus;
    @ItemProperty(alias = "订单状态描述")
    private String orderStatusText;
    @ItemProperty(alias = "处方来源")
    private Integer fromFlag;
    @ItemProperty(alias = "订单号")
    private String orderCode;
    private Integer jumpPageType;
    @ItemProperty(alias = "状态")
    private String statusText;
    @ItemProperty(alias = "诊断名称")
    private String organDiseaseName;
    @ItemProperty(alias = "药品详情")
    private List<HisRecipeDetailVO> recipeDetail;
    @ItemProperty(alias = "是否隐方")
    private Integer isCachePlatform;
    @ItemProperty(alias = "病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;
    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;


}
