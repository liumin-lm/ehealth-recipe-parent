package recipe.vo.greenroom;

import ctd.schema.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class DrugsEnterpriseVO implements Serializable {
    private static final long serialVersionUID = -3888615911477279544L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "药企名称")
    private String name;

    @ItemProperty(alias = "平台自定义药企编码")
    private String enterpriseCode;

    @ItemProperty(alias = "药企关键机构")
    private Integer organId;

    @ItemProperty(alias = "药企分配appKey从开放平台获取")
    private String appKey;

    @ItemProperty(alias = "药企在平台的账户")
    private String account;

    @ItemProperty(alias = "用户名")
    private String userId;

    @ItemProperty(alias = "密码")
    private String password;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "药企联系电话")
    private String tel;

    @ItemProperty(alias = "药企实现类简称，默认使用 common， 也就是国药的一套实现")
    private String callSys;

    @ItemProperty(alias = "结算方式 0:药店价格 1:医院价格")
    private Integer settlementMode;

    @ItemProperty(alias = "调用接口标识")
    private String token;

    @ItemProperty(alias = "药企平台鉴权地址")
    private String authenUrl;

    @ItemProperty(alias = "药企平台业务处理地址")
    private String businessUrl;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持")
    private Integer payModeSupport;

    @ItemProperty(alias = "院内补充库存 0:非补充，1：补充库存")
    private Integer hosInteriorSupport;

    @ItemProperty(alias = "提交订单的类型0 提交订单到第三方 1 系统提交")
    private Integer orderType;

    @ItemProperty(alias = "状态标识")
    private Integer status;

    @ItemProperty(alias = "排序，1最前，越往后越小")
    private Integer sort;

    @ItemProperty(alias = "校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存")
    private Integer checkInventoryFlag;

    @ItemProperty(alias = "创建类型：1：非自建  0：自建")
    private Integer createType;

    @ItemProperty(alias = "运费细则图片ID")
    @FileToken(expires = 3600)
    private String transFeeDetail;

    @ItemProperty(alias = "是否医院类型药企：1医院结算药企，0普通药企")
    private Integer isHosDep;

    @ItemProperty(alias = "药企备注")
    private String memo;

    @ItemProperty(alias = "是否支持省直医保：1不支持，0支持 默认0")
    private Integer medicalInsuranceSupport;

    @ItemProperty(alias = "0:不支付药品费用，1:全部支付")
    private Integer storePayFlag;

    @ItemProperty(alias = "配送主体类型 1医院配送 2 药企配送")
    private Integer sendType;

    @ItemProperty(alias = "配送费支付方式 1-在线支付 2-线下支付 3-第三方支付 4-上传运费细则标准")
    private Integer expressFeePayWay;

    @ItemProperty(alias = "管理单元")
    private String manageUnit;

    @ItemProperty(alias = "展示配送药店标识")
    private Integer showStoreFlag;

    @ItemProperty(alias = "药企下载处方签类型")
    private Integer downSignImgType;

    @ItemProperty(alias = "运费的获取方式 0 平台 1 第三方")
    private Integer expressFeeType;

    @ItemProperty(alias = "药企对接方式 0 平台 1 前置机")
    private Integer operationType;

    @ItemProperty(alias = "是否显示期望配送时间,,默认否 0:否,1:显示非必填，2显示必填")
    private Integer isShowExpectSendDate;

    @ItemProperty(alias = "期望配送时间是否含周末,默认否 0:否,1:是")
    private Integer expectSendDateIsContainsWeekend;

    @ItemProperty(alias = "配送时间说明文案")
    private String sendDateText;

    @ItemProperty(alias = "物流公司")
    private Integer logisticsCompany;

    @ItemProperty(alias = "物流类型 1-平台 2-药企 3-药企(His)")
    private Integer logisticsType;

    @ItemProperty(alias = "是否支持合并快递单 0:不支持合并 1:下单时支持与前面的订单使用同一个快递单")
    private Integer logisticsMergeFlag;

    @ItemProperty(alias = "可合并订单下单时间")
    private String logisticsMergeTime;

    @ItemProperty(alias = "寄件人名称")
    private String consignorName;

    @ItemProperty(alias = "寄件人手机号")
    private String consignorMobile;

    @ItemProperty(alias = "寄件人省份编码")
    private String consignorProvince;

    @ItemProperty(alias = "寄件人城市编码")
    private String consignorCity;

    @ItemProperty(alias = "寄件人区域编码")
    private String consignorDistrict;

    @ItemProperty(alias = "寄件人详细地址")
    private String consignorAddress;

    @ItemProperty(alias = "寄件人街道编码")
    private String consignorStreet;

    @ItemProperty(alias = "订单备注")
    private String orderMemo;

    @ItemProperty(alias = "药企更新token的标识")
    private Integer updateTokenFlag;

    @ItemProperty(alias = "集揽模式")
    private Integer collectMode;

    @ItemProperty(alias = "到院取药日期是否展示今天 0 否 1 是")
    private Integer isShowToday;

    @ItemProperty(alias = "第三方药企编码")
    private String thirdEnterpriseCode;

    @ItemProperty(alias = "药企联系电话")
    private String enterprisePhone;

    @ItemProperty(alias = "包邮金额")
    private BigDecimal freeDeliveryMoney;

    @ItemProperty(alias = "展示物流方式 0 平台获取的物流信息 1 第三方页面链接展示")
    private Integer showLogisticsType;

    @ItemProperty(alias = "第三方物流页面链接")
    private String showLogisticsLink;

    @ItemProperty(alias = "药企所属商户： 0：普通药企, 1:印象智能, 2:金投云药房")
    private Integer merchantType;
}
