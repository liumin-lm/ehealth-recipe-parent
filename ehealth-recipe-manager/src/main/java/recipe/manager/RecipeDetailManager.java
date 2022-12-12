package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.RecipePreSettleDrugFeeDTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.UsePathwaysDTO;
import com.ngari.patient.dto.UsingRateDTO;
import com.ngari.patient.service.IUsePathwaysService;
import com.ngari.patient.service.IUsingRateService;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.enumerate.type.DrugBelongTypeEnum;
import recipe.util.JsonUtil;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        }catch (Exception e){
            logger.error("saveRecipePreSettleDrugFeeDTOS recipeIds={} error", JsonUtil.toString(recipeId), e);
        }
    }

    /**
     * 保存处方明细
     *
     * @param recipe           处方信息
     * @param details          明细信息
     * @param organDrugListMap 机构药品
     * @return
     */
    public List<Recipedetail> saveRecipeDetails(Recipe recipe, List<Recipedetail> details, Map<String, OrganDrugList> organDrugListMap) {
        logger.info("RecipeDetailManager saveRecipeDetails  recipe = {},  details = {},  organDrugListMap = {}"
                , JSON.toJSONString(recipe), JSON.toJSONString(details), JSON.toJSONString(organDrugListMap));

        recipeDetailDAO.updateDetailInvalidByRecipeId(recipe.getRecipeId());
        BigDecimal totalMoney = new BigDecimal(0);
        for (Recipedetail detail : details) {
            BigDecimal drugCost = setRecipeDetail(detail, recipe.getRecipeId(), organDrugListMap);
            totalMoney = totalMoney.add(drugCost);
            if (ValidateUtil.integerIsEmpty(detail.getRecipeDetailId())) {
                recipeDetailDAO.save(detail);
            } else {
                recipeDetailDAO.update(detail);
            }
        }
        recipe.setTotalMoney(totalMoney);
        recipe.setActualPrice(totalMoney);
        logger.info("RecipeDetailManager saveRecipeDetails details:{}", JSON.toJSONString(details));
        return details;
    }

    /**
     * 批量查询处方明细
     *
     * @param recipeIds 处方id
     * @return 处方明细
     */
    public Map<Integer, List<Recipedetail>> findRecipeDetailMap(List<Integer> recipeIds) {
        List<Recipedetail> recipeDetails = findRecipeDetails(recipeIds);
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

    /**
     * 过滤保密处方，重新设置保密处方信息
     * @param recipePdfDTO
     */
    public void filterSecrecyDrug(RecipeInfoDTO recipePdfDTO){
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
     * 写入明细字段
     *
     * @param detail           处方明细
     * @param recipeId         处方id
     * @param organDrugListMap 机构药品
     * @return
     */
    private BigDecimal setRecipeDetail(Recipedetail detail, Integer recipeId, Map<String, OrganDrugList> organDrugListMap) {
        Date nowDate = DateTime.now().toDate();
        detail.setRecipeId(recipeId);
        detail.setStatus(1);
        detail.setCreateDt(nowDate);
        detail.setLastModify(nowDate);
        if (Integer.valueOf("2").equals(detail.getType())) {
            BigDecimal price = detail.getSalePrice();
            return price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).setScale(4, BigDecimal.ROUND_HALF_UP);
        }
        OrganDrugList organDrug = organDrugListMap.get(detail.getDrugId() + detail.getOrganDrugCode());
        if (null == organDrug) {
            return new BigDecimal(0);
        }
        detail.setProducer(organDrug.getProducer());
        detail.setProducerCode(organDrug.getProducerCode());
        detail.setLicenseNumber(organDrug.getLicenseNumber());
        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
        detail.setDrugName(organDrug.getDrugName());
        detail.setDrugSpec(organDrug.getDrugSpec());
        detail.setDrugUnit(organDrug.getUnit());
        detail.setDefaultUseDose(organDrug.getUseDose());
        detail.setSaleName(organDrug.getSaleName());
        detail.setDosageUnit(organDrug.getUseDoseUnit());
        detail.setPack(organDrug.getPack());
        detail.setSalePrice(organDrug.getSalePrice());
        BigDecimal price = organDrug.getSalePrice();
        BigDecimal drugCost = price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).setScale(4, BigDecimal.ROUND_HALF_UP);
        detail.setDrugCost(drugCost);
        return drugCost;
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
        BigDecimal totalMoney = new BigDecimal(0d);
        if (CollectionUtils.isEmpty(detailList)) {
            return totalMoney;
        }
        for (Recipedetail detail : detailList) {
            BigDecimal price = detail.getSalePrice();
            BigDecimal drugCost;
            if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType)) {
                detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                //保留3位小数
                drugCost = price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).divide(BigDecimal.valueOf(detail.getPack()), 4, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
            } else {
                //保留3位小数
                drugCost = price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).setScale(4, RoundingMode.HALF_UP);
            }
            detail.setDrugCost(drugCost);
            totalMoney = totalMoney.add(drugCost);
        }
        return totalMoney;
    }

    /**
     * 保存处方药品信息
     *
     * @param recipeDetails 药品信息
     * @param recipe        处方信息
     */
    public void saveStagingRecipeDetail(List<Recipedetail> recipeDetails, Recipe recipe) {
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return;
        }
        int organId = recipe.getClinicOrgan();
        //药品总金额
        BigDecimal totalMoney = new BigDecimal(0);
        List<Integer> drugIds = new ArrayList<>(recipedetails.size());
        List<String> organDrugCodes = new ArrayList<>(recipedetails.size());
        Date nowDate = DateTime.now().toDate();
        String recipeMode = recipe.getRecipeMode();

        for (Recipedetail detail : recipedetails) {
            //设置药品详情基础数据
            detail.setStatus(1);
            detail.setHisReturnSalePrice(null);
            detail.setRecipeId(recipe.getRecipeId());
            detail.setCreateDt(nowDate);
            detail.setLastModify(nowDate);
            if (StringUtils.isEmpty(detail.getSuperScalarCode())) {
                detail.setSuperScalarCode("");
            }
            if (StringUtils.isEmpty(detail.getSuperScalarName())) {
                detail.setSuperScalarName("");
            }
            if (null != detail.getDrugId()) {
                drugIds.add(detail.getDrugId());
            }
            if (StringUtils.isNotEmpty(detail.getOrganDrugCode())) {
                organDrugCodes.add(detail.getOrganDrugCode());
            }
        }
        if (CollectionUtils.isNotEmpty(drugIds)) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            //判断平台基础库药品是否删除
            List<DrugList> drugLists = drugListDAO.findByDrugIdsforDel(drugIds);
            if (CollectionUtils.isNotEmpty(drugLists)) {
                List<String> delDrugName = Lists.newArrayList();
                for (DrugList drugList : drugLists) {
                    delDrugName.add(drugList.getDrugName());
                }
            }
            //是否为老的药品兼容方式，老的药品传入方式没有organDrugCode
            boolean oldFlag = organDrugCodes.isEmpty();
            Map<String, OrganDrugList> organDrugListMap = Maps.newHashMap();
            Map<Integer, OrganDrugList> organDrugListIdMap = Maps.newHashMap();
            List<OrganDrugList> organDrugList = Lists.newArrayList();
            if (oldFlag) {
                organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIds);
            } else {
                organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodes);
            }

            if (CollectionUtils.isNotEmpty(organDrugList)) {
                if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
                    //药品有混非外带药和外带药的不能一起开
                    int takeMedicineSize = 0;
                    List<String> takeOutDrugName = Lists.newArrayList();
                    for (OrganDrugList obj : organDrugList) {
                        //检验是否都为外带药
                        if (Integer.valueOf(1).equals(obj.getTakeMedicine())) {
                            takeMedicineSize++;
                            takeOutDrugName.add(obj.getSaleName());
                        }
                        organDrugListMap.put(obj.getOrganDrugCode() + obj.getDrugId(), obj);
                        organDrugListIdMap.put(obj.getDrugId(), obj);
                    }

                    if (takeMedicineSize > 0) {
                        if (takeMedicineSize != organDrugList.size()) {
                            String errorDrugName = Joiner.on(",").join(takeOutDrugName);
                            //外带药和处方药混合开具是不允许的
                            LOGGER.warn("setDetailsInfo 存在外带药且混合开具. recipeId=[{}], drugIds={}, 外带药={}", recipe.getRecipeId(), JSONUtils.toString(drugIds), errorDrugName);
                            throw new DAOException(ErrorCode.SERVICE_ERROR, errorDrugName + "为外带药,不能与其他药品开在同一张处方单上");
                        } else {
                            //外带处方， 同时也设置成只能配送处方
                            recipe.setTakeMedicine(1);
                            recipe.setDistributionFlag(1);
                        }
                    }
                    //判断某诊断下某药品能否开具
                    if (recipe != null && recipe.getOrganDiseaseName() != null) {
                        canOpenRecipeDrugsAndDisease(recipe, drugIds);
                    }
                } else {
                    for (OrganDrugList obj : organDrugList) {
                        organDrugListMap.put(obj.getOrganDrugCode() + obj.getDrugId(), obj);
                        organDrugListIdMap.put(obj.getDrugId(), obj);
                    }
                }


                OrganDrugList organDrug;
                List<String> delOrganDrugName = Lists.newArrayList();
                PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                com.ngari.patient.service.IUsingRateService usingRateService = AppDomainContext.getBean("basic.usingRateService", IUsingRateService.class);
                com.ngari.patient.service.IUsePathwaysService usePathwaysService = AppDomainContext.getBean("basic.usePathwaysService", IUsePathwaysService.class);
                for (Recipedetail detail : recipedetails) {
                    //设置药品基础数据
                    if (oldFlag) {
                        organDrug = organDrugListIdMap.get(detail.getDrugId());
                    } else {
                        organDrug = organDrugListMap.get(detail.getOrganDrugCode() + detail.getDrugId());
                    }
                    if (null != organDrug) {
                        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
                        detail.setDrugName(organDrug.getDrugName());
                        detail.setDrugSpec(organDrug.getDrugSpec());
                        detail.setDrugUnit(organDrug.getUnit());
                        detail.setDefaultUseDose(organDrug.getUseDose());
                        detail.setSaleName(organDrug.getSaleName());
                        //如果前端传了剂量单位优先用医生选择的剂量单位
                        //医生端剂量单位可以选择规格单位还是最小单位
                        if (StringUtils.isNotEmpty(detail.getUseDoseUnit())) {
                            detail.setUseDoseUnit(detail.getUseDoseUnit());
                        } else {
                            detail.setUseDoseUnit(organDrug.getUseDoseUnit());
                        }
                        detail.setDosageUnit(organDrug.getUseDoseUnit());
                        //设置药品包装数量
                        detail.setPack(organDrug.getPack());
                        //频次处理
                        if (StringUtils.isNotEmpty(detail.getUsingRateId())) {
                            UsingRateDTO usingRateDTO = usingRateService.getById(Integer.valueOf(detail.getUsingRateId()));
                            if (usingRateDTO != null) {
                                detail.setUsingRateTextFromHis(usingRateDTO.getText());
                                detail.setOrganUsingRate(usingRateDTO.getUsingRateKey());
                                if (usingRateDTO.getRelatedPlatformKey() != null) {
                                    detail.setUsingRate(usingRateDTO.getRelatedPlatformKey());
                                } else {
                                    detail.setUsingRate(usingRateDTO.getUsingRateKey());
                                }

                            }
                        }
                        //用法处理
                        if (StringUtils.isNotEmpty(detail.getUsePathwaysId())) {
                            UsePathwaysDTO usePathwaysDTO = usePathwaysService.getById(Integer.valueOf(detail.getUsePathwaysId()));
                            if (usePathwaysDTO != null) {
                                detail.setUsePathwaysTextFromHis(usePathwaysDTO.getText());
                                detail.setOrganUsePathways(usePathwaysDTO.getPathwaysKey());
                                if (usePathwaysDTO.getRelatedPlatformKey() != null) {
                                    detail.setUsePathways(usePathwaysDTO.getRelatedPlatformKey());
                                } else {
                                    detail.setUsePathways(usePathwaysDTO.getPathwaysKey());
                                }
                            }
                        }
                        //中药基础数据处理
                        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                            if (StringUtils.isBlank(detail.getUsePathways())) {
                                detail.setUsePathways(recipe.getTcmUsePathways());
                            }
                            if (StringUtils.isBlank(detail.getUsingRate())) {
                                detail.setUsingRate(recipe.getTcmUsingRate());
                            }
                            if (detail.getUseDose() != null) {
                                detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                            }
                        } else if (RecipeBussConstant.RECIPETYPE_HP.equals(recipe.getRecipeType())) {
                            if (detail.getUseDose() != null) {
                                detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                            }
                        }
                        //添加机构药品信息
                        //date 20200225
                        detail.setProducer(organDrug.getProducer());
                        detail.setProducerCode(organDrug.getProducerCode());
                        detail.setLicenseNumber(organDrug.getLicenseNumber());

                        //设置药品价格
                        BigDecimal price = organDrug.getSalePrice();
                        if (null == price) {
                            LOGGER.warn("setDetailsInfo 药品ID：" + organDrug.getDrugId() + " 在医院(ID为" + organId + ")的价格为NULL！");
                            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品数据异常！");
                        }
                        detail.setSalePrice(price);
                        BigDecimal drugCost;
                        LOGGER.info("detail.setUseTotalDose detail.getUseDose:{}", JSON.toJSONString(detail));
                        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                            //保留3位小数
                            drugCost = price.multiply(new BigDecimal(detail.getUseTotalDose())).divide(BigDecimal.valueOf(organDrug.getPack()), 4, BigDecimal.ROUND_HALF_UP).setScale(4, BigDecimal.ROUND_HALF_UP);
                        } else {
                            //保留3位小数
                            drugCost = price.multiply(new BigDecimal(detail.getUseTotalDose())).setScale(4, BigDecimal.ROUND_HALF_UP);
                        }
                        LOGGER.info("计算金额 price：{},drugCost:{},detail.getUseTotalDose():{},organDrug.getPack():{}", price, drugCost, detail.getUseTotalDose(), organDrug.getPack());
                        detail.setDrugCost(drugCost);
                        totalMoney = totalMoney.add(drugCost);
                        //药房处理
                        if (detail.getPharmacyId() != null && StringUtils.isEmpty(detail.getPharmacyName())) {
                            PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(detail.getPharmacyId());
                            if (pharmacyTcm != null) {
                                detail.setPharmacyName(pharmacyTcm.getPharmacyName());
                            }
                        }
                    } else {
                        if (StringUtils.isNotEmpty(detail.getDrugName())) {
                            delOrganDrugName.add(detail.getDrugName());
                        }
                    }
                    if (null == detail.getUseDays()) {
                        detail.setUseDays(0);
                    }
                    //date 202000601
                    //设置处方用药天数字符类型
                    if (StringUtils.isEmpty(detail.getUseDaysB())) {
                        detail.setUseDaysB(detail.getUseDays().toString());
                    }
                }
            }
        } else {
            LOGGER.warn("setDetailsInfo 详情里没有药品ID. recipeId=[{}]", recipe.getRecipeId());
        }
        LOGGER.warn("计算金额 totalMoney：{}", totalMoney);
        totalMoney = totalMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
        LOGGER.warn("计算金额 Up totalMoney：{}", totalMoney);
        recipe.setTotalMoney(totalMoney);
        recipe.setActualPrice(totalMoney);
        return true;

    }
}
