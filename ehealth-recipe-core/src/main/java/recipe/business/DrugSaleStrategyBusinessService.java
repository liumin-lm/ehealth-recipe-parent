package recipe.business;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.core.api.IDrugSaleStrategyBusinessService;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.DrugSaleStrategyDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.util.ObjectCopyUtils;

import java.util.List;

/*import recipe.manager.SaleDrugListManager;*/

/**
 * @description： 药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@Service
public class DrugSaleStrategyBusinessService extends BaseService implements IDrugSaleStrategyBusinessService {

    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;

    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

    @Override
    @LogRecord
    public void operationDrugSaleStrategy(DrugSaleStrategyVO param) {
        DrugSaleStrategy drugSaleStrategy=new DrugSaleStrategy();
        ObjectCopyUtils.copyProperties(drugSaleStrategy,param);
        if("add".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.save(drugSaleStrategy);
        }
        if("update".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
        }
        if("delete".equals(param.getType())){
            drugSaleStrategy.setStatus(0);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
            //关联删除药企药品目录销售策略
            List<SaleDrugList> saleDrugListList=saleDrugListDAO.findByDrugId(param.getDrugId());
            saleDrugListList.forEach(saleDrugList -> {
                saleDrugList.setSaleStrategyId(null);
                saleDrugListDAO.save(saleDrugList);
            });
        }
    }
}
