package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.entity.SaleDrugSalesStrategy;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;

import java.util.ArrayList;
import java.util.List;
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
        return saleDrugList1;
    }

    @Override
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList) {
        logger.info("saveSaleDrugSalesStrategy saleDrugList={}",JSONUtils.toString(saleDrugList));
        //获取之前的药企药品目录
        SaleDrugList saleDrugList1 = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList = new ArrayList<>();
        //把前端传的默认的药企药品销售策略去除
        if(StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())){
            saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(), SaleDrugSalesStrategy.class);
            saleDrugSalesStrategyList.removeIf(saleDrugSalesStrategy -> saleDrugSalesStrategy.getIsDefault().equals("true"));
        }
        saleDrugList1.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategyList));
        logger.info("saveSaleDrugSalesStrategy saleDrugList1={}",JSONUtils.toString(saleDrugList1));
        //最后进行更新
        saleDrugListDAO.update(saleDrugList1);
    }
}
