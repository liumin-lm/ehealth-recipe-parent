package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author fuzi
 */
@Getter
@Setter
public class TherapyRecipePageVO {
    /**
     * 处方信息
     */
    private Map<Integer, List<RecipeInfoVO>> recipeInfoList;

    /**
     * 分页总条数
     */
    private Integer total;
    /**
     * 页数
     */
    private Integer start;
    /**
     * 分页条数
     */
    private Integer limit;
}
