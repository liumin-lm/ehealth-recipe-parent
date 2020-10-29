package recipe.status.factory.givemodefactory;

import com.ngari.recipe.vo.UpdateOrderStatusVO;

/**
 * @author fuzi
 */
public interface IGiveModeService {
    /**
     * 获取实现类 类型
     *
     * @return
     */
    Integer getGiveMode();

    /**
     * 根据购药方式 更新处方订单状态
     *
     * @param orderStatus
     */
    void updateStatus(UpdateOrderStatusVO orderStatus);
}
