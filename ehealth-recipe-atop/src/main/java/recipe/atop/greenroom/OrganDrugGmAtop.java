package recipe.atop.greenroom;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.atop.BaseAtop;

/**
 * @description： 运营平台机构药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "saleDrugGmAtop")
public class OrganDrugGmAtop extends BaseAtop {

//    @Autowired
//    private ISaleDrugBusinessService enterpriseBusinessService;


    /**
     * 根据OrganId、DrugId获取药企药品
     *
     * @param saleDrugList
     */
    @RpcService
    public void addOrganDrugSalesStrategy(String salesStrategy) {
//        validateAtop(saleDrugList.getOrganId(),saleDrugList.getDrugId());
//        SaleDrugList res = enterpriseBusinessService.findSaleDrugListByDrugIdAndOrganId(saleDrugList);
    }


}
