package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class RecipeOrderRefundPageVO extends PageVO implements Serializable {
    private static final long serialVersionUID = -4539856704907546388L;

    private List<RecipeOrderRefundVO> recipeOrderRefundVOList;
}
