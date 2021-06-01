package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 09:18
 */
@Data
public class ThirdRecipeDetailRequest implements Serializable{
    private static final long serialVersionUID = 6733955549865903496L;

    private String appkey;

    private String tid;

    private Integer recipeId;
}
