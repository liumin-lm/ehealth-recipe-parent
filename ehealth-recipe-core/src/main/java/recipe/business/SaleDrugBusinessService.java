package recipe.business;

import com.ngari.recipe.entity.SaleDrugList;
import ctd.util.JSONUtils;
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
        if (null == saleDrugListDb) {
            return res;
        }
        saleDrugListDb.setEnterpriseSalesStrategy(saleDrugListManager.getNeedShowEnterpriseSalesStrategy(saleDrugListDb));
        return saleDrugListDb;
    }

    @Override
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList) {
        logger.info("saveSaleDrugSalesStrategy saleDrugList={}", JSONUtils.toString(saleDrugList));
        //获取之前的药企药品目录
        SaleDrugList saleDrugList1 = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        if (saleDrugList1 == null) {
            return;
        }
        saleDrugList1.setEnterpriseSalesStrategy(saleDrugListManager.getNeedSaveEnterpriseSalesStrategy(saleDrugList1));
        logger.info("saveSaleDrugSalesStrategy saleDrugList1={}", JSONUtils.toString(saleDrugList1));
        //最后进行更新
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList1);
    }
}
