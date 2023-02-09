package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.TakeMedicineByToHos;
import com.ngari.infra.logistics.mode.WriteBackLogisticsOrderDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.*;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.status.RecipeSourceTypeEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.YesOrNoEnum;
import recipe.enumerate.type.*;
import recipe.third.IFileDownloadService;
import recipe.util.LocalStringUtil;
import recipe.util.RecipeBusiThreadPool;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 药企 功能处理类
 *
 * @author fuzi
 */
@Service
public class EnterpriseManager extends BaseManager {
    /**
     * 扁鹊
     */
    private static String ENTERPRISE_BAN_QUE = "bqEnterprise";
    /**
     * 设置pdf base 64内容
     */
    private static String IMG_HEAD = "data:image/jpeg;base64,";
    private static final String LOGISTICS_COMPANY_SF = "1";
    @Autowired
    private EnterpriseClient enterpriseClient;
    @Autowired
    private IFileDownloadService fileDownloadService;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Resource
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private DrugStockClient drugStockClient;
    @Autowired
    private DepartClient departClient;
    @Autowired
    private OrganDrugsSaleConfigDAO organDrugsSaleConfigDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private PharmacyDAO pharmacyDAO;
    @Autowired
    private EnterpriseDecoctionAddressDAO enterpriseDecoctionAddressDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private SmsClient smsClient;

    /**
     * 获取是否医院结算的药企
     *
     * @param recipeOrder recipeOrder
     * @return
     */
    public Boolean getIsHosSettle(RecipeOrder recipeOrder) {
        // 是否医院结算药企
        Boolean isHosDep = false;
        if(Objects.nonNull(recipeOrder) && Objects.nonNull(recipeOrder.getEnterpriseId())){
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrder.getEnterpriseId());
            if(Objects.nonNull(drugsEnterprise) && Objects.nonNull(drugsEnterprise.getSettlementMode()) && YesOrNoEnum.YES.getType().equals(drugsEnterprise.getSettlementMode())) {
                isHosDep = true;
            }
        } else {
            isHosDep = true;
        }
        return isHosDep;
    }

    /**
     * 到院取药获取取药点
     *
     * @param organId  机构id
     * @param dbRecipe 处方详情
     * @return
     */
    public List<TakeMedicineByToHos> getTakeMedicineByToHosList(Integer organId, Recipe dbRecipe) {
        logger.info("EnterpriseManager getTakeMedicineByToHosList organId:{},dbRecipe:{} ", JSONUtils.toString(organId)
                , JSONUtils.toString(dbRecipe));
        OrganDTO organDTO = organClient.organDTO(organId);
        OrganBean organBean = ObjectCopyUtils.convert(organDTO, OrganBean.class);
        RecipeBean recipeBean = ObjectCopyUtils.convert(dbRecipe, RecipeBean.class);
        List<Recipedetail> detailList = recipeDetailDAO.findByRecipeId(dbRecipe.getRecipeId());
        List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(detailList, RecipeDetailBean.class);
        return enterpriseClient.getTakeMedicineByToHosList(organBean, recipeDetailBeans, recipeBean);
    }

    /**
     * 检查 药企药品 是否满足开方药品
     * 验证能否药品配送以及能否开具到一张处方单上
     *
     * @param enterpriseIds 药企id
     * @param recipeDetails 处方明显-开方药品
     * @return 药企-不满足的 药品名称
     */
    public Map<Integer, List<Integer>> enterpriseDrugIdGroup(List<Integer> enterpriseIds, List<Recipedetail> recipeDetails) {
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        Map<Integer, List<Integer>> enterpriseDrugIdsGroup = saleDrugListDAO.findDepDrugRelation(drugIds, enterpriseIds);
        logger.info("DrugStockManager enterpriseDrugNameGroup enterpriseDrugIdsGroup= {}", JSON.toJSONString(enterpriseDrugIdsGroup));
        Map<Integer, List<Integer>> enterpriseDrugGroup = new HashMap<>();
        enterpriseIds.forEach(a -> {
            List<Integer> drugIdList = enterpriseDrugIdsGroup.get(a);
            if (CollectionUtils.isEmpty(drugIdList)) {
                enterpriseDrugGroup.put(a, drugIds);
                return;
            }
            List<Integer> drugId = recipeDetails.stream().filter(recipeDetail -> !drugIdList.contains(String.valueOf(recipeDetail.getDrugId()))).map(Recipedetail::getDrugId).collect(Collectors.toList());
            enterpriseDrugGroup.put(a, drugId);
        });
        logger.info("DrugStockManager enterpriseDrugNameGroup enterpriseDrugGroup= {}", JSON.toJSONString(enterpriseDrugGroup));
        return enterpriseDrugGroup;
    }


    /**
     * 检查开处方是否需要进行药企库存校验
     *
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    public boolean checkEnterprise(Integer organId) {
        Integer checkEnterprise = configurationClient.getCheckEnterpriseByOrganId(organId);
        if (ValidateUtil.integerIsEmpty(checkEnterprise)) {
            return false;
        }
        //获取机构配置的药企是否存在 如果有则需要校验 没有则不需要
        List<DrugsEnterprise> enterprise = drugsEnterpriseByOrganId(organId);
        return CollectionUtils.isNotEmpty(enterprise);
    }

    public List<DrugsEnterprise> drugsEnterpriseByOrganId(Integer organId) {
        return organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
    }

    public QueryResult<DrugsEnterprise> drugsEnterpriseLimit(String name, Integer createType, Integer organId, Integer start, Integer limit, List<Integer> drugsEnterpriseIds) {
        return drugsEnterpriseDAO.queryDrugsEnterpriseResultByOrganId(name, createType, organId, drugsEnterpriseIds, start, limit);
    }

    /**
     * 药企库存
     *
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @return 1 有库存 0 无库存
     */
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        logger.info("EnterpriseManager scanEnterpriseDrugStock recipeDetails:{}，drugsEnterprise={}", JSON.toJSONString(recipeDetails), JSON.toJSONString(drugsEnterprise));
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIdsEffectivity(drugsEnterprise.getId(), drugIds);
        DrugStockAmountDTO drugStockAmount = new DrugStockAmountDTO();
        if (CollectionUtils.isEmpty(saleDrugLists)) {
            drugStockAmount.setResult(false);
            drugStockAmount.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
            logger.warn("EnterpriseManager scanEnterpriseDrugStock saleDrugLists is null");
            return drugStockAmount;
        }
        drugIds = saleDrugLists.stream().map(SaleDrugList::getDrugId).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIds);
        Map<Integer, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
        //默认走批量新接口
        Map<Integer, List<SaleDrugList>> saleDrugListMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        Set<Integer> pharmacyIds = recipeDetails.stream().map(Recipedetail::getPharmacyId).collect(Collectors.toSet());
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmacyIds);
        DrugStockAmountDTO drugStockAmountV1 = drugStockClient.scanEnterpriseDrugStockV1(recipe, drugsEnterprise, recipeDetails, saleDrugListMap, organDrugMap, pharmacyTcmByIds);
        logger.info("EnterpriseManager scanEnterpriseDrugStock scanEnterpriseDrugStockV1 end drugStockAmountV1 = {}", JSON.toJSONString(drugStockAmountV1));
        if (null != drugStockAmountV1) {
            return drugStockAmountV1;
        }
        //前置机 没对接新接口 走老接口
        List<DrugInfoDTO> drugInfoList = new LinkedList<>();
        boolean result = true;
        for (Recipedetail recipeDetail : recipeDetails) {
            DrugStockAmountDTO drugStockAmountDTO = drugStockClient.scanEnterpriseDrugStock(recipe, drugsEnterprise, Collections.singletonList(recipeDetail), saleDrugListMap, organDrugMap, pharmacyTcmByIds);
            drugInfoList.addAll(drugStockAmountDTO.getDrugInfoList());
            if (!drugStockAmountDTO.isResult()) {
                result = false;
            }
        }
        drugStockAmount.setResult(result);
        drugStockAmount.setDrugInfoList(drugInfoList);
        logger.info("EnterpriseManager scanEnterpriseDrugStock scanEnterpriseDrugStock end drugStockAmount = {}", JSON.toJSONString(drugStockAmount));
        return drugStockAmount;
    }

    /**
     * 根据药企id 于 药品ids 获取药企药品列表
     *
     * @param enterpriseId 药企id
     * @param drugIds      药品ids
     * @return 药企药品列表
     */
    public List<SaleDrugList> saleDrugList(Integer enterpriseId, List<Integer> drugIds) {
        return saleDrugListDAO.findByOrganIdAndDrugIds(enterpriseId, drugIds);
    }

    public Map<Integer, SaleDrugList> saleDrugListEffectivity(Integer enterpriseId, List<Integer> drugIds) {
        List<SaleDrugList> saleDrugList = saleDrugListDAO.findByOrganIdAndDrugIdsEffectivity(enterpriseId, drugIds);
        return saleDrugList.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 到店取药 药企获取
     *
     * @param recipe
     * @param recipeSupportGiveModeEnum
     * @return
     */
    public List<DrugsEnterprise> findEnterpriseByTFDS(Recipe recipe, RecipeSupportGiveModeEnum recipeSupportGiveModeEnum) {
        logger.info("EnterpriseManager findEnterpriseByTFDS req payModeSupport:{},recipe:{}", recipeSupportGiveModeEnum.getName(), JSONUtils.toString(recipe));

        List<DrugsEnterprise> drugsEnterpriseList = new ArrayList<>();
        Integer recipeId = recipe.getRecipeId();
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);

        Integer appointEnterpriseType = recipeExtend.getAppointEnterpriseType();
        logger.info("EnterpriseManager findEnterpriseByTFDS  appointEnterpriseType={}", appointEnterpriseType);
        AppointEnterpriseTypeEnum appointEnterpriseTypeEnum = AppointEnterpriseTypeEnum.getAppointEnterpriseTypeEnum(appointEnterpriseType);

        switch (appointEnterpriseTypeEnum) {
            case ORGAN_APPOINT:
                break;
            case ENTERPRISE_APPOINT:
                String deliveryCode = recipeExtend.getDeliveryCode();
                drugsEnterpriseList = findEnterpriseListByAppoint(deliveryCode, recipe, RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
                break;
            case DEFAULT:
            default:
                drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), recipeSupportGiveModeEnum.getType());
                break;
        }
        logger.info("EnterpriseManager findEnterpriseByTFDS  res={}", JSONUtils.toString(drugsEnterpriseList));
        return drugsEnterpriseList;

    }

    /**
     * 配送到家模式下获取药企
     *
     * @param sendType
     * @param recipeSupportGiveModeEnum
     * @param recipe
     * @return
     */
    public List<DrugsEnterprise> findEnterpriseByOnLine(String sendType, RecipeSupportGiveModeEnum recipeSupportGiveModeEnum, Recipe recipe) {
        logger.info("EnterpriseManager findEnterpriseByOnLine req sendType:{},recipeSupportGiveModeEnum:{},recipe:{}", sendType, recipeSupportGiveModeEnum.getName(), JSONUtils.toString(recipe));
        List<DrugsEnterprise> drugsEnterpriseList = new ArrayList<>();
        if (new Integer(2).equals(recipe.getRecipeSource())) {
            //北京互联网根据HIS传过来的药企进行展示
            HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
            if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getDeliveryCode())) {
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getByAccount(hisRecipe.getDeliveryCode());
                if (enterprise != null) {
                    OrganAndDrugsepRelation organAndDrugsepRelation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(recipe.getClinicOrgan(), enterprise.getId());
                    if (organAndDrugsepRelation != null) {
                        drugsEnterpriseList.add(enterprise);
                    }
                }
            }
            logger.info("EnterpriseManager findEnterpriseByOnLine 北京互联网 res={}", JSONUtils.toString(drugsEnterpriseList));
            return drugsEnterpriseList;
        }
        //筛选出来的数据已经去掉不支持任何方式配送的药企
        Integer recipeId = recipe.getRecipeId();
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);

        Integer appointEnterpriseType = recipeExtend.getAppointEnterpriseType();
        logger.info("EnterpriseManager findEnterpriseByOnLine  appointEnterpriseType={}", appointEnterpriseType);

        AppointEnterpriseTypeEnum appointEnterpriseTypeEnum = AppointEnterpriseTypeEnum.getAppointEnterpriseTypeEnum(appointEnterpriseType);

        switch (appointEnterpriseTypeEnum) {
            case ORGAN_APPOINT:
                break;
            case ENTERPRISE_APPOINT:
                String deliveryCode = recipeExtend.getDeliveryCode();
                drugsEnterpriseList = findEnterpriseListByAppoint(deliveryCode, recipe, RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
                break;
            case DEFAULT:
            default:
                if (StringUtils.isNotEmpty(sendType)) {
                    if (Integer.valueOf(1).equals(recipe.getRecipeSource())) {
                        drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndOtherAndSendType(recipe.getClinicOrgan(), recipeSupportGiveModeEnum.getType());
                    } else {
                        drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), recipeSupportGiveModeEnum.getType());
                    }
                } else {
                    //考虑到浙江省互联网项目的药店取药也会走这里,sendType是"" 还是需要查询一下支持的药企
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), recipeSupportGiveModeEnum.getType());
                }
                break;
        }
        logger.info("EnterpriseManager findEnterpriseByOnLine  res={}", JSONUtils.toString(drugsEnterpriseList));
        return drugsEnterpriseList;
    }

    /**
     * 上传处方pdf给第三方
     *
     * @param recipeId
     */
    public void uploadRecipePdfToHis(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        enterpriseClient.uploadRecipePdfToHis(recipe);
    }

    /**
     * 根据购药方式 推送合并处方信息到药企
     *
     * @param organId   机构id
     * @param giveMode  购药类型
     * @param recipeIds 处方id
     * @return
     */
    public SkipThirdDTO uploadRecipeInfoToThird(Integer organId, String giveMode, List<Integer> recipeIds, String encData) {
        logger.info("EnterpriseManager uploadRecipeInfoToThird organId:{},giveMode:{},recipeIds:{}", organId, giveMode, JSONUtils.toString(recipeIds));
        //处方选择购药方式时回写his
        Boolean pushToHisAfterChoose = configurationClient.getValueBooleanCatch(organId, "pushToHisAfterChoose", false);
        if (!pushToHisAfterChoose) {
            return null;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        //将处方上传到第三方
        SkipThirdDTO result = null;
        for (Recipe recipe : recipes) {
            recipe.setGiveMode(GiveModeTextEnum.getGiveMode(giveMode));
            result = pushRecipeForThird(recipe, 1, encData);
            if (0 == result.getCode()) {
                break;
            }
        }
        return result;
    }

    /**
     * 推送处方数据到药企
     *
     * @param recipe 处方信息
     * @param node
     * @return
     */
    public SkipThirdDTO pushRecipeForThird(Recipe recipe, Integer node, String encData) {
        logger.info("EnterpriseManager pushRecipeForThird recipeId:{}, node:{}.", recipe.getRecipeId(), node);
        SkipThirdDTO result = new SkipThirdDTO();
        result.setCode(1);
        List<DrugsEnterprise> drugsEnterpriseList = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            return result;
        }
        //是否存在扁鹊药企
        List<DrugsEnterprise> bqDrugEnterpriseList = drugsEnterpriseList.stream().filter(drugsEnterprise -> ENTERPRISE_BAN_QUE.equals(drugsEnterprise.getAccount())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(bqDrugEnterpriseList)) {
            result = pushRecipeInfoForThird(recipe, bqDrugEnterpriseList.get(0), node, encData);
            logger.info("EnterpriseManager pushRecipeForThird result:{}", JSONUtils.toString(result));
            return result;
        }
        logger.info("EnterpriseManager pushRecipeForThird result:{}", JSONUtils.toString(result));
        return result;
    }

    public void pushRecipeInfoToBq(Recipe recipe, Integer node){
        try {
            Boolean uploadBqPlatformFlag = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "uploadBqPlatformFlag", false);
            if (uploadBqPlatformFlag) {
                pushRecipeInfoForThird(recipe, null, node, "");
            }
        } catch (Exception e) {
            logger.error("EnterpriseManager pushRecipeForThird error", e);
        }
    }

    /**
     * 推送药企信息去前置机
     *
     * @param recipe     处方信息
     * @param enterprise 药企
     * @param node
     * @return
     */
    public SkipThirdDTO pushRecipeInfoForThird(Recipe recipe, DrugsEnterprise enterprise, Integer node, String encData) {
        logger.info("RemoteDrugEnterpriseService pushRecipeInfoForThird recipeId:{},enterprise:{},node:{}.", recipe.getRecipeId(), JSONUtils.toString(enterprise), node);
        //传过来的处方不是最新的需要重新从数据库获取
        Recipe recipeNew = recipeDAO.getByRecipeId(recipe.getRecipeId());
        //todo 为什么赋值新的GiveMode 却没有更新到数据库？
        recipeNew.setGiveMode(recipe.getGiveMode());
        //通过前置机进行推送
        PushRecipeAndOrder pushRecipeAndOrder = getPushRecipeAndOrder(recipeNew, enterprise, encData);
        SkipThirdDTO skipThirdDTO = enterpriseClient.pushRecipeInfoForThird(pushRecipeAndOrder, node);
        if (0 == skipThirdDTO.getCode()) {
            saveRecipeLog(recipeNew.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "购药按钮药企推送失败:" + skipThirdDTO.getMsg());
            return skipThirdDTO;
        }
        if (StringUtils.isNotEmpty(skipThirdDTO.getUrl())) {
            saveRecipeLog(recipeNew.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "购药按钮药企url:" + skipThirdDTO.getUrl());
        }
        //推送药企处方成功,判断是否为扁鹊平台
        if (null != enterprise && ENTERPRISE_BAN_QUE.equals(enterprise.getAccount())) {
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipeNew.getRecipeId());
            recipeUpdate.setEnterpriseId(enterprise.getId());
            recipeUpdate.setPushFlag(1);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        } else if (StringUtils.isNotEmpty(skipThirdDTO.getPrescId())) {
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeNew.getRecipeId(), ImmutableMap.of("rxid", skipThirdDTO.getPrescId()));
        }
        if (StringUtils.isNotEmpty(skipThirdDTO.getChargeItemCode())) {
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeNew.getRecipeId(), ImmutableMap.of("charge_item_code", skipThirdDTO.getChargeItemCode()));
        }
        if (StringUtils.isNotEmpty(skipThirdDTO.getRecipeCode())) {
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeNew.getRecipeId(), ImmutableMap.of("charge_id", skipThirdDTO.getRecipeCode()));
        }
        RecipeBusiThreadPool.execute(() -> enterpriseClient.uploadRecipePdfToHis(recipeNew));
        return skipThirdDTO;
    }

    /**
     * 前置机推送药企数据组织
     *
     * @param recipe     处方信息
     * @param enterprise 药企信息
     * @return
     */
    public PushRecipeAndOrder getPushRecipeAndOrder(Recipe recipe, DrugsEnterprise enterprise, String encData) {
        PushRecipeAndOrder pushRecipeAndOrder = new PushRecipeAndOrder();
        pushRecipeAndOrder.setOrganId(recipe.getClinicOrgan());
        //设置订单信息
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            enterpriseClient.addressBean(recipeOrder, pushRecipeAndOrder);
        }
        pushRecipeAndOrder.setEncData(encData);
        //设置医生信息
        pushRecipeAndOrder.setDoctorDTO(doctorClient.jobNumber(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart()));
        //设置审方药师信息
        pushRecipeAndOrder.setRecipeAuditReq(recipeAuditReq(recipe));
        //设置药企信息
        pushRecipeAndOrder.setDrugsEnterpriseBean(ObjectCopyUtils.convert(enterprise, DrugsEnterpriseBean.class));
        //设置患者信息
        PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
        pushRecipeAndOrder.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.patient.dto.PatientDTO.class));
        //设置用户信息
        if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
            PatientDTO userDTO = patientClient.getPatientDTO(recipe.getRequestMpiId());
            pushRecipeAndOrder.setUserDTO(ObjectCopyUtils.convert(userDTO, com.ngari.patient.dto.PatientDTO.class));
        }
        String openId = patientClient.getOpenId();
        if (StringUtils.isNotEmpty(openId)) {
            pushRecipeAndOrder.setOpenId(openId);
        }
        //设置科室信息
        pushRecipeAndOrder.setDepartmentDTO(departClient.getDepartmentByDepart(recipe.getDepart()));

        //多处方处理
        List<Recipe> recipes = Arrays.asList(recipe);
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            recipes = recipeDAO.findByRecipeIds(recipeIdList);
        }
        if (recipes.size() > 1) {
            //说明为多处方
            pushRecipeAndOrder.setMergeRecipeFlag(1);
        } else {
            pushRecipeAndOrder.setMergeRecipeFlag(0);
        }
        List<MargeRecipeBean> margeRecipeBeans = new ArrayList<>();
        for (Recipe rec : recipes) {
            //设置处方信息
            RecipeBean recipeBean = ObjectCopyUtils.convert(rec, RecipeBean.class);
            try {
                RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
                if (revisitExDTO != null && StringUtils.isNotEmpty(revisitExDTO.getProjectChannel())) {
                    pushRecipeAndOrder.getDrugsEnterpriseBean().setThirdEnterpriseCode(revisitExDTO.getProjectChannel());
                    recipeBean.setPatientChannelId(revisitExDTO.getProjectChannel());
                }
            } catch (Exception e) {
                logger.error("getPushRecipeAndOrder queryPatientChannelId error", e);
            }
            if (RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(rec.getRecipeSourceType())) {
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(rec.getClinicOrgan(), rec.getRecipeCode());
                if (Objects.nonNull(hisRecipe) && Objects.nonNull(hisRecipe.getSendType())) {
                    recipeBean.setSendType(hisRecipe.getSendType());
                }
            }
            pushRecipeAndOrder.setRecipeBean(recipeBean);
            //设置药企扩展信息
            pushRecipeAndOrder.setExpandDTO(expandDTO(rec, enterprise));
            //设置处方扩展信息
            pushRecipeAndOrder.setRecipeExtendBean(recipeExtend(rec));
            //设置处方药品信息
            pushRecipeAndOrder.setPushDrugListBeans(setSingleRecipeInfo(enterprise, rec.getRecipeId(), rec.getClinicOrgan()));
            //todo 永远给最后一个Recipe信息到pushRecipeAndOrder对象？
            if (1 == pushRecipeAndOrder.getMergeRecipeFlag()) {
                MargeRecipeBean margeRecipeBean = new MargeRecipeBean();
                margeRecipeBean.setRecipeBean(pushRecipeAndOrder.getRecipeBean());
                margeRecipeBean.setRecipeExtendBean(pushRecipeAndOrder.getRecipeExtendBean());
                margeRecipeBean.setExpandDTO(pushRecipeAndOrder.getExpandDTO());
                margeRecipeBean.setPushDrugListBeans(pushRecipeAndOrder.getPushDrugListBeans());
                margeRecipeBeans.add(margeRecipeBean);
            }
        }
        pushRecipeAndOrder.setMargeRecipeBeans(margeRecipeBeans);
        logger.info("getPushRecipeAndOrder pushRecipeAndOrder:{}.", JSONUtils.toString(pushRecipeAndOrder));
        return pushRecipeAndOrder;
    }


    /**
     * 组织返回结果msg
     *
     * @param doSignRecipe
     * @param msg
     * @return
     */
    public void doSignRecipe(DoSignRecipeDTO doSignRecipe, String msg) {
        doSignRecipe.setSignResult(false);
        doSignRecipe.setErrorFlag(true);
        doSignRecipe.setCanContinueFlag("-1");
        doSignRecipe.setMsg(msg);
    }

    public DrugsEnterprise drugsEnterprise(Integer enterpriseId) {
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(enterpriseId);
        return drugsEnterprise;
    }

    /**
     * 获取优先级最高的药企列表
     * @param subDepList
     */
    @LogRecord
    public List<DrugsEnterprise> enterprisePriorityLevel(Integer organId, List<DrugsEnterprise> subDepList){
        Boolean openEnterprisePriorityFlag = configurationClient.getValueBooleanCatch(organId, "openEnterprisePriorityFlag", false);
        if (!openEnterprisePriorityFlag) {
            return subDepList;
        }
        if (CollectionUtils.isEmpty(subDepList)) {
            return subDepList;
        }
        List<Integer> enterpriseIds = subDepList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        List<OrganAndDrugsepRelation> organAndDrugsDepRelationList = organAndDrugsepRelationDAO.findByOrganIdEntId(organId, enterpriseIds);
        if (CollectionUtils.isNotEmpty(subDepList) && CollectionUtils.isNotEmpty(organAndDrugsDepRelationList)) {
            OrganAndDrugsepRelation organAndDrugsepRelation = organAndDrugsDepRelationList.stream().max(Comparator.comparing(enterprise->Optional.ofNullable(enterprise.getPriorityLevel()).orElse(0))).orElseGet(null);
            if (Objects.nonNull(organAndDrugsepRelation)) {
                List<Integer> ids = organAndDrugsDepRelationList.stream().filter(a -> a.getPriorityLevel().equals(organAndDrugsepRelation.getPriorityLevel())).map(OrganAndDrugsepRelation::getDrugsEnterpriseId).collect(Collectors.toList());
                return subDepList.stream().filter(a -> ids.contains(a.getId())).collect(Collectors.toList());
            }
        }
        return subDepList;
    }

    /**
     * 药企对接物流，同步运单信息至基础服务
     *
     * @param order
     * @param logisticsCompany
     * @param trackingNumber
     */
    public void sendLogisticsInfoToBase(RecipeOrder order,Integer recipeId, String logisticsCompany, String trackingNumber) {
        try {
            if (null != order && order.getEnterpriseId() != null) {
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_ENT)) {
                    WriteBackLogisticsOrderDto logisticsOrder = new WriteBackLogisticsOrderDto();
                    // 机构id
                    logisticsOrder.setOrganId(order.getOrganId());
                    // 平台用户id
                    logisticsOrder.setUserId(order.getMpiId());
                    // 业务类型
                    logisticsOrder.setBusinessType(DrugEnterpriseConstant.BUSINESS_TYPE);
                    // 业务编码
                    logisticsOrder.setBusinessNo(order.getOrderCode());
                    // 快递编码
                    logisticsOrder.setLogisticsCode(logisticsCompany);
                    // 运单号
                    logisticsOrder.setWaybillNo(trackingNumber);
                    // 运单费
                    logisticsOrder.setWaybillFee(order.getExpressFee());
                    // 收件人名称
                    logisticsOrder.setAddresseeName(order.getReceiver());
                    // 收件人手机号
                    logisticsOrder.setAddresseePhone(order.getRecMobile());
                    // 收件省份
                    logisticsOrder.setAddresseeProvince(LocalStringUtil.getAddressDic(order.getAddress1()));
                    // 收件城市
                    logisticsOrder.setAddresseeCity(LocalStringUtil.getAddressDic(order.getAddress2()));
                    // 收件镇/区
                    logisticsOrder.setAddresseeDistrict(LocalStringUtil.getAddressDic(order.getAddress3()));
                    // 收件人街道
                    logisticsOrder.setAddresseeStreet(LocalStringUtil.getAddressDic(order.getStreetAddress()));
                    // 收件详细地址
                    logisticsOrder.setAddresseeAddress(order.getAddress4());
                    // 寄托物名称
                    logisticsOrder.setDepositumName(DrugEnterpriseConstant.DEPOSITUM_NAME);
                    if (LOGISTICS_COMPANY_SF.equals(logisticsCompany)) {
                        logisticsOrder.setSfType(2);
                    }
                    PatientDTO patientDTO = patientClient.getPatientDTO(order.getMpiId());
                    if (patientDTO != null) {
                        // 就诊人名称
                        logisticsOrder.setPatientName(patientDTO.getPatientName());
                        // 就诊人手机号
                        logisticsOrder.setPatientPhone(patientDTO.getMobile());
                        // 就诊人身份证
                        String cardNo = StringUtils.isNotBlank(patientDTO.getIdcard()) ? patientDTO.getIdcard() : patientDTO.getIdcard2();
                        if (StringUtils.isNotBlank(cardNo) && cardNo.length() > 18) {
                            cardNo = null;
                        }
                        logisticsOrder.setPatientIdentityCardNo(cardNo);
                    }
                    //设置挂号序号
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                    if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                        logisticsOrder.setOutpatientNumber(recipeExtend.getRegisterID());
                    }
                    logger.info("药企对接物流运单信息回写基础服务，入参={}", JSONObject.toJSONString(logisticsOrder));
                    ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
                    String writeResult = logisticsOrderService.writeBackLogisticsOrder(logisticsOrder);
                    logger.info("药企对接物流运单信息回写基础服务，结果={}", writeResult);
                }
            }
        } catch (Exception e) {
            logger.error("sendLogisticsInfoToBase error ", e);
        }
    }

    /**
     * 推送发药通知短信
     * @param drugsEnterprise
     * @return
     */
    @LogRecord
    public void pushEnterpriseSendDrugPhone(Recipe recipe, DrugsEnterprise drugsEnterprise){
        if (null == drugsEnterprise) {
            return;
        }
        try {
            OrganDrugsSaleConfig organDrugsSaleConfig = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterprise.getId());
            List<String> mobilePhoneList = new ArrayList<>(5);
            if (null != organDrugsSaleConfig && StringUtils.isNotEmpty(organDrugsSaleConfig.getSendDrugNotifyPhone())) {
                mobilePhoneList = Arrays.asList(organDrugsSaleConfig.getSendDrugNotifyPhone().split(","));
            } else {
                if (EnterpriseCreateTypeEnum.MY_SELF.getType().equals(drugsEnterprise.getCreateType())) {
                    List<Pharmacy> list = pharmacyDAO.findByDepId(drugsEnterprise.getId());
                    String mobile = list.get(0).getPharmacyPhone();
                    mobilePhoneList.add(mobile);
                } else {
                    mobilePhoneList.add(drugsEnterprise.getEnterprisePhone());
                }
            }
            logger.info("pushEnterpriseSendDrugPhone mobilePhoneList:{}", JSON.toJSONString(mobilePhoneList));
            if (CollectionUtils.isNotEmpty(mobilePhoneList)) {
                mobilePhoneList.forEach(mobile ->{
                    if (StringUtils.isNotEmpty(mobile)) {
                        Map<String, Object> smsMap = Maps.newHashMap();
                        smsMap.put("mobile", mobile);
                        smsClient.pushSmsInfo(recipe, "RecipeOrderCreate", smsMap);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("pushEnterpriseSendDrugPhone recipeId:{}, error ",recipe.getRecipeId(), e);
        }
    }

    /**
     * 退费推送管理员手机号
     * @param recipe
     * @param drugsEnterprise
     */
    @LogRecord
    public void pushEnterpriseRefundPhone(Recipe recipe, DrugsEnterprise drugsEnterprise){
        if (null == drugsEnterprise) {
            return;
        }
        try {
            OrganDrugsSaleConfig organDrugsSaleConfig = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterprise.getId());
            if (null == organDrugsSaleConfig) {
                return;
            }
            if (StringUtils.isEmpty(organDrugsSaleConfig.getRefundNotifyPhone())) {
                return;
            }
            List<String> mobilePhoneList = Arrays.asList(organDrugsSaleConfig.getRefundNotifyPhone().split(","));
            mobilePhoneList.forEach(mobile ->{
                if (StringUtils.isNotEmpty(mobile)) {
                    Map<String, Object> smsMap = Maps.newHashMap();
                    smsMap.put("mobile", mobile);
                    smsClient.pushSmsInfo(recipe, "RecipeRefundCheckNotify", smsMap);
                }
            });
        } catch (Exception e) {
            logger.error("pushEnterpriseRefundPhone recipeId:{}, error", recipe.getRecipeId(), e);
        }
    }

    @LogRecord
    public void pushEnterpriseFailOrderNotify(Recipe recipe, DrugsEnterprise drugsEnterprise, Integer failCount){
        if (null == drugsEnterprise) {
            return;
        }
        try {
            OrganDrugsSaleConfig organDrugsSaleConfig = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterprise.getId());
            if (null == organDrugsSaleConfig) {
                return;
            }
            if (StringUtils.isEmpty(organDrugsSaleConfig.getOrderPushFailPhone())) {
                return;
            }
            List<String> mobilePhoneList = Arrays.asList(organDrugsSaleConfig.getOrderPushFailPhone().split(","));
            mobilePhoneList.forEach(mobile -> {
                if (StringUtils.isNotEmpty(mobile)) {
                    Map<String, Object> smsMap = Maps.newHashMap();
                    smsMap.put("mobile", mobile);
                    smsMap.put("enterpriseName", drugsEnterprise.getName());
                    smsMap.put("failCount", failCount);
                    smsClient.pushSmsInfo(recipe, "PushEnterpriseFailOrderNotify", smsMap);
                }
            });
        } catch (Exception e) {
            logger.error("pushEnterpriseRefundPhone recipeId:{}, error", recipe.getRecipeId(), e);
        }
    }

    @LogRecord
    public HisResponseTO doCancelRecipeForEnterprise(Recipe recipe) {
        HospitalReqTo req = new HospitalReqTo();
        req.setOrganId(recipe.getClinicOrgan());
        req.setPrescriptionNo(String.valueOf(recipe.getRecipeId()));
        req.setRecipeId(recipe.getRecipeId());
        req.setRecipeCode(recipe.getRecipeCode());
        req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
        req.setOrderCode(recipe.getOrderCode());
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
        if (Objects.nonNull(drugsEnterprise)) {
            req.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        }
        return enterpriseClient.doCancelRecipeForEnterprise(req);
    }

    /**
     * 获取推送药品数据
     *
     * @param enterprise 药企信息
     * @param recipeId   处方id
     * @param organId    机构id
     * @return 推送药品数据
     */
    private List<PushDrugListBean> setSingleRecipeInfo(DrugsEnterprise enterprise, Integer recipeId, Integer organId) {
        //设置配送药品信息
        List<PushDrugListBean> pushDrugListBeans = new ArrayList<>();
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        for (Recipedetail recipedetail : recipeDetails) {
            PushDrugListBean pushDrugListBean = new PushDrugListBean();
            pushDrugListBean.setRecipeDetailBean(ObjectCopyUtils.convert(recipedetail, RecipeDetailBean.class));
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
            if (organDrug != null) {
                pushDrugListBean.setOrganDrugListBean(ObjectCopyUtils.convert(organDrug, OrganDrugListBean.class));
                pushDrugListBean.getRecipeDetailBean().setMedicalDrugCode(organDrug.getMedicalDrugCode());
            }
            if (null != enterprise) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
                if (null != saleDrugList) {
                    pushDrugListBean.setSaleDrugListDTO(ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class));
                }
            }
            pushDrugListBeans.add(pushDrugListBean);
        }
        return pushDrugListBeans;
    }

    /**
     * 设置审方药师信息
     *
     * @param recipe
     * @return 设置审方药师信息
     */
    private RecipeAuditReq recipeAuditReq(Recipe recipe) {
        RecipeAuditReq recipeAuditReq = new RecipeAuditReq();
        if (recipe == null) {
            return recipeAuditReq;
        }
        //设置审方药师信息
        AppointDepartDTO appointDepart = departClient.getAppointDepartByOrganIdAndDepart(recipe);
        //科室代码
        recipeAuditReq.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        recipeAuditReq.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        DoctorExtendDTO doctorExtendDTO = doctorClient.getDoctorExtendDTO(recipe.getChecker());
        if (null != doctorExtendDTO) {
            recipeAuditReq.setMedicalNo(doctorExtendDTO.getMedicalDoctorCode());
        }
        if (!ValidateUtil.integerIsEmpty(recipe.getDoctor())) {
            DoctorDTO doctor = doctorClient.jobNumber(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart());
            recipeAuditReq.setAuditDoctorNo(doctor.getJobNumber());
            recipeAuditReq.setAuditDoctorName(doctor.getName());
        }
        return recipeAuditReq;
    }

    /**
     * 处方扩展信息
     *
     * @param recipe 处方信息
     * @return 处方扩展信息
     */
    private RecipeExtendBean recipeExtend(Recipe recipe) {
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        RecipeExtendBean recipeExtendBean = ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class);
        DoctorExtendDTO doctorExtendDTO = doctorClient.getDoctorExtendDTO(recipe.getDoctor());
        if (null != doctorExtendDTO) {
            recipeExtendBean.setDoctorMedicalNo(doctorExtendDTO.getMedicalDoctorCode());
        }
        //制法Code 煎法Code 中医证候Code
        try {
            if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                recipeExtendBean.setDecoctionCode(decoctionWay.getDecoctionCode());
            }
            if (StringUtils.isNotBlank(recipeExtend.getMakeMethodId())) {
                DrugMakingMethod drugMakingMethod = drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                recipeExtendBean.setMakeMethod(drugMakingMethod.getMethodCode());
            }
            //TODO everyTcmNumFre 不需要推送药企？？？
//            recipeExtendBean.setEveryTcmNumFre(recipeExtend.getEveryTcmNumFre());
            recipeExtend.setRequirementsForTakingText(recipeExtend.getRequirementsForTakingText());
            recipeExtend.setRequirementsForTakingCode(recipeExtend.getRequirementsForTakingCode());
                recipeExtendBean.setSymptomCode(recipeExtend.getSymptomId());
        } catch (Exception e) {
            logger.error("getPushRecipeAndOrder recipe:{} error", recipe.getRecipeId(), e);
        }
        return recipeExtendBean;
    }

    /**
     * 药企扩展信息
     *
     * @param recipe     处方信息
     * @param enterprise 药企信息
     * @return 药企扩展信息
     */
    private ExpandDTO expandDTO(Recipe recipe, DrugsEnterprise enterprise) {
        //设置扩展信息
        ExpandDTO expandDTO = new ExpandDTO();
        String orgCode = patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan());
        if (StringUtils.isNotEmpty(orgCode)) {
            expandDTO.setOrgCode(orgCode);
        }
        if (!ValidateUtil.integerIsEmpty(recipe.getChecker())) {
            DoctorDTO doctor = doctorClient.getDoctor(recipe.getChecker());
            expandDTO.setCheckerName(doctor.getName());
        }
        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
            expandDTO.setSignFile(recipe.getChemistSignFile());
        } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
            expandDTO.setSignFile(recipe.getSignFile());
        }
        try {
            if (StringUtils.isNotBlank(expandDTO.getSignFile())) {
                String imgStr = IMG_HEAD + fileDownloadService.downloadImg(expandDTO.getSignFile());
                expandDTO.setPdfContent(imgStr);
            }
            if (null != enterprise && null != enterprise.getDownSignImgType() && 1 == enterprise.getDownSignImgType()) {
                //获取处方签链接
                String signImgFile = recipeParameterDao.getByName("fileImgUrl");
                expandDTO.setPrescriptionImg(signImgFile + expandDTO.getSignFile());
            } else if (StringUtils.isNotEmpty(recipe.getSignImg())) {
                //设置处方笺base
                String imgStr = IMG_HEAD + fileDownloadService.downloadImg(recipe.getSignImg());
                expandDTO.setPrescriptionImg(imgStr);
            }
        } catch (Exception e) {
            logger.error("getPushRecipeAndOrder 获取处方图片服务异常 recipeId:{}，", recipe.getRecipeId(), e);
        }
        return expandDTO;
    }


    /**
     * 获取指定药企信息 并根据药企指定的购药方式过滤
     *
     * @param deliveryCode
     * @return
     */
    private List<DrugsEnterprise> findEnterpriseListByAppoint(String deliveryCode, Recipe recipe, Integer type) {
        List<DrugsEnterprise> drugsEnterpriseList = new ArrayList<>();
        if (StringUtils.isEmpty(deliveryCode)) {
            throw new DAOException("指定药企为空");
        }
        List<String> ids = Arrays.asList(deliveryCode.split("\\|"));
        List<Integer> collect = ids.stream().map(id -> {
            return Integer.valueOf(id);
        }).collect(Collectors.toList());
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByIds(collect);
        List<OrganAndDrugsepRelation> drugsDepRelationList = organAndDrugsepRelationDAO.findByOrganIdEntId(recipe.getClinicOrgan(), collect);
        Map<Integer, OrganAndDrugsepRelation> drugsDepRelationMap = drugsDepRelationList.stream().collect(Collectors.toMap(OrganAndDrugsepRelation::getDrugsEnterpriseId, a -> a, (k1, k2) -> k1));

        // 根据 药企的购药方式 过滤信息
        String[] recipeSupportGiveMode = recipe.getRecipeSupportGiveMode().split(",");
        List<String> recipeSupportGiveModeList = Arrays.asList(recipeSupportGiveMode);
        logger.info("EnterpriseManager findEnterpriseListByAppoint  drugsEnterprises={},recipeSupportGiveMode={}", JSONUtils.toString(drugsEnterprises), JSONUtils.toString(recipeSupportGiveMode));
        drugsEnterprises.forEach(drugsEnterprise -> {
            OrganAndDrugsepRelation drugsDepRelation = drugsDepRelationMap.get(drugsEnterprise.getId());
            //药企设置支持的购药方式
            String enterpriseSetGiveMode = drugsDepRelation.getDrugsEnterpriseSupportGiveMode();
            if (StringUtils.isEmpty(enterpriseSetGiveMode)) {
                return;
            }
            String[] enterpriseSetGiveModeStr = enterpriseSetGiveMode.split(",");
            List<String> enterpriseSetGiveModeList = Arrays.asList(enterpriseSetGiveModeStr);
            if (enterpriseSetGiveModeList.contains(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType().toString())
                    && recipeSupportGiveModeList.contains(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType().toString())
                    && RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType().equals(type)) {
                // 药企支付到店取药
                drugsEnterpriseList.add(drugsEnterprise);
            }
            if ((enterpriseSetGiveModeList.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString())
                    || enterpriseSetGiveModeList.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString()))
                    && RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().equals(type)) {
                Integer sendType = getEnterpriseSendType(recipe.getClinicOrgan(), drugsEnterprise.getId());
                if (RecipeSendTypeEnum.ALRAEDY_PAY.getSendType().equals(sendType) &&
                        recipeSupportGiveModeList.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString())) {
                    // 医院配送
                    drugsEnterpriseList.add(drugsEnterprise);
                } else if (RecipeSendTypeEnum.NO_PAY.getSendType().equals(sendType) &&
                        recipeSupportGiveModeList.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString())) {
                    // 药企配送
                    drugsEnterpriseList.add(drugsEnterprise);
                }
            }
        });

        logger.info("EnterpriseManager findEnterpriseListByAppoint  res={}", JSONUtils.toString(drugsEnterpriseList));
        return drugsEnterpriseList;
    }

    /**
     * 根据药企名称查询药企列表
     *
     * @param name
     * @return
     */
    public List<DrugsEnterprise> findAllDrugsEnterpriseByName(String name) {
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(name);
        logger.info("EnterpriseManager findAllDrugsEnterpriseByName drugsEnterprises:{}", JSONUtils.toString(drugsEnterprises));
        return drugsEnterprises;
    }

    /**
     * 保存药企机构销售配置
     *
     * @param organDrugsSaleConfig
     */
    public void saveOrganDrugsSaleConfig(OrganDrugsSaleConfig organDrugsSaleConfig) {
        logger.info("EnterpriseManager saveOrganDrugsSaleConfig organDrugsSaleConfig:{}", JSONUtils.toString(organDrugsSaleConfig));
        OrganDrugsSaleConfig organDrugsSaleConfigs = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(organDrugsSaleConfig.getDrugsEnterpriseId());
        if (Objects.isNull(organDrugsSaleConfigs)) {
            organDrugsSaleConfigDAO.save(organDrugsSaleConfig);
        } else {
            organDrugsSaleConfig.setId(organDrugsSaleConfigs.getId());
            organDrugsSaleConfigDAO.updateNonNullFieldByPrimaryKey(organDrugsSaleConfig);
        }
    }

    /**
     * 查询药企配置,这个要看是否走机构配置
     *
     * @param organId
     * @param drugsEnterpriseId
     */
    public OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId, Integer giveMode) {
        logger.info("EnterpriseManager getOrganDrugsSaleConfig organId:{}  drugsEnterpriseId:{} giveMode:{}", organId, drugsEnterpriseId, giveMode);
        // 患者端使用到的机构配置,这个接口仅这些使用
        ArrayList<String> key = Lists.newArrayList("toSendStationFlag", "payModeToHosOnlinePayConfig", "supportToHosPayFlag", "toHosPlanDate",
                "toHosPlanAmTime", "toHosPlanPmTime", "getQrTypeForRecipe", "getQrTypeForRecipeRemind", "isShowPlanTime");
        // 到院自取是否采用药企管理模式
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(organId, "drugToHosByEnterprise", false);
        if (drugToHosByEnterprise && GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType().equals(giveMode)) {
            if (Objects.isNull(drugsEnterpriseId)) {
                throw new DAOException("采用药企销售配置模式药企id不能为空");
            }
            OrganDrugsSaleConfig organDrugsSaleConfig = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterpriseId);
            if (Objects.isNull(organDrugsSaleConfig)) {
                throw new DAOException("未配置药企销售配置");
            }
            organDrugsSaleConfig.setOrganId(organId);
            organDrugsSaleConfig.setDrugsEnterpriseId(drugsEnterpriseId);
            return organDrugsSaleConfig;
        }
        Map<String, Object> configurationByKeyList = configurationClient.getConfigurationByKeyList(organId, key);
        return coverConfig(configurationByKeyList, organId);
    }

    /**
     * 机构配置转换
     *
     * @param configurationByKeyList
     * @return
     */
    private OrganDrugsSaleConfig coverConfig(Map<String, Object> configurationByKeyList, Integer organId) {
        logger.info("EnterpriseManager coverConfig configurationByKeyList:{} ", JSONUtils.toString(configurationByKeyList));
        OrganDrugsSaleConfig organDrugsSaleConfig = new OrganDrugsSaleConfig();
        organDrugsSaleConfig.setOrganId(organId);
        Boolean isSupportSendToStation = (Boolean) configurationByKeyList.get("toSendStationFlag");
        organDrugsSaleConfig.setIsSupportSendToStation(isSupportSendToStation ? 1 : 0);
        organDrugsSaleConfig.setTakeOneselfPaymentChannel((Integer) configurationByKeyList.get("payModeToHosOnlinePayConfig"));
        organDrugsSaleConfig.setTakeOneselfPayment((Boolean) configurationByKeyList.get("supportToHosPayFlag") ? 1 : 2);
        Boolean isShowPlanTime = (Boolean) configurationByKeyList.get("isShowPlanTime");
        if (isShowPlanTime) {
            String[] toHosPlanDate = (String[]) configurationByKeyList.get("toHosPlanDate");
            organDrugsSaleConfig.setTakeOneselfPlanDate(Integer.parseInt(toHosPlanDate[0]));
        } else {
            organDrugsSaleConfig.setTakeOneselfPlanDate(0);
        }
        organDrugsSaleConfig.setTakeOneselfPlanAmTime(configurationByKeyList.get("toHosPlanAmTime").toString());
        organDrugsSaleConfig.setTakeOneselfPlanPmTime(configurationByKeyList.get("toHosPlanPmTime").toString());
        organDrugsSaleConfig.setTakeDrugsVoucher((Integer) configurationByKeyList.get("getQrTypeForRecipe"));
        Object getQrTypeForRecipeRemind = configurationByKeyList.get("getQrTypeForRecipeRemind");
        if (Objects.nonNull(getQrTypeForRecipeRemind)) {
            organDrugsSaleConfig.setSpecialTips(getQrTypeForRecipeRemind.toString());
        }
        logger.info("EnterpriseManager coverConfig organDrugsSaleConfig:{} ", JSONUtils.toString(organDrugsSaleConfig));
        return organDrugsSaleConfig;
    }

    public List<Pharmacy> pharmacy() {
        return pharmacyDAO.find1();
    }

    /**
     * 先删除所有机构药企煎法关联的地址
     * @param organId
     * @param enterpriseId
     * @param decoctionId
     */
    public void deleteEnterpriseDecoctionAddress(Integer organId, Integer enterpriseId, Integer decoctionId) {
        enterpriseDecoctionAddressDAO.deleteEnterpriseDecoctionAddress(organId,enterpriseId,decoctionId);
    }

    /**
     * 更新机构药企煎法关联的地址
     * @param enterpriseDecoctionAddresses
     */
    public void addEnterpriseDecoctionAddressList(List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses) {
        enterpriseDecoctionAddressDAO.addEnterpriseDecoctionAddressList(enterpriseDecoctionAddresses);
    }

    /**
     * 查询药企煎法地址
     * @param organId
     * @param enterpriseId
     * @param decoctionId
     * @return
     */
    public List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(Integer organId, Integer enterpriseId, Integer decoctionId,String area) {
        if (StringUtils.isEmpty(area)) {
            return enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressList(organId, enterpriseId, decoctionId);
        } else {
            return enterpriseDecoctionAddressDAO.findEnterpriseDecoctionAddressListByArea(organId, enterpriseId, decoctionId,area);
        }
    }

    public OrganDrugsSaleConfig getOrganDrugsSaleConfigOfPatient(Integer organId, Integer drugsEnterpriseId) {
        logger.info("getOrganDrugsSaleConfigOfPatient organId:{},drugsEnterpriseId:{} ",organId,drugsEnterpriseId );
        return organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(drugsEnterpriseId);
    }

    /**
     * 获取药企配送主体
     * @param organId
     * @param drugsEnterpriseId
     * @return
     */
    @LogRecord
    public Integer getEnterpriseSendType(Integer organId, Integer drugsEnterpriseId) {
        if (Objects.isNull(organId) || Objects.isNull(drugsEnterpriseId)){
            throw new DAOException("入参错误");
        }
        OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(organId, drugsEnterpriseId);
        if (Objects.isNull(relation)) {
            return null;
        }
        String drugsEnterpriseSupportGiveMode = relation.getDrugsEnterpriseSupportGiveMode();
        if (StringUtils.isEmpty(drugsEnterpriseSupportGiveMode)) {
            return null;
        }
        String[] split = drugsEnterpriseSupportGiveMode.split(",");
        List<String> list = Arrays.asList(split);
        Integer sendType = null;
        if (list.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString())) {
            sendType = RecipeSendTypeEnum.ALRAEDY_PAY.getSendType();
        } else if (list.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString())) {
            sendType = RecipeSendTypeEnum.NO_PAY.getSendType();
        }
        return sendType;
    }

    /**
     * 将患者选择的购药方式转化为药企对应支持的方式
     * @param selectBuyMedicineWay 患者选择的购药方式
     * @return
     */
    public RecipeSupportGiveModeEnum getDepSupportMode(Integer selectBuyMedicineWay) {
        RecipeSupportGiveModeEnum supportGiveModeEnum = RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS;
        switch (selectBuyMedicineWay) {
            case 1:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS;
                break;
            case 2:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES;
                break;
            case 3:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SUPPORT_TO_HOS;
                break;
            case 4:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SUPPORT_TFDS;
                break;
            case 6:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE;
                break;
        }
        return supportGiveModeEnum;
    }

    /**
     * 根据payMode获取支持的购药方式
     * @param payMode
     * @return
     */
    public RecipeSupportGiveModeEnum getDepSupportModeByPayMode(Integer payMode){
        RecipeSupportGiveModeEnum supportGiveModeEnum = null;
        switch (payMode) {
            case 1:
            case 2:
            case 5:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS;
                break;
            case 3:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SUPPORT_TO_HOS;
                break;
            case 4:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.SUPPORT_TFDS;
                break;
            case 6:
                supportGiveModeEnum = RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE;
                break;
        }
        return supportGiveModeEnum;
    }

    /**
     * 校验药企支持的代煎方式
     * @param recipeList
     * @param depId
     * @param patientIsDecoction
     */
    public void checkSupportDecoction(List<Recipe> recipeList, Integer depId, Integer patientIsDecoction, Integer giveMode) {
        // 检验是中药处方
        List<Integer> list = recipeList.stream().map(Recipe::getRecipeType).collect(Collectors.toList());
        if (list.contains(RecipeBussConstant.RECIPETYPE_TCM)) {
            // 中药处方代煎需要校验药企是否支持配送代煎
            OrganAndDrugsepRelation relation = organAndDrugsepRelationDAO.getOrganAndDrugsepByOrganIdAndEntId(recipeList.get(0).getClinicOrgan(), depId);
            if (Objects.isNull(relation)) {
                return;
            }
            String supportDecoctionState = "";
            if (GiveModeTextEnum.SENDTOHOS.getGiveMode().equals(giveMode)) {
                supportDecoctionState = relation.getSupportDecoctionState();
            } else {
                supportDecoctionState = relation.getSupportSelfDecoctionState();
            }
            if (StringUtils.isNotEmpty(supportDecoctionState)) {
                logger.info("checkSupportDecoction SupportDecoctionState ={}  patientIsDecoction={}", supportDecoctionState, patientIsDecoction);
                List<Integer> supportDecoctionType = JSONUtils.parse(supportDecoctionState, List.class);
                if (supportDecoctionType.contains(patientIsDecoction)) {
                    throw new DAOException(609, "当前代煎类型不支持该购药方式，请换一种购药方式");
                }
            }
        }
    }
}

