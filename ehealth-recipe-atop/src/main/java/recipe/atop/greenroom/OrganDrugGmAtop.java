package recipe.atop.greenroom;

import com.ngari.recipe.entity.OrganDrugList;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IOrganDrugBusinessService;

/**
 * @description： 运营平台机构药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "organDrugGmAtop")
public class OrganDrugGmAtop extends BaseAtop {

    @Autowired
    private IOrganDrugBusinessService organDrugBusinessService;


    /**
     * 添加机构药品列表销售策略
     * @param salesStrategy 销售策略
     */
    @RpcService
    public void addOrganDrugSalesStrategy(OrganDrugList organDrugList) {
        validateAtop(organDrugList.getOrganDrugId());
        organDrugBusinessService.addOrganDrugSalesStrategy(organDrugList);
    }


}
