package recipe.core.api;


import com.ngari.recipe.dto.SkipThirdBean;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;

public interface IRecipeOrderService {
    /**
     * 更新核发药师信息
     *
     * @param recipeId
     * @param giveUser
     * @return
     */
    ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser);

    /**
     * 订单状态更新
     *
     * @param updateOrderStatusVO 状态对象
     * @return
     */
    ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO);

    void uploadRecipeInfoToThird(SkipThirdReqVO skipThirdReqVO);

    /**
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @return
     */
    SkipThirdBean getThirdUrl(Integer integer);

    SkipThirdBean getSkipUrl(SkipThirdReqVO skipThirdReqVO);
}
