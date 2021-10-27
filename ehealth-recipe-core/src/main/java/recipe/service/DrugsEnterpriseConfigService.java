package recipe.service;

import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.DrugsEnterpriseConfig;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.account.UserRoleToken;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
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

        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getDrugsenterpriseId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "同步医院药品开关值 is required");
        }
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        DrugsEnterpriseConfig byDrugsenterpriseId = drugsEnterpriseConfigDAO.getByDrugsenterpriseId(drugsEnterpriseConfig.getDrugsenterpriseId());
        if (ObjectUtils.isEmpty(byDrugsenterpriseId)){
            checkConfig(drugsEnterpriseConfig);
            DrugsEnterpriseConfig save = drugsEnterpriseConfigDAO.save(drugsEnterpriseConfig);
            if (!ObjectUtils.isEmpty(urt)){
                busActionLogService.recordBusinessLogRpcNew("药企配置管理", "", "DrugsEnterpriseConfig", "【" + urt.getUserName() + "】新增药企配置【" + JSONUtils.toString(save)
                        +"】", drugsEnterpriseDAO.getById(drugsEnterpriseConfig.getDrugsenterpriseId()).getName());
            }
            return save;
        }else {
            checkConfig(drugsEnterpriseConfig);
            drugsEnterpriseConfig.setId(byDrugsenterpriseId.getId());
            DrugsEnterpriseConfig update = drugsEnterpriseConfigDAO.update(drugsEnterpriseConfig);
            if (!ObjectUtils.isEmpty(urt)){
                busActionLogService.recordBusinessLogRpcNew("药企配置管理", "", "DrugsEnterpriseConfig", "【" + urt.getUserName() + "】更新药企配置【"+JSONUtils.toString(drugsEnterpriseConfig)+"】-》【" + JSONUtils.toString(update)
                        +"】", drugsEnterpriseDAO.getById(byDrugsenterpriseId.getDrugsenterpriseId()).getName());
            }
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
