package recipe.atop.greenroom;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugSaleStrategyBusinessService;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.core.api.ISaleDrugBusinessService;

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

}
