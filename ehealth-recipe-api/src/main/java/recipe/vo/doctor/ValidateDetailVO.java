package recipe.vo.doctor;

import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 校验线上线下 药品数据VO
 *
 * @author fuzi
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateDetailVO {
    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 处方类型
     */
    private Integer recipeType;
    /**
     * 处方药品明细
     */
    private List<RecipeDetailBean> recipeDetails;
    /**
     * 处方扩展字段
     */
    private RecipeExtendBean recipeExtendBean;
    /**
     * 处方信息
     */
    private RecipeBean recipeBean;
    /**
     * 是否长处方 是： true  否：false
     */
    private Boolean longRecipe;

}
