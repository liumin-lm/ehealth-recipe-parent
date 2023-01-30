package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.his.recipe.mode.DrugTakeChangeReqTO;
import com.ngari.his.recipe.mode.FTYSendTimeReqDTO;
import com.ngari.patient.service.AddrAreaService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.platform.recipe.mode.DrugsEnterpriseBean;
import com.ngari.platform.recipe.mode.MedicineStationDTO;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressAndPrice;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.event.GlobalEventExecFactory;
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
import recipe.client.EnterpriseClient;
import recipe.client.OrganClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.*;
import recipe.enumerate.type.AppointEnterpriseTypeEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.hisservice.HisRequestInit;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.manager.EnterpriseManager;
import recipe.manager.RecipeDetailManager;
import recipe.manager.RecipeManager;
import recipe.manager.StateManager;
import recipe.purchase.CommonOrder;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.*;
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

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @description： 药企 业务类
 * @author： yinsheng
 * @date： 2021-12-08 18:58
 */
@Service
public class EnterpriseBusinessService extends BaseService implements IEnterpriseBusinessService {
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
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RemoteDrugEnterpriseService remoteDrugEnterpriseService;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private OrganClient organClient;
    @Resource
    private EnterpriseClient enterpriseClient;
    @Resource
    private StateManager stateManager;
    @Resource
    private OrganAndDrugsepRelationDAO drugsDepRelationDAO;


    @Override
    public Boolean existEnterpriseByName(String name) {
        List<DrugsEnterprise> drugsEnterprises = enterpriseManager.findAllDrugsEnterpriseByName(name);
        return CollectionUtils.isNotEmpty(drugsEnterprises);
    }

    public Boolean existEnterpriseByOrganIdAndDepId(Integer organId, Integer depId){
        OrganAndDrugsepRelation drugsDepRelation = drugsDepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, depId);
        if (Objects.nonNull(drugsDepRelation)) {
            return true;
        }
        return false;
    }

    @Override
    public void saveOrganEnterpriseRelation(OrganEnterpriseRelationVo organEnterpriseRelationVo) {
        logger.info("DrugsEnterpriseBusinessService saveOrganEnterpriseRelation organEnterpriseRelationVo={}", JSONArray.toJSONString(organEnterpriseRelationVo));
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organEnterpriseRelationVo.getOrganId(), organEnterpriseRelationVo.getDrugsEnterpriseId());
        if(organEnterpriseRelationVo.getEnterpriseDrugForm().size() > 20){
            throw new DAOException("最多支持20种剂型配置");
        }
        String giveModeTypes = StringUtils.join(organEnterpriseRelationVo.getGiveModeTypes(), ByteUtils.COMMA);
        String recipeTypes = StringUtils.join(organEnterpriseRelationVo.getRecipeTypes(), ByteUtils.COMMA);
        String decoctionIds = StringUtils.join(organEnterpriseRelationVo.getDecoctionIds(), ByteUtils.COMMA);
        if (Objects.isNull(relation)) {
            OrganAndDrugsepRelation newRelation = new OrganAndDrugsepRelation();
            newRelation.setCannotMedicalFlag(JSONArray.toJSONString(organEnterpriseRelationVo.getCannotMedicalFlag()));
            newRelation.setDrugsEnterpriseSupportGiveMode(giveModeTypes);
            newRelation.setDrugsEnterpriseId(organEnterpriseRelationVo.getDrugsEnterpriseId());
            newRelation.setEnterpriseRecipeTypes(recipeTypes);
            newRelation.setEnterpriseDecoctionIds(decoctionIds);
            newRelation.setEnterpriseDrugForm(JSONArray.toJSONString(organEnterpriseRelationVo.getEnterpriseDrugForm()));
            newRelation.setSupportDecoctionState(JSONArray.toJSONString(organEnterpriseRelationVo.getSupportDecoctionType()));
            newRelation.setSupportSelfDecoctionState(JSONArray.toJSONString(organEnterpriseRelationVo.getSupportSelfDecoctionState()));
            newRelation.setOrganId(organEnterpriseRelationVo.getOrganId());
            organAndDrugsepRelationDAO.save(newRelation);
        } else {
            relation.setCannotMedicalFlag(JSONArray.toJSONString(organEnterpriseRelationVo.getCannotMedicalFlag()));
            relation.setDrugsEnterpriseSupportGiveMode(giveModeTypes);
            relation.setEnterpriseRecipeTypes(recipeTypes);
            relation.setEnterpriseDecoctionIds(decoctionIds);
            relation.setEnterpriseDrugForm(JSONArray.toJSONString(organEnterpriseRelationVo.getEnterpriseDrugForm()));
            relation.setSupportDecoctionState(JSONArray.toJSONString(organEnterpriseRelationVo.getSupportDecoctionType()));
            relation.setSupportSelfDecoctionState(JSONArray.toJSONString(organEnterpriseRelationVo.getSupportSelfDecoctionState()));
            organAndDrugsepRelationDAO.updateNonNullFieldByPrimaryKey(relation);
        }
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
        if (StringUtils.isNotEmpty(relation.getEnterpriseDrugForm())) {
            List<String> drugFrom = JSONUtils.parse((relation.getEnterpriseDrugForm()), List.class);
            organEnterpriseRelationVo.setEnterpriseDrugForm(drugFrom);
        }
        if (StringUtils.isNotEmpty(relation.getSupportDecoctionState())) {
            List<Integer> supportDecoctionType = JSONUtils.parse((relation.getSupportDecoctionState()), List.class);
            organEnterpriseRelationVo.setSupportDecoctionType(supportDecoctionType);
        }
        if (StringUtils.isNotEmpty(relation.getSupportSelfDecoctionState())) {
            List<Integer> supportSelfDecoctionState = JSONUtils.parse((relation.getSupportSelfDecoctionState()), List.class);
            organEnterpriseRelationVo.setSupportSelfDecoctionState(supportSelfDecoctionState);
        }
        if (StringUtils.isNotEmpty(relation.getCannotMedicalFlag())) {
            List<Integer> cannotMedicalFlag = JSONUtils.parse((relation.getCannotMedicalFlag()), List.class);
            organEnterpriseRelationVo.setCannotMedicalFlag(cannotMedicalFlag);
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
        //如果没有具体的煎法关联地址传进来,默认不需要更新
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS())) {
            return;
        }

        // 更新机构药企煎法关联的地址
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses = BeanCopyUtils.copyList(enterpriseDecoctionAddressReq.getEnterpriseDecoctionAddressDTOS(), EnterpriseDecoctionAddress::new);

        List<FutureTask<String>> futureTasks = new LinkedList<>();
        List<List<EnterpriseDecoctionAddress>> groupList = com.google.common.collect.Lists.partition(enterpriseDecoctionAddresses, 500);
        groupList.forEach(a -> {
            FutureTask<String> ft = new FutureTask<>(() -> batchAddEnterpriseDecoctionAddress(a));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        });
        String result = "";
        try {
            for (FutureTask<String> futureTask : futureTasks) {
                String str = futureTask.get();
                if (!"200".equals(str)) {
                    result = str;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("addEnterpriseAddressList error ", e);
            throw new DAOException(DAOException.VALUE_NEEDED, "address error");
        }
    }

    @Override
    public List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        return enterpriseManager.findEnterpriseDecoctionAddressList(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(),
                enterpriseDecoctionAddressReq.getDecoctionId(), enterpriseDecoctionAddressReq.getArea());
    }

    @Override
    public List<OrganAndDrugsepRelation> findOrganAndDrugsepRelationBean(Integer enterpriseId) {
        UserRoleToken ur = UserRoleToken.getCurrent();
        String manageUnit = ur.getManageUnit();
        // 机构管理员获取机构信息
        if (!"eh".equals(manageUnit)) {
            List<Integer> organIds = organClient.findOrganIdsByManageUnit(manageUnit);
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
        AddrAreaService addrAreaService = BasicAPI.getService(AddrAreaService.class);
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
        Long allStreetCount = addrAreaService.getAllStreetCount();
        List<EnterpriseDecoctionList> enterpriseDecoctionLists = decoctionWayList.stream().map(decoctionWay -> {
            EnterpriseDecoctionList enterpriseDecoctionList = null;
            if ("-1".equals(enterpriseDecoctionIds) || list.contains(decoctionWay.getDecoctionId())) {
                enterpriseDecoctionList = new EnterpriseDecoctionList();
                enterpriseDecoctionList.setDecoctionId(decoctionWay.getDecoctionId());
                enterpriseDecoctionList.setDecoctionName(decoctionWay.getDecoctionText());
                int status = 0;
                if (MapUtils.isNotEmpty(finalCollect) && CollectionUtils.isNotEmpty(finalCollect.get(decoctionWay.getDecoctionId()))) {
                    int count = finalCollect.get(decoctionWay.getDecoctionId()).size();
                    logger.info("findEnterpriseDecoctionList DecoctionId ={}, count={}, allStreetCount={}",
                            decoctionWay.getDecoctionId(), count, allStreetCount);
                    if (count < allStreetCount) {
                        status = 1;
                    } else {
                        status = 2;
                    }
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
        List<EnterpriseDecoctionAddress> enterpriseDecoctionAddressList = enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressListAndStatus(checkAddressReq.getOrganId(),
                checkAddressReq.getEnterpriseId(),
                checkAddressReq.getDecoctionId());
        String checkAddress = checkAddressReq.getAddress3();
        boolean b = checkAddressDecoctionHaveStreet(enterpriseDecoctionAddressList, checkAddress);
        if (b && StringUtils.isNotEmpty(checkAddressReq.getAddress4())) {
            checkAddress = checkAddressReq.getAddress4();
        }
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

    private boolean checkAddressDecoctionHaveStreet(List<EnterpriseDecoctionAddress> list, String address) {
        for (EnterpriseDecoctionAddress e : list) {
            if (e.getAddress().startsWith(address) && e.getAddress().length() > 6) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean retryPushRecipeOrder(Integer recipeId) {
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugEnterpriseResult result = remoteDrugEnterpriseService.pushSingleRecipeInfo(recipeId);
        return result.getCode() == 1;
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
        drugsEnterprise.setLogisticsMergeFlag(drugsEnterpriseVO.getLogisticsMergeFlag());
        drugsEnterprise.setLogisticsMergeTime(drugsEnterpriseVO.getLogisticsMergeTime());
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
        return addressCan(list, checkAddressVo.getAddress3());
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
        //记录日志
        List<Recipe> recipeList = recipeDAO.findByOrderCode(orderCodeList);
        recipeList.forEach(recipe -> {
            recipeManager.saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.getRecipeStatusEnum(recipe.getStatus()), RecipeStatusEnum.getRecipeStatusEnum(recipe.getStatus()), drugsEnterprise.getName() + "获取处方成功");
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
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType(), "待配送,配送人：" + enterpriseSendOrderVO.getSender());
        });
        //上传监管平台
        recipeIdList.forEach(recipeId -> {
            SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
            syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
        });
        recipeIdList.forEach(recipeId -> {
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DISPENSING, RecipeStateEnum.SUB_ORDER_DELIVERED_MEDICINE);
        });
        stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_ORDER, OrderStateEnum.SUB_ORDER_DELIVERED_MEDICINE);
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
        recipeIdList.forEach(recipeId -> {
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DISTRIBUTION, RecipeStateEnum.SUB_ORDER_DELIVERED);
        });
        stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_ORDER, OrderStateEnum.SUB_ORDER_DELIVERED);
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
        stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_DISPENSING, OrderStateEnum.SUB_DONE_SEND);
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
        List<EnterpriseAddressAndPrice> collect = enterpriseAddresses.stream().map(enterpriseAddress -> {
            EnterpriseAddressAndPrice enterpriseAddressAndPrice = BeanCopyUtils.copyProperties(enterpriseAddress, EnterpriseAddressAndPrice::new);
            enterpriseAddressAndPrice.setDrugDistributionPriceId(enterpriseAddress.getId());
            return enterpriseAddressAndPrice;
        }).collect(Collectors.toList());


        return collect;
    }

    ///**
    // * P1-27531-108310- 【实施/上海】【上海市第七人民医院】【B】【BUG】互联网医院中草药处方无法选择在可配范围内的快递地址
    // * 上面需求在findEnterpriseAddressProvinceV2中已开发
    // *
    // * @param enterpriseId
    // * @return
    // */
    //@Override
    //public List<EnterpriseAddressAndPrice> findEnterpriseAddressProvince(Integer enterpriseId) {
    //    List<EnterpriseAddress> enterpriseAddresses = enterpriseAddressDAO.findByEnterPriseId(enterpriseId);
    //    if (CollectionUtils.isEmpty(enterpriseAddresses)) {
    //        return Lists.newArrayList();
    //    }
    //    logger.info("findEnterpriseAddressProvince enterpriseAddresses={}", JSON.toJSONString(enterpriseAddresses));
    //    List<EnterpriseAddressAndPrice> list = enterpriseAddresses.stream().map(enterpriseAddress -> {
    //        EnterpriseAddressAndPrice enterpriseAddressAndPrice = new EnterpriseAddressAndPrice();
    //        enterpriseAddressAndPrice.setEnterpriseId(enterpriseAddress.getEnterpriseId());
    //        enterpriseAddressAndPrice.setAddress(enterpriseAddress.getAddress().substring(0, 2));
    //        return enterpriseAddressAndPrice;
    //    }).filter(distinctByKey(e -> e.getAddress())).collect(Collectors.toList());
    //    return list;
    //}

    @Override
    public List<EnterpriseAddressAndPrice> findEnterpriseAddressProvince(Integer enterpriseId) {
        List<EnterpriseAddress> enterpriseAddresses = enterpriseAddressDAO.findByEnterPriseId(enterpriseId);
        enterpriseAddresses = enterpriseAddresses.stream().filter(x -> StringUtils.isNotEmpty(x.getAddress()) && x.getAddress().length() > 1).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enterpriseAddresses)) {
            return Lists.newArrayList();
        }

        //每个省配置的街道数量map
        Map<String, Long> map = enterpriseAddresses.stream()
                .peek(e -> e.setAddress(e.getAddress().substring(0, 2)))
                .collect(Collectors.groupingBy(EnterpriseAddress::getAddress, Collectors.counting()));
        logger.info("findEnterpriseAddressProvince map={}", JSON.toJSONString(map));

        List<EnterpriseAddressAndPrice> list = enterpriseAddresses.stream().map(enterpriseAddress -> {
            EnterpriseAddressAndPrice enterpriseAddressAndPrice = new EnterpriseAddressAndPrice();
            enterpriseAddressAndPrice.setEnterpriseId(enterpriseAddress.getEnterpriseId());
            enterpriseAddressAndPrice.setAddress(enterpriseAddress.getAddress().substring(0, 2));
            return enterpriseAddressAndPrice;
        }).filter(distinctByKey(e -> e.getAddress())).collect(Collectors.toList());

        //设置ConfigFlag字段，若enterpriseAddress配置数量 >= 基础服务下街道数量，显示添加全部地区
        AddrAreaService addrAreaService = BasicAPI.getService(AddrAreaService.class);
        list.forEach(x -> {
            Long countBasic = addrAreaService.getCountAreaLikeCode(x.getAddress()) + 1;
            Long countRecipe = map.get(x.getAddress());
            logger.info("findEnterpriseAddressProvince countBasic = {}, countRecipe = {}", countBasic, countRecipe);
            if (countRecipe < countBasic) {
                x.setConfigFlag(1);
            } else {
                x.setConfigFlag(2);
            }
        });
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
                if (saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList)) {
                    updateData.add(saleDrugList.getOrganDrugCode());
                }
            });
        }
        EnterpriseResultBean resultBean = new EnterpriseResultBean();
        resultBean.setCode(EnterpriseResultBean.SUCCESS);
        resultBean.setMsg("传入条数:" + enterpriseDrugVOList.size() + ",更新条数:" + updateData.size());
        return resultBean;
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId) {
        return enterpriseManager.getOrganDrugsSaleConfig(organId, drugsEnterpriseId, GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType());
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfigOfPatient(Integer organId, Integer drugsEnterpriseId) {
        return enterpriseManager.getOrganDrugsSaleConfigOfPatient(organId, drugsEnterpriseId);
    }

    @Override
    public List<MedicineStationVO> getMedicineStationList(MedicineStationVO medicineStationVO) {
        logger.info("getMedicineStationList medicineStationVO:{}", JSONUtils.toString(medicineStationVO));
        OrganDTO organDTO = organClient.organDTO(medicineStationVO.getOrganId());
        DrugsEnterprise drugsEnterprise = enterpriseManager.drugsEnterprise(medicineStationVO.getEnterpriseId());
        DrugsEnterpriseBean enterpriseBean = com.ngari.patient.utils.ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
        MedicineStationDTO medicineStationDTO = com.ngari.patient.utils.ObjectCopyUtils.convert(medicineStationVO, MedicineStationDTO.class);
        OrganBean organBean = com.ngari.patient.utils.ObjectCopyUtils.convert(organDTO, OrganBean.class);
        //获取取药站点列表
        List<MedicineStationDTO> medicineStationDTOList = enterpriseClient.getMedicineStationList(medicineStationDTO, organBean, enterpriseBean);
        List<MedicineStationVO> medicineStationVOList = com.ngari.patient.utils.ObjectCopyUtils.convert(medicineStationDTOList, MedicineStationVO.class);

        //根据坐标计算距离
        medicineStationVOList.forEach(medicineStation -> {
            try {
                if (StringUtils.isNotEmpty(medicineStation.getLat()) && StringUtils.isNotEmpty(medicineStation.getLng())) {
                    Double distance = DistanceUtil.getDistance(Double.parseDouble(medicineStationVO.getLat()), Double.parseDouble(medicineStationVO.getLng()),
                            Double.parseDouble(medicineStation.getLat()), Double.parseDouble(medicineStation.getLng()));
                    medicineStation.setDistance(Double.parseDouble(String.format("%.2f", distance)));
                } else {
                    medicineStation.setDistance(0D);
                }
            } catch (Exception e) {
                logger.error("getMedicineStationList error", e);
                medicineStation.setDistance(0D);
            }
        });
        return medicineStationVOList;
    }

    @Override
    public void updateEnterpriseAddressAndPrice(List<EnterpriseAddressDTO> enterpriseAddressDTOS) {
        //如果没有具体的,默认不需要更新
        if (CollectionUtils.isEmpty(enterpriseAddressDTOS)) {
            return;
        }

        // 更新机构药企关联的地址
        List<EnterpriseAddress> enterpriseAddresses = BeanCopyUtils.copyList(enterpriseAddressDTOS, EnterpriseAddress::new);

        List<FutureTask<String>> futureTasks = new LinkedList<>();
        List<List<EnterpriseAddress>> groupList = com.google.common.collect.Lists.partition(enterpriseAddresses, 500);
        groupList.forEach(a -> {
            FutureTask<String> ft = new FutureTask<>(() -> batchAddEnterpriseAddress(a));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        });
        String result = "";
        try {
            for (FutureTask<String> futureTask : futureTasks) {
                String str = futureTask.get();
                if (!"200".equals(str)) {
                    result = str;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("updateEnterpriseAddressAndPrice error ", e);
            throw new DAOException(DAOException.VALUE_NEEDED, "address error");
        }
    }

    @Override
    public void cancelEnterpriseAddress(Integer enterpriseId, String area) {
        enterpriseAddressDAO.cancelEnterpriseAddress(enterpriseId, area);
    }

    @Override
    public void cancelEnterpriseDecoctionAddress(EnterpriseDecoctionAddressReq enterpriseDecoctionAddressReq) {
        enterpriseDecoctionAddressDAO.cancelEnterpriseDecoctionAddress(enterpriseDecoctionAddressReq.getOrganId(), enterpriseDecoctionAddressReq.getEnterpriseId(), enterpriseDecoctionAddressReq.getDecoctionId());
    }

    @Override
    public Integer checkSendAddressForOrder(CheckOrderAddressVo checkAddressVo) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        //查询对应药企配送的地址
        //没有子订单而且配送药企为空，则提示
        if (null == checkAddressVo.getEnterpriseId()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药企ID为空");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(checkAddressVo.getEnterpriseId());
        Integer flag = getEnterpriseSendFlag(drugsEnterprise, checkAddressVo);
        return flag;

    }

    /**
     * 判断有误街道数据
     * @param address
     * @param list
     * @return
     */
    private Boolean checkAddressHaveStreet(String address, List<EnterpriseAddress> list) {
        for (EnterpriseAddress e : list) {
            if (e.getAddress().startsWith(address) && e.getAddress().length() > 6) {
               return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getFTYSendTime(FTYSendTimeReq ftySendTimeREQ) {
        FTYSendTimeReqDTO ftySendTimeReqDTO = BeanCopyUtils.copyProperties(ftySendTimeREQ, FTYSendTimeReqDTO::new);
        List<Date> ftySendTime = enterpriseClient.getFTYSendTime(ftySendTimeReqDTO);
        List<String> list = ftySendTime.stream().map(f -> {
            return DateFormatUtils.format(f, "yyyy-MM-dd");
        }).collect(Collectors.toList());

        return list;
    }

    @Override
    public Integer checkSendAddressForEnterprises(CheckOrderAddressVo checkOrderAddressVo) {
        //查询对应药企配送的地址
        //没有子订单而且配送药企为空，则提示
        if (CollectionUtils.isEmpty(checkOrderAddressVo.getEnterpriseIds())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药企ID为空");
        }
        List<DrugsEnterprise> enterprises = drugsEnterpriseDAO.findByIdIn(checkOrderAddressVo.getEnterpriseIds());
        if (CollectionUtils.isEmpty(enterprises)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药企不存在");
        }
        //0-能配送 1-省不能配送 2-市不能配送 3-区域不能配送 4-街道不能配送
        Integer flag = 0;
        for (DrugsEnterprise enterprise : enterprises) {
            if (flag != 0) {
                continue;
            }
            flag = getEnterpriseSendFlag(enterprise, checkOrderAddressVo);
        }
        return flag;

    }

    @Override
    public Map<Integer, DrugsEnterprise> findDrugsEnterpriseByIds(List<Integer> ids) {
        logger.info("EnterpriseBusinessService findDrugsEnterpriseByIds ids :{}", JSON.toJSONString(ids));
        List<DrugsEnterprise> enterprises = drugsEnterpriseDAO.findByIdIn(ids);
        return Optional.ofNullable(enterprises).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugsEnterprise::getId, a -> a, (k1, k2) -> k1));
    }


    @Override
    public List<EnterpriseStock> enterprisesList(Integer organId) {
        List<EnterpriseStock> list = new ArrayList<>();
        EnterpriseStock organ = organDrugListManager.organ(organId);
        if (null != organ) {
            list.add(organ);
        }
        List<DrugsEnterprise> enterprises = buttonManager.organAndEnterprise(organId, null, null);
        if (CollectionUtils.isEmpty(enterprises)) {
            return list;
        }
        enterprises.forEach(a -> {
            EnterpriseStock enterpriseStock = new EnterpriseStock();
            enterpriseStock.setDeliveryName(a.getName());
            enterpriseStock.setDeliveryCode(a.getId().toString());
            enterpriseStock.setAppointEnterpriseType(AppointEnterpriseTypeEnum.ENTERPRISE_APPOINT.getType());
            list.add(enterpriseStock);
        });
        return list;
    }

    public List<DrugsEnterpriseVO> findDrugEnterprise(){
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(1);
        return ObjectCopyUtils.convert(drugsEnterprises, DrugsEnterpriseVO.class);
    }

    private Integer getEnterpriseSendFlag(DrugsEnterprise enterprise, CheckOrderAddressVo checkOrderAddressVo) {
        Integer flag = 0;

        if (enterprise != null && enterprise.getOrderType() == 0) {
            //标识跳转到第三方支付,不需要对配送地址进行校验
            flag = 0;
        }
        List<EnterpriseAddress> list = enterpriseAddressDAO.findByEnterPriseId(enterprise.getId());
        if (CollectionUtils.isEmpty(list)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该药企没有配送地址");
        }
        if (!addressCan(list, checkOrderAddressVo.getAddress1())) {
            logger.error("address1不能配送！depId:" + checkOrderAddressVo.getEnterpriseId() + ",address1:" + checkOrderAddressVo.getAddress1());
            flag = 1;
        }
        if (!addressCan(list, checkOrderAddressVo.getAddress2())) {
            logger.error("address2不能配送！depId:" + checkOrderAddressVo.getEnterpriseId() + ",address2:" + checkOrderAddressVo.getAddress2());
            flag = 2;
        }
        if (!addressCan(list, checkOrderAddressVo.getAddress3())) {
            logger.error("address3不能配送！depId:" + checkOrderAddressVo.getEnterpriseId() + ",address3:" + checkOrderAddressVo.getAddress3());
            flag = 3;
        }
        // 目前不是所有机构都用了街道,所以需要先判空
        Boolean haveStreet = checkAddressHaveStreet(checkOrderAddressVo.getAddress3(), list);
        if (haveStreet && StringUtils.isNotEmpty(checkOrderAddressVo.getAddress4()) && !addressCan(list, checkOrderAddressVo.getAddress4())) {
            logger.error("address4不能配送！depId:" + checkOrderAddressVo.getEnterpriseId() + ",address4:" + checkOrderAddressVo.getAddress4());
            flag = 4;
        }
        return flag;
    }

    private void syncFinishOrderHandle(List<Integer> recipeIdList, RecipeOrder recipeOrder, boolean isSendFlag) {
        logger.info("syncFinishOrderHandle recipeIdList:{}, recipeOrder:{}", recipeIdList, JSON.toJSONString(recipeOrder));
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        RecipeBusiThreadPool.execute(() -> {
            recipeList.forEach(recipe -> {
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_SEND);
                //HIS消息发送
                hisService.recipeFinish(recipe.getRecipeId());
                //更新pdf
                CommonOrder.finishGetDrugUpdatePdf(recipe.getRecipeId());
                HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                if (isSendFlag) {
                    hisSyncService.uploadFinishMedicine(recipe.getRecipeId());
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
                    String memo = "配送中，配送人：" + enterpriseSendOrderVO.getSender() +"，快递公司：" + logisticsCompanyCode + "，快递单号：" + enterpriseSendOrderVO.getTrackingNumber();
                    enterpriseManager.saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND, RecipeStatusEnum.RECIPE_STATUS_IN_SEND, memo);

                    RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                    PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
                    PatientBean patientBean = ObjectCopyUtils.convert(patientDTO, PatientBean.class);
                    DrugTakeChangeReqTO request = HisRequestInit.initDrugTakeChangeReqTO(recipe, recipeDetailMap.get(recipe.getRecipeId()), patientBean, null);
                    service.drugTakeChange(request);

                    //监管平台上传配送信息(派药)
                    HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                    hisSyncService.uploadSendMedicine(recipe.getRecipeId());
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


    private String batchAddEnterpriseDecoctionAddress
            (List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses) {
        for (EnterpriseDecoctionAddress enterpriseDecoctionAddress : enterpriseDecoctionAddresses) {

            try {
                if (Objects.isNull(enterpriseDecoctionAddress.getId())) {
                    enterpriseDecoctionAddressDAO.save(enterpriseDecoctionAddress);
                } else {
                    enterpriseDecoctionAddressDAO.update(enterpriseDecoctionAddress);
                }
            } catch (Exception e) {
                logger.warn("batchAddEnterpriseDecoctionAddress error EnterpriseDecoctionAddress = {}", JSON.toJSONString(enterpriseDecoctionAddress), e);
                return e.getMessage();
            }
        }
        return "200";
    }

    private String batchAddEnterpriseAddress(List<EnterpriseAddress> enterpriseAddresses) {
        for (EnterpriseAddress enterpriseAddress : enterpriseAddresses) {

            try {
                if (Objects.isNull(enterpriseAddress.getId())) {
                    enterpriseAddressDAO.save(enterpriseAddress);
                } else {
                    enterpriseAddressDAO.update(enterpriseAddress);
                }
            } catch (Exception e) {
                logger.warn("batchAddEnterpriseDecoctionAddress error EnterpriseDecoctionAddress = {}", JSON.toJSONString(enterpriseAddress), e);
                return e.getMessage();
            }
        }
        return "200";
    }

}
