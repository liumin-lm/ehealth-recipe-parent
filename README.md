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
### End