package recipe.atop.greenroom;

import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugSaleStrategyBusinessService;

import java.util.List;

@RpcBean(value = "drugSaleStrategyGmAtop")
public class DrugSaleStrategyGmAtop extends BaseAtop {

    @Autowired
    private IDrugSaleStrategyBusinessService drugSaleStrategyBusinessService;

    /**
     * 操作药品销售策略
     * @param drugSaleStrategy 销售策略
     */
    @RpcService
    public void addDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        //validateAtop(drugSaleStrategy.getStrategyTitle(),drugSaleStrategy.getDrugUnit());
        drugSaleStrategyBusinessService.operationDrugSaleStrategy(drugSaleStrategy);
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
