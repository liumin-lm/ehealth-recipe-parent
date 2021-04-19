package recipe.drugTool.validate;

import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.service.client.DrugClient;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 处方明细校验类
 *
 * @author fuzi
 */
@Service
public class RecipeDetailValidateTool {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private DrugClient drugClient;

    /**
     * 校验药品状态 0:正常，1已失效，2未完善
     */
    private final static int VALIDATE_STATUS_YES = 0;
    public final static Integer VALIDATE_STATUS_FAILURE = 1;
    private final static int VALIDATE_STATUS_PERFECT = 2;

    /**
     * 校验比对药品
     *
     * @param recipeDetailBean 处方明细
     * @param organDrugGroup   机构药品组
     * @return
     */
    public OrganDrugList validateOrganDrug(RecipeDetailBean recipeDetailBean, Map<String, List<OrganDrugList>> organDrugGroup) {
        recipeDetailBean.setValidateStatus(VALIDATE_STATUS_YES);
        //校验药品存在
        if (StringUtils.isEmpty(recipeDetailBean.getOrganDrugCode())) {
            recipeDetailBean.setValidateStatus(VALIDATE_STATUS_FAILURE);
            return null;
        }
        List<OrganDrugList> organDrugs = organDrugGroup.get(recipeDetailBean.getOrganDrugCode());
        if (CollectionUtils.isEmpty(organDrugs)) {
            recipeDetailBean.setValidateStatus(VALIDATE_STATUS_FAILURE);
            return null;
        }
        //校验比对药品
        OrganDrugList organDrug = null;
        if (ValidateUtil.integerIsEmpty(recipeDetailBean.getDrugId()) && 1 == organDrugs.size()) {
            organDrug = organDrugs.get(0);
        }
        if (!ValidateUtil.integerIsEmpty(recipeDetailBean.getDrugId())) {
            for (OrganDrugList drug : organDrugs) {
                if (drug.getDrugId().equals(recipeDetailBean.getDrugId())) {
                    organDrug = drug;
                    break;
                }
            }
        }
        if (null == organDrug) {
            recipeDetailBean.setValidateStatus(VALIDATE_STATUS_FAILURE);
            logger.info("RecipeDetailService validateDrug organDrug is null OrganDrugCode ：  {}", recipeDetailBean.getOrganDrugCode());
            return null;
        }
        //设置剂型
        recipeDetailBean.setDrugForm(organDrug.getDrugForm());
        return organDrug;
    }

    /**
     * 校验药品药房是否变动
     *
     * @param commonPharmacyId 续房药房id
     * @param pharmacyCode     续方药房code
     * @param pharmacy         机构药房id
     * @param pharmacyCodeMap  药房表信息
     * @return true 不一致
     */
    public boolean pharmacyVariation(Integer commonPharmacyId, String pharmacyCode, String pharmacy, Map<String, PharmacyTcm> pharmacyCodeMap) {
        if (ValidateUtil.integerIsEmpty(commonPharmacyId) && StringUtils.isEmpty(pharmacyCode) && StringUtils.isNotEmpty(pharmacy)) {
            return true;
        }
        if (ValidateUtil.integerIsEmpty(commonPharmacyId) && StringUtils.isNotEmpty(pharmacyCode)) {
            PharmacyTcm pharmacyTcm = pharmacyCodeMap.get(pharmacyCode);
            if (null == pharmacyTcm) {
                return true;
            }
            commonPharmacyId = pharmacyTcm.getPharmacyId();
        }
        if (ValidateUtil.integerIsEmpty(commonPharmacyId) && StringUtils.isNotEmpty(pharmacy)) {
            return true;
        }
        if (!ValidateUtil.integerIsEmpty(commonPharmacyId) && StringUtils.isEmpty(pharmacy)) {
            return true;
        }
        if (!ValidateUtil.integerIsEmpty(commonPharmacyId) && StringUtils.isNotEmpty(pharmacy) &&
                !Arrays.asList(pharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(commonPharmacyId))) {
            return true;
        }
        return false;
    }


    /**
     * 校验药 数据是否完善
     * 错误数据设置为null
     *
     * @param recipeDetail 处方明细数据
     * @param recipeDay    处方药物使用天数时间
     * @param organDrug    机构药品
     */
    public void validateDrug(RecipeDetailBean recipeDetail, String[] recipeDay, OrganDrugList organDrug, Integer recipeType, List<DrugEntrustDTO> drugEntrusts) {
        //剂量单位是否与机构药品目录单位一致
        if (StringUtils.isEmpty(recipeDetail.getUseDoseUnit()) || (!recipeDetail.getUseDoseUnit().equals(organDrug.getUseDoseUnit())
                && !recipeDetail.getUseDoseUnit().equals(organDrug.getUseDoseSmallestUnit()))) {
            recipeDetail.setUseDoseUnit(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        
        /**校验中药 数据是否完善*/
        if (RecipeUtil.isTcmType(recipeType)) {
            //每次剂量
            if (ValidateUtil.doubleIsEmpty(recipeDetail.getUseDose())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
            if (entrustValidate(recipeDetail, drugEntrusts)) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
            //用药频次，用药途径是否在机构字典范围内
            medicationsValidate(organDrug.getOrganId(), recipeDetail);
            useDayValidate(recipeDay, recipeDetail);
        } else {
            /**校验西药 数据是否完善*/
            //每次剂量
            if (ValidateUtil.doubleIsEmpty(recipeDetail.getUseDose()) && !"适量".equals(recipeDetail.getUseDoseStr())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
            //开药总数是否为空
            if (ValidateUtil.doubleIsEmpty(recipeDetail.getUseTotalDose())) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
            //用药频次，用药途径是否在机构字典范围内
            if (medicationsValidate(organDrug.getOrganId(), recipeDetail)) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
            if (useDayValidate(recipeDay, recipeDetail)) {
                recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
            }
        }
    }

    /**
     * 校验中药嘱托
     *
     * @param drugEntrusts 机构嘱托
     * @param recipeDetail 处方明细数据
     * @return
     */
    private boolean entrustValidate(RecipeDetailBean recipeDetail, List<DrugEntrustDTO> drugEntrusts) {
        if (StringUtils.isEmpty(recipeDetail.getDrugEntrustCode()) && StringUtils.isEmpty(recipeDetail.getMemo())) {
            return true;
        }
        if (CollectionUtils.isEmpty(drugEntrusts)) {
            recipeDetail.setEntrustmentId(null);
            recipeDetail.setMemo(null);
            return true;
        }
        boolean entrusts = true;
        for (DrugEntrustDTO drugEntrustDTO : drugEntrusts) {
            if (drugEntrustDTO.getDrugEntrustCode().equals(recipeDetail.getDrugEntrustCode())) {
                entrusts = false;
                recipeDetail.setEntrustmentId(drugEntrustDTO.getDrugEntrustId().toString());
                break;
            } else if (drugEntrustDTO.getDrugEntrustName().equals(recipeDetail.getMemo())) {
                entrusts = false;
                recipeDetail.setEntrustmentId(drugEntrustDTO.getDrugEntrustId().toString());
                break;
            }
        }
        if (entrusts) {
            recipeDetail.setEntrustmentId(null);
            recipeDetail.setMemo(null);
            return true;
        }
        return false;
    }

    /**
     * 开药天数是否在当前机构配置项天数范围内
     *
     * @param recipeDay    处方药物使用天数时间
     * @param recipeDetail
     * @return
     */
    private boolean useDayValidate(String[] recipeDay, RecipeDetailBean recipeDetail) {
        boolean useDay = false;
        Double minUseDay = Double.valueOf(recipeDay[0]);
        Double maxUseDay = Double.valueOf(recipeDay[1]);
        if (null == recipeDetail.getUseDays() || recipeDetail.getUseDays() < minUseDay || recipeDetail.getUseDays() > maxUseDay) {
            recipeDetail.setUseDays(null);
            useDay = true;
        }
        if (StringUtils.isEmpty(recipeDetail.getUseDaysB())) {
            recipeDetail.setUseDaysB(null);
            useDay = true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getUseDaysB()) && (Double.valueOf(recipeDetail.getUseDaysB()) < minUseDay || Double.valueOf(recipeDetail.getUseDaysB()) > maxUseDay)) {
            recipeDetail.setUseDaysB(null);
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
        UsingRateDTO usingRateDTO = drugClient.usingRate(organId, recipeDetail.getOrganUsingRate());
        if (null == usingRateDTO) {
            recipeDetail.setUsingRate(null);
            recipeDetail.setUsingRateTextFromHis(null);
            recipeDetail.setUsingRateId(null);
            recipeDetail.setOrganUsingRate(null);
            us = true;
        } else {
            recipeDetail.setUsingRate(usingRateDTO.getUsingRateKey());
            recipeDetail.setUsingRateId(String.valueOf(usingRateDTO.getId()));
        }
        UsePathwaysDTO usePathwaysDTO = drugClient.usePathways(organId, recipeDetail.getOrganUsePathways());
        if (null == usePathwaysDTO) {
            recipeDetail.setUsePathways(null);
            recipeDetail.setUsePathwaysTextFromHis(null);
            recipeDetail.setUsePathwaysId(null);
            recipeDetail.setOrganUsePathways(null);
            us = true;
        } else {
            recipeDetail.setUsePathways(usePathwaysDTO.getPathwaysKey());
            recipeDetail.setUsePathwaysId(String.valueOf(usePathwaysDTO.getId()));
        }
        return us;
    }
}
