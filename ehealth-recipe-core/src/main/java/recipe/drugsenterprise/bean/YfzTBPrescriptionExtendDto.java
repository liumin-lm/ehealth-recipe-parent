package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 对接上海六院易复诊开处方接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 * 对接药企 无需脱敏
 */
@Schema
public class YfzTBPrescriptionExtendDto implements Serializable {
    /**
     * 电子处方 ID
     */
    private String prescriptionNo;
    /**
     * 支付方式，默认值线上支付
     */
    private String costCategory;
    /**
     * 收货人姓名
     */
    private String receiverName;
    /**
     * 收货人手机号
     */
    private String receiverMobile;
    /**
     * 收货地址
     */
    private String receiverAddress;
    /**
     * 订单支付时间
     */
    private Date payDatetime;
    /**
     * 订单类型，默认值送药上门
     */
    private String orderType;
    /**
     * 订单创建时间
     */
    private String orderCreateDate;
    /**
     * 订单支付时间
     */
    private String orderPayDate;
    /**
     * 配送费
     */
    private String deliverypPrice;
    /**
     * 订单总金额
     */
    private String orderDrugPrice;
    /**
     * 订单状态，默认值9 待发货
     */
    private String orderStatus;
    /**
     * 默认值1  线上送货 0自取
     */
    private String orderSource;
    /**
     * 订单备注
     */
    private String memo;
    /**
     * 订单号
     */
    private String orderNo;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPrescriptionNo() {
        return prescriptionNo;
    }

    public void setPrescriptionNo(String prescriptionNo) {
        this.prescriptionNo = prescriptionNo;
    }

    public String getCostCategory() {
        return costCategory;
    }

    public void setCostCategory(String costCategory) {
        this.costCategory = costCategory;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverMobile() {
        return receiverMobile;
    }

    public void setReceiverMobile(String receiverMobile) {
        this.receiverMobile = receiverMobile;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public Date getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(Date payDatetime) {
        this.payDatetime = payDatetime;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderCreateDate() {
        return orderCreateDate;
    }

    public void setOrderCreateDate(String orderCreateDate) {
        this.orderCreateDate = orderCreateDate;
    }

    public String getOrderPayDate() {
        return orderPayDate;
    }

    public void setOrderPayDate(String orderPayDate) {
        this.orderPayDate = orderPayDate;
    }

    public String getDeliverypPrice() {
        return deliverypPrice;
    }

    public void setDeliverypPrice(String deliverypPrice) {
        this.deliverypPrice = deliverypPrice;
    }

    public String getOrderDrugPrice() {
        return orderDrugPrice;
    }

    public void setOrderDrugPrice(String orderDrugPrice) {
        this.orderDrugPrice = orderDrugPrice;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(String orderSource) {
        this.orderSource = orderSource;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
