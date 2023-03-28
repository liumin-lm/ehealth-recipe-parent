package recipe.vo.doctor;

import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.second.OrganVO;

import java.util.List;
import java.util.Objects;

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
     * 医院信息
     */
    private OrganVO organVO;
    /**
     * 复诊时间
     */
    private String revisitTime;
    /**
     * 快捷购药模板ID
     */
    private Integer mouldId;
    /**
     * 审方状态
     */
    private Integer auditState;
    /**
     * 签名状态
     */
    private Integer checkerSignState;

    /**
     * 新的审核状态
     */
    private Integer recipeAuditShowState;

    /**
     * 药方购买数量
     */
    private Integer buyNum;

    /**
     * 老常用方id
     */
    private Integer commonRecipeId;
    private String recipeCode;
    /**
     * 快捷购药分享医生
     */
    private Integer fastRecipeShareDoctor;
    /**
     * 快捷购药分享科室代码
     */
    private String fastRecipeShareDepart;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeInfoVO that = (RecipeInfoVO) o;
        return Objects.equals(recipeCode, that.recipeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeCode);
    }
}
