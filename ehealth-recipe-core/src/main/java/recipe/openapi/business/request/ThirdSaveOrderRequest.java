package recipe.openapi.business.request;

import lombok.Data;

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

    private Integer giveMode;

    private ThirdRecipeOrderRequest recipeOrder;
}
