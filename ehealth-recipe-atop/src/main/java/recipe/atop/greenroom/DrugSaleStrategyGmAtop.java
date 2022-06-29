package recipe.atop.greenroom;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;

import java.util.List;

@RpcBean(value = "drugSaleStrategyGmAtop")
public class DrugSaleStrategyGmAtop extends BaseAtop {

    @Autowired
    private IDrugBusinessService drugBusinessService;

    /**
     * 操作药品销售策略
     *
     * @param drugSaleStrategy 销售策略
     */
    @RpcService
    @LogRecord
    public void operationDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        drugBusinessService.operationDrugSaleStrategy(drugSaleStrategy);
    }

    /**
     * 查询销售策略
     *
     * @param drugSaleStrategy
     */
    @RpcService
    @LogRecord
    public List<DrugSaleStrategy> findDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        return drugBusinessService.findDrugSaleStrategy(drugSaleStrategy);
    }

    /**
     * 查询销售策略
     * @param depId
     * @param drugId
     * @return
     */
    @RpcService
    public List<DrugSaleStrategyVO> findDrugSaleStrategyByDepIdAndDrugId(Integer depId, Integer drugId) {
        return drugBusinessService.findDrugSaleStrategy(depId, drugId);
    }

    /**
     * 保存销售策略
     * @param depId
     * @param drugId
     * @param strategyId
     */
    @RpcService
    public void saveDrugSaleStrategy(Integer depId, Integer drugId, Integer strategyId){
        drugBusinessService.saveDrugSaleStrategy(depId, drugId, strategyId);
    }

}
