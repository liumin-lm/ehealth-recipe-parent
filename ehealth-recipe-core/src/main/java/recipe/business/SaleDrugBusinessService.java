package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.entity.SaleDrugSalesStrategy;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.DrugsEnterpriseDAO;
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
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    @Override
    public SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        SaleDrugList res = new SaleDrugList();
        SaleDrugList saleDrugListDb = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        if (null == saleDrugListDb) {
            return res;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugListDb.getOrganId());
        if (null == drugsEnterprise) {
            return res;
        }
        List<OrganDrugList> organDrugListList = organDrugListDAO.findByDrugIdAndOrganId(saleDrugList.getDrugId(), drugsEnterprise.getOrganId());
        if (CollectionUtils.isEmpty(organDrugListList)) {
            return res;
        }
        OrganDrugList organDrugList = organDrugListList.get(0);

        SaleDrugSalesStrategy saleDrugSalesStrategy = new SaleDrugSalesStrategy();
        //默认销售策略没id
        saleDrugSalesStrategy.setOrganDrugListSalesStrategyId(null);
        if (StringUtils.isEmpty(saleDrugListDb.getEnterpriseSalesStrategy())) {
            saleDrugSalesStrategy.setButtonIsOpen("true");
        } else {
            saleDrugSalesStrategy.setButtonIsOpen("false");
        }
        saleDrugSalesStrategy.setIsDefault("true");
        saleDrugSalesStrategy.setButtonIsOpen(organDrugList.getUnit());

        String enterpriseSalesStrategy = saleDrugListDb.getEnterpriseSalesStrategy();
        List<SaleDrugSalesStrategy> saleDrugSalesStrategies = JSONObject.parseArray(enterpriseSalesStrategy, SaleDrugSalesStrategy.class);
        saleDrugSalesStrategies.add(saleDrugSalesStrategy);
        saleDrugListDb.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategies));
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugListDb);
        //根据药企药品找到对应的机构药品的默认销售策略（）
        //取机构药品目录的默认销售策略
        //organDrugListDAO
        return saleDrugListDb;
    }
}
