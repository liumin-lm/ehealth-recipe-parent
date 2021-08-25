package recipe.vo.doctor;

import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author fuzi
 */
@Getter
@Setter
public class RecipeInfoVO {
    /**
     * 处方信息
     */
    private RecipeBean recipeBean;
    /**
     * 处方扩展字段
     */
    private RecipeExtendBean recipeExtendBean;
    /**
     * 处方药品明细
     */
    private List<RecipeDetailBean> recipeDetails;
    /**
     * 诊疗处方
     */
    private RecipeTherapyVO recipeTherapyVO;
    /**
     * 患者信息
     */
    private PatientVO patientVO;
    /**
     * 复诊id
     */
    private Integer clinicId;
}
