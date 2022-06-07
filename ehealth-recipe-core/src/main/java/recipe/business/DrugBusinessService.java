package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.HospitalDrugListDTO;
import com.ngari.platform.recipe.mode.HospitalDrugListReqDTO;
import com.ngari.recipe.drug.model.CommonDrugListDTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.client.IConfigurationClient;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IDrugBusinessService;
import recipe.dao.OrganDrugListDAO;
import recipe.enumerate.status.YesOrNoEnum;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.DrugManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.OrganDrugListManager;
import recipe.util.MapValueUtil;
import recipe.vo.patient.PatientContinueRecipeCheckDrugReq;
import recipe.vo.patient.PatientContinueRecipeCheckDrugRes;
import recipe.vo.patient.PatientOptionalDrugVo;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 平台药品 业务类
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
    public List<RecipeRulesDrugcorrelation> getListDrugRules(List<Integer> list, Integer ruleId) {
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
        List<DrugCommon> drugCommonList = drugManager.commonDrugList(commonDrug.getOrganId(), commonDrug.getDoctor(), drugTypes);
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
        organDrugList.forEach(a -> {
            SearchDrugDetailDTO searchDrug = ObjectCopyUtils.convert(a, SearchDrugDetailDTO.class);
            RecipeUtil.SearchDrugDetailDTO(searchDrug, configDrugNameMap, configSaleNameMap, drugEntrustNameMap);
            searchDrug.setUseDoseAndUnitRelation(RecipeUtil.defaultUseDose(a));
            //添加es价格空填值逻辑
            DrugList drugListNow = drugMap.get(a.getDrugId());
            if (null != drugListNow) {
                searchDrug.setPrice1(drugListNow.getPrice1());
                searchDrug.setPrice2(drugListNow.getPrice2());
                searchDrug.setDrugType(drugListNow.getDrugType());
            }
            drugList.add(searchDrug);
        });
        return drugList;
    }

    private void getCheckText(PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrugRes, List<String> drugName) {
        patientContinueRecipeCheckDrugRes.setCheckFlag(YesOrNoEnum.YES.getType());
        StringBuilder stringBuilder = new StringBuilder("处方内药品");
        drugName.forEach(drug -> stringBuilder.append("【").append(drug).append("】"));
        stringBuilder.append("不支持线上开药,是否继续?");
        patientContinueRecipeCheckDrugRes.setCheckText(stringBuilder.toString());
    }
}
