package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.RecipePreSettleDrugFeeDTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.JSONUtils;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.PharmacyTcmDAO;
import recipe.enumerate.type.DrugBelongTypeEnum;
import recipe.util.JsonUtil;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailManager extends BaseManager {
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

    /**
     * 保存预结算返回药品详细信息
     *
     * @param recipePreSettleDrugFeeDTOS 预结算返回信息
     * @return
     */
    public void saveRecipePreSettleDrugFeeDTOS(List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTOS,List<Integer> recipeIds) {
        logger.info("RecipeDetailManager saveRecipePreSettleDrugFeeDTOS  recipePreSettleDrugFeeDTOS = {}"
                , JSON.toJSONString(recipePreSettleDrugFeeDTOS));
        try {
            // 保存预结算返回药品详细信息
            if (CollectionUtils.isNotEmpty(recipePreSettleDrugFeeDTOS)) {
                Map<String, List<RecipePreSettleDrugFeeDTO>> collect = recipePreSettleDrugFeeDTOS.stream().collect(Collectors.groupingBy(a -> a.getRecipeCode() + a.getOrganDrugCode()));
                List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
                List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
                Map<Integer, String> recipeCodeMap = recipes.stream().collect(Collectors.toMap(Recipe::getRecipeId, Recipe::getRecipeCode));
                for (Recipedetail recipeDetail : recipeDetails) {
                    String recipeCode = recipeCodeMap.get(recipeDetail.getRecipeId());
                    String[] split = recipeCode.split(",");
                    for (String s : split) {
                        List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTO = collect.get(s + recipeDetail.getOrganDrugCode());
                        if (CollectionUtils.isNotEmpty(recipePreSettleDrugFeeDTO)) {
                            recipeDetail.setHisReturnSalePrice(recipePreSettleDrugFeeDTO.get(0).getSalePrice());
                            recipeDetail.setDrugCost(recipePreSettleDrugFeeDTO.get(0).getDrugCost());
                        }
                    }
                }
                recipeDetailDAO.updateAllRecipeDetail(recipeDetails);
            }
        }catch (Exception e){
            logger.error("saveRecipePreSettleDrugFeeDTOS recipeIds={} error", JsonUtil.toString(recipeIds), e);
        }
    }

    /**
     * 开方his回写
     *
     * @param recipePreSettleDrugFeeDTOS 预结算返回信息
     * @return
     */
    public void saveRecipeDetailBySendSuccess(List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTOS,Integer recipeId) {
        logger.info("RecipeDetailManager saveRecipePreSettleDrugFeeDTOS  saveRecipeDetailBySendSuccess = {},recipeId={}"
                , JSON.toJSONString(recipePreSettleDrugFeeDTOS),recipeId);

        try {
            // 保存开方his返回药品详细信息
            if (CollectionUtils.isNotEmpty(recipePreSettleDrugFeeDTOS)) {
                Map<String, List<RecipePreSettleDrugFeeDTO>> collect = recipePreSettleDrugFeeDTOS.stream().collect(Collectors.groupingBy(a -> a.getOrganDrugCode()));
                List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
                for (Recipedetail recipeDetail : recipeDetails) {
                    List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTO = collect.get(recipeDetail.getOrganDrugCode());
                    if (CollectionUtils.isNotEmpty(recipePreSettleDrugFeeDTO)) {
                        recipeDetail.setDrugCost(recipePreSettleDrugFeeDTO.get(0).getDrugCost());
                        recipeDetail.setHisReturnSalePrice(recipePreSettleDrugFeeDTO.get(0).getSalePrice());
                    }
                }
                recipeDetailDAO.updateAllRecipeDetail(recipeDetails);
            }
        } catch (Exception e) {
            logger.error("saveRecipePreSettleDrugFeeDTOS recipeIds={} error", JsonUtil.toString(recipeId), e);
        }
    }

    /**
     * 保存处方药品信息
     *
     * @param recipeDetails 药品信息
     * @param recipe        处方信息
     */
    public void saveRecipeDetails(List<Recipedetail> recipeDetails, Recipe recipe) {
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return;
        }
        Integer organId = recipe.getClinicOrgan();
        List<String> organDrugCodes = recipeDetails.stream().map(Recipedetail::getOrganDrugCode).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodes);
        logger.info("RecipeDetailManager saveRecipeDetails organDrugList = {}", JSON.toJSONString(organDrugList));
        if (CollectionUtils.isEmpty(organDrugList)) {
            organDrugList = new LinkedList<>();
        }
        Map<String, OrganDrugList> organDrugListMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getOrganDrugCode() + k.getDrugId(), a -> a, (k1, k2) -> k1));
        //用药途径 用药频次
        Map<Integer, UsePathways> usePathwaysMap = drugClient.usePathwaysMap(organId);
        Map<Integer, UsingRate> usingRateMap = drugClient.usingRateMap(organId);
        //设置药品默认字段-处方药品默认数据
        recipeDetails.forEach(a -> this.setRecipeDetail(a, recipe.getRecipeId(), organDrugListMap, usePathwaysMap, usingRateMap));
        //设置药品金额等-处方默认数据
        Recipe recipeUpdate = drugClient.updateRecipe(recipe, recipeDetails, organDrugList);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("RecipeDetailManager saveRecipeDetails recipeUpdate = {}, organDrugList = {}", JSON.toJSONString(recipeUpdate), JSON.toJSONString(organDrugList));
        //保存处方明细
        this.saveRecipeDetails(recipeDetails);
    }


    /**
     * 批量查询处方明细
     *
     * @param recipeIds 处方id
     * @return 处方明细
     */
    public Map<Integer, List<Recipedetail>> findRecipeDetailMap(List<Integer> recipeIds) {
        List<Recipedetail> recipeDetails = this.findRecipeDetails(recipeIds);
        return Optional.ofNullable(recipeDetails).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
    }

    public List<Recipedetail> findRecipeDetails(List<Integer> recipeIds) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            return null;
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        logger.info("RecipeDetailManager findRecipeDetails recipeDetails:{}", JSON.toJSONString(recipeDetails));
        return recipeDetails;
    }

    public Map<String, Double> findRecipeDetailSumTotalDose(List<Integer> recipeIds) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            return null;
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findRecipeDetailSumTotalDose(recipeIds);
        logger.info("RecipeDetailManager findRecipeDetailSumTotalDose recipeDetails:{}", JSON.toJSONString(recipeDetails));
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        return recipeDetails.stream().collect(Collectors.toMap(Recipedetail::getOrganDrugCode, Recipedetail::getUseTotalDose));
    }

    /**
     * 过滤保密处方，重新设置保密处方信息
     *
     * @param recipePdfDTO
     */
    public void filterSecrecyDrug(RecipeInfoDTO recipePdfDTO) {
        logger.info("RecipeDetailManager filterSecrecyDrug begin recipePdfDTO:{}", JSON.toJSONString(recipePdfDTO));
        List<Recipedetail> recipeDetailList = recipePdfDTO.getRecipeDetails();
        List<Recipedetail> secrecyRecipeDetailList = recipeDetailList.stream().filter(recipeDetail -> DrugBelongTypeEnum.SECRECY_DRUG.getType().equals(recipeDetail.getType())).collect(Collectors.toList());
        if (org.springframework.util.CollectionUtils.isEmpty(secrecyRecipeDetailList)) {
            return;
        }
        List<Recipedetail> recipeDetails = new ArrayList<>();
        Recipe recipe = recipePdfDTO.getRecipe();
        List<Recipedetail> noSecrecyRecipeDetailList = recipeDetailList.stream().filter(recipeDetail -> !DrugBelongTypeEnum.SECRECY_DRUG.getType().equals(recipeDetail.getType())).collect(Collectors.toList());
        Recipedetail secrecyRecipeDetail = new Recipedetail();
        secrecyRecipeDetail.setDrugName(recipe.getOfflineRecipeName());
        secrecyRecipeDetail.setType(DrugBelongTypeEnum.SECRECY_DRUG.getType());
        secrecyRecipeDetail.setDrugDisplaySplicedName(recipe.getOfflineRecipeName());
        BigDecimal drugCost = secrecyRecipeDetailList.stream().filter(recipeDetail -> DrugBelongTypeEnum.SECRECY_DRUG.getType().equals(recipeDetail.getType())).map(Recipedetail::getDrugCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        secrecyRecipeDetail.setDrugCost(drugCost);
        recipeDetails.add(secrecyRecipeDetail);
        recipeDetails.addAll(noSecrecyRecipeDetailList);
        recipePdfDTO.setRecipeDetails(recipeDetails);
        logger.info("RecipeDetailManager filterSecrecyDrug end recipePdfDTO:{}", JSON.toJSONString(recipePdfDTO));
    }

    /**
     * 获取项目数量
     * @param recipeId recipeId
     * @return 数量
     */
    public Long getCountByRecipeId(Integer recipeId){
        return recipeDetailDAO.getCountByRecipeId(recipeId);
    }

    /**
     * 获取处方详情列表
     *
     * @param recipeId 处方ID
     * @return 处方详情列表
     */
    public List<Recipedetail> findByRecipeId(Integer recipeId) {
        return recipeDetailDAO.findByRecipeId(recipeId);
    }

    /**
     * 校验his 药品规则，大病医保等
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetails, String registerId, String dbType) {
        // 请求his
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setDbType(dbType);
        request.setRegisterID(registerId);
        request.setPatId(recipe.getPatientID());
        if (!ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (null != revisitExDTO) {
                request.setPatId(revisitExDTO.getPatId());
                request.setCardType(revisitExDTO.getCardType());
                request.setCardId(revisitExDTO.getCardId());
                request.setRegisterID(revisitExDTO.getRegisterNo());
                request.setDbType(revisitExDTO.getDbType());
            }
        }
        Set<Integer> pharmaIds = new HashSet<>();
        List<Integer> drugIdList = recipeDetails.stream().map(a -> {
            pharmaIds.add(a.getPharmacyId());
            return a.getDrugId();
        }).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
        DoctorDTO doctorDTO = doctorClient.jobNumber(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart());
        PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
        //科室代码
        AppointDepartDTO appointDepart = departClient.getAppointDepartByOrganIdAndDepart(recipe);
        String appointDepartCode = null != appointDepart ? appointDepart.getAppointDepartCode() : "";
        request.setOrganId(recipe.getClinicOrgan());
        request.setJobNumber(doctorDTO.getJobNumber());
        request.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.patient.dto.PatientDTO.class));
        request.setAppointDepartCode(appointDepartCode);
        offlineRecipeClient.hisDrugRule(recipeDetails, organDrugList, pharmacyTcmByIds, request);
    }

    /**
     * 计算处方总金额
     *
     * @param recipeType 处方类型
     * @param detailList 处方明细
     * @param recipe     处方数据
     * @return 处方总金额
     */
    public BigDecimal totalMoney(Integer recipeType, List<Recipedetail> detailList, Recipe recipe) {
        return drugClient.totalMoney(recipeType, detailList, recipe);
    }


    /**
     * 保存处方明细
     *
     * @param details 药品信息
     * @return
     */
    private List<Recipedetail> saveRecipeDetails(List<Recipedetail> details) {
        if (CollectionUtils.isEmpty(details)) {
            return details;
        }
        Integer recipeId = details.get(0).getRecipeId();
        recipeDetailDAO.updateDetailInvalidByRecipeId(recipeId);
//        for (Recipedetail detail : details) {
//            if (ValidateUtil.integerIsEmpty(detail.getRecipeDetailId())) {
//                recipeDetailDAO.save(detail);
//            } else {
//                recipeDetailDAO.update(detail);
//            }
//        }
        recipeDetailDAO.saveRecipeDetails(details);
        logger.info("RecipeDetailManager saveRecipeDetails details:{}", JSON.toJSONString(details));
        return details;
    }

    /**
     * 设置药品默认字段-处方药品默认数据
     *
     * @param detail           处方明细
     * @param recipeId         处方
     * @param organDrugListMap 机构药品
     * @return
     */
    private void setRecipeDetail(Recipedetail detail, Integer recipeId, Map<String, OrganDrugList> organDrugListMap,
                                 Map<Integer, UsePathways> usePathwaysMap, Map<Integer, UsingRate> usingRateMap) {
        //设置药品详情基础数据
        detail.setStatus(1);
        detail.setHisReturnSalePrice(null);
        detail.setRecipeId(recipeId);
        detail.setCreateDt(DateTime.now().toDate());
        detail.setLastModify(DateTime.now().toDate());
        detail.setUseDays(null == detail.getUseDays() ? 0 : detail.getUseDays());
        detail.setSuperScalarCode(StringUtils.isEmpty(detail.getSuperScalarCode()) ? "" : detail.getSuperScalarCode());
        detail.setSuperScalarName(StringUtils.isEmpty(detail.getSuperScalarName()) ? "" : detail.getSuperScalarName());
        //设置药品基础数据
        OrganDrugList organDrug = organDrugListMap.get(detail.getOrganDrugCode() + detail.getDrugId());
        if (null == organDrug) {
            return;
        }
        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
        detail.setDrugName(organDrug.getDrugName());
        detail.setDrugSpec(organDrug.getDrugSpec());
        detail.setDrugUnit(organDrug.getUnit());
        detail.setDefaultUseDose(organDrug.getUseDose());
        detail.setSaleName(organDrug.getSaleName());
        detail.setDosageUnit(organDrug.getUseDoseUnit());
        detail.setProducer(organDrug.getProducer());
        detail.setProducerCode(organDrug.getProducerCode());
        detail.setLicenseNumber(organDrug.getLicenseNumber());
        //设置药品包装数量
        detail.setPack(organDrug.getPack());
        detail.setUseDoseUnit(StringUtils.isEmpty(detail.getUseDoseUnit()) ? organDrug.getUseDoseUnit() : detail.getUseDoseUnit());
        //设置药品价格
        detail.setSalePrice(null == organDrug.getSalePrice() ? new BigDecimal(0) : organDrug.getSalePrice());
        //前端展示
        Map<String, String> drugUnitdoseAndUnitMap = new HashMap<>();
        String unitDoseForSpecificationUnit = null == organDrug.getUseDose() ? "" : organDrug.getUseDose().toString();
        String unitForSpecificationUnit = null == organDrug.getUseDoseUnit() ? "" : organDrug.getUseDoseUnit();
        String unitDoseForSmallUnit = null == organDrug.getSmallestUnitUseDose() ? "" : organDrug.getSmallestUnitUseDose().toString();
        String unitForSmallUnit = null == organDrug.getUseDoseSmallestUnit() ? "" : organDrug.getUseDoseSmallestUnit();
        drugUnitdoseAndUnitMap.put("unitDoseForSpecificationUnit", unitDoseForSpecificationUnit);
        drugUnitdoseAndUnitMap.put("unitForSpecificationUnit", unitForSpecificationUnit);
        drugUnitdoseAndUnitMap.put("unitDoseForSmallUnit", unitDoseForSmallUnit);
        drugUnitdoseAndUnitMap.put("unitForSmallUnit", unitForSmallUnit);
        detail.setDrugUnitdoseAndUnit(JSONUtils.toString(drugUnitdoseAndUnitMap));
        //设置药品频次途径-处方药品默认数据
        drugClient.setRecipeDetail(detail, usePathwaysMap, usingRateMap);
    }

}
