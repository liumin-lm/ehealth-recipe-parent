package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.OrganDrugSalesStrategy;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.entity.SaleDrugSalesStrategy;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IOrganDrugBusinessService;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;

import java.util.*;

/**
 * @author zgy
 * @date 2022/5/24 13:53
 */
@Service
public class OrganDrugBusinessService extends BaseService implements IOrganDrugBusinessService {

    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

    @Override
    public void addOrganDrugSalesStrategy(OrganDrugList organDrugList) {
        logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy organDrugList={}",JSONUtils.toString(organDrugList));
        Random random = new Random();
        String id = String.valueOf(System.currentTimeMillis() + random.nextInt(10));
        //获取前端传的销售策略
        String salesStrategy = organDrugList.getSalesStrategy();
        //根据机构药品ID查询之前的销售策略
        OrganDrugList drugList = organDrugListDAO.get(organDrugList.getOrganDrugId());
        logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy drugList={}",JSONUtils.toString(drugList));
        List<OrganDrugSalesStrategy> organDrugSalesStrategyList = new ArrayList<>();
        List<OrganDrugSalesStrategy> organDrugSalesStrategy = new ArrayList<>();
        if(StringUtils.isNotEmpty(drugList.getSalesStrategy())){
            organDrugSalesStrategyList = JSONObject.parseArray(drugList.getSalesStrategy(),OrganDrugSalesStrategy.class);
        }
        if(null != salesStrategy){
            organDrugSalesStrategy = JSONObject.parseArray(salesStrategy,OrganDrugSalesStrategy.class);
            //目前只有一个值
            organDrugSalesStrategy.get(0).setId(id);
            organDrugSalesStrategyList.add(organDrugSalesStrategy.get(0));
            logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy organDrugSalesStrategyList={}",JSONUtils.toString(organDrugSalesStrategyList));
            organDrugList.setSalesStrategy(JSONUtils.toString(organDrugSalesStrategyList));
        }
        organDrugListDAO.updateData(organDrugList);
        SaleDrugSalesStrategy saleDrugSalesStrategy = new SaleDrugSalesStrategy();
        saleDrugSalesStrategy.setOrganDrugListSalesStrategyId(id);
        saleDrugSalesStrategy.setUnit(organDrugSalesStrategy.get(0).getUnit());
        saleDrugSalesStrategy.setButtonIsOpen("false");
        saleDrugSalesStrategy.setIsDefault("false");
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugList.getDrugId(), drugList.getOrganId());
        logger.info("addOrganDrugSalesStrategy saleDrugList={}",JSONUtils.toString(saleDrugList));
        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList  = new ArrayList<>();
        if(StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())){
            saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(),SaleDrugSalesStrategy.class);
            saleDrugSalesStrategyList.add(saleDrugSalesStrategy);
        }
        saleDrugList.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategyList));
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList);
    }
}
