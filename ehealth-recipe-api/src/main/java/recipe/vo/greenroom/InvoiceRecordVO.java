package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class InvoiceRecordVO implements Serializable {
    private static final long serialVersionUID = 6346295360773190828L;

    @ItemProperty(alias="主键")
    private Integer id;

    @ItemProperty(alias="userId")
    private String userId;

    @ItemProperty(alias="业务类型")
    private Integer businessType;

    @ItemProperty(alias="业务id")
    private String businessId;

    @ItemProperty(alias="抬头类型 1 个人 2 企业")
    private Integer riseType;

    @ItemProperty(alias="发票抬头")
    private String invoiceTitle;

    @ItemProperty(alias="发票状态")
    private Integer invoiceStatus;

    @ItemProperty(alias="发票链接")
    private String invoiceUrl;

    @ItemProperty(alias="接受人")
    private String receiverName;

    @ItemProperty(alias="接受人手机号")
    private String receiverPhone;

    @ItemProperty(alias="接受人邮件")
    private String receiverEmail;

    @ItemProperty(alias="创建时间")
    private Date createtime;

    @ItemProperty(alias="修改时间")
    private Date updatetime;

    @ItemProperty(alias = "模板ID")
    private String templateId;

    @ItemProperty(alias ="金额")
    private BigDecimal price;

    @ItemProperty(alias ="发票备注")
    private String invoiceContent;

    @ItemProperty(alias="税号")
    private String taxNumber;

    @ItemProperty(alias="注册地址")
    private String registeredAddress;

    @ItemProperty(alias="注册电话")
    private String registeredPhone;

    @ItemProperty(alias="开户银行")
    private String bankAccount;

    @ItemProperty(alias="银行账号")
    private String bankAccountNumber;
}
