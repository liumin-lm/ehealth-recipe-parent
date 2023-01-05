package recipe.manager;

import com.ngari.recipe.entity.FastRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.dao.FastRecipeDAO;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2023-01-04
 */
@Service
public class FastRecipeManager extends BaseManager {

    private static final Logger logger = LoggerFactory.getLogger(FastRecipeManager.class);

    @Resource
    FastRecipeDAO fastRecipeDAO;

    public void deductStock(Integer mouldId, int buyNum) {
        logger.info("deductStock param: mouldId={}, buyNum={}", mouldId, buyNum);
        FastRecipe fastRecipe = fastRecipeDAO.get(mouldId);
        if (Objects.isNull(fastRecipe)) {
            return;
        }
        if (Objects.nonNull(fastRecipe.getStockNum())) {
            fastRecipeDAO.updateInventoryByMouldId(mouldId, buyNum);
        } else {
            logger.info("deductStock error! 现库存：{}", fastRecipe.getStockNum());
        }
    }
}
