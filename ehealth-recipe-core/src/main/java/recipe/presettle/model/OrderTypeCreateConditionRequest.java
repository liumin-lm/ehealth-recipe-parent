package recipe.presettle.model;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import lombok.Builder;
import lombok.Data;

/**
 * created by shiyuping on 2020/11/27
 * @author shiyuping
 * 获取处方订单类型条件请求对象
 */
@Data
@Builder
public class OrderTypeCreateConditionRequest implements java.io.Serializable {
    private static final long serialVersionUID = -8217630229301431466L;
    /**
     * 处方信息
     */
    private Recipe recipe;
    /**
     * 处方扩展信息
     */
    private RecipeExtend recipeExtend;
    /**
     * 药企信息
     */
    private DrugsEnterprise drugsEnterprise;

    /**
     * 处方订单类型
     */
    private RecipeOrder recipeOrder;
}
