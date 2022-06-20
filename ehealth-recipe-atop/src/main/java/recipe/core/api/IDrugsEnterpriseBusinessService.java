package recipe.core.api;

import com.ngari.recipe.drugdistributionprice.model.DrugDistributionPriceBean;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressDTO;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.EnterpriseDecoctionAddress;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import ctd.persistence.bean.QueryResult;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

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

    /**
     * 新增药企煎法地址
     * @param enterpriseDecoctionAddressReq
     */
    void addEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq);

    /**
     * 查询药企煎法地址
     * @param enterpriseDecoctionAddressReq
     * @return
     */
    List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq);

    /**
     * 根据药企id获取机构列表
     * @param enterpriseId
     * @return
     */
    List<OrganAndDrugsepRelation> findOrganAndDrugsepRelationBean(Integer enterpriseId);

    /**
     * 获取机构药企煎法 信息
     * @param enterpriseId
     * @param organId
     * @return
     */
    List<EnterpriseDecoctionList> findEnterpriseDecoctionList(Integer enterpriseId, Integer organId);

    /**
     * 校验机构药企煎法 地址
     * @param checkAddressReq
     * @return
     */
    CheckAddressRes checkEnterpriseDecoctionAddress(CheckAddressReq checkAddressReq);

    /**
     * 重新推送药企
     * @param recipeId
     * @return
     */
    boolean retryPushRecipeOrder(Integer recipeId);

    boolean pushDrugDispenser(Integer recipeId);

    /**
     * 药企推送失败 的处方重新推送定时任务
     */
    void rePushRecipeToDrugsEnterprise();

    /**
     * 更新药企信息
     * @param drugsEnterpriseVO
     * @return
     */
    boolean updateDrugEnterprise(DrugsEnterpriseVO drugsEnterpriseVO);

    /**
     * 获取地址是否可以配送
     * @param checkAddressVo
     * @return
     */
    Boolean checkSendAddress(CheckAddressVo checkAddressVo);

    /**
     * 药企确认订单
     * @param enterpriseConfirmOrderVO
     * @return
     */
    EnterpriseResultBean confirmOrder (EnterpriseConfirmOrderVO enterpriseConfirmOrderVO);

    /**
     * 准备发货
     * @param enterpriseSendOrderVO
     * @return
     */
    EnterpriseResultBean readySendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);

    /**
     * 订单发货接口
     * @param enterpriseSendOrderVO
     * @return
     */
    EnterpriseResultBean sendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);

    /**
     * 订单完成接口
     * @param enterpriseSendOrderVO
     * @return
     */
    EnterpriseResultBean finishOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);
}
