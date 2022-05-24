package recipe.business;

import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.SaleDrugList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;

import java.util.List;

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
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    @Override
    public SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        SaleDrugList saleDrugList1 = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        List<OrganAndDrugsepRelation> organAndDrugsepRelations = organAndDrugsepRelationDAO.findByEntId(saleDrugList1.getOrganId());
        //根据药企药品找到对应的机构药品的默认销售策略（）
        //取机构药品目录的默认销售策略
        //organDrugListDAO
        return saleDrugList1;
    }
}
