### 平台与前置机各流程标准接口

```
处方预校验->hisCheckRecipe
处方新增->recipeSend 
his库存查询->scanDrugStock 
处方撤销/已完成状态更新-->recipeUpdate 
处方审核结果通知-->recipeAudit 
更新患者取药方式-->drugTakeChange
平台定时查询his处方状态(支付/完成)-->listQuery
处方支付结算通知(自费)-->payNotify 
处方退款通知his-->recipeRefund
线下处方记录查询-->queryRecipeListInfo
处方医保预结算-->recipeMedicalPreSettleN
处方自费预结算-->recipeCashPreSettle
处方医保结算-->recipeMedicalSettle

查询his药品信息并价格同步->queryDrugInfo
```
### 数据库表描述
```
base_drug_decoctionway 煎法表
```
### 常用接口定位
```
cancelRecipe 处方撤销
submitRecipeHis 患者创建订单 根据配送方式上传处方给his  配置项  patientRecipeUploadHis 患者提交订单需要回写his的购药方式
cancelOrder 订单取消
updateRecipeOrderStatus 运营平台订单编辑接口
updateRecipeTrannckingInfo 基础服务更新处方物流状态
updateRecipeStatus 浙江省互联网定时将确认中处方修改为审核中或者待处理 

```
### End