package recipe.core.api.greenroom;

import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;

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
     * @param drugsEnterpriseId
     * @return
     */
    OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer drugsEnterpriseId);
}
