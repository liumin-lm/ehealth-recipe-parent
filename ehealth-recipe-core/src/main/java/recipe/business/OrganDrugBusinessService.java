package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IOrganDrugBusinessService;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
//import recipe.manager.SaleDrugListManager;

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
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
//    @Autowired
//    private SaleDrugListManager saleDrugListManager;

//    @Override
//    public void addOrganDrugSalesStrategy(OrganDrugList organDrugList) {
//        logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy organDrugList={}",JSONUtils.toString(organDrugList));
//        //根据机构药品ID查询之前的销售策略
//        OrganDrugList organDrugListDb = organDrugListDAO.get(organDrugList.getOrganDrugId());
//        if("add".equals(organDrugList.getType())){
//            Random random = new Random();
//            String id = String.valueOf(System.currentTimeMillis() + random.nextInt(5));
//            //获取前端传的销售策略
//            String salesStrategy = organDrugList.getSalesStrategy();
//
//            logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy drugList={}",JSONUtils.toString(organDrugListDb));
//            List<OrganDrugSalesStrategy> organDrugSalesStrategyList = new ArrayList<>();
//            List<OrganDrugSalesStrategy> organDrugSalesStrategy = new ArrayList<>();
//            if(StringUtils.isNotEmpty(organDrugListDb.getSalesStrategy())){
//                organDrugSalesStrategyList = JSONObject.parseArray(organDrugListDb.getSalesStrategy(),OrganDrugSalesStrategy.class);
//            }
//            if(null != salesStrategy){
//                organDrugSalesStrategy = JSONObject.parseArray(salesStrategy,OrganDrugSalesStrategy.class);
//                //目前只有一个值
//                organDrugSalesStrategy.get(0).setId(id);
//                organDrugSalesStrategyList.add(organDrugSalesStrategy.get(0));
//                logger.info("OrganDrugBusinessService addOrganDrugSalesStrategy organDrugSalesStrategyList={}",JSONUtils.toString(organDrugSalesStrategyList));
//                organDrugListDb.setSalesStrategy(JSONUtils.toString(organDrugSalesStrategyList));
//            }
//            organDrugListDAO.updateNonNullFieldByPrimaryKey(organDrugListDb);
//            saleDrugListManager.saveEnterpriseSalesStrategyByOrganDrugList(organDrugList,"add");
//        }
//        if("delete".equals(organDrugList.getType())){
//            if(StringUtils.isNotEmpty(organDrugListDb.getSalesStrategy())){
//                List<OrganDrugSalesStrategy> organDrugSalesStrategyListDb = JSONObject.parseArray(organDrugListDb.getSalesStrategy(),OrganDrugSalesStrategy.class);
//                List<OrganDrugSalesStrategy> organDrugSalesStrategyList = JSONObject.parseArray(organDrugList.getSalesStrategy(),OrganDrugSalesStrategy.class);
//                organDrugSalesStrategyList.forEach(organDrugSalesStrategy -> {
//                    //TODO 销售策略
//                    organDrugSalesStrategyListDb.remove(organDrugSalesStrategy);
//                });
//                organDrugListDb.setSalesStrategy(JSONUtils.toString(organDrugSalesStrategyListDb));
//                organDrugListDAO.updateData(organDrugListDb);
//                saleDrugListManager.saveEnterpriseSalesStrategyByOrganDrugList(organDrugListDb,"update");
//            }
//        }
//        if("update".equals(organDrugList.getType())){
//            if(StringUtils.isNotEmpty(organDrugListDb.getSalesStrategy())){
//                List<OrganDrugSalesStrategy> organDrugSalesStrategyListDb = JSONObject.parseArray(organDrugListDb.getSalesStrategy(),OrganDrugSalesStrategy.class);
//                List<OrganDrugSalesStrategy> organDrugSalesStrategyList = JSONObject.parseArray(organDrugList.getSalesStrategy(),OrganDrugSalesStrategy.class);
//                OrganDrugSalesStrategy organDrugSalesStrategy=organDrugSalesStrategyList.get(0);
//                organDrugSalesStrategyListDb.forEach(organDrugSalesStrategyDb -> {
//                    if(organDrugSalesStrategyDb.getId().equals(organDrugSalesStrategy.getId())){
//                        organDrugSalesStrategyDb.setId(organDrugSalesStrategy.getId());
//                        organDrugSalesStrategyDb.setAmount(organDrugSalesStrategy.getAmount());
//                        organDrugSalesStrategyDb.setIsDefault(organDrugSalesStrategy.getIsDefault());
//                        organDrugSalesStrategyDb.setTitle(organDrugSalesStrategy.getTitle());
//                        organDrugSalesStrategyDb.setUnit(organDrugSalesStrategy.getUnit());
//                    }
//                });
//                organDrugListDb.setSalesStrategy(JSONUtils.toString(organDrugSalesStrategyListDb));
//                organDrugListDAO.updateData(organDrugListDb);
//                saleDrugListManager.saveEnterpriseSalesStrategyByOrganDrugList(organDrugListDb,"update");
//            }
//        }
//
//
//    }
}
