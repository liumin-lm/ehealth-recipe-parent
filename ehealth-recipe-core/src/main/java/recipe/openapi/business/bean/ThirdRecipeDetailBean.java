package recipe.openapi.business.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\18 0018 15:49
 */
@Data
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

    private String usingRate;

    private String usePathways;

    private Double useTotalDose;

    private Double sendNumber;

    private Integer useDays;

    private Double drugCost;

    private String memo;

    private String organDrugCode;
}
