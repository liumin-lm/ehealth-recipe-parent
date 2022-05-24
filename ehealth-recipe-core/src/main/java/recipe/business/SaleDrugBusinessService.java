package recipe.business;

import com.ngari.recipe.entity.SaleDrugList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.manager.SaleDrugListManager;

/**
 * @description： 药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@Service
public class SaleDrugBusinessService extends BaseService implements ISaleDrugBusinessService {

    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    @Autowired
    private SaleDrugListManager saleDrugListManager;

    @Override
    public SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        SaleDrugList res = new SaleDrugList();
        SaleDrugList saleDrugListDb = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        saleDrugListDb.setEnterpriseSalesStrategy(saleDrugListManager.getEnterpriseSalesStrategy(saleDrugList));
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugListDb);
        //根据药企药品找到对应的机构药品的默认销售策略（）
        //取机构药品目录的默认销售策略
        //organDrugListDAO
        return saleDrugListDb;
    }
}
