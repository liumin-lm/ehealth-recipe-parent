package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @description： 患者端列表药品信息出参
 * @author： whf
 * @date： 2023-03-07 9:54
 */
@Data
public class RecipeDetailForRecipeListResVo implements Serializable {
    private static final long serialVersionUID = -5496342157450760315L;

    @ItemProperty(alias = "药品商品名")
    private String saleName;

    @ItemProperty(alias="处方明细序号")
    private Integer recipeDetailId;

    @ItemProperty(alias="处方序号")
    private Integer recipeId;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias="机构药品编号")
    private String drugItemCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias = "腹透液  空0否  1是  ")
    private Integer peritonealDialysisFluidType;
}
