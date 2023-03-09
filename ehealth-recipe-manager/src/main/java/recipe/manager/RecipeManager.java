package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.HisOrderCodeResTO;
import com.ngari.his.visit.mode.RecipeChargeItemCodeReqTo;
import com.ngari.his.visit.mode.RecipeChargeItemCodeResTo;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.exception.DAOException;
import ctd.util.FileAuth;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.DocIndexClient;
import recipe.client.RecipeAuditClient;
import recipe.client.RecipeHisClient;
import recipe.client.factory.recipedate.RecipeDataSaveFactory;
import recipe.common.CommonConstant;
import recipe.common.OnsConfig;
import recipe.common.UrlConfig;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RequirementsForTakingDao;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.enumerate.type.AppointEnterpriseTypeEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeShowQrConfigEnum;
import recipe.util.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方
 *
 * @author yinsheng
 * @date 2021\6\30 0030 14:21
 */
@Service
public class RecipeManager extends BaseManager {

    private static final String REVISIT_SOURCE_YZSQ = "fz-yzsq-001";

    private static final String REVISIT_SOURCE_YJXF = "fz-yjxf-001";

    private static final String REVISIT_SOURCE_BJGY = "fz-bjgy-001";
    @Autowired
    private DocIndexClient docIndexClient;
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    /**
     * todo 什么情况？
     */
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private IRecipeCheckService iRecipeCheckService;
    @Autowired
    private RequirementsForTakingDao requirementsForTakingDao;
    @Autowired
    private RecipeHisClient recipeHisClient;
    @Autowired
    private RecipeDataSaveFactory recipeDataSaveFactory;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private RedisClient redisClient;


    /**
     * 保存处方信息
     *
     * @param recipe 处方信息
     * @return
     */
    public Recipe saveRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getRecipeId())) {
            recipe.setCreateDate(new Date());
            recipe = recipeDAO.save(recipe);
        } else {
            recipe = recipeDAO.update(recipe);
        }
        logger.info("RecipeManager saveRecipe recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }

    /**
     * 保存处方信息
     * 设置处方默认数据
     *
     * @param recipe 前端传入的处方对象
     */
    public Recipe saveStagingRecipe(Recipe recipe) {
        if (null == recipe) {
            throw new DAOException("recipe不能为空");
        }
        recipeDataSaveFactory.setRecipeList(recipe);
        return this.saveRecipe(recipe);
    }

    /**
     * 保存处方扩展信息
     *
     * @param extend 扩展信息
     * @param recipe 处方信息
     */
    public void saveStagingRecipeExt(RecipeExtend extend, Recipe recipe) {
        if (null == extend) {
            return;
        }
        recipeDataSaveFactory.setRecipeExtList(recipe, extend);
        this.saveRecipeExtend(extend, recipe.getRecipeId());
    }

    /**
     * 保存处方扩展信息 诊疗处方
     *
     * @param recipeExtend 处方扩展信息
     * @param recipe       处方信息
     * @return
     */
    public RecipeExtend saveRecipeExtend(RecipeExtend recipeExtend, Recipe recipe) {
        if (!ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            recipeExtend.setCardNo(revisitExDTO.getCardId());
        }
        return this.saveRecipeExtend(recipeExtend, recipe.getRecipeId());
    }


    private RecipeExtend saveRecipeExtend(RecipeExtend recipeExtend, Integer recipeId) {
        if (ValidateUtil.integerIsEmpty(recipeExtend.getRecipeId())) {
            recipeExtend.setRecipeId(recipeId);
            recipeExtend = recipeExtendDAO.save(recipeExtend);
        } else {
            recipeExtend = recipeExtendDAO.update(recipeExtend);
        }
        logger.info("RecipeManager saveRecipeExtend recipeExtend:{}", JSONUtils.toString(recipeExtend));
        return recipeExtend;
    }


    /**
     * 获取处方信息
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    public Recipe getByRecipeCodeAndClinicOrgan(String recipeCode, Integer clinicOrgan) {
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan param recipeCode:{},clinicOrgan:{}", recipeCode, clinicOrgan);
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan res recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }


    /**
     * 查询处方信息
     *
     * @param recipeId
     * @return
     */
    public Recipe getRecipeById(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (StringUtils.isEmpty(recipe.getOrganDiseaseId())) {
            RecipeExtend recipeExtend = this.recipeExtend(recipeId);
            EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(recipeExtend.getDocIndexId());
            recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
            recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
            recipe.setMemo(emrDetail.getMemo());
        }
        return recipe;
    }

    public List<Recipe> findByRecipeIds(List<Integer> recipeIds) {
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        logger.info("RecipeManager findByRecipeIds recipeIds:{}, recipes:{}", JSON.toJSONString(recipeIds), JSON.toJSONString(recipes));
        return recipes;
    }

    public RecipeExtend recipeExtend(Integer recipeId) {
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        logger.info("RecipeManager recipeExtend recipeExtend:{}", JSON.toJSONString(recipeExtend));
        return recipeExtend;
    }


    /**
     * 根据业务类型(咨询/复诊)和业务单号(咨询/复诊单号)获取处方信息
     *
     * @param bussSource 咨询/复诊
     * @param clinicId   咨询/复诊单号
     * @return 处方列表
     */
    public List<Recipe> findWriteHisRecipeByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        logger.info("RecipeManager findWriteHisRecipeByBussSourceAndClinicId param bussSource:{},clinicId:{}", bussSource, clinicId);
        List<Recipe> recipes = recipeDAO.findWriteHisRecipeByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findWriteHisRecipeByBussSourceAndClinicId recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }

    /**
     * 获取有效的处方单
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    public List<Recipe> findEffectiveRecipeByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        logger.info("RecipeManager findRecipeByBussSourceAndClinicId param bussSource:{},clinicId:{}", bussSource, clinicId);
        List<Recipe> recipes = recipeDAO.findEffectiveRecipeByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findEffectiveRecipeByBussSourceAndClinicId recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }


    /**
     * 获取处方相关信息 并且 字典转换
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeInfoDictionary(Integer recipeId) {
        RecipeInfoDTO recipeInfoDTO = getRecipeInfoDTO(recipeId);
        PatientDTO patientBean = recipeInfoDTO.getPatientBean();
        if (StringUtils.isNotEmpty(patientBean.getPatientSex())) {
            patientBean.setPatientSex(DictionaryUtil.getDictionary("eh.base.dictionary.Gender", String.valueOf(patientBean.getPatientSex())));
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null != recipeExtend.getWeight()) {
            patientBean.setWeight(String.valueOf(recipeExtend.getWeight()));
        } else {
            patientBean.setWeight("");
        }
        return recipeInfoDTO;
    }

    public RecipeDTO getRecipe(Integer recipeId) {
        return super.getRecipeDTO(recipeId);
    }

    /**
     * 获取处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeInfoDTO(Integer recipeId) {
        RecipeDTO recipeDTO = getRecipeDTO(recipeId);
        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        BeanUtils.copyProperties(recipeDTO, recipeInfoDTO);
        Recipe recipe = recipeInfoDTO.getRecipe();
        PatientDTO patientBean = patientClient.getPatientEncipher(recipe.getMpiid());
        recipeInfoDTO.setPatientBean(patientBean);
        logger.info("RecipeOrderManager getRecipeInfoDTO patientBean:{}", JSON.toJSONString(patientBean));
        return recipeInfoDTO;
    }

    /**
     * 获取处方相关信息 简要数据
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeDTO getRecipeDTOSimple(Integer recipeId) {
        RecipeDTO recipeDTO = getRecipeDTO(recipeId);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipeDTO.getRecipe().getOrderCode());
        recipeDTO.setRecipeOrder(recipeOrder);
        return recipeDTO;
    }

    /**
     * 获取处方相关信息 补全数据
     *
     * @param recipeId 处方id
     * @return
     */
    @Override
    public RecipeDTO getRecipeDTO(Integer recipeId) {
        RecipeDTO recipeDTO = super.getRecipeDTO(recipeId);
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        if (null == recipeExtend) {
            return recipeDTO;
        }
        recipeExtend.setCardTypeName(DictionaryUtil.getDictionary("eh.mpi.dictionary.CardType", recipeExtend.getCardType()));
        Integer docIndexId = recipeExtend.getDocIndexId();
        EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(docIndexId);
        if (StringUtils.isEmpty(emrDetail.getOrganDiseaseId())) {
            return recipeDTO;
        }
        Recipe recipe = recipeDTO.getRecipe();
        recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
        recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
        recipe.setMemo(emrDetail.getMemo());
        recipeExtend.setSymptomId(emrDetail.getSymptomId());
        recipeExtend.setSymptomName(emrDetail.getSymptomName());
        recipeExtend.setAllergyMedical(emrDetail.getAllergyMedical());
        if (!ValidateUtil.integerIsEmpty(recipe.getClinicId()) && StringUtils.isEmpty(recipeExtend.getCardNo())) {
            RevisitExDTO consultExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (null != consultExDTO) {
                recipeExtend.setCardNo(consultExDTO.getCardId());
                recipeExtend.setCardType(consultExDTO.getCardType());
            }
        }
        //当卡类型是医保卡的时候，调用端的配置判断是否开启，如果开启，则调用端提供的工具进行卡号的展示
        if ("2".equals(recipeExtend.getCardType())) {
            boolean hospitalCardLengthControl = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "hospitalCardLengthControl", false);
            if (hospitalCardLengthControl && StringUtils.isNotBlank(recipeExtend.getCardNo()) && recipeExtend.getCardNo().length() == 28) {
                recipeExtend.setCardNo(recipeExtend.getCardNo().substring(0, 10));
            }
        }
        if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
            if (null != decoctionWay) {
                recipeExtend.setDecoctionPrice(decoctionWay.getDecoctionPrice());
            }
        }
        return recipeDTO;
    }


    /**
     * 获取到院取药凭证
     *
     * @param recipe       处方信息
     * @param recipeExtend 处方扩展信息
     * @return 取药凭证
     */
    public String getToHosProof(Recipe recipe, RecipeExtend recipeExtend, RecipeOrder order) {
        logger.info("getToHosProof recipe:{} order:{}", JSONArray.toJSONString(recipe), JSONArray.toJSONString(order));
        String qrName = "";
        try {
            if (Objects.isNull(order)) {
                return "";
            }
            OrganDrugsSaleConfig organDrugsSaleConfig = enterpriseManager.getOrganDrugsSaleConfig(recipe.getClinicOrgan(), order.getEnterpriseId(), recipe.getGiveMode());
            RecipeShowQrConfigEnum qrConfigEnum = RecipeShowQrConfigEnum.getEnumByType(organDrugsSaleConfig.getTakeDrugsVoucher());
            switch (qrConfigEnum) {
                case CARD_NO:
                    //就诊卡号
                    if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
                        qrName = recipeExtend.getCardNo();
                    }
                    break;
                case REGISTER_ID:
                    if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                        qrName = recipeExtend.getRegisterID();
                    }
                    break;
                case PATIENT_ID:
                    if (StringUtils.isNotEmpty(recipe.getPatientID())) {
                        qrName = recipe.getPatientID();
                    }
                    break;
                case MEDICAL_RECORD_NUMBER:
                    //病历号
                    if (StringUtils.isNotEmpty(recipeExtend.getMedicalRecordNumber())) {
                        qrName = recipeExtend.getMedicalRecordNumber();
                    }
                    break;
                case RECIPE_CODE:
                    if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                        qrName = recipe.getRecipeCode();
                    }
                    break;
                case TAKE_DRUG_CODE:
                    qrName = offlineRecipeClient.queryMedicineCode(recipe.getClinicOrgan(), recipe.getRecipeId(), recipe.getRecipeCode());
                    break;
                case SERIALNUMBER:
                    qrName = offlineRecipeClient.queryRecipeSerialNumber(recipe.getClinicOrgan(), recipe.getPatientName(), recipe.getPatientID(), recipeExtend.getRegisterID(), recipe.getRecipeCode());
                    break;
                case CERTIFICATE:
                    PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
                    qrName = patientDTO.getIdcard();
                    break;
                case SERIALNUMBER_TAKE_DRUG_CODE:
                    //优先取药柜发药流水号，药柜流水号没有，取his医院流水号
                    qrName = recipeExtend.getMedicineCode();
                    if(StringUtils.isEmpty(qrName)){
                        qrName = offlineRecipeClient.queryRecipeSerialNumber(recipe.getClinicOrgan(), recipe.getPatientName(), recipe.getPatientID(), recipeExtend.getRegisterID(), recipe.getRecipeCode());
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("RecipeManager getToHosProof error", e);
        }
        return qrName;
    }

    /**
     * 获取医生撤销处方时间和原因
     *
     * @param recipeId
     * @return
     */
    public RecipeCancelDTO getCancelReasonForPatient(int recipeId) {
        logger.info("getCancelReasonForPatient recipeCancel recipeId:{}", recipeId);
        RecipeCancelDTO recipeCancel = new RecipeCancelDTO();
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (StringUtils.isNotEmpty(recipeExtend.getCancellation())) {
            recipeCancel.setCancelReason(recipeExtend.getCancellation());
            recipeCancel.setCancelDate(new Date());
            return recipeCancel;
        }
        return recipeCancel;
    }

    /**
     * 根据订单号查询处方列表
     *
     * @param orderCode orderCode
     * @return List<Recipe>
     */
    public List<Recipe> findRecipeByOrderCode(String orderCode) {
        return recipeDAO.findRecipeListByOrderCode(orderCode);
    }

    /**
     * 更新推送his返回信息处方数据
     *
     * @param recipeResult 处方结果
     * @param recipeId     处方id
     * @param pushType     推送类型: 1：提交处方，2:撤销处方
     */
    public void updatePushHisRecipe(Recipe recipeResult, Integer recipeId, Integer pushType) {
        if (null == recipeResult) {
            return;
        }
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        if (!CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            //表示为撤销处方
            updateRecipe.setWriteHisState(WriteHisEnum.NONE.getType());
            recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
            return;
        }

        //如果处方来源是复诊，则patientID取复诊的
        updateRecipe.setPatientID(recipeResult.getPatientID());
        if (new Integer(2).equals(recipeResult.getBussSource())) {
            RevisitBean revisitBean = revisitClient.getRevisitByClinicId(recipeResult.getClinicId());
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipeResult.getClinicId());
            if(Objects.nonNull(revisitBean)) {
                //设置处方入口类型
                String sourceTag = Objects.isNull(revisitBean.getSourceTag()) ? "" : revisitBean.getSourceTag();
                switch (sourceTag) {
                    case REVISIT_SOURCE_BJGY:
                        updateRecipe.setFastRecipeFlag(1);
                        break;
                    case REVISIT_SOURCE_YZSQ:
                        updateRecipe.setFastRecipeFlag(2);
                        break;
                    case REVISIT_SOURCE_YJXF:
                        updateRecipe.setFastRecipeFlag(3);
                        break;
                    default:
                        updateRecipe.setFastRecipeFlag(0);
                        break;
                }
            }

            if (null != revisitExDTO && StringUtils.isNotEmpty(revisitExDTO.getPatId())) {
                updateRecipe.setPatientID(revisitExDTO.getPatId());
            }
        }
        updateRecipe.setRecipeCode(recipeResult.getRecipeCode());
        updateRecipe.setWriteHisState(WriteHisEnum.WRITE_HIS_STATE_ORDER.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        logger.info("RecipeManager updatePushHisRecipe updateRecipe:{}.", JSON.toJSONString(updateRecipe));
    }

    /**
     * 更新推送his返回信息处方扩展数据
     *
     * @param recipeExtendResult 处方扩展结果
     * @param recipeId           处方id
     * @param pushType           推送类型: 1：提交处方，2:撤销处方
     */
    public void updatePushHisRecipeExt(RecipeExtend recipeExtendResult, Integer recipeId, Integer pushType) {
        if (null == recipeExtendResult) {
            return;
        }
        if (!CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            return;
        }
        RecipeExtend updateRecipeExt = new RecipeExtend();
        updateRecipeExt.setRecipeId(recipeId);
        updateRecipeExt.setRegisterID(recipeExtendResult.getRegisterID());
        updateRecipeExt.setMedicalType(recipeExtendResult.getMedicalType());
        updateRecipeExt.setMedicalTypeText(recipeExtendResult.getMedicalTypeText());
        updateRecipeExt.setRecipeCostNumber(recipeExtendResult.getRecipeCostNumber());
        updateRecipeExt.setHisDiseaseSerial(recipeExtendResult.getHisDiseaseSerial());
        updateRecipeExt.setHisOrderCode(recipeExtendResult.getHisOrderCode());
        updateRecipeExt.setHisBusId(recipeExtendResult.getHisBusId());
        recipeExtendDAO.updateNonNullFieldByPrimaryKey(updateRecipeExt);
        logger.info("RecipeManager updatePushHisRecipeExt updateRecipeExt:{}.", JSON.toJSONString(updateRecipeExt));
    }

    /**
     * 校验开处方单数限制
     * todo 废弃 其他接口中的引用 ，目前提供前端调用接口，暂时保留老代码支持兼容老app使用
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @return true 可开方
     */
    @Deprecated
    public Boolean isOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId) {
        logger.info("RecipeManager isOpenRecipeNumber clinicId: {},organId: {}", clinicId, organId);
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return true;
        }
        //运营平台没有处方单数限制，默认可以无限进行开处方
        Integer openRecipeNumber = configurationClient.getValueCatch(organId, "openRecipeNumber", 99);
        logger.info("RecipeManager isOpenRecipeNumber openRecipeNumber={}", openRecipeNumber);
        if (ValidateUtil.integerIsEmpty(openRecipeNumber)) {
            saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "开方张数已超出医院限定范围，不能继续开方。");
            throw new DAOException(ErrorCode.SERVICE_ERROR, "开方张数0已超出医院限定范围，不能继续开方。");
        }
        //查询当前复诊存在的有效处方单
        List<Recipe> recipeCount = recipeDAO.findRecipeClinicIdAndStatus(clinicId, RecipeStatusEnum.RECIPE_REPEAT_COUNT);
        if (CollectionUtils.isEmpty(recipeCount)) {
            return true;
        }
        List<Integer> recipeIds;
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            recipeIds = recipeCount.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        } else {
            recipeIds = recipeCount.stream().filter(a -> !a.getRecipeId().equals(recipeId)).map(Recipe::getRecipeId).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            return true;
        }
        logger.info("RecipeManager isOpenRecipeNumber recipeCount={}", recipeIds.size());
        if (recipeIds.size() >= openRecipeNumber) {
            saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "开方张数已超出医院限定范围，不能继续开方。");
            throw new DAOException(ErrorCode.SERVICE_ERROR, "开方张数已超出医院限定范围，不能继续开方。");
        }
        return true;
    }

    /**
     * 根据复诊id获取处方id，并排除 特定处方id
     *
     * @param clinicId 复诊id
     * @param recipeId 特定处方id
     * @return 处方明细
     */
    public List<Integer> findRecipeByClinicId(Integer clinicId, Integer recipeId, List<Integer> status) {
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndStatus(clinicId, status);
        return findRecipeByClinicId(recipeList, recipeId);
    }

    /**
     * 获取需要校验重复处方的处方id
     * @param clinicId
     * @param recipeId
     * @param status
     * @param processState
     * @return
     */
    public List<Integer> findRecipeByClinicIdAndStatusAndProcessState(Integer clinicId, Integer recipeId, List<Integer> status,List<Integer> processState) {
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndStatusAndProcessState(clinicId, status, processState);
        return findRecipeByClinicId(recipeList, recipeId);
    }

    /**
     * 根据复诊id与状态字段 获取处方id，并排除 特定处方id
     *
     * @param clinicId 复诊id
     * @param recipeId 特定处方id
     * @return 处方明细
     */
    public List<Integer> findRecipeByClinicIdAndProcessState(Integer clinicId, Integer recipeId, List<Integer> processState) {
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndProcessState(clinicId, processState);
        return findRecipeByClinicId(recipeList, recipeId);
    }


    public List<Map<String, Object>> getCheckNotPassDetail(Recipe recipe) {
        //获取审核不通过详情
        List<Map<String, Object>> mapList = recipeAuditClient.getCheckNotPassDetail(recipe.getRecipeId());
        if (CollectionUtils.isEmpty(mapList)) {
            return mapList;
        }
        mapList.forEach(a -> {
            List results = (List) a.get("checkNotPassDetails");
            List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtil.convert(results, RecipeDetailBean.class);
            for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                UsingRateDTO usingRateDTO = drugClient.usingRate(recipe.getClinicOrgan(), recipeDetailBean.getOrganUsingRate());
                if (null != usingRateDTO) {
                    recipeDetailBean.setUsingRateId(String.valueOf(usingRateDTO.getId()));
                }
                UsePathwaysDTO usePathwaysDTO = drugClient.usePathways(recipe.getClinicOrgan(), recipeDetailBean.getOrganUsePathways(), recipeDetailBean.getDrugType());
                if (null != usePathwaysDTO) {
                    recipeDetailBean.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
                }
            }
        });
        return mapList;
    }

    /**
     * 通过处方信息获取卡号
     *
     * @param recipe
     * @return
     */
    public String getCardNoByRecipe(Recipe recipe) {
        String cardNo = "";
        //根据业务id 保存就诊卡号和就诊卡类型
        if (null != recipe.getClinicId()) {
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
                if (null != revisitExDTO) {
                    cardNo = revisitExDTO.getCardId();
                }
            } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                ConsultExDTO consultExDTO = consultClient.getConsultExByClinicId(recipe.getClinicId());
                if (null != consultExDTO) {
                    cardNo = consultExDTO.getCardId();
                }
            }
        }
        return cardNo;
    }

    /**
     * 获取复诊信息设置处方信息
     *
     * @param recipe
     * @param recipeExtend
     */
    public void setRecipeInfoFromRevisit(Recipe recipe, RecipeExtend recipeExtend) {
        if (null != recipe.getClinicId()) {
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                RevisitExDTO revisitExDTO = revisitClient.retryGetByClinicId(recipe.getClinicId());
                logger.info("setRecipeInfoFromRevisit revisitExDTO:{}", JSON.toJSONString(revisitExDTO));
                if (null != revisitExDTO) {
                    recipeExtend.setRegisterID(revisitExDTO.getRegisterNo());
                    recipeExtend.setCardType(revisitExDTO.getCardType());
                    recipe.setPatientID(revisitExDTO.getPatId());
                    recipeExtend.setMedicalRecordNumber(revisitExDTO.getMedicalRecordNo());
                }
                RevisitBean revisitBean = revisitClient.getRevisitByClinicId(recipe.getClinicId());
                if (null != revisitBean) {
                    recipe.setDoctor(revisitBean.getConsultDoctor());
                    recipe.setDepart(revisitBean.getConsultDepart());
                    DoctorDTO doctorDTO = doctorClient.getDoctor(revisitBean.getConsultDoctor());
                    recipe.setDoctorName(doctorDTO.getName());
                    recipe.setRequestMpiId(revisitBean.getRequestMpi());
                    recipe.setRequestUrt(revisitBean.getRequestMpiUrt());
                }
            }
        }
    }

    /**
     * 药企销售价格
     *
     * @param recipeId 处方id
     * @param depId    药企id
     */
    public Map<Integer, List<SaleDrugList>> getRecipeDetailSalePrice(Integer recipeId, Integer depId) {
        logger.info("RecipeManager getRecipeDetailSalePrice req = recipeId:{} depId:{}", JSON.toJSONString(recipeId), depId);

        if (Objects.isNull(recipeId)) {
            return null;
        }
        // 医生指定药企
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipeId);
        if (Objects.nonNull(extend) && AppointEnterpriseTypeEnum.ENTERPRISE_APPOINT.getType().equals(extend.getAppointEnterpriseType()) && Objects.isNull(depId)) {
            String deliveryCode = extend.getDeliveryCode();
            if (StringUtils.isEmpty(deliveryCode)) {
                return null;
            }
            List<String> ids = Arrays.asList(deliveryCode.split("\\|"));
            depId = Integer.valueOf(ids.get(0));
            logger.info("RecipeManager getRecipeDetailSalePrice depId={}", depId);
        }

        if (null == depId) {
            return null;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (null == drugsEnterprise) {
            return null;
        }
        // 药企结算根据医院价格不用更新
        if (new Integer(1).equals(drugsEnterprise.getSettlementMode())) {
            return null;
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        List<Integer> drugsIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(depId, drugsIds);
        if (CollectionUtils.isEmpty(saleDrugLists)) {
            return null;
        }
        Map<Integer, List<SaleDrugList>> saleDrugMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        logger.info("RecipeManager getRecipeDetailSalePrice res = saleDrugMap:{}", JSON.toJSONString(saleDrugMap));

        return saleDrugMap;

    }

    /**
     * 根据药企信息更改处方药品销售价格
     *
     * @param recipeList
     * @param depId
     */
    public void updateRecipeDetailSalePrice(List<Recipe> recipeList, Integer depId) {
        logger.info("RecipeManager updateRecipeDetailSalePrice req = recipeList:{} depId:{}", JSON.toJSONString(recipeList), depId);

        if (CollectionUtils.isEmpty(recipeList) || Objects.isNull(depId)) {
            return;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (Objects.isNull(drugsEnterprise)) {
            return;
        }
        // 药企结算根据医院价格不用更新
        if (new Integer(1).equals(drugsEnterprise.getSettlementMode())) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        List<Integer> drugsIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(depId, drugsIds);
        if (CollectionUtils.isEmpty(saleDrugLists)) {
            return;
        }
        Map<Integer, List<SaleDrugList>> saleDrugMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        for (Recipedetail recipeDetail : recipeDetails) {
            List<SaleDrugList> drugLists = saleDrugMap.get(recipeDetail.getDrugId());
            if (CollectionUtils.isEmpty(drugLists)) {
                continue;
            }
            recipeDetail.setSalePrice(drugLists.get(0).getPrice());
            recipeDetail.setDrugCost(drugLists.get(0).getPrice().multiply(new BigDecimal(recipeDetail.getUseTotalDose())).setScale(4,BigDecimal.ROUND_HALF_UP));
        }
        logger.info("RecipeManager updateRecipeDetailSalePrice req = recipeDetails:{}", JSON.toJSONString(recipeDetails));
        recipeDetailDAO.updateAllRecipeDetail(recipeDetails);
    }

    /**
     * 更新处方退费的结点状态
     *
     * @param recipeList 处方信息
     * @param status     结点状态
     */
    public void updateRecipeRefundStatus(List<Recipe> recipeList, Integer status) {
        recipeList.forEach(recipe -> {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (recipeExtend != null) {
                recipeExtend.setRefundNodeStatus(status);
                recipeExtendDAO.update(recipeExtend);
            }
        });
    }

    /**
     * 保存审方结果
     *
     * @param recipe
     */
    public void saveRecipeCheck(Recipe recipe) {
        if (null == recipe.getChecker()) {
            return;
        }
        RecipeCheckBean recipeCheckBean = new RecipeCheckBean();
        recipeCheckBean.setRecipeId(recipe.getRecipeId());
        recipeCheckBean.setCheckDate(new Date());
        recipeCheckBean.setChecker(recipe.getChecker());
        recipeCheckBean.setCheckerName(recipe.getCheckerText());
        recipeCheckBean.setCheckOrgan(recipe.getCheckOrgan());
        recipeCheckBean.setCheckStatus(1);
        recipeCheckBean.setGrabOrderStatus(0);
        iRecipeCheckService.saveRecipeCheck(recipeCheckBean);
    }

    /**
     * 获取处方签名文件地址
     * @param fileId
     * @param validTime
     * @return
     */
    public String getRecipeSignFileUrl(String fileId, long validTime){
        return UrlConfig.fileViewUrl+fileId+"?token="+ FileAuth.instance().createToken(fileId,validTime);
    }

    /**
     * 查询同组处方
     *
     * @param groupCode 处方组号
     * @param type      0： 默认全部 1：查询暂存，2查询可撤销
     * @return 处方id集合
     */
    public List<Recipe> recipeByGroupCode(String groupCode, Integer type) {
        if (ValidateUtil.integerIsEmpty(type)) {
            return recipeDAO.findRecipeByGroupCode(groupCode);
        }
        //获取暂存同组处方
        if (type.equals(1)) {
            return recipeDAO.findRecipeByGroupCodeAndStatus(groupCode, Collections.singletonList(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType()));
        }
        //获取可撤销同组处方
        List<Recipe> recipeList = recipeDAO.findRecipeByGroupCode(groupCode);
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        List<Recipe> recipes = new ArrayList<>();
        recipeList.forEach(a -> {
            boolean cancelFlag;
            if (StringUtils.isEmpty(a.getOrderCode())) {
                cancelFlag = RecipeStatusEnum.checkRecipeRevokeStatus(a, null);
            } else {
                RecipeOrder recipeOrders = recipeOrderDAO.getByOrderCode(a.getOrderCode());
                cancelFlag = RecipeStatusEnum.checkRecipeRevokeStatus(a, recipeOrders);
            }
            if (cancelFlag && !RecipeStateEnum.STATE_DELETED.contains(a.getProcessState())
                    && !RecipeAuditStateEnum.FAIL_DOC_CONFIRMING.getType().equals(a.getAuditState())) {
                recipes.add(a);
            }
        });
        return recipes;
    }

    /**
     * 完成处方
     * @param recipeIdList
     * @param finishDate
     */
    public void finishRecipes(List<Integer> recipeIdList, Date finishDate){
        recipeDAO.updateRecipeFinishInfoByRecipeIds(recipeIdList, finishDate);
    }

    public AdvanceWarningResDTO getAdvanceWarning(AdvanceWarningReqDTO advanceWarningReqDTO) {
        logger.info("getAdvanceWarning advanceWarningReqDTO={}",JSONUtils.toString(advanceWarningReqDTO));
        AdvanceInfoReqTO advanceInfoReqTO = new AdvanceInfoReqTO();
        AdvanceInfoPatientDTO patientDTO = new AdvanceInfoPatientDTO();
        List<EncounterDTO> encounterDTOList = new ArrayList<>();
        AdvanceWarningResDTO advanceWarningResDTO = new AdvanceWarningResDTO();
        Recipe recipe = recipeDAO.get(advanceWarningReqDTO.getRecipeId());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(advanceWarningReqDTO.getRecipeId());
        if(null == recipe){
            return advanceWarningResDTO;
        }
        advanceInfoReqTO.setOrganId(recipe.getClinicOrgan());
        //系统编码
        advanceInfoReqTO.setSyscode("recipe");
        if(Objects.nonNull(recipeExtend)){
            //就诊流水号
            advanceInfoReqTO.setMdtrtSn(recipeExtend.getRegisterID());
        }
        //触发场景
        advanceInfoReqTO.setTrigScen("2");
        //app必传
        if(new Integer(1).equals(advanceWarningReqDTO.getServerFlag())){
            //应用的appId
            advanceInfoReqTO.setAppId("202206291421");
            //app端传身份证号
            com.ngari.patient.dto.PatientDTO patient = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
            patientDTO.setPatnId(patient.getIdcard());
        }else{
            //pc端传patientId
            patientDTO.setPatnId(recipe.getPatientID());
        }
        patientDTO.setPatnName(recipe.getPatientName());
        patientDTO.setCurrMdtrtId(String.valueOf(recipe.getRecipeId()));
        //端标识
        advanceInfoReqTO.setServerFlag(advanceWarningReqDTO.getServerFlag());
        //业务类型:处方
        advanceInfoReqTO.setBusinessType(0);
        EncounterDTO encounterDTO = new EncounterDTO();
        //就诊标识
        encounterDTO.setMdtrtId(String.valueOf(recipe.getRecipeId()));
        //医疗服务机构标识
        encounterDTO.setMedinsId("H12010500650");
        //医疗机构名称
        encounterDTO.setMedinsName(String.valueOf(recipe.getClinicOrgan()));
        //入院日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            encounterDTO.setAdmDate(sdf.parse(String.valueOf(recipe.getCreateDate())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //主诊断编码
        encounterDTO.setDscgMainDiseCodg(recipe.getOrganDiseaseId());
        //主诊断名称
        encounterDTO.setDscgMainDiseName(recipe.getOrganDiseaseName());
        //医师标识
        encounterDTO.setDrCodg(String.valueOf(recipe.getDoctor()));
        //入院科室标识
        encounterDTO.setAdmDeptCodg(recipe.getAppointDepart());
        //入院科室名称
        encounterDTO.setAdmDeptName(recipe.getAppointDepartName());
        //就诊类型：门诊
        encounterDTO.setMedMdtrtType("1");
        //就医疗类型：普通门诊
        encounterDTO.setMedType("11");
        //总费用
        encounterDTO.setMedfeeSumamt(recipe.getActualPrice().doubleValue());
        //生育状态
        encounterDTO.setMatnStas("0");
        //险种
        encounterDTO.setInsutype("310");
        //异地结算标志
        encounterDTO.setOutSetlFlag("0");
        encounterDTOList.add(encounterDTO);
        patientDTO.setEncounterDtos(encounterDTOList);
        advanceInfoReqTO.setPatientDTO(patientDTO);
        AdvanceInfoResTO advanceInfo = offlineRecipeClient.getAdvanceInfo(advanceInfoReqTO);
        advanceWarningResDTO.setPopUrl(advanceInfo.getPopUrl());
        logger.info("getAdvanceWarning advanceWarningResDTO={}",JSONUtils.toString(advanceWarningResDTO));
        return advanceWarningResDTO;
    }

    public List<RequirementsForTakingDTO> findRequirementsForTakingByDecoctionId(Integer organId, Integer decoctionId) {
        List<RequirementsForTakingDTO> requirementsForTakingVOS=new ArrayList<>();
        List<RequirementsForTaking> requirementsForTakings=requirementsForTakingDao.findAllByOrganId(organId);
        if (CollectionUtils.isEmpty(requirementsForTakings)) {
            return requirementsForTakingVOS;
        }
        if (decoctionId == null) {
            //如果煎法未选择，则服用要求按展示全部处理
            logger.info("findRequirementsForTakingByDecoctionId res:{}", JSONUtils.toString(requirementsForTakings));
            return ObjectCopyUtils.convert(requirementsForTakings, RequirementsForTakingDTO.class);
        } else {
            //根据煎法筛选出关键的服用要求选项展示给医生选择
            requirementsForTakings.forEach(requirementsForTaking -> {
                String decoctionwayId = requirementsForTaking.getDecoctionwayId();
                if (StringUtils.isEmpty(decoctionwayId)) {
                    return;
                }
                List<String> decoctionwayIdList = Arrays.asList(decoctionwayId.split(","));
                if (CollectionUtils.isEmpty(decoctionwayIdList)) {
                    return;
                }
                if (decoctionwayIdList.contains(String.valueOf(decoctionId))) {
                    requirementsForTakingVOS.add(ObjectCopyUtils.convert(requirementsForTaking, RequirementsForTakingDTO.class));
                }
            });
        }
        return requirementsForTakingVOS;
    }

    /**
     * 自助机查询接口
     *
     * @param
     * @return
     */
    public Integer automatonCount(Recipe recipe, String startTime, String endTime, Integer terminalType,
                                  List<String> terminalId, List<Integer> processState) {
        return recipeDAO.countByAutomaton(recipe, startTime, endTime, terminalType, terminalId, processState);
    }

    /**
     * 自助机查询接口
     *
     * @param
     * @return
     */
    public List<Recipe> automatonList(Recipe recipe, String startTime, String endTime, Integer terminalType,
                                      List<String> terminalId, List<Integer> processState, Integer start, Integer limit) {
        return recipeDAO.findAutomatonList(recipe, startTime, endTime, terminalType, terminalId, processState, start, limit);
    }


    public List<RecipeRefundDTO> getRecipeRefundInfo(Integer doctorId,Date startTime,Date endTime,Integer start,Integer limit) {
        List<RecipeRefundDTO> recipeRefundInfo = recipeDAO.getRecipeRefundInfo(doctorId,startTime,endTime,start,limit);
        recipeRefundInfo.forEach(recipeRefundDTO -> {
            List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRecipeRefundByRecipeIdAndNode(recipeRefundDTO.getRecipeId(), -1);
            if(recipeRefunds.size()>0){
                recipeRefundDTO.setReason(recipeRefunds.get(0).getReason());
            }
            else {
                recipeRefundDTO.setReason("医生撤销或系统原因");
            }
        });
        logger.info("RecipeManager getRecipeRefundInfo recipeRefundInfo={}",JSONUtils.toString(recipeRefundInfo));
        return recipeRefundInfo;
    }

    public RecipeSkipDTO getRecipeSkipUrl(Integer organId, String recipeCode, Integer recipeType) {
        RecipeSkipDTO recipeSkipDTO = new RecipeSkipDTO();
        recipeSkipDTO.setShowFlag(false);
        recipeSkipDTO.setClickFlag(false);
        try {
            String recipeSkipOrgan = parameterDao.getByName("recipeSkipOrgan");
            if (LocalStringUtil.hasOrgan(organId.toString(), recipeSkipOrgan)) {
                //包含机构
                String recipeSkipUrl = parameterDao.getByName("recipeSkipUrl");
                String recipeClickFlag = parameterDao.getByName("recipeSkipClickFlag");
                Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, organId);
                if(Objects.nonNull(recipe) && new Integer(1).equals(recipeType)){
                    logger.info("RecipeManager getRecipeSkipUrl recipe={}",JSONUtils.toString(recipe));
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if(Objects.isNull(recipeExtend)){
                        return recipeSkipDTO;
                    }
                    //线上处方，并且状态为待发药，配送中，待取药，已完成才展示按钮
                    if(recipe.getProcessState().equals(RecipeStateEnum.PROCESS_STATE_DISPENSING.getType())
                            || recipe.getProcessState().equals(RecipeStateEnum.PROCESS_STATE_DISTRIBUTION.getType())
                            || recipe.getProcessState().equals(RecipeStateEnum.PROCESS_STATE_MEDICINE.getType())
                            || recipe.getProcessState().equals(RecipeStateEnum.PROCESS_STATE_DONE.getType())){
                        logger.info("RecipeManager getRecipeSkipUrl ProcessState={}",JSONUtils.toString(recipe.getProcessState()));
                        recipeSkipDTO.setShowFlag(true);
                    }
                    recipeSkipDTO.setSkipUrl(recipeSkipUrl + recipeExtend.getChargeId());
                }else if(new Integer(2).equals(recipeType)){
                    //门诊处方直接展示按钮
                    recipeSkipDTO.setShowFlag(true);
                    recipeSkipDTO.setSkipUrl(recipeSkipUrl + recipeCode);
                }
                recipeSkipDTO.setClickFlag(StringUtils.isEmpty(recipeClickFlag)?false:Boolean.valueOf(recipeClickFlag));
            }
        } catch (Exception e) {
            logger.error("getRecipeSkipUrl error", e);
        }
        logger.info("RecipeManager getRecipeSkipUrl recipeSkipDTO={}",JSONUtils.toString(recipeSkipDTO));
        return recipeSkipDTO;

    }

    public void saveRecipeExtendChargeId(String recipeCodeS,Integer organId,Map<String, String> codeMap){
        List<String> recipeCodeList = JSONUtils.parse(recipeCodeS, List.class);
        Map<String, Integer> dataMap = new HashMap<>();
        for(String recipeCode : recipeCodeList){
            Recipe recipes = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, organId);
            if(Objects.isNull(recipes)){
                continue;
            }
            //recipeCode会有用逗号分隔的情况
            String[] recipeCodeSplit = recipeCode.split(",");
            for(String str : recipeCodeSplit){
                dataMap.put(str,recipes.getRecipeId());
            }
        }
        for(String key : dataMap.keySet()){
            String hisId = null;
            if(Objects.nonNull(codeMap)){
                hisId = codeMap.get(key);
            }
            if(StringUtils.isEmpty(hisId)){
                continue;
            }
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(dataMap.get(key));
            if (Objects.nonNull(recipeExtend)){
                recipeExtend.setChargeId(hisId);
                recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
            }
        }
    }

    /**
     * 获取收费项
     * @param recipes 处方
     */
    public void queryChargeItemCode(List<Recipe> recipes){
        logger.info("RecipeManager queryChargeItemCode recipes:{}", JSON.toJSONString(recipes));
        try {
            Recipe dbRecipe = recipes.get(0);
            Boolean windhpFlag = configurationClient.getValueBooleanCatch(dbRecipe.getClinicOrgan(), "windhpFlag", false);
            if (!windhpFlag) {
                return;
            }
            RecipeExtend dbRecipeExtend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
            RecipeChargeItemCodeReqTo request = new RecipeChargeItemCodeReqTo();
            PatientDTO patientDTO = patientClient.getPatientDTO(dbRecipe.getMpiid());
            request.setPatientID(dbRecipe.getPatientID());
            request.setOrganId(dbRecipe.getClinicOrgan());
            request.setPatientName(dbRecipe.getPatientName());
            request.setCertificateType(patientDTO.getCertificateType());
            request.setRegisterID(dbRecipeExtend.getRegisterID());
            request.setCertificate(patientDTO.getCertificate());
            request.setCardType(dbRecipeExtend.getCardType());
            request.setCardNo(dbRecipeExtend.getCardNo());
            request.setBeginTime(DateConversion.getDateTimeDaysAgo(3));
            request.setEndTime(new Date());
            List<String> cisRecipeCodes = new ArrayList<>();
            recipes.forEach(recipe -> cisRecipeCodes.add(recipe.getRecipeCode()));
            request.setRecipeCodes(cisRecipeCodes);
            List<Integer> recipeIdList = recipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
            List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
            List<String> cisRecipeCostNumber = new ArrayList<>();
            recipeExtendList.forEach(recipeExtend -> cisRecipeCostNumber.add(recipeExtend.getRecipeCostNumber()));
            request.setRecipeCostNumber(cisRecipeCostNumber);
            RecipeChargeItemCodeResTo recipeChargeItemCodeResTo = consultClient.getRecipeChargeItems(request);
            if (Objects.isNull(recipeChargeItemCodeResTo)) {
                return;
            }
            Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
            Map<String, String> hisRecipeCodeMap = recipeChargeItemCodeResTo.getHisRecipeCode();
            Map<String, String> hisRecipeCostNumberMap = recipeChargeItemCodeResTo.getHisRecipeCostNumber();
            recipes.forEach(recipe -> {
                String hisRecipeCode = hisRecipeCodeMap.get(recipe.getRecipeCode());
                RecipeExtend recipeExtend = recipeExtendMap.get(recipe.getRecipeId());
                if (StringUtils.isNotEmpty(hisRecipeCode)) {
                    recipeExtend.setChargeId(hisRecipeCode);
                } else {
                    recipeExtend.setChargeId("");
                }
                String recipeCostNumber = recipeExtend.getRecipeCostNumber();
                StringBuilder recipeChargeCode = new StringBuilder("");
                if (StringUtils.isNotEmpty(recipeCostNumber)) {
                    List<String> recipeCostNumbers = Arrays.asList(recipeCostNumber.split(","));
                    recipeCostNumbers.forEach(recipeCostItem -> {
                        String recipeCost = hisRecipeCostNumberMap.get(recipeCostItem);
                        if (StringUtils.isEmpty(recipeCost)) {
                            return;
                        }
                        recipeChargeCode.append(recipeCost).append(",");
                    });
                    if (recipeChargeCode.toString().contains(",")) {
                        recipeChargeCode.deleteCharAt(recipeChargeCode.lastIndexOf(","));
                    }
                }
                logger.info("RecipeManager queryChargeItemCode recipeChargeCode:{}", recipeChargeCode);
                recipeExtend.setChargeItemCode(recipeChargeCode.toString());
                recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
            });
        } catch (Exception e) {
            logger.error("RecipeManager queryChargeItemCode error", e);
        }
    }

    /**
     * 获取默认药师
     * @param organId
     * @return
     */
    public List<Integer> getDocIdInTime(Integer organId) {
        List<Integer> list = new ArrayList<>();
        try {
            String defaultRecipeCheckDoctor = configurationClient.getValueCatch(organId, "defaultRecipecheckDoctor", "");
            if (StringUtils.isEmpty(defaultRecipeCheckDoctor)) {
                return list;
            }
            if (!"-1".equals(defaultRecipeCheckDoctor)) {
                // 固定药师
                list = Arrays.stream(defaultRecipeCheckDoctor.split(",")).map(Integer::valueOf).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("RecipeManager getDocIdInTime Exception", e);
        }
        return list;
    }

    /**
     * 获取收费项编码
     * @param recipeExtendList
     * @return
     */
    public List<String> getChargeItemCode(List<RecipeExtend> recipeExtendList){
        try {
            final List<String> chargeItemCodeList = new ArrayList<>();
            recipeExtendList.forEach(recipeExtend -> {
                String chargeItemCode = recipeExtend.getChargeItemCode();
                if (StringUtils.isNotEmpty(chargeItemCode)) {
                    chargeItemCodeList.add(chargeItemCode);
                }
                if (StringUtils.isNotEmpty(recipeExtend.getChargeId()) && !chargeItemCodeList.toString().contains(recipeExtend.getChargeId())) {
                    chargeItemCodeList.add(recipeExtend.getChargeId());
                }
            });
            logger.info("RecipeManager getChargeItemCode chargeItemCodeList:{}", JSON.toJSONString(chargeItemCodeList));
            return chargeItemCodeList;
        } catch (Exception e) {
            logger.error("RecipeManager getChargeItemCode error", e);
        }
        return new ArrayList<>();
    }


    /**
     * 获取医保id
     *
     * @param clinicOrgan
     * @param recipeIdList
     * @return
     */
    public HisSettleReqDTO getHisOrderCode(Integer clinicOrgan, List<Integer> recipeIdList) {
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        if (CollectionUtils.isEmpty(recipes)) {
            return null;
        }
        HisSettleReqDTO reqDTO = new HisSettleReqDTO();
        com.ngari.recipe.dto.PatientDTO patientBean = patientClient.getPatientDTO(recipes.get(0).getMpiid());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeIdList.get(0));
        List<HisOrderCodeResTO> hisOrderCodeResTOS = null;
        Boolean getAliPayYb = configurationClient.getValueBooleanCatch(clinicOrgan, "getAliPayYb", false);
        try {
            if (getAliPayYb) {
                hisOrderCodeResTOS = recipeHisClient.queryHisOrderCodeByRecipeCode(recipes.get(0).getPatientID(), clinicOrgan, recipes, patientBean, recipeExtend);
            }
        } catch (Exception e) {
            logger.error("获取医保id错误");
            return null;
        }
        if (CollectionUtils.isEmpty(hisOrderCodeResTOS)) {
            // 如果前置机没有返回数据,就使用ext表中的
            List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
            if (CollectionUtils.isNotEmpty(recipeExtends)) {
                List<String> hisOrderCodes = recipeExtends.stream().filter(recipeExt -> StringUtils.isNotEmpty(recipeExt.getHisOrderCode())).map(RecipeExtend::getHisOrderCode).collect(Collectors.toList());
                List<String> hisBusId = recipeExtends.stream().filter(recipeExt -> StringUtils.isNotEmpty(recipeExt.getHisBusId())).map(RecipeExtend::getHisBusId).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(hisOrderCodes)) {
                    reqDTO.setYbId(Joiner.on("|").join(hisOrderCodes));
                }
                if (CollectionUtils.isNotEmpty(hisBusId)) {
                    reqDTO.setHisBusId(Joiner.on("|").join(hisBusId));
                }
            }
            return reqDTO;
        }

        Map<String, List<Recipe>> recipeMap = recipes.stream().collect(Collectors.groupingBy(Recipe::getRecipeCode));
        List<String> hisOrderCode = new ArrayList<>();
        List<String> hisBusId = new ArrayList<>();
        for (HisOrderCodeResTO hisOrderCodeResTO : hisOrderCodeResTOS) {
            if (MapUtils.isNotEmpty(recipeMap) && CollectionUtils.isNotEmpty(recipeMap.get(hisOrderCodeResTO.getRecipeCode()))) {
                Recipe recipe = recipeMap.get(hisOrderCodeResTO.getRecipeCode()).get(0);
                RecipeExtend recipeExt = new RecipeExtend();
                recipeExt.setRecipeId(recipe.getRecipeId());
                if (StringUtils.isNotEmpty(hisOrderCodeResTO.getHisOrderCode())) {
                    recipeExt.setHisOrderCode(hisOrderCodeResTO.getHisOrderCode());
                    hisOrderCode.add(hisOrderCodeResTO.getHisOrderCode());
                }
                if (StringUtils.isNotEmpty(hisOrderCodeResTO.getHisBusId())) {
                    recipeExt.setHisBusId(hisOrderCodeResTO.getHisBusId());
                    hisBusId.add(hisOrderCodeResTO.getHisBusId());
                }
                recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExt);
            }
        }
        if (CollectionUtils.isNotEmpty(hisOrderCode)) {
            reqDTO.setYbId(Joiner.on("|").join(hisOrderCode));
        }
        if (CollectionUtils.isNotEmpty(hisBusId)) {
            reqDTO.setHisBusId(Joiner.on("|").join(hisBusId));
        }
        return reqDTO;
    }

    ///**
    // * 快捷购药是否存在库存
    // * @param recipeId
    // * @return
    // */
    //public boolean fastRecipeStock(Integer recipeId){
    //    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
    //    FastRecipe fastRecipe = fastRecipeDAO.get(recipeExtend.getMouldId());
    //    if (Objects.isNull(fastRecipe)) {
    //        return false;
    //    }
    //    if (Objects.isNull(fastRecipe.getStockNum())) {
    //        return true;
    //    }
    //    if (0 == fastRecipe.getStockNum()) {
    //        return false;
    //    }
    //    return recipeExtend.getFastRecipeNum() <= fastRecipe.getStockNum();
    //}

    /**
     * 排除 特定处方id
     *
     * @param recipeList
     * @param recipeId
     * @return
     */
    private List<Integer> findRecipeByClinicId(List<Recipe> recipeList, Integer recipeId) {
        logger.info("RecipeManager findRecipeByClinicId recipeList:{},recipeId={}", JSON.toJSONString(recipeList), recipeId);
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        } else {
            return recipeList.stream().map(Recipe::getRecipeId).filter(id -> !id.equals(recipeId)).collect(Collectors.toList());
        }
    }

    /**
     * 获取二方id下关联的处方
     *
     * @param clinicId   二方id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    public List<Recipe> findRecipeAllByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        List<Recipe> list = recipeDAO.findRecipeAllByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findRecipeAllByBussSourceAndClinicId list :{}", JSON.toJSONString(list));
        return list;
    }

    /**
     * 判断处方是否写入成功his
     *
     * @param recipeId
     * @return
     */
    @LogRecord
    public boolean recipeWriteHis(Integer recipeId) {
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return false;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        if (RecipeStateEnum.STATE_DELETED.contains(recipe.getProcessState())) {
            return true;
        }
        if (WriteHisEnum.WRITE_HIS_STATE_ORDER.getType().equals(recipe.getWriteHisState())) {
            return true;
        }
        return false;
    }

    /**
     * 新增处方通知
     * @param recipeId
     * @param
     */
    public void addRecipeNotify(Integer recipeId, String orderStatus) {
        logger.info("addRecipeNotify recipeId:{} ,orderStatus:{} ", recipeId,orderStatus);
        try {
            String redisKey=recipeId+"addRecipeNotify"+orderStatus;
            String addRecipeNotifyCache = redisClient.get(redisKey);
            if(StringUtils.isNotEmpty(addRecipeNotifyCache)){
                logger.info("addRecipeNotify already notify recipeId:{} ,orderStatus:{} ", recipeId,orderStatus);
                return;
            }
            redisClient.setEX(redisKey, 30 * 24 * 3600L, String.valueOf(recipeId));
            RecipeDTO recipeDTO = super.getRecipeDTO(recipeId);
            com.ngari.platform.recipe.mode.RecipeInfoDTO recipeDTO1=new com.ngari.platform.recipe.mode.RecipeInfoDTO();
            recipeDTO1.setRecipe(ObjectCopyUtils.convert(recipeDTO.getRecipe(),RecipeBean.class));
            recipeDTO1.setRecipeExtend(ObjectCopyUtils.convert(recipeDTO.getRecipeExtend(),RecipeExtendBean.class));
            recipeDTO1.setRecipeDetails(ObjectCopyUtils.convert(recipeDTO.getRecipeDetails(),RecipeDetailBean.class));
            logger.info("addRecipeNotify sendMsgToMq send to MQ start, busId:{}，param:{}", recipeId, JSONUtils.toString(recipeDTO1));
            MQHelper.getMqPublisher().publish(OnsConfig.addRecipeTopic, recipeDTO1, null);
            logger.info("addRecipeNotify sendMsgToMq send to MQ end, busId:{}", recipeId);
        } catch (Exception e) {
            logger.error("addRecipeNotify sendMsgToMq can't send to MQ,  busId:{}", recipeId, e);
        }
    }

    /**
     * his回写处方数据更新表字段
     *
     * @param recipeId
     * @param recipeCode
     * @param patientId
     * @param writeHisState
     * @param amount
     * @param recipeFee
     * @param detailSize
     */
    @LogRecord
    public void sendSuccessRecipe(Integer recipeId, String recipeCode, String patientId, Integer writeHisState,
                                  String amount, BigDecimal recipeFee, Integer detailSize) {
        Recipe recipe = recipeDAO.get(recipeId);
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipe.getRecipeId());
        updateRecipe.setWriteHisState(null == writeHisState ? WriteHisEnum.WRITE_HIS_STATE_ORDER.getType() : writeHisState);
        //医院处方号
        if (StringUtils.isNotEmpty(recipeCode)) {
            updateRecipe.setRecipeCode(recipeCode);
        }
        //病人医院病历号
        if (StringUtils.isNotEmpty(patientId) && StringUtils.isEmpty(recipe.getPatientID())) {
            updateRecipe.setPatientID(patientId);
        }
        //医保处方处理
        if (Integer.valueOf(1).equals(recipe.getMedicalFlag())) {
            updateRecipe.setGiveMode(RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
        }
        boolean recipeSendUpdatePrice = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeSendUpdatePrice", false);
        if (recipeSendUpdatePrice) {
            //医院处方费
            BigDecimal totalMoney = StringUtils.isNotEmpty(amount) ? new BigDecimal(amount) : Objects.nonNull(recipeFee) ? recipeFee : null;
            List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            //处方总金额， 外带药处方不做处理
            if (null != totalMoney && recipeDetails.size() == detailSize) {
                updateRecipe.setTotalMoney(totalMoney);
                updateRecipe.setActualPrice(totalMoney);
            } else {
                BigDecimal money = drugClient.totalMoney(recipe.getRecipeType(), recipeDetails, recipe);
                updateRecipe.setTotalMoney(money);
                updateRecipe.setActualPrice(money);
            }
        }
        logger.info("RecipeManager sendSuccessRecipe, updateRecipe:{}", JSON.toJSONString(updateRecipe));
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        super.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "HIS审核返回：写入his成功，审核通过");
    }

    /**
     * his回写处方数据更新表字段
     *
     * @param recipeId
     * @param recipeCostNumber
     * @param pharmNo
     * @param hisOrderCode
     * @param hisDiseaseSerial
     */
    @LogRecord
    public void sendSuccessRecipeExt(Integer recipeId, String recipeCostNumber, String pharmNo, String hisOrderCode, String hisDiseaseSerial
            , String registerId, String medicalType, String medicalTypeText,String hisBusId) {
        if (ValidateUtil.validateObjectsIsEmpty(recipeCostNumber, pharmNo, hisOrderCode, hisDiseaseSerial, registerId,
                medicalType, medicalTypeText, hisBusId)) {
            return;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null == recipeExtend) {
            return;
        }
        RecipeExtend updateRecipeExtend = new RecipeExtend();
        // 将取药窗口更新到ext表
        updateRecipeExtend.setPharmNo(pharmNo);
        updateRecipeExtend.setRecipeId(recipeId);
        updateRecipeExtend.setRecipeCostNumber(recipeCostNumber);
        updateRecipeExtend.setHisOrderCode(hisOrderCode);
        updateRecipeExtend.setHisBusId(hisBusId);
        if (StringUtils.isNotEmpty(hisDiseaseSerial)) {
            updateRecipeExtend.setHisDiseaseSerial(hisDiseaseSerial);
        }
        if (StringUtils.isNotEmpty(registerId) && StringUtils.isEmpty(recipeExtend.getRegisterID())) {
            updateRecipeExtend.setRegisterID(registerId);
        }
        if (StringUtils.isNotEmpty(medicalType) && StringUtils.isEmpty(recipeExtend.getMedicalType())) {
            updateRecipeExtend.setMedicalType(medicalType);
        }
        if (StringUtils.isNotEmpty(medicalTypeText) && StringUtils.isEmpty(recipeExtend.getMedicalTypeText())) {
            updateRecipeExtend.setMedicalTypeText(medicalTypeText);
        }
        logger.info("RecipeManager sendSuccessRecipe, recipeExtend:{}", JSON.toJSONString(updateRecipeExtend));
        recipeExtendDAO.updateNonNullFieldByPrimaryKey(updateRecipeExtend);
    }

    /**
     * 设置处方取消状态
     *
     * @param recipeOrder
     * @return
     */
    public List<Integer> setRecipeCancelState(RecipeOrder recipeOrder) {
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        recipeList.forEach(recipe -> {
            recipe.setStatus(RecipeStatusConstant.REVOKE);
            recipe.setLastModify(new Date());
            recipe.setPayFlag(PayFlagEnum.REFUND_SUCCESS.getType());
            recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
        });
        return recipeIdList;
    }

    /**
     * 线上处方 患者端列表查询
     *
     * @param req
     * @return
     */
    @LogRecord
    public List<RecipeInfoDTO> patientRecipeList(PatientRecipeListReqDTO req) {
        List<String> isHisRecipe = configurationClient.getPropertyByStringList("findRecipeListType");
        if (!isHisRecipe.contains("onLine")) {
            return Collections.emptyList();
        }
        List<Integer> recipeState = RecipeStateEnum.RECIPE_ALL;
        switch (req.getState()) {
            case 0:
                recipeState = RecipeStateEnum.RECIPE_ALL;
                break;
            case 1:
                recipeState = Lists.newArrayList(RecipeStateEnum.PROCESS_STATE_AUDIT.getType());
                break;
            case 2:
                recipeState = Lists.newArrayList(RecipeStateEnum.PROCESS_STATE_ORDER.getType());
                break;
            case 3:
                recipeState = RecipeStateEnum.RECIPE_COLLECT;
                break;
            case 4:
                recipeState = RecipeStateEnum.RECIPE_OVER;
                break;
            default:
                break;
        }
        req.setRecipeState(recipeState);
        req.setEndTime(DateUtils.addDays(req.getEndTime(),1));
        List<Recipe> recipes = recipeDAO.findPatientRecipeList(req);
        if (CollectionUtils.isEmpty(recipes)) {
            return Collections.emptyList();
        }
        List<Integer> recipeIdList = recipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIdList(recipeIdList);
        Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        Map<Integer, RecipeExtend> recipeExtMap = recipeExtList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
        List<RecipeInfoDTO> recipeInfoDTOS = recipes.stream().map(recipe -> {
            RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
            recipeInfoDTO.setRecipe(recipe);
            if (MapUtils.isNotEmpty(recipeExtMap) && Objects.nonNull(recipeExtMap.get(recipe.getRecipeId()))) {
                recipeInfoDTO.setRecipeExtend(recipeExtMap.get(recipe.getRecipeId()));
            }
            if (MapUtils.isNotEmpty(recipeDetailMap) && CollectionUtils.isNotEmpty(recipeDetailMap.get(recipe.getRecipeId()))) {
                recipeInfoDTO.setRecipeDetails(recipeDetailMap.get(recipe.getRecipeId()));
            }
            return recipeInfoDTO;
        }).collect(Collectors.toList());
        return recipeInfoDTOS;
    }
}
