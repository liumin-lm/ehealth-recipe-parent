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
cancelRecipeHis 患者端提交订单取消订单时取药his处方
updateRecipeOrderStatus 运营平台订单编辑接口
updateRecipeTrannckingInfo 基础服务更新处方物流状态
updateRecipeStatus 浙江省互联网定时将确认中处方修改为审核中或者待处理(根据机构配置项noNeedNoticeOrganFlag过滤) 
getOrderDetail 订单页获取订单详情
eh.recipeService  findPatientRecipesByIdsAndDepId 确认订单获取处方详情
互联网处方新增接口流程：
sendRecipeToHIS->recipeStatusToHis(MQ发送消息)->HisMQService.recordRecipeTmpByOns(MQ接收消息)->noticeHisRecipeInfo(接口通知his平台新增了处方)
->his通过QueryRecipe反查平台处方信息新增到his
->RecipeStatusFromHisObserver his将处方状态通知平台
findRecipeListForPatientByTabStatus 患者端列表展示
getPatientRecipeById 患者端处方详情
药师CA异步回调：retryCaPharmacistCallBackToRecipe
门诊处方药品说明书：recipeaudit.prescriptionService getDrugSpecificationV1
门诊处方用药指导：recipe.outRecipePatientAtop getMedicationGuide
审核不通过，二次审核等操作：doAfterCheckNotPassYs
扁鹊回传处方状态接口：recipeStatusNotice
购药方式保存：StockBusinessService saveGiveMode
医生二次签名/审核强制通过 doSecondSignRecipe
医生端设置购药方式：validateRecipeGiveMode
机构药品目录药品上传监管平台：uploadDrugToRegulation
批量同步机构药品到监管平台：uploadOrgansDrugToRegulation
杭州互联网设置医保自费按钮操作 setConfirmOrderExtInfo
复诊查询处方状态是否有效 judgeRecipeStatus
患者端下单校验药企库存接口 getOrderStockFlag
浙江省互联网反查处方写入his queryRecipeInfo
确认订单页 eh.payService findConfirmOrderInfoExt  对应处方接口：obtainConfirmOrder
```
```
关于机构药品目录同步流程及接口描述：
一、配置项
机构药品目录-同步设置
涉及配置：接口对接模式  自主查询  主动推送
新增数据审核模式：系统审核  人工审核
二、配置项对应的表及字段
basic库base_organconfig
配置项同步数据类型中，新增数据->enable_drug_add 删除数据->enable_drug_delete
三、对应接口
自主查询：
X-Service-Id: eh.recipeService 
X-Service-Method: drugInfoSynMovementDTask 
主动推送：
X-Service-Id: eh.recipeService 
X-Service-Method: syncOrganDrug 
四、流程
新增数据审核模式为系统审核时，接口对接模式为自主查询，平台根据配置的定时时间从his主动查询线下药品信息，
获取到药品信息后，首先根据机构药品编码和机构药品目录比对，如果不存在，则新增到临时表和平台药品目录并标明药品来源，
并获取到平台药品id，以便直接新增到机构药品目录，在新增的过程中，会对线下药品进行校验，以下字段：
organDrugCode药品编码、drugName药品名称、useDose单次剂量(规格单位)[注中药可不填]、drugSpec药品规格、
drugType药品类型、pack药品包装数量、unit药品单位、producer药品生产厂家、usePathways用药途径（需要能对照上）、usingRate用药频次
```
### End
