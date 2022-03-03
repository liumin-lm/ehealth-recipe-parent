package recipe.vo.greenroom;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 获取机构药品目录出参
 *
 * @author lium
 */
@Schema
@Data
public class ListOrganDrugRes implements java.io.Serializable {
    private static final long serialVersionUID = -2026791423853766129L;

    @ItemProperty(alias = "机构药品序号")
    private Integer organDrugId;

    @ItemProperty(alias = "医疗机构代码")
    private Integer organId;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "化学名")
    private String chemicalName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "转换系数")
    private Integer pack;

    @ItemProperty(alias = "药品包装单位")
    private String unit;

    @ItemProperty(alias = "实际单次剂量（规格单位）")
    private Double useDose;

    @ItemProperty(alias = "推荐单次剂量（规格单位）")
    private Double recommendedUseDose;

    @ItemProperty(alias = "单次剂量单位（规格单位）")
    private String useDoseUnit;

    @ItemProperty(alias = "实际单位剂量（最小单位）")
    private Double smallestUnitUseDose;

    @ItemProperty(alias = "默认单位剂量（最小单位）")
    private Double defaultSmallestUnitUseDose;

    @ItemProperty(alias = "单位剂量单位（最小单位）")
    private String useDoseSmallestUnit;

    @ItemProperty(alias = "使用频率平台")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径平台")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "搜索关键字，一般包含通用名，商品名及医院自定义值")
    private String searchKey;

    @ItemProperty(alias = "销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "使用状态")
    @Dictionary(id = "eh.base.dictionary.OrganDrugStatus")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后生成时间")
    private Date lastModify;

    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;

    @ItemProperty(alias = "外带药标志 1:外带药")
    private Integer takeMedicine;

    @ItemProperty(alias = "院内检索关键字")
    private String retrievalCode;

    @ItemProperty(alias = "医院药房名字")
    private String pharmacyName;

    @ItemProperty(alias = "监管平台药品编码")
    private String regulationDrugCode;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "是否基药")
    private Integer baseDrug;

    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    @ItemProperty(alias = "包装材料")
    private String packingMaterials;

    @ItemProperty(alias = "医保药品编码")
    private String medicalDrugCode;
    @ItemProperty(alias = "HIS剂型编码")
    private String drugFormCode;
    @ItemProperty(alias = "医保剂型编码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    @ItemProperty(alias = "药房")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0   否，1  是   默认0")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症 说明")
    private String indicationsDeclare;

    @ItemProperty(alias = "是否支持下载处方笺 0   否，1  是   默认1")
    private Boolean supportDownloadPrescriptionPad;

    @ItemProperty(alias = "配送药企ids")
    private String drugsEnterpriseIds;


    @ItemProperty(alias = "药品适用业务 历史数据默认处理  1-药品处方  eh.base.dictionary.ApplyBusiness ")
    private String applyBusiness;

    @ItemProperty(alias = "单复方 ")
    @Dictionary(id = "eh.cdr.dictionary.UnilateralCompound")
    private Integer unilateralCompound;

}