package com.ngari.recipe.pay.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * created by shiyuping on 2021/1/22
 */
@Data
public class PayResultDTO implements Serializable {
    private static final long serialVersionUID = 185470924185038273L;
    /**
     * 业务主键id，做业务处理时应以此字段为条件查询业务单信息
     */
    private Integer busId;
    /**
     * 支付的云平台订单号
     */
    private String outTradeNo;
    /**
     * 云平台订单号对应的第三方交易流水号
     */
    private String tradeNo;
    /**
     * 订单支付时间
     */
    private Date paymentDate;

    private String payOrganId;

    private String payWay;

    private Map<String, String> notifyMap;
}
