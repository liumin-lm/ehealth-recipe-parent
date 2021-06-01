package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author yuyun
 */
@Schema
@Entity
@Table(name = "cdr_recipedetail")
@Access(AccessType.PROPERTY)
public class Recipedetail implements java.io.Serializable {

	private static final long serialVersionUID = -5228478904040591198L;

	@ItemProperty(alias = "药品商品名")
	private String saleName;

	@ItemProperty(alias="处方明细序号")
	private Integer recipeDetailId;
	
	@ItemProperty(alias="处方序号")
	private Integer recipeId;
	
	@ItemProperty(alias="药品组号")
	private String drugGroup;
	
	@ItemProperty(alias="药品序号")
	private Integer drugId;
	
	@ItemProperty(alias="机构药品编号")
	private String organDrugCode;
	
	@ItemProperty(alias="药物名称")
	private String drugName;
	
	@ItemProperty(alias="药物规格")
	private String drugSpec;

	@ItemProperty(alias = "药品包装数量")
	private Integer pack;
	
	@ItemProperty(alias="药物单位")
	private String drugUnit;
	
	@ItemProperty(alias="药物使用次剂量")
	private Double useDose;

	@ItemProperty(alias="默认每次剂量")
	private Double defaultUseDose;

    @ItemProperty(alias="药物使用次剂量--中文标识-适量")
    private String useDoseStr;

	@ItemProperty(alias="药物使用规格单位")
	private String useDoseUnit;

	@ItemProperty(alias="药物剂量单位")
	private String dosageUnit;

	@ItemProperty(alias="平台药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
	private String usingRate;

	@ItemProperty(alias="平台药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
	private String usePathways;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(
            alias = "机构的频次代码"
    )
    private String organUsingRate;
    @ItemProperty(
            alias = "机构的用法代码"
    )
    private String organUsePathways;

	//用药频率说明（来源his）
	@ItemProperty(alias="用药频率说明")
	private String usingRateTextFromHis; //防止覆盖原有usingRateText

	//用药方式说明（来源his）
	@ItemProperty(alias="用药方式说明")
	private String usePathwaysTextFromHis;//防止覆盖原有usePathwaysText

	@ItemProperty(alias="药物使用总数量")
	private Double useTotalDose;
	
	@ItemProperty(alias="药物发放数量")
	private Double sendNumber;
	
	@ItemProperty(alias="药物使用天数")
	private Integer useDays;
	
	@ItemProperty(alias="药物金额")
	private BigDecimal drugCost;

	@ItemProperty(alias="药品嘱托Id")
	private String entrustmentId;

	@ItemProperty(alias="药品嘱托信息")
	private String memo;

	@ItemProperty(alias="药品嘱托编码")
	private String drugEntrustCode;

	@ItemProperty(alias="药品效期")
	private Date validDate;
	
	@ItemProperty(alias="药品批号")
	private String drugBatch;
	
	@ItemProperty(alias="创建时间")
	private Date createDt;
	
	@ItemProperty(alias="最后修改时间")
	private Date lastModify;
	
	@ItemProperty(alias="销售价格")
	private BigDecimal salePrice;
	
	@ItemProperty(alias="药品编码")
	private String drugCode;
	
	@ItemProperty(alias="无税单价")
	private BigDecimal price;
	
	@ItemProperty(alias="税率")
	private Double rate;
	
	@ItemProperty(alias="含税单价")
	private BigDecimal ratePrice;

    @ItemProperty(alias="无税总额")
    private BigDecimal totalPrice;

    @ItemProperty(alias="税额")
    private BigDecimal tax;

    @ItemProperty(alias="含税总额")
    private BigDecimal totalRatePrice;

    @ItemProperty(alias="医院系统医嘱号")
    private String orderNo;

    @ItemProperty(alias="取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "是否启用")
    private Integer status;

    @ItemProperty(alias="药企发票编号")
    private String invoiceNo;

    @ItemProperty(alias="药企开票日期")
    private Date invoiceDate;

    @ItemProperty(alias="医院给患者发票编号")
    private String patientInvoiceNo;

    @ItemProperty(alias="医院给患者开票日期")
    private Date patientInvoiceDate;

	@ItemProperty(alias="处方明细单号")
	private String recipedtlno;

	@ItemProperty(alias = "剂型")
	private String drugForm;

	//date 20200225
	//同步机构药品信息新添字段
	@ItemProperty(alias = "生产厂家")
	private String producer;

	//date 20200225
	//同步机构药品信息新添字段
	@ItemProperty(alias = "批准文号")
	private String licenseNumber;

	//date 20200225
	//同步机构药品信息新添字段
	@ItemProperty(alias = "生产厂家代码")
	private String producerCode;

	@ItemProperty(alias = "实际销售价格")
	private BigDecimal actualSalePrice;

	@ItemProperty(alias = "药企药品编码")
	private String saleDrugCode;

	@ItemProperty(alias = "结算方式 0:药店价格 1:医院价格")
	private Integer settlementMode;

	@ItemProperty(alias = "药物使用天数(小数类型)")
	private String useDaysB;
	@ItemProperty(alias = "处方药品详情类型")
	@Dictionary(id = "eh.base.dictionary.DrugType")
	private Integer drugType;

	/**
	 * 医保药品编码
	 */
	private String medicalDrugCode;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

	@Column(name = "UseDaysB")
	public String getUseDaysB() {
		return useDaysB;
	}

	@Column(name = "drugType")
	public Integer getDrugType() {
		return drugType;
	}

	@ItemProperty(alias = "开处方时保存单位剂量【规格单位】|单位【规格单位】|单位剂量【最小单位】|单位【最小单位】,各个字段用|隔开，用来计算患者端显示实际每次剂量")
	private String drugUnitdoseAndUnit;

	@ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
	private Integer tcmContraindicationType;

	@ItemProperty(alias = "中药禁忌原因")
	private String tcmContraindicationCause;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

	@ItemProperty(alias = "单个药品医保类型")
	private Integer drugMedicalFlag;

	@Column(name = "drugEntrustCode")
	public String getDrugEntrustCode() {
		return drugEntrustCode;
	}

	public void setDrugEntrustCode(String drugEntrustCode) {
		this.drugEntrustCode = drugEntrustCode;
	}

	@Column(name = "entrustmentId")
	public String getEntrustmentId() {
		return entrustmentId;
	}

	public void setEntrustmentId(String entrustmentId) {
		this.entrustmentId = entrustmentId;
	}

	public void setUseDaysB(String useDaysB) {
		this.useDaysB = useDaysB;
	}
	public void setDrugType(Integer drugType) {
		this.drugType = drugType;
	}

	public Recipedetail() {
	}

	public Recipedetail(Integer recipeId, Integer drugId, String drugName,
			String drugSpec, String drugUnit, Double sendNumber, BigDecimal drugCost) {
		this.recipeId = recipeId;
		this.drugId = drugId;
		this.drugName = drugName;
		this.drugSpec = drugSpec;
		this.drugUnit = drugUnit;
		this.sendNumber = sendNumber;
		this.drugCost = drugCost;
	}

	public Recipedetail(Integer recipeId, String drugGroup, Integer drugId,
			String organDrugCode, String drugName, String drugSpec,
			String drugUnit, Double useDose, String useDoseUnit,
			String usingRate, String usePathways, Double useTotalDose, Double sendNumber,
			Integer useDays, BigDecimal salePrice, BigDecimal drugCost, String memo,
			Timestamp createDt, Timestamp lastModify, Timestamp validDate,
			String drugBatch, String drugCode, BigDecimal price, Double rate, BigDecimal ratePrice) {
		this.recipeId = recipeId;
		this.drugGroup = drugGroup;
		this.drugId = drugId;
		this.organDrugCode = organDrugCode;
		this.drugName = drugName;
		this.drugSpec = drugSpec;
		this.drugUnit = drugUnit;
		this.useDose = useDose;
		this.useDoseUnit = useDoseUnit;
		this.usingRate = usingRate;
		this.usePathways = usePathways;
		this.useTotalDose = useTotalDose;
		this.sendNumber = sendNumber;
		this.useDays = useDays;
		this.salePrice = salePrice;
		this.drugCost = drugCost;
		this.memo = memo;
		this.createDt = createDt;
		this.lastModify = lastModify;
		this.validDate = validDate;
		this.drugBatch = drugBatch;
		this.drugCode = drugCode;
		this.price = price;
		this.rate = rate;
		this.ratePrice = ratePrice;
	}

	public Recipedetail(Integer recipeDetailId, Integer recipeId, String drugGroup, Integer drugId, String organDrugCode, String drugName, String drugSpec, Integer pack, String drugUnit, Double useDose, Double defaultUseDose, String useDoseStr, String useDoseUnit, String dosageUnit, String usingRate, String usePathways, String organUsingRate, String organUsePathways, String usingRateTextFromHis, String usePathwaysTextFromHis, Double useTotalDose, Double sendNumber, Integer useDays, BigDecimal drugCost, String memo, Date validDate, String drugBatch, Date createDt, Date lastModify, BigDecimal salePrice, String drugCode, BigDecimal price, Double rate, BigDecimal ratePrice, BigDecimal totalPrice, BigDecimal tax, BigDecimal totalRatePrice, String orderNo, String pharmNo, Integer status, String invoiceNo, Date invoiceDate, String patientInvoiceNo, Date patientInvoiceDate, String recipedtlno, String drugForm, String producer, String licenseNumber, String producerCode, BigDecimal actualSalePrice, String saleDrugCode, Integer settlementMode, String useDaysB, Integer drugType, String medicalDrugCode, Integer pharmacyId, String pharmacyName, String drugUnitdoseAndUnit, Integer tcmContraindicationType, String tcmContraindicationCause) {
		this.recipeDetailId = recipeDetailId;
		this.recipeId = recipeId;
		this.drugGroup = drugGroup;
		this.drugId = drugId;
		this.organDrugCode = organDrugCode;
		this.drugName = drugName;
		this.drugSpec = drugSpec;
		this.pack = pack;
		this.drugUnit = drugUnit;
		this.useDose = useDose;
		this.defaultUseDose = defaultUseDose;
		this.useDoseStr = useDoseStr;
		this.useDoseUnit = useDoseUnit;
		this.dosageUnit = dosageUnit;
		this.usingRate = usingRate;
		this.usePathways = usePathways;
		this.organUsingRate = organUsingRate;
		this.organUsePathways = organUsePathways;
		this.usingRateTextFromHis = usingRateTextFromHis;
		this.usePathwaysTextFromHis = usePathwaysTextFromHis;
		this.useTotalDose = useTotalDose;
		this.sendNumber = sendNumber;
		this.useDays = useDays;
		this.drugCost = drugCost;
		this.memo = memo;
		this.validDate = validDate;
		this.drugBatch = drugBatch;
		this.createDt = createDt;
		this.lastModify = lastModify;
		this.salePrice = salePrice;
		this.drugCode = drugCode;
		this.price = price;
		this.rate = rate;
		this.ratePrice = ratePrice;
		this.totalPrice = totalPrice;
		this.tax = tax;
		this.totalRatePrice = totalRatePrice;
		this.orderNo = orderNo;
		this.pharmNo = pharmNo;
		this.status = status;
		this.invoiceNo = invoiceNo;
		this.invoiceDate = invoiceDate;
		this.patientInvoiceNo = patientInvoiceNo;
		this.patientInvoiceDate = patientInvoiceDate;
		this.recipedtlno = recipedtlno;
		this.drugForm = drugForm;
		this.producer = producer;
		this.licenseNumber = licenseNumber;
		this.producerCode = producerCode;
		this.actualSalePrice = actualSalePrice;
		this.saleDrugCode = saleDrugCode;
		this.settlementMode = settlementMode;
		this.useDaysB = useDaysB;
		this.drugType = drugType;
		this.medicalDrugCode = medicalDrugCode;
		this.pharmacyId = pharmacyId;
		this.pharmacyName = pharmacyName;
		this.drugUnitdoseAndUnit = drugUnitdoseAndUnit;
		this.tcmContraindicationType = tcmContraindicationType;
		this.tcmContraindicationCause = tcmContraindicationCause;
	}

	public Integer getTcmContraindicationType() {
		return tcmContraindicationType;
	}

	public void setTcmContraindicationType(Integer tcmContraindicationType) {
		this.tcmContraindicationType = tcmContraindicationType;
	}

	public String getTcmContraindicationCause() {
		return tcmContraindicationCause;
	}

	public void setTcmContraindicationCause(String tcmContraindicationCause) {
		this.tcmContraindicationCause = tcmContraindicationCause;
	}

	@Column(name = "licenseNumber")
	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	@Column(name = "ProducerCode", length = 20)
	public String getProducerCode() {
		return producerCode;
	}

	public void setProducerCode(String producerCode) {
		this.producerCode = producerCode;
	}

	@Column(name = "producer")
	public String getProducer() {
		return producer;
	}

	public void setProducer(String producer) {
		this.producer = producer;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "RecipeDetailID", unique = true, nullable = false)
	public Integer getRecipeDetailId() {
		return this.recipeDetailId;
	}

	public void setRecipeDetailId(Integer recipeDetailId) {
		this.recipeDetailId = recipeDetailId;
	}

	@Column(name = "RecipeID", nullable = false)
	public Integer getRecipeId() {
		return this.recipeId;
	}

	public void setRecipeId(Integer recipeId) {
		this.recipeId = recipeId;
	}

	@Column(name = "DrugGroup")
	public String getDrugGroup() {
		return this.drugGroup;
	}

	public void setDrugGroup(String drugGroup) {
		this.drugGroup = drugGroup;
	}

	@Column(name = "DrugID")
	public Integer getDrugId() {
		return this.drugId;
	}

	public void setDrugId(Integer drugId) {
		this.drugId = drugId;
	}

    @Column(name = "OrganDrugCode", length = 30)
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
		this.organDrugCode = organDrugCode;
    }

	@Column(name = "DrugName", nullable = false, length = 50)
	public String getDrugName() {
		return this.drugName;
	}

	public void setDrugName(String drugName) {
		this.drugName = drugName;
	}

	@Column(name = "DrugSpec", nullable = false, length = 30)
	public String getDrugSpec() {
		return this.drugSpec;
	}

	public void setDrugSpec(String drugSpec) {
		this.drugSpec = drugSpec;
	}

	@Column(name = "Pack", nullable = false, length = 11)
	public Integer getPack() {
		return pack;
	}

	public void setPack(Integer pack) {
		this.pack = pack;
	}

	@Column(name = "DrugUnit")
	public String getDrugUnit() {
		return this.drugUnit;
	}

	public void setDrugUnit(String drugUnit) {
		this.drugUnit = drugUnit;
	}

	@Column(name = "UseDose")
	public Double getUseDose() {
		return this.useDose;
	}

	public void setUseDose(Double useDose) {
		this.useDose = useDose;
	}

	@Column(name = "DefaultUseDose")
	public Double getDefaultUseDose() {
		return defaultUseDose;
	}

	public void setDefaultUseDose(Double defaultUseDose) {
		this.defaultUseDose = defaultUseDose;
	}

	@Column(name = "UseDoseUnit", length = 6)
	public String getUseDoseUnit() {
		return this.useDoseUnit;
	}

	public void setUseDoseUnit(String useDoseUnit) {
		this.useDoseUnit = useDoseUnit;
	}

	@Column(name = "DosageUnit")
	public String getDosageUnit() {
		return dosageUnit;
	}

	public void setDosageUnit(String dosageUnit) {
		this.dosageUnit = dosageUnit;
	}

	@Column(name = "UsingRate", length = 20)
	public String getUsingRate() {
		return this.usingRate;
	}

	public void setUsingRate(String usingRate) {
		this.usingRate = usingRate;
	}

	@Column(name = "UsePathways", length = 20)
	public String getUsePathways() {
		return this.usePathways;
	}

	public void setUsePathways(String usePathways) {
		this.usePathways = usePathways;
	}

	@Column(name = "UseTotalDose", precision = 10, scale = 3)
	public Double getUseTotalDose() {
		return this.useTotalDose;
	}

	public void setUseTotalDose(Double useTotalDose) {
		this.useTotalDose = useTotalDose;
	}

	@Column(name = "SendNumber",  precision = 10, scale = 3)
	public Double getSendNumber() {
		return this.sendNumber;
	}

	public void setSendNumber(Double sendNumber) {
		this.sendNumber = sendNumber;
	}

	@Column(name = "UseDays")
	public Integer getUseDays() {
		return this.useDays;
	}

	public void setUseDays(Integer useDays) {
		this.useDays = useDays;
	}

    @Column(name = "DrugCost", precision = 10, scale = 3)
    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

	@Column(name = "Memo", length = 200)
	public String getMemo() {
		return this.memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	@Column(name = "ValidDate", length = 19)
	public Date getValidDate() {
		return this.validDate;
	}

	public void setValidDate(Date validDate) {
		this.validDate = validDate;
	}

	@Column(name = "DrugBatch", length = 30)
	public String getDrugBatch() {
		return this.drugBatch;
	}

	public void setDrugBatch(String drugBatch) {
		this.drugBatch = drugBatch;
	}
	@Column(name = "CreateDt", length = 19)
	public Date getCreateDt() {
		return this.createDt;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	@Column(name = "LastModify", length = 19)
	public Date getLastModify() {
		return this.lastModify;
	}

	public void setLastModify(Date lastModify) {
		this.lastModify = lastModify;
	}

    @Column(name = "salePrice", precision = 10)
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

	@Column(name = "DrugCode", length = 30)
	public String getDrugCode() {
		return this.drugCode;
	}

	public void setDrugCode(String drugCode) {
		this.drugCode = drugCode;
	}

    @Column(name = "Price", precision = 10)
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

	@Column(name = "Rate", precision = 10, scale = 4)
	public Double getRate() {
		return this.rate;
	}

	public void setRate(Double rate) {
		this.rate = rate;
	}

	@Column(name = "RatePrice", precision = 10)
    public BigDecimal getRatePrice() {
        return ratePrice;
    }

    public void setRatePrice(BigDecimal ratePrice) {
        this.ratePrice = ratePrice;
    }

    @Column(name = "OrderNo")
    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    @Column(name = "PharmNo")
    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "TotalPrice")
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Column(name = "Tax")
    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    @Column(name = "TotalRatePrice")
    public BigDecimal getTotalRatePrice() {
        return totalRatePrice;
    }

    public void setTotalRatePrice(BigDecimal totalRatePrice) {
        this.totalRatePrice = totalRatePrice;
    }

    @Column(name = "InvoiceNo")
    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    @Column(name = "InvoiceDate")
    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    @Column(name = "PatientInvoiceNo")
    public String getPatientInvoiceNo() {
        return patientInvoiceNo;
    }

    public void setPatientInvoiceNo(String patientInvoiceNo) {
        this.patientInvoiceNo = patientInvoiceNo;
    }

    @Column(name = "PatientInvoiceDate")
    public Date getPatientInvoiceDate() {
        return patientInvoiceDate;
    }

    public void setPatientInvoiceDate(Date patientInvoiceDate) {
        this.patientInvoiceDate = patientInvoiceDate;
    }

	@Column(name = "recipedtlno")
	public String getRecipedtlno() {
		return recipedtlno;
	}

	public void setRecipedtlno(String recipedtlno) {
		this.recipedtlno = recipedtlno;
	}

	@Transient
	public String getDrugForm() {
		return drugForm;
	}

	public void setDrugForm(String drugForm) {
		this.drugForm = drugForm;
	}

	@Column(name = "actualSalePrice")
	public BigDecimal getActualSalePrice() {
		return actualSalePrice;
	}

	public void setActualSalePrice(BigDecimal actualSalePrice) {
		this.actualSalePrice = actualSalePrice;
	}

	@Column(name = "saleDrugCode")
	public String getSaleDrugCode() {
		return saleDrugCode;
	}

	public void setSaleDrugCode(String saleDrugCode) {
		this.saleDrugCode = saleDrugCode;
	}

    @Column(name = "useDoseStr")
    public String getUseDoseStr() {
        return useDoseStr;
    }

    public void setUseDoseStr(String useDoseStr) {
        this.useDoseStr = useDoseStr;
    }

	@Column(name = "usingRateText")
	public String getUsingRateTextFromHis() {
		return usingRateTextFromHis;
	}

	@Column(name = "usePathwaysText")
	public String getUsePathwaysTextFromHis() {
		return usePathwaysTextFromHis;
	}

	public void setUsingRateTextFromHis(String usingRateTextFromHis) {
		this.usingRateTextFromHis = usingRateTextFromHis;
	}

	public void setUsePathwaysTextFromHis(String usePathwaysTextFromHis) {
		this.usePathwaysTextFromHis = usePathwaysTextFromHis;
	}

	@Transient
	public String getMedicalDrugCode() {
		return medicalDrugCode;
	}

	public void setMedicalDrugCode(String medicalDrugCode) {
		this.medicalDrugCode = medicalDrugCode;
	}

    @Column(name = "organUsingRate")
    public String getOrganUsingRate() {
        return organUsingRate;
    }

    public void setOrganUsingRate(String organUsingRate) {
        this.organUsingRate = organUsingRate;
    }

    @Column(name = "organUsePathways")
    public String getOrganUsePathways() {
        return organUsePathways;
    }

    public void setOrganUsePathways(String organUsePathways) {
        this.organUsePathways = organUsePathways;
    }

	public String getDrugUnitdoseAndUnit() {
		return drugUnitdoseAndUnit;
	}

	public void setDrugUnitdoseAndUnit(String drugUnitdoseAndUnit) {
		this.drugUnitdoseAndUnit = drugUnitdoseAndUnit;
	}

	public Integer getSettlementMode() {
		return settlementMode;
	}

	public void setSettlementMode(Integer settlementMode) {
		this.settlementMode = settlementMode;
	}

    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    @Transient
    public String getUsingRateId() {
        return usingRateId;
    }

    public void setUsingRateId(String usingRateId) {
        this.usingRateId = usingRateId;
    }

    @Transient
    public String getUsePathwaysId() {
        return usePathwaysId;
    }

    public void setUsePathwaysId(String usePathwaysId) {
        this.usePathwaysId = usePathwaysId;
    }

	public void setSaleName(String saleName) {
		this.saleName = saleName;
	}

	public String getSaleName() {
		return saleName;
	}

    public String getDrugDisplaySplicedName() {
        return drugDisplaySplicedName;
    }

    public void setDrugDisplaySplicedName(String drugDisplaySplicedName) {
        this.drugDisplaySplicedName = drugDisplaySplicedName;
    }

    public String getDrugDisplaySplicedSaleName() {
        return drugDisplaySplicedSaleName;
    }

    public void setDrugDisplaySplicedSaleName(String drugDisplaySplicedSaleName) {
        this.drugDisplaySplicedSaleName = drugDisplaySplicedSaleName;
    }

    @Column(name = "drugMedicalFlag")
	public Integer getDrugMedicalFlag() {
		return drugMedicalFlag;
	}

	public void setDrugMedicalFlag(Integer drugMedicalFlag) {
		this.drugMedicalFlag = drugMedicalFlag;
	}
}
