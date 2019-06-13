package recipe.bean;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2019/3/5
 * @description： 药企订单对象
 * @version： 1.0
 */
public class DeptOrderDTO implements Serializable {

    private static final long serialVersionUID = 1997854947213822714L;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 订单状态
     */
    private Long status;

    /**
     * 订单详情链接
     */
    private String orderDetailUrl;

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public String getOrderDetailUrl() {
        return orderDetailUrl;
    }

    public void setOrderDetailUrl(String orderDetailUrl) {
        this.orderDetailUrl = orderDetailUrl;
    }
}
