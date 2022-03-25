package recipe.vo.second.enterpriseOrder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class EnterpriseDownDataVO implements Serializable {
    private static final long serialVersionUID = -7476041664936713722L;
    /**
     * 返回状态码 200 成功 -1 失败
     */
    private Integer code;
    /**
     * 信息
     */
    private String msg;

    private List<DownRecipeOrderVO> recipeOrderList;
}
