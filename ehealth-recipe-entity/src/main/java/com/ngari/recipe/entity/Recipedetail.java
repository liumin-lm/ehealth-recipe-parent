package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yuyun
 */
@Schema
@Entity
@Table(name = "cdr_recipedetail")
@Access(AccessType.PROPERTY)
public class Recipedetail implements java.io.Serializable {

	private static final long serialVersionUID = -5228478904040591198L;

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

	@ItemProperty(alias="药物使用规格单位")
	private String useDoseUnit;

	@ItemProperty(alias="药物剂量单位")
	private String dosageUnit;

	@ItemProperty(alias="药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
	private String usingRate;

	@ItemProperty(alias="药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
	private String usePathways;
	
	@ItemProperty(alias="药物使用总数量")
	private Double useTotalDose;
	
	@ItemProperty(alias="药物发放数量")
	private Double sendNumber;
	
	@ItemProperty(alias="药物使用天数")
	private Integer useDays;
	
	@ItemProperty(alias="药物金额")
	private BigDecimal drugCost;
	
	@ItemProperty(alias="备注信息")
	private String memo;

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

	@Column(name = "Memo", length = 50)
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
}