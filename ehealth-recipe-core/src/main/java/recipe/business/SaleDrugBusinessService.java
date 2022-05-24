package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.entity.SaleDrugSalesStrategy;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.manager.SaleDrugListManager;

import java.util.ArrayList;
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

    @Autowired
    private SaleDrugListManager saleDrugListManager;

    @Override
    public SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        SaleDrugList res = new SaleDrugList();
        SaleDrugList saleDrugListDb = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        if (null == saleDrugListDb) {
            return res;
        }
        saleDrugListDb.setEnterpriseSalesStrategy(saleDrugListManager.getEnterpriseSalesStrategy(saleDrugList));
        return saleDrugListDb;
    }

    @Override
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList) {
        logger.info("saveSaleDrugSalesStrategy saleDrugList={}", JSONUtils.toString(saleDrugList));
        //获取之前的药企药品目录
        SaleDrugList saleDrugList1 = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList = new ArrayList<>();
        //把前端传的默认的药企药品销售策略去除
        if (StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())) {
            saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(), SaleDrugSalesStrategy.class);
            saleDrugSalesStrategyList.removeIf(saleDrugSalesStrategy -> saleDrugSalesStrategy.getIsDefault().equals("true"));
        }
        saleDrugList1.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategyList));
        logger.info("saveSaleDrugSalesStrategy saleDrugList1={}", JSONUtils.toString(saleDrugList1));
        //最后进行更新
        saleDrugListDAO.update(saleDrugList1);
    }
}
