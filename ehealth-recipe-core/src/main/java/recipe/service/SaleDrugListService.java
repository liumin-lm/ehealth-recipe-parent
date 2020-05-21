package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListAndSaleDrugListDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.SaleDrugListDTO;
import com.ngari.recipe.drug.service.IDrugService;
import com.ngari.recipe.drug.service.ISaleDrugListService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import recipe.constant.ErrorCode;
import recipe.dao.*;
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

    private static Logger logger = Logger.getLogger(SaleDrugListService.class);

    private void validateSaleDrugList(SaleDrugList saleDrugList) {
        if (null == saleDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
//        if (StringUtils.isEmpty(saleDrugList.getOrganDrugCode())) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
//        }
//        if (null == saleDrugList.getRatePrice()) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "ratePrice is needed");
//        }
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == saleDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == saleDrugList.getPrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is needed");
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
        SaleDrugListDAO dao = DAOFactory.getDAO(SaleDrugListDAO.class);
        if (null == saleDrugList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleDrugList is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (!drugsEnterpriseDAO.exist(saleDrugList.getOrganId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugsEnterprise not exist");
        }
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (!drugListDAO.exist(saleDrugList.getDrugId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
        }

        //验证药品必要信息
        validateSaleDrugList(saleDrugList);
        saleDrugList.setCreateDt(new Date());
        saleDrugList.setStatus(1);
        dao.save(saleDrugList);
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
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList target = saleDrugListDAO.get(saleDrugList.getOrganDrugId());
        if (null == target) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "此药在该医院药品列表中不存在");
        } else {
            BeanUtils.map(saleDrugList, target);
            validateSaleDrugList(target);
            target.setLastModify(new Date());
            target = saleDrugListDAO.update(target);
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
    @RpcService
    public QueryResult<DrugListAndSaleDrugListDTO> querySaleDrugListByOrganIdAndKeyword(final Date startTime, final Date endTime,final Integer organId,
                                                                                        final String drugClass,
                                                                                        final String keyword, final Integer status,
                                                                                        final int start, final int limit) {
        if(organId == null){
            return null;
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        QueryResult result = saleDrugListDAO.querySaleDrugListByOrganIdAndKeyword(startTime,endTime,organId, drugClass, keyword, status, start, limit);
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
        return ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class);
    }
}
