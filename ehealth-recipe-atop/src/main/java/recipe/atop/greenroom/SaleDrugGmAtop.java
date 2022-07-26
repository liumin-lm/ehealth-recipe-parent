package recipe.atop.greenroom;

import com.ngari.recipe.entity.SaleDrugList;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;

/**
 * @description： 运营平台药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "saleDrugGmAtop")
public class SaleDrugGmAtop extends BaseAtop {

    @Autowired
    private IDrugBusinessService iDrugBusinessService;

    /**
     * 根据OrganId、DrugId获取药企药品
     *
     * @param saleDrugList
     */
    @RpcService
    public SaleDrugList getSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        validateAtop(saleDrugList.getOrganId(),saleDrugList.getDrugId());
        SaleDrugList res = iDrugBusinessService.findSaleDrugListByDrugIdAndOrganId(saleDrugList);
        return res;
    }
    /**
     * 保存药企销售策略
     *
     * @param saleDrugList
     */
    @RpcService
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList){
        validateAtop(saleDrugList.getOrganId(), saleDrugList.getDrugId());
        iDrugBusinessService.saveSaleDrugSalesStrategy(saleDrugList);
    }



}
