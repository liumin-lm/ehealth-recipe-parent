package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 *
 * @author yinsheng
 * @date 2019\3\1 0001 15:59
 */
@Schema
public class RxOrderResponse implements Serializable{
    private static final long serialVersionUID = -7022562337756941423L;

    @Verify(isNotNull = true, desc = "订单号")
    private Long bizOrderId;

    @Verify(isNotNull = true, desc = "订单状态")
    private Integer status;

    @Verify(isNotNull = true, desc = "订单详情URL")
    private String bizOrderDetailUrl;

    @Verify(isNotNull = false, desc = "状态为已关闭时，才有此值")
    private String reason;

    @Verify(isNotNull = false, desc = "订单扩展字段")
    private String attribute;

    public Long getBizOrderId() {
        return bizOrderId;
    }

    public void setBizOrderId(Long bizOrderId) {
        this.bizOrderId = bizOrderId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getBizOrderDetailUrl() {
        return bizOrderDetailUrl;
    }

    public void setBizOrderDetailUrl(String bizOrderDetailUrl) {
        this.bizOrderDetailUrl = bizOrderDetailUrl;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
}
