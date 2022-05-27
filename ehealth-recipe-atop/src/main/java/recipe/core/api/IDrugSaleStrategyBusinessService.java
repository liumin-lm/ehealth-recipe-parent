package recipe.core.api;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.vo.DrugSaleStrategyVO;

import java.util.List;

/**
 * 销售策略业务处理类
 */
public interface IDrugSaleStrategyBusinessService {


    void operationDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy);

    List<DrugSaleStrategyVO> findDrugSaleStrategy(Integer depId, Integer drugId);

    List<DrugSaleStrategy> findDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy);

    void saveDrugSaleStrategy(Integer depId, Integer drugId, Integer strategyId);
}
