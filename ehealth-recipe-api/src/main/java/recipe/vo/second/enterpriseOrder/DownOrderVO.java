package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import recipe.vo.base.BaseOrderVO;

import java.io.Serializable;

/**
 * @description： 第三方下载订单信息
 * @author： yinsheng
 * @date： 2021-12-08 15:50
 */
public class DownOrderVO extends BaseOrderVO implements Serializable {
    private static final long serialVersionUID = 1698992569210516481L;

    @ItemProperty(alias = "订单支付标志 0未支付，1已支付")
    private Integer orderPayFlag;

    public Integer getOrderPayFlag() {
        return orderPayFlag;
    }

    public void setOrderPayFlag(Integer orderPayFlag) {
        if (super.getPayFlag() == 1 && super.getPayMode() == 1) {
            this.orderPayFlag = 1;
        } else {
            this.orderPayFlag = 0;
        }
    }
}
