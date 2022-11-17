package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description：就医引导处方详情出参
 * @author： whf
 * @date： 2022-11-17 10:07
 */
@Getter
@Setter
@ToString
public class RecipeDetailToGuideResVO implements Serializable {
    private static final long serialVersionUID = -7839734481700348290L;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias="机构药品编号")
    private String drugItemCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias = "药品商品名")
    private String saleName;
}
