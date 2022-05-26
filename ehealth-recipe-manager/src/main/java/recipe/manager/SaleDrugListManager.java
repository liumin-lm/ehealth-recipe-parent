package recipe.manager;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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
     * 根据药企药品获取药企药品需要显示的销售策略
     * @param saleDrugListDb
     * @return
     */
    @LogRecord
    public String getNeedShowEnterpriseSalesStrategy(SaleDrugList saleDrugListDb) {
        List<SaleDrugSalesStrategy> saleDrugSalesStrategies =new ArrayList<SaleDrugSalesStrategy>();
        try {
            //如果没有药企药品（新增药品药品页面），则返回默认的销售策略
            if (null == saleDrugListDb) {
                return null;
            }
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugListDb.getOrganId());
            if (null == drugsEnterprise) {
                return null;
            }
            if(drugsEnterprise.getOrganId()==null){
                logger.error("药企没有关联机构！");
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
            //如果存在销售策略，
            if (StringUtils.isEmpty(saleDrugListDb.getEnterpriseSalesStrategy())) {
                saleDrugSalesStrategy.setButtonIsOpen("true");
            } else {
                saleDrugSalesStrategy.setButtonIsOpen("false");
            }
            saleDrugSalesStrategy.setIsDefault("true");
            saleDrugSalesStrategy.setUnit(organDrugList.getUnit());

            String enterpriseSalesStrategy = saleDrugListDb.getEnterpriseSalesStrategy();
            saleDrugSalesStrategies = JSONObject.parseArray(enterpriseSalesStrategy, SaleDrugSalesStrategy.class);
            if (CollectionUtils.isEmpty(saleDrugSalesStrategies)) {
                saleDrugSalesStrategies = new ArrayList<>();
            }
            saleDrugSalesStrategies.add(saleDrugSalesStrategy);
        } catch (DAOException e) {
            logger.error("销售策略 getNeedShowEnterpriseSalesStrategy",e);
            e.printStackTrace();
        }
        return JSONUtils.toString(saleDrugSalesStrategies);
    }


    /**
     * 根据药企药品获取药企药品需要保存的销售策略
     * @param saleDrugList
     * @return
     */
    public String getNeedSaveEnterpriseSalesStrategy(SaleDrugList saleDrugList) {
        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList = new ArrayList<>();
        try {
            if (null == saleDrugList) {
                return null;
            }
            //把前端传的默认的药企药品销售策略去除
            if (StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())) {
                saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(), SaleDrugSalesStrategy.class);
                saleDrugSalesStrategyList.removeIf(saleDrugSalesStrategy -> "true".equals(saleDrugSalesStrategy.getIsDefault()));
            }
        } catch (Exception e){
            logger.error("销售策略 getNeedSaveEnterpriseSalesStrategy",e);
            e.printStackTrace();
        }
        return JSONUtils.toString(saleDrugSalesStrategyList);
    }

    /**
     * 根据机构药品销售策略保存药企药品销售策略
     * @param organDrugList
     * @param type
     */
    @LogRecord
    public void saveEnterpriseSalesStrategyByOrganDrugList(OrganDrugList organDrugList,String type) {
        try {
            if(organDrugList==null||StringUtils.isEmpty(organDrugList.getSalesStrategy())){
                return;
            }
            List<OrganDrugSalesStrategy> organDrugSalesStrategys=JSONObject.parseArray(organDrugList.getSalesStrategy(),OrganDrugSalesStrategy.class);
            if(CollectionUtils.isEmpty(organDrugSalesStrategys)){
                return;
            }
            List<SaleDrugSalesStrategy> saleDrugSalesStrategies=new ArrayList<>();
            organDrugSalesStrategys.forEach(organDrugSalesStrategy -> {
                //过滤默认销售策略
                if("true".equals(organDrugSalesStrategy.getIsDefault())){
                    return;
                }
                SaleDrugSalesStrategy saleDrugSalesStrategy = new SaleDrugSalesStrategy();
                saleDrugSalesStrategy.setOrganDrugListSalesStrategyId(organDrugSalesStrategy.getId());
                saleDrugSalesStrategy.setUnit(organDrugSalesStrategy.getUnit());
                saleDrugSalesStrategy.setButtonIsOpen("false");
                saleDrugSalesStrategy.setIsDefault("false");
                saleDrugSalesStrategies.add(saleDrugSalesStrategy);
            });
            //获取药企药品并保存药企药品销售策略
            List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIds(organDrugList.getOrganId());
            if(CollectionUtils.isNotEmpty(drugsEnterpriseList)){
                for(DrugsEnterprise drugsEnterprise : drugsEnterpriseList){
                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(organDrugList.getDrugId(), drugsEnterprise.getId());
                    logger.info("addOrganDrugSalesStrategy saleDrugList={}",JSONUtils.toString(saleDrugList));
                    if(null != saleDrugList){
                        List<SaleDrugSalesStrategy> saleDrugSalesStrategyList  = new ArrayList<>();
                        //如果是新增机构销售策略，药企药品销售策略追加到原来的策略基础上  如果是修改，直接替换原来的值
                        if("add".equals(type)){
                            if(StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())){
                                saleDrugSalesStrategyList = JSONObject.parseArray(saleDrugList.getEnterpriseSalesStrategy(),SaleDrugSalesStrategy.class);
                            }
                            saleDrugSalesStrategyList.addAll(saleDrugSalesStrategies);
                            saleDrugList.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategyList));
                            saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList);
                        }else if("delete".equals(type)){
                            if(StringUtils.isNotEmpty(saleDrugList.getEnterpriseSalesStrategy())){
                                saleDrugListDAO.updateenterpriseSalesStrategyByOrganDrugId(saleDrugList.getOrganDrugId());
                            }
                        }else if("update".equals(type)){
                            saleDrugSalesStrategyList.addAll(saleDrugSalesStrategies);
                            saleDrugList.setEnterpriseSalesStrategy(JSONUtils.toString(saleDrugSalesStrategyList));
                            saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList);
                        }

                    }
                }
            }
        } catch (Exception e){
            logger.error("销售策略 saveEnterpriseSalesStrategyByOrganDrugList",e);
            e.printStackTrace();
        }
    }
}
