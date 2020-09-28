package recipe.openapi.bussess.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 16:44
 */
@Data
public class ThirdSaveOrderRequest implements Serializable{
    private static final long serialVersionUID = -2709460126807192501L;

    private String appkey;

    private String tid;

    private Integer recipeId;

    private String recipeCode;

    private ThirdRecipeOrderRequest recipeOrder;
}
