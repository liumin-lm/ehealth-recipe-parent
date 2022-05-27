package recipe.business;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IDrugSaleStrategyBusinessService;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.DrugSaleStrategyDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.util.ObjectCopyUtils;

/*import recipe.manager.SaleDrugListManager;*/

/**
 * @description： 药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@Service
public class DrugSaleStrategyBusinessService extends BaseService implements IDrugSaleStrategyBusinessService {

    @Autowired
    private DrugSaleStrategyDAO saleDrugListDAO;

    @Override
    public void operationDrugSaleStrategy(DrugSaleStrategyVO param) {
        DrugSaleStrategy drugSaleStrategy=new DrugSaleStrategy();
        ObjectCopyUtils.copyProperties(drugSaleStrategy,param);
        if("add".equals(param.getType())){
            saleDrugListDAO.save(drugSaleStrategy);
        }
        if("update".equals(param.getType())){
            saleDrugListDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
        }
        if("delete".equals(param.getType())){
            saleDrugListDAO.remove(drugSaleStrategy);
        }
        return;
    }
}
