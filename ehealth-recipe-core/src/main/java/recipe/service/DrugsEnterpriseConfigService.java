package recipe.service;

import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.DrugsEnterpriseConfig;
import com.ngari.recipe.entity.SaleDrugListSyncField;
import ctd.account.UserRoleToken;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.dao.DrugsEnterpriseConfigDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.SaleDrugListSyncFieldDAO;

import java.util.*;

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
    SaleDrugListSyncFieldDAO saleDrugListSyncFieldDAO;

    @Autowired
    DrugsEnterpriseDAO drugsEnterpriseDAO;


    public void checkConfig(DrugsEnterpriseConfig drugsEnterpriseConfig){
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncDataSource())){
            drugsEnterpriseConfig.setSyncDataSource(1);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getEnable_drug_syncType())){
            drugsEnterpriseConfig.setEnable_drug_syncType("1,2");
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getSyncSaleDrugCodeType())){
            drugsEnterpriseConfig.setSyncSaleDrugCodeType(1);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getAddSyncDataRange())){
            drugsEnterpriseConfig.setAddSyncDataRange(2);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getUpdateSyncDataRange())){
            drugsEnterpriseConfig.setUpdateSyncDataRange(2);
        }
        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getDelSyncDataRange())){
            drugsEnterpriseConfig.setDelSyncDataRange(2);
        }

        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getAddSyncDrugType())){
            drugsEnterpriseConfig.setAddSyncDrugType("1,2,3");
        }

        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getUpdateSyncDrugType())){
            drugsEnterpriseConfig.setUpdateSyncDrugType("1,2,3");
        }

        if (ObjectUtils.isEmpty(drugsEnterpriseConfig.getDelSyncDrugType())){
            drugsEnterpriseConfig.setDelSyncDrugType("1,2,3");
        }
    }

    /**
     * 保存或更新DrugsEnterpriseConfig
     * 新方法addOrUpdateDrugsEnterpriseConfig2
     * @param drugsEnterpriseConfig
     * @return
     */
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

    /**
     * 保存或更新DrugsEnterpriseConfig
     * @param drugsEnterpriseConfig
     * @return
     */
    @RpcService
    public DrugsEnterpriseConfig addOrUpdateDrugsEnterpriseConfig2(DrugsEnterpriseConfig drugsEnterpriseConfig){
        logger.info("addOrUpdateDrugsEnterpriseConfig2 param:{}",JSONUtils.toString(drugsEnterpriseConfig));
        DrugsEnterpriseConfig drugsEnterpriseConfig1=addOrUpdateDrugsEnterpriseConfig(drugsEnterpriseConfig);
        List<SaleDrugListSyncField> saleDrugListSyncFieldList=drugsEnterpriseConfig.getSaleDrugListSyncFieldList();
        if(!CollectionUtils.isEmpty(saleDrugListSyncFieldList)){
            saleDrugListSyncFieldList.forEach(saleDrugListSyncField -> {
                saleDrugListSyncFieldDAO.update(saleDrugListSyncField);
            });
            drugsEnterpriseConfig1.setSaleDrugListSyncFieldList(saleDrugListSyncFieldList);
        }else{
            drugsEnterpriseConfig1.setSaleDrugListSyncFieldList(addSaleDrugListSyncFieldForEnterprise(drugsEnterpriseConfig.getDrugsenterpriseId()));

        }
        return drugsEnterpriseConfig1;

    }

    /**
     * 根据药企ID  查询药企配置表
     *
     * @param drugsenterpriseId
     * @return
     */
    @RpcService
    @LogRecord
    public DrugsEnterpriseConfig getConfigByDrugsenterpriseId(Integer drugsenterpriseId){
        if (ObjectUtils.isEmpty(drugsenterpriseId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsenterpriseId);
        if (ObjectUtils.isEmpty(drugsEnterprise)){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企"+drugsenterpriseId);
        }
        DrugsEnterpriseConfig byDrugsenterpriseId = drugsEnterpriseConfigDAO.getByDrugsenterpriseId(drugsenterpriseId);
        List<SaleDrugListSyncField> saleDrugListSyncFieldListDb = saleDrugListSyncFieldDAO.findByDrugsenterpriseId(drugsenterpriseId);
        if (ObjectUtils.isEmpty(byDrugsenterpriseId)){
                DrugsEnterpriseConfig config=new DrugsEnterpriseConfig();
                config.setDrugsenterpriseId(drugsenterpriseId);
                config.setEnable_drug_sync(1);
                DrugsEnterpriseConfig config1 = addOrUpdateDrugsEnterpriseConfig(config);
                if (CollectionUtils.isEmpty(saleDrugListSyncFieldListDb)){
                    logger.info("getConfigByDrugsenterpriseId 新增配置1");
                    config1.setSaleDrugListSyncFieldList(addSaleDrugListSyncFieldForEnterprise(drugsenterpriseId));
                }else{
                    byDrugsenterpriseId.setSaleDrugListSyncFieldList(saleDrugListSyncFieldListDb);
                }
                return config1;

        }else{
            if (CollectionUtils.isEmpty(saleDrugListSyncFieldListDb)){
                logger.info("getConfigByDrugsenterpriseId 新增配置2 ");
                byDrugsenterpriseId.setSaleDrugListSyncFieldList(addSaleDrugListSyncFieldForEnterprise(drugsenterpriseId));
            }else{
                byDrugsenterpriseId.setSaleDrugListSyncFieldList(saleDrugListSyncFieldListDb);
            }
            return byDrugsenterpriseId;
        }

    }

    /**
     * 给药企添加药企药品目录同步字段配置（配置初始化）
     * @param drugsenterpriseId
     * @return
     */
    @LogRecord
    List<SaleDrugListSyncField> addSaleDrugListSyncFieldForEnterprise(Integer drugsenterpriseId) {
        List<SaleDrugListSyncField> saleDrugListSyncFieldList=new ArrayList<>();
        LinkedHashMap<String,String> fieldMap=initFieldMap();
        LinkedHashMap<String,String> addIsAllowEditFieldMap =initAddIsAllowEditFieldMap();
        LinkedHashMap<String,String> updateIsAllowEditFieldMap =initUpdateIsAllowEditFieldMap();
        Set set = fieldMap.keySet();
        List<String> typeList=initTypeList();
        for (Object key : set) {
            typeList.forEach(type->{
                SaleDrugListSyncField saleDrugListSyncField=new SaleDrugListSyncField();
                saleDrugListSyncField.setDrugsenterpriseId(drugsenterpriseId);
                saleDrugListSyncField.setFieldCode(key+"");
                saleDrugListSyncField.setFieldName(fieldMap.get(key));
                saleDrugListSyncField.setType(type);
                if("1".equals(type)){
                    //新增
                    saleDrugListSyncField.setIsAllowEdit(addIsAllowEditFieldMap.get(key));
                }else{
                    saleDrugListSyncField.setIsAllowEdit(updateIsAllowEditFieldMap.get(key));
                }
                checkSaleDrugListSyncField(saleDrugListSyncField);
                SaleDrugListSyncField saleDrugListSyncField1 = addOrUpdateSaleDrugListSyncField(saleDrugListSyncField);
                saleDrugListSyncFieldList.add(saleDrugListSyncField1);
            });
        }
        return saleDrugListSyncFieldList;
    }

    @LogRecord
    private LinkedHashMap<String,String> initFieldMap(){
        LinkedHashMap<String,String> fieldMap=new LinkedHashMap<>();
        fieldMap.put("saleDrugCode","药企药品编码");
        fieldMap.put("drugName","药品名");//机构药品名称
        fieldMap.put("saleName","商品名");
        fieldMap.put("price","价格（每售价，不含税）");//无税单价
        fieldMap.put("drugSpec","机构药品规格");
        fieldMap.put("status","使用状态");
        return fieldMap;
    }

    @LogRecord
    private LinkedHashMap<String,String> initAddIsAllowEditFieldMap(){
        LinkedHashMap<String,String> fieldMap=new LinkedHashMap<>();
        fieldMap.put("saleDrugCode","0");
        fieldMap.put("drugName","0");
        fieldMap.put("saleName","0");
        fieldMap.put("drugSpec","0");
        fieldMap.put("price","0");
        fieldMap.put("status","0");
        return fieldMap;
    }

    @LogRecord
    private LinkedHashMap<String,String> initUpdateIsAllowEditFieldMap(){
        LinkedHashMap<String,String> fieldMap=new LinkedHashMap<>();
        fieldMap.put("saleDrugCode","0");
        fieldMap.put("drugName","1");
        fieldMap.put("saleName","1");
        fieldMap.put("drugSpec","1");
        fieldMap.put("price","1");
        fieldMap.put("status","1");
        return fieldMap;
    }

    @LogRecord
    private List<String> initTypeList(){
        List<String> typeList=new ArrayList<>();
        typeList.add("1");
        typeList.add("2");
        return typeList;
    }

    @LogRecord
    private synchronized SaleDrugListSyncField addOrUpdateSaleDrugListSyncField(SaleDrugListSyncField saleDrugListSyncField) {
        if (ObjectUtils.isEmpty(saleDrugListSyncField.getDrugsenterpriseId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "药企ID is required");
        }

        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        List<SaleDrugListSyncField> saleDrugListSyncFieldDbs = saleDrugListSyncFieldDAO.findByDrugsenterpriseIdAndFieldCodeAndType(saleDrugListSyncField.getDrugsenterpriseId(),saleDrugListSyncField.getFieldCode(),saleDrugListSyncField.getType());
        logger.info("addOrUpdateSaleDrugListSyncField saleDrugListSyncFieldDbs:{}",JSONUtils.toString(saleDrugListSyncFieldDbs));
        SaleDrugListSyncField saleDrugListSyncFieldDb=null;
        if(!CollectionUtils.isEmpty(saleDrugListSyncFieldDbs)){
            saleDrugListSyncFieldDb=saleDrugListSyncFieldDbs.get(0);
            logger.info("addOrUpdateSaleDrugListSyncField saleDrugListSyncFieldDb:{}",JSONUtils.toString(saleDrugListSyncFieldDb));
        }
        if (ObjectUtils.isEmpty(saleDrugListSyncFieldDb)){
            checkSaleDrugListSyncField(saleDrugListSyncField);
            SaleDrugListSyncField save = saleDrugListSyncFieldDAO.save(saleDrugListSyncField);
            if (!ObjectUtils.isEmpty(urt)){
                busActionLogService.recordBusinessLogRpcNew("药企配置管理", "", "SaleDrugListSyncField", "【" + urt.getUserName() + "】新增药企药品同步字段配置【" + JSONUtils.toString(save)
                        +"】", drugsEnterpriseDAO.getById(saleDrugListSyncField.getDrugsenterpriseId()).getName());
            }
            return save;
        }else {
            checkSaleDrugListSyncField(saleDrugListSyncFieldDb);
            saleDrugListSyncField.setId(saleDrugListSyncFieldDb.getId());
            saleDrugListSyncField.setUpdateTime(new Date());
            SaleDrugListSyncField update = saleDrugListSyncFieldDAO.update(saleDrugListSyncField);
            if (!ObjectUtils.isEmpty(urt)){
                busActionLogService.recordBusinessLogRpcNew("药企配置管理", "", "SaleDrugListSyncField", "【" + urt.getUserName() + "】更新药企药品同步字段配置【"+JSONUtils.toString(saleDrugListSyncField)+"】-》【" + JSONUtils.toString(update)
                        +"】", drugsEnterpriseDAO.getById(saleDrugListSyncField.getDrugsenterpriseId()).getName());
            }
            return update;
        }
    }

    /**
     * 默认值设置
     * @param saleDrugListSyncField
     */
    private void checkSaleDrugListSyncField(SaleDrugListSyncField saleDrugListSyncField) {
        if (ObjectUtils.isEmpty(saleDrugListSyncField.getCreateTime())){
            saleDrugListSyncField.setCreateTime(new Date());
        }
        if (ObjectUtils.isEmpty(saleDrugListSyncField.getIsSync())){
            saleDrugListSyncField.setIsSync("1");
        }
        saleDrugListSyncField.setUpdateTime(new Date());
    }


}
