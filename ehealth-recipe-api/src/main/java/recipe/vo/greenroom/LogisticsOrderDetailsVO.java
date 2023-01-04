package recipe.vo.greenroom;

import com.ngari.desensitize.annotation.MaskSensitiveData;
import com.ngari.desensitize.enums.DesensitionType;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Author zgy
 * @Date 2023-01-04
 * 物流订单详情
 */
@Data
public class LogisticsOrderDetailsVO implements Serializable {


    private static final long serialVersionUID = 131512781008829694L;

    @ItemProperty(alias="机构ID")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer organId;

    @ItemProperty(alias="业务类型")
    @Dictionary(id = "eh.infra.dictionary.BusinessType")
    private Integer businessType;

    @ItemProperty(alias="业务编号")
    private String businessNo;

    @ItemProperty(alias="寄托物名称")
    private String depositumName;

    @ItemProperty(alias="物流公司实际编码")
//    @Dictionary(id = "eh.infra.dictionary.LogisticsCode")
    private String logisticsCode;

    // 此字段为临时添加，后续要启用，并去掉logisticsCode字典的注释
    private String logisticsCodeText;

    @ItemProperty(alias="实际运单号")
    private String waybillNo;

    @ItemProperty(alias="物流公司业务名称")
    private String businessCompanyName;

    @ItemProperty(alias="业务运单号")
    private String businessWaybillNo;

    @ItemProperty(alias="运单费用")
    private BigDecimal waybillFee;

    @ItemProperty(alias="总运单费用")
    private BigDecimal fee;

    @ItemProperty(alias="包装费用")
    private BigDecimal packFee;

    @ItemProperty(alias="寄件联系人")
    @MaskSensitiveData(
            type = DesensitionType.NAME
    )
    private String consignorName;

    @ItemProperty(alias="寄件电话(手机)")
    @MaskSensitiveData(
            type = DesensitionType.MOBILE
    )
    private String consignorPhone;

    @ItemProperty(alias="寄件省份")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String consignorProvince;

    @ItemProperty(alias="寄件城市")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String consignorCity;

    @ItemProperty(alias="寄件镇/区")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String consignorDistrict;

    @ItemProperty(alias="寄件街道")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String consignorStreet;

    @ItemProperty(alias="寄件详细地址")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String consignorAddress;

    @ItemProperty(alias="收件联系人")
    @MaskSensitiveData(
            type = DesensitionType.NAME
    )
    private String addresseeName;

    @ItemProperty(alias="收件电话(手机)")
    @MaskSensitiveData(
            type = DesensitionType.MOBILE
    )
    private String addresseePhone;

    @ItemProperty(alias="收件省份")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String addresseeProvince;

    @ItemProperty(alias="收件城市")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String addresseeCity;

    @ItemProperty(alias="收件镇/区")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String addresseeDistrict;

    @ItemProperty(alias="收件街道")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String addresseeStreet;

    @ItemProperty(alias="收件详细地址")
    @MaskSensitiveData(
            type = DesensitionType.ADDRESS
    )
    private String addresseeAddress;
    @ItemProperty(alias="支付状态")
    @Dictionary(id = "eh.infra.dictionary.PayStatus")
    private Integer payStatus;
    @ItemProperty(alias="支付方式")
    @Dictionary(id = "eh.infra.dictionary.PayMethod")
    private Integer payMethod;

    @ItemProperty(alias="运单路由")
    private List<WaybillRouteSummaryVO> waybillRouteList;

    @ItemProperty(alias="面单标题")
    private String printTitle;

    @ItemProperty(alias="预计交货时间")
    private String deliverTime;

    @ItemProperty(alias = "邮件回调修改稿时间")
    private Date mailNoDate;

    @ItemProperty(alias="仓库和药企业保存的机构id")
    private Integer organNo;

    @ItemProperty(alias="物流账号类型 药企0 机构1 仓库2")
    @Dictionary(id = "eh.infra.dictionary.Type")
    private Integer accountType;

    @ItemProperty(alias = "当前状态")
    private Integer currentState;

    @ItemProperty(alias = "用户经度")
    private String userLng;

    @ItemProperty(alias = "用户纬度")
    private String userLat;
}
