package recipe.business;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.entity.Pharmacy;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.OrganDrugsSaleConfigDAO;
import recipe.manager.EnterpriseManager;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (Objects.isNull(relation)) {
            throw new DAOException("机构药企关联关系不存在");
        }
        String join = StringUtils.join(organEnterpriseRelationVo.getGiveModeTypes(), ",");
        relation.setDrugsEnterpriseSupportGiveMode(join);
        organAndDrugsepRelationDAO.updateNonNullFieldByPrimaryKey(relation);
    }


    @Override
    public void saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo) {
        OrganDrugsSaleConfig organDrugsSaleConfig = new OrganDrugsSaleConfig();
        BeanUtils.copyProperties(organDrugsSaleConfigVo, organDrugsSaleConfig);
        enterpriseManager.saveOrganDrugsSaleConfig(organDrugsSaleConfig);
    }

    @Override
    public OrganEnterpriseRelationVo getOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        logger.info("DrugsEnterpriseBusinessService getOrganEnterpriseRelation req organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        if (Objects.isNull(relation)) {
            throw new DAOException("请到机构配置关联药企");
        }
        List<Integer> list = Lists.newArrayList();
        if (StringUtils.isNotEmpty(relation.getDrugsEnterpriseSupportGiveMode())) {
            String[] split = relation.getDrugsEnterpriseSupportGiveMode().split(",");
            for (String s : split) {
                list.add(Integer.valueOf(s));
            }
        }
        organEnterpriseRelationVo.setGiveModeTypes(list);
        logger.info("DrugsEnterpriseBusinessService getOrganEnterpriseRelation res organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        return organEnterpriseRelationVo;
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer drugsEnterpriseId) {
        OrganDrugsSaleConfig byOrganIdAndEnterpriseId = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterpriseId);

        return byOrganIdAndEnterpriseId;
    }

    @Override
    public QueryResult<DrugsEnterprise> drugsEnterpriseLimit(OrganEnterpriseRelationVo vo) {
        List<Integer> drugsEnterpriseIds = null;
        if (1 == vo.getType()) {
            List<DrugsEnterprise> drugsEnterpriseList = enterpriseManager.drugsEnterpriseByOrganId(vo.getOrganId());
            if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
                return null;
            }
            drugsEnterpriseIds = drugsEnterpriseList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        }
        return enterpriseManager.drugsEnterpriseLimit(vo.getName(), vo.getCreateType(), vo.getOrganId(), vo.getStart(), vo.getLimit(), drugsEnterpriseIds);
    }

    @Override
    public List<PharmacyVO> pharmacy() {
        List<Pharmacy> list = enterpriseManager.pharmacy();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return ObjectCopyUtils.convert(list, PharmacyVO.class);
    }
}
