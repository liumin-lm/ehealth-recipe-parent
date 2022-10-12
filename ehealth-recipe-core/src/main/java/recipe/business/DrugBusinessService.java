package recipe.business;

import com.google.common.collect.Lists;
import com.ngari.WxServiceAPI;
import com.ngari.intface.WxConfigService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganConfigDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.HospitalDrugListDTO;
import com.ngari.platform.recipe.mode.HospitalDrugListReqDTO;
import com.ngari.recipe.drug.model.CommonDrugListDTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.DrugSaleStrategyVO;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.annotation.ItemProperty;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import ctd.util.converter.ConversionUtils;
import eh.base.constant.ErrorCode;
import eh.entity.base.ClientConfig;
import eh.entity.base.HisServiceConfig;
import eh.entity.base.Organ;
import eh.entity.base.OrganConfig;
import eh.entity.wx.WXConfig;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import recipe.aop.LogRecord;
import recipe.bean.cqjgptbussdata.Drug;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.client.IConfigurationClient;
import recipe.client.OrganClient;
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
import recipe.vo.patient.PatientContinueRecipeCheckDrugReq;
import recipe.vo.patient.PatientContinueRecipeCheckDrugRes;
import recipe.vo.patient.PatientOptionalDrugVo;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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
    private OrganClient organClient;
    @Autowired
    private OrganDrugListSyncFieldDAO organDrugListSyncFieldDAO;
    @Autowired
    private DrugOrganConfigDAO drugOrganConfigDao;

    @Override
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo) {
        return drugManager.findDrugWithEsByPatient(searchDrugReqVo.getSaleName(), searchDrugReqVo.getOrganId().toString(), Arrays.asList(RecipeTypeEnum.RECIPETYPE_WM.getType().toString(), RecipeTypeEnum.RECIPETYPE_CPM.getType().toString()), searchDrugReqVo.getStart(), searchDrugReqVo.getLimit());
    }

    @Override
    public List<SearchDrugDetailDTO> searchOrganDrugEs(DrugInfoDTO drugInfoDTO, int start, int limit) {
        List<String> drugInfo = drugManager.searchOrganDrugEs(drugInfoDTO, start, limit);
        Integer organId = drugInfoDTO.getOrganId();
        Integer drugType = drugInfoDTO.getDrugType();
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, drugType));
        //药品商品名拼接配置
        Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, drugType));
        Map<Integer, DrugEntrust> drugEntrustNameMap = drugManager.drugEntrustIdMap(organId);
        List<SearchDrugDetailDTO> list = new LinkedList<>();
        drugInfo.forEach(s -> {
            SearchDrugDetailDTO drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
            RecipeUtil.SearchDrugDetailDTO(drugList, configDrugNameMap, configSaleNameMap, drugEntrustNameMap);
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
            getCheckText(patientContinueRecipeCheckDrugRes,collect);
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
            getCheckText(patientContinueRecipeCheckDrugRes, drugName);
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

//    @Override
//    public void setDrugOrganConfig(Integer organId, String key, String value) {
////        UserPermissionService userPermissionService = AppContextHolder.getBean("opbase.userPermissionService",UserPermissionService.class);
////        Boolean havePermission = userPermissionService.havePermissionNode(PropertyOrganService.ORGAN_PROPERTY_PID);
////        if (!havePermission){
////            throw new DAOException("无权限操作");
////        }
//        setOrganConfig(organId, key, value);
//    }

//    /**
//     * 设置机构Config
//     *
//     * @param organId
//     * @param key
//     * @param value
//     */
//    @RpcService
//    public void setOrganConfig(Integer organId, String key, String value) {
//        Organ organ = ObjectCopyUtils.convert(organClient.organDTO(organId),Organ.class);
//        if (organ == null) {
//            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "机构不存在");
//        }
//        switch (key) {
//            case "OrganConfig.enableDrugSync":
//                setConfig(organId, "enableDrugSync", Boolean.valueOf(value));
//                drugManager.logChangeConfig(OrganConfig.class, organ, "药品目录是否支持接口同步", Boolean.valueOf(value));
//                break;
//            case "OrganConfig.enableDrugSyncArtificial":
//                setConfig(organId, "enableDrugSyncArtificial", Boolean.valueOf(value));
//                drugManager.logChangeConfig(OrganConfig.class, organ, "药品目录同步是否需要人工审核", Boolean.valueOf(value));
//                break;
//            case "OrganConfig.drugFromList":
//                setConfig(organId, "drugFromList", value);
//                drugManager.logChangeConfig(OrganConfig.class, organ, "手动同步剂型list暂存", value);
//                break;
//
//        }
//    }
//
//    protected void setConfig(Integer organId, String key, Object value) {
//        OrganConfigVO config =getConfigByOrganId(organId);
//        switch (key) {
//            case "enableDrugSync":
//                config.setEnableDrugSync((Boolean) value);
//                break;
//            case "enableDrugSyncArtificial":
//                config.setEnableDrugSyncArtificial((Boolean) value);
//                break;
//            case "drugFromList":
//                config.setDrugFromList((String)value);
//                break;
//        }
//        drugOrganConfigDao.update(ObjectCopyUtils.convert(config, DrugOrganConfig.class));
//    }

    private void getCheckText(PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrugRes, List<String> drugName) {
        patientContinueRecipeCheckDrugRes.setCheckFlag(YesOrNoEnum.YES.getType());
        StringBuilder stringBuilder = new StringBuilder("处方内药品");
        drugName.forEach(drug -> stringBuilder.append("【").append(drug).append("】"));
        stringBuilder.append("不支持线上开药,是否继续?");
        patientContinueRecipeCheckDrugRes.setCheckText(stringBuilder.toString());
    }
}
