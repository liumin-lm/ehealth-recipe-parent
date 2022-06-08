package recipe.manager;

import com.ngari.recipe.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.*;

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
    private DrugListDAO drugListDAO;

    public DrugSaleStrategy getDrugSaleStrategyById(Integer id) {
        return drugSaleStrategyDAO.getDrugSaleStrategyById(id);
    }

    public List<DrugSaleStrategy> findDrugSaleStrategy(Integer drugId){
        return drugSaleStrategyDAO.findByDrugId(drugId);
    }

    public DrugSaleStrategy getDefaultDrugSaleStrategy(Integer depId, Integer drugId){
        if (null == drugId) {
            return  null;
        }
        DrugList drugList = drugListDAO.getById(drugId);
        if (null == drugList) {
            return  null;
        }
        DrugSaleStrategy drugSaleStrategy = new DrugSaleStrategy();
        drugSaleStrategy.setDrugId(drugId);
        drugSaleStrategy.setDrugAmount(1);
        drugSaleStrategy.setDrugUnit(drugList.getUnit());
        drugSaleStrategy.setStrategyTitle("默认出售策略");
        drugSaleStrategy.setStatus(1);
        drugSaleStrategy.setId(0);
        return drugSaleStrategy;
    }
}
