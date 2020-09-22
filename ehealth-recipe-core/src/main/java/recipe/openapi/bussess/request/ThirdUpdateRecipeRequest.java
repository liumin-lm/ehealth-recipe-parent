package recipe.openapi.bussess.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\22 0022 11:23
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ThirdUpdateRecipeRequest extends ThirdBaseRequest implements Serializable {
    private static final long serialVersionUID = -6311561970728381949L;

    private Integer recipeId;

    private String recipeCode;

    private Integer organId;

    private Integer status;
}
