package recipe.atop.greenroom;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugSaleStrategyBusinessService;

import java.util.List;

@RpcBean(value = "drugSaleStrategyGmAtop")
public class DrugSaleStrategyGmAtop extends BaseAtop {

    @Autowired
    private IDrugSaleStrategyBusinessService drugSaleStrategyBusinessService;

    /**
     * 操作药品销售策略
     *
     * @param drugSaleStrategy 销售策略
     */
    @RpcService
    @LogRecord
    public void operationDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        drugSaleStrategyBusinessService.operationDrugSaleStrategy(drugSaleStrategy);
    }

    /**
     * 查询销售策略
     *
     * @param drugSaleStrategy
     */
    @RpcService
    @LogRecord
    public List<DrugSaleStrategy> findDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        return drugSaleStrategyBusinessService.findDrugSaleStrategy(drugSaleStrategy);
    }

    /**
     * 查询销售策略
     * @param depId
     * @param drugId
     * @return
     */
    @RpcService
    public List<DrugSaleStrategyVO> findDrugSaleStrategy(Integer depId, Integer drugId) {
        return drugSaleStrategyBusinessService.findDrugSaleStrategy(depId, drugId);
    }

}
