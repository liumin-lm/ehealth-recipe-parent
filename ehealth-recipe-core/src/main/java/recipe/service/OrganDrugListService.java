package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.RegulationDrugCategoryReq;
import com.ngari.his.regulation.entity.RegulationNotifyDataReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.opbase.util.OpSecurityUtil;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.ProvUploadOrganDTO;
import com.ngari.patient.dto.zjs.RegulationDoctorIndicatorsBean;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.ProvUploadOrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.*;
import com.ngari.recipe.drug.service.IOrganDrugListService;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HqlUtils;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.dao.bean.DrugListAndOrganDrugList;
import recipe.drugTool.service.DrugToolService;
import recipe.drugsenterprise.ByRemoteService;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * @author zhongzx
 * @date 2016/5/27
 * 机构药品服务
 */
@RpcBean("organDrugListService")
public class OrganDrugListService implements IOrganDrugListService {

    private static final Logger logger = LoggerFactory.getLogger(OrganDrugListService.class);
    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    /**
     * 把药品添加到对应医院
     *
     * @param organDrugList
     * @return
     * @author zhongzx
     */
    @RpcService
    public boolean addDrugListForOrgan(OrganDrugList organDrugList) {
        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        logger.info("新增机构药品服务[addDrugListForOrgan]:" + JSONUtils.toString(organDrugList));
        if (null == organDrugList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugList is null");
        }
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (!drugListDAO.exist(organDrugList.getDrugId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
        }

        //验证药品必要信息
        validate(organDrugList);
        OrganDrugList save = dao.save(organDrugList);
        organDrugSync(save);
        return true;
    }

    private void validate(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        /*if (StringUtils.isEmpty(organDrugList.getProducerCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "producerCode is needed");
        }*/
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
        organDrugList.setCreateDt(new Date());
        if (null == organDrugList.getStatus()) {
            organDrugList.setStatus(1);
        }
        organDrugList.setLastModify(new Date());
    }

    /**
     * 同步自健药企药品
     * @param organDrugList
     */
    public void organDrugSync(OrganDrugList organDrugList){
        DrugToolService bean = AppDomainContext.getBean("eh.drugToolService", DrugToolService.class);
        bean.organDrugSync(organDrugList);
    }

    /**
     * 同步自健药企药品
     * @param organDrugList
     */
    public void organDrugSyncDelete(OrganDrugList organDrugList,Integer status){
        DrugToolService bean = AppDomainContext.getBean("eh.drugToolService", DrugToolService.class);
        List<OrganDrugList> lists= Lists.newArrayList();
        lists.add(organDrugList);
        DrugsEnterpriseDAO dao = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterpriseConfigService configService = AppContextHolder.getBean("eh.drugsEnterpriseConfigService", DrugsEnterpriseConfigService.class);
        List<DrugsEnterprise> drugsEnterprises = dao.findByOrganId(organDrugList.getOrganId());
        if (drugsEnterprises != null && drugsEnterprises.size() > 0 ){
            for (DrugsEnterprise drugsEnterpris : drugsEnterprises) {
                DrugsEnterpriseConfig config = configService.getConfigByDrugsenterpriseId(drugsEnterpris.getId());
                if (config.getEnable_drug_sync()==1){
                    String[] strings = config.getEnable_drug_syncType().split(",");
                    List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
                    if (syncTypeList.indexOf("3")!=-1){
                        if (status==1){
                            bean.deleteOrganDrugDataToSaleDrugList(lists,drugsEnterpris.getId());
                        }else if (status == 2){
                            try {
                                bean.updateOrganDrugDataToSaleDrugList(lists,drugsEnterpris.getId());
                            } catch (Exception e) {
                                logger.info("机构药品禁用删除同步对应药企"+e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void validateOrganDrugList(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        /*if (StringUtils.isEmpty(organDrugList.getProducerCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "producerCode is needed");
        }*/
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
    }

    /**
     * 批量导入邵逸夫药品（暂时用）
     *
     * @return
     * @author zhongzx
     */
    public void addDrugListForBatch() {
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugProducerDAO producerDAO = DAOFactory.getDAO(DrugProducerDAO.class);
        List<DrugList> list = dao.findAll();
        for (DrugList d : list) {
            OrganDrugList organDrug = new OrganDrugList();
            organDrug.setDrugId(d.getDrugId());
            organDrug.setOrganId(1);
            organDrug.setCreateDt(new Date());
            organDrug.setLastModify(new Date());
            //把药品产地转换成相应的医院的代码
            List<DrugProducer> producers = producerDAO.findByNameAndOrgan(d.getProducer(), 1);
            if (null != producers && producers.size() > 0) {
                organDrug.setProducerCode(producers.get(0).getCode());
            } else {
                organDrug.setProducerCode("");
            }
            organDrug.setStatus(1);
            OrganDrugList save = organDrugListDAO.save(organDrug);
            organDrugSync(save);
        }
    }

    /**
     * 删除机构药品数据
     *
     * @param organDrugListId 入参药品参数
     */
    @RpcService
    public void deleteOrganDrugListById(Integer organDrugListId) {
        if (organDrugListId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = organDrugListDAO.get(organDrugListId);
        organDrugSyncDelete(organDrugList,1);
        organDrugListDAO.remove(organDrugListId);

    }

    /**
     * 删除机构药品数据
     *
     * @param organId 入参机构ID
     */
    @RpcService
    public void deleteByOrganId(Integer organId) {
        if (organId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        organDrugListDAO.deleteByOrganId(organId);
        UserRoleToken urt = UserRoleToken.getCurrent();
        OrganService bean = AppDomainContext.getBean("basic.organService", OrganService.class);

        OrganDTO byOrganId = bean.getByOrganId(organId);
        if (ObjectUtils.isEmpty(byOrganId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该机构!");
        }
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + urt.getUserName() + "】一键删除【" + byOrganId.getName()
                +"】药品", byOrganId.getName());
    }

    /**
     * 批量删除机构药品数据
     *
     * @param organDrugListIds 入参药品参数集合
     */
    @RpcService
    public void deleteOrganDrugListByIds(List<Integer> organDrugListIds) {
        if (CollectionUtils.isEmpty(organDrugListIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        Integer organDu = organDrugListIds.get(0);
        OrganDrugList organDul = organDrugListDAO.get(organDu);
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organDul.getOrganId());
        StringBuilder msg = new StringBuilder("【" + organDTO.getName() + "】删除药品");
        for (Integer organDrugListId : organDrugListIds) {
            OrganDrugList organDrugList = organDrugListDAO.get(organDrugListId);
            msg.append("【" + organDrugList.getOrganDrugId() + "-" + organDrugList.getDrugName() + "】");
            deleteOrganDrugListById(organDrugListId);
        }
        busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", msg.toString(), organDTO.getName());
    }

    /**
     * 删除药品在医院中的信息
     *
     * @param organDrugListId 入参药品参数
     * @param status          入参药品参数
     */
    @RpcService
    public OrganDrugList updateOrganDrugListStatusById(Integer organDrugListId, Integer status, String disableReason) {
        if (organDrugListId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = organDrugListDAO.get(organDrugListId);
        OrganDTO organDTO = organService.getByOrganId(organDrugList.getOrganId());
        organDrugList.setStatus(status);
        String msg = "启用";
        if (status.equals(0)) {
            if (ObjectUtils.isEmpty(disableReason)) {
                throw new DAOException(DAOException.VALUE_NEEDED, "disableReason is required");
            }
            msg = "禁用";
            organDrugList.setDisableReason(disableReason);
        }
        logger.info("禁用机构药品:" + JSONUtils.toString(organDrugList));
        organDrugList.setLastModify(new Date());
        organDrugSyncDelete(organDrugList,2);
        logger.info("禁用机构药品01:" + JSONUtils.toString(organDrugList));
        OrganDrugList update = organDrugListDAO.update(organDrugList);
        logger.info("禁用机构药品02:" + JSONUtils.toString(update));
        busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + organDTO.getName() + "】" + msg + "【" + organDrugList.getOrganDrugId() + "-" + organDrugList.getDrugName() + "】", organDTO.getName());
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        RegulationNotifyDataReq req = new RegulationNotifyDataReq();
        req.setBussType("drug");
        req.setNotifyTime(System.currentTimeMillis() - 1000);
        req.setOrganId(organDrugList.getOrganId());
        iRegulationService.notifyData(organDrugList.getOrganId(), req);
        return update;
    }
    /**
     * 药品目录-机构药品目录增加一个权限策略，一键禁用（只在监管平台增加此权限），支持将当前有效状态的药品全部更改为无效状态；
     *
     * @param organId 机构Id
     */
    @RpcService
    public void updateOrganDrugListStatusByOrganId(Integer organId) {
        if (organId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        Integer status = 0;
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        try {
            organDrugListDAO.updateDrugStatus(organId, status);
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organDTO = organService.getByOrganId(organId);
            IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
            busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + organDTO.getName() + "】" + "药品一键禁用!" , organDTO.getName());
        } catch (Exception e) {
            logger.info("一键禁用机构药品[updateOrganDrugListStatusByOrganId]:" + e);
        }
    }


    /**
     * 药品目录-机构药品目录增加一个权限策略，一键禁用（只在监管平台增加此权限），支持将当前有效状态的药品全部更改为无效状态；
     *
     * @param organId 机构Id
     */
    @RpcService
    public void updateOrganDrugListStatusByIdSync(Integer organId,Integer organDrugId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = organDrugListDAO.get(organDrugId);
        try {
            organDrugList.setStatus(0);
            OrganDrugList update = organDrugListDAO.update(organDrugList);
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organDTO = organService.getByOrganId(organId);
            IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
            busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + organDTO.getName() + "】" + "药品同步 药品禁用!"+update.getDrugName() , organDTO.getName());
        } catch (Exception e) {
            logger.info("同步药品禁用药品[updateOrganDrugListStatusById]:" + e);
        }
    }


    /**
     * 更新药品在医院中的信息
     *
     * @param organDrugList 入参药品参数
     * @return 机构药品信息
     * @author zhongzx
     */
    @RpcService
    public OrganDrugListDTO updateOrganDrugList(OrganDrugList organDrugList) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        updateValidate(organDrugList);
        //对药品名、药品代码、院内检索码作唯一性校验
        List<String> drugCodes = new ArrayList<>(1);
        drugCodes.add(organDrugList.getOrganDrugCode());
        if (StringUtils.isEmpty(organDrugList.getSaleName())) {
            organDrugList.setSaleName(organDrugList.getDrugName());
        } else {
            organDrugList.setSaleName(organDrugList.getSaleName());
        }
        Integer organId = organDrugList.getOrganId();
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        //tong步赋值老的途径与频率
        setOldRateAndWays(organDrugList);
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        if (organDrugList.getOrganDrugId() == null || organDrugList.getOrganDrugId() == 0) {
            logger.info("新增机构药品服务[updateOrganDrugList]:" + JSONUtils.toString(organDrugList));
            //说明为该机构新增机构药品
            if (!drugListDAO.exist(organDrugList.getDrugId())) {
                throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
            }
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organDrugList.getOrganId(), drugCodes);
            if (organDrugLists != null && organDrugLists.size() > 0) {
                //说明不唯一了
                throw new DAOException(DAOException.VALUE_NEEDED, "该机构药品代码已经存在");
            }
            //验证药品必要信息
            validate(organDrugList);
            DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
            organDrugList.setOrganDrugId(null);
            organDrugList.setProducer(drugList.getProducer());
            if (StringUtils.isEmpty(organDrugList.getProducerCode())) {
                organDrugList.setProducerCode("");
            }
            OrganDrugList saveOrganDrugList = organDrugListDAO.save(organDrugList);
            addOrganDrugListToBy(saveOrganDrugList);
            uploadOrganDrugListToJg(saveOrganDrugList);
            RegulationNotifyDataReq req = new RegulationNotifyDataReq();
            req.setBussType("drug");
            req.setNotifyTime(System.currentTimeMillis() - 1000);
            req.setOrganId(saveOrganDrugList.getOrganId());
            iRegulationService.notifyData(saveOrganDrugList.getOrganId(), req);
            organDrugSync(saveOrganDrugList);
            busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + organDTO.getName() + "】新增药品【" + saveOrganDrugList.getOrganDrugId() + "-" + saveOrganDrugList.getDrugName() + "】", organDTO.getName());
            return ObjectCopyUtils.convert(saveOrganDrugList, OrganDrugListDTO.class);
        } else {
            logger.info("修改机构药品服务[updateOrganDrugList]:" + JSONUtils.toString(organDrugList));
            OrganDrugList target = organDrugListDAO.get(organDrugList.getOrganDrugId());
            if (null == target) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "此药在该医院药品列表中不存在");
            } else {
                //说明为更新机构药品目录,需要校验是否变更编号
                if (!organDrugList.getOrganDrugCode().equals(target.getOrganDrugCode())) {
                    //对药品名、药品代码、院内检索码作唯一性校验
                    List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organDrugList.getOrganId(), drugCodes);
                    if (organDrugLists != null && organDrugLists.size() > 0) {
                        //说明不唯一了
                        throw new DAOException(DAOException.VALUE_NEEDED, "该机构药品代码已经存在");
                    }
                }
                BeanUtils.map(organDrugList, target);
                if (organDrugList.getUseDose() == null) {
                    target.setUseDose(null);
                }
                if (organDrugList.getRecommendedUseDose() == null) {
                    target.setRecommendedUseDose(null);
                }
                if (organDrugList.getSmallestUnitUseDose() == null) {
                    target.setSmallestUnitUseDose(null);
                }
                if (organDrugList.getDefaultSmallestUnitUseDose() == null) {
                    target.setDefaultSmallestUnitUseDose(null);
                }
                target.setLastModify(new Date());
                validateOrganDrugList(target);
                target = organDrugListDAO.update(target);
                uploadOrganDrugListToJg(target);
                organDrugSync(target);
                busActionLogService.recordBusinessLogRpcNew("机构药品管理", "", "OrganDrugList", "【" + organDTO.getName() + "】更新药品【" + target.getOrganDrugId() + "-" + target.getDrugName() + "】", organDTO.getName());
            }
            RegulationNotifyDataReq req = new RegulationNotifyDataReq();
            req.setBussType("drug");
            req.setNotifyTime(System.currentTimeMillis() - 1000);
            req.setOrganId(target.getOrganId());
            iRegulationService.notifyData(target.getOrganId(), req);
            return ObjectCopyUtils.convert(target, OrganDrugListDTO.class);
        }
    }

    private void setOldRateAndWays(OrganDrugList organDrugList) {
        try {
            IUsingRateService usingRateService = AppContextHolder.getBean("eh.usingRateService", IUsingRateService.class);
            IUsePathwaysService usePathwaysService = AppContextHolder.getBean("eh.usePathwaysService", IUsePathwaysService.class);
            if (organDrugList != null && !StringUtils.isEmpty(organDrugList.getUsingRateId())) {
                Integer usingRateId = Integer.valueOf(organDrugList.getUsingRateId());
                UsingRateDTO usingRateDTO = usingRateService.getById(usingRateId);
                organDrugList.setUsingRate(usingRateDTO.getRelatedPlatformKey());
            }
            if (organDrugList != null && !StringUtils.isEmpty(organDrugList.getUsePathwaysId())) {
                Integer usePathwaysId = Integer.valueOf(organDrugList.getUsePathwaysId());
                UsePathwaysDTO usePathwaysDTO = usePathwaysService.getById(usePathwaysId);
                organDrugList.setUsePathways(usePathwaysDTO.getRelatedPlatformKey());
            }
        } catch (Exception e) {
            logger.error("设置老使用频率失败", e);
        }

    }


    //上海六院的新增药品信息同步到百洋
    private void addOrganDrugListToBy(OrganDrugList organDrugList) {
        try {
            if (organDrugList != null) {
                //(异步的过程，不影响主流程)
                GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
                        String organCode = recipeParameterDao.getByName("sh_baiyang_druglist");
                        if (StringUtils.isNotEmpty(organCode)) {
                            if (Integer.parseInt(organCode) == organDrugList.getOrganId()) {
                                logger.info("同步药品数据到百洋药企：" + JSONUtils.toString(organDrugList));
                                //表示是上海六院的新增药品，需要同步到百洋药企
                                ByRemoteService byRemoteService = ApplicationUtils.getRecipeService(ByRemoteService.class);
                                byRemoteService.corresPondingHospDrugByOrganDrugListHttpRequest(organDrugList);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.error("addOrganDrugListToBy 同步到百洋药企药品数据出错：" + e.getMessage(), e);
        }
    }

    /**
     * 上传机构药品到监管平台
     */
    private void uploadOrganDrugListToJg(final OrganDrugList saveOrganDrugList) {
        //机构药品目录保存成功,异步上传到监管平台
        if (saveOrganDrugList != null) {
            //(异步的过程，不影响主流程)
            GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    uploadDrugToRegulation(saveOrganDrugList);
                }
            });
        }
    }

    /**
     * 两个地方需要上传---1.运营平台添加机构药品 2.药品工具提交机构药品数据
     *
     * @param saveOrganDrugList
     */
    public void uploadDrugToRegulation(OrganDrugList saveOrganDrugList) {
        List<RegulationDrugCategoryReq> drugCategoryReqs = new ArrayList<>();
        try {
            //使用最新的上传方式，兼容互联网环境和平台环境上传 旧his.provinceDataUploadService
            IRegulationService hisService =
                    AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            RegulationDrugCategoryReq drugCategoryReq = packingDrugCategoryReq(saveOrganDrugList);
            drugCategoryReqs.add(drugCategoryReq);
            HisResponseTO hisResponseTO = hisService.uploadDrugCatalogue(saveOrganDrugList.getOrganId(), drugCategoryReqs);
            logger.info("hisResponseTO parames:" + JSONUtils.toString(hisResponseTO));
        } catch (Exception e) {
            logger.error("上传药品到监管平台失败,{" + JSONUtils.toString(drugCategoryReqs) + "},{" + e.getMessage() + "}.", e);
        }
    }

    /**
     * 包装监管平台数据
     *
     * @param organDrugList
     * @return
     */
    private RegulationDrugCategoryReq packingDrugCategoryReq(OrganDrugList organDrugList) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        IMinkeOrganService minkeOrganService = AppContextHolder.getBean("jgpt.minkeOrganService", IMinkeOrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
        RegulationDrugCategoryReq drugCategoryReq = new RegulationDrugCategoryReq();
        String organId = minkeOrganService.getRegisterNumberByUnitId(organDTO.getMinkeUnitID());
        //这俩个字段 在his-server set进去了 平台模式这里可以不传
        drugCategoryReq.setUnitID(organDTO.getMinkeUnitID());
        drugCategoryReq.setOrganID(organId);
        drugCategoryReq.setOrganName(organDTO.getName());
        //如果存在 转换省平台药品id
        Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(organDrugList.getDrugId());
        if (targetDrugId != null) {
            drugCategoryReq.setPlatDrugCode(targetDrugId.toString());
        } else {
//            drugCategoryReq.setPlatDrugCode(organDrugList.getDrugId().toString());
            //对应运营平台药品详情中的 监管平台药品ID
            drugCategoryReq.setPlatDrugCode(organDrugList.getRegulationDrugCode());
        }
        //还原：测试说先不改。
        //1 如果存在 转换省平台药品id （入驻（浙江省）该表为空）
//        if (targetDrugId != null) {
//            drugCategoryReq.setPlatDrugCode(targetDrugId.toString());
//        } else {
//            String regulationDrugCode = organDrugList.getRegulationDrugCode();
//            //2 入驻 直接取平台药品ID 不用维护监管平台药品ID
//            if(StringUtils.isEmpty(regulationDrugCode)){
//                drugCategoryReq.setPlatDrugCode(organDrugList.getDrugId().toString());
//            }
//            //3 自建  对应运营平台药品详情中的 监管平台药品ID*
//            else{
//                drugCategoryReq.setPlatDrugCode(regulationDrugCode);
//            }
//        }
        drugCategoryReq.setPlatDrugName(organDrugList.getDrugName());
        if (StringUtils.isNotEmpty(organDrugList.getOrganDrugCode())) {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugCode());
        } else {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugId().toString());
        }
        drugCategoryReq.setDrugPrice(organDrugList.getSalePrice());
        drugCategoryReq.setHospDrugName(organDrugList.getDrugName());
        drugCategoryReq.setHospTradeName(organDrugList.getSaleName());
        if (StringUtils.isNotEmpty(organDrugList.getDrugSpec())) {
            drugCategoryReq.setHospDrugPacking(organDrugList.getDrugSpec());
        } else {
            if (StringUtils.isNotEmpty(drugList.getDrugSpec())) {
                drugCategoryReq.setHospDrugPacking(drugList.getDrugSpec());
            } else {
                drugCategoryReq.setHospDrugPacking("/");
            }
        }
        if (StringUtils.isNotEmpty(organDrugList.getProducer())) {
            drugCategoryReq.setHospDrugManuf(organDrugList.getProducer());
        } else {
            drugCategoryReq.setHospDrugManuf(drugList.getProducer());
        }

        drugCategoryReq.setUseFlag(organDrugList.getStatus() + "");
        drugCategoryReq.setDrugClass(drugList.getDrugClass());
        drugCategoryReq.setUpdateTime(new Date());
        drugCategoryReq.setCreateTime(new Date());
        return drugCategoryReq;
    }

    private void updateValidate(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        if (StringUtils.isEmpty(organDrugList.getDrugName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName is needed");
        }
        if (StringUtils.isEmpty(organDrugList.getSaleName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleName is needed");
        }
        if (organDrugList.getPack() == null || organDrugList.getPack() <= 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pack is needed or not is 0");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
    }

    /**
     * 机构药品查询
     *
     * @param organId   机构
     * @param drugClass 药品分类
     * @param keyword   查询关键字:药品序号 or 药品名称 or 生产厂家 or 商品名称 or 批准文号
     * @param start
     * @param limit
     * @return
     * @author houxr
     */
    @RpcService
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugListByOrganIdAndKeyword(final Integer organId,
                                                                                          final String drugClass,
                                                                                          final String keyword, final Integer status,
                                                                                          final int start, final int limit) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        QueryResult result = organDrugListDAO.queryOrganDrugListByOrganIdAndKeyword(organId, drugClass, keyword, status, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }

    /**
     * * 运营平台（权限改造）
     *
     * @param organId
     * @param drugClass
     * @param keyword
     * @param status
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugListByOrganIdAndKeywordForOp(final Integer organId,
                                                                                               final String drugClass,
                                                                                               final String keyword, final Integer status,
                                                                                               final int start, final int limit) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        QueryResult result = organDrugListDAO.queryOrganDrugListByOrganIdAndKeyword(organId, drugClass, keyword, status, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }



    /**
     * 获取需要审核药品数量 以及 机构未关联监管平台药品数量
     *
     * @param organId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Map<String, Object> getOrganDrugSyncSituation(Integer organId){
        Map<String, Object> map= Maps.newHashMap();
        DrugListMatchDAO dao = DAOFactory.getDAO(DrugListMatchDAO.class);
        List<DrugListMatch> matchList = dao.findMatchDataByOrganAndStatusAndrugSource(organId);
        map.put("DrugListMatch",matchList.size());
        List<OrganDrugList> drugLists = organDrugListDAO.findByOrganIdAndRegulationDrugCode(organId);
        map.put("OrganDrugList",drugLists.size());
        return map;
    }

    /**
     * * 运营平台（新增机构药品 查询）
     *
     * @param organId
     * @param drugClass
     * @param keyword
     * @param status
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugListByOrganIdAndKeywordAndProducer(final Integer organId,
                                                                                               final String drugClass,
                                                                                               final String keyword,final String producer, final Integer status,
                                                                                               final int start, final int limit) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        QueryResult result = organDrugListDAO.queryOrganDrugListByOrganIdAndKeywordAndProducer(organId, drugClass, keyword,producer, status, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }

    /**
     * * 运营平台（查询机构药品目录（可根据是否能配送查询））
     *
     * @param organId
     * @param drugClass
     * @param keyword
     * @param status
     * @param start
     * @param limit
     * @return
     */
    @Override
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugAndSaleForOp(final Date startTime, final Date endTime, final Integer organId,
                                                                               final String drugClass,
                                                                               final String keyword, final Integer status,
                                                                               final Integer isregulationDrug,
             final Integer type,final int start, final int limit, Boolean canDrugSend) {
        QueryResult result = null;
        OpSecurityUtil.isAuthorisedOrgan(organId);
        try {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            result = organDrugListDAO.queryOrganDrugAndSaleForOp(startTime, endTime, organId, drugClass, keyword, status,isregulationDrug,type, start, limit, canDrugSend);
            result.setItems(covertData(result.getItems()));
        } catch (Exception e) {
            logger.error("queryOrganDrugAndSaleForOp error", e);
            throw new DAOException(609, e.getMessage());
        }
        return result;
    }


    @Override
    public List<RegulationDrugCategoryBean> queryRegulationDrug(Map<String, Object> params) {
        ProvUploadOrganService provUploadOrganService = AppContextHolder.getBean("basic.provUploadOrganService", ProvUploadOrganService.class);
        List<RegulationDoctorIndicatorsBean> regulationDoctorIndicatorsReqList = new ArrayList<>();
        List drugList = HqlUtils.execHqlFindList("select a.organDrugId,a.organId,a.drugName,a.status,a.medicalDrugCode,a.drugForm, a.producer," +
                "a.baseDrug,a.licenseNumber,b.drugClass,a.regulationDrugCode,a.organDrugCode,a.saleName,a.drugSpec,a.salePrice,a.drugFormCode" +
                " from OrganDrugList a, DrugList b where a.drugId = b.drugId and a.lastModify>=:startDate and a.lastModify<=:endDate and a.organId IN :organIds", params);
        List<RegulationDrugCategoryBean> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(drugList)) {
            logger.info("机构药品信息数据总数：" + drugList.size());
            Map<Integer, String[]> organMsg = new HashMap<>();
            RegulationDrugCategoryBean bean;
            OrganService organService = AppContextHolder.getBean("basic.organService", OrganService.class);
            for (Object o : drugList) {
                bean = new RegulationDrugCategoryBean();
                Object[] obj = (Object[]) o;
                Integer organId = (Integer) obj[1];
                String[] str;
                String proOrganId = "";
                String proUnitID = "";
                String organName = "";
                if (organMsg.containsKey(organId)) {
                    str = organMsg.get(organId) == null ? new String[3] : organMsg.get(organId);
                    proOrganId = str[0];
                    proUnitID = str[1];
                    organName = str[2];
                } else {
                    str = new String[3];
                    organName = organService.getNameById(organId);
                    ProvUploadOrganDTO provUploadOrganDTO = provUploadOrganService.getByNgariOrganId(organId);
                    if (provUploadOrganDTO != null) {
                        proOrganId = provUploadOrganDTO.getOrganId();
                        proUnitID = provUploadOrganDTO.getUnitId();
                    }
                    str[0] = proOrganId;
                    str[1] = proUnitID;
                    str[2] = organName;
                    organMsg.put(organId, str);
                }
                bean.setOrganID(proOrganId);
                bean.setUnitID(proUnitID);
                bean.setOrganName(organName);
                String drugName = obj[2] == null ? null : String.valueOf(obj[2]);
                bean.setHospDrugName(drugName);  //医院药品通用名drugName药品注册通用名
                bean.setUseFlag(obj[3] == null ? "" : String.valueOf(obj[3]));  //status使用标志
                bean.setMedicalDrugCode(obj[4] == null ? null : String.valueOf(obj[4]));  //项目标准代码:medicalDrugCode医保药品编码
                bean.setDrugForm(obj[5] == null ? null : String.valueOf(obj[5]));  //剂型:drugForm剂型名称
                bean.setBaseDrug(obj[7] == null ? null : String.valueOf(obj[7]));  //是否基药:baseDrug基药标识
                bean.setLicenseNumber(obj[8] == null ? null : String.valueOf(obj[8]));  //批准文号:licenseNumber批准文号
                String drugClass = obj[9] == null ? "" : String.valueOf(obj[9]);
                String producer = obj[6] == null ? "" : String.valueOf(obj[6]);
                bean.setHospitalPreparation(StringUtils.indexOf(producer, "新华医院调拨") == -1 ? "0" : "1");  //上海儿童-院内制剂标志
                bean.setKssFlag(drugClass.startsWith("0101") ? "1" : "0");
                bean.setDmjfFlag(drugClass.startsWith("02") || drugClass.startsWith("04") ? "1" : "0");
                bean.setModifyFlag("1");

                bean.setPlatDrugCode(obj[10] == null ? null : String.valueOf(obj[10]));
                bean.setPlatDrugName(drugName);
                bean.setHospDrugCode(obj[11] == null ? null : String.valueOf(obj[11]));
                bean.setHospTradeName(obj[12] == null ? null : String.valueOf(obj[12]));
                bean.setHospDrugAlias(null);
                bean.setHospDrugPacking(obj[13] == null ? null : String.valueOf(obj[13]));
                bean.setHospDrugManuf(producer);
                bean.setDrugClass(drugClass);
                bean.setDrugPrice(obj[14] == null ? null : (BigDecimal) obj[14]);
                bean.setDrugFormCode(obj[15] == null ? null : String.valueOf(obj[15]));

                result.add(bean);
            }
        }
        return result;
    }

    @Override
    public List<OrganDrugListBean> findByOrganIdAndDrugIdAndOrganDrugCode(int organId, int drugId, String organDrugCode) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCode(organId, drugId, organDrugCode);
        return ObjectCopyUtils.convert(organDrugLists, OrganDrugListBean.class);
    }

    @Override
    public List<OrganDrugListBean> findByOrganId(int organId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> byOrganId = organDrugListDAO.findByOrganId(organId);
        return ObjectCopyUtils.convert(byOrganId, OrganDrugListBean.class);
    }

    @Override
    public List<OrganDrugListBean> findByDrugIdAndOrganId(int drugId, int organId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugList = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
        return ObjectCopyUtils.convert(organDrugList, OrganDrugListBean.class);
    }

    /**
     * 更新药品最新的价格等
     *
     * @param organId      机构id
     * @param recipeDetail
     */
    public void saveOrganDrug(Integer organId, Recipedetail recipeDetail) {
        logger.info("saveOrganDrug  organId={}, recipeDetail：{}", organId, JSONUtils.toString(recipeDetail));
        if (null == recipeDetail || null == organId || null == recipeDetail.getDrugId()) {
            return;
        }
        OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
        if (null == organDrug) {
            logger.warn("saveOrganDrug  organDrug is null organId={}, recipeDetail：{}", organId, JSONUtils.toString(recipeDetail));
            return;
        }
        Boolean isUpdate = false;
        if (null != recipeDetail.getSalePrice()) {
            organDrug.setSalePrice(recipeDetail.getSalePrice());
            isUpdate = true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getDrugSpec())) {
            organDrug.setDrugSpec(recipeDetail.getDrugSpec());
            isUpdate = true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getMedicalDrugCode())) {
            organDrug.setMedicalDrugCode(recipeDetail.getMedicalDrugCode());
            isUpdate = true;
        }
        if (null != recipeDetail.getPack()) {
            organDrug.setPack(recipeDetail.getPack());
            isUpdate = true;
        }
        if (isUpdate) {
            logger.info("saveOrganDrug  organDrug：{}", JSONUtils.toString(organDrug));
            OrganDrugList update = organDrugListDAO.update(organDrug);
            organDrugSync(update);
        }
    }

    private List<DrugListAndOrganDrugListDTO> covertData(List<DrugListAndOrganDrugList> dbList) {
        List<DrugListAndOrganDrugListDTO> newList = Lists.newArrayList();
        DrugListAndOrganDrugListDTO backDTO;
        if (CollectionUtils.isNotEmpty(dbList)) {
            for (DrugListAndOrganDrugList daod : dbList) {
                backDTO = new DrugListAndOrganDrugListDTO();
                backDTO.setDrugList(ObjectCopyUtils.convert(daod.getDrugList(), DrugListBean.class));
                backDTO.setOrganDrugList(ObjectCopyUtils.convert(daod.getOrganDrugList(), OrganDrugListDTO.class));
                backDTO.setCanDrugSend(daod.getCanDrugSend());
                backDTO.setDepSaleDrugInfos(daod.getDepSaleDrugInfos());
                newList.add(backDTO);
            }
        }
        return newList;
    }

    @Override
    public OrganDrugListBean getByOrganIdAndOrganDrugCode(int organId, String organDrugCode) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(organId, organDrugCode);
        return ObjectCopyUtils.convert(organDrugList, OrganDrugListBean.class);
    }

    @Override
    public Integer findTargetDrugIdByOriginalDrugId(Integer originalDrugId) {
        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(originalDrugId);
        return targetDrugId;
    }

    @Override
    public OrganDrugListBean getByOrganIdAndOrganDrugCodeAndDrugId(Integer organId, String organDrugId, Integer drugId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        return ObjectCopyUtils.convert(organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId,organDrugId,drugId), OrganDrugListBean.class);
    }

    @Override
    public List<OrganDrugListBean> findByDrugIdsAndOrganIds(List<Integer> drugIds, List<Integer> organIds) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugList = organDrugListDAO.findByDrugIdsAndOrganIds(drugIds, organIds);
        return ObjectCopyUtils.convert(organDrugList, OrganDrugListBean.class);
    }

    @RpcService
    public Long getCountByDrugId(int drugId){
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        Long result =  organDrugListDAO.getCountByDrugId(drugId);
        return result == null ? 0 : result;
    }

}
