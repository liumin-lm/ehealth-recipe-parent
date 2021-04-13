package recipe.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.DrugEntrust;
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
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.dao.DrugEntrustDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.service.client.DrugClient;
import recipe.service.client.IConfigurationClient;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;

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

    @Autowired
    private DrugEntrustDAO drugEntrustDAO;

    /**
     * 校验线上线下 药品数据
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    public List<RecipeDetailBean> validateDrug(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(organId, recipeType);
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
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, recipeType));
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
            if (ValidateUtil.integerIsEmpty(a.getDrugId()) && 1 == organDrugs.size()) {
                organDrug = organDrugs.get(0);
            }
            if (!ValidateUtil.integerIsEmpty(a.getDrugId())) {
                for (OrganDrugList drug : organDrugs) {
                    if (drug.getDrugId().equals(a.getDrugId())) {
                        organDrug = drug;
                        //设置剂型--后面药品名拼接会用到
                        a.setDrugForm(drug.getDrugForm());
                        break;
                    }
                }
            }
            if (null == organDrug) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                logger.info("RecipeDetailService validateDrug organDrug is null OrganDrugCode ：  {}", a.getOrganDrugCode());
                return;
            }
            //校验药品药房是否变动
            if (pharmacyVariation(a.getPharmacyId(), a.getPharmacyCode(), organDrug.getPharmacy(), pharmacyCodeMap)) {
                a.setValidateStatus(VALIDATE_STATUS_FAILURE);
                logger.info("RecipeDetailService validateDrug pharmacy OrganDrugCode ：= {}", a.getOrganDrugCode());
                return;
            }
            //校验数据是否完善
            validateDrug(a, recipeDay, organDrug, recipeType);
            /**返回前端必须字段*/
            a.setStatus(organDrug.getStatus());
            a.setDrugId(organDrug.getDrugId());
            if (CollectionUtils.isEmpty(a.getUseDoseAndUnitRelation())) {
                List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = new LinkedList<>();
                if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
                }
                if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
                }
                a.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
            }
            //续方也会走这里但是 续方要用药品名实时配置
            a.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(a, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
        });
        return recipeDetails;
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
    private boolean pharmacyVariation(Integer commonPharmacyId, String pharmacyCode, String pharmacy, Map<String, PharmacyTcm> pharmacyCodeMap) {
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
    private void validateDrug(RecipeDetailBean recipeDetail, String[] recipeDay, OrganDrugList organDrug, Integer recipeType) {
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
            if (entrustValidate(organDrug.getOrganId(), recipeDetail)) {
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
     * @param organId      机构id
     * @param recipeDetail 处方明细数据
     * @return
     */
    private boolean entrustValidate(Integer organId, RecipeDetailBean recipeDetail) {
        if (StringUtils.isEmpty(recipeDetail.getDrugEntrustCode()) && StringUtils.isEmpty(recipeDetail.getMemo())) {
            return false;
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        if (CollectionUtils.isEmpty(drugEntrusts)) {
            recipeDetail.setEntrustmentId(null);
            recipeDetail.setMemo(null);
            return true;
        }
        if (StringUtils.isNotEmpty(recipeDetail.getDrugEntrustCode())) {
            boolean code = drugEntrusts.stream().noneMatch(a -> a.getDrugEntrustCode().equals(recipeDetail.getDrugEntrustCode()));
            if (code) {
                recipeDetail.setEntrustmentId(null);
                recipeDetail.setMemo(null);
                return true;
            }
        } else if (StringUtils.isNotEmpty(recipeDetail.getMemo())) {
            boolean name = drugEntrusts.stream().noneMatch(a -> a.getDrugEntrustName().equals(recipeDetail.getMemo()));
            if (name) {
                recipeDetail.setEntrustmentId(null);
                recipeDetail.setMemo(null);
                return true;
            }
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
