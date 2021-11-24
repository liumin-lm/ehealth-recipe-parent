package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.EnterpriseClient;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.*;
import recipe.third.IFileDownloadService;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
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
    private SymptomDAO symptomDAO;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private DrugStockClient drugStockClient;

    /**
     * 检查 药企药品 是否满足开方药品
     * 验证能否药品配送以及能否开具到一张处方单上
     *
     * @param enterpriseIds 药企id
     * @param recipeDetails 处方明显-开方药品
     * @return 药企-不满足的 药品名称
     */
    public Map<Integer, List<String>> checkEnterpriseDrugName(List<Integer> enterpriseIds, List<Recipedetail> recipeDetails) {
        List<String> nameList = new LinkedList<>();
        List<Integer> drugIds = recipeDetails.stream().map(a -> {
            nameList.add(a.getDrugName());
            return a.getDrugId();
        }).collect(Collectors.toList());
        Map<Integer, List<Integer>> enterpriseDrugIdGroup = saleDrugListDAO.findDepDrugRelation(drugIds, enterpriseIds);
        logger.info("DrugStockManager enterpriseDrugNameGroup enterpriseDrugIdGroup= {}", JSON.toJSONString(enterpriseDrugIdGroup));

        Map<Integer, List<String>> enterpriseDrugNameGroup = new HashMap<>();
        enterpriseIds.forEach(a -> {
            List<Integer> drugIdList = enterpriseDrugIdGroup.get(a);
            if (CollectionUtils.isEmpty(drugIdList)) {
                enterpriseDrugNameGroup.put(a, nameList);
                return;
            }
            List<String> names = recipeDetails.stream().filter(recipeDetail -> !drugIdList.contains(String.valueOf(recipeDetail.getDrugId()))).map(Recipedetail::getDrugName).collect(Collectors.toList());
            enterpriseDrugNameGroup.put(a, names);
        });
        logger.info("DrugStockManager enterpriseDrugNameGroup enterpriseDrugNameGroup= {}", JSON.toJSONString(enterpriseDrugNameGroup));
        return enterpriseDrugNameGroup;
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
        List<DrugsEnterprise> enterprise = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        return CollectionUtils.isNotEmpty(enterprise);
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
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
        return drugStockClient.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails, saleDrugLists);
    }


    /**
     * 到店取药 药企获取
     *
     * @param recipe
     * @param payModeSupport
     * @return
     */
    public List<DrugsEnterprise> findEnterpriseByTFDS(Recipe recipe, List<Integer> payModeSupport) {
        logger.info("EnterpriseManager findEnterpriseByTFDS req payModeSupport:{},recipe:{}", JSONUtils.toString(payModeSupport), JSONUtils.toString(recipe));

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
                drugsEnterpriseList = findEnterpriseListByAppoint(deliveryCode, recipe,RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
                break;
            case DEFAULT:
            default:
                drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), payModeSupport);
                break;
        }
        logger.info("EnterpriseManager findEnterpriseByTFDS  res={}", JSONUtils.toString(drugsEnterpriseList));
        return drugsEnterpriseList;

    }

    /**
     * 配送到家模式下获取药企
     *
     * @param sendType
     * @param payModeSupport
     * @param recipe
     * @return
     */
    public List<DrugsEnterprise> findEnterpriseByOnLine(String sendType, List<Integer> payModeSupport, Recipe recipe) {
        logger.info("EnterpriseManager findEnterpriseByOnLine req sendType:{},payModeSupport:{},recipe:{}", sendType, JSONUtils.toString(payModeSupport), JSONUtils.toString(recipe));
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
                drugsEnterpriseList = findEnterpriseListByAppoint(deliveryCode, recipe,RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
                break;
            case DEFAULT:
            default:
                if (StringUtils.isNotEmpty(sendType)) {
                    if (Integer.valueOf(1).equals(recipe.getRecipeSource())) {
                        drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndOtherAndSendType(recipe.getClinicOrgan(), payModeSupport, Integer.parseInt(sendType));
                    } else {
                        drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, Integer.parseInt(sendType));
                    }
                } else {
                    //考虑到浙江省互联网项目的药店取药也会走这里,sendType是"" 还是需要查询一下支持的药企
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), payModeSupport);
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
    public SkipThirdDTO uploadRecipeInfoToThird(Integer organId, String giveMode, List<Integer> recipeIds) {
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
            result = pushRecipeForThird(recipe, 1);
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
    public SkipThirdDTO pushRecipeForThird(Recipe recipe, Integer node) {
        logger.info("EnterpriseManager pushRecipeForThird recipeId:{}, node:{}.", recipe.getRecipeId(), node);
        SkipThirdDTO result = new SkipThirdDTO();
        result.setCode(1);
        List<DrugsEnterprise> drugsEnterpriseList = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            return result;
        }
        for (DrugsEnterprise drugsEnterprise : drugsEnterpriseList) {
            try {
                //todo 只用最后一个返回？
                if (1 == drugsEnterprise.getOperationType() && ENTERPRISE_BAN_QUE.equals(drugsEnterprise.getAccount())) {
                    result = pushRecipeInfoForThird(recipe, drugsEnterprise, node);
                }
            } catch (Exception e) {
                logger.error("EnterpriseManager pushRecipeForThird error ", e);
            }
        }
        logger.info("EnterpriseManager pushRecipeForThird result:{}", JSONUtils.toString(result));
        return result;
    }

    /**
     * 推送药企信息去前置机
     *
     * @param recipe     处方信息
     * @param enterprise 药企
     * @param node
     * @return
     */
    public SkipThirdDTO pushRecipeInfoForThird(Recipe recipe, DrugsEnterprise enterprise, Integer node) {
        logger.info("RemoteDrugEnterpriseService pushRecipeInfoForThird recipeId:{},enterprise:{},node:{}.", recipe.getRecipeId(), JSONUtils.toString(enterprise), node);
        //传过来的处方不是最新的需要重新从数据库获取
        Recipe recipeNew = recipeDAO.getByRecipeId(recipe.getRecipeId());
        //todo 为什么赋值新的GiveMode 却没有更新到数据库？
        recipeNew.setGiveMode(recipe.getGiveMode());
        //通过前置机进行推送
        PushRecipeAndOrder pushRecipeAndOrder = getPushRecipeAndOrder(recipeNew, enterprise);
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
        Executors.newSingleThreadExecutor().execute(() -> enterpriseClient.uploadRecipePdfToHis(recipeNew));
        return skipThirdDTO;
    }

    /**
     * 前置机推送药企数据组织
     *
     * @param recipe     处方信息
     * @param enterprise 药企信息
     * @return
     */
    public PushRecipeAndOrder getPushRecipeAndOrder(Recipe recipe, DrugsEnterprise enterprise) {
        PushRecipeAndOrder pushRecipeAndOrder = new PushRecipeAndOrder();
        pushRecipeAndOrder.setOrganId(recipe.getClinicOrgan());
        //设置订单信息
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            enterpriseClient.addressBean(recipeOrder, pushRecipeAndOrder);
        }
        //设置医生信息
        pushRecipeAndOrder.setDoctorDTO(doctorClient.jobNumber(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart()));
        //设置审方药师信息
        pushRecipeAndOrder.setRecipeAuditReq(recipeAuditReq(recipe.getClinicOrgan(), recipe.getChecker(), recipe.getDepart()));
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
        //设置科室信息
        pushRecipeAndOrder.setDepartmentDTO(organClient.departmentDTO(recipe.getDepart()));

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
                if (revisitExDTO != null) {
                    recipeBean.setPatientChannelId(revisitExDTO.getProjectChannel());
                }
            } catch (Exception e) {
                logger.error("getPushRecipeAndOrder queryPatientChannelId error", e);
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
     * @param object
     * @param msg
     * @return
     */
    public void doSignRecipe(DoSignRecipeDTO doSignRecipe, Object object, String msg) {
        doSignRecipe.setSignResult(false);
        doSignRecipe.setErrorFlag(true);
        doSignRecipe.setCanContinueFlag("-1");
        if (null != object) {
            List<String> nameList = (List<String>) object;
            if (CollectionUtils.isNotEmpty(nameList)) {
                String nameStr = "【" + Joiner.on("、").join(nameList) + "】";
                msg = "由于该处方单上的" + nameStr + msg;
            }
        }
        doSignRecipe.setMsg(msg);
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
     * @param organId  机构id
     * @param doctorId 医生id
     * @param departId 开方科室id
     * @return 设置审方药师信息
     */
    private RecipeAuditReq recipeAuditReq(Integer organId, Integer doctorId, Integer departId) {
        RecipeAuditReq recipeAuditReq = new RecipeAuditReq();
        //设置审方药师信息
        AppointDepartDTO appointDepart = organClient.departDTO(organId, departId);
        //科室代码
        recipeAuditReq.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        recipeAuditReq.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        if (!ValidateUtil.integerIsEmpty(doctorId)) {
            DoctorDTO doctor = doctorClient.jobNumber(organId, doctorId, departId);
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
            if (StringUtils.isNotBlank(recipeExtend.getSymptomId())) {
                Symptom symptom = symptomDAO.get(recipeExtend.getSymptomId());
                recipeExtendBean.setSymptomCode(symptom.getSymptomCode());
            }
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

        // 根据 药企的购药方式 过滤信息
        String[] recipeSupportGiveMode = recipe.getRecipeSupportGiveMode().split(",");
        List<String> strings = Arrays.asList(recipeSupportGiveMode);
        logger.info("EnterpriseManager findEnterpriseListByAppoint  drugsEnterprises={},recipeSupportGiveMode={}", JSONUtils.toString(drugsEnterprises), JSONUtils.toString(recipeSupportGiveMode));
        drugsEnterprises.forEach(drugsEnterprise -> {
            if (RecipeDistributionFlagEnum.drugsEnterpriseAll.contains(drugsEnterprise.getPayModeSupport())) {
                drugsEnterpriseList.add(drugsEnterprise);
            } else if (RecipeDistributionFlagEnum.drugsEnterpriseTo.contains(drugsEnterprise.getPayModeSupport())
                    && strings.contains(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType().toString())
                    && RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType().equals(type)) {

                // 药企支付到店取药
                drugsEnterpriseList.add(drugsEnterprise);
            } else if (RecipeDistributionFlagEnum.drugsEnterpriseSend.contains(drugsEnterprise.getPayModeSupport())
                    && RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().equals(type)) {

                if (RecipeSendTypeEnum.ALRAEDY_PAY.getSendType().equals(drugsEnterprise.getSendType()) &&
                        strings.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString())) {
                    // 医院配送
                    drugsEnterpriseList.add(drugsEnterprise);
                } else if (RecipeSendTypeEnum.NO_PAY.getSendType().equals(drugsEnterprise.getSendType()) &&
                        strings.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString())) {
                    // 药企配送
                    drugsEnterpriseList.add(drugsEnterprise);
                }
            }
        });

        logger.info("EnterpriseManager findEnterpriseListByAppoint  res={}", JSONUtils.toString(drugsEnterpriseList));
        return drugsEnterpriseList;
    }


}

