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
import recipe.aop.LogRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    @LogRecord
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
        Random random = new Random();
        String id = String.valueOf(System.currentTimeMillis() + random.nextInt(5));
        saleDrugSalesStrategy.setOrganDrugListSalesStrategyId(id);
        if (StringUtils.isEmpty(saleDrugListDb.getEnterpriseSalesStrategy())) {
            saleDrugSalesStrategy.setButtonIsOpen("true");
        } else {
            saleDrugSalesStrategy.setButtonIsOpen("false");
        }
        saleDrugSalesStrategy.setIsDefault("true");
        saleDrugSalesStrategy.setUnit(organDrugList.getUnit());

        String enterpriseSalesStrategy = saleDrugListDb.getEnterpriseSalesStrategy();
        List<SaleDrugSalesStrategy> saleDrugSalesStrategies = JSONObject.parseArray(enterpriseSalesStrategy, SaleDrugSalesStrategy.class);
        if (CollectionUtils.isEmpty(saleDrugSalesStrategies)) {
            saleDrugSalesStrategies = new ArrayList<>();
        }
        saleDrugSalesStrategies.add(saleDrugSalesStrategy);
        return JSONUtils.toString(saleDrugSalesStrategies);
    }


    /**
     * @param saleDrugList
     * @return
     */
    public String getNeedSaveEnterpriseSalesStrategy(SaleDrugList saleDrugList) {
        if (null == saleDrugList) {
            return null;
        }
        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList = new ArrayList<>();
        //把前端传的默认的药企药品销售策略去除
        if (StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())) {
            saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(), SaleDrugSalesStrategy.class);
            saleDrugSalesStrategyList.removeIf(saleDrugSalesStrategy -> "true".equals(saleDrugSalesStrategy.getIsDefault()));
        }
        return JSONUtils.toString(saleDrugSalesStrategyList);
    }
}
