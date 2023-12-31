package com.ngari.recipe.entity;

import ctd.schema.annotation.*;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药企
 *
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Entity
@Schema
@Table(name = "cdr_drugsenterprise")
@Access(AccessType.PROPERTY)
@DynamicInsert
public class DrugsEnterprise implements java.io.Serializable {

    private static final long serialVersionUID = 7806649469165719455L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "药企名称")
    private String name;

    @ItemProperty(alias = "平台自定义药企编码")
    private String enterpriseCode;

    //作废
    @ItemProperty(alias = "药企关键机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
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

    @ItemProperty(alias = "校验药品库存标志 0 全程都不校验 1 开方时校验 2 下单时校验 3 开方时和下单时都进行校验")
    private Integer checkInventoryType;

    @ItemProperty(alias = "库存校验途径 1 校验药企 2 校验医院库存")
    private Integer checkInventoryWay;

    @ItemProperty(alias = "创建类型：1：非自建  0：自建")
    @Dictionary(id = "eh.cdr.dictionary.DepType")
    private Integer createType;

    @ItemProperty(alias = "运费细则图片ID")
    @FileToken(expires = 3600)
    private String transFeeDetail;

    @ItemProperty(alias = "是否医院类型药企：1医院结算药企，0普通药企, 这个字段转移到organ_drugs_sale_config表，后续可删除药企表这个字段")
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
    @Dictionary(id = "eh.cdr.dictionary.LogisticsCompany")
    private Integer logisticsCompany;

    @ItemProperty(alias = "物流类型 1-平台 2-药企 3-药企(His)")
    private Integer logisticsType;

    @ItemProperty(alias = "是否支持合并快递单 0:不支持合并 1:下单时支持与前面的订单使用同一个快递单")
    private Integer logisticsMergeFlag;

    @ItemProperty(alias = "可合并订单下单时间")
    private String logisticsMergeTime;

    @ItemProperty(alias = "订单备注")
    private String orderMemo;

    @ItemProperty(alias = "药企更新token的标识")
    private Integer updateTokenFlag;

    @ItemProperty(alias = "到院取药日期是否展示今天 0 否 1 是")
    private Integer isShowToday;

    @ItemProperty(alias = "第三方药企编码")
    private String thirdEnterpriseCode;

    @ItemProperty(alias = "药企联系电话")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String enterprisePhone;

    @ItemProperty(alias = "包邮金额")
    private BigDecimal freeDeliveryMoney;

    @ItemProperty(alias = "展示物流方式 0 平台获取的物流信息 1 第三方页面链接展示")
    private Integer showLogisticsType;

    @ItemProperty(alias = "第三方物流页面链接")
    private String showLogisticsLink;

    @ItemProperty(alias = "药企所属商户")
    @Dictionary(id = "eh.cdr.dictionary.MerchantType")
    private Integer merchantType;

    @ItemProperty(alias = "优先级")
    private Integer priorityLevel;

    @ItemProperty(alias = "支付方式 1 线上支付 2 货到付款")
    private String payMode;

    @ItemProperty(alias = "药柜编码")
    private String medicineChestCode;
    
    @Column(name = "medicine_chest_code")
    public String getMedicineChestCode() {
        return medicineChestCode;
    }

    public void setMedicineChestCode(String medicineChestCode) {
        this.medicineChestCode = medicineChestCode;
    }

    @Column(name = "enterprise_phone")
    public String getEnterprisePhone() {
        return enterprisePhone;
    }

    public void setEnterprisePhone(String enterprisePhone) {
        this.enterprisePhone = enterprisePhone;
    }

    @Column(name = "is_show_today")
    public Integer getIsShowToday() {
        return isShowToday;
    }

    public void setIsShowToday(Integer isShowToday) {
        this.isShowToday = isShowToday;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "enterpriseCode")
    public String getEnterpriseCode() {
        return enterpriseCode;
    }

    public void setEnterpriseCode(String enterpriseCode) {
        this.enterpriseCode = enterpriseCode;
    }

    @Column(name = "appKey")
    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    @Column(name = "Account", length = 20)
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @Column(name = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Column(name = "Password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "Tel")
    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    @Column(name = "CallSys")
    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    @Column(name = "settlementMode")
    public Integer getSettlementMode() {
        return settlementMode;
    }

    public void setSettlementMode(Integer settlementMode) {
        this.settlementMode = settlementMode;
    }

    @Column(name = "Token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(name = "AuthenUrl")
    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    @Column(name = "BusinessUrl")
    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    @Column(name = "CreateDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "PayModeSupport")
    public Integer getPayModeSupport() {
        return payModeSupport;
    }

    public void setPayModeSupport(Integer payModeSupport) {
        this.payModeSupport = payModeSupport;
    }

    @Column(name = "hosInteriorSupport")
    public Integer getHosInteriorSupport() {
        return hosInteriorSupport;
    }

    public void setHosInteriorSupport(Integer hosInteriorSupport) {
        this.hosInteriorSupport = hosInteriorSupport;
    }

    @Column(name = "orderType")
    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "Sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Column(name = "checkInventoryFlag")
    public Integer getCheckInventoryFlag() {
        return checkInventoryFlag;
    }

    public void setCheckInventoryFlag(Integer checkInventoryFlag) {
        this.checkInventoryFlag = checkInventoryFlag;
    }

    @Column(name = "check_inventory_type")
    public Integer getCheckInventoryType() {
        return checkInventoryType;
    }

    public void setCheckInventoryType(Integer checkInventoryType) {
        this.checkInventoryType = checkInventoryType;
    }

    @Column(name = "check_inventory_way")
    public Integer getCheckInventoryWay() {
        return checkInventoryWay;
    }

    public void setCheckInventoryWay(Integer checkInventoryWay) {
        this.checkInventoryWay = checkInventoryWay;
    }

    public Integer getCreateType() {
        return createType;
    }

    public void setCreateType(Integer createType) {
        this.createType = createType;
    }

    @Column(name = "TransFeeDetail")
    public String getTransFeeDetail() {
        return transFeeDetail;
    }

    public void setTransFeeDetail(String transFeeDetail) {
        this.transFeeDetail = transFeeDetail;
    }

    @Deprecated
    @Column(name = "isHosDep")
    public Integer getIsHosDep() {
        return isHosDep;
    }

    public void setIsHosDep(Integer isHosDep) {
        this.isHosDep = isHosDep;
    }

    @Column(name = "memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "MedicalInsuranceSupport")
    public Integer getMedicalInsuranceSupport() {
        return medicalInsuranceSupport;
    }

    public void setMedicalInsuranceSupport(Integer medicalInsuranceSupport) {
        this.medicalInsuranceSupport = medicalInsuranceSupport;
    }

    @Column(name = "storePayFlag")
    public Integer getStorePayFlag() {
        return storePayFlag;
    }

    public void setStorePayFlag(Integer storePayFlag) {
        this.storePayFlag = storePayFlag;
    }

    @Column(name = "sendType")
    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    @Column(name = "expressFeePayWay")
    public Integer getExpressFeePayWay() {
        return expressFeePayWay;
    }

    public void setExpressFeePayWay(Integer expressFeePayWay) {
        this.expressFeePayWay = expressFeePayWay;
    }

    @Column(name = "manageUnit")
    public String getManageUnit() {
        return manageUnit;
    }

    public void setManageUnit(String manageUnit) {
        this.manageUnit = manageUnit;
    }

    @Column(name = "showStoreFlag")
    public Integer getShowStoreFlag() {
        return showStoreFlag;
    }

    public void setShowStoreFlag(Integer showStoreFlag) {
        this.showStoreFlag = showStoreFlag;
    }

    @Column(name = "downSignImgType")
    public Integer getDownSignImgType() {
        return downSignImgType;
    }

    public void setDownSignImgType(Integer downSignImgType) {
        this.downSignImgType = downSignImgType;
    }

    @Column(name = "expressFeeType")
    public Integer getExpressFeeType() {
        return expressFeeType;
    }

    public void setExpressFeeType(Integer expressFeeType) {
        this.expressFeeType = expressFeeType;
    }

    @Column(name = "operationType")
    public Integer getOperationType() {
        return operationType;
    }

    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }

    @Column(name = "organId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DrugsEnterprise that = (DrugsEnterprise) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Integer getIsShowExpectSendDate() {
        return isShowExpectSendDate;
    }

    public void setIsShowExpectSendDate(Integer isShowExpectSendDate) {
        this.isShowExpectSendDate = isShowExpectSendDate;
    }

    public Integer getExpectSendDateIsContainsWeekend() {
        return expectSendDateIsContainsWeekend;
    }

    public void setExpectSendDateIsContainsWeekend(Integer expectSendDateIsContainsWeekend) {
        this.expectSendDateIsContainsWeekend = expectSendDateIsContainsWeekend;
    }

    public String getSendDateText() {
        return sendDateText;
    }

    public void setSendDateText(String sendDateText) {
        this.sendDateText = sendDateText;
    }

    @Column(name = "logisticsCompany")
    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    @Column(name = "logisticsType")
    public Integer getLogisticsType() {
        return logisticsType;
    }

    public void setLogisticsType(Integer logisticsType) {
        this.logisticsType = logisticsType;
    }

    @Column(name = "orderMemo")
    public String getOrderMemo() {
        return orderMemo;
    }

    public void setOrderMemo(String orderMemo) {
        this.orderMemo = orderMemo;
    }

    @Column(name = "update_token_flag")
    public Integer getUpdateTokenFlag() {
        return updateTokenFlag;
    }

    public void setUpdateTokenFlag(Integer updateTokenFlag) {
        this.updateTokenFlag = updateTokenFlag;
    }

    @Column(name = "third_enterprise_code")
    public String getThirdEnterpriseCode() {
        return thirdEnterpriseCode;
    }

    public void setThirdEnterpriseCode(String thirdEnterpriseCode) {
        this.thirdEnterpriseCode = thirdEnterpriseCode;
    }

    @Column(name = "free_delivery_money")
    public BigDecimal getFreeDeliveryMoney() {
        return freeDeliveryMoney;
    }

    public void setFreeDeliveryMoney(BigDecimal freeDeliveryMoney) {
        this.freeDeliveryMoney = freeDeliveryMoney;
    }

    @Column(name = "show_logistics_type")
    public Integer getShowLogisticsType() {
        return showLogisticsType;
    }

    public void setShowLogisticsType(Integer showLogisticsType) {
        this.showLogisticsType = showLogisticsType;
    }

    @Column(name = "show_logistics_link")
    public String getShowLogisticsLink() {
        return showLogisticsLink;
    }

    public void setShowLogisticsLink(String showLogisticsLink) {
        this.showLogisticsLink = showLogisticsLink;
    }

    @Column(name = "merchant_type")
    public Integer getMerchantType() {
        return merchantType;
    }

    public void setMerchantType(Integer merchantType) {
        this.merchantType = merchantType;
    }

    @Column(name = "logistics_merge_flag")
    public Integer getLogisticsMergeFlag() {
        return logisticsMergeFlag;
    }

    public void setLogisticsMergeFlag(Integer logisticsMergeFlag) {
        this.logisticsMergeFlag = logisticsMergeFlag;
    }

    @Column(name = "logistics_merge_time")
    public String getLogisticsMergeTime() {
        return logisticsMergeTime;
    }

    public void setLogisticsMergeTime(String logisticsMergeTime) {
        this.logisticsMergeTime = logisticsMergeTime;
    }

    @Transient
    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    @Transient
    public String getPayMode() {
        return payMode;
    }

    public void setPayMode(String payMode) {
        this.payMode = payMode;
    }
}
