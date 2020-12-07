package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 10:42
 */
@Data
public class ThirdGetDepListRequest implements Serializable{
    private static final long serialVersionUID = -1926238824070605318L;

    private String appkey;

    private String tid;

    private Integer recipeId;

    private Integer payMode;

    private Map<String, String> filterConditions;

}
