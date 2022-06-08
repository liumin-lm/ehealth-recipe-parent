package recipe.business;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.core.api.IDrugSaleStrategyBusinessService;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugSaleStrategyDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.manager.DrugSaleStrategyManager;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description： 药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@Service
public class DrugSaleStrategyBusinessService extends BaseService implements IDrugSaleStrategyBusinessService {

    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private DrugSaleStrategyManager drugSaleStrategyManager;
    @Autowired
    private DrugListDAO drugListDAO;

    @Override
    @LogRecord
    public void operationDrugSaleStrategy(DrugSaleStrategyVO param) {
        DrugSaleStrategy drugSaleStrategy=new DrugSaleStrategy();
        ObjectCopyUtils.copyProperties(drugSaleStrategy,param);
        if("add".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.save(drugSaleStrategy);
        }
        if("update".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
        }
        if("delete".equals(param.getType())){
            drugSaleStrategy.setStatus(0);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
            //关联删除药企药品目录销售策略
            List<SaleDrugList> saleDrugListList=saleDrugListDAO.findByDrugId(param.getDrugId());
            saleDrugListList.forEach(saleDrugList -> {
                saleDrugList.setSaleStrategyId(null);
                saleDrugListDAO.update(saleDrugList);
            });
        }
    }

    @Override
    @LogRecord
    public List<DrugSaleStrategyVO> findDrugSaleStrategy(Integer depId, Integer drugId) {
        List<DrugSaleStrategyVO> drugSaleStrategyVOList = new ArrayList<>();
        //获取该配送药品选中的销售策略
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, depId);
        DrugSaleStrategy drugSaleStrategy = null;
        if (null != saleDrugList && null != saleDrugList.getSaleStrategyId()) {
            drugSaleStrategy = drugSaleStrategyManager.getDrugSaleStrategyById(saleDrugList.getSaleStrategyId());
        }
        //获取药品所有的销售策略
        List<DrugSaleStrategy> allDrugSaleStrategyList = drugSaleStrategyManager.findDrugSaleStrategy(drugId);
        //获取该药品默认的销售策略
        DrugSaleStrategy defaultDrugSaleStrategy = drugSaleStrategyManager.getDefaultDrugSaleStrategy(depId, drugId);
        DrugSaleStrategyVO defaultDrugSaleStrategyVO = null;
        if (null != defaultDrugSaleStrategy) {
            defaultDrugSaleStrategyVO = ObjectCopyUtils.convert(defaultDrugSaleStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVOList.add(defaultDrugSaleStrategyVO);
        }
        if (null != drugSaleStrategy) {
            DrugSaleStrategyVO drugSaleStrategyVO = ObjectCopyUtils.convert(drugSaleStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVO.setButtonOpenFlag(true);
            drugSaleStrategyVOList.add(drugSaleStrategyVO);
        } else {
            if (null != defaultDrugSaleStrategyVO) {
                defaultDrugSaleStrategyVO.setButtonOpenFlag(true);
            }
        }
        for (DrugSaleStrategy drugStrategy : allDrugSaleStrategyList) {
            if (null != drugSaleStrategy && drugStrategy.getId().equals(drugSaleStrategy.getId())) {
                continue;
            }
            DrugSaleStrategyVO drugSaleStrategyVO = ObjectCopyUtils.convert(drugStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVOList.add(drugSaleStrategyVO);
        }
        return drugSaleStrategyVOList;
    }

    @Override
    public List<DrugSaleStrategy> findDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        List<DrugSaleStrategy> drugSaleStrategyList = drugSaleStrategyDAO.findByDrugId(drugSaleStrategy.getDrugId());
        DrugList drugList = drugListDAO.getById(drugSaleStrategy.getDrugId());
        if (null != drugList) {
            DrugSaleStrategy saleStrategy = new DrugSaleStrategy();
            saleStrategy.setDrugId(drugSaleStrategy.getDrugId());
            saleStrategy.setDrugAmount(1);
            saleStrategy.setDrugUnit(drugList.getUnit());
            saleStrategy.setStrategyTitle("默认出售策略");
            saleStrategy.setStatus(1);
            saleStrategy.setId(0);
            drugSaleStrategyList.add(saleStrategy);
            Collections.sort(drugSaleStrategyList);
        }
        return drugSaleStrategyList;
    }

    @Override
    public void saveDrugSaleStrategy(Integer depId, Integer drugId, Integer strategyId) {
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, depId);
        if (new Integer(0).equals(strategyId)) {
            saleDrugList.setSaleStrategyId(null);
        } else {
            saleDrugList.setSaleStrategyId(strategyId);
        }
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList);
    }
}
