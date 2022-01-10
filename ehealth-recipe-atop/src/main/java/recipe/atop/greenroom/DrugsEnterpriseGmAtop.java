package recipe.atop.greenroom;

import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.greenroom.IDrugsEnterpriseBusinessService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;

import java.util.List;

/**
 * @description： 运营平台药企相关
 * @author： yinsheng
 * @date： 2021-12-08 9:45
 */
@RpcBean(value = "drugsEnterpriseGmAtop")
public class DrugsEnterpriseGmAtop extends BaseAtop {

    @Autowired
    private IDrugsEnterpriseBusinessService enterpriseBusinessService;

    /**
     * 根据名称查询药企是否存在
     * @param name 药企名称
     * @return 是否存在
     */
    @RpcService
    public Boolean existEnterpriseByName(String name){
        validateAtop(name);
        return enterpriseBusinessService.existEnterpriseByName(name);
    }

    /**
     * 保存机构药企关联关系
     * @param organEnterpriseRelationVo
     */
    @RpcService
    public void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo){
        validateAtop(organEnterpriseRelationVo.getOrganId(),organEnterpriseRelationVo.getDrugsEnterpriseId(),organEnterpriseRelationVo.getGiveModeTypes());
        // 医院配送与药企配送只能2选1 到院自取与到店自取只能2选1
        RecipeSupportGiveModeEnum.checkOrganEnterpriseRelationGiveModeType(organEnterpriseRelationVo.getGiveModeTypes());
        enterpriseBusinessService.saveOrganEnterpriseRelation(organEnterpriseRelationVo);
    }

    /**
     * 查询药企机构销售配置
     * @param organId 机构id
     * @param drugsEnterpriseId 药企id
     */
    @RpcService
    public OrganDrugsSaleConfigVo findOrganDrugsSaleConfig(Integer organId,Integer drugsEnterpriseId){
        validateAtop(organId,drugsEnterpriseId);
        OrganDrugsSaleConfig organDrugsSaleConfig = enterpriseBusinessService.findOrganDrugsSaleConfig(organId, drugsEnterpriseId);
        OrganDrugsSaleConfigVo organDrugsSaleConfigVo = new OrganDrugsSaleConfigVo();
        BeanUtils.copyProperties(organDrugsSaleConfig,organDrugsSaleConfigVo);
        return organDrugsSaleConfigVo;
    }

    /**
     * 保存机构药企销售配置
     * @param organDrugsSaleConfigVo
     */
    @RpcService
    public OrganDrugsSaleConfigVo saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo){
        validateAtop(organDrugsSaleConfigVo.getOrganId(),organDrugsSaleConfigVo.getDrugsEnterpriseId());
        enterpriseBusinessService.saveOrganDrugsSaleConfig(organDrugsSaleConfigVo);
        return organDrugsSaleConfigVo;
    }
}
