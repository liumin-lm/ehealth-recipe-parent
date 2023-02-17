package recipe.vo.greenroom;

import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运营平台处方订单退费
 * @author yinsheng
 * @date 2022/04/07 15:41
 */
@Getter
@Setter
@Schema
public class RecipeOrderRefundVO implements Serializable {
    private static final long serialVersionUID = -6152135464708446009L;

    @ItemProperty(alias = "定单号")
    private String orderCode;

    @ItemProperty(alias = "取药凭证")
    private String takeDrugsVoucher;

    @ItemProperty(alias = "支付金额")
    private Double actualPrice;

    @ItemProperty(alias = "发货状态")
    private String sendStatusText;

    @ItemProperty(alias = "购药方式")
    private String giveModeText;

    @ItemProperty(alias = "支付方式")
    private String payModeText;

    @ItemProperty(alias = "出售药企药店")
    private String depName;

    @ItemProperty(alias = "下单人")
    private String patientName;

    @ItemProperty(alias = "下单渠道")
    private String channel;

    @ItemProperty(alias = "下单时间")
    private String createTime;

    @ItemProperty(alias = "支付时间")
    private String payTime;

    @ItemProperty(alias = "订单状态")
    private String orderStatusText;

    @ItemProperty(alias = "订单类型 便捷购药标识：0普通订单，1便捷购药订单")
    private Integer fastRecipeFlag;

    @ItemProperty(alias = "退货状态")
    private String refundStatusText;

    @ItemProperty(alias = "发票状态 1 开具 0 无需开具")
    private Integer invoiceStatus;

    @ItemProperty(alias = "是否已打印发药清单")
    private Boolean printDrugDistributionListFlag;

    @ItemProperty(alias = "是否已打印快递面单")
    private Boolean printExpressBillFlag;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.infra.dictionary.LogisticsCode")
    private Integer logisticsCompany;

    @ItemProperty(alias = "快递单号")
    private String trackingNumber;

    @ItemProperty(alias = "收货人")
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @ItemProperty(alias = "订单状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeOrderStatus")
    private Integer status;

    @ItemProperty(alias = "发药药师姓名")
    private String dispensingApothecaryName;

    @ItemProperty(alias = "是否可以打印快递面单")
    private Boolean printWaybillByLogisticsOrderNo;

}
