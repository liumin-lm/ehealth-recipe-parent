package recipe.service;

import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.util.ApplicationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author houxr
 * @date 2016/8/3.
 */
@RpcBean("organAndDrugsepRelationService")
public class OrganAndDrugsepRelationService {

    private static final Log logger = LogFactory.getLog(OrganAndDrugsepRelationService.class);


    /**
     * 根据机构添加药企
     *
     * @param organId
     * @param entpriseIds
     * @return
     */
    @RpcService
    public List<OrganAndDrugsepRelation> addDrugEntRelationByOrganIdAndEntIds(Integer organId, List<Integer> entpriseIds) {
        logger.info("机构药企维护:[organId:" + organId + ",entpriseIds:" + JSONUtils.toString(entpriseIds));
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is empty!");
        }
        if (ObjectUtils.isEmpty(entpriseIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "EntpriseIds is empty!");
        }
        IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);
        OrganAndDrugsepRelationDAO relationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        OrganBean organ = iOrganService.get(organId);
        if (null == organ) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该机构不存在!");
        }
        List<OrganAndDrugsepRelation> retList = new ArrayList<OrganAndDrugsepRelation>();
        for (Integer entId : entpriseIds) {
            OrganAndDrugsepRelation relation = new OrganAndDrugsepRelation();
            relation.setOrganId(organId);
            relation.setDrugsEnterpriseId(entId);
            relation = relationDAO.save(relation);
            retList.add(relation);
        }
        return retList;
    }

    /**
     * 根据机构删除对应药企
     *
     * @param organId
     * @param entId
     */
    @RpcService
    public void deleteDrugEntRelationByOrganIdAndEntId(Integer organId, Integer entId) {
        logger.info("机构药企维护删除:[organId:" + organId + ",drugsEnterpriseId:" + entId);
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is empty!");
        }
        if (ObjectUtils.isEmpty(entId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsEnterpriseId is empty!");
        }
        OrganAndDrugsepRelationDAO relationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        OrganAndDrugsepRelation drugsepRelation = relationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, entId);
        relationDAO.remove(drugsepRelation.getId());
    }


    /**
     * 根据机构获取对应的药企列表
     *
     * @param organId 医院内码
     * @param status  药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterprise> findDrugsEnterpriseByOrganId(final Integer organId, final Integer status) {
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is empty!");
        }
        if (ObjectUtils.isEmpty(status)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "status is empty!");
        }
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        return organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, status);
    }

}
