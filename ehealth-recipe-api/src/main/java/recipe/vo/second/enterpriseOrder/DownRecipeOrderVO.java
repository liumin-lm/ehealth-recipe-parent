package recipe.vo.second.enterpriseOrder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 第三方下载处方订单
 * @author： yins
 * @date： 2021-12-08 15:50
 */
@Getter
@Setter
public class DownRecipeOrderVO implements Serializable {
    private static final long serialVersionUID = -6533232440281030512L;

    /**
     * 药企需要的订单信息
     */
    private DownOrderVO order;
    /**
     * 收件人信息
     */
    private ReceiverInfoVO receiverInfo;
    /**
     * 处方信息列表
     */
    private List<DownRecipeVO> recipeList;

}
