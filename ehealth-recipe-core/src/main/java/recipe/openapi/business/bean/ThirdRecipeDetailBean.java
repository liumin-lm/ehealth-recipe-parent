package recipe.openapi.business.bean;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\18 0018 15:49
 */
@Data
@Schema
public class ThirdRecipeDetailBean implements Serializable{
    private static final long serialVersionUID = 6048242158398003003L;

    private Integer recipeDetailId;

    private Integer recipeId;

    private String drugSpec;

    private Integer pack;

    private String drugUnit;

    private String drugName;

    private Double useDose;

    private Double defaultUseDose;

    private String useDoseUnit;

    private String dosageUnit;
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    private Double useTotalDose;

    private Double sendNumber;

    private Integer useDays;

    private Double drugCost;

    private String memo;

    private String organDrugCode;

    @ItemProperty(alias = "机构的频次代码")
    private String organUsingRate;
    @ItemProperty(alias = "机构的用法代码")
    private String organUsePathways;
}
