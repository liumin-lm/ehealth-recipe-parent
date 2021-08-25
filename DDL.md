### 处方字段逐步优化计划方案

#### 1. 删除目前业务中已经没有使用到的字段

### recipe
第一期 recipe-》 order表中字段迁移 ：需要确定前端是否使用 8月
cdr_hisprescription
cdr_hisprescription_detail

物流信息跟支付一样,已存在order表中

```
`AddressID` int(11) DEFAULT NULL COMMENT '收货地址ID',
`Receiver` varchar(20) DEFAULT NULL COMMENT '收货人',
`RecMobile` varchar(20) DEFAULT NULL COMMENT '收货人手机号',
`RecTel` varchar(20) DEFAULT NULL COMMENT '收货人电话' ,
`Address1` varchar(20) DEFAULT NULL COMMENT '省',
`Address2` varchar(20) DEFAULT NULL COMMENT '市',
`Address3` varchar(20) DEFAULT NULL COMMENT '区',
`Address4` varchar(255) DEFAULT NULL COMMENT '详细地址',
`ZipCode` varchar(20) DEFAULT NULL COMMENT  '邮政编码',
`startSendDate` datetime DEFAULT NULL COMMENT  '准备配送时间',
```

```
`dispens_people` varchar(16) DEFAULT NULL COMMENT '调配人',(无代码调用,无意义,可删除)
```



支付 order已存在支付相关字段

```
`PayListID` int(11) DEFAULT NULL COMMENT '结算单号',
`TransValue` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '运费',(recipe 类中没有)
`CouponId` int(11) DEFAULT NULL COMMENT '优惠券Id',(order)
`PayMode` tinyint(3) unsigned DEFAULT NULL COMMENT '支付方式 1线上支付 2货到付款 3到院支付',(order)
`TradeNo` varchar(100) DEFAULT NULL COMMENT '交易流水号',(order)
`WxPayWay` varchar(10) DEFAULT NULL COMMENT '支付方式',(order)
`OutTradeNo` varchar(64) DEFAULT NULL COMMENT '商户订单号',(order)
`payOrganId` varchar(30) DEFAULT NULL COMMENT '支付平台分配的机构id',(order)
`WxPayErrorCode` varchar(32) DEFAULT NULL COMMENT '微信支付错误码',(order)
```

第二期 12 做掉

这部分字段已经迁移到ca表

```
`signRecipeCode` text COMMENT '医生处方数字签名值',
`signPharmacistCode` text COMMENT '药师处方数字签名值',
`signCADate` text COMMENT '医生处方签名生成的时间戳结构体，由院方服务器获取',
`signPharmacistCADate` text COMMENT '药师处方签名生成的时间戳结构体，由院方服务器获取',
```


审核相关 之前跟响响确认过相关字段审核信息都有,如果没有业务必须要冗余的,建议只留一个审核状态

```
`CheckOrgan` int(11) DEFAULT NULL COMMENT '审核机构',
`Checker` int(11) DEFAULT NULL COMMENT '审核人' ,
`CheckerText` varchar(64) DEFAULT NULL COMMENT '药师姓名',
`CheckFailMemo` varchar(255) DEFAULT NULL COMMENT '审核失败原因',
`SupplementaryMemo` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '药师审核不通过，医生补充说明',
`CheckDateYs` datetime DEFAULT NULL COMMENT '药师审核时间',(人工审方时间)
`CheckDate` datetime DEFAULT NULL COMMENT '审核时间',(CheckDateYs重复,自动审方时使用区分自动审方与人工审方可以使用checkType区分)
```




### recipe_ext
第一期改动 recipe_ext-> order

数据库有 类没有

```
`registerNo` varchar(200) DEFAULT '' COMMENT '门诊挂号序号（医保）',
`hisSettlementNo` varchar(200) DEFAULT '' COMMENT 'HIS收据号（医保）',
`preSettleTotalAmount` varchar(10) DEFAULT NULL COMMENT '处方预结算返回支付总金额',
`fundAmount` varchar(10) DEFAULT NULL COMMENT '处方预结算返回医保支付金额',
`cashAmount` varchar(10) DEFAULT NULL COMMENT '处方预结算返回自费金额',

```

```
`skin_test` varchar(64) DEFAULT NULL COMMENT '皮肤反应测验',
```

第二期 ： 可以在第一期 删除字段的同时 与前端切换接口字段，切换之后第二期删除

电子病历相关 都已经同步在电子病历 （兼容老接口所以字段在用 涉及到前端改动）

```
`mainDieaseDescribe` varchar(100) DEFAULT NULL COMMENT '主诉',
`currentMedical` varchar(1000) DEFAULT NULL COMMENT '现病史',
`histroyMedical` varchar(1000) DEFAULT NULL COMMENT '既往史',
`allergyMedical` varchar(1000) DEFAULT NULL COMMENT '过敏史',
`onsetDate` date DEFAULT NULL COMMENT '发病日期',
`historyOfPresentIllness` varchar(500) DEFAULT NULL COMMENT '现病史',
`handleMethod` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '处理方法',
`physicalCheck` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '体格检查',
```
审方 也已经同步在审方表

```
`drugEntrustment` varchar(2000) DEFAULT NULL COMMENT '审方用药医嘱',(平台审方时药师写的医嘱)(拿掉)
```



第二期 可延后 不确定前端是否使用

#### 2.合并含义相同字段

1.合并时要写清楚备注不使用了,现在使用的字段是哪个,防止用错字段

2.2个版本之后没问题及时删除

```
`fromflag` int(11) DEFAULT '0' COMMENT '处方来源 0:HIS，1:平台',(拿掉)
`recipeSourceType` tinyint(4) DEFAULT '1' COMMENT '处方来源类型 0:his 1 平台处方 2 线下转线上的处方',
ext 表
`fromFlag` tinyint(4) DEFAULT NULL COMMENT '处方来源 0 线下his同步 1 平台处方',(说是recipe表的是给邵逸夫线下转线上,ext是给其他线下转线上用的,有处方来源,能否合并?)
```

```
`ChemistSignFile` varchar(50) DEFAULT NULL COMMENT '药师签名'
`SignFile` varchar(50) DEFAULT NULL COMMENT '医生签名',只要其中一个
```

```
`ChooseFlag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '用户选择购药方式：0未确认，1已确定',(如果是为了推送药企的标识,pushflag可以替代,用户是否选择购药方式 givemode is not null替代)
`PushFlag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '药企推送标志位, 0未推送，1已推送',
```

```
`recipePayType` tinyint(4) DEFAULT '0' COMMENT '处方支付类型 0 普通支付  1 直接第三方支付',
`payMode` tinyint(1) DEFAULT NULL COMMENT '''处方费用支付方式 1 线上支付 2 线下支付''',
```

```
`recipeCostNumber` varchar(200) DEFAULT NULL COMMENT 'his处方付费序号合集',(处方)
`PayListID` int(11) DEFAULT NULL COMMENT '结算单号',
```

```
取其一
`rxid` varchar(100) DEFAULT NULL COMMENT '第三方处方ID',
`rxNo` varchar(16) DEFAULT NULL COMMENT '天猫返回的处方编号',
```

三期 ：业务字段迁移

#### 3.逐步迁移表字段

迁移规则:

使用频繁的字段放在主表

列表查询相关放在主表

根据对应的业务关系,例如发药信息应该迁移到订单中

使用数据时仅操作单条数据的放在ext表

迁移的时候优化相关代码逻辑



recipe 字段

```
用户信息,仅在推送时使用,放在ext表
`currentClient` int(11) DEFAULT NULL COMMENT '当前用户设备Id',
`baseClientId` int(11) DEFAULT NULL COMMENT 'base_ClientConfig表ID',
`requestMpiId` varchar(32) DEFAULT NULL COMMENT '处方发起人账号id',
`requestUrt` int(11) DEFAULT NULL COMMENT '处方发起人设备信息',
```

```
原先用于对购药方式的一个判断,实际含义1是药企有库存 2是医院有库存,现新增了recipeSupportGiveMode字段,对处方支持的购药方式做了详细的区分
`DistributionFlag` tinyint(1) DEFAULT NULL COMMENT '配送处方标记 默认0，1: 只能配送',
```

```
仅开方时用到,放ext表
`TakeMedicine` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '外带处方标志 1:外带药处方',(开药的时候回用到)
```

```
这种标志位,一次使用没有后续业务逻辑 放ext
`RemindFlag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否进行处方失效提醒',
`PushFlag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '药企推送标志位, 0未推送，1已推送',
`syncFlag` tinyint(1) unsigned DEFAULT '0' COMMENT '监管平台同步标记: 0未同步，1已同步',
`checkStatus` int(11) DEFAULT '0' COMMENT '处方审核的状态（0：常态，非一次审核不通过，1：一次审核不通过）',
`checkMode` tinyint(4) DEFAULT '1' COMMENT '审方途径 1 平台审方 2 HIS审方',
`grabOrderStatus` tinyint(4) DEFAULT '0' COMMENT '是否被接方 0 未接方 1已接方',
`invalidTime` datetime DEFAULT NULL COMMENT '处方失效时间',
```

```
无业务逻辑,仅保存了字段 放ext
`OldRecipeId` int(11) DEFAULT NULL COMMENT '药师审核不通过的旧处方Id',(没有代码调用,线上数据库有数据,重新开具处方)
```

```
发药情况 属于订单放order
`GiveOrgan` int(20) DEFAULT NULL COMMENT '发药机构',
`GiveFlag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '发药状态',
`GiveDate` datetime DEFAULT NULL COMMENT '发药日期',
`GiveUser` varchar(20) DEFAULT NULL COMMENT '发药人',(实际代码含义,是配送员 sender 字段)
```

```
中药信息 跟大部队一起放ext 
`CopyNum` int(11) DEFAULT NULL COMMENT'剂数',
`RecipeMemo` varchar(100) DEFAULT NULL COMMENT '处方备注'(嘱托),
```

```
`PushFlag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '药企推送标志位, 0未推送，1已推送',
`EnterpriseId` int(11) DEFAULT NULL COMMENT '药企ID',(主要是扁鹊等第三方使用,平台处方使用order中的药企id)
```


recipe不能动,第三方订单返回数据需要回写
```
`PayDate` datetime DEFAULT NULL COMMENT '支付日期' ,
`ActualPrice` decimal(10,2) DEFAULT NULL COMMENT '最终支付费用',(order)
```
ext 正在使用,先不动
```
`payAmount` varchar(10) CHARACTER SET utf8 DEFAULT NULL COMMENT '预结算返回应付金额',
```
第一期延后
```
`Sender` varchar(30) DEFAULT NULL COMMENT '配送人',
`startSendDate` datetime DEFAULT NULL COMMENT  '准备配送时间',
有2021年8月份的信息
这两个字段订单表中没有 放到二期 一期要先加字段 迁移数据
修改入库代码
```