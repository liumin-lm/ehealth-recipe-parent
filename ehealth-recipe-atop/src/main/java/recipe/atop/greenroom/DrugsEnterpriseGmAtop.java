package recipe.atop.greenroom;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressDTO;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.EnterpriseDecoctionAddress;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.organdrugsep.model.OrganAndDrugsepRelationBean;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description： 运营平台药企相关
 * @author： yinsheng
 * @date： 2021-12-08 9:45
 */
@RpcBean(value = "drugsEnterpriseGmAtop")
public class DrugsEnterpriseGmAtop extends BaseAtop {

    @Autowired
    private IDrugsEnterpriseBusinessService enterpriseBusinessService;

    @Autowired
    private IDrugsEnterpriseBusinessService drugsEnterpriseBusinessService;


    /**
     * 根据药企机构查询煎法
     *
     * @param enterpriseId
     */
    @RpcService
    public List<EnterpriseDecoctionList> findEnterpriseDecoctionList(Integer enterpriseId,Integer organId) {
        validateAtop(enterpriseId,organId);
        List<EnterpriseDecoctionList> list = enterpriseBusinessService.findEnterpriseDecoctionList(enterpriseId,organId);
        return list;
    }

    /**
     * 根据药企查询机构
     *
     * @param enterpriseId
     */
    @RpcService
    public List<OrganAndDrugsepRelationBean> findOrganAndDrugsepRelationBean(Integer enterpriseId) {
        validateAtop(enterpriseId);
        List<OrganAndDrugsepRelation> organAndDrugsepRelations = enterpriseBusinessService.findOrganAndDrugsepRelationBean(enterpriseId);
        return BeanCopyUtils.copyList(organAndDrugsepRelations,OrganAndDrugsepRelationBean::new);
    }

    /**
     * 新增药企煎法地址
     *
     * @param enterpriseDecoctionAddressReq
     */
    @RpcService
    public List<EnterpriseDecoctionAddressDTO> addEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        validateAtop(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());
        enterpriseBusinessService.addEnterpriseDecoctionAddressList(enterpriseDecoctionAddressReq);
        return enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS();
    }

    /**
     * 查询药企煎法地址
     *
     * @param enterpriseDecoctionAddressReq
     */
    @RpcService
    public List<EnterpriseDecoctionAddressDTO> findEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        validateAtop(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());
        List<EnterpriseDecoctionAddress> list = enterpriseBusinessService.findEnterpriseDecoctionAddressList(enterpriseDecoctionAddressReq);
        return BeanCopyUtils.copyList(list, EnterpriseDecoctionAddressDTO::new);
    }

    /**
     * 查询药企列表
     *
     * @param organEnterpriseRelationVo
     * @return
     */
    @RpcService
    public OrganEnterpriseRelationVo drugsEnterpriseLimit(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        validateAtop(organEnterpriseRelationVo.getType(), organEnterpriseRelationVo.getStart(), organEnterpriseRelationVo.getLimit());
        organEnterpriseRelationVo.setStart((organEnterpriseRelationVo.getStart() - 1) * organEnterpriseRelationVo.getLimit());
        QueryResult<DrugsEnterprise> queryResult = enterpriseBusinessService.drugsEnterpriseLimit(organEnterpriseRelationVo);
        if (null == queryResult || ValidateUtil.longIsEmpty(queryResult.getTotal())) {
            organEnterpriseRelationVo.setTotal(0);
            return organEnterpriseRelationVo;
        }
        List<DrugsEnterpriseBean> drugsEnterpriseList = ObjectCopyUtils.convert(queryResult.getItems(), DrugsEnterpriseBean.class);
        List<PharmacyVO> pharmacyList = enterpriseBusinessService.pharmacy();
        Map<Integer, PharmacyVO> map = pharmacyList.stream().collect(Collectors.toMap(PharmacyVO::getDrugsenterpriseId, a -> a, (k1, k2) -> k1));
        drugsEnterpriseList.forEach(a -> {
            if (ValidateUtil.integerIsEmpty(a.getCreateType())) {
                a.setPharmacy(map.get(a.getId()));
            }
        });
        organEnterpriseRelationVo.setDrugsEnterpriseList(drugsEnterpriseList);
        organEnterpriseRelationVo.setTotal((int) queryResult.getTotal());
        return organEnterpriseRelationVo;
    }

    /**
     * 根据名称查询药企是否存在
     *
     * @param name 药企名称
     * @return 是否存在
     */
    @RpcService
    public Boolean existEnterpriseByName(String name) {
        validateAtop(name);
        return enterpriseBusinessService.existEnterpriseByName(name);
    }

    /**
     * 保存机构药企关联关系
     *
     * @param organEnterpriseRelationVo
     */
    @RpcService
    public void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        validateAtop(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        // 医院配送与药企配送只能2选1 到院自取与到店自取只能2选1
        RecipeSupportGiveModeEnum.checkOrganEnterpriseRelationGiveModeType(organEnterpriseRelationVo.getGiveModeTypes());
        enterpriseBusinessService.saveOrganEnterpriseRelation(organEnterpriseRelationVo);
    }

    /**
     * 保存机构药企关联关系
     *
     * @param organEnterpriseRelationVo
     */
    @RpcService
    public OrganEnterpriseRelationVo getOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        validateAtop(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        return enterpriseBusinessService.getOrganEnterpriseRelation(organEnterpriseRelationVo);
    }

    /**
     * 查询药企机构销售配置
     *
     * @param organId           机构id
     * @param drugsEnterpriseId 药企id
     */
    @RpcService
    public OrganDrugsSaleConfigVo findOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId) {
        validateAtop(drugsEnterpriseId);
        OrganDrugsSaleConfig organDrugsSaleConfig = enterpriseBusinessService.getOrganDrugsSaleConfig(drugsEnterpriseId);
        if (Objects.isNull(organDrugsSaleConfig)) {
            return null;
        }
        OrganDrugsSaleConfigVo organDrugsSaleConfigVo = new OrganDrugsSaleConfigVo();
        BeanUtils.copyProperties(organDrugsSaleConfig, organDrugsSaleConfigVo);
        return organDrugsSaleConfigVo;
    }

    /**
     * 保存机构药企销售配置
     *
     * @param organDrugsSaleConfigVo
     */
    @RpcService
    public OrganDrugsSaleConfigVo saveOrganDrugsSaleConfig(OrganDrugsSaleConfigVo organDrugsSaleConfigVo) {
        validateAtop(organDrugsSaleConfigVo.getDrugsEnterpriseId());
        enterpriseBusinessService.saveOrganDrugsSaleConfig(organDrugsSaleConfigVo);
        return organDrugsSaleConfigVo;
    }

    /**
     * 重试处方推送
     *
     * @param recipeId
     */
    @RpcService
    public boolean retryPushRecipeOrder(Integer recipeId){
        validateAtop(recipeId);
        return enterpriseBusinessService.retryPushRecipeOrder(recipeId);
    }

    /**
     * 更新药企信息
     * @param drugsEnterpriseVO
     * @return
     */
    @RpcService
    public boolean updateDrugEnterprise(DrugsEnterpriseVO drugsEnterpriseVO){
        validateAtop(drugsEnterpriseVO, drugsEnterpriseVO.getId());
        return enterpriseBusinessService.updateDrugEnterprise(drugsEnterpriseVO);
    }

    /**
     * 运营平台调用发药机发药
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public Boolean pushDrugDispenser(Integer recipeId) {
        return drugsEnterpriseBusinessService.pushDrugDispenser(recipeId);
    }
}
