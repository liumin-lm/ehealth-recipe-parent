package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.his.recipe.mode.MedicalReimbursementTypeReqTO;
import com.ngari.his.recipe.mode.MedicalReimbursementTypeResTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.common.model.RevisitExDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugClient;
import recipe.client.DrugStockClient;
import recipe.client.IConfigurationClient;
import recipe.client.OperationClient;
import recipe.dao.PharmacyTcmDAO;
import recipe.enumerate.type.AppointEnterpriseTypeEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机构药品处理
 *
 * @author fuzi
 */
@Service
public class OrganDrugListManager extends BaseManager {
    private static final Logger logger = LoggerFactory.getLogger(OrganDrugListManager.class);
    @Autowired
    private DrugStockClient drugStockClient;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 校验机构药品库存 用于 药品展示
     *
     * @param organId               机构id
     * @param id                    药企或机构id
     * @param appointEnterpriseType 0默认，1查询医院，2查询药企
     * @param detailList            药品详情
     * @return
     */
    public EnterpriseStock organStock(Integer organId, Integer id, Integer appointEnterpriseType, List<Recipedetail> detailList) {
        if (Integer.valueOf(2).equals(appointEnterpriseType)) {
            return null;
        }
        organId = ValidateUtil.integerIsEmpty(id) ? organId : id;
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(organId);
        return this.organStock(recipe, detailList);
    }

    public String organStockDownload(Integer organId, List<Recipedetail> detailList) {
        //下载处方签
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        String supportDownloadButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
        if (StringUtils.isEmpty(supportDownloadButton) || CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        List<Integer> drugIds = detailList.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        Long notCountDownloadRecipe = organDrugListDAO.getCountDownloadRecipe(organId, drugIds);
        logger.info("OrganDrugListManager.organStockDownload notCountDownloadRecipe={}", notCountDownloadRecipe);
        if (ValidateUtil.longIsEmpty(notCountDownloadRecipe)) {
            return supportDownloadButton;
        }
        return null;
    }

    /**
     * 校验机构药品库存
     *
     * @param recipe
     * @param detailList
     * @return
     */
    public EnterpriseStock organStock(Recipe recipe, List<Recipedetail> detailList) {
        EnterpriseStock enterpriseStock = this.organ(recipe.getClinicOrgan());
        if (null == enterpriseStock) {
            return null;
        }
        //校验医院库存
        DrugStockAmountDTO scanResult = this.scanDrugStockByRecipeId(recipe, detailList);
        enterpriseStock.setDrugInfoList(scanResult.getDrugInfoList());
        enterpriseStock.setDrugName(scanResult.getNotDrugNames());
        enterpriseStock.setStock(scanResult.isResult());
        logger.info("OrganDrugListManager.organStock enterpriseStock={}", JSON.toJSONString(enterpriseStock));
        return enterpriseStock;
    }

    /**
     * 获取机构库存校验对象
     *
     * @param organId
     * @return
     */
    public EnterpriseStock organ(Integer organId) {
        // 到院取药是否采用药企管理模式
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(organId, "drugToHosByEnterprise", false);
        if (drugToHosByEnterprise) {
            logger.info("OrganDrugListManager.organ drugToHosByEnterprise={}", drugToHosByEnterprise);
            return null;
        }
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        //无到院取药
        GiveModeButtonDTO showButton = RecipeSupportGiveModeEnum.getGiveMode(giveModeButtonBeans, RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
        if (null == showButton) {
            return null;
        }
        //返回出参
        OrganDTO organDTO = organClient.organDTO(organId);
        EnterpriseStock enterpriseStock = new EnterpriseStock();
        showButton.setType(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
        enterpriseStock.setGiveModeButton(Collections.singletonList(showButton));
        enterpriseStock.setDeliveryName(organDTO.getName() + "门诊药房");
        enterpriseStock.setDeliveryCode(organId.toString());
        enterpriseStock.setAppointEnterpriseType(AppointEnterpriseTypeEnum.ORGAN_APPOINT.getType());
        logger.info("OrganDrugListManager organ enterpriseStock={}", JSON.toJSONString(enterpriseStock));
        return enterpriseStock;
    }


    /**
     * 查询机构药品库存 用于机构展示
     *
     * @param recipe
     * @param detailList
     * @return 机构药品库存结果
     */
    public DrugStockAmountDTO scanDrugStockByRecipeId(Recipe recipe, List<Recipedetail> detailList) {
        logger.info("OrganDrugListManager scanDrugStockByRecipeId recipe={}  recipeDetails = {}", JSONArray.toJSONString(recipe), JSONArray.toJSONString(detailList));
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        drugStockAmountDTO.setResult(true);
        if (null != recipe.getTakeMedicine() && 1 == recipe.getTakeMedicine()) {
            //外带药处方则不进行校验
            drugStockAmountDTO.setDrugInfoList(DrugStockClient.getDrugInfoDTO(detailList, true));
            return drugStockAmountDTO;
        }
        if (CollectionUtils.isEmpty(detailList)) {
            drugStockAmountDTO.setResult(false);
            drugStockAmountDTO.setDrugInfoList(DrugStockClient.getDrugInfoDTO(detailList, false));
            return drugStockAmountDTO;
        }
        // 判断his 是否启用
        if (!configurationClient.isHisEnable(recipe.getClinicOrgan())) {
            drugStockAmountDTO.setResult(false);
            drugStockAmountDTO.setDrugInfoList(DrugStockClient.getDrugInfoDTO(detailList, false));
            logger.info("OrganDrugListManager scanDrugStockByRecipeId 医院HIS未启用 organId: {}", recipe.getClinicOrgan());
            return drugStockAmountDTO;
        }
        Set<Integer> pharmaIds = new HashSet<>();
        List<Integer> drugIdList = detailList.stream().map(a -> {
            pharmaIds.add(a.getPharmacyId());
            return a.getDrugId();
        }).collect(Collectors.toList());

        // 请求his
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        if (CollectionUtils.isEmpty(organDrugList)) {
            drugStockAmountDTO.setResult(false);
            drugStockAmountDTO.setDrugInfoList(DrugStockClient.getDrugInfoDTO(detailList, false));
            return drugStockAmountDTO;
        }
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
        return drugStockClient.scanDrugStock(detailList, recipe.getClinicOrgan(), organDrugList, pharmacyTcmByIds);
    }

    /**
     * 根据code获取机构药品
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品code
     * @return 机构code = key对象
     */
    public List<OrganDrugList> findOrganDrugCode(int organId, List<String> organDrugCode) {
        if (CollectionUtils.isEmpty(organDrugCode)) {
            return Collections.emptyList();
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCode);
        logger.info("OrganDrugListManager getOrganDrugCode organDrugList= {}", JSON.toJSONString(organDrugList));
        return organDrugList;
    }

    /**
     * 根据code获取机构药品 分组
     *
     * @param organId      机构id
     * @param drugCodeList 机构药品code
     * @return 机构code = key对象
     */
    public Map<String, List<OrganDrugList>> getOrganDrugCode(int organId, List<String> drugCodeList) {
        if (CollectionUtils.isEmpty(drugCodeList)) {
            return new HashMap<>();
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, drugCodeList);
        logger.info("OrganDrugListManager getOrganDrugCode organDrugList= {}", JSON.toJSONString(organDrugList));
        return Optional.ofNullable(organDrugList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
    }


    /**
     * 根据 drugId 查询药品，用drugId+organDrugCode为key
     *
     * @param organId 机构id
     * @param drugIds 药品id
     * @return drugId+organDrugCode为key返回药品Map
     */
    public Map<String, OrganDrugList> getOrganDrugByIdAndCode(int organId, List<Integer> drugIds) {
        if (CollectionUtils.isEmpty(drugIds)) {
            return new HashMap<>();
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIds);
        logger.info("OrganDrugListManager getOrganDrugByIdAndCode organDrugList= {}", JSON.toJSONString(organDrugList));
        return Optional.ofNullable(organDrugList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
    }

    /**
     * 替换RecipeDetail中的机构药品编号
     *
     * @param organId          机构id
     * @param recipeDetailList 处方明细
     */
    public void setDrugItemCode(int organId, List<Recipedetail> recipeDetailList) {
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            return;
        }
        List<Integer> drugIds = recipeDetailList.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        Map<String, OrganDrugList> map = this.getOrganDrugByIdAndCode(organId, drugIds);
        recipeDetailList.forEach(a -> {
            OrganDrugList organDrug = map.get(a.getDrugId() + a.getOrganDrugCode());
            if (null != organDrug) {
                a.setDrugItemCode(organDrug.getDrugItemCode());
                if(organDrug.getUnitHisCode() != null){
                    a.setUnitHisCode(organDrug.getUnitHisCode());
                }
                if(organDrug.getUseDoseUnitHisCode() != null){
                    a.setUseDoseUnitHisCode(organDrug.getUseDoseUnitHisCode());
                }
                if(organDrug.getUseDoseSmallestUnitHisCode() != null){
                    a.setUseDoseSmallestUnitHisCode(organDrug.getUseDoseSmallestUnitHisCode());
                }
            }
        });
    }

    /**
     * 校验比对药品
     *
     * @param validateOrganDrugDTO 校验机构药品对象
     * @param organDrugGroup       机构药品组
     * @return 返回机构药品
     */
    public OrganDrugList validateOrganDrug(ValidateOrganDrugDTO validateOrganDrugDTO, Map<String, List<OrganDrugList>> organDrugGroup) {
        validateOrganDrugDTO.setValidateStatus(true);
        //校验药品存在
        if (StringUtils.isEmpty(validateOrganDrugDTO.getOrganDrugCode())) {
            validateOrganDrugDTO.setValidateStatus(false);
            return null;
        }
        List<OrganDrugList> organDrugs = organDrugGroup.get(validateOrganDrugDTO.getOrganDrugCode());
        if (CollectionUtils.isEmpty(organDrugs)) {
            validateOrganDrugDTO.setValidateStatus(false);
            return null;
        }
        //校验比对药品
        OrganDrugList organDrug = null;
        if (ValidateUtil.integerIsEmpty(validateOrganDrugDTO.getDrugId()) && 1 == organDrugs.size()) {
            organDrug = organDrugs.get(0);
        }
        if (!ValidateUtil.integerIsEmpty(validateOrganDrugDTO.getDrugId())) {
            for (OrganDrugList drug : organDrugs) {
                if (drug.getDrugId().equals(validateOrganDrugDTO.getDrugId())) {
                    organDrug = drug;
                    break;
                }
            }
        }
        if (null == organDrug) {
            validateOrganDrugDTO.setValidateStatus(false);
            return null;
        }
        if (Integer.valueOf(1).equals(organDrug.getUnavailable())) {
            validateOrganDrugDTO.setValidateStatus(false);
        }
        return organDrug;
    }

    /***
     * 比对获取药品单位
     * @param useDoseUnit 药品单位
     * @param organDrug 机构药品
     * @return 药品单位
     */
    public static String getUseDoseUnit(String useDoseUnit, OrganDrugList organDrug) {
        if (StringUtils.isEmpty(useDoseUnit)) {
            return null;
        }
        String unit = null != organDrug.getUseDoseUnit() ? organDrug.getUseDoseUnit().trim() : null;
        if (useDoseUnit.trim().equals(unit)) {
            return organDrug.getUseDoseUnit();
        }
        if (useDoseUnit.trim().equals(organDrug.getUseDoseSmallestUnit())) {
            return organDrug.getUseDoseSmallestUnit();
        }
        return null;
    }

    /***
     * 比对获取开药单位
     * @param drugUnit 开药单位
     * @param organDrug 机构药品
     * @return 药品单位
     */
    public static String getDrugUnit(String drugUnit, OrganDrugList organDrug) {
        if (StringUtils.isEmpty(drugUnit)) {
            return null;
        }
        String unit = null != organDrug.getUnit() ? organDrug.getUnit().trim() : null;
        if (drugUnit.trim().equals(unit)) {
            return organDrug.getUnit();
        }
        return null;
    }

    /**
     * 校验his 药品规则，靶向药
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        Boolean targetedDrug = consultClient.getTargetedDrugTypeRecipeRight(recipe.getDoctor());
        if (targetedDrug) {
            return;
        }
        List<Integer> drugIdList = recipeDetails.stream().map(RecipeDetailDTO::getDrugId).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        logger.info("OrganDrugListManager validateHisDrugRule organDrugList={}", JSON.toJSONString(organDrugList));
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        recipeDetails.forEach(a -> {
            //存在其他权限
            if (!ValidateUtil.integerIsEmpty(a.getValidateHisStatus())) {
                return;
            }
            //判断靶向药权限
            OrganDrugList organDrug = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == organDrug) {
                return;
            }
            if (ValidateUtil.integerIsEmpty(organDrug.getTargetedDrugType())) {
                return;
            }
            a.setValidateHisStatus(2);
            a.setValidateHisStatusText("不可开具靶向药品");
        });
    }

    /**
     * 校验his 药品规则，抗肿瘤药物
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateAntiTumorDrug(Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        List<Integer> antiTumorDrug = consultClient.getAntiTumorDrugLevel(recipe.getDoctor());
        //说明两种权限都有
        if (antiTumorDrug.containsAll(Stream.of(1,2).collect(Collectors.toList()))) {
            return;
        }
        List<Integer> drugIdList = recipeDetails.stream().map(RecipeDetailDTO::getDrugId).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        logger.info("OrganDrugListManager validateAntiTumorDrug organDrugList={}", JSON.toJSONString(organDrugList));
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        recipeDetails.forEach(a -> {
            //存在其他权限
            if (!ValidateUtil.integerIsEmpty(a.getValidateHisStatus())) {
                return;
            }
            //判断靶向药权限
            OrganDrugList organDrug = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == organDrug) {
                return;
            }
            if (ValidateUtil.integerIsEmpty(organDrug.getAntiTumorDrugFlag())) {
                return;
            }
            if(!antiTumorDrug.contains(organDrug.getAntiTumorDrugLevel())){
                a.setValidateHisStatus(3);
                a.setValidateHisStatusText("不可开具抗肿瘤药品");
            }
        });
    }

    /**
     * 校验his 药品规则，抗菌素药物
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateAntibioticsDrug(Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        List<Integer> antibioticsDrug = consultClient.getAntibioticsDrugLevelLevel(recipe.getDoctor());
        //说明三种权限都有
        if (antibioticsDrug.containsAll(Stream.of(1,2,3).collect(Collectors.toList()))) {
            return;
        }
        List<Integer> drugIdList = recipeDetails.stream().map(RecipeDetailDTO::getDrugId).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        logger.info("OrganDrugListManager validateAntibioticsDrug organDrugList={}", JSON.toJSONString(organDrugList));
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        recipeDetails.forEach(a -> {
            //存在其他权限
            if (!ValidateUtil.integerIsEmpty(a.getValidateHisStatus())) {
                return;
            }
            //判断抗菌素药物权限
            OrganDrugList organDrug = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == organDrug) {
                return;
            }
            //0表示不是抗菌素药品
            if( !new Integer(0).equals(organDrug.getAntibioticsDrugLevel()) && !antibioticsDrug.contains(organDrug.getAntibioticsDrugLevel())){
                a.setValidateHisStatus(4);
                a.setValidateHisStatusText("不可开具抗菌素药品");
            }

        });
    }

    /**
     * 校验精麻毒放和特殊使用级抗生素药物
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateOtherDrug(Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        List<Integer> drugIdList = recipeDetails.stream().map(RecipeDetailDTO::getDrugId).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        logger.info("OrganDrugListManager validateOtherDrug organDrugList={}", JSON.toJSONString(organDrugList));
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        recipeDetails.forEach(a -> {
            OrganDrugList organDrug = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == organDrug) {
                return;
            }
            //判断药物类型
            List<Integer> dataType = new ArrayList<>();
            dataType.add(organDrug.getPsychotropicDrugFlag());
            dataType.add(organDrug.getToxicDrugFlag());
            dataType.add(organDrug.getNarcoticDrugFlag());
            dataType.add(organDrug.getRadioActivityDrugFlag());
            //优先展示精麻毒放
            if(dataType.contains(1)){
                a.setValidateHisStatus(5);
                a.setValidateHisStatusText("“精”“麻”“毒”“放”类药品，禁止在互联网医院上开具");
            }
            if(!dataType.contains(1) && new Integer(1).equals(organDrug.getSpecialUseAntibioticDrugFlag())){
                a.setValidateHisStatus(6);
                a.setValidateHisStatusText("“特殊使用级抗生素”类药品，禁止在互联网医院上开具");
            }
        });
    }

    /**
     * 查询his 药品说明书
     *
     * @param organId      机构id
     * @param recipedetail 药品数据
     * @return
     */
    public DrugSpecificationInfoDTO hisDrugBook(Integer organId, Recipedetail recipedetail) {
        OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
        if (null == organDrug) {
            return null;
        }
        return offlineRecipeClient.drugSpecification(organId, organDrug);
    }

    /**
     * 获取药房相关药品
     *
     * @param organId
     * @param organDrugCodeList
     * @param pharmacyId
     * @return
     */
    public List<OrganDrugList> pharmacyDrug(Integer organId, List<String> organDrugCodeList, Integer pharmacyId) {
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        if (ValidateUtil.integerIsEmpty(pharmacyId)) {
            return organDrugList;
        }
        List<OrganDrugList> lists = new ArrayList<>();
        organDrugList.forEach(a -> {
            if (StringUtils.isEmpty(a.getPharmacy())) {
                return;
            }
            List<Integer> pharmacyIds = Arrays.stream(a.getPharmacy().split(ByteUtils.COMMA)).map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
            if (pharmacyIds.contains(pharmacyId)) {
                lists.add(a);
            }
        });
        logger.info("OrganDrugListManager pharmacyDrug lists={}", JSON.toJSONString(lists));
        return lists;
    }

    /**
     * 判断药品的医保报销类型
     *
     * @param hisDrugRule
     * @param recipe
     * @param recipeDetails
     * @return
     */
    public void validateMedicalReimbursementTypeOfDrugs(List<String> hisDrugRule, Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        //"6": "判断药品的医保报销类型"
        if (hisDrugRule.contains("6")) {
            logger.info("validateMedicalReimbursementTypeOfDrugs his start ");
            if(!new Integer(2).equals(recipe.getBussSource())){
                return ;
            }
            //医保属性,处方来源为复诊时从复诊获取
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if(revisitExDTO != null){
                Integer medicalInsuranceAttribute = revisitExDTO.getMedicalCardType();
            }
            List<MedicalReimbursementTypeReqTO> medicalReimbursementTypeReqTOList = new ArrayList<>();
            for (RecipeDetailDTO recipeDetail : recipeDetails) {
                MedicalReimbursementTypeReqTO medicalReimbursementTypeReqTO = new MedicalReimbursementTypeReqTO();
                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
                if(organDrugList != null){
                    medicalReimbursementTypeReqTO.setProducerCode(organDrugList.getProducerCode());
                }
                medicalReimbursementTypeReqTO.setMedicalInsuranceAttribute(medicalInsuranceAttribute);
                medicalReimbursementTypeReqTO.setOrganDrugCode(recipeDetail.getOrganDrugCode());
                medicalReimbursementTypeReqTO.setOrganId(recipe.getClinicOrgan());
                medicalReimbursementTypeReqTOList.add(medicalReimbursementTypeReqTO);
            }
            logger.info("OrganDrugListManager validateMedicalReimbursementTypeOfDrugs medicalReimbursementTypeReqTOList={}", JSON.toJSONString(medicalReimbursementTypeReqTOList));
            List<MedicalReimbursementTypeResTO> medicalReimbursementTypeResTOList = drugClient.validateMedicalReimbursementTypeOfDrugs(medicalReimbursementTypeReqTOList);
            Map<String, MedicalReimbursementTypeResTO> organDrugMap = medicalReimbursementTypeResTOList.stream().collect(Collectors.toMap(MedicalReimbursementTypeResTO::getOrganDrugCode, a -> a, (k1, k2) -> k1));
            for (RecipeDetailDTO recipeDetail : recipeDetails) {
                MedicalReimbursementTypeResTO medicalReimbursementTypeResTO = organDrugMap.get(recipeDetail.getOrganDrugCode());
                if(medicalReimbursementTypeResTO != null){
                    recipeDetail.setMedicalInsuranceCategory(medicalReimbursementTypeResTO.getMedicalInsuranceCategory());
                }
            }
        }
        else {
            logger.info("validateMedicalReimbursementTypeOfDrugs organDrugList start ");
            for (RecipeDetailDTO recipeDetail : recipeDetails) {
                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
                if(organDrugList != null){
                    recipeDetail.setMedicalInsuranceCategory(organDrugList.getMedicalInsuranceCategory());
                }
            }
        }
    }
}
