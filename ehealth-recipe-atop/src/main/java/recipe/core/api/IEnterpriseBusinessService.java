package recipe.core.api;

import com.ngari.recipe.drugsenterprise.model.*;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.EnterpriseDecoctionAddress;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.bean.QueryResult;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;
import recipe.vo.patient.*;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.CheckOrderAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDrugVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

import java.util.List;
import java.util.Map;

/**
 * 药企相关
 */
public interface IEnterpriseBusinessService {

    /**
     * 根据名称查询药企是否存在
     *
     * @param name 药企名称
     * @return 是否存在
     */
    Boolean existEnterpriseByName(String name);

    /**
     * 查询药企是否存在于流转药企中
     * @param organId
     * @param depId
     * @return
     */
    Boolean existEnterpriseByOrganIdAndDepId(Integer organId, Integer depId);

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
     * 根据机构id获取流转关系列表
     * @param organId
     * @return
     */
    List<OrganAndDrugsepRelation> findOrganAndDrugsDepRelationBeanByOrganId(Integer organId);

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

    /**
     * 根据订单id调用发药机接口
     * @param orderId
     * @return
     */
    Boolean pushDrugDispenserByOrder(Integer orderId);

    /**
     * 更新药企的优先级
     * @param organId
     * @param depId
     * @param level
     * @return
     */
    Boolean updateEnterprisePriorityLevel(Integer organId, Integer depId, Integer level);

    /**
     * 获取药企配送地址以及费用
     * @param enterpriseId
     * @return
     */
    List<EnterpriseAddressAndPrice> findEnterpriseAddressAndPrice(Integer enterpriseId,String area);

    /**
     * 获取有配置的省
     * @param enterpriseId
     * @return
     */
    List<EnterpriseAddressAndPrice> findEnterpriseAddressProvince(Integer enterpriseId);

    /**
     * 药企药品信息同步接口
     *
     * @param enterpriseDrugVOList
     * @return
     */
    EnterpriseResultBean renewDrugInfo(List<EnterpriseDrugVO> enterpriseDrugVOList);


    /**
     * 机构药企销售配置
     *
     * @param organId
     * @param drugsEnterpriseId
     * @return
     */
    OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId);


    /**
     * 获取药企配送的站点
     *
     * @param medicineStationVO 取药站点的信息
     * @return 可以取药站点的列表
     */
    List<MedicineStationVO> getMedicineStationList(MedicineStationVO medicineStationVO);

    OrganDrugsSaleConfig getOrganDrugsSaleConfigOfPatient(Integer organId, Integer drugsEnterpriseId);

    /**
     * 修改药企支持的配送地址
     *
     * @param enterpriseAddressDTOS
     */
    void updateEnterpriseAddressAndPrice(List<EnterpriseAddressDTO> enterpriseAddressDTOS);

    /**
     * 清除当前区域下的所有配送信息
     * @param enterpriseId
     * @param area
     */
    void cancelEnterpriseAddress(Integer enterpriseId, String area);

    /**
     * 清除当前煎法下的所有配送信息
     * @param enterpriseDecoctionAddressReq
     */
    void cancelEnterpriseDecoctionAddress(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq);

    /**
     * 端校验地址入参
     * @param checkAddressVo
     * @return
     */
    Integer checkSendAddressForOrder(CheckOrderAddressVo checkAddressVo);

    /**
     * 腹透液配送时间获取
     *
     * @param ftySendTimeREQ
     * @return
     */
    List<String> getFTYSendTime(FTYSendTimeReq ftySendTimeREQ);

    /**
     * 端多个药企获取配送地址 是否可配送
     *
     * @param checkOrderAddressVo
     * @return
     */
    Integer checkSendAddressForEnterprises(CheckOrderAddressVo checkOrderAddressVo);

    /**
     * 根据药企id查询药企
     *
     * @param ids
     * @return
     */
    Map<Integer, DrugsEnterprise> findDrugsEnterpriseByIds(List<Integer> ids);

    /**
     * 根据机构获取 配置下的药企+ 到院自取的机构 返回前端列表
     *
     * @param organId
     * @return
     */
    List<EnterpriseStock> enterprisesList(Integer organId);

    /**
     * 查询药企
     * @return
     */
    List<DrugsEnterpriseVO> findDrugEnterprise();

    /**
     * 推送失败订单提醒任务
     */
    void pushFailOrderNotify();

    /**
     * 获取销售配置
     * @param findOrganDrugsSaleConfigResVo
     * @return
     */
    OrganDrugsSaleConfig getOrganDrugsSaleConfigV1(FindOrganDrugsSaleConfigResVo findOrganDrugsSaleConfigResVo);

    /**
     * 第三方药企更新配送地址和配送费用
     *
     * @param enterpriseAddressList
     * @return
     */
    Boolean setEnterpriseAddressAndPrice(List<EnterpriseAddressVO> enterpriseAddressList);

    /**
     * 与校验成功- 互联网 使用 存储his指定药企
     *
     * @param doSignRecipe
     * @param recipeBean
     */
    void checkRecipeGiveDeliveryMsg(DoSignRecipeDTO doSignRecipe, RecipeBean recipeBean);
}
