package recipe.factory.status.givemodefactory;

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

    /**
     * 更新后置方法
     *
     * @param orderStatus
     */
    void updateStatusAfter(UpdateOrderStatusVO orderStatus);
}
