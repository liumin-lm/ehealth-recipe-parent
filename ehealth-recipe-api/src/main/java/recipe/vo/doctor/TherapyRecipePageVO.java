package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.util.List;

/**
 * @author fuzi
 */
@Getter
@Setter
public class TherapyRecipePageVO extends PageVO {
    /**
     * 处方信息
     */
    private List<RecipeInfoVO> recipeInfoList;

}
