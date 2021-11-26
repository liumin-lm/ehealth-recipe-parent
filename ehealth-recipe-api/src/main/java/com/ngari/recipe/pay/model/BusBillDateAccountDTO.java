package com.ngari.recipe.pay.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * created by liumin on 2021/11/22
 */
@Data
public class BusBillDateAccountDTO implements Serializable {
    private static final long serialVersionUID = 6724199234598624208L;
    /**
     * 平台业务单号
     */
    private Integer busId;

    /**
     * 业务请求支付平台的订单号，下单唯一标识
     */
    private String outTradeNo;

    /**
     * 支付宝、微信等支付系统中返回的订单支付流水号
     */
    private String tradeNo;

    /**
     * 正交易传收费总金额，负交易传原订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private Date paymentDate;

    /**
     * HIS系统订单号
     */
    private String settlementNo;

    /**
     * 交易类型 1正交易、2负交易
     */
    private String tradeStatus;

    /**
     * 正交易传0，负交易传退费金额
     */
    private String refundAmount;

    /**
     * 正交易传空，负交易时传入。退款时传入的唯一流水号
     */
    private String refundBatchNo;

    /**
     * 正交易传空，负交易时传入。负交易发起退款申请的时间
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private Date refundDate;

    /**
     * 交易支付渠道：1支付宝，2微信，可根据PayWayEnum获取
     */
    private String payType;

    /**
     * 平台业务类型，详见BusTypeEnum
     */
    private String busType;

    /**
     * 病人在HIS系统的唯一标识
     */
    private String patientId;

    /**
     * 患者姓名
     */
    private String pname;

    /**
     * 患者联系电话
     */
    private String phone;
    private String mpiid;

    /**
     * 1、全自费；2、全医保；3、部分医保部分自费
     */
    private String settlementType;

    /**
     * 医保报销金额
     */
    private BigDecimal medAmount;

    /**
     * 自费支付金额
     */
    private BigDecimal personAmount;

    /**
     * 其他渠道支付金额
     */
    private BigDecimal otherAmount;

    /**
     * 订单描述
     */
    private String remark;

    /**
     * 平台业务发起的机构
     */
    private String organId;

    /**
     * 支付平台实际收款的支付机构ID
     */
    private String payOrganId;
}
