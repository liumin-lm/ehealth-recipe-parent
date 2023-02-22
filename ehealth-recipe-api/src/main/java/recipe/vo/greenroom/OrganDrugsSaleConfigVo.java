package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 机构药企销售配置
 * @author： whf
 * @date： 2022-01-10 15:38
 */
@Getter
@Setter
public class OrganDrugsSaleConfigVo implements Serializable {
    /**
     * 自增主键
     */
    private Integer id;

    /**
     * 机构id
     */
    private Integer organId;

    /**
     * 药企id
     */
    private Integer drugsEnterpriseId;

    /**
     * 是否支持配送到站 0 不支持 1支持
     */
    private Integer isSupportSendToStation;

    /**
     * 自取支付方式 1 在线支付 2 线下支付
     */
    private Integer takeOneselfPayment;

    @ItemProperty(alias = "自取支付方式 1 在线支付 2 线下支付")
    private List<String> takeOneselfPaymentWay;

    /**
     * 自取支付通道 1平台支付 2卫宁支付
     */
    private Integer takeOneselfPaymentChannel;

    /**
     * 自取预约时间 0 不预约 1预约当天 3 预约3天内 7预约7天内 15预约15天内
     */
    private Integer takeOneselfPlanDate;

    /**
     * 取药预约取药时间段配置：上午时间段
     */
    private String takeOneselfPlanAmTime;

    /**
     * 取药预约取药时间段配置：下午时间段
     */
    private String takeOneselfPlanPmTime;

    /**
     * 取药凭证 0不展示 1就诊卡号 2挂号序号 3病历号 4his单号 5发药流水号 6取药流水号
     */
    private Integer takeDrugsVoucher;

    /**
     * 医保患者特殊提示
     */
    private String specialTips;

    /**
     * 是否调用药企发药机：0不调用，1调用
     */
    private Integer useDrugDispenserFlag;

    /**
     * 是否打印用法标签：0不打印，1打印
     */
    private Integer printUsageLabelFlag;

    /**
     * 是否打开发票申请：0不打开，1打开
     */
    private Integer invoiceRequestFlag;

    @ItemProperty(alias = "发药通知电话")
    private String sendDrugNotifyPhone;

    @ItemProperty(alias = "退费审核通知电话")
    private String refundNotifyPhone;

    @ItemProperty(alias = "是否支持打印发票：0不支持，1支持")
    private Integer invoiceSupportFlag;

    @ItemProperty(alias = "退款申请中允许发药：0不允许，1允许 默认允许")
    private Integer refundFeeisAllowSendDrug;

    @ItemProperty(alias = "是否走医院预结算：1是，0否")
    private Integer isHosDep;

    @ItemProperty(alias = "标准的收款方式  1 在线支付 2 货到付款 ")
    private String standardPaymentWay;

    @ItemProperty(alias = "药品订单推送失败通知电话")
    private String orderPushFailPhone;

    @ItemProperty(alias = "到店取药收款方式  2 货到付款 1 在线支付 ")
    private List<String> storePaymentWay;

    @ItemProperty(alias = "取药收款提示文案")
    private String paymentWayTips;

}
