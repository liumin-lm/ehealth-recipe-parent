package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author maoze
 * @description
 * @date 2021年10月12日 15:48
 */
@Data
public class RecipeRulesDrugcorrelationVo implements Serializable {
    private static final long serialVersionUID = -6604988044493266204L;

    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "合理用药规则Id")
    private Integer medicationRulesId;

    @ItemProperty(alias = "合理用药规则Id")
    private Integer drugRelationship;

    @ItemProperty(alias = "规则药品编码")
    private Integer drugId;

    @ItemProperty(alias = "规则药品名称")
    private String drugName;

    @ItemProperty(alias = "规则药品名称")
    private Integer correlationDrugId;

    @ItemProperty(alias = "规则药品名称")
    private String correlationDrugName;

    @ItemProperty(alias = "最小规则药品 用量范围")
    private BigDecimal minimumDosageRange;

    @ItemProperty(alias = "最大规则药品 用量范围")
    private BigDecimal MaximumDosageRange;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date LastModify;

}
