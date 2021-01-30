package recipe.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.service.client.DrugClient;
import recipe.service.client.IConfigurationClient;
import recipe.util.ByteUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailService {
    /**
     * 校验药品状态 0:正常，1已失效，2未完善
     */
    private final static int VALIDATE_STATUS_YES = 0;
    private final static int VALIDATE_STATUS_FAILURE = 1;
    private final static int VALIDATE_STATUS_PERFECT = 2;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private DrugClient drugClient;
    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 校验线上线下 药品数据
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    public List<RecipeDetailBean> validateDrug(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(organId);
        //药房信息
        List<PharmacyTcm> pharmacyList = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("RecipeDetailService validateDrug pharmacyList= {}", JSON.toJSONString(pharmacyList));
        Map<String, PharmacyTcm> pharmacyCodeMap = Optional.ofNullable(pharmacyList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyCode, a -> a, (k1, k2) -> k1));
        //查询机构药品
        List<String> organDrugCodeList = recipeDetails.stream().map(RecipeDetailBean::getOrganDrugCode).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        logger.info("RecipeDetailService validateDrug organDrugList= {}", JSON.toJSONString(organDrugList));
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
        //校验数据判断状态
        recipeDetails.forEach(a -> {
            a.setValidateStatus(VALIDATE_STATUS_YES);
            //校验药品存在
            if (StringUtils.isEmpty(a.getOrganDrugCode())) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                return;
            }
            List<OrganDrugList> organDrugs = organDrugGroup.get(a.getOrganDrugCode());
            if (CollectionUtils.isEmpty(organDrugs)) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                return;
            }
            //校验比对药品
            OrganDrugList organDrug = null;
            if (null == a.getDrugId() && 1 == organDrugs.size()) {
                organDrug = organDrugs.get(0);
            }
            if (null != a.getDrugId()) {
                for (OrganDrugList drug : organDrugs) {
                    if (drug.getDrugId().equals(a.getDrugId())) {
                        organDrug = drug;
                        break;
                    }
                }
            }
            if (null == organDrug) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                return;
            }
            //校验药品药房是否变动
            if (pharmacyVariation(a.getPharmacyId(), a.getPharmacyCode(), organDrug.getPharmacy(), pharmacyCodeMap)) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                return;
            }
            //校验数据是否完善
            if (RecipeUtil.isTcmType(recipeType)) {
                validateChineDrug(a, recipeDay, organDrug);
            } else {
                validateDrug(a, recipeDay, organDrug);
            }
        });
        return recipeDetails;
    }


    /**
     * 校验药品药房是否变动
     *
     * @param commonPharmacyId 常用方药房id
     * @param pharmacy         机构药房id
     * @return true 不一致
     */
    private boolean pharmacyVariation(Integer commonPharmacyId, String pharmacyCode, String pharmacy, Map<String, PharmacyTcm> pharmacyCodeMap) {
        if (null == commonPharmacyId && StringUtils.isEmpty(pharmacyCode) && StringUtils.isNotEmpty(pharmacy)) {
            return true;
        }
        if (null == commonPharmacyId && StringUtils.isNotEmpty(pharmacyCode)) {
            PharmacyTcm pharmacyTcm = pharmacyCodeMap.get(pharmacyCode);
            if (null == pharmacyTcm) {
                return true;
            }
            commonPharmacyId = pharmacyTcm.getPharmacyId();
        }

        if (null == commonPharmacyId && StringUtils.isNotEmpty(pharmacy)) {
            return true;
        }
        if (null != commonPharmacyId && StringUtils.isEmpty(pharmacy)) {
            return true;
        }
        if (null != commonPharmacyId && StringUtils.isNotEmpty(pharmacy) &&
                !Arrays.asList(pharmacy.split(ByteUtils.COMMA)).contains(String.valueOf(commonPharmacyId))) {
            return true;
        }
        return false;
    }


    /**
     * 校验西药 数据是否完善
     * 错误数据设置为null
     *
     * @param recipeDetail 处方明细数据
     * @param recipeDay    处方药物使用天数时间
     * @param organDrug    机构药品
     */
    private void validateDrug(RecipeDetailBean recipeDetail, String[] recipeDay, OrganDrugList organDrug) {
        if (null == recipeDetail.getUseDose() || null == recipeDetail.getUseTotalDose()) {
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        if (StringUtils.isEmpty(recipeDetail.getDosageUnit()) || (!recipeDetail.getDosageUnit().equals(organDrug.getUseDoseUnit())
                && !recipeDetail.getDosageUnit().equals(organDrug.getUseDoseSmallestUnit()))) {
            recipeDetail.setDosageUnit(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        UsingRateDTO usingRateDTO = drugClient.usingRate(organDrug.getOrganId(), recipeDetail.getUsingRate());
        if (null == usingRateDTO) {
            recipeDetail.setUsingRate(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        UsePathwaysDTO usePathwaysDTO = drugClient.usePathways(organDrug.getOrganId(), recipeDetail.getUsePathways());
        if (null == usePathwaysDTO) {
            recipeDetail.setUsePathways(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        Integer minUseDay = Integer.valueOf(recipeDay[0]);
        Integer maxUseDay = Integer.valueOf(recipeDay[1]);
        if (null == recipeDetail.getUseDays() || recipeDetail.getUseDays() > minUseDay || recipeDetail.getUseDays() < maxUseDay) {
            recipeDetail.setUseDays(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        if (null == recipeDetail.getUseDaysB() || Double.valueOf(recipeDetail.getUseDaysB()) > minUseDay || Double.valueOf(recipeDetail.getUseDaysB()) < maxUseDay) {
            recipeDetail.setUseDaysB(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
    }

    /**
     * 校验中药 数据是否完善
     * 错误数据设置为null
     *
     * @param recipeDetail 处方明细数据
     * @param recipeDay    处方药物使用天数时间
     * @param organDrug    机构药品
     */
    private void validateChineDrug(RecipeDetailBean recipeDetail, String[] recipeDay, OrganDrugList organDrug) {
        if (null == recipeDetail.getUseDose()) {
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        if (StringUtils.isEmpty(recipeDetail.getDosageUnit()) || (!recipeDetail.getDosageUnit().equals(organDrug.getUseDoseUnit())
                && !recipeDetail.getDosageUnit().equals(organDrug.getUseDoseSmallestUnit()))) {
            recipeDetail.setDosageUnit(null);
            recipeDetail.setValidateStatus(VALIDATE_STATUS_PERFECT);
        }
        UsingRateDTO usingRateDTO = drugClient.usingRate(organDrug.getOrganId(), recipeDetail.getUsingRate());
        if (null == usingRateDTO) {
            recipeDetail.setUsingRate(null);
        }
        UsePathwaysDTO usePathwaysDTO = drugClient.usePathways(organDrug.getOrganId(), recipeDetail.getUsePathways());
        if (null == usePathwaysDTO) {
            recipeDetail.setUsePathways(null);
        }
        Integer minUseDay = Integer.valueOf(recipeDay[0]);
        Integer maxUseDay = Integer.valueOf(recipeDay[1]);
        if (null == recipeDetail.getUseDays() || recipeDetail.getUseDays() > minUseDay || recipeDetail.getUseDays() < maxUseDay) {
            recipeDetail.setUseDays(null);
        }
        if (null == recipeDetail.getUseDaysB() || Double.valueOf(recipeDetail.getUseDaysB()) > minUseDay || Double.valueOf(recipeDetail.getUseDaysB()) < maxUseDay) {
            recipeDetail.setUseDaysB(null);
        }
    }


}
