package recipe.vo.greenroom;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @Author zgy
 * @Date 2023-02-17
 */
@Data
public class DrugDistributionListInfoVO implements Serializable {
    private static final long serialVersionUID = 5187782743856421101L;

    //药品订单详情
    private RecipeOrderRefundDetailVO recipeOrderRefundDetailVO;

    //处方详情、审核信息
    private List<Map<String, Object>> recipeDetailsAndCheckInfo;

    //物流编码文件流
    private String logisticsOrderNo;
}
