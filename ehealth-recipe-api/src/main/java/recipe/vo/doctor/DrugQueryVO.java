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
    private Integer recipeType;
    /**
     * 煎法Id
     */
    private String decoctionId;
    private List<RecipeDetailBean> recipeDetails;
    private Integer enterpriseId;
    /**
     *  0默认，1查询医院，2查询药企
     */
    private Integer appointEnterpriseType;

    private Integer mouldId;

    private Integer buyNum;

}
