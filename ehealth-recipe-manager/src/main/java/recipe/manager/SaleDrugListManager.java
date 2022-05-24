package recipe.manager;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.entity.SaleDrugSalesStrategy;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 药企药品处理
 *
 * @author 刘敏
 */
@Service
public class SaleDrugListManager extends BaseManager {
    private static final Logger logger = LoggerFactory.getLogger(SaleDrugListManager.class);


    /**
     * @param saleDrugListDb
     * @return
     */
    public String getEnterpriseSalesStrategy(SaleDrugList saleDrugListDb) {
        if (null == saleDrugListDb) {
            return null;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugListDb.getOrganId());
        if (null == drugsEnterprise) {
            return null;
        }
        List<OrganDrugList> organDrugListList = organDrugListDAO.findByDrugIdAndOrganId(saleDrugListDb.getDrugId(), drugsEnterprise.getOrganId());
        if (CollectionUtils.isEmpty(organDrugListList)) {
            return null;
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
        return JSONUtils.toString(saleDrugSalesStrategies);
    }


}
