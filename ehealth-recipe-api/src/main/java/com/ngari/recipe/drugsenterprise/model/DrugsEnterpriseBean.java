package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.*;
import ctd.util.JSONUtils;
import recipe.vo.greenroom.PharmacyVO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 药企
 *
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Schema
public class DrugsEnterpriseBean implements Serializable {

    private static final long serialVersionUID = 3811188626885371263L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "药企名称")
    private String name;

    @ItemProperty(alias = "平台自定义药企编码")
    private String enterpriseCode;

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

    @ItemProperty(alias = "状态标识")
    private Integer status;

    @ItemProperty(alias = "排序，1最前，越往后越小")
    private Integer sort;

    @ItemProperty(alias = "校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存")
    private Integer checkInventoryFlag;

    @ItemProperty(alias = "药店信息")
    private Map<String, String> pharmacyInfo;

    @ItemProperty(alias = "创建类型：1：非自建  0：自建")
    @Dictionary(id = "eh.cdr.dictionary.DepType")
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

    @ItemProperty(alias = "配送主体类型 1医院配送 2 药企配送  ")
    private Integer sendType;

    @ItemProperty(alias = "配送费支付方式 1-在线支付 2-线下支付 3-第三方支付 4-上传运费收费标准(运费不取设置的运费仅展示图片的)")
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

    private Integer payeeCode;
    @ItemProperty(alias = "是否显示期望配送时间,,默认否 0:否,1:是")
    private Integer isShowExpectSendDate;

    @ItemProperty(alias = "期望配送时间是否含周末,默认否 0:否,1:是")
    private Integer expectSendDateIsContainsWeekend;

    @ItemProperty(alias = "配送时间说明文案")
    private String sendDateText;

    @ItemProperty(alias = "院内补充库存 0:非补充，1：补充库存")
    private Integer hosInteriorSupport;

    @ItemProperty(alias = "结算方式 0:药店价格 1:医院价格")
    private Integer settlementMode;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.infra.dictionary.LogisticsCode")
    private Integer logisticsCompany;

    @ItemProperty(alias = "物流类型 1-平台 2-药企")
    private Integer logisticsType;

    @ItemProperty(alias = "是否支持合并快递单 0:不支持合并 1:下单时支持与前面的订单使用同一个快递单")
    private Integer logisticsMergeFlag;

    @ItemProperty(alias = "可合并订单下单时间")
    private String logisticsMergeTime;

    @ItemProperty(alias = "寄件人名称")
    private String consignorName;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "寄件人手机号")
    private String consignorMobile;

    @ItemProperty(alias = "寄件人省份编码")
    private String consignorProvince;

    @ItemProperty(alias = "寄件人城市编码")
    private String consignorCity;

    @ItemProperty(alias = "寄件人区域编码")
    private String consignorDistrict;

    @Desensitizations(type = DesensitizationsType.ADDRESS)
    @ItemProperty(alias = "寄件人详细地址")
    private String consignorAddress;

    @ItemProperty(alias = "寄件人街道编码")
    private String consignorStreet;

    @ItemProperty(alias = "药企关联物流列表")
    private List<DrugEnterpriseLogisticsBean> drugEnterpriseLogisticsBeans;

    @ItemProperty(alias = "订单备注")
    private String orderMemo;

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

    @ItemProperty(alias = "药企所属商户")
    @Dictionary(id = "eh.cdr.dictionary.MerchantType")
    private Integer merchantType;

    @ItemProperty(alias = "优先级")
    private Integer priorityLevel;

    @ItemProperty(alias = "校验药品库存标志 0 全程都不校验 1 开方时校验 2 下单时校验 3 开方时和下单时都进行校验")
    private Integer checkInventoryType;

    @ItemProperty(alias = "库存校验途径 1 校验药企 2 校验医院库存")
    private Integer checkInventoryWay;

    @ItemProperty(alias = "药柜编码")
    private String medicineChestCode;

    public String getMedicineChestCode() {
        return medicineChestCode;
    }

    public void setMedicineChestCode(String medicineChestCode) {
        this.medicineChestCode = medicineChestCode;
    }

    private List<String> supportGiveModeNameList;

    private PharmacyVO pharmacy;

    public PharmacyVO getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(PharmacyVO pharmacy) {
        this.pharmacy = pharmacy;
    }

    public String getEnterprisePhone() {
        return enterprisePhone;
    }

    public void setEnterprisePhone(String enterprisePhone) {
        this.enterprisePhone = enterprisePhone;
    }

    public List<DrugEnterpriseLogisticsBean> getDrugEnterpriseLogisticsBeans() {
        return drugEnterpriseLogisticsBeans;
    }

    public void setDrugEnterpriseLogisticsBeans(List<DrugEnterpriseLogisticsBean> drugEnterpriseLogisticsBeans) {
        this.drugEnterpriseLogisticsBeans = drugEnterpriseLogisticsBeans;
    }


    public DrugsEnterpriseBean() {
    }

    public Integer getIsShowToday() {
        return isShowToday;
    }

    public void setIsShowToday(Integer isShowToday) {
        this.isShowToday = isShowToday;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getPayModeSupport() {
        return payModeSupport;
    }

    public void setPayModeSupport(Integer payModeSupport) {
        this.payModeSupport = payModeSupport;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getCheckInventoryFlag() {
        return checkInventoryFlag;
    }

    public void setCheckInventoryFlag(Integer checkInventoryFlag) {
        this.checkInventoryFlag = checkInventoryFlag;
    }

    public Map<String, String> getPharmacyInfo() {
        return pharmacyInfo;
    }

    public void setPharmacyInfo(Map<String, String> pharmacyInfo) {
        this.pharmacyInfo = pharmacyInfo;
    }

    public Integer getCreateType() {
        return createType;
    }

    public void setCreateType(Integer createType) {
        this.createType = createType;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }

    public String getTransFeeDetail() {
        return transFeeDetail;
    }

    public void setTransFeeDetail(String transFeeDetail) {
        this.transFeeDetail = transFeeDetail;
    }

    public Integer getIsHosDep() {
        return isHosDep;
    }

    public void setIsHosDep(Integer isHosDep) {
        this.isHosDep = isHosDep;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getEnterpriseCode() {
        return enterpriseCode;
    }

    public void setEnterpriseCode(String enterpriseCode) {
        this.enterpriseCode = enterpriseCode;
    }

    public Integer getMedicalInsuranceSupport() {
        return medicalInsuranceSupport;
    }

    public void setMedicalInsuranceSupport(Integer medicalInsuranceSupport) {
        this.medicalInsuranceSupport = medicalInsuranceSupport;
    }

    public Integer getStorePayFlag() {
        return storePayFlag;
    }

    public void setStorePayFlag(Integer storePayFlag) {
        this.storePayFlag = storePayFlag;
    }

    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    public Integer getExpressFeePayWay() {
        return expressFeePayWay;
    }

    public void setExpressFeePayWay(Integer expressFeePayWay) {
        this.expressFeePayWay = expressFeePayWay;
    }

    public String getManageUnit() {
        return manageUnit;
    }

    public void setManageUnit(String manageUnit) {
        this.manageUnit = manageUnit;
    }

    public Integer getShowStoreFlag() {
        return showStoreFlag;
    }

    public void setShowStoreFlag(Integer showStoreFlag) {
        this.showStoreFlag = showStoreFlag;
    }

    public Integer getDownSignImgType() {
        return downSignImgType;
    }

    public void setDownSignImgType(Integer downSignImgType) {
        this.downSignImgType = downSignImgType;
    }

    public Integer getExpressFeeType() {
        return expressFeeType;
    }

    public void setExpressFeeType(Integer expressFeeType) {
        this.expressFeeType = expressFeeType;
    }

    public Integer getOperationType() {
        return operationType;
    }

    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
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

    public Integer getPayeeCode() {
        return payeeCode;
    }

    public void setPayeeCode(Integer payeeCode) {
        this.payeeCode = payeeCode;
    }

    public Integer getHosInteriorSupport() {
        return hosInteriorSupport;
    }

    public void setHosInteriorSupport(Integer hosInteriorSupport) {
        this.hosInteriorSupport = hosInteriorSupport;
    }

    public Integer getSettlementMode() {
        return settlementMode;
    }

    public void setSettlementMode(Integer settlementMode) {
        this.settlementMode = settlementMode;
    }

    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public Integer getLogisticsType() {
        return logisticsType;
    }

    public void setLogisticsType(Integer logisticsType) {
        this.logisticsType = logisticsType;
    }

    public String getConsignorName() {
        return consignorName;
    }

    public void setConsignorName(String consignorName) {
        this.consignorName = consignorName;
    }

    public String getConsignorMobile() {
        return consignorMobile;
    }

    public void setConsignorMobile(String consignorMobile) {
        this.consignorMobile = consignorMobile;
    }

    public String getConsignorProvince() {
        return consignorProvince;
    }

    public void setConsignorProvince(String consignorProvince) {
        this.consignorProvince = consignorProvince;
    }

    public String getConsignorCity() {
        return consignorCity;
    }

    public void setConsignorCity(String consignorCity) {
        this.consignorCity = consignorCity;
    }

    public String getConsignorDistrict() {
        return consignorDistrict;
    }

    public void setConsignorDistrict(String consignorDistrict) {
        this.consignorDistrict = consignorDistrict;
    }

    public String getConsignorAddress() {
        return consignorAddress;
    }

    public void setConsignorAddress(String consignorAddress) {
        this.consignorAddress = consignorAddress;
    }

    public String getConsignorStreet() {
        return consignorStreet;
    }

    public void setConsignorStreet(String consignorStreet) {
        this.consignorStreet = consignorStreet;
    }

    public String getOrderMemo() {
        return orderMemo;
    }

    public void setOrderMemo(String orderMemo) {
        this.orderMemo = orderMemo;
    }

    public Integer getCollectMode() {
        return collectMode;
    }

    public void setCollectMode(Integer collectMode) {
        this.collectMode = collectMode;
    }

    public String getThirdEnterpriseCode() {
        return thirdEnterpriseCode;
    }

    public void setThirdEnterpriseCode(String thirdEnterpriseCode) {
        this.thirdEnterpriseCode = thirdEnterpriseCode;
    }

    public BigDecimal getFreeDeliveryMoney() {
        return freeDeliveryMoney;
    }

    public void setFreeDeliveryMoney(BigDecimal freeDeliveryMoney) {
        this.freeDeliveryMoney = freeDeliveryMoney;
    }

    public Integer getShowLogisticsType() {
        return showLogisticsType;
    }

    public void setShowLogisticsType(Integer showLogisticsType) {
        this.showLogisticsType = showLogisticsType;
    }

    public String getShowLogisticsLink() {
        return showLogisticsLink;
    }

    public void setShowLogisticsLink(String showLogisticsLink) {
        this.showLogisticsLink = showLogisticsLink;
    }

    public Integer getMerchantType() {
        return merchantType;
    }

    public void setMerchantType(Integer merchantType) {
        this.merchantType = merchantType;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public List<String> getSupportGiveModeNameList() {
        return supportGiveModeNameList;
    }

    public void setSupportGiveModeNameList(List<String> supportGiveModeNameList) {
        this.supportGiveModeNameList = supportGiveModeNameList;
    }

    public Integer getCheckInventoryType() {
        return checkInventoryType;
    }

    public void setCheckInventoryType(Integer checkInventoryType) {
        this.checkInventoryType = checkInventoryType;
    }

    public Integer getCheckInventoryWay() {
        return checkInventoryWay;
    }

    public void setCheckInventoryWay(Integer checkInventoryWay) {
        this.checkInventoryWay = checkInventoryWay;
    }

    public Integer getLogisticsMergeFlag() {
        return logisticsMergeFlag;
    }

    public void setLogisticsMergeFlag(Integer logisticsMergeFlag) {
        this.logisticsMergeFlag = logisticsMergeFlag;
    }

    public String getLogisticsMergeTime() {
        return logisticsMergeTime;
    }

    public void setLogisticsMergeTime(String logisticsMergeTime) {
        this.logisticsMergeTime = logisticsMergeTime;
    }
}
