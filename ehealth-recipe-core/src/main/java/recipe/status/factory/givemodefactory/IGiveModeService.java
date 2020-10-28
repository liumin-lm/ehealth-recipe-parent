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
     * 更新状态
     *
     * @param orderStatus
     */
    void updateStatus(UpdateOrderStatusVO orderStatus);
}
