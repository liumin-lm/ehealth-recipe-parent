package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.ListOrganDrugReq;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import es.api.DrugSearchService;
import es.vo.DoctorDrugDetailVO;
import es.vo.PatientDrugDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.util.LocalStringUtil;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 药品数据处理类
 *
 * @author fuzi
 */
@Service
public class DrugManager extends BaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugManager.class);
    @Autowired
    private DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private DrugEntrustDAO drugEntrustDAO;
    @Autowired
    private DrugListDAO drugListDAO;
    @Autowired
    private DispensatoryDAO dispensatoryDAO;
    @Autowired
    private RecipeRulesDrugCorrelationDAO recipeRulesDrugCorrelationDAO;
    @Autowired
    private DrugCommonDAO drugCommonDAO;
    @Autowired
    private DrugSearchService searchService;
    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;

    /**
     * 更新drugList 到Es
     *
     * @param drugLists
     */
    @LogRecord
    public void updateDrugListToEs(List<DrugList> drugLists, int deleteFlag) {
        List<PatientDrugDetailVO> updateList = new ArrayList<>();
        for (DrugList drugList : drugLists) {
            PatientDrugDetailVO detailVO = new PatientDrugDetailVO();
            BeanUtils.copyProperties(drugList, detailVO);
            detailVO.setDeleteFlag(deleteFlag);
            detailVO.setDrugId(drugList.getDrugId());
            updateList.add(detailVO);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            searchService.updatePatientDrugDetail(updateList);
        }
    }

    /**
     * 更新 organDrugList 到es
     *
     * @param organDrugLists
     * @param deleteFlag
     */
    @LogRecord
    public void updateOrganDrugListToEs(List<OrganDrugList> organDrugLists, int deleteFlag, List<DrugList> drugList) {
        if (CollectionUtils.isEmpty(organDrugLists)) {
            LOGGER.warn("updateOrganDrugListToEs OrganDrugList is empty. ");
            return;
        }
        List<DoctorDrugDetailVO> updateList = new ArrayList<>(organDrugLists.size());
        List<Integer> drugIds = organDrugLists.stream().map(OrganDrugList::getDrugId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugList)) {
            drugList = drugListDAO.findByDrugIds(drugIds);
        }
        //基础数据为空的话则存在问题
        if (CollectionUtils.isEmpty(drugList)) {
            LOGGER.warn("updateOrganDrugListToEs drugList is empty. drugIds={}", JSONUtils.toString(drugIds));
            drugList = Lists.newArrayList();
        }
        Map<Integer, List<DrugList>> drugListMap = drugList.stream().collect(Collectors.groupingBy(DrugList::getDrugId));
        organDrugLists.forEach(organDrugList -> {
            DoctorDrugDetailVO doctorDrugDetailVO = new DoctorDrugDetailVO();
            BeanUtils.copyProperties(organDrugList, doctorDrugDetailVO);
            doctorDrugDetailVO.setDeleteFlag(deleteFlag);
            List<DrugList> drugLists = drugListMap.get(organDrugList.getDrugId());
            if (CollectionUtils.isEmpty(drugLists)) {
                doctorDrugDetailVO.setStatus(0);
            } else {
                //药品用法用量默认使用机构的，无机构数据则使用平台的，两者都无数据则为空
                if (org.springframework.util.StringUtils.isEmpty(organDrugList.getUsePathways())) {
                    doctorDrugDetailVO.setUsePathways(drugLists.get(0).getUsePathways());
                }
                if (org.springframework.util.StringUtils.isEmpty(organDrugList.getUsingRate())) {
                    doctorDrugDetailVO.setUsingRate(drugLists.get(0).getUsingRate());
                }
                if (org.springframework.util.StringUtils.isEmpty(organDrugList.getUsePathwaysId())) {
                    doctorDrugDetailVO.setUsePathwaysId(drugLists.get(0).getUsePathwaysId());
                }
                if (org.springframework.util.StringUtils.isEmpty(organDrugList.getUsingRateId())) {
                    doctorDrugDetailVO.setUsingRateId(drugLists.get(0).getUsingRateId());
                }
                //重置searchKey
                //机构药品名+平台商品名+机构商品名+院内别名
                String searchKey = organDrugList.getDrugName() + ";" + drugLists.get(0).getSaleName() + ";" + organDrugList.getSaleName() + ";" +
                        LocalStringUtil.toString(organDrugList.getRetrievalCode()) + ";" + organDrugList.getChemicalName();
                doctorDrugDetailVO.setSearchKey(searchKey.replaceAll(" ", ";"));
                doctorDrugDetailVO.setPlatformSaleName(drugLists.get(0).getSaleName());
                doctorDrugDetailVO.setDrugType(drugLists.get(0).getDrugType());
                doctorDrugDetailVO.setApplyBusiness(organDrugList.getApplyBusiness());
                //status字段修改注意：先判断基础药品库，再处理机构药品库
                if (0 == drugLists.get(0).getStatus()) {
                    doctorDrugDetailVO.setStatus(0);
                } else {
                    doctorDrugDetailVO.setStatus(organDrugList.getStatus());
                }
                //设置药房id列表
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(organDrugList.getPharmacy())) {
                    try {
                        List<String> splitToList = Splitter.on(",").splitToList(organDrugList.getPharmacy());
                        List<Integer> pharmacyIds = splitToList.stream().map(Integer::valueOf).collect(Collectors.toList());
                        doctorDrugDetailVO.setPharmacyIds(pharmacyIds);
                    } catch (Exception e) {
                        LOGGER.error("pharmacyId transform exception! updateList={}", JSONUtils.toString(organDrugList), e);
                    }
                }
            }
            updateList.add(doctorDrugDetailVO);

        });
        if (CollectionUtils.isEmpty(updateList)) {
            return;
        }
        searchService.updateDoctorDrugDetail(updateList);
    }

    /**
     * todo 分层不合理 静态不合理 方法使用不合理 需要修改 （尹盛）
     * 后台处理药品显示名---卡片消息/处方笺/处方列表页第一个药名/电子病历详情
     *
     * @param recipedetail
     * @param drugType
     * @return
     */
    public static String dealWithRecipeDrugName(Recipedetail recipedetail, Integer drugType, Integer organId) {
        LOGGER.info("DrugManager dealwithRecipeDrugName recipedetail:{},drugType:{},organId:{}", JSONUtils.toString(recipedetail), drugType, organId);
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType) || RecipeBussConstant.RECIPETYPE_HP.equals(drugType)) {
            StringBuilder stringBuilder = new StringBuilder();
            //所有页面中药药品显示统一“药品名称”和“剂量单位”以空格间隔
            stringBuilder.append(recipedetail.getDrugName());
            if (StringUtils.isNotEmpty(recipedetail.getMemo())) {
                stringBuilder.append("(").append(recipedetail.getMemo()).append(")").append(StringUtils.SPACE);
            }
            if (StringUtils.isNotEmpty(recipedetail.getUseDoseStr())) {
                stringBuilder.append(recipedetail.getUseDoseStr());
            } else {
                stringBuilder.append(recipedetail.getUseDose());
            }
            stringBuilder.append(recipedetail.getUseDoseUnit());
            return stringBuilder.toString();
        }
        if (StringUtils.isEmpty(recipedetail.getDrugDisplaySplicedName())) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(organId, recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
            LOGGER.info("DrugClient dealwithRecipeDrugName organDrugLists:{}", JSONUtils.toString(organDrugLists));
            return dealWithRecipeDetailName(organDrugLists, recipedetail);
        }
        LOGGER.info("DrugManager dealwithRecipeDrugName res:{}", recipedetail.getDrugDisplaySplicedName());
        return recipedetail.getDrugDisplaySplicedName();
    }

    public static String dealWithRecipeDetailName(List<OrganDrugList> organDrugLists, Recipedetail recipedetail) {
        LOGGER.info("DrugClient dealwithRecipedetailName organDrugLists:{},recipedetail:{}", JSONUtils.toString(organDrugLists), JSONUtils.toString(recipedetail));
        StringBuilder stringBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(organDrugLists)) {
            //机构药品名称、剂型、药品规格、单位
            stringBuilder.append(organDrugLists.get(0).getDrugName());
            if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                stringBuilder.append(organDrugLists.get(0).getDrugForm());
            }
        } else {
            LOGGER.info("DrugClient res:{}", stringBuilder.toString());
            stringBuilder.append(recipedetail.getDrugName());
        }
        //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
        stringBuilder.append(StringUtils.SPACE);
        stringBuilder.append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit());
        LOGGER.info("DrugClient res:{}", stringBuilder.toString());
        return stringBuilder.toString();
    }

    /**
     * 获取制法code 为key的Map
     *
     * @param organId 机构id
     * @return code = key对象
     */
    public Map<String, DrugMakingMethod> drugMakingMethodCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugMakingMethod> drugMakingMethodList = drugMakingMethodDao.findByOrganId(organId);
        logger.info("DrugClient drugMakingMethodList organId = {}, drugMakingMethodList:{}", organId, JSON.toJSONString(drugMakingMethodList));
        return Optional.ofNullable(drugMakingMethodList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugMakingMethod::getMethodCode, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取煎法code 为key的Map
     *
     * @param organId 机构id
     * @return code = key对象
     */
    public Map<String, DecoctionWay> decoctionWayCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DecoctionWay> decoctionWayList = drugDecoctionWayDao.findByOrganId(organId);
        logger.info("DrugClient decoctionWayCodeMap organId = {} ,decoctionWayList:{}", organId, JSON.toJSONString(decoctionWayList));
        return Optional.ofNullable(decoctionWayList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DecoctionWay::getDecoctionCode, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取嘱托（特殊煎法）code 为key的Map
     *
     * @param organId 机构id
     * @return 机构name = key对象
     */
    public Map<String, DrugEntrust> drugEntrustNameMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        if (CollectionUtils.isEmpty(drugEntrusts)) {
            drugEntrusts = drugEntrustDAO.findByOrganId(0);
        }
        logger.info("DrugClient drugEntrustNameMap organId = {} ,drugEntrusts={}", organId, JSON.toJSONString(drugEntrusts));
        return Optional.ofNullable(drugEntrusts).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugEntrust::getDrugEntrustName, a -> a, (k1, k2) -> k1));
    }

    public Map<Integer, DrugEntrust> drugEntrustIdMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        logger.info("DrugClient drugEntrustNameMap organId = {} ,drugEntrusts={}", organId, JSON.toJSONString(drugEntrusts));
        return Optional.ofNullable(drugEntrusts).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugEntrust::getDrugEntrustId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构 药物使用频率
     *
     * @param organId        机构id
     * @param organUsingRate 机构药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String organUsingRate) {
        return drugClient.usingRate(organId, organUsingRate);
    }

    /**
     * 获取机构 药物使用途径
     *
     * @param organId          机构id
     * @param organUsePathways 机构药物使用途径代码
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String organUsePathways, Integer drugType) {
        return drugClient.usePathways(organId, organUsePathways, drugType);
    }

    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 id = key对象
     */
    public Map<Integer, UsingRate> usingRateMap(Integer organId) {
        return drugClient.usingRateMap(organId);
    }


    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 机构code = key对象
     */
    public Map<String, UsingRate> usingRateMapCode(Integer organId) {
        return drugClient.usingRateMapCode(organId);
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 id = key对象
     */
    public Map<Integer, UsePathways> usePathwaysMap(Integer organId) {
        return drugClient.usePathwaysMap(organId);
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 机构code = key对象
     */
    public Map<String, UsePathways> usePathwaysCodeMap(Integer organId) {
        return drugClient.usePathwaysCodeMap(organId);
    }

    /**
     * 患者端搜索药品
     *
     * @param saleName 搜索关键字
     * @param organId  机构id
     * @param drugType 类型
     * @param start    起始
     * @param limit    条数
     * @return
     */
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(String saleName, String organId, List<String> drugType, int start, int limit) {
        logger.info("DrugManager findDrugWithEsByPatient saleName : {} organId:{} drugType:{} start:{}  limit:{}", saleName, organId, JSON.toJSONString(drugType), start, limit);
        // 搜索药品信息
        List<PatientDrugWithEsDTO> drugWithEsByPatient = drugClient.findDrugWithEsByPatient(saleName, organId, drugType, start, limit);
        if (CollectionUtils.isEmpty(drugWithEsByPatient)) {
            return new ArrayList<>();
        }
        // 拼接 药品图片
        Set<Integer> drugIds = drugWithEsByPatient.stream().map(PatientDrugWithEsDTO::getDrugId).collect(Collectors.toSet());
        List<DrugList> byDrugIds = drugListDAO.findByDrugIds(drugIds);
        logger.info("DrugManager findDrugWithEsByPatient  byDrugIds:{}", JSON.toJSONString(byDrugIds));
        if (CollectionUtils.isEmpty(byDrugIds)) {
            return drugWithEsByPatient;
        }
        Map<Integer, List<DrugList>> collect = byDrugIds.stream().collect(Collectors.groupingBy(DrugList::getDrugId));
        drugWithEsByPatient.forEach(patientDrugWithEsDTO -> {
            BigDecimal salePrice = patientDrugWithEsDTO.getSalePrice();
            int numberOfDecimalPlaces = getNumberOfDecimalPlaces(salePrice);
            if (numberOfDecimalPlaces <= 2) {
                salePrice = salePrice.setScale(2, BigDecimal.ROUND_HALF_UP);
            } else if (numberOfDecimalPlaces > 4) {
                salePrice = salePrice.setScale(4, BigDecimal.ROUND_HALF_UP);
            }
            patientDrugWithEsDTO.setSalePrice(salePrice);
            patientDrugWithEsDTO.setShowSalePrice(String.valueOf(salePrice));
            List<DrugList> drugLists = collect.get(patientDrugWithEsDTO.getDrugId());
            if (CollectionUtils.isNotEmpty(drugLists)) {
                patientDrugWithEsDTO.setDrugPic(drugLists.get(0).getDrugPic());
            }
        });
        logger.info("DrugManager findDrugWithEsByPatient res drugWithEsByPatient:{}", JSON.toJSONString(drugWithEsByPatient));
        return drugWithEsByPatient;
    }

    /**
     * 获取 bigDecimal 的小数位数
     * @param bigDecimal
     * @return
     */
    private int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
        String string = bigDecimal.stripTrailingZeros().toPlainString();
        int index = string.indexOf(".");
        return index < 0 ? 0 : string.length() - index - 1;
    }


    /**
     * 查询es 药品数据
     *
     * @param drugInfoDTO 查询信息
     * @param start       页数
     * @param limit       条数
     * @return
     */
    public List<String> searchOrganDrugEs(DrugInfoDTO drugInfoDTO, int start, int limit) {
        boolean isMergeRecipeType = configurationClient.getValueBooleanCatch(drugInfoDTO.getOrganId(), "isMergeRecipeType", false);
        return drugClient.searchOrganDrugEs(drugInfoDTO, isMergeRecipeType, start, limit);
    }

    /**
     * 获取药品说明书
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品编码
     * @return
     */
    public Dispensatory getDrugBook(Integer organId, String organDrugCode) {
        logger.info("DrugManager.getDrugBook req organId={} organDrugCode={}", organId, organDrugCode);
        // 根据机构与机构药品编码获取 药品id
        List<OrganDrugList> byOrganDrugCodeAndOrganId = organDrugListDAO.findByOrganDrugCodeAndOrganId(organDrugCode, organId);
        if (CollectionUtils.isEmpty(byOrganDrugCodeAndOrganId)) {
            return null;
        }
        Integer drugId = byOrganDrugCodeAndOrganId.get(0).getDrugId();
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugId);
        logger.info("DrugManager.getDrugBook res dispensatory={} drugId={}", dispensatory, drugId);
        return dispensatory;
    }

    /**
     * 十八反十九畏的规则
     *
     * @param list
     * @param ruleId
     * @return
     */
    public List<RecipeRulesDrugCorrelation> getListDrugRules(List<Integer> list, Integer ruleId) {
        logger.info("DrugManager.getListDrugRules req list={} ruleId={}", JSON.toJSONString(list), ruleId);
        List<RecipeRulesDrugCorrelation> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(list) || Objects.isNull(ruleId)) {
            return result;
        }
        result = recipeRulesDrugCorrelationDAO.findListRules(list, ruleId);
        logger.info("DrugManager.getDrugBook res result={} ruleId={}", JSON.toJSONString(result), ruleId);
        return result;
    }

    /**
     * 根据ID获取平台药品列表
     *
     * @param drugIds 平台药品id
     * @return
     */
    public List<DrugList> drugList(List<Integer> drugIds) {
        if (CollectionUtils.isEmpty(drugIds)) {
            return new LinkedList<>();
        }
        List<DrugList> drugs = drugListDAO.findByDrugIds(drugIds);
        if (CollectionUtils.isEmpty(drugs)) {
            return new LinkedList<>();
        }
        return drugs;
    }

    /**
     * 根据ID获取平台药品列表
     *
     * @param drugIds 平台药品id
     * @return
     */
    public List<DrugList> drugList(List<Integer> drugIds, Integer drugType) {
        if (CollectionUtils.isEmpty(drugIds)) {
            return new LinkedList<>();
        }
        List<DrugList> drugs = drugListDAO.findByDrugIdsAndType(drugIds, drugType);
        if (CollectionUtils.isEmpty(drugs)) {
            return new LinkedList<>();
        }
        return drugs;
    }

    /**
     * 根据ID获取平台药品列表
     *
     * @param drugIds 平台药品id
     * @return
     */
    public List<DrugList> findByDrugIdsAndStatus(List<Integer> drugIds) {
        if (CollectionUtils.isEmpty(drugIds)) {
            return new LinkedList<>();
        }
        List<DrugList> drugs = drugListDAO.findByDrugIdsAndStatus(drugIds);
        if (CollectionUtils.isEmpty(drugs)) {
            return new LinkedList<>();
        }
        return drugs;
    }

    /**
     * 定时 获取用药提醒的线下处方
     */
    public void remindPatient(List<RecipeInfoDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        logger.info("DrugManager remindPatient list size:{}", list.size());
        List<List<RecipeInfoDTO>> recipeInfoList = Lists.partition(list, 150);
        if (CollectionUtils.isEmpty(recipeInfoList)) {
            return;
        }
        recipeInfoList.forEach(a -> patientClient.remindPatientTakeMedicine(a));
    }

    public List<OrganDrugList> listOrganDrug(ListOrganDrugReq listOrganDrugReq) {
        return organDrugListDAO.listOrganDrugByTime(listOrganDrugReq);
    }

    public List<DrugCommon> commonDrugList(Integer organId, Integer doctorId, List<Integer> drugTypes) {
        List<DrugCommon> drugCommonList = drugCommonDAO.findByOrganIdAndDoctorIdAndTypes(organId, doctorId, drugTypes, 0, 30);
        logger.info("DrugManager commonDrugList organId={},doctorId-{},drugTypes={}, list={}"
                , organId, doctorId, JSON.toJSONString(drugTypes), JSON.toJSONString(drugCommonList));
        return drugCommonList;
    }

    public Integer saveCommonDrug(Integer recipeId) {
        return this.saveCommonDrug(recipeDAO.getByRecipeId(recipeId));
    }

    /**
     * 保存常用药
     *
     * @param recipe
     * @return
     */
    public Integer saveCommonDrug(Recipe recipe) {
        if (ValidateUtil.validateObjects(recipe, recipe.getClinicOrgan(), recipe.getDoctor())) {
            return null;
        }
        logger.info("DrugManager saveCommonDrug start recipe={}", recipe.getRecipeId());
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return null;
        }
        recipeDetails.forEach(a -> {
            if (ValidateUtil.validateObjects(a.getDrugId(), a.getOrganDrugCode())) {
                return;
            }
            DrugCommon drugCommon = drugCommonDAO.getByOrganIdAndDoctorIdAndDrugCode(recipe.getClinicOrgan(), recipe.getDoctor(), a.getOrganDrugCode());
            if (null != drugCommon) {
                drugCommon.setSort(drugCommon.getSort() + 1);
                drugCommonDAO.update(drugCommon);
            } else {
                drugCommon = new DrugCommon();
                drugCommon.setDrugId(a.getDrugId());
                drugCommon.setDrugType(a.getDrugType());
                drugCommon.setOrganDrugCode(a.getOrganDrugCode());
                drugCommon.setDoctorId(recipe.getDoctor());
                drugCommon.setOrganId(recipe.getClinicOrgan());
                drugCommon.setSort(1);
                drugCommonDAO.save(drugCommon);
            }
        });
        logger.info("DrugManager saveCommonDrug end recipe={}", recipe.getRecipeId());
        return recipe.getRecipeId();
    }

    public DrugSaleStrategy getDrugSaleStrategyById(Integer id) {
        return drugSaleStrategyDAO.getDrugSaleStrategyById(id);
    }

    public List<DrugSaleStrategy> findDrugSaleStrategy(Integer drugId){
        return drugSaleStrategyDAO.findByDrugId(drugId);
    }

    public DrugSaleStrategy getDefaultDrugSaleStrategy(Integer depId, Integer drugId){
        if (null == drugId) {
            return  null;
        }
        DrugList drugList = drugListDAO.getById(drugId);
        if (null == drugList) {
            return  null;
        }
        DrugSaleStrategy drugSaleStrategy = new DrugSaleStrategy();
        drugSaleStrategy.setDrugId(drugId);
        drugSaleStrategy.setDrugAmount(1);
        drugSaleStrategy.setDrugUnit(drugList.getUnit());
        drugSaleStrategy.setStrategyTitle("默认出售策略");
        drugSaleStrategy.setStatus(1);
        drugSaleStrategy.setId(0);
        return drugSaleStrategy;
    }

}
