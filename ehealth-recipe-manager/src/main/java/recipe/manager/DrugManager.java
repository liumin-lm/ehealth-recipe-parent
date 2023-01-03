package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.his.recipe.mode.MedicationInfoResTO;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
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
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.client.BusinessLogClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.SyncDrugConstant;
import recipe.dao.*;
import recipe.util.LocalStringUtil;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private DispensatoryDAO dispensatoryDAO;
    @Autowired
    private RecipeRulesDrugCorrelationDAO recipeRulesDrugCorrelationDAO;
    @Autowired
    private DrugCommonDAO drugCommonDAO;
    @Autowired
    private DrugSearchService searchService;
    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;
    @Autowired
    private OrganDrugListSyncFieldDAO organDrugListSyncFieldDAO;
    @Autowired
    private DrugOrganConfigDAO drugOrganConfigDAO;
    @Autowired
    private BusinessLogClient businessLogClient;
    private String YES="1";
    private String NO ="0";


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

    public List<DrugCommon> commonDrugList(Integer organId, Integer doctorId, List<Integer> drugTypes, String drugForm) {
        List<DrugCommon> drugCommonList;
        if (drugTypes.contains(RecipeBussConstant.RECIPETYPE_TCM)) {
            drugCommonList = drugCommonDAO.findByOrganIdAndDoctorIdAndDrugForm(organId, doctorId, drugForm, 0, 30);
        }else {
            drugCommonList = drugCommonDAO.findByOrganIdAndDoctorIdAndTypes(organId, doctorId, drugTypes, 0, 30);
        }
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
            Integer drugType = ValidateUtil.integerIsEmpty(a.getDrugType()) ? recipe.getRecipeType() : a.getDrugType();
            if (null != drugCommon) {
                drugCommon.setSort(drugCommon.getSort() + 1);
                drugCommon.setDrugType(drugType);
                drugCommonDAO.update(drugCommon);
            } else {
                drugCommon = new DrugCommon();
                drugCommon.setDrugId(a.getDrugId());
                drugCommon.setDrugType(drugType);
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

    /**
     * 添加机构药品同步字段
     * @param organId
     * @return
     */
    @LogRecord
    public List<OrganDrugListSyncField> addOrganDrugListSyncFieldForOrgan(Integer organId) {
        List<OrganDrugListSyncField> organDrugListSyncFieldList=new ArrayList<>();
        LinkedHashMap<String,DrugSyncFieldDTO> fieldMap=initSyncFieldMapForOrgan();
        Set set = fieldMap.keySet();
        List<String> typeList=initTypeList();
        for (Object key : set) {
            DrugSyncFieldDTO drugSyncFieldDTO=fieldMap.get(key);
            typeList.forEach(type->{
                OrganDrugListSyncField organDrugListSyncField=new OrganDrugListSyncField();
                organDrugListSyncField.setOrganId(organId);
                organDrugListSyncField.setFieldCode(key+"");
                organDrugListSyncField.setFieldName(drugSyncFieldDTO.getFieldName());
                organDrugListSyncField.setType(type);
                if("1".equals(type)){
                    //新增
                    organDrugListSyncField.setIsAllowEdit(drugSyncFieldDTO.getAddIsAllowEdit());
                }else{
                    organDrugListSyncField.setIsAllowEdit(drugSyncFieldDTO.getUpdateIsAllowEdit());
                }
                organDrugListSyncFieldList.add(addOrUpdateOrganDrugListSyncField(organDrugListSyncField));
            });
        }
        return organDrugListSyncFieldList;

    }

    /**
     * 通过OrganId(),FieldCode(),Type()查询，存在update 不存在add
     *
     * @param organDrugListSyncField
     * @return
     */
    @LogRecord
    public  OrganDrugListSyncField addOrUpdateOrganDrugListSyncField(OrganDrugListSyncField organDrugListSyncField) {
        logger.info("addOrUpdateOrganDrugListSyncField:{}",JSONUtils.toString(organDrugListSyncField));
        if (ObjectUtils.isEmpty(organDrugListSyncField.getOrganId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构 is required");
        }

        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        List<OrganDrugListSyncField> organDrugListSyncFieldDbs = organDrugListSyncFieldDAO.findByOrganIdAndFieldCodeAndType(organDrugListSyncField.getOrganId(),organDrugListSyncField.getFieldCode(),organDrugListSyncField.getType());
        logger.info("addOrUpdateOrganDrugListSyncField organDrugListSyncFieldDbs:{}",JSONUtils.toString(organDrugListSyncFieldDbs));
        OrganDrugListSyncField organDrugListSyncFieldDb=null;
        if(CollectionUtils.isNotEmpty(organDrugListSyncFieldDbs)){
            organDrugListSyncFieldDb=organDrugListSyncFieldDbs.get(0);
            logger.info("addOrUpdateOrganDrugListSyncField saleDrugListSyncFieldDb:{}",JSONUtils.toString(organDrugListSyncFieldDbs));
        }
        organDrugListSyncField.init(organDrugListSyncField);
        if (ObjectUtils.isEmpty(organDrugListSyncFieldDb)){
            OrganDrugListSyncField save = organDrugListSyncFieldDAO.save(organDrugListSyncField);
            if (!ObjectUtils.isEmpty(urt)){
                try {
                    busActionLogService.recordBusinessLogRpcNew("机构配置管理", "", "OrganDrugListSyncField", "【" + urt.getUserName() + "】新增机构药品同步字段配置【" + JSONUtils.toString(save)
                            +"】", organClient.organDTO(organDrugListSyncField.getOrganId()).getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logger.info("addOrUpdateOrganDrugListSyncField save:{}",JSONUtils.toString(save));
            return save;
        }else {
            organDrugListSyncField.setId(organDrugListSyncFieldDb.getId());
            OrganDrugListSyncField update = organDrugListSyncFieldDAO.update(organDrugListSyncField);
            if (!ObjectUtils.isEmpty(urt)){
                try {
                    busActionLogService.recordBusinessLogRpcNew("机构配置管理", "", "OrganDrugListSyncField", "【" + urt.getUserName() + "】更新机构药品同步字段配置【"+JSONUtils.toString(organDrugListSyncField)+"】-》【" + JSONUtils.toString(update)
                            +"】", organClient.organDTO(organDrugListSyncField.getOrganId()).getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logger.info("addOrUpdateOrganDrugListSyncField update:{}",JSONUtils.toString(update));
            return update;
        }
    }

    @LogRecord
    private LinkedHashMap<String,DrugSyncFieldDTO> initSyncFieldMapForOrgan(){
        LinkedHashMap<String,DrugSyncFieldDTO> fieldMap=new LinkedHashMap<>();
        //line 1
        fieldMap.put(SyncDrugConstant.organDrugCode,new DrugSyncFieldDTO("药品唯一索引","organDrugCode",YES,NO,YES,NO) );
        fieldMap.put(SyncDrugConstant.drugName,new DrugSyncFieldDTO("药品名",SyncDrugConstant.drugName,YES,NO,YES,YES));
        fieldMap.put(SyncDrugConstant.saleName,new DrugSyncFieldDTO("商品名",SyncDrugConstant.saleName,YES,NO,YES,YES));
        fieldMap.put(SyncDrugConstant.retrievalCode,new DrugSyncFieldDTO("院内检索码",SyncDrugConstant.retrievalCode,YES,NO,YES,YES));
//        fieldMap.put(SyncDrugConstant.drugType,new DrugSyncFieldDTO("药品类型",SyncDrugConstant.drugType,YES,NO,YES,YES));

        fieldMap.put(SyncDrugConstant.drugSpec,new DrugSyncFieldDTO("药品规格",SyncDrugConstant.drugSpec,YES,NO,YES,YES));
        fieldMap.put(SyncDrugConstant.salePrice,new DrugSyncFieldDTO("价格",SyncDrugConstant.salePrice,YES,NO,YES,YES));
//        fieldMap.put(SyncDrugConstant.applyBusiness,new DrugSyncFieldDTO("适用业务",SyncDrugConstant.applyBusiness,YES,NO,YES,YES));
//        fieldMap.put("status","状态");

        fieldMap.put(SyncDrugConstant.unit,new DrugSyncFieldDTO("出售单位",SyncDrugConstant.unit,YES,NO,YES,YES));//？？
        fieldMap.put(SyncDrugConstant.pack,new DrugSyncFieldDTO("出售单位下的包装量",SyncDrugConstant.pack,YES,NO,YES,YES));
        fieldMap.put(SyncDrugConstant.useDose,new DrugSyncFieldDTO("注册规格单位剂量",SyncDrugConstant.useDose,YES,NO,YES,YES));
        fieldMap.put(SyncDrugConstant.producer,new DrugSyncFieldDTO("生产厂家",SyncDrugConstant.retrievalCode,YES,NO,YES,YES));

        fieldMap.put(SyncDrugConstant.pharmacy,new DrugSyncFieldDTO("开方药房",SyncDrugConstant.pharmacy,YES,YES,YES,YES));//对应编码与名称吗
        fieldMap.put(SyncDrugConstant.drugForm,new DrugSyncFieldDTO("剂型",SyncDrugConstant.drugForm,YES,YES,YES,YES));
        fieldMap.put(SyncDrugConstant.drugsEnterpriseIds,new DrugSyncFieldDTO("出售流转药企",SyncDrugConstant.drugsEnterpriseIds,YES,YES,YES,YES));
        fieldMap.put(SyncDrugConstant.drugEntrust,new DrugSyncFieldDTO("药品嘱托",SyncDrugConstant.drugEntrust,YES,YES,YES,YES));
        fieldMap.put(SyncDrugConstant.medicalInsuranceControl,new DrugSyncFieldDTO("医保控制",SyncDrugConstant.medicalInsuranceControl,YES,YES,YES,YES));

        fieldMap.put(SyncDrugConstant.indicationsDeclare,new DrugSyncFieldDTO("适应症说明",SyncDrugConstant.indicationsDeclare,YES,YES,YES,YES));

        return fieldMap;
    }

    @LogRecord
    private List<String> initTypeList(){
        List<String> typeList=new ArrayList<>();
        //新增
        typeList.add("1");
        //更新
        typeList.add("2");
        logger.info("initTypeList:{}",JSONUtils.toString(typeList));
        return typeList;
    }

//    protected void logChangeConfig(Class<?> bizClass, Organ organ, String optionName, Object value) {
//        if (value instanceof Boolean) {
//            value = ((boolean) value) ? "打开" : "关闭";
//        } else {
//            value = "改为: " + value;
//        }
//        businessLogClient.recordBusinessLog("修改机构设置", organ.getOrganId().toString(), bizClass.getName(),
//                String.format("[%2$s](%1$s)的'%3$s'选项被%4$s", organ.getOrganId(), organ.getName(), optionName, value), organ.getName());
//    }

    /**
     * 更新机构数据字典中用药频次、用药途径的同步配置
     * @param medicationSyncConfig
     * @return
     */
    public Boolean updateMedicationSyncConfig(MedicationSyncConfig medicationSyncConfig) {
        return medicationSyncConfigDAO.updateNonNullFieldByPrimaryKey(medicationSyncConfig);
    }

    /**
     * 查询机构数据字典中用药频次、用药途径的同步配置
     * @param organId,datctype
     * @return
     */
    public MedicationSyncConfig getMedicationSyncConfig(Integer organId,Integer dataType) {
        MedicationSyncConfig medicationSyncConfig = medicationSyncConfigDAO.getMedicationSyncConfigByOrganIdAndDataType(organId, dataType);
        if(Objects.isNull(medicationSyncConfig)){
            medicationSyncConfig = new MedicationSyncConfig();
            medicationSyncConfig.setOrganId(organId);
            medicationSyncConfig.setDataType(dataType);
            medicationSyncConfigDAO.save(medicationSyncConfig);
            medicationSyncConfig = medicationSyncConfigDAO.getMedicationSyncConfigByOrganIdAndDataType(organId, dataType);
        }
        return medicationSyncConfig;
    }

    /**
     * 定时同步机构数据字典中用药频次、用药途径
     */
    public List<MedicationInfoResTO> medicationInfoSyncTask() {
        LocalTime localTime = LocalTime.now();
        LocalTime localTime1 = localTime.minusMinutes(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String format = localTime.format(dateTimeFormatter);
        String format1 = localTime1.format(dateTimeFormatter);
        Time etime = Time.valueOf(format);
        Time stime = Time.valueOf(format1);
        List<MedicationSyncConfig> medicationSyncConfigs = medicationSyncConfigDAO.findByRegularTime(stime, etime);
        //List<MedicationSyncConfig> medicationSyncConfigs = medicationSyncConfigDAO.findByTime();
        logger.info("medicationInfoSyncTask medicationSyncConfigs:{}",JSONUtils.toString(medicationSyncConfigs));
        List<MedicationInfoResTO> medicationInfoResTOList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(medicationSyncConfigs)) {
            for (MedicationSyncConfig medicationSyncConfig : medicationSyncConfigs) {
                try {
                    Boolean enableSync = medicationSyncConfig.getEnableSync();
                    Integer dockingMode = medicationSyncConfig.getDockingMode();
                    if (ObjectUtils.isEmpty(dockingMode)) {
                        throw new DAOException(DAOException.VALUE_NEEDED,
                                (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次")  + "未找到同步模式配置!");
                    }
                    if (dockingMode == 2) {
                        throw new DAOException(DAOException.VALUE_NEEDED,
                                (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次")  + "同步模式 为【主动推送】 调用无效!");
                    }
                    if (!enableSync) {
                        throw new DAOException(DAOException.VALUE_NEEDED,
                                (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次")  +  "同步开关未开启");
                    }
                    List<MedicationInfoResTO> medicationInfoResTOS = drugClient.medicationInfoSyncTask(medicationSyncConfig.getOrganId(), medicationSyncConfig.getDataType());
                    medicationInfoResTOList.addAll(medicationInfoResTOS);
                } catch (Exception e) {
                    logger.error("medicationInfoSyncTask同步 organId=[{}],机构定时更新异常：exception=[{}].", medicationSyncConfig.getOrganId(), e);
                }
            }
        }
        return medicationInfoResTOList;
    }

    /**
     * 根据同步数据范围判断能否同步数据
     * @param syncDataRange 新增 药品同步 数据范围   1药品类型 2  药品剂型  默认1
     * @param syncDrugType
     * @param
     * @param
     * @return
     */
    @LogRecord
    public boolean isAllowDealBySyncDataRange(String organDrugCode, Integer syncDataRange, String syncDrugType, String drugFormList, Integer drugType, String drugForm) {
        //新增药品 勾选的药品类型是否包括当前药品类型 包括新增，不包括不新增
        //更新字段 字段没被勾选上不更新，其余情况全部更新
        boolean isAllow=false;
        try {
            if (syncDataRange==null||syncDataRange == 1) {
                //同步数据范围 药品类型
                if (ObjectUtils.isEmpty(syncDrugType)) {
                    LOGGER.info("isAllowDealBySyncDataRange 此条药品不允许同步 原因是未找到该药企[同步药品类型]配置数据:{}",organDrugCode);
                    throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[同步药品类型]配置数据!");
                }

                String[] syncDrugTypeStr = syncDrugType.split(",");
                List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(syncDrugTypeStr));
                //1西药 2中成药 3中药
                if (syncDrugTypeList.indexOf("1") != -1&&new Integer("1").equals(drugType)
                        ||syncDrugTypeList.indexOf("2") != -1 && new Integer("2").equals(drugType)
                        ||syncDrugTypeList.indexOf("3") != -1 && new Integer("3").equals(drugType)) {
                    isAllow=true;
                }else{
                    LOGGER.info("isAllowDealBySyncDataRange 此条药品不允许同步 原因是当前药品药品类型没在配置范围内:{}",organDrugCode);
                }
            }else{
                //同步数据范围 药品剂型
                List drugForms = Lists.newArrayList();
                if (!ObjectUtils.isEmpty(drugFormList)) {
                    String[] split = drugFormList.split(",");
                    for (String s : split) {
                        drugForms.add(s);
                    }
                }
                if (drugForms != null && drugForms.size() > 0) {
                    int i = drugForms.indexOf(drugForm);
                    if (-1 != i) {
                        isAllow=true;
                    }else{
                        LOGGER.info("isAllowDealBySyncDataRange 此条药品不允许同步 原因是当前药品剂型没在配置范围内:{}",organDrugCode);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("isAllowDealBySyncDataRange error organDrugCode:{}",organDrugCode,e);
        }

        return isAllow;
    }

}
