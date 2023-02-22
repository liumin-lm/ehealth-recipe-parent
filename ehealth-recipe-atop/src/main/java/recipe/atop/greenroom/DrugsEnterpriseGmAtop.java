package recipe.atop.greenroom;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.drugsenterprise.model.*;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.EnterpriseDecoctionAddress;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.organdrugsep.model.OrganAndDrugsepRelationBean;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 运营平台药企相关
 * @author： yinsheng
 * @date： 2021-12-08 9:45
 */
@RpcBean(value = "drugsEnterpriseGmAtop")
public class DrugsEnterpriseGmAtop extends BaseAtop {

    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;


    /**
     * 清除当前煎法下的所有配送信息
     *
     * @param enterpriseDecoctionAddressReq
     */
    @RpcService
    public void cancelEnterpriseDecoctionAddress(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        validateAtop(enterpriseDecoctionAddressReq, enterpriseDecoctionAddressReq.getDecoctionId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getOrganId());
        enterpriseBusinessService.cancelEnterpriseDecoctionAddress(enterpriseDecoctionAddressReq);
    }

    /**
     * 清除当前区域下的所有配送信息
     *
     * @param enterpriseId
     * @param area
     */
    @RpcService
    public void cancelEnterpriseAddress(Integer enterpriseId, String area) {
        validateAtop(enterpriseId, area);
        enterpriseBusinessService.cancelEnterpriseAddress(enterpriseId, area);
    }

    /**
     * 根据药企获取药企配送地址快递费
     *
     * @param enterpriseAddressDTOS
     */
    @RpcService(timeout = 60)
    public void updateEnterpriseAddressAndPrice(List<EnterpriseAddressDTO> enterpriseAddressDTOS) {
        validateAtop(enterpriseAddressDTOS);
        enterpriseBusinessService.updateEnterpriseAddressAndPrice(enterpriseAddressDTOS);
    }

    /**
     * 根据药企获取药企配送地址快递费
     *
     * @param enterpriseId
     */
//    @RpcService
//    public DrugDistributionPriceBean findEnterpriseAddressFeeList(Integer enterpriseId,String addrArea) {
//        validateAtop(enterpriseId);
//        DrugDistributionPriceBean enterpriseIdAndAddrArea = drugDistributionPriceService.getByEnterpriseIdAndAddrArea(enterpriseId, addrArea);
//        return enterpriseIdAndAddrArea;
//    }

    /**
     * 添加药企配送地址快递费
     *
     * @param list
     */
//    @RpcService
//    public List<DrugDistributionPriceBean> addEnterpriseAddressFeeList(List<DrugDistributionPriceBean> list) {
//        validateAtop(list);
//        drugDistributionPriceService.savePriceList(list);
//        return list;
//    }

    /**
     * 根据药企机构查询煎法
     *
     * @param enterpriseId
     */
    @RpcService
    public List<EnterpriseDecoctionList> findEnterpriseDecoctionList(Integer enterpriseId,Integer organId) {
        validateAtop(enterpriseId);
        if (Objects.isNull(organId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        List<EnterpriseDecoctionList> list = enterpriseBusinessService.findEnterpriseDecoctionList(enterpriseId,organId);
        return list;
    }

    /**
     * 获取药企的配送地址以及费用
     *
     * @param enterpriseId
     */
    @RpcService
    public List<EnterpriseAddressAndPrice> findEnterpriseAddressAndPrice(Integer enterpriseId,String area) {
        validateAtop(enterpriseId);
        List<EnterpriseAddressAndPrice> list = enterpriseBusinessService.findEnterpriseAddressAndPrice(enterpriseId,area);
        return list;
    }

    /**
     * 获取药企的配送地址以及费用
     *
     * @param enterpriseId
     */
    @RpcService
    public List<EnterpriseAddressAndPrice> findEnterpriseAddressProvince(Integer enterpriseId) {
        validateAtop(enterpriseId);
        List<EnterpriseAddressAndPrice> list = enterpriseBusinessService.findEnterpriseAddressProvince(enterpriseId);
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
        Integer organId = organEnterpriseRelationVo.getOrganId();
        //越权校验
        if(organId !=null){
            if (organId == 0) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "纳里健康下无法添加流转药企");
            }
            isAuthorisedOrgan(organEnterpriseRelationVo.getOrganId());
        }

        QueryResult<DrugsEnterprise> queryResult = enterpriseBusinessService.drugsEnterpriseLimit(organEnterpriseRelationVo);
        if (null == queryResult || ValidateUtil.longIsEmpty(queryResult.getTotal())) {
            organEnterpriseRelationVo.setTotal(0);
            return organEnterpriseRelationVo;
        }
        List<DrugsEnterpriseBean> drugsEnterpriseList = ObjectCopyUtils.convert(queryResult.getItems(), DrugsEnterpriseBean.class);
        List<OrganAndDrugsepRelation> organAndDrugsDepRelationList = enterpriseBusinessService.findOrganAndDrugsDepRelationBeanByOrganId(organId);
        Map<Integer, OrganAndDrugsepRelation> organAndDrugsDepRelationMap = organAndDrugsDepRelationList.stream().collect(Collectors.toMap(OrganAndDrugsepRelation::getDrugsEnterpriseId,a->a,(k1,k2)->k1));
        logger.info("drugsEnterpriseLimit organAndDrugsDepRelationMap:{}", JSON.toJSONString(organAndDrugsDepRelationMap));
        List<PharmacyVO> pharmacyList = enterpriseBusinessService.pharmacy();
        Map<Integer, PharmacyVO> map = pharmacyList.stream().collect(Collectors.toMap(PharmacyVO::getDrugsenterpriseId, a -> a, (k1, k2) -> k1));
        drugsEnterpriseList.forEach(a -> {
            if (ValidateUtil.integerIsEmpty(a.getCreateType())) {
                a.setPharmacy(map.get(a.getId()));
            }
            if (null == organAndDrugsDepRelationMap.get(a.getId())) {
                return;
            }
            a.setPriorityLevel(organAndDrugsDepRelationMap.get(a.getId()).getPriorityLevel());
            OrganAndDrugsepRelation organAndDrugsepRelation = organAndDrugsDepRelationMap.get(a.getId());
            String drugsEnterpriseSupportGiveMode = organAndDrugsepRelation.getDrugsEnterpriseSupportGiveMode();
            List<String> drugsEnterpriseSupportGiveModeName = new ArrayList<>();
            if (StringUtils.isNotEmpty(drugsEnterpriseSupportGiveMode)) {
                Arrays.asList(drugsEnterpriseSupportGiveMode.split(",")).forEach(giveMode->{
                    drugsEnterpriseSupportGiveModeName.add(RecipeSupportGiveModeEnum.getNameByType(Integer.parseInt(giveMode)));
                });
                a.setSupportGiveModeNameList(drugsEnterpriseSupportGiveModeName);
            }
        });
        drugsEnterpriseList = drugsEnterpriseList.stream().sorted(Comparator.comparing(drugsEnterpriseBean -> Optional.ofNullable(drugsEnterpriseBean.getPriorityLevel()).orElse(0),Comparator.reverseOrder())).collect(Collectors.toList());
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
     * 查询药企
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseVO> findDrugEnterprise(){
        List<DrugsEnterpriseVO> drugsEnterpriseVOList = enterpriseBusinessService.findDrugEnterprise();
        List<DrugsEnterpriseVO> result = new ArrayList<>();
        drugsEnterpriseVOList.forEach(drugsEnterpriseVO -> {
            DrugsEnterpriseVO drugsEnterprise = new DrugsEnterpriseVO();
            drugsEnterprise.setId(drugsEnterpriseVO.getId());
            drugsEnterprise.setName(drugsEnterpriseVO.getName());
            result.add(drugsEnterprise);
        });
        return result;
    }

    /**
     * 查询药企是否存在于流转药企中
     * @param organId
     * @param depId
     * @return
     */
    @RpcService
    public Boolean existEnterpriseByOrganIdAndDepId(Integer organId, Integer depId){
        return enterpriseBusinessService.existEnterpriseByOrganIdAndDepId(organId, depId);
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
        if (StringUtils.isNotEmpty(organDrugsSaleConfig.getStorePaymentWay())) {
            List<Integer> storePaymentWayList = JSON.parseArray(organDrugsSaleConfig.getStorePaymentWay(), Integer.class);
            organDrugsSaleConfigVo.setStorePaymentWay(storePaymentWayList);
        }
        if (StringUtils.isNotEmpty(organDrugsSaleConfig.getTakeOneselfPaymentWay())) {
            List<Integer> takeOneselfPaymentWayList = JSON.parseArray(organDrugsSaleConfig.getTakeOneselfPaymentWay(), Integer.class);
            organDrugsSaleConfigVo.setTakeOneselfPaymentWay(takeOneselfPaymentWayList);
        }
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
        if (StringUtils.isNotEmpty(organDrugsSaleConfigVo.getSendDrugNotifyPhone())) {
            //说明发药电话不为空,需要校验手机号是否合规
            validatePhoneInfo(organDrugsSaleConfigVo.getSendDrugNotifyPhone());
        }
        if (StringUtils.isNotEmpty(organDrugsSaleConfigVo.getRefundNotifyPhone())) {
            validatePhoneInfo(organDrugsSaleConfigVo.getRefundNotifyPhone());
        }
        if (StringUtils.isNotEmpty(organDrugsSaleConfigVo.getOrderPushFailPhone())) {
            validatePhoneInfo(organDrugsSaleConfigVo.getOrderPushFailPhone());
        }
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
        return enterpriseBusinessService.pushDrugDispenser(recipeId);
    }

    /**
     * 运营平台调用发药机发药根据订单
     *
     * @param orderId
     * @return
     */
    @RpcService
    public Boolean pushDrugDispenserByOrder(Integer orderId) {
        return enterpriseBusinessService.pushDrugDispenserByOrder(orderId);
    }

    /**
     * 更新药企的优先级
     * @param depId
     * @param level
     * @return
     */
    @RpcService
    public Boolean updateEnterprisePriorityLevel(Integer organId, Integer depId, Integer level){
        return enterpriseBusinessService.updateEnterprisePriorityLevel(organId, depId, level);
    }

    /**
     * 校验手机号
     * @param mobilePhones 手机列表
     */
    private void validatePhoneInfo(String mobilePhones) {
        mobilePhones = mobilePhones.replace(" ","");
        List<String> mobilePhoneList = Arrays.asList(mobilePhones.split(","));
        if (mobilePhoneList.size() > 5) {
            throw new DAOException("联系电话最多支持5个");
        }
        mobilePhoneList.forEach(mobile -> {
            if (!ValidateUtil.isPhoneLegal(mobile)) {
                throw new DAOException("手机号格式错误");
            }
        });
        Set<String> mobilePhoneSet = new HashSet<>();
        mobilePhoneSet.addAll(mobilePhoneList);
        if (mobilePhoneList.size() != mobilePhoneSet.size()) {
            throw new DAOException("存在重复手机号");
        }
    }
}
