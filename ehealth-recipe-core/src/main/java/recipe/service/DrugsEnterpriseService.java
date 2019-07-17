package recipe.service;

import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganConfigService;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.serviceprovider.BaseService;

import java.util.List;

/**
 * 药企相关接口
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/2.
 */
@RpcBean("drugsEnterpriseService")
public class DrugsEnterpriseService extends BaseService<DrugsEnterpriseBean>{

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugsEnterpriseService.class);

    /**
     * 有效药企查询 status为1
     *
     * @param status 药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseBean> findDrugsEnterpriseByStatus(final Integer status) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> list = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(status);
        return getList(list, DrugsEnterpriseBean.class);
    }

    /**
     * 新建药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean addDrugsEnterprise(final DrugsEnterprise drugsEnterprise) {
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(drugsEnterprise.getName());
        if (drugsEnterpriseList.size() != 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise exist!");
        }
        DrugsEnterprise newDrugsEnterprise = drugsEnterpriseDAO.save(drugsEnterprise);
        return getBean(newDrugsEnterprise, DrugsEnterpriseBean.class);
    }


    /**
     * 更新药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean updateDrugsEnterprise(final DrugsEnterprise drugsEnterprise) {
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        LOGGER.info(JSONUtils.toString(drugsEnterprise));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise target = drugsEnterpriseDAO.get(drugsEnterprise.getId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "DrugsEnterprise not exist!");
        }
        BeanUtils.map(drugsEnterprise, target);
        target = drugsEnterpriseDAO.update(target);
        return getBean(target, DrugsEnterpriseBean.class);
    }

    /**
     * 根据药企名称分页查询药企
     *
     * @param name  药企名称
     * @param start 分页起始位置
     * @param limit 每页条数
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public QueryResult<DrugsEnterpriseBean> queryDrugsEnterpriseByStartAndLimit(final String name, final int start, final int limit) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        QueryResult result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByStartAndLimit(name, start, limit);
        List<DrugsEnterpriseBean> list = getList(result.getItems(), DrugsEnterpriseBean.class);
        result.setItems(list);
        return result;
    }

    @RpcService
    public List<DrugsEnterpriseBean> findByOrganId(Integer organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return getList(drugsEnterpriseDAO.findByOrganId(organId), DrugsEnterpriseBean.class);
    }

    /**
     * 检查开处方是否需要进行药企库存校验
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    @RpcService
    public boolean checkEnterprise(Integer organId){
        OrganConfigService organConfigService = BasicAPI.getService(OrganConfigService.class);
        Integer checkEnterprise = organConfigService.getCheckEnterpriseByOrganId(organId);
        if(Integer.valueOf(0).equals(checkEnterprise)){
            return false;
        }

        return true;
    }

    /**
     * 推送医院补充库存药企
     * 流转处方推送
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosInteriorSupport(Integer recipeId, Integer organId){
        //武昌需求处理，推送无库存的处方至医院补充库存药企||流转处方推送
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndHosInteriorSupport(organId);
        if(CollectionUtils.isNotEmpty(enterpriseList)){
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }

    /**
     * 流转处方推送药企
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosTransferSupport(Integer recipeId, Integer organId){
        //推送流转处方至医院指定取药药企
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(recipe.getPayMode());
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndPayModeSupport(organId,payModeSupport);
        if(CollectionUtils.isNotEmpty(enterpriseList)){
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }
}
