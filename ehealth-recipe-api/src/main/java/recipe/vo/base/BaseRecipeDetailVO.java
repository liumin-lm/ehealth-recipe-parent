package recipe.vo.base;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 处方详情基础的字段定义
 *
 * @author yinsheng
 */
@Getter
@Setter
public class BaseRecipeDetailVO implements Serializable {
    private static final long serialVersionUID = -2102586753542432308L;
    @ItemProperty(alias = "药品商品名")
    private String saleName;

    @ItemProperty(alias="处方序号")
    private Integer recipeId;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias="药企药品编号")
    private String saleDrugCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias="药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物单位")
    private String unit;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias = "药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias = "药物使用规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "药物剂量单位")
    private String dosageUnit;

    @ItemProperty(alias = "药物使用频率")
    private String usingRateName;

    @ItemProperty(alias = "药物使用途径")
    private String usePathwaysName;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "药物金额 = useTotalDose * salePrice")
    private BigDecimal drugCost;

    @ItemProperty(alias = "药品嘱托信息")
    private String memo;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "销售价格 = organDrug.salePrice")
    private BigDecimal salePrice;

    @ItemProperty(alias = "取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;
}
