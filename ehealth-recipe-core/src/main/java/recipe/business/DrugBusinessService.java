package recipe.business;

import com.ngari.his.recipe.mode.DrugSpecificationInfoDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.RecipeRulesDrugcorrelation;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.service.IDrugEntrustService;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.core.api.IDrugBusinessService;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.DrugManager;
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

    @Resource
    private DrugManager drugManager;
    @Autowired
    private IDrugEntrustService drugEntrustService;

    @Override
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo) {
        return drugManager.findDrugWithEsByPatient(searchDrugReqVo.getSaleName(), searchDrugReqVo.getOrganId(), Arrays.asList(RecipeTypeEnum.RECIPETYPE_WM.getType().toString(), RecipeTypeEnum.RECIPETYPE_CPM.getType().toString()), searchDrugReqVo.getStart(), searchDrugReqVo.getLimit());
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
    public Map<Integer, DrugList> drugList(List<Integer> drugIds) {
        return drugManager.drugList(drugIds);
    }

}
