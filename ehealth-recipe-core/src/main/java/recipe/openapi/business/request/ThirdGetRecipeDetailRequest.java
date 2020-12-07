package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\18 0018 16:00
 */
@Data
public class ThirdGetRecipeDetailRequest implements Serializable{
    private static final long serialVersionUID = -6496187637005413012L;

    private String appkey;

    private String tid;

    private String tabStatus;

    private Integer index;

    private Integer limit;
}
