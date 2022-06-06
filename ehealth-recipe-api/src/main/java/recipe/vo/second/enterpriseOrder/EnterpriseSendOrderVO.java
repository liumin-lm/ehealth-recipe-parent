package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class EnterpriseSendOrderVO implements Serializable {
    private static final long serialVersionUID = 9078330874662954562L;

    @ItemProperty(alias = "订单编号")
    private String orderCode;
    @ItemProperty(alias = "发货时间")
    private String sendDate;
    @ItemProperty(alias = "发货人")
    private String sender;
    @ItemProperty(alias = "物流公司")
    private String logisticsCompany;
    @ItemProperty(alias = "快递单号")
    private String trackingNumber;
    @ItemProperty(alias = "门店名称")
    private String drugStoreName;
    @ItemProperty(alias = "门店编码")
    private String drugStoreCode;
}
