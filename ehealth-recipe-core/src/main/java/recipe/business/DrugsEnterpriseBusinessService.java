package recipe.business;

import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.greenroom.IDrugsEnterpriseBusinessService;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.OrganDrugsSaleConfigDAO;
import recipe.manager.EnterpriseManager;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;

import java.util.List;
import java.util.Objects;

/**
 * @description： 药企 业务类
 * @author： yinsheng
 * @date： 2021-12-08 18:58
 */
@Service
public class DrugsEnterpriseBusinessService extends BaseService implements IDrugsEnterpriseBusinessService {
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Autowired
    private OrganDrugsSaleConfigDAO organDrugsSaleConfigDAO;
    @Override
    public Boolean existEnterpriseByName(String name) {
        List<DrugsEnterprise> drugsEnterprises = enterpriseManager.findAllDrugsEnterpriseByName(name);
        if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
            return true;
        }
        return false;
    }

    @Override
    public void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        logger.info("DrugsEnterpriseBusinessService saveOrganEnterpriseRelation organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        if(Objects.isNull(relation)){
            throw new DAOException("机构药企关联关系不存在");
        }
        String join = StringUtils.join(organEnterpriseRelationVo.getGiveModeTypes(), ",");
        relation.setDrugsEnterpriseSupportGiveMode(join);
        organAndDrugsepRelationDAO.updateNonNullFieldByPrimaryKey(relation);
    }

    @Override
    public OrganDrugsSaleConfig findOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId) {
        return organDrugsSaleConfigDAO.findByOrganIdAndEnterpriseId(organId,drugsEnterpriseId);
    }

    @Override
    public void saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo) {
        OrganDrugsSaleConfig organDrugsSaleConfig = new OrganDrugsSaleConfig();
        BeanUtils.copyProperties(organDrugsSaleConfigVo,organDrugsSaleConfig);
        enterpriseManager.saveOrganDrugsSaleConfig(organDrugsSaleConfig);
    }
}
