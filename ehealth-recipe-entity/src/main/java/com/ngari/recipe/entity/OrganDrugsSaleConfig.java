package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description： 机构药企销售配置
 * @author： whf
 * @date： 2022-01-10 14:59
 */
@Entity
@Schema
@Table(name = "organ_drugs_sale_config")
@Access(AccessType.PROPERTY)
@NoArgsConstructor
@DynamicInsert
public class OrganDrugsSaleConfig implements Serializable {

    @ItemProperty(alias = "自增主键")
    private Integer id;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "药企id")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "是否支持配送到站 0 不支持 1支持")
    private Integer isSupportSendToStation;

    @ItemProperty(alias = "自取支付方式 1 在线支付 2 线下支付")
    private Integer takeOneselfPayment;

    @ItemProperty(alias = "自取支付通道 1平台支付 2卫宁支付")
    private Integer takeOneselfPaymentChannel;

    @ItemProperty(alias = "自取预约时间 0 不预约 1预约当天 3 预约3天内 7预约7天内 15预约15天内")
    private Integer takeOneselfPlanDate;

    @ItemProperty(alias = "取药预约取药时间段配置：上午时间段")
    private String takeOneselfPlanAmTime;

    @ItemProperty(alias = "取药预约取药时间段配置：下午时间段")
    private String takeOneselfPlanPmTime;

    @ItemProperty(alias = "取药凭证 1不展示 2就诊卡号 3挂号序号 4患者id 5his单号 6发药流水号 7取药流水号 8病历号 9身份证号")
    private Integer takeDrugsVoucher;

    @ItemProperty(alias = "医保患者特殊提示")
    private String specialTips;

    @ItemProperty(alias = "是否调用药企发药机：0不调用，1调用")
    private Integer useDrugDispenserFlag;

    @ItemProperty(alias = "是否打印用法标签：0不打印，1打印")
    private Integer printUsageLabelFlag;

    @ItemProperty(alias = "是否打开发票申请：0不打开，1打开")
    private Integer invoiceRequestFlag;

    @ItemProperty(alias = "是否支持打印发票：0不支持，1支持")
    private Integer invoiceSupportFlag;

    @ItemProperty(alias = "退款申请中允许发药：0不允许，1允许")
    private Integer refundFeeisAllowSendDrug;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "修改时间")
    private Date modifiedTime;

    @ItemProperty(alias = "发药通知电话")
    private String sendDrugNotifyPhone;

    @ItemProperty(alias = "退费审核通知电话")
    private String refundNotifyPhone;

    @ItemProperty(alias = "是否走医院预结算：1是，0否")
    private Integer isHosDep;

    @ItemProperty(alias = "标准的收款方式  1 在线支付 2 货到付款 ")
    private Integer standardPaymentWay;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    @Column(name = "refund_fee_is_allow_send_drug")
    public Integer getRefundFeeisAllowSendDrug() {
        return refundFeeisAllowSendDrug;
    }

    public void setRefundFeeisAllowSendDrug(Integer refundFeeisAllowSendDrug) {
        this.refundFeeisAllowSendDrug = refundFeeisAllowSendDrug;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "drugs_enterprise_id")
    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }

    @Column(name = "is_support_send_to_station")
    public Integer getIsSupportSendToStation() {
        return isSupportSendToStation;
    }

    public void setIsSupportSendToStation(Integer isSupportSendToStation) {
        this.isSupportSendToStation = isSupportSendToStation;
    }

    @Column(name = "take_oneself_payment")
    public Integer getTakeOneselfPayment() {
        return takeOneselfPayment;
    }

    public void setTakeOneselfPayment(Integer takeOneselfPayment) {
        this.takeOneselfPayment = takeOneselfPayment;
    }

    @Column(name = "take_oneself_payment_channel")
    public Integer getTakeOneselfPaymentChannel() {
        return takeOneselfPaymentChannel;
    }

    public void setTakeOneselfPaymentChannel(Integer takeOneselfPaymentChannel) {
        this.takeOneselfPaymentChannel = takeOneselfPaymentChannel;
    }

    @Column(name = "take_oneself_plan_date")
    public Integer getTakeOneselfPlanDate() {
        return takeOneselfPlanDate;
    }

    public void setTakeOneselfPlanDate(Integer takeOneselfPlanDate) {
        this.takeOneselfPlanDate = takeOneselfPlanDate;
    }

    @Column(name = "take_oneself_plan_am_time")
    public String getTakeOneselfPlanAmTime() {
        return takeOneselfPlanAmTime;
    }

    public void setTakeOneselfPlanAmTime(String takeOneselfPlanAmTime) {
        this.takeOneselfPlanAmTime = takeOneselfPlanAmTime;
    }

    @Column(name = "take_oneself_plan_pm_time")
    public String getTakeOneselfPlanPmTime() {
        return takeOneselfPlanPmTime;
    }

    public void setTakeOneselfPlanPmTime(String takeOneselfPlanPmTime) {
        this.takeOneselfPlanPmTime = takeOneselfPlanPmTime;
    }

    @Column(name = "take_drugs_voucher")
    public Integer getTakeDrugsVoucher() {
        return takeDrugsVoucher;
    }

    public void setTakeDrugsVoucher(Integer takeDrugsVoucher) {
        this.takeDrugsVoucher = takeDrugsVoucher;
    }

    @Column(name = "special_tips")
    public String getSpecialTips() {
        return specialTips;
    }

    public void setSpecialTips(String specialTips) {
        this.specialTips = specialTips;
    }

    @Column(name = "use_drug_dispenser_flag")
    public Integer getUseDrugDispenserFlag() {
        return useDrugDispenserFlag;
    }

    public void setUseDrugDispenserFlag(Integer useDrugDispenserFlag) {
        this.useDrugDispenserFlag = useDrugDispenserFlag;
    }

    @Column(name = "print_usage_label_flag")
    public Integer getPrintUsageLabelFlag() {
        return printUsageLabelFlag;
    }

    public void setPrintUsageLabelFlag(Integer printUsageLabelFlag) {
        this.printUsageLabelFlag = printUsageLabelFlag;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "modified_time")
    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    @Column(name = "invoice_request_flag")
    public Integer getInvoiceRequestFlag() {
        return invoiceRequestFlag;
    }

    public void setInvoiceRequestFlag(Integer invoiceRequestFlag) {
        this.invoiceRequestFlag = invoiceRequestFlag;
    }

    @Column(name = "send_drug_notify_phone")
    public String getSendDrugNotifyPhone() {
        return sendDrugNotifyPhone;
    }

    public void setSendDrugNotifyPhone(String sendDrugNotifyPhone) {
        this.sendDrugNotifyPhone = sendDrugNotifyPhone;
    }

    @Column(name = "refund_notify_phone")
    public String getRefundNotifyPhone() {
        return refundNotifyPhone;
    }

    public void setRefundNotifyPhone(String refundNotifyPhone) {
        this.refundNotifyPhone = refundNotifyPhone;
    }

    @Column(name = "invoice_support_flag")
    public Integer getInvoiceSupportFlag() {
        return invoiceSupportFlag;
    }

    public void setInvoiceSupportFlag(Integer invoiceSupportFlag) {
        this.invoiceSupportFlag = invoiceSupportFlag;
    }

    @Column(name = "is_hos_dep")
    public Integer getIsHosDep() {
        return isHosDep;
    }

    public void setIsHosDep(Integer isHosDep) {
        this.isHosDep = isHosDep;
    }


    @Column(name = "standard_payment_way")
    public Integer getStandardPaymentWay() {
        return standardPaymentWay;
    }

    public void setStandardPaymentWay(Integer standardPaymentWay) {
        this.standardPaymentWay = standardPaymentWay;
    }
}
