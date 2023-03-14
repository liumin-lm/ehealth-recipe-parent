package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.miscellany.mode.HospitalDrugInformationResponseTO;
import com.ngari.his.miscellany.mode.HospitalInformationRequestTO;
import com.ngari.his.miscellany.service.IWnHospitalInformationService;
import com.ngari.his.recipe.mode.MedicationInfoReqTO;
import com.ngari.his.recipe.mode.MedicationInfoResTO;
import com.ngari.patient.service.IUsingRateService;
import com.ngari.platform.recipe.mode.HospitalDrugListDTO;
import com.ngari.platform.recipe.mode.HospitalDrugListReqDTO;
import com.ngari.platform.recipe.mode.OrganDrugListBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.*;
import ctd.spring.AppDomainContext;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import es.api.DrugSearchService;
import es.vo.DrugVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.aop.LogRecord;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 药品数据 交互处理类
 *
 * @author fuzi
 */
@Service
public class DrugClient extends BaseClient {
    @Autowired
    private IUsingRateService usingRateService;
    @Autowired
    private IUsePathwaysService usePathwaysService;
    @Autowired
    private DrugSearchService drugSearchService;
    @Autowired
    private IWnHospitalInformationService hospitalInformationService;


    /**
     * 患者端搜索药品
     *
     * @param saleName 搜索关键字
     * @param organId  机构id
     * @param drugType 药品类型
     * @param start    起始条数
     * @param limit    条数
     * @return
     */
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(String saleName, String organId, List<String> drugType, int start, int limit) {
        logger.info("DrugClient findDrugWithEsByPatient saleName : {} organId:{} drugType:{} start:{}  limit:{}", saleName, organId, JSON.toJSONString(drugType), start, limit);
        if (Objects.isNull(organId)) {
            return null;
        }
        List<String> drugStrings = drugSearchService.searchOrganDrugForPatient(saleName, organId, drugType, start, limit);
        logger.info("findDrugWithEsByPatient drugStrings={}", JSONArray.toJSONString(drugStrings));
        if (CollectionUtils.isEmpty(drugStrings)) {
            return null;
        }
        List<PatientDrugWithEsDTO> patientDrugWithEsDTOS = drugStrings.stream().map(drugString -> {
            PatientDrugWithEsDTO patientDrugWithEsDTO = JSONArray.parseObject(drugString, PatientDrugWithEsDTO.class);
            return patientDrugWithEsDTO;
        }).collect(Collectors.toList());

        return patientDrugWithEsDTOS;
    }

    /**
     * 查询es 药品数据
     *
     * @param drugInfo          查询信息
     * @param isMergeRecipeType 是否中西药合开
     * @param start             页数
     * @param limit             条数
     * @return
     */
    public List<String> searchOrganDrugEs(DrugInfoDTO drugInfo, boolean isMergeRecipeType, int start, int limit) {
        List<String> drugTypes;
        if (isMergeRecipeType && !RecipeUtil.isTcmType(drugInfo.getDrugType())) {
            drugTypes = Arrays.asList(RecipeTypeEnum.RECIPETYPE_WM.getType().toString(), RecipeTypeEnum.RECIPETYPE_CPM.getType().toString());
        } else {
            drugTypes = Collections.singletonList(drugInfo.getDrugType().toString());
        }
        DrugVO drugVO = new DrugVO();
        drugVO.setDrugType(drugTypes);
        drugVO.setOrganId(drugInfo.getOrganId());
        drugVO.setSaleName(drugInfo.getDrugName());
        drugVO.setPharmacyId(drugInfo.getPharmacyId());
        drugVO.setApplyBusiness(drugInfo.getApplyBusiness());
        drugVO.setDrugForm(drugInfo.getDrugForm());
        drugVO.setStart(start);
        drugVO.setLimit(limit);
        logger.info("DrugClient searchOrganDrugEs drugVO={}", JSON.toJSONString(drugVO));
        List<String> drugStrings = drugSearchService.searchOrganDrugEs(drugVO);
        logger.info("DrugClient searchOrganDrugEs drugStrings={}", JSON.toJSONString(drugStrings));
        return drugStrings;
    }

    /**
     * 获取机构 药物使用频率
     *
     * @param organId        机构id
     * @param organUsingRate 机构药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String organUsingRate) {
        if (null == organId || StringUtils.isEmpty(organUsingRate)) {
            return null;
        }
        try {
            com.ngari.patient.dto.UsingRateDTO usingRateDTO = usingRateService.newFindUsingRateDTOByOrganAndKey(organId, organUsingRate);
            if (null == usingRateDTO) {
                return null;
            }

            return ObjectCopyUtils.convert(usingRateDTO, UsingRateDTO.class);
        } catch (Exception e) {
            logger.warn("DrugClient usingRate usingRateDTO error", e);
            return null;
        }
    }

    /**
     * TODO 频率频次引用的包有的是basic有的是base,后期basic迁移完统一处理
     * 获取机构 药物使用途径
     *
     * @param organId          机构id
     * @param organUsePathways 机构药物使用途径代码
     * @param drugType 用药途径处方类型  字典eh.base.dictionary.PrescriptionCategory   1西药方 2成药方 3中药方 4膏方
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String organUsePathways, Integer drugType) {
        if (null == organId || StringUtils.isEmpty(organUsePathways)) {
            return null;
        }
        try {
            com.ngari.patient.service.IUsePathwaysService usePathwaysService = AppDomainContext.getBean("basic.usePathwaysService", com.ngari.patient.service.IUsePathwaysService.class);
            com.ngari.patient.dto.UsePathwaysDTO usePathwaysDTO;
            if (ValidateUtil.integerIsEmpty(drugType)) {
                usePathwaysDTO = usePathwaysService.newFindUsePathwaysByOrganAndKey(organId, organUsePathways);
            } else {
                usePathwaysDTO = usePathwaysService.newGetUsePathwaysByOrganAndKeyAndCategory(organId, organUsePathways, drugType.toString());
            }
            if (null == usePathwaysDTO) {
                return null;
            }
            return ObjectCopyUtils.convert(usePathwaysDTO, UsePathwaysDTO.class);
        } catch (Exception e) {
            logger.warn("DrugClient usePathways usePathwaysDTO error", e);
            return null;
        }
    }

    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 id = key对象
     */
    public Map<Integer, UsingRate> usingRateMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>(1);
        }
        List<com.ngari.patient.dto.UsingRateDTO> UsingRateDTO = usingRateService.findAllusingRateByOrganId(organId);
        List<UsingRate> usingRates = ObjectCopyUtils.convert(UsingRateDTO, UsingRate.class);
        logger.info("DrugClient usingRateMap organId = {} usingRates:{}", organId, JSON.toJSONString(usingRates));
        return Optional.ofNullable(usingRates).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsingRate::getId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 机构code = key对象
     */
    public Map<String, UsingRate> usingRateMapCode(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<com.ngari.patient.dto.UsingRateDTO> UsingRateDTO = usingRateService.findAllusingRateByOrganId(organId);
        List<UsingRate> usingRates = ObjectCopyUtils.convert(UsingRateDTO, UsingRate.class);
        logger.info("DrugClient usingRateMapCode organId = {} usingRates:{}", organId, JSON.toJSONString(usingRates));
        return Optional.ofNullable(usingRates).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsingRate::getUsingRateKey, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 id = key对象
     */
    public Map<Integer, UsePathways> usePathwaysMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>(1);
        }
        List<UsePathways> usePathways = usePathwaysService.findAllUsePathwaysByOrganId(organId);
        logger.info("DrugClient usePathwaysMap organId = {} usePathways:{}", organId, JSON.toJSONString(usePathways));
        return Optional.ofNullable(usePathways).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsePathways::getId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 机构code = key对象
     */
    public Map<String, UsePathways> usePathwaysCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<UsePathways> usePathways = usePathwaysService.findAllUsePathwaysByOrganId(organId);
        logger.info("DrugClient usePathwaysCodeMap organId = {} usePathways:{}", organId, JSON.toJSONString(usePathways));
        return Optional.ofNullable(usePathways).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsePathways::getPathwaysKey, a -> a, (k1, k2) -> k1));
    }

    /**
     * 查询医院药品信息
     * @param hospitalDrugListReqDTO
     * @return
     */
    public List<HospitalDrugListDTO> findHospitalDrugList(HospitalDrugListReqDTO hospitalDrugListReqDTO) {
        try {
            HisResponseTO<List<HospitalDrugListDTO>> hisResponse = recipeHisService.findHospitalDrugList(hospitalDrugListReqDTO);
            return getResponse(hisResponse);
        } catch (Exception e) {
            logger.error("DrugClient findHospitalDrugList hisResponse", e);
        }
        return new ArrayList<>();
    }


    /**
     * 获取煎法 codeMap对象
     *
     * @param decoctionCode key
     * @param codeMap       map字典
     * @return
     */
    public static DecoctionWay validateDecoction(String decoctionCode, Map<String, DecoctionWay> codeMap) {
        DecoctionWay decoctionWay = new DecoctionWay();
        if (StringUtils.isEmpty(decoctionCode)) {
            return decoctionWay;
        }
        if (null == codeMap) {
            return decoctionWay;
        }
        DecoctionWay code = codeMap.get(decoctionCode);
        if (null == code) {
            return decoctionWay;
        }
        return code;
    }

    /**
     * 获取制法 codeMap对象
     *
     * @param makeMethod key
     * @param codeMap    map字典
     * @return
     */
    public static DrugMakingMethod validateMakeMethod(String makeMethod, Map<String, DrugMakingMethod> codeMap) {
        DrugMakingMethod drugMakingMethod = new DrugMakingMethod();
        if (StringUtils.isEmpty(makeMethod)) {
            return drugMakingMethod;
        }
        if (null == codeMap) {
            return drugMakingMethod;
        }
        DrugMakingMethod code = codeMap.get(makeMethod);
        if (null == code) {
            return drugMakingMethod;
        }
        return code;
    }

    public List<MedicationInfoResTO> medicationInfoSyncTask(Integer organId,Integer dataType) {
        try {
            MedicationInfoReqTO medicationInfoReqTO = new MedicationInfoReqTO();
            medicationInfoReqTO.setOrganId(organId);
            medicationInfoReqTO.setDataType(dataType);
            HisResponseTO<List<MedicationInfoResTO>> hisResponseTO = recipeHisService.queryMedicationInfo(medicationInfoReqTO);
            return getResponse(hisResponseTO);
        }catch (Exception e){
            logger.error("DrugClient medicationInfoSyncTask hisResponse", e);
        }
        return new ArrayList<>();
    }

    public com.ngari.patient.dto.UsingRateDTO getUsingRateById(Integer usingRateId) {
        return usingRateService.getById(usingRateId);
    }

    public com.ngari.patient.dto.UsePathwaysDTO getUsePathwaysById(int usePathwaysId) {
        return ObjectCopyUtils.convert(usePathwaysService.getById(usePathwaysId), com.ngari.patient.dto.UsePathwaysDTO.class);
    }


    /**
     * 设置处方默认数据-金额计算
     *
     * @param recipe 处方头对象
     */
    public Recipe updateRecipe(Recipe recipe, List<Recipedetail> recipeDetails, List<OrganDrugList> organDrugList) {
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        //外带处方， 同时也设置成只能配送处方
        boolean takeMedicine = organDrugList.stream().allMatch(a -> Integer.valueOf(1).equals(a.getTakeMedicine()));
        if (takeMedicine) {
            recipeUpdate.setTakeMedicine(1);
            recipeUpdate.setDistributionFlag(1);
        }
        //药品总金额
        BigDecimal totalMoney = this.totalMoney(recipe.getRecipeType(), recipeDetails, recipe);
        recipeUpdate.setTotalMoney(totalMoney);
        recipeUpdate.setActualPrice(totalMoney);
        return recipeUpdate;
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
        if (org.apache.commons.collections.CollectionUtils.isEmpty(detailList)) {
            return totalMoney;
        }
        for (Recipedetail detail : detailList) {
            BigDecimal price = detail.getSalePrice();
            BigDecimal drugCost;
            if (RecipeUtil.isTcmType(recipeType)) {
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
        return totalMoney.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 设置处方药品默认数据
     *
     * @param detail
     * @param usePathwaysMap
     * @param usingRateMap
     */
    public void setRecipeDetail(Recipedetail detail, Map<Integer, UsePathways> usePathwaysMap, Map<Integer, UsingRate> usingRateMap) {
        //频次处理
        if (StringUtils.isNotEmpty(detail.getUsingRateId()) && !usingRateMap.isEmpty()) {
            UsingRate usingRate = usingRateMap.get(Integer.valueOf(detail.getUsingRateId()));
            if (usingRate != null) {
                detail.setUsingRateTextFromHis(usingRate.getText());
                detail.setOrganUsingRate(usingRate.getUsingRateKey());
                detail.setUsingRate(usingRate.getRelatedPlatformKey());
            }
        }
        //用法处理
        if (StringUtils.isNotEmpty(detail.getUsePathwaysId()) && !usePathwaysMap.isEmpty()) {
            UsePathways usePathways = usePathwaysMap.get(Integer.valueOf(detail.getUsePathwaysId()));
            if (usePathways != null) {
                detail.setUsePathwaysTextFromHis(usePathways.getText());
                detail.setOrganUsePathways(usePathways.getPathwaysKey());
                detail.setUsePathways(usePathways.getRelatedPlatformKey());
            }
        }
    }

    /**
     * 查询his药品信息
     * @param request
     * @return
     */
    @LogRecord
    public List<OrganDrugListBean> findHisDrugList(HospitalInformationRequestTO request) {
        try {
            HisResponseTO<List<HospitalDrugInformationResponseTO>> response = hospitalInformationService.getHospitalDrugInformation(request);
            List<HospitalDrugInformationResponseTO> drugInformationResponseList = getResponse(response);
            List<OrganDrugListBean> organDrugListBeans = new ArrayList<>(drugInformationResponseList.size());
            drugInformationResponseList.forEach(drugInformation -> {
                OrganDrugListBean organDrugListBean = new OrganDrugListBean();
                organDrugListBean.setDrugName(drugInformation.getYpmc());
                organDrugListBean.setDrugSpec(drugInformation.getYpgg());
                organDrugListBean.setUnit(drugInformation.getJldw());
                organDrugListBean.setOrganDrugCode(drugInformation.getYpdm());
                organDrugListBean.setPharmacyName(drugInformation.getYfmc());
                organDrugListBean.setProducer(drugInformation.getCjmc());
                organDrugListBean.setMedicalDrugCode(drugInformation.getYbdm());
                organDrugListBeans.add(organDrugListBean);
            });
        } catch (Exception e) {
            logger.error("DrugClient findHisDrugList error", e);
        }
        return new ArrayList<>();
    }

}
