package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.miscellany.mode.HospitalInformationRequestTO;
import com.ngari.his.recipe.mode.MedicationInfoResTO;
import com.ngari.patient.dto.UsePathwaysDTO;
import com.ngari.patient.dto.UsingRateDTO;
import com.ngari.patient.service.IUsePathwaysService;
import com.ngari.patient.service.IUsingRateService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.HospitalDrugListDTO;
import com.ngari.platform.recipe.mode.HospitalDrugListReqDTO;
import com.ngari.recipe.drug.model.*;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.client.IConfigurationClient;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IDrugBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.YesOrNoEnum;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.DrugManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.OrganDrugListManager;
import recipe.util.MapValueUtil;
import recipe.vo.greenroom.OrganConfigVO;
import recipe.vo.greenroom.OrganDrugListSyncFieldVo;
import recipe.vo.patient.HisDrugInfoReqVO;
import recipe.vo.patient.PatientContinueRecipeCheckDrugReq;
import recipe.vo.patient.PatientContinueRecipeCheckDrugRes;
import recipe.vo.patient.PatientOptionalDrugVo;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 药品业务类
 * @author： whf
 * @date： 2021-08-23 18:58
 */
@Service
public class DrugBusinessService extends BaseService implements IDrugBusinessService {
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Resource
    private DrugManager drugManager;
    @Autowired
    private HisRecipeManager hisRecipeManager;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DrugClient drugClient;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private RecipeRulesDrugCorrelationDAO recipeRulesDrugCorrelationDAO;
    @Autowired
    private DrugSaleStrategyDAO drugSaleStrategyDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private DrugListDAO drugListDAO;
    @Autowired
    private OrganDrugListSyncFieldDAO organDrugListSyncFieldDAO;
    @Autowired
    private DrugOrganConfigDAO drugOrganConfigDao;
    @Autowired
    private IUsingRateService usingRateService;
    @Autowired
    private IUsePathwaysService usePathwaysService;
    @Autowired
    private MedicationSyncConfigDAO medicationSyncConfigDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

    @Override
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo) {
        return drugManager.findDrugWithEsByPatient(searchDrugReqVo.getSaleName(), searchDrugReqVo.getOrganId().toString(), Arrays.asList(RecipeTypeEnum.RECIPETYPE_WM.getType().toString(), RecipeTypeEnum.RECIPETYPE_CPM.getType().toString()), searchDrugReqVo.getStart(), searchDrugReqVo.getLimit());
    }

    @Override
    public List<SearchDrugDetailDTO> searchOrganDrugEs(DrugInfoDTO drugInfoDTO, int start, int limit, Integer clinicId) {
        List<String> drugInfo = drugManager.searchOrganDrugEs(drugInfoDTO, start, limit);
        Integer organId = drugInfoDTO.getOrganId();
        Integer drugType = drugInfoDTO.getDrugType();
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, drugType));
        //药品商品名拼接配置
        Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, drugType));
        Map<Integer, DrugEntrust> drugEntrustNameMap = drugManager.drugEntrustIdMap(organId);

        Map<String, Double> map = recipeDetailManager.sumTotalMap(clinicId);

        List<SearchDrugDetailDTO> list = new LinkedList<>();
        drugInfo.forEach(s -> {
            SearchDrugDetailDTO drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
            RecipeUtil.SearchDrugDetailDTO(drugList, configDrugNameMap, configSaleNameMap, drugEntrustNameMap);
            Double num = map.get(drugList.getOrganDrugCode());
            if (null != num) {
                drugList.setOpenDrugNum(num.intValue());
            }
            list.add(drugList);
        });
        logger.info("DrugBusinessService searchOrganDrugEs list= {}", JSONUtils.toString(list));
        return list;
    }

    @Override
    public Dispensatory getDrugBook(Integer organId, String organDrugCode) {
        return drugManager.getDrugBook(organId, organDrugCode);
    }

    @Override
    public List<RecipeRulesDrugCorrelation> getListDrugRules(List<Integer> list, Integer ruleId) {
        return drugManager.getListDrugRules(list, ruleId);
    }

    @Override
    public DrugSpecificationInfoDTO hisDrugBook(Integer organId, Recipedetail recipedetail) {
        return organDrugListManager.hisDrugBook(organId, recipedetail);
    }

    @Override
    public List<DrugList> drugList(List<Integer> drugIds) {
        return drugManager.drugList(drugIds);
    }

    @Override
    public Map<String, OrganDrugList> organDrugMap(Integer organId, List<Integer> drugIds) {
        return organDrugListManager.getOrganDrugByIdAndCode(organId, drugIds);
    }

    @Override
    public void queryRemindRecipe(String dateTime) {
        List<Integer> organIdList = configurationClient.organIdList("remindPatientTakeMedicineFlag", "true");
        organIdList.forEach(a -> {
            try {
                List<RecipeInfoDTO> list = hisRecipeManager.queryRemindRecipe(a, dateTime);
                drugManager.remindPatient(list);
            } catch (Exception e) {
                logger.info("DrugBusinessService queryRemindRecipe organId= {}", a, e);
            }
        });
    }

    @Override
    public List<OrganDrugList> listOrganDrug(com.ngari.platform.recipe.mode.ListOrganDrugReq listOrganDrugReq) {
        return drugManager.listOrganDrug(ObjectCopyUtils.convert(listOrganDrugReq, com.ngari.recipe.dto.ListOrganDrugReq.class));
    }

    @Override

    public List<HospitalDrugListVO> findHospitalDrugList(HospitalDrugListReqVO hospitalDrugListReqVO) {
        HospitalDrugListReqDTO hospitalDrugListReqDTO = recipe.util.ObjectCopyUtils.convert(hospitalDrugListReqVO, HospitalDrugListReqDTO.class);
        List<HospitalDrugListDTO> hospitalDrugListDTOList = drugClient.findHospitalDrugList(hospitalDrugListReqDTO);
        return recipe.util.ObjectCopyUtils.convert(hospitalDrugListDTOList, HospitalDrugListVO.class);
    }

    @Override
    public DispensatoryDTO getOrganDrugList(Integer organId, Integer drugId) {
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
        if (CollectionUtils.isNotEmpty(organDrugLists)) {
            OrganDrugList organDrugList = organDrugLists.get(0);
            DispensatoryDTO dispensatoryDTO = new DispensatoryDTO();
            dispensatoryDTO.setDrugName(organDrugList.getDrugName());
            dispensatoryDTO.setSaleName(organDrugList.getSaleName());
            dispensatoryDTO.setSpecs(organDrugList.getDrugSpec());
            dispensatoryDTO.setManufacturers(organDrugList.getProducer());
            dispensatoryDTO.setUnit(organDrugList.getUnit());
            return dispensatoryDTO;
        }
        return new DispensatoryDTO();
    }

    @Override
    public List<OrganDrugList> organDrugList(Integer organId, List<String> organDrugCodes) {
        if (CollectionUtils.isEmpty(organDrugCodes)) {
            return Collections.emptyList();
        }
        return organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodes);

    }

    @Override
    public OrganDrugList getOrganDrugList(Integer organId, String organDrugCode, Integer drugId) {
        return organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, organDrugCode, drugId);
    }

    @Override
    public List<DrugList> findByDrugIdsAndStatus(List<Integer> drugIds) {
        return drugManager.findByDrugIdsAndStatus(drugIds);
    }

    @Override
    public PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrug(PatientContinueRecipeCheckDrugReq patientContinueRecipeCheckDrugReq) {
        PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrugRes = new PatientContinueRecipeCheckDrugRes();
        List<PatientOptionalDrugVo> patientOptionalDrugVos = patientContinueRecipeCheckDrugReq.getPatientOptionalDrugVo();
        if (CollectionUtils.isEmpty(patientOptionalDrugVos)) {
            throw new DAOException(609, "续方药品信息不能为空");
        }
        List<String> organDrugCodeList = patientOptionalDrugVos.stream().map(PatientOptionalDrugVo::getOrganDrugCode).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(organDrugCodeList)) {
            throw new DAOException(609, "机构编码不能为空");
        }
        List<OrganDrugList> organDrugListList = organDrugListDAO.findByOrganIdAndDrugCodes(patientContinueRecipeCheckDrugReq.getOrganId(), organDrugCodeList);
        List<String> drugName = new ArrayList<>();
        List<PatientOptionalDrugVo> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(organDrugListList)) {
            List<String> collect = patientOptionalDrugVos.stream().map(PatientOptionalDrugVo::getDrugName).collect(Collectors.toList());
            getCheckText(patientContinueRecipeCheckDrugRes, collect, patientContinueRecipeCheckDrugReq.getShoppingCartType());
            patientContinueRecipeCheckDrugRes.setPatientOptionalDrugVo(list);
            return patientContinueRecipeCheckDrugRes;
        }
        Map<String, List<OrganDrugList>> organDrugListMap = organDrugListList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
        patientOptionalDrugVos.forEach(patientOptionalDrugVo -> {
            List<OrganDrugList> organDrugLists = organDrugListMap.get(patientOptionalDrugVo.getOrganDrugCode());
            if (CollectionUtils.isEmpty(organDrugLists)) {
                drugName.add(patientOptionalDrugVo.getDrugName());
            } else {
                patientOptionalDrugVo.setDrugId(organDrugLists.get(0).getDrugId());
                patientOptionalDrugVo.setOrganId(patientContinueRecipeCheckDrugReq.getOrganId());
                patientOptionalDrugVo.setDrugName(organDrugLists.get(0).getDrugName());
                patientOptionalDrugVo.setDrugSpec(organDrugLists.get(0).getDrugSpec());
                patientOptionalDrugVo.setUnit(organDrugLists.get(0).getUnit());
                list.add(patientOptionalDrugVo);
            }
        });
        if (CollectionUtils.isEmpty(drugName)) {
            patientContinueRecipeCheckDrugRes.setCheckFlag(YesOrNoEnum.NO.getType());
        } else {
            getCheckText(patientContinueRecipeCheckDrugRes, drugName, patientContinueRecipeCheckDrugReq.getShoppingCartType());
        }
        patientContinueRecipeCheckDrugRes.setPatientOptionalDrugVo(list);
        return patientContinueRecipeCheckDrugRes;
    }

    @Override
    public List<SearchDrugDetailDTO> commonDrugList(CommonDrugListDTO commonDrug) {
        List<Integer> drugTypes = Collections.singletonList(commonDrug.getDrugType());
        //西药 中成药 判断混开
        if (!RecipeUtil.isTcmType(commonDrug.getDrugType())) {
            boolean isMergeRecipeType = configurationClient.getValueBooleanCatch(commonDrug.getOrganId(), "isMergeRecipeType", false);
            if (isMergeRecipeType) {
                drugTypes = Arrays.asList(RecipeBussConstant.RECIPETYPE_WM, RecipeBussConstant.RECIPETYPE_CPM);
            }
        }
        //获取常用药记录
        List<DrugCommon> drugCommonList = drugManager.commonDrugList(commonDrug.getOrganId(), commonDrug.getDoctor(), drugTypes, RecipeDrugFormTypeEnum.getDrugForm(commonDrug.getRecipeDrugForm()));
        if (CollectionUtils.isEmpty(drugCommonList)) {
            return Collections.emptyList();
        }
        //获取药房相关药品
        List<String> organDrugCodeList = drugCommonList.stream().map(DrugCommon::getOrganDrugCode).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListManager.pharmacyDrug(commonDrug.getOrganId(), organDrugCodeList, commonDrug.getPharmacyId());
        if (CollectionUtils.isEmpty(organDrugList)) {
            return Collections.emptyList();
        }
        //药品嘱托
        Map<Integer, DrugEntrust> drugEntrustNameMap = drugManager.drugEntrustIdMap(commonDrug.getOrganId());
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(commonDrug.getOrganId(), commonDrug.getDrugType()));
        //药品商品名拼接配置
        Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(commonDrug.getOrganId(), commonDrug.getDrugType()));
        //平台药品
        List<Integer> drugIds = organDrugList.stream().map(OrganDrugList::getDrugId).distinct().collect(Collectors.toList());
        List<DrugList> drugs = this.drugList(drugIds);
        Map<Integer, DrugList> drugMap = drugs.stream().collect(Collectors.toMap(DrugList::getDrugId, a -> a, (k1, k2) -> k1));
        //返回药品出参
        List<SearchDrugDetailDTO> drugList = new ArrayList<>();
        Map<String, OrganDrugList> organDrugMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        drugCommonList.forEach(a -> {
            OrganDrugList organDrug = organDrugMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == organDrug) {
                return;
            }
            SearchDrugDetailDTO searchDrug = ObjectCopyUtils.convert(organDrug, SearchDrugDetailDTO.class);
            //添加es价格空填值逻辑
            DrugList drugListNow = drugMap.get(a.getDrugId());
            if (null != drugListNow) {
                searchDrug.setPrice1(drugListNow.getPrice1());
                searchDrug.setPrice2(drugListNow.getPrice2());
                searchDrug.setDrugType(drugListNow.getDrugType());
            }
            searchDrug.setHospitalPrice(searchDrug.getSalePrice());
            searchDrug.setUseDoseAndUnitRelation(RecipeUtil.defaultUseDose(organDrug));
            RecipeUtil.SearchDrugDetailDTO(searchDrug, configDrugNameMap, configSaleNameMap, drugEntrustNameMap);
            drugList.add(searchDrug);
        });
        return drugList;
    }

    @Override
    public List<RecipeRulesDrugCorrelation> findRulesByDrugIdAndRuleId(Integer drugId, Integer ruleId) {
        return recipeRulesDrugCorrelationDAO.findRulesByDrugIdAndRuleId(drugId, ruleId);
    }

    @Override
    public List<RecipeRulesDrugCorrelation> findRulesByCorrelationDrugIdAndRuleId(Integer correlationDrugId, Integer ruleId) {
        return recipeRulesDrugCorrelationDAO.findRulesByCorrelationDrugIdAndRuleId(correlationDrugId, ruleId);
    }

    @Override
    @LogRecord
    public void operationDrugSaleStrategy(DrugSaleStrategyVO param) {
        DrugSaleStrategy drugSaleStrategy=new DrugSaleStrategy();
        recipe.util.ObjectCopyUtils.copyProperties(drugSaleStrategy,param);
        if("add".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.save(drugSaleStrategy);
        }
        if("update".equals(param.getType())){
            drugSaleStrategy.setStatus(1);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
        }
        if("delete".equals(param.getType())){
            drugSaleStrategy.setStatus(0);
            drugSaleStrategyDAO.updateNonNullFieldByPrimaryKey(drugSaleStrategy);
            //关联删除药企药品目录销售策略
            List<SaleDrugList> saleDrugListList=saleDrugListDAO.findByDrugId(param.getDrugId());
            saleDrugListList.forEach(saleDrugList -> {
                saleDrugList.setSaleStrategyId(null);
                saleDrugListDAO.update(saleDrugList);
            });
        }
    }

    @Override
    @LogRecord
    public List<DrugSaleStrategyVO> findDrugSaleStrategy(Integer depId, Integer drugId) {
        List<DrugSaleStrategyVO> drugSaleStrategyVOList = new ArrayList<>();
        //获取该配送药品选中的销售策略
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, depId);
        DrugSaleStrategy drugSaleStrategy = null;
        if (null != saleDrugList && null != saleDrugList.getSaleStrategyId()) {
            drugSaleStrategy = drugManager.getDrugSaleStrategyById(saleDrugList.getSaleStrategyId());
        }
        //获取药品所有的销售策略
        List<DrugSaleStrategy> allDrugSaleStrategyList = drugManager.findDrugSaleStrategy(drugId);
        //获取该药品默认的销售策略
        DrugSaleStrategy defaultDrugSaleStrategy = drugManager.getDefaultDrugSaleStrategy(depId, drugId);
        DrugSaleStrategyVO defaultDrugSaleStrategyVO = null;
        if (null != defaultDrugSaleStrategy) {
            defaultDrugSaleStrategyVO = recipe.util.ObjectCopyUtils.convert(defaultDrugSaleStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVOList.add(defaultDrugSaleStrategyVO);
        }
        if (null != drugSaleStrategy) {
            DrugSaleStrategyVO drugSaleStrategyVO = recipe.util.ObjectCopyUtils.convert(drugSaleStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVO.setButtonOpenFlag(true);
            drugSaleStrategyVOList.add(drugSaleStrategyVO);
        } else {
            if (null != defaultDrugSaleStrategyVO) {
                defaultDrugSaleStrategyVO.setButtonOpenFlag(true);
            }
        }
        for (DrugSaleStrategy drugStrategy : allDrugSaleStrategyList) {
            if (null != drugSaleStrategy && drugStrategy.getId().equals(drugSaleStrategy.getId())) {
                continue;
            }
            DrugSaleStrategyVO drugSaleStrategyVO = recipe.util.ObjectCopyUtils.convert(drugStrategy, DrugSaleStrategyVO.class);
            drugSaleStrategyVOList.add(drugSaleStrategyVO);
        }
        return drugSaleStrategyVOList;
    }

    @Override
    public List<DrugSaleStrategy> findDrugSaleStrategy(DrugSaleStrategyVO drugSaleStrategy) {
        List<DrugSaleStrategy> drugSaleStrategyList = drugSaleStrategyDAO.findByDrugId(drugSaleStrategy.getDrugId());
        DrugList drugList = drugListDAO.getById(drugSaleStrategy.getDrugId());
        if (null != drugList) {
            DrugSaleStrategy saleStrategy = new DrugSaleStrategy();
            saleStrategy.setDrugId(drugSaleStrategy.getDrugId());
            saleStrategy.setDrugAmount(1);
            saleStrategy.setDrugUnit(drugList.getUnit());
            saleStrategy.setStrategyTitle("默认出售策略");
            saleStrategy.setStatus(1);
            saleStrategy.setId(0);
            drugSaleStrategyList.add(saleStrategy);
            Collections.sort(drugSaleStrategyList);
        }
        return drugSaleStrategyList;
    }

    @Override
    public void saveDrugSaleStrategy(Integer depId, Integer drugId, Integer strategyId) {
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, depId);
        if (new Integer(0).equals(strategyId)) {
            saleDrugList.setSaleStrategyId(null);
        } else {
            saleDrugList.setSaleStrategyId(strategyId);
        }
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList);
    }

    @Override
    public SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        SaleDrugList saleDrugListDb = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        if (null == saleDrugListDb) {
            saleDrugListDb = recipe.util.ObjectCopyUtils.convert(saleDrugList, SaleDrugList.class);
        }
        return saleDrugListDb;
    }

    @Override
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList) {
        logger.info("saveSaleDrugSalesStrategy saleDrugList={}", JSONUtils.toString(saleDrugList));
        //获取之前的药企药品目录
        SaleDrugList saleDrugList1 = saleDrugListDAO.getByDrugIdAndOrganId(saleDrugList.getDrugId(), saleDrugList.getOrganId());
        if (saleDrugList1 == null) {
            return;
        }
        logger.info("saveSaleDrugSalesStrategy saleDrugList1={}", JSONUtils.toString(saleDrugList1));
        //最后进行更新
        saleDrugListDAO.updateNonNullFieldByPrimaryKey(saleDrugList1);
    }

    @Override
    public void organDrugList2Es(Integer organId) {
        List<OrganDrugList> drugLists = organDrugListDAO.findByOrganIdWithOutStatus(organId);
        drugManager.updateOrganDrugListToEs(drugLists, 0, null);
    }

    @Override
    @LogRecord
    public OrganConfigVO getConfigByOrganId(Integer organId) {
        OrganConfigVO organConfigVO=new OrganConfigVO();
        DrugOrganConfig drugOrganConfig=drugOrganConfigDao.getByOrganId(organId);
        if (ObjectUtils.isEmpty(drugOrganConfig)){
            drugOrganConfig=new DrugOrganConfig();
            drugOrganConfig.setOrganId(organId);
            drugOrganConfigDao.save(drugOrganConfig);
            drugOrganConfig=drugOrganConfigDao.getByOrganId(organId);
        }
        BeanUtils.copyProperties(drugOrganConfig, organConfigVO);
        //同步字段设置
        List<OrganDrugListSyncField> organDrugListSyncFields=organDrugListSyncFieldDAO.findByOrganId(organId);
        if(CollectionUtils.isEmpty(organDrugListSyncFields)){
            logger.info("addOrganDrugListSyncFieldForOrgan 新增配置:{}",organId);
            organConfigVO.setOrganDrugListSyncFieldList(ObjectCopyUtils.convert(drugManager.addOrganDrugListSyncFieldForOrgan(organId), OrganDrugListSyncFieldVo.class));
        }else{
            organConfigVO.setOrganDrugListSyncFieldList(ObjectCopyUtils.convert(organDrugListSyncFields, OrganDrugListSyncFieldVo.class));
        }
        return organConfigVO;
    }

    @Override
    public OrganConfigVO updateOrganConfig(OrganConfigVO organConfigVO) {
        drugOrganConfigDao.update(ObjectCopyUtils.convert(organConfigVO, DrugOrganConfig.class));
        List<OrganDrugListSyncField> organDrugListSyncFieldList=ObjectCopyUtils.convert(organConfigVO.getOrganDrugListSyncFieldList(),OrganDrugListSyncField.class);
        if(CollectionUtils.isEmpty(organDrugListSyncFieldList)){
            drugManager.addOrganDrugListSyncFieldForOrgan(organConfigVO.getOrganId());
        }else{
            organDrugListSyncFieldList.forEach(a->{
                drugManager.addOrUpdateOrganDrugListSyncField(a);
            });
        }
        return organConfigVO;
    }

    private void getCheckText(PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrugRes, List<String> drugName, Integer shoppingCartType) {
        patientContinueRecipeCheckDrugRes.setCheckFlag(YesOrNoEnum.YES.getType());
        StringBuilder stringBuilder = new StringBuilder();
        if (Objects.isNull(shoppingCartType)) {
            stringBuilder.append("处方内药品");
        }
        drugName.forEach(drug -> stringBuilder.append("【").append(drug).append("】"));
        stringBuilder.append("不支持线上开药,是否继续?");
        patientContinueRecipeCheckDrugRes.setCheckText(stringBuilder.toString());
    }

    @Override
    public List<String> medicationInfoSyncTask() {
        List<MedicationInfoResTO> medicationInfoResTOList = drugManager.medicationInfoSyncTask();
        logger.info("RecipeBusinessService medicationInfoSyncTask medicationInfoResTOList={}",JSONUtils.toString(medicationInfoResTOList));
        List<String> errorMsg = Lists.newArrayList();
        if(medicationInfoResTOList.size() > 0){
            for(MedicationInfoResTO medicationInfoResTO : medicationInfoResTOList) {
                List<String> msg = processingMedicationParameters(medicationInfoResTO);
                errorMsg.addAll(msg);
            }
        }
        logger.info("DrugBusinessService processingUsingRateParameters errorMsg={}",JSONUtils.toString(errorMsg));
        return errorMsg;
    }



    @Override
    public Boolean updateMedicationSyncConfig(MedicationSyncConfig medicationSyncConfig) {
        logger.info("DrugBusinessService updateMediationSyncConfig mediationSyncConfig={}",JSONUtils.toString(medicationSyncConfig));
        return drugManager.updateMedicationSyncConfig(medicationSyncConfig);
    }

    @Override
    public MedicationSyncConfig getMedicationSyncConfig(Integer organId,Integer dataType) {
        logger.info("DrugBusinessService saveMediationSyncConfig organId={},dataType={}",organId,dataType);
        return drugManager.getMedicationSyncConfig(organId,dataType);
    }

    @Override
    public HisResponseTO medicationInfoSyncTaskForHis(List<MedicationInfoResTO> medicationInfoResTOList) {
        logger.info("DrugBusinessService medicationInfoSyncTaskForHis medicationInfoResTOList={}",medicationInfoResTOList);
        HisResponseTO hisResponseTO = new HisResponseTO();
        if (ObjectUtils.isEmpty(medicationInfoResTOList)) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg("推送数据为空!");
            return hisResponseTO;
        }
        Set<Integer> dataTypeSet = medicationInfoResTOList.stream().map(MedicationInfoResTO::getDataType).collect(Collectors.toSet());
        Map<Integer, List<MedicationInfoResTO>> medicationInfoResTOMap = medicationInfoResTOList.stream().collect(Collectors.groupingBy(MedicationInfoResTO::getDataType));
        List<MedicationSyncConfig> medicationSyncConfigs = medicationSyncConfigDAO.findByOrganId(medicationInfoResTOList.get(0).getOrganId());
        Map<Integer,MedicationSyncConfig> medicationSyncConfigMap = medicationSyncConfigs.stream().collect(Collectors.toMap(MedicationSyncConfig::getDataType, a -> a, (k1, k2) -> k1));
        List<String> msgs = Lists.newArrayList();
        MedicationSyncConfig medicationSyncConfig = new MedicationSyncConfig();
        for(Integer dataType : dataTypeSet){
            try{
                medicationSyncConfig = medicationSyncConfigMap.get(dataType);
                Boolean enableSync = medicationSyncConfig.getEnableSync();
                Integer dockingMode = medicationSyncConfig.getDockingMode();
                if (ObjectUtils.isEmpty(dockingMode)) {
                    msgs.add("organId:" + medicationSyncConfig.getOrganId() + "，" +
                            (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "未找到同步模式配置!");
                    throw new DAOException(DAOException.VALUE_NEEDED, (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "未找到同步模式配置!");
                }
                if (dockingMode == 1) {
                    msgs.add("organId:" + medicationSyncConfig.getOrganId() + "，" +
                            (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "同步模式 为【自主查询】 调用无效!");
                    throw new DAOException(DAOException.VALUE_NEEDED, (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "同步模式 为【自主查询】 调用无效!");
                }
                if (!enableSync) {
                    msgs.add("organId:" + medicationSyncConfig.getOrganId() + "，" +
                            (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "同步开关未开启");
                    throw new DAOException(DAOException.VALUE_NEEDED, (new Integer(1).equals(medicationSyncConfig.getDataType()) ? "用药途径" : "用药频次") + "同步开关未开启");
                }
                List<MedicationInfoResTO> medicationInfoResTOS = medicationInfoResTOMap.get(dataType);
                medicationInfoResTOS.forEach(MedicationInfoResTO -> {
                    List<String> msg = processingMedicationParameters(MedicationInfoResTO);
                    msgs.addAll(msg);
                });
            }catch (Exception e){
                logger.error("medicationInfoSyncTaskForHis同步 organId=[{}],机构主动查询异常：error=[{}].", medicationSyncConfig.getOrganId(),e);
            }

        }
        if (!ObjectUtils.isEmpty(msgs)) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg(msgs.toString());
            return hisResponseTO;
        }
        hisResponseTO.setMsgCode("200");
        hisResponseTO.setMsg("success");
        return hisResponseTO;
    }

    @Override
    public List<DrugList> findDrugListByInfo(DrugListBean drugListBean) {
        logger.info("findDrugListByInfo drugListBean={}", JSONUtils.toString(drugListBean));
        List<DrugList> drugLists = drugListDAO.findDrugListByInfo(drugListBean.getDrugName(), drugListBean.getProducer(), drugListBean.getDrugSpec(), drugListBean.getPack(), drugListBean.getUnit());
        logger.info("findDrugListByInfo drugLists={}", JSONUtils.toString(drugLists));
        return drugLists;
    }

    /**
     * 保存、更新、删除机构数据字典（用药频次、用药途径）
     * @param medicationInfoResTO
     * @return
     */
    public List<String> processingMedicationParameters(MedicationInfoResTO medicationInfoResTO){
        List<String> msg = Lists.newArrayList();
        //用药途径
        if(new Integer(1).equals(medicationInfoResTO.getDataType())){
            //首先看是否是需要删除的数据
            if(new Integer(1).equals(medicationInfoResTO.getDeleteFlag())) {
                try {
                    usePathwaysService.deleteUsePathwaysByKey(medicationInfoResTO.getMedicationCode(), medicationInfoResTO.getOrganId());
                }catch (Exception e){
                    msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药途径编码：" + medicationInfoResTO.getMedicationCode() + "，" + "删除失败:" + e.toString().split(":")[1]);
                }
            }else{
                logger.info("DrugBusinessService processingMedicationParameters organId={},category={}",medicationInfoResTO.getMedicationCode(),medicationInfoResTO.getCategory());
                UsePathwaysDTO usePathwaysDTO = usePathwaysService.getUsePathwaysByOrganAndKeyAndCategory(medicationInfoResTO.getOrganId(), medicationInfoResTO.getMedicationCode(),medicationInfoResTO.getCategory());
                logger.info("DrugBusinessService processingMedicationParameters usePathwaysDTO={}",JSONUtils.toString(usePathwaysDTO));
                if(Objects.isNull(usePathwaysDTO)){
                    try {
                        //新增
                        usePathwaysDTO = new UsePathwaysDTO();
                        usePathwaysDTO.setOrganId(medicationInfoResTO.getOrganId());
                        if(StringUtils.isEmpty(medicationInfoResTO.getMedicationCode())){
                            throw new DAOException(DAOException.VALUE_NEEDED,"用药途径编码不能为空!");
                        }else{
                            usePathwaysDTO.setPathwaysKey(medicationInfoResTO.getMedicationCode());
                        }
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getMedicationText())){
                            usePathwaysDTO.setText(medicationInfoResTO.getMedicationText());
                        }else {
                            throw new DAOException(DAOException.VALUE_NEEDED,"用药途径名称不能为空!");
                        }
                        String category = medicationInfoResTO.getCategory();
                        if(StringUtils.isNotEmpty(category)){
                            if(category.contains("，")){
                                throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段内容格式不正确!");
                            }
                            usePathwaysDTO.setCategory(category);
                        }else{
                            throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段不能为空!");
                        }
                        if(medicationInfoResTO.getSort() != null){
                            usePathwaysDTO.setSort(medicationInfoResTO.getSort());
                        }else {
                            usePathwaysDTO.setSort(1000);
                        }
                    }catch (Exception e){
                        msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药途径编码：" + medicationInfoResTO.getMedicationCode() + "，" + "新增失败:" + e.toString().split(":")[1]);
                    }
                }else{
                    try{
                        //更新
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getMedicationText())){
                            usePathwaysDTO.setText(medicationInfoResTO.getMedicationText());
                        }
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getCategory())){
                            if(medicationInfoResTO.getCategory().contains("，")){
                                throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段内容格式不正确!");
                            }
                            usePathwaysDTO.setCategory(medicationInfoResTO.getCategory());
                        }
                        if(medicationInfoResTO.getSort() != null){
                            usePathwaysDTO.setSort(medicationInfoResTO.getSort());
                        }
                    }catch (Exception e){
                        msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药途径编码：" + medicationInfoResTO.getMedicationCode() + "，" + "更新失败:" + e.toString().split(":")[1]);
                    }
                }
                usePathwaysDTO.setEnglishNames(medicationInfoResTO.getEnglishNames());
                usePathwaysDTO.setPinYin(medicationInfoResTO.getPinYin());
                usePathwaysDTO.setRelatedPlatformKey(medicationInfoResTO.getRelatedPlatformKey());
                if(msg.size() > 0){
                    return msg;
                }
                if(usePathwaysDTO.getId() != null){
                    logger.info("processingMedicationParameters updateUsePathwaysById usePathwaysDTO={}",JSONUtils.toString(usePathwaysDTO));
                    usePathwaysService.updateUsePathwaysById(usePathwaysDTO);
                }
                else{
                    logger.info("processingMedicationParameters saveOrganUsingRate usePathwaysDTO={}",JSONUtils.toString(usePathwaysDTO));
                    usePathwaysService.saveOrganUsePathways(usePathwaysDTO);
                }
            }
        }
        //用药频次
        else{
            //删除
            if(new Integer(1).equals(medicationInfoResTO.getDeleteFlag())){
                try {
                    usingRateService.deleteUsingRateByKey(medicationInfoResTO.getMedicationCode(),medicationInfoResTO.getOrganId());
                }catch (Exception e){
                    msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药频次编码：" + medicationInfoResTO.getMedicationCode() + "，" + "删除失败:" + e.toString().split(":")[1]);
                }
            }else {
                logger.info("DrugBusinessService processingMedicationParameters organId={},medicationCode={}",medicationInfoResTO.getMedicationCode(),medicationInfoResTO.getMedicationCode());
                UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(medicationInfoResTO.getOrganId(), medicationInfoResTO.getMedicationCode());
                logger.info("DrugBusinessService processingMedicationParameters usingRateDTO={}",JSONUtils.toString(usingRateDTO));
                if(Objects.isNull(usingRateDTO)){
                    try{
                        //新增
                        usingRateDTO = new UsingRateDTO();
                        usingRateDTO.setOrganId(medicationInfoResTO.getOrganId());
                        if(StringUtils.isEmpty(medicationInfoResTO.getMedicationCode())){
                            throw new DAOException(DAOException.VALUE_NEEDED,"用药频次编码不能为空!");
                        }else{
                            usingRateDTO.setUsingRateKey(medicationInfoResTO.getMedicationCode());
                        }
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getMedicationText())){
                            usingRateDTO.setText(medicationInfoResTO.getMedicationText());
                        }else {
                            throw new DAOException(DAOException.VALUE_NEEDED,"用药频次名称不能为空!");
                        }
                        String category = medicationInfoResTO.getCategory();
                        if(StringUtils.isNotEmpty(category)){
                            if(category.contains("，")){
                                throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段内容格式不正确!");
                            }
                            usingRateDTO.setCategory(category);
                        }else{
                            throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段不能为空!");
                        }
                        if(medicationInfoResTO.getSort() != null){
                            usingRateDTO.setSort(medicationInfoResTO.getSort());
                        }else {
                            usingRateDTO.setSort(1000);
                        }
                    }catch (Exception e){
                        msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药频次编码：" + medicationInfoResTO.getMedicationCode() + "，" + "新增失败:" + e.toString().split(":")[1]);
                    }
                }else{
                    try{
                        //更新
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getMedicationText())){
                            usingRateDTO.setText(medicationInfoResTO.getMedicationText());
                        }
                        if(StringUtils.isNotEmpty(medicationInfoResTO.getCategory())){
                            if(medicationInfoResTO.getCategory().contains("，")){
                                throw new DAOException(DAOException.VALUE_NEEDED,"处方类型字段内容格式不正确!");
                            }
                            usingRateDTO.setCategory(medicationInfoResTO.getCategory());
                        }
                        if(medicationInfoResTO.getSort() != null){
                            usingRateDTO.setSort(medicationInfoResTO.getSort());
                        }
                    }catch (Exception e){
                        msg.add("organId：" + medicationInfoResTO.getOrganId() + "，" +"用药频次编码：" + medicationInfoResTO.getMedicationCode() + "，" + "更新失败:" + e.toString().split(":")[1]);
                    }
                }
                usingRateDTO.setEnglishNames(medicationInfoResTO.getEnglishNames());
                usingRateDTO.setPinYin(medicationInfoResTO.getPinYin());
                usingRateDTO.setUsingRateAlgorithm(medicationInfoResTO.getUsingRateAlgorithm());
                usingRateDTO.setRelatedPlatformKey(medicationInfoResTO.getRelatedPlatformKey());
                if(msg.size() > 0){
                    return msg;
                }
                if(usingRateDTO.getId() != null){
                    logger.info("processingMedicationParameters updateUsingRateById usingRateDTO={}",JSONUtils.toString(usingRateDTO));
                    usingRateService.updateUsingRateById(usingRateDTO);
                }
                else {
                    logger.info("processingMedicationParameters saveOrganUsingRate usingRateDTO={}",JSONUtils.toString(usingRateDTO));
                    usingRateService.saveOrganUsingRate(usingRateDTO);
                }
            }
        }
        return msg;
    }

    @Override
    public List<OrganDrugListBean> findHisDrugList(HisDrugInfoReqVO hisDrugInfoReqVO) {
        HospitalInformationRequestTO request = new HospitalInformationRequestTO();
        request.setCxc(hisDrugInfoReqVO.getSearchKeyWord());
        request.setOrganId(hisDrugInfoReqVO.getOrganId());
        request.setCxfw(hisDrugInfoReqVO.getSearchRang().toString());
        request.setJsfs(hisDrugInfoReqVO.getSearchWay());
        String organList = recipeParameterDao.getByName("organDrugCode_idm_organId");
        List<com.ngari.platform.recipe.mode.OrganDrugListBean> organDrugListBeans = drugClient.findHisDrugList(request, organList);
        List<String> organDrugCodes = organDrugListBeans.stream().map(com.ngari.platform.recipe.mode.OrganDrugListBean::getOrganDrugCode).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(organDrugCodes)) {
            return Collections.emptyList();
        }
        Set<String> pharmacyCodeSet = organDrugListBeans.stream().map(com.ngari.platform.recipe.mode.OrganDrugListBean::getPharmacy).collect(Collectors.toSet());
        List<PharmacyTcm> pharmacyTcmList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(pharmacyCodeSet)) {
            pharmacyTcmList = pharmacyTcmDAO.findByOrganIdAndCodes(hisDrugInfoReqVO.getOrganId(), pharmacyCodeSet);
        }
        Map<String, PharmacyTcm> pharmacyMap = pharmacyTcmList.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyCode, a -> a, (k1, k2) -> k1));
        organDrugListBeans.forEach(organDrugListBean -> {
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndPharmacy(hisDrugInfoReqVO.getOrganId(), organDrugListBean.getOrganDrugCode(), organDrugListBean.getPharmacy());
            OrganDrugList organDrugList = null;
            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                organDrugList = organDrugLists.get(0);
            }
            PharmacyTcm pharmacyTcm = null;
            if (StringUtils.isNotEmpty(organDrugListBean.getPharmacy())) {
                pharmacyTcm = pharmacyMap.get(organDrugListBean.getPharmacy());
            }
            if (Objects.nonNull(organDrugList)) {
                String hisPharmacyCode = Objects.isNull(pharmacyTcm)?"":pharmacyTcm.getPharmacyId().toString();
                String pharmacyCode = StringUtils.isEmpty(organDrugList.getPharmacy())?"":organDrugList.getPharmacy();
                if (pharmacyCode.contains(hisPharmacyCode)) {
                    organDrugListBean.setDrugId(organDrugList.getDrugId());
                }
            }
        });
        List<OrganDrugListBean> organDrugListBeanList = ObjectCopyUtils.convert(organDrugListBeans, OrganDrugListBean.class);
        organDrugListBeanList = organDrugListBeanList.stream()
                .sorted(Comparator.comparing(OrganDrugListBean::getDrugId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return organDrugListBeanList;
    }

    @Override
    public Boolean checkOrganDrugList(Integer organId, String organDrugCode) {
        List<String> organDrugCodes = Arrays.asList(organDrugCode);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodes);
        if (CollectionUtils.isEmpty(organDrugLists)) {
            return false;
        }
        return true;
    }

}
