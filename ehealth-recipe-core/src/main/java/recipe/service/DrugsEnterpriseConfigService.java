package recipe.service;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.DrugsEnterpriseConfig;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.dao.DrugsEnterpriseConfigDAO;
import recipe.dao.DrugsEnterpriseDAO;

/**
 * @author rfh
 * @date 2021/10/23
 * 药企配置服务
 */
@RpcBean("drugsEnterpriseConfigService")
public class DrugsEnterpriseConfigService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    DrugsEnterpriseConfigDAO drugsEnterpriseConfigDAO;

    @Autowired
    DrugsEnterpriseDAO drugsEnterpriseDAO;


    public void checkConfig(DrugsEnterpriseConfig drugsEnterpriseConfig){
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncDataSource())){
            drugsEnterpriseConfig.setSyncDataSource(1);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getEnable_drug_syncType())){
            drugsEnterpriseConfig.setEnable_drug_syncType("1,2,3");
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncSaleDrugCodeType())){
            drugsEnterpriseConfig.setSyncSaleDrugCodeType(1);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncDataRange())){
            drugsEnterpriseConfig.setSyncDataRange(2);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncDrugType())){
            drugsEnterpriseConfig.setSyncDrugType("1,2,3");
        }
    }

    @RpcService
    public DrugsEnterpriseConfig addOrUpdateDrugsEnterpriseConfig(DrugsEnterpriseConfig drugsEnterpriseConfig){
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getDrugsenterpriseId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "药企ID is required");
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getEnable_drug_sync())){
            throw new DAOException(DAOException.VALUE_NEEDED, "同步医院药品开关值 is required");
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getId())){
            checkConfig(drugsEnterpriseConfig);
            DrugsEnterpriseConfig save = drugsEnterpriseConfigDAO.save(drugsEnterpriseConfig);
            return save;
        }else {
            checkConfig(drugsEnterpriseConfig);
            DrugsEnterpriseConfig update = drugsEnterpriseConfigDAO.update(drugsEnterpriseConfig);
            return update;
        }
    }

    @RpcService
    public DrugsEnterpriseConfig getConfigByDrugsenterpriseId(Integer drugsenterpriseId){
        if (ObjectUtils.isEmpty(drugsenterpriseId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required");
        }
        DrugsEnterpriseConfig byDrugsenterpriseId = drugsEnterpriseConfigDAO.getByDrugsenterpriseId(drugsenterpriseId);
        if (ObjectUtils.isEmpty(byDrugsenterpriseId)){
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsenterpriseId);
            if (ObjectUtils.isEmpty(drugsEnterprise)){
                throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企"+drugsenterpriseId);
            }else {
                DrugsEnterpriseConfig config=new DrugsEnterpriseConfig();
                config.setDrugsenterpriseId(drugsenterpriseId);
                config.setEnable_drug_sync(1);
                DrugsEnterpriseConfig config1 = addOrUpdateDrugsEnterpriseConfig(config);
                return config1;
            }
        }
        return byDrugsenterpriseId;
    }



}
