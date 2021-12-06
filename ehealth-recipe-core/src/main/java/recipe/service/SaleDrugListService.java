package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListAndSaleDrugListDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.SaleDrugListDTO;
import com.ngari.recipe.drug.service.ISaleDrugListService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.dao.bean.DrugListAndSaleDrugList;
import recipe.serviceprovider.drug.service.RemoteDrugService;

import java.util.Date;
import java.util.List;

/**
 * @author houxr
 * @date 2016/7/14
 * 销售药品管理服务
 */
@RpcBean("saleDrugListService")
public class SaleDrugListService implements ISaleDrugListService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private void validateSaleDrugList(SaleDrugList saleDrugList) {
        if (null == saleDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == saleDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == saleDrugList.getPrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "药品价格 必填!");
        }
        if (null == saleDrugList.getDrugName()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "药品名 必填!");
        }
        if (null == saleDrugList.getOrganDrugCode()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "药品编码 必填!");
        }

    }

    /**
     * 新增销售机构药品服务
     *
     * @param saleDrugList
     * @return
     * @author houxr
     */
    @RpcService
    public boolean addSaleDrugList(SaleDrugList saleDrugList) {
        logger.info("新增销售机构药品服务[addSaleDrugList]:" + JSONUtils.toString(saleDrugList));
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        SaleDrugListDAO dao = DAOFactory.getDAO(SaleDrugListDAO.class);
        if (null == saleDrugList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleDrugList is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (!drugsEnterpriseDAO.exist(saleDrugList.getOrganId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugsEnterprise not exist");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (!drugListDAO.exist(saleDrugList.getDrugId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
        }

        SaleDrugList saleDrug = dao.getByOrganIdAndDrugId(saleDrugList.getOrganId(),saleDrugList.getDrugId());
        if (saleDrug != null){
            throw new DAOException( "添加重复");
        }

        DrugList drugList = drugListDAO.get(saleDrugList.getDrugId());
        if (drugList == null){
            drugList = new DrugList();
        }
        saleDrugList.setSaleDrugCode(saleDrugList.getOrganDrugCode());

        //验证药品必要信息
        validateSaleDrugList(saleDrugList);
        saleDrugList.setCreateDt(new Date());
        saleDrugList.setLastModify(new Date());
        saleDrugList.setStatus(1);
        dao.save(saleDrugList);
        busActionLogService.recordBusinessLogRpcNew("药企药品管理","","SaleDrugList","【"+drugsEnterprise.getName()+"】新增药品【"+saleDrugList.getOrganDrugId()+"-"+drugList.getDrugName()+"】",drugsEnterprise.getName());
        return true;
    }


    /**
     * 更新销售机构药品信息
     *
     * @param saleDrugList
     * @return
     * @author houxr
     */
    @RpcService
    public SaleDrugListDTO updateSaleDrugList(SaleDrugList saleDrugList) {
        logger.info("修改销售机构药品服务[updateSaleDrugList]:" + JSONUtils.toString(saleDrugList));
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList target = saleDrugListDAO.get(saleDrugList.getOrganDrugId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugList.getOrganId());
        if (null == target) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "此药在该医院药品列表中不存在");
        } else {
            Integer oldStatus = target.getStatus();
            Integer newStatus = saleDrugList.getStatus();
            BeanUtils.map(saleDrugList, target);
            validateSaleDrugList(target);
            target.setSaleDrugCode(saleDrugList.getOrganDrugCode());
            target.setLastModify(new Date());
            target = saleDrugListDAO.update(target);
            DrugList drugList = drugListDAO.get(saleDrugList.getDrugId());
            if (drugList == null) {
                drugList = new DrugList();
            }
            if (!oldStatus.equals(newStatus)) {
                //禁用 启用
                String type = newStatus == 0 ? "禁用" : "启用";
                busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + drugsEnterprise.getName() + "】" + type + "【" + saleDrugList.getOrganDrugId() + " -" + drugList.getDrugName() + "】", drugsEnterprise.getName());
            } else {
                //更新
                busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + drugsEnterprise.getName() + "】更新药品【" + saleDrugList.getOrganDrugId()
                        + "-" + drugList.getDrugName() + "】", drugsEnterprise.getName());
            }
        }
        return ObjectCopyUtils.convert(target, SaleDrugListDTO.class);
    }

    /**
     * 批量删除药企药品数据
     * @param saleDrugList 前台传参集合
     */
    @RpcService
    public void deletesaleDrugListBySaleDrugLists(List<DrugListBean> saleDrugList) {
        if (saleDrugList.isEmpty()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        RemoteDrugService remoteDrugService = AppDomainContext.getBean("eh.remoteDrugService", RemoteDrugService.class);
        for (DrugListBean drugListBean : saleDrugList) {
            remoteDrugService.updateDrugList(drugListBean);

        }
    }

    /**
     * 一键禁用
     * @param organId 药企
     */
    @RpcService
    public void updateInvalidByOrganId(Integer organId) {
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        SaleDrugListDAO dao = DAOFactory.getDAO(SaleDrugListDAO.class);
        dao.updateInvalidByOrganId(organId);
    }

    /**
     * 一键删除
     * @param organId 药企
     */
    @RpcService
    public void deleteByOrganId(Integer organId) {
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(organId);
        if (ObjectUtils.isEmpty(drugsEnterprise)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企!");
        }
        SaleDrugListDAO dao = DAOFactory.getDAO(SaleDrugListDAO.class);
        dao.deleteByOrganId(organId);
        busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + urt.getUserName() + "】一键删除【" + drugsEnterprise.getName()
                +"】药品", drugsEnterprise.getName());
    }

    /**
     * 销售机构药品查询
     *
     * @param organId   机构
     * @param drugClass 药品分类
     * @param keyword   查询关键字:药品序号 or 药品名称 or 生产厂家 or 商品名称 or 批准文号
     * @param start
     * @param limit
     * @return
     * @author houxr
     */
    @Override
    public QueryResult<DrugListAndSaleDrugListDTO> querySaleDrugListByOrganIdAndKeyword(final Date startTime, final Date endTime,final Integer organId,
                                                                                        final String drugClass,
                                                                                        final String keyword, final Integer status,final Integer type,
                                                                                        final int start, final int limit) {
        if(organId == null){
            return null;
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        QueryResult result = saleDrugListDAO.querySaleDrugListByOrganIdAndKeyword(startTime,endTime,organId, drugClass, keyword, status,type, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }

    private List<DrugListAndSaleDrugListDTO> covertData(List<DrugListAndSaleDrugList> dbList) {
        List<DrugListAndSaleDrugListDTO> newList = Lists.newArrayList();
        DrugListAndSaleDrugListDTO backDTO;
        for (DrugListAndSaleDrugList daod : dbList) {
            backDTO = new DrugListAndSaleDrugListDTO();
            backDTO.setDrugList(ObjectCopyUtils.convert(daod.getDrugList(), DrugListBean.class));
            backDTO.setSaleDrugList(ObjectCopyUtils.convert(daod.getSaleDrugList(), SaleDrugListDTO.class));
            newList.add(backDTO);
        }

        return newList;
    }

    @RpcService
    public boolean checkDrugIntroduce(Integer drugId, Integer useTotalDose, Integer organId){
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        if (organDTO == null) {
            logger.info("机构不存在, organ:" + organId);
            return true;
        }
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                if (saleDrugList != null && saleDrugList.getInventory() != null && saleDrugList.getInventory().intValue() >= useTotalDose) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @RpcService
    public SaleDrugListDTO getByOrganIdAndDrugId(Integer enterprise, Integer drugId ){
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList=saleDrugListDAO.getByDrugIdAndOrganId(drugId,enterprise);
        //logger.info("getByOrganIdAndDrugId1111:" + saleDrugList);
        return ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class);
    }

    @Override
    public SaleDrugListDTO getByDrugId(Integer drugId ){
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList=saleDrugListDAO.get(drugId);
        //logger.info("getByOrganIdAndDrugId1111:" + saleDrugList);
        return ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class);
    }

    /**
     * 删除药企药品数据
     *
     */
    @RpcService
    public void deleteSaleDrugListByIds(List<Integer> ids) {
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        if (CollectionUtils.isEmpty(ids)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = saleDrugListDAO.get(ids.get(0));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(saleDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        StringBuilder msg = new StringBuilder();
        for(Integer id : ids){
            SaleDrugList saleDrugList1 = saleDrugListDAO.get(id);
            DrugList drugList = drugListDAO.get(saleDrugList1.getDrugId());
            if (drugList == null) {
                drugList = new DrugList();
            }
            saleDrugListDAO.remove(id);
            msg.append("【" + saleDrugList1.getOrganDrugId() + " -" + drugList.getDrugName() + "】");
        }
        busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + drugsEnterprise.getName() + "】删除" + msg.toString(), drugsEnterprise.getName());
    }

    @RpcService
    public void updateIntroduceByDepId(Integer depId, Integer introduce){
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        saleDrugListDAO.updateIntroduceByDepId(depId, introduce);
    }

    @RpcService
    public Long getCountByDrugId(int drugId){
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        Long result =  saleDrugListDAO.getCountByDrugId(drugId);
        return result == null ? 0 : result;
    }


}
