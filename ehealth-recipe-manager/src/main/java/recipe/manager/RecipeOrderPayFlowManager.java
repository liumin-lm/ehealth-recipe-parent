package recipe.manager;

import com.ngari.recipe.entity.RecipeOrderPayFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeOrderPayFlowDao;

import java.util.Date;

/**
 * 交易流水处理
 *
 * @author yinsheng
 */
@Service
public class RecipeOrderPayFlowManager extends BaseManager{

    @Autowired
    private RecipeOrderPayFlowDao recipeOrderPayFlowDao;

    public RecipeOrderPayFlow getByOrderIdAndType(Integer orderId, Integer payFlowType){
        return recipeOrderPayFlowDao.getByOrderIdAndType(orderId, payFlowType);
    }

    public RecipeOrderPayFlow getByOutTradeNo(String outTradeNo){
        return recipeOrderPayFlowDao.getByOutTradeNo(outTradeNo);
    }

    public boolean updateNonNullFieldByPrimaryKey(RecipeOrderPayFlow recipeOrderPayFlow){
        return recipeOrderPayFlowDao.updateNonNullFieldByPrimaryKey(recipeOrderPayFlow);
    }

    public void save(RecipeOrderPayFlow recipeOrderPayFlow){
        Date date = new Date();
        recipeOrderPayFlow.setCreateTime(date);
        recipeOrderPayFlow.setModifiedTime(date);
        recipeOrderPayFlowDao.save(recipeOrderPayFlow);
    }
}
