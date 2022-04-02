package recipe.core.api;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import ctd.persistence.bean.QueryResult;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;

import java.util.List;

/**
 * 药企相关
 */
public interface IDrugsEnterpriseBusinessService {

    /**
     * 根据名称查询药企是否存在
     * @param name 药企名称
     * @return 是否存在
     */
    Boolean existEnterpriseByName(String name);

    /**
     * 保存机构药企关联关系
     * @param organEnterpriseRelationVo
     */
    void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo);


    /**
     * 保存药企机构销售配置
     * @param organDrugsSaleConfigVo
     */
    void saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo);

    /**
     * 获取药企流转配置
     * @param organEnterpriseRelationVo
     * @return
     */
    OrganEnterpriseRelationVo getOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo);

    /**
     * 查询药企机构销售配置
     *
     * @param drugsEnterpriseId
     * @return
     */
    OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer drugsEnterpriseId);

    /**
     * 查询药企列表
     *
     * @param organEnterpriseRelationVo
     * @return
     */
    QueryResult<DrugsEnterprise> drugsEnterpriseLimit(OrganEnterpriseRelationVo organEnterpriseRelationVo);

    /**
     * 查询药店数据
     *
     * @return
     */
    List<PharmacyVO> pharmacy();
}
