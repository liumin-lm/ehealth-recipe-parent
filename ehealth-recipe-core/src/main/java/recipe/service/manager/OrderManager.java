package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.RecipeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeOrderDAO;

/**
 * 订单
 * @author yinsheng
 * @date 2021\6\30 0030 15:22
 */
@Service
public class OrderManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    /**
     * 根据订单编号获取订单信息
     * @param orderCode  订单编号
     * @return           订单信息
     */
    public RecipeOrder getOrderByOrderCode(String orderCode){
        logger.info("OrderManager getRecipeOrderByOrderCode orderCode:{}.", orderCode);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        logger.info("OrderManager getRecipeOrderByOrderCode Order:{}.", JSON.toJSON(order));
        return order;
    }
}
