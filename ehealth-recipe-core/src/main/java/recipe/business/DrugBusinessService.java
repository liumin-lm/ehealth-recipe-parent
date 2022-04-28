package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.HospitalDrugListDTO;
import com.ngari.platform.recipe.mode.HospitalDrugListReqDTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.service.IDrugEntrustService;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.DrugClient;
import recipe.client.IConfigurationClient;
import recipe.core.api.IDrugBusinessService;
import recipe.dao.OrganDrugListDAO;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.DrugManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.OrganDrugListManager;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private IDrugEntrustService drugEntrustService;
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
        List<DrugEntrustDTO> drugEntrusts = drugEntrustService.querDrugEntrustByOrganId(organId);
        boolean drugEntrustName = drugEntrusts.stream().anyMatch(a -> "无特殊煎法".equals(a.getDrugEntrustName()));
        Map<Integer, DrugEntrustDTO> drugEntrustsMap = drugEntrusts.stream().collect(Collectors.toMap(DrugEntrustDTO::getDrugEntrustId, a -> a, (k1, k2) -> k1));
        List<SearchDrugDetailDTO> list = new LinkedList<>();
        drugInfo.forEach(s -> {
            SearchDrugDetailDTO drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
            //前端展示的药品拼接名处理
            drugList.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(drugList, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(drugList.getDrugType())));
            //前端展示的药品商品名拼接名处理
            drugList.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(drugList, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(drugList.getDrugType())));
            //替换嘱托
            if (null != drugType && 3 == drugType) {
                Integer drugEntrustId = ByteUtils.strValueOf(drugList.getDrugEntrust());
                DrugEntrustDTO drugEntrustDTO = drugEntrustsMap.get(drugEntrustId);
                if (null != drugEntrustDTO && !ValidateUtil.integerIsEmpty(drugEntrustId)) {
                    drugList.setDrugEntrustId(drugEntrustDTO.getDrugEntrustId().toString());
                    drugList.setDrugEntrustCode(drugEntrustDTO.getDrugEntrustCode());
                    drugList.setDrugEntrust(drugEntrustDTO.getDrugEntrustValue());
                } else if (drugEntrustName) {
                    //运营平台没有配置默认值，没有嘱托Id，中药特殊处理,药品没有维护字典--默认无特殊煎法
                    drugList.setDrugEntrustId("56");
                    drugList.setDrugEntrustCode("sos");
                    drugList.setDrugEntrust("无特殊煎法");
                }
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
    public List<RecipeRulesDrugcorrelation> getListDrugRules(List<Integer> list, Integer ruleId) {
        return drugManager.getListDrugRules(list, ruleId);
    }

    @Override
    public DrugSpecificationInfoDTO hisDrugBook(Integer organId, Recipedetail recipedetail) {
        return drugManager.hisDrugBook(organId, recipedetail);
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
    public void queryRemindRecipe() {
        List<Integer> organIdList = configurationClient.organIdList("remindPatientTakeMedicineFlag", "true");
        organIdList.forEach(a -> {
            try {
                int pageSize;
                int pageNo = 0;
                do {
                    List<RecipeInfoDTO> list = hisRecipeManager.queryRemindRecipe(a, ++pageNo);
                    pageSize = list.size();
                    if (CollectionUtils.isNotEmpty(list)) {
                        drugManager.remindPatient(list);
                    }
                } while (pageSize >= 1000);
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
}
