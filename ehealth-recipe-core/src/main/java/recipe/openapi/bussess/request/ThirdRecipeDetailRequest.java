package recipe.openapi.bussess.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 09:18
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ThirdRecipeDetailRequest extends ThirdBaseRequest implements Serializable{
    private static final long serialVersionUID = 6733955549865903496L;

    private Integer recipeId;
}
