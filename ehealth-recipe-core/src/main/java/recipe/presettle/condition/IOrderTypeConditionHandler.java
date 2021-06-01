package recipe.presettle.condition;

import recipe.presettle.model.OrderTypeCreateConditionRequest;

/**
 * created by shiyuping on 2020/11/27
 * 获取订单类型判断处理接口---根据条件判断使用哪种类型订单
 * @author shiyuping
 */
public interface IOrderTypeConditionHandler {

    /**
     * 获取各个条件下对应的订单类型
     * @return
     */
    Integer getOrderType(OrderTypeCreateConditionRequest request);

    /**
     * 获取条件判断执行优先级,值越小越排前
     * @return
     */
    int getPriorityLevel();
}
