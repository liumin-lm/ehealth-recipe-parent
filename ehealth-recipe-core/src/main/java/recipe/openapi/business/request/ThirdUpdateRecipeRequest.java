package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\22 0022 11:23
 */
@Data
public class ThirdUpdateRecipeRequest implements Serializable {
    private static final long serialVersionUID = -6311561970728381949L;

    private String appkey;

    private String tid;

    private Integer recipeId;

    private String recipeCode;

    private Integer organId;

    private Integer status;
}
