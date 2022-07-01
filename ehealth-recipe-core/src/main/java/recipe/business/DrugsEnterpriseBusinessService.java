package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.his.recipe.mode.DrugTakeChangeReqTO;
import com.ngari.patient.service.AddrAreaService;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressAndPrice;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.client.PatientClient;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.hisservice.HisRequestInit;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.manager.EnterpriseManager;
import recipe.manager.RecipeDetailManager;
import recipe.manager.RecipeLogManage;
import recipe.manager.RecipeManager;
import recipe.purchase.CommonOrder;
import recipe.service.RecipeHisService;
import recipe.service.RecipeMsgService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;
import recipe.vo.patient.AddressAreaVo;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDrugVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
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
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private EnterpriseDecoctionAddressDAO enterpriseDecoctionAddressDAO;
    @Autowired
    private EnterpriseAddressDAO enterpriseAddressDAO;
    @Autowired
    private OrganService organService;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RemoteDrugEnterpriseService remoteDrugEnterpriseService;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeLogManage recipeLogManage;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private DrugDistributionPriceDAO drugDistributionPriceDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

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
        String giveModeTypes = StringUtils.join(organEnterpriseRelationVo.getGiveModeTypes(), ByteUtils.COMMA);
        relation.setDrugsEnterpriseSupportGiveMode(giveModeTypes);
        String recipeTypes = StringUtils.join(organEnterpriseRelationVo.getRecipeTypes(), ByteUtils.COMMA);
        relation.setEnterpriseRecipeTypes(recipeTypes);
        String decoctionIds = StringUtils.join(organEnterpriseRelationVo.getDecoctionIds(), ByteUtils.COMMA);
        relation.setEnterpriseDecoctionIds(decoctionIds);
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
        if (StringUtils.isNotEmpty(relation.getDrugsEnterpriseSupportGiveMode())) {
            List<Integer> giveModeTypes = Arrays.stream(relation.getDrugsEnterpriseSupportGiveMode().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setGiveModeTypes(giveModeTypes);
        }
        if (StringUtils.isNotEmpty(relation.getEnterpriseDecoctionIds())) {
            List<Integer> enterpriseDecoctionIds = Arrays.stream(relation.getEnterpriseDecoctionIds().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setDecoctionIds(enterpriseDecoctionIds);
        }
        if (StringUtils.isNotEmpty(relation.getEnterpriseRecipeTypes())) {
            List<Integer> enterpriseRecipeTypes = Arrays.stream(relation.getEnterpriseRecipeTypes().split(ByteUtils.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
            organEnterpriseRelationVo.setRecipeTypes(enterpriseRecipeTypes);
        }
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
            vo.setOrganId(null);
            drugsEnterpriseIds = drugsEnterpriseList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        }
        logger.info("DrugsEnterpriseBusinessService drugsEnterpriseLimit drugsEnterpriseIds:{}", JSON.toJSONString(drugsEnterpriseIds));
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

    @Override
    public void addEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        // 先删除所有机构药企煎法关联的地址
        enterpriseManager.deleteEnterpriseDecoctionAddress(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());

        //如果没有具体的煎法关联地址传进来,默认不需要更新
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS())) {
            return;
        }

        // 更新机构药企煎法关联的地址
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = BeanCopyUtils.copyList(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS(), EnterpriseDecoctionAddress::new);
        enterpriseManager.addEnterpriseDecoctionAddressList(enterpriseDecoctionAddresses);
    }

    @Override
    public List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        return enterpriseManager.findEnterpriseDecoctionAddressList(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());
    }

    @Override
    public List<OrganAndDrugsepRelation> findOrganAndDrugsepRelationBean(Integer enterpriseId) {
        UserRoleToken ur = UserRoleToken.getCurrent();
        String manageUnit = ur.getManageUnit();
        // 机构管理员获取机构信息
        if (!"eh".equals(manageUnit)) {
            List<Integer> organIds = organService.findOrganIdsByManageUnit(manageUnit + "%");
            logger.info("findOrganAndDrugsepRelationBean manageUnit={},organIds={}", JSONArray.toJSONString(organIds), JSONArray.toJSONString(manageUnit));
            if (CollectionUtils.isNotEmpty(organIds)) {
                return organAndDrugsepRelationDAO.findByEntIdAndOrganIds(enterpriseId, organIds);
            }
        }
        return organAndDrugsepRelationDAO.findByEntId(enterpriseId);
    }

    @Override
    public List<OrganAndDrugsepRelation> findOrganAndDrugsDepRelationBeanByOrganId(Integer organId){
        return organAndDrugsepRelationDAO.findByOrganId(organId);
    }

    @Override
    public List<EnterpriseDecoctionList> findEnterpriseDecoctionList(Integer enterpriseId, Integer organId) {
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, enterpriseId);
        if (Objects.isNull(relation)) {
            return null;
        }
        String enterpriseDecoctionIds = relation.getEnterpriseDecoctionIds();
        if (StringUtils.isEmpty(enterpriseDecoctionIds)) {
            return null;
        }
        String[] split = enterpriseDecoctionIds.split(",");
        List<Integer> list = new ArrayList<>();
        for (String s : split) {
            list.add(Integer.valueOf(s));
        }
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressListByOrganIdAndEntId(organId, enterpriseId);
        Map<Integer, List<EnterpriseDecoctionAddress>> collect = null;
        if (CollectionUtils.isNotEmpty(enterpriseDecoctionAddresses)) {
            collect = enterpriseDecoctionAddresses.stream().collect(Collectors.groupingBy(EnterpriseDecoctionAddress::getDecoctionId));
        }
        // 获取机构下的所有煎法
        List<DecoctionWay> decoctionWayList = drugDecoctionWayDao.findByOrganId(organId);
        Map<Integer, List<EnterpriseDecoctionAddress>> finalCollect = collect;
        List<EnterpriseDecoctionList> enterpriseDecoctionLists = decoctionWayList.stream().map(decoctionWay -> {
            EnterpriseDecoctionList enterpriseDecoctionList = null;
            if ("-1".equals(enterpriseDecoctionIds) || list.contains(decoctionWay.getDecoctionId())) {
                enterpriseDecoctionList = new EnterpriseDecoctionList();
                enterpriseDecoctionList.setDecoctionId(decoctionWay.getDecoctionId());
                enterpriseDecoctionList.setDecoctionName(decoctionWay.getDecoctionText());
                int status = 0;
                if (MapUtils.isNotEmpty(finalCollect) && CollectionUtils.isNotEmpty(finalCollect.get(decoctionWay.getDecoctionId()))) {
                    status = 1;
                }
                enterpriseDecoctionList.setStatus(status);
            }
            return enterpriseDecoctionList;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return enterpriseDecoctionLists;
    }

    @Override
    public CheckAddressRes checkEnterpriseDecoctionAddress(CheckAddressReq checkAddressReq) {
        CheckAddressRes checkAddressRes = new CheckAddressRes();
        Boolean sendFlag = false;
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddressList = enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressList(checkAddressReq.getOrganId(),
                checkAddressReq.getEnterpriseId(),
                checkAddressReq.getDecoctionId());
        String checkAddress = checkAddressReq.getAddress3();
//        if (StringUtils.isNotEmpty(checkAddressReq.getAddress4())) {
//            checkAddress = checkAddressReq.getAddress4();
//        }
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddressList)) {
            List<EnterpriseAddress> list = enterpriseAddressDAO.findByEnterPriseId(checkAddressReq.getEnterpriseId());
            if (CollectionUtils.isNotEmpty(list)) {
                List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = BeanCopyUtils.copyList(list, EnterpriseDecoctionAddress::new);
                if (addressCanSend(enterpriseDecoctionAddresses, checkAddress)) {
                    sendFlag = true;
                    checkAddressRes.setSendFlag(sendFlag);
                    return checkAddressRes;
                }
            }
        }
        List<AddressAreaVo> list = enterpriseDecoctionAddressList.stream().map(enterpriseDecoctionAddress -> {
            AddressAreaVo addressAreaVo = null;
            if (enterpriseDecoctionAddress.getAddress().length() == 2) {
                addressAreaVo = new AddressAreaVo();
                addressAreaVo.setAddress1(enterpriseDecoctionAddress.getAddress());
            }
            return addressAreaVo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        checkAddressRes.setAreaList(list);
        // 配送地址精确到区域,区域可以配送就可以配送
        if (addressCanSend(enterpriseDecoctionAddressList, checkAddress)) {
            sendFlag = true;
        }
        checkAddressRes.setSendFlag(sendFlag);
        return checkAddressRes;
    }

    @Override
    public boolean retryPushRecipeOrder(Integer recipeId) {
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugEnterpriseResult result = remoteDrugEnterpriseService.pushSingleRecipeInfo(recipeId);
        return result.getCode() != 1 ? false : true;
    }

    @Override
    public boolean pushDrugDispenser(Integer recipeId) {
        return remoteDrugEnterpriseService.pushDrugDispenser(recipeId);
    }


    @Override
    public void rePushRecipeToDrugsEnterprise() {
        // 获取需要重新推送的订单
        Date endDate = new Date();
        Date startDate = DateUtils.addDays(endDate, -2);
        List<RecipeOrder> recipeOrders = recipeOrderDAO.findUnPushOrder(DateFormatUtils.format(startDate, "yyyy-MM-dd HH:mm:ss"), DateFormatUtils.format(endDate, "yyyy-MM-dd HH:mm:ss"));
        recipeOrders.forEach(order -> {
            List<Integer> recipeIds = JSONUtils.parse(order.getRecipeIdList(), List.class);
            remoteDrugEnterpriseService.pushSingleRecipeInfo(Integer.valueOf(recipeIds.get(0)));
        });

    }

    @Override
    public boolean updateDrugEnterprise(DrugsEnterpriseVO drugsEnterpriseVO) {
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(drugsEnterpriseVO.getId());
        drugsEnterprise.setShowLogisticsLink(drugsEnterpriseVO.getShowLogisticsLink());
        drugsEnterprise.setShowLogisticsType(drugsEnterpriseVO.getShowLogisticsType());
        drugsEnterprise.setLogisticsType(drugsEnterpriseVO.getLogisticsType());
        drugsEnterpriseDAO.update(drugsEnterprise);
        return true;
    }

    @Override
    public Boolean checkSendAddress(CheckAddressVo checkAddressVo) {
        List<OrganAndDrugsepRelation> organAndDrugsepRelations = organAndDrugsepRelationDAO.findByOrganId(checkAddressVo.getOrganId());
        if (CollectionUtils.isEmpty(organAndDrugsepRelations)){
            return false;
        }
        List<Integer> enterpriseIds = organAndDrugsepRelations.stream().map(OrganAndDrugsepRelation::getDrugsEnterpriseId).collect(Collectors.toList());
        List<EnterpriseAddress> list = enterpriseAddressDAO.findByEnterPriseIds(enterpriseIds);
        if (CollectionUtils.isEmpty(list)){
            return false;
        }
        if (addressCan(list, checkAddressVo.getAddress3())) {
            return true;
        }
        return false;
    }

    @Override
    public EnterpriseResultBean confirmOrder(EnterpriseConfirmOrderVO enterpriseConfirmOrderVO) {
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAppKey(enterpriseConfirmOrderVO.getAppKey());
        if (null == drugsEnterprise) {
            return EnterpriseResultBean.getFail("当前appKey错误");
        }
        List<String> orderCodeList = enterpriseConfirmOrderVO.getOrderCodeList();
        List<RecipeOrder> recipeOrderList = recipeOrderDAO.findByOrderCode(orderCodeList);
        if (CollectionUtils.isEmpty(recipeOrderList)) {
            return EnterpriseResultBean.getFail("没有查询到订单信息");
        }
        recipeOrderList.forEach(recipeOrder -> {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            recipeDAO.updateRecipeByDepIdAndRecipes(drugsEnterprise.getId(), recipeIdList);
        });
        return EnterpriseResultBean.getSuccess("成功");
    }

    @Override
    public EnterpriseResultBean readySendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(enterpriseSendOrderVO.getOrderCode());
        if (null == recipeOrder) {
            return EnterpriseResultBean.getFail("没有查询到订单信息");
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        //更新处方信息
        recipeDAO.updateSendInfoByRecipeIds(recipeIdList, enterpriseSendOrderVO.getSendDate(), enterpriseSendOrderVO.getSender(), RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType());
        //记录日志
        recipeIdList.forEach(recipeId->{
            recipeLogManage.saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType(), "待配送,配送人：" + enterpriseSendOrderVO.getSender());
        });
        //上传监管平台
        recipeIdList.forEach(recipeId -> {
            SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
            syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
        });
        return EnterpriseResultBean.getSuccess("成功");
    }

    @Override
    public EnterpriseResultBean sendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(enterpriseSendOrderVO.getOrderCode());
        if (null == recipeOrder) {
            return EnterpriseResultBean.getFail("没有查询到订单信息");
        }
        List<Integer> supportStatus = Arrays.asList(RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType(), RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType());
        if (supportStatus.contains(recipeOrder.getStatus())) {
            return EnterpriseResultBean.getFail("当前订单不允许发货");
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        //更新处方信息
        recipeDAO.updateSendInfoByRecipeIds(recipeIdList, enterpriseSendOrderVO.getSendDate(), enterpriseSendOrderVO.getSender(), RecipeStatusEnum.RECIPE_STATUS_IN_SEND.getType());
        //更新订单信息
        Integer logisticsCompanyCode;
        try {
            logisticsCompanyCode = StringUtils.isEmpty(enterpriseSendOrderVO.getLogisticsCompany()) ? 0 : Integer.valueOf(enterpriseSendOrderVO.getLogisticsCompany());
        } catch (NumberFormatException e) {
            String errorMsg = "当前所传入快递公司编码：" + enterpriseSendOrderVO.getLogisticsCompany() + "，与纳里提供物流字典不匹配，请对照文档字典";
            return EnterpriseResultBean.getFail(errorMsg);
        }
        recipeOrder.setLogisticsCompany(logisticsCompanyCode);
        recipeOrder.setTrackingNumber(enterpriseSendOrderVO.getTrackingNumber());
        recipeOrder.setStatus(RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType());
        recipeOrder.setDrugStoreName(enterpriseSendOrderVO.getDrugStoreName());
        recipeOrder.setDrugStoreCode(enterpriseSendOrderVO.getDrugStoreCode());
        recipeOrder.setSendTime(DateConversion.parseDate(enterpriseSendOrderVO.getSendDate(), DateConversion.DEFAULT_DATE_TIME));
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
        //异步处理后续流程
        syncSendOrderHandle(enterpriseSendOrderVO, logisticsCompanyCode, recipeIdList, recipeOrder);
        return EnterpriseResultBean.getSuccess("成功");
    }

    @Override
    public EnterpriseResultBean finishOrder(EnterpriseSendOrderVO enterpriseSendOrderVO){
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(enterpriseSendOrderVO.getOrderCode());
        if (null == recipeOrder) {
            return EnterpriseResultBean.getFail("没有查询到订单信息");
        }
        List<Integer> sendStatusList = Arrays.asList(RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType());
        if (sendStatusList.contains(recipeOrder.getStatus())) {
            return EnterpriseResultBean.getFail("当前订单已完成，不允许再次更新");
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        Date finishDate = StringUtils.isEmpty(enterpriseSendOrderVO.getFinishDate())? DateTime.now().toDate():DateConversion.parseDate(enterpriseSendOrderVO.getFinishDate(), DateConversion.DEFAULT_DATE_TIME);
        boolean isSendFlag = false;
        if (!RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText().equals(recipeOrder.getGiveModeKey())) {
            isSendFlag = true;
        }
        //更新处方
        recipeManager.finishRecipes(recipeIdList, finishDate);
        //更新订单完成
        recipeOrder.setEffective(1);
        recipeOrder.setPayFlag(PayFlagEnum.PAYED.getType());
        recipeOrder.setFinishTime(finishDate);
        recipeOrder.setStatus(RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType());
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
        syncFinishOrderHandle(recipeIdList, recipeOrder, isSendFlag);
        return EnterpriseResultBean.getSuccess("成功");
    }

    @Override
    public Boolean pushDrugDispenserByOrder(Integer orderId) {
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        recipeIdList.forEach(recipeId->{
            pushDrugDispenser(recipeId);
        });
        return true;
    }

    @Override
    public Boolean updateEnterprisePriorityLevel(Integer organId, Integer depId, Integer level) {
        OrganAndDrugsepRelation organAndDrugsepRelation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, depId);
        if (ObjectUtils.isEmpty(organAndDrugsepRelation)) {
            throw new DAOException("药企不存在");
        }
        organAndDrugsepRelation.setPriorityLevel(level);
        organAndDrugsepRelationDAO.update(organAndDrugsepRelation);
        return true;
    }

    @Override
    public List<EnterpriseAddressAndPrice> findEnterpriseAddressAndPrice(Integer enterpriseId,String area) {
        List<EnterpriseAddress> enterpriseAddresses = enterpriseAddressDAO.findByEnterPriseIdAndArea(enterpriseId,area);
        if (CollectionUtils.isEmpty(enterpriseAddresses)) {
            return Lists.newArrayList();
        }
        List<DrugDistributionPrice> drugDistributionPrices = drugDistributionPriceDAO.findByEnterpriseId(enterpriseId);
        if (CollectionUtils.isEmpty(drugDistributionPrices)){
            return BeanCopyUtils.copyList(enterpriseAddresses,EnterpriseAddressAndPrice::new);
        }
        Map<String, List<DrugDistributionPrice>> listMap = drugDistributionPrices.stream().collect(Collectors.groupingBy(DrugDistributionPrice::getAddrArea));
        List<EnterpriseAddressAndPrice> collect = enterpriseAddresses.stream().map(enterpriseAddress -> {
            EnterpriseAddressAndPrice enterpriseAddressAndPrice = BeanCopyUtils.copyProperties(enterpriseAddress, EnterpriseAddressAndPrice::new);
            if (MapUtils.isNotEmpty(listMap) && CollectionUtils.isNotEmpty(listMap.get(enterpriseAddress.getAddress()))) {
                List<DrugDistributionPrice> prices = listMap.get(enterpriseAddress.getAddress());
                enterpriseAddressAndPrice.setDistributionPrice(prices.get(0).getDistributionPrice());
                enterpriseAddressAndPrice.setBuyFreeShipping(prices.get(0).getBuyFreeShipping());
                enterpriseAddressAndPrice.setDrugDistributionPriceId(prices.get(0).getId());

            }
            return enterpriseAddressAndPrice;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<EnterpriseAddressAndPrice> findEnterpriseAddressProvince(Integer enterpriseId) {
        List<EnterpriseAddress> enterpriseAddresses = enterpriseAddressDAO.findByEnterPriseId(enterpriseId);
        if(CollectionUtils.isEmpty(enterpriseAddresses)){
            return Lists.newArrayList();
        }

        List<EnterpriseAddressAndPrice> list = enterpriseAddresses.stream().map(enterpriseAddress -> {
            EnterpriseAddressAndPrice enterpriseAddressAndPrice = new EnterpriseAddressAndPrice();
            enterpriseAddressAndPrice.setEnterpriseId(enterpriseAddress.getEnterpriseId());
            enterpriseAddressAndPrice.setAddress(enterpriseAddress.getAddress().substring(0, 2));

            return enterpriseAddressAndPrice;
        }).filter(distinctByKey(e -> e.getAddress())).collect(Collectors.toList());
        return list;
    }


    @Override
    public EnterpriseResultBean renewDrugInfo(List<EnterpriseDrugVO> enterpriseDrugVOList) {
        Map<String, List<EnterpriseDrugVO>> enterpriseDrugVOListMap = enterpriseDrugVOList.stream().collect(Collectors.groupingBy(EnterpriseDrugVO::getAppKey));
        final List<String> updateData = new ArrayList<>();
        for (Map.Entry<String, List<EnterpriseDrugVO>> entry : enterpriseDrugVOListMap.entrySet()) {
            String appKey = entry.getKey();
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAppKey(appKey);
            if (null == drugsEnterprise) {
                continue;
            }
            List<EnterpriseDrugVO> enterpriseDrugVOS = entry.getValue();
            List<String> drugCodeList = enterpriseDrugVOS.stream().map(EnterpriseDrugVO::getDrugCode).collect(Collectors.toList());
            Map<String, EnterpriseDrugVO> enterpriseDrugVOMap = enterpriseDrugVOS.stream().collect(Collectors.toMap(EnterpriseDrugVO::getDrugCode,a->a,(k1,k2)->k1));
            List<SaleDrugList> saleDrugListList = saleDrugListDAO.findByOrganIdAndDrugCodes(drugsEnterprise.getId(), drugCodeList);
            saleDrugListList.forEach(saleDrugList -> {
                EnterpriseDrugVO enterpriseDrugVO = enterpriseDrugVOMap.get(saleDrugList.getOrganDrugCode());
                if (StringUtils.isNotEmpty(enterpriseDrugVO.getDrugName())) {
                    saleDrugList.setDrugName(enterpriseDrugVO.getDrugName());
                }
                if (StringUtils.isNotEmpty(enterpriseDrugVO.getSaleName())) {
                    saleDrugList.setSaleName(enterpriseDrugVO.getSaleName());
                }
                if (StringUtils.isNotEmpty(enterpriseDrugVO.getDrugSpec())) {
                    saleDrugList.setDrugSpec(enterpriseDrugVO.getDrugSpec());
                }
                if (null != enterpriseDrugVO.getPrice()) {
                    saleDrugList.setPrice(enterpriseDrugVO.getPrice());
                }
                if (null != enterpriseDrugVO.getInventory()) {
                    saleDrugList.setInventory(enterpriseDrugVO.getInventory());
                }
                if (saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList)){
                    updateData.add(saleDrugList.getOrganDrugCode());
                }
            });
        }
        EnterpriseResultBean resultBean = new EnterpriseResultBean();
        resultBean.setCode(EnterpriseResultBean.SUCCESS);
        resultBean.setMsg("传入条数:" + enterpriseDrugVOList.size() + ",更新条数:"+ updateData.size());
        return resultBean;
    }

    private void syncFinishOrderHandle(List<Integer> recipeIdList, RecipeOrder recipeOrder, boolean isSendFlag) {
        logger.info("syncFinishOrderHandle recipeIdList:{}, recipeOrder:{}", recipeIdList, JSON.toJSONString(recipeOrder));
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        RecipeBusiThreadPool.execute(() -> {
            recipeList.forEach(recipe->{
                //HIS消息发送
                hisService.recipeFinish(recipe.getRecipeId());
                //更新pdf
                CommonOrder.finishGetDrugUpdatePdf(recipe.getRecipeId());
                HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                if (isSendFlag) {
                    CommonResponse response = hisSyncService.uploadFinishMedicine(recipe.getRecipeId());
                    if (CommonConstant.SUCCESS.equals(response.getCode())) {
                        //记录日志
                        recipeLogManage.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeStatusEnum.RECIPE_STATUS_FINISH.getType(),
                                "监管平台配送信息[配送到家-处方完成]上传成功");
                    } else {
                        //记录日志
                        recipeLogManage.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeStatusEnum.RECIPE_STATUS_FINISH.getType(),
                                "监管平台配送信息[配送到家-处方完成]上传失败：" + response.getMsg());
                    }
                } else {
                    SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                    syncExecutorService.uploadRecipeVerificationIndicators(recipe.getRecipeId());
                }
            });
            Recipe recipe = recipeList.get(0);
            if (isSendFlag) {
                //配送到家
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
            } else {
                //发送取药完成消息
                RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH);
            }

        });
    }

    private void syncSendOrderHandle(EnterpriseSendOrderVO enterpriseSendOrderVO,Integer logisticsCompanyCode, List<Integer> recipeIdList, RecipeOrder recipeOrder) {
        logger.info("syncSendOrderHandle logisticsCompanyCode:{},recipeIdList:{},recipeOrder:{}", logisticsCompanyCode, recipeIdList, JSON.toJSONString(recipeOrder));
        RecipeBusiThreadPool.execute(() -> {
            //更新物流信息
            enterpriseManager.sendLogisticsInfoToBase(recipeOrder, recipeIdList.get(0), logisticsCompanyCode + "", enterpriseSendOrderVO.getTrackingNumber());
            //推送患者物流信息
            RecipeMsgService.batchSendMsg(recipeIdList.get(0), RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
            //推送患者配送中信息
            RecipeMsgService.batchSendMsg(recipeIdList.get(0), RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType());
            List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
            Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetailManager.findRecipeDetailMap(recipeIdList);
            if (!ValidateUtil.integerIsEmpty(logisticsCompanyCode) && StringUtils.isNotEmpty(enterpriseSendOrderVO.getTrackingNumber())) {
                recipeList.forEach(recipe -> {
                    RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                    PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
                    PatientBean patientBean = ObjectCopyUtils.convert(patientDTO, PatientBean.class);
                    DrugTakeChangeReqTO request = HisRequestInit.initDrugTakeChangeReqTO(recipe, recipeDetailMap.get(recipe.getRecipeId()), patientBean, null);
                    service.drugTakeChange(request);

                    //监管平台上传配送信息(派药)
                    HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                    CommonResponse response = hisSyncService.uploadSendMedicine(recipe.getRecipeId());
                    if (CommonConstant.SUCCESS.equals(response.getCode())) {
                        //记录日志
                        recipeLogManage.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType(),
                                "监管平台配送信息[派药]上传成功");
                    } else {
                        //记录日志
                        recipeLogManage.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType(),
                                "监管平台配送信息[派药]上传失败：" + response.getMsg());
                    }
                });
            }
        });
    }

    private boolean addressCan(List<EnterpriseAddress> list, String address) {
        boolean flag = false;
        if (StringUtils.isEmpty(address)) {
            return flag;
        }
        for (EnterpriseAddress e : list) {
            if (e.getAddress().startsWith(address)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    private boolean addressCanSend(List<EnterpriseDecoctionAddress> list, String address) {
        boolean flag = false;
        if (StringUtils.isEmpty(address)) {
            return flag;
        }
        for (EnterpriseDecoctionAddress e : list) {
            if (e.getAddress().startsWith(address)) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
