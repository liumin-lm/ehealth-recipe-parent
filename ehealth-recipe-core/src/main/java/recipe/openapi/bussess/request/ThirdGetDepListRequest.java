package recipe.openapi.bussess.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 10:42
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ThirdGetDepListRequest extends ThirdBaseRequest implements Serializable{
    private static final long serialVersionUID = -1926238824070605318L;

    private Integer recipeId;

    private Integer payMode;

    private Map<String, String> filterConditions;

}
