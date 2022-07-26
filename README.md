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
更新购药方式地址-->updateTakeDrugWay

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
updateRecipeStatus 浙江省互联网定时将确认中处方修改为审核中或者待处理(根据机构配置项noNeedNoticeOrganFlag过滤) 
getOrderDetail 订单页获取订单详情
eh.recipeService  findPatientRecipesByIdsAndDepId 确认订单获取处方详情
互联网处方新增接口流程：
sendRecipeToHIS->recipeStatusToHis(MQ发送消息)->HisMQService.recordRecipeTmpByOns(MQ接收消息)->noticeHisRecipeInfo(接口通知his平台新增了处方)
->his通过QueryRecipe反查平台处方信息新增到his
->RecipeStatusFromHisObserver his将处方状态通知平台
findRecipesForPatientAndTabStatusNew 患者端列表展示
getPatientRecipeById 患者端处方详情
药师CA异步回调：retryCaPharmacistCallBackToRecipe
门诊处方药品说明书：recipeaudit.prescriptionService getDrugSpecificationV1
门诊处方用药指导：recipe.outRecipePatientAtop getMedicationGuide
审核不通过，二次审核等操作：doAfterCheckNotPassYs
扁鹊回传处方状态接口：recipeStatusNotice
```
### End