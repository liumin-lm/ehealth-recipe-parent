package recipe.manager;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.SaleDrugList;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.DrugSaleStrategyDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * 药品销售策略处理类
 *
 * @author fuzi
 */
@Service
public class DrugSaleStrategyManager extends BaseManager {

    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    public DrugSaleStrategy getDrugSaleStrategyById(Integer id) {
        return drugSaleStrategyDAO.getDrugSaleStrategyById(id);
    }

    public List<DrugSaleStrategy> findDrugSaleStrategy(Integer drugId){
        return drugSaleStrategyDAO.findByDrugId(drugId);
    }

    public DrugSaleStrategy getDefaultDrugSaleStrategy(Integer depId, Integer drugId){
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (null == drugsEnterprise || null == drugsEnterprise.getOrganId()) {
            return null;
        }
        List<OrganDrugList> organDrugListList = organDrugListDAO.findByDrugIdAndOrganId(drugId, drugsEnterprise.getOrganId());
        if (CollectionUtils.isEmpty(organDrugListList)) {
            return null;
        }
        OrganDrugList organDrugList = organDrugListList.get(0);
        DrugSaleStrategy drugSaleStrategy = new DrugSaleStrategy();
        drugSaleStrategy.setDrugId(drugId);
        drugSaleStrategy.setDrugAmount(1);
        drugSaleStrategy.setDrugUnit(organDrugList.getUnit());
        drugSaleStrategy.setStrategyTitle("默认出售策略");
        drugSaleStrategy.setStatus(1);
        drugSaleStrategy.setId(drugId);
        return drugSaleStrategy;
    }
}
