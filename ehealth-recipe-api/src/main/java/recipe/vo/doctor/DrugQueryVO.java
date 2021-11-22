package recipe.vo.doctor;

import com.ngari.recipe.recipe.model.RecipeDetailBean;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2020/12/21
 *
 * @author shiyuping
 */
@Data
public class DrugQueryVO implements Serializable {
    private Integer organId;
    private List<Integer> drugIds;
    private Integer pharmacyId;
    private List<RecipeDetailBean> recipeDetails;
}
