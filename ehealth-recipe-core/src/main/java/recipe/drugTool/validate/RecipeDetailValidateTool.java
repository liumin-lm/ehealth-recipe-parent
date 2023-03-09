package recipe.drugTool.validate;

import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.opbase.base.mode.OrganDictionaryItemDTO;
import com.ngari.recipe.dto.RequirementsForTakingDTO;
import com.ngari.recipe.dto.ValidateOrganDrugDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.client.IConfigurationClient;
import recipe.client.OperationClient;
import recipe.constant.ErrorCode;
import recipe.dao.DrugDecoctionWayDao;
import recipe.dao.DrugMakingMethodDao;
import recipe.dao.RequirementsForTakingDao;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.manager.DrugManager;
import recipe.manager.OrganDrugListManager;
import recipe.manager.RecipeManager;
import recipe.manager.RevisitManager;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处方明细校验类
 *
 * @author fuzi
 */
@Service
public class RecipeDetailValidateTool {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private DrugManager drugManager;
    @Autowired
    DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    RequirementsForTakingDao requirementsForTakingDao;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private RevisitManager revisitManager;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;

    /**
     * 患者选择煎法
     */
    private final static String DECOCTION_DEPLOY_PATIENT = "2";
    /**
     * 校验药品状态 0:正常，1已失效，2未完善
     */
    private final static int VALIDATE_STATUS_YES = 0;
    public final static Integer VALIDATE_STATUS_FAILURE = 1;
    public final static Integer VALIDATE_STATUS_PERFECT = 2;
    /**
     * 药品超量天数
     */
    private final static Integer SUPER_SCALAR_DAYS = 7;
    /**
     * 字典业务类型 15 超量
     */
    private final static Integer DIC_BUS_TYPE = 15;

    /**
     * 校验比对药品
     *
     * @param recipeDetailBean 处方明细
     * @param organDrugGroup   机构药品组
     * @return
     */
    public OrganDrugList validateOrganDrug(RecipeDetailBean recipeDetailBean, Map<String, List<OrganDrugList>> organDrugGroup) {
        ValidateOrganDrugDTO validateOrganDrugDTO = new ValidateOrganDrugDTO(recipeDetailBean.getOrganDrugCode(), null, recipeDetailBean.getDrugId());
        OrganDrugList organDrugList = organDrugListManager.validateOrganDrug(validateOrganDrugDTO, organDrugGroup);
        if (validateOrganDrugDTO.getValidateStatus()) {
            recipeDetailBean.setValidateStatus(VALIDATE_STATUS_YES);
        } else {
            recipeDetailBean.setValidateStatus(VALIDATE_STATUS_FAILURE);
        }
        if (null != organDrugList) {
            //设置剂型
            recipeDetailBean.setDrugForm(organDrugList.getDrugForm());
        }
        return organDrugList;
    }

    /**
     * 校验药 数据是否完善
     * 错误数据设置为null
     *
     * @param recipeDetail 处方明细数据
     * @param recipeDay    处方药物使用天数时间
     * @param organDrug    机构药品
     */
    public void validateDrug(RecipeDetailBean recipeDetail, String[] recipeDay, OrganDrugList organDrug, Integer recipeType,
                             Map<String, DrugEntrust> drugEntrustNameMap, Integer organId, Integer version) {
        recipeDetail.setDrugName(organDrug.getDrugName());
        recipeDetail.setSaleName(organDrug.getSaleName());
        //剂量单位是否与机构药品目录单位一致
        if (StringUtils.isEmpty(OrganDrugListManager.getUseDoseUnit(recipeDetail.getUseDoseUnit(), organDrug))) {
            recipeDetail.setUseDoseUnit(null);
            recipeDetail.setUseDose(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            recipeDetail.setValidateStatusText("机构药品药物使用规格单位或者最小单位错误UseDoseUnit");
        }

        //开药单位是否与机构药品目录单位一致，不一致，重新填写开具数量
        if (StringUtils.isEmpty(OrganDrugListManager.getDrugUnit(recipeDetail.getDrugUnit(), organDrug))) {
            recipeDetail.setUseTotalDose(null);
            recipeDetail.setPack(null);
            recipeDetail.setUseDose(null);
            recipeDetail.setDrugUnit(organDrug.getUnit());
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            recipeDetail.setValidateStatusText("机构药品药物单位错误DrugUnit");
        }

        //开药天数
        useDayValidate(recipeType, recipeDay, recipeDetail);
        /**校验中药 数据是否完善*/
        if (RecipeUtil.isTcmType(recipeType)) {
            //每次剂量
            if (ValidateUtil.validateObjects(recipeDetail.getUseDose())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品每次剂量是空UseDos");
            }
            //校验中药嘱托
            entrustValidate(recipeDetail, drugEntrustNameMap);
            //用药频次，用药途径是否在机构字典范围内
            medicationsValidate(organDrug.getOrganId(), recipeDetail);
        } else {
            /**校验西药 数据是否完善*/
            //校验皮试属性
            if (!Integer.valueOf(1).equals(organDrug.getSkinTestDrugFlag())) {
                recipeDetail.setSkinTestFlag(0);
                recipeDetail.setSkinTestDrugFlag(organDrug.getSkinTestDrugFlag());
            }
            if (Integer.valueOf(1).equals(organDrug.getSkinTestDrugFlag()) && ValidateUtil.integerIsEmpty(recipeDetail.getSkinTestFlag())) {
                recipeDetail.setSkinTestFlag(0);
                recipeDetail.setSkinTestDrugFlag(organDrug.getSkinTestDrugFlag());
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品【" + organDrug.getDrugName() + "】为皮试药品");
            }
            //超量原因
            if (null != version && drugSuperScalarValidate(organId, recipeDetail)) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品药品超量错误天数问题UseDays");
            }
            //每次剂量
            if (ValidateUtil.validateObjects(recipeDetail.getUseDose()) && !"适量".equals(recipeDetail.getUseDoseStr())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品每次剂量是空UseDos");
            }
            //开药总数是否为空
            if (ValidateUtil.validateObjects(recipeDetail.getUseTotalDose())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品开药总数是空UseTotalDose");
            }
            //用药频次，用药途径是否在机构字典范围内ecipeDetailService validateDrug organDrug is null OrganDrugCode
            if (medicationsValidate(organDrug.getOrganId(), recipeDetail)) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
                recipeDetail.setValidateStatusText("机构药品频次，用药错误");
            }
        }
    }

    /**
     * 判断药品开药天数
     *
     * @param recipeType   处方类型
     * @param recipeDay    处方药物使用天数时间
     * @param recipeDetail 处方药品明细
     * @return
     */
    public void useDayValidate(Integer recipeType, String[] recipeDay, RecipeDetailBean recipeDetail) {
        // 校验中药
        if (RecipeUtil.isTcmType(recipeType)) {
            useDayValidate(recipeDay, recipeDetail);
            return;
        }
        // 校验西药
        boolean useDayValidate = useDayValidate(recipeDay, recipeDetail);
        if (useDayValidate) {
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            recipeDetail.setValidateStatusText("机构药品开药天数错误");
        }
    }

    /**
     * 判断药品超量
     *
     * @param organId      机构ID
     * @param recipeDetail 处方药品明细
     */
    public boolean drugSuperScalarValidate(Integer organId, RecipeDetailBean recipeDetail) {
        List<OrganDictionaryItemDTO> dictionaryItemDTOList = operationClient.findItemByOrgan(organId, DIC_BUS_TYPE);
        if (CollectionUtils.isEmpty(dictionaryItemDTOList)) {
            return false;
        }
        //当前开药天数超过7天,并且没有维护超量原因
        if (null != recipeDetail.getUseDays() && recipeDetail.getUseDays() > SUPER_SCALAR_DAYS && StringUtils.isEmpty(recipeDetail.getSuperScalarCode())) {
            recipeDetail.setSuperScalarCode(null);
            recipeDetail.setSuperScalarName(null);
            return true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getSuperScalarCode())) {
            OrganDictionaryItemDTO organDictionaryItemDTO = operationClient.getByDictionaryCode(DIC_BUS_TYPE, organId, recipeDetail.getSuperScalarCode());
            if (null == organDictionaryItemDTO) {
                recipeDetail.setSuperScalarCode(null);
                recipeDetail.setSuperScalarName(null);
                return true;
            }
        }
        return false;
    }


    /**
     * 校验煎法
     *
     * @param recipeExtendBean
     */
    public void validateDecoction(Integer organId, RecipeExtendBean recipeExtendBean) {
        String decoctionDeploy = configurationClient.getValueEnumCatch(organId, "decoctionDeploy", null);
        if (DECOCTION_DEPLOY_PATIENT.equals(decoctionDeploy)) {
            return;
        }
        if (StringUtils.isEmpty(recipeExtendBean.getDecoctionCode()) && StringUtils.isEmpty(recipeExtendBean.getDecoctionText())) {
            return;
        }
        List<DecoctionWay> decoctionWayList = drugDecoctionWayDao.findByOrganId(organId);
        if (CollectionUtils.isEmpty(decoctionWayList)) {
            decoctionWay(null, recipeExtendBean);
            return;
        }
        Map<String, DecoctionWay> mapCode = decoctionWayList.stream().collect(Collectors.toMap(DecoctionWay::getDecoctionCode, a -> a, (k1, k2) -> k1));
        DecoctionWay decoctionWay = mapCode.get(recipeExtendBean.getDecoctionCode());
        if (null != decoctionWay) {
            decoctionWay(decoctionWay, recipeExtendBean);
            return;
        }

        Map<String, DecoctionWay> mapText = decoctionWayList.stream().collect(Collectors.toMap(DecoctionWay::getDecoctionText, a -> a, (k1, k2) -> k1));
        decoctionWay = mapText.get(recipeExtendBean.getDecoctionText());
        if (null != decoctionWay) {
            decoctionWay(decoctionWay, recipeExtendBean);
            return;
        }
        decoctionWay(null, recipeExtendBean);
    }


    /**
     * 校验制法
     *
     * @param recipeExtendBean
     */
    public void validateMakeMethod(Integer organId, RecipeExtendBean recipeExtendBean) {
        if (StringUtils.isEmpty(recipeExtendBean.getMakeMethod()) && StringUtils.isEmpty(recipeExtendBean.getMakeMethodText())) {
            return;
        }
        List<DrugMakingMethod> drugMakingMethodList = drugMakingMethodDao.findByOrganId(organId);
        if (CollectionUtils.isEmpty(drugMakingMethodList)) {
            drugMakingMethod(null, recipeExtendBean);
            return;
        }
        Map<String, DrugMakingMethod> mapCode = drugMakingMethodList.stream().collect(Collectors.toMap(DrugMakingMethod::getMethodCode, a -> a, (k1, k2) -> k1));
        DrugMakingMethod drugMakingMethod = mapCode.get(recipeExtendBean.getMakeMethod());
        if (StringUtils.isNotEmpty(recipeExtendBean.getMakeMethod())) {
            drugMakingMethod(drugMakingMethod, recipeExtendBean);
            return;
        }

        Map<String, DrugMakingMethod> mapText = drugMakingMethodList.stream().collect(Collectors.toMap(DrugMakingMethod::getMethodText, a -> a, (k1, k2) -> k1));
        drugMakingMethod = mapText.get(recipeExtendBean.getMakeMethodText());
        if (StringUtils.isNotEmpty(recipeExtendBean.getMakeMethodText())) {
            drugMakingMethod(drugMakingMethod, recipeExtendBean);
        }
    }

    /**
     * 一张医保处方最多开几个中成药
     *
     * @param recipeBean
     * @param detailBeanList
     */
    public void validateMedicalChineDrugNumber(RecipeBean recipeBean, RecipeExtendBean recipeExtend, List<RecipeDetailBean> detailBeanList) {
        Integer medicalFlag = revisitManager.medicalFlag(recipeBean.getClinicId(), recipeBean.getBussSource());
        Integer chineDrugNumber = 5;
        //自费
        if (Integer.valueOf(1).equals(recipeExtend.getForceCashType()) ||
                (ValidateUtil.integerIsEmpty(recipeBean.getMedicalFlag()) && ValidateUtil.integerIsEmpty(medicalFlag))) {
            chineDrugNumber = configurationClient.getValueCatchReturnInteger(recipeBean.getClinicOrgan(), "selfMedicalChineDrugNumber", 5);
            logger.info("RecipeDetailValidateTool validateMedicalChineDrugNumber selfMedicalChineDrugNumber ={}", chineDrugNumber);
        }
        //医保
        if (!Integer.valueOf(1).equals(recipeExtend.getForceCashType()) &&
                (Integer.valueOf(1).equals(recipeBean.getMedicalFlag()) || Integer.valueOf(1).equals(medicalFlag))) {
            chineDrugNumber = configurationClient.getValueCatchReturnInteger(recipeBean.getClinicOrgan(), "medicalChineDrugNumber", 5);
            logger.info("RecipeDetailValidateTool validateMedicalChineDrugNumber medicalChineDrugNumber ={}", chineDrugNumber);
        }
        List<Integer> drugIds = detailBeanList.stream().map(RecipeDetailBean::getDrugId).collect(Collectors.toList());
        List<DrugList> drugList = drugManager.drugList(drugIds, 2);
        if (CollectionUtils.isEmpty(drugList)) {
            return;
        }
        if (drugList.size() > chineDrugNumber) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "中成药最多开具" + chineDrugNumber + "个");
        }
    }

    /**
     * 煎法
     *
     * @param decoctionWay
     * @param recipeExtendBean
     */
    private void decoctionWay(DecoctionWay decoctionWay, RecipeExtendBean recipeExtendBean) {
        if (null != decoctionWay) {
            recipeExtendBean.setDecoctionCode(decoctionWay.getDecoctionCode());
            recipeExtendBean.setDecoctionId(decoctionWay.getDecoctionId().toString());
            recipeExtendBean.setDecoctionText(decoctionWay.getDecoctionText());
            recipeExtendBean.setDecoctionExhibitionFlag(decoctionWay.getDecoctionExhibitionFlag());
        } else {
            recipeExtendBean.setDecoctionCode(null);
            recipeExtendBean.setDecoctionId(null);
            recipeExtendBean.setDecoctionText(null);
            recipeExtendBean.setDecoctionExhibitionFlag(null);
        }
    }

    /**
     * 制法
     *
     * @param drugMakingMethod
     * @param recipeExtendBean
     */
    private void drugMakingMethod(DrugMakingMethod drugMakingMethod, RecipeExtendBean recipeExtendBean) {
        if (null != drugMakingMethod) {
            recipeExtendBean.setMakeMethod(drugMakingMethod.getMethodCode());
            recipeExtendBean.setMakeMethodId(drugMakingMethod.getMethodId().toString());
            recipeExtendBean.setMakeMethodText(drugMakingMethod.getMethodText());
        } else {
            recipeExtendBean.setMakeMethod(null);
            recipeExtendBean.setMakeMethodId(null);
            recipeExtendBean.setMakeMethodText(null);
        }
    }

    /**
     * 服用要求
     *
     * @param requirementsForTaking
     * @param recipeExtendBean
     */
    private void requirementsForTaking(RequirementsForTaking requirementsForTaking, RecipeExtendBean recipeExtendBean) {
        if (null != requirementsForTaking) {
            recipeExtendBean.setRequirementsForTakingCode(requirementsForTaking.getCode());
            recipeExtendBean.setRequirementsForTakingId(String.valueOf(requirementsForTaking.getId()));
            recipeExtendBean.setRequirementsForTakingText(requirementsForTaking.getText());
        } else {
            recipeExtendBean.setRequirementsForTakingCode(null);
            recipeExtendBean.setRequirementsForTakingId(null);
            recipeExtendBean.setRequirementsForTakingText(null);
        }
    }


    /**
     * 开药天数是否在当前机构配置项天数范围内
     *
     * @param recipeDay    处方药物使用天数时间
     * @param recipeDetail 处方药品明细
     * @return
     */
    private boolean useDayValidate(String[] recipeDay, RecipeDetailBean recipeDetail) {
        boolean useDay = false;
        Double minUseDay = Double.valueOf(recipeDay[0]);
        Double maxUseDay = Double.valueOf(recipeDay[1]);
        if (null == recipeDetail.getUseDays() || recipeDetail.getUseDays() < minUseDay || recipeDetail.getUseDays() > maxUseDay) {
            recipeDetail.setUseDays(0);
            useDay = true;
        }
        if (StringUtils.isEmpty(recipeDetail.getUseDaysB())) {
            recipeDetail.setUseDaysB("0");
            useDay = true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getUseDaysB()) && (Double.valueOf(recipeDetail.getUseDaysB()) < minUseDay || Double.valueOf(recipeDetail.getUseDaysB()) > maxUseDay)) {
            recipeDetail.setUseDaysB("0");
            useDay = true;
        }
        return useDay;
    }

    /**
     * 用药频次，用药途径是否在机构字典范围内
     *
     * @param organId
     * @param recipeDetail
     * @return
     */
    private boolean medicationsValidate(Integer organId, RecipeDetailBean recipeDetail) {
        boolean us = false;
        UsingRateDTO usingRateDTO = drugManager.usingRate(organId, recipeDetail.getOrganUsingRate());
        if (null == usingRateDTO) {
            recipeDetail.setUsingRate(null);
            recipeDetail.setUsingRateTextFromHis(null);
            recipeDetail.setUsingRateId(null);
            recipeDetail.setOrganUsingRate(null);
            us = true;
        } else {
            recipeDetail.setUsingRate(usingRateDTO.getRelatedPlatformKey());
            recipeDetail.setUsingRateId(String.valueOf(usingRateDTO.getId()));
        }
        UsePathwaysDTO usePathwaysDTO = drugManager.usePathways(organId, recipeDetail.getOrganUsePathways(), recipeDetail.getDrugType());
        if (null == usePathwaysDTO) {
            recipeDetail.setUsePathways(null);
            recipeDetail.setUsePathwaysTextFromHis(null);
            recipeDetail.setUsePathwaysId(null);
            recipeDetail.setOrganUsePathways(null);
            us = true;
        } else {
            recipeDetail.setUsePathways(usePathwaysDTO.getRelatedPlatformKey());
            recipeDetail.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
        }
        return us;
    }

    /**
     * 校验中药嘱托
     *
     * @param recipeDetail       处方明细数据
     * @param drugEntrustNameMap 机构嘱托
     * @return
     */
    public boolean entrustValidate(RecipeDetailBean recipeDetail, Map<String, DrugEntrust> drugEntrustNameMap) {
        if (StringUtils.isEmpty(recipeDetail.getMemo())) {
            return false;
        }
        //嘱托
        DrugEntrust drugEntrust = drugEntrustNameMap.get(recipeDetail.getMemo());
        if (null != drugEntrust) {
            recipeDetail.setDrugEntrustCode(drugEntrust.getDrugEntrustCode());
            recipeDetail.setEntrustmentId(String.valueOf(drugEntrust.getDrugEntrustId()));
            recipeDetail.setMemo(drugEntrust.getDrugEntrustName());
            return false;
        }
        recipeDetail.setDrugEntrustCode(null);
        recipeDetail.setEntrustmentId(null);
        recipeDetail.setMemo(null);
        return true;
    }


    /**
     * 校验服用要求
     * 此逻辑放在校验煎法后
     * @param organId
     * @param recipeExtendBean
     */
    public void validateRequirementsForTaking(Integer organId, RecipeExtendBean recipeExtendBean) {
        //强制要求code一定要给
        if (StringUtils.isEmpty(recipeExtendBean.getRequirementsForTakingCode()) && StringUtils.isEmpty(recipeExtendBean.getRequirementsForTakingText())) {
            return;
        }
        //拿到历史处方煎法
        String writeRecipeSelectDecoctionId = recipeExtendBean.getDecoctionId();
        //处方煎法煎法关联的服用要求（如果服用要求被删掉了，通过煎法匹配会查不出来==》不会包含当前服用要求=》清空 ）
        List<RequirementsForTakingDTO> requirementsForTakingDTOList = recipeManager.findRequirementsForTakingByDecoctionId(organId, StringUtils.isEmpty(writeRecipeSelectDecoctionId) ? null : Integer.parseInt(writeRecipeSelectDecoctionId));
        //是否包含历史处方上的服用要求
        if (CollectionUtils.isEmpty(requirementsForTakingDTOList)) {
            requirementsForTaking(null, recipeExtendBean);
            return;
        }
        for (RequirementsForTakingDTO requirementsForTakingDTO : requirementsForTakingDTOList) {
            if (StringUtils.isNotEmpty(requirementsForTakingDTO.getCode()) && requirementsForTakingDTO.getCode().equals(recipeExtendBean.getRequirementsForTakingCode())) {
                //包含
                requirementsForTaking(ObjectCopyUtils.convert(requirementsForTakingDTO, RequirementsForTaking.class), recipeExtendBean);
                return;
            }
        }
        requirementsForTaking(null, recipeExtendBean);
    }

    /**
     * 判断中药处方剂型
     *
     * @param recipeType
     * @param recipeDrugForm
     * @param organDrug
     * @return
     */
    public boolean validateDrugForm(Integer recipeType, Integer recipeDrugForm, OrganDrugList organDrug, RecipeDetailBean a) {
        a.setDrugForm(organDrug.getDrugForm());
        if (!RecipeUtil.isTcmType(recipeType)) {
            return false;
        }
        if (RecipeDrugFormTypeEnum.getDrugForm(recipeDrugForm).equals(organDrug.getDrugForm())) {
            return false;
        }
        return true;
    }
}
