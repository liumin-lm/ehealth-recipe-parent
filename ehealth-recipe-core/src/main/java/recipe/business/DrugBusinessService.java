package recipe.business;

import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.RecipeRulesDrugcorrelation;
import com.ngari.recipe.vo.SearchDrugReqVo;
import org.springframework.stereotype.Service;
import recipe.core.api.IDrugBusinessService;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.DrugManager;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @description： 药品 业务类
 * @author： whf
 * @date： 2021-08-23 18:58
 */
@Service
public class DrugBusinessService extends BaseService implements IDrugBusinessService {

    @Resource
    private DrugManager drugManager;

    @Override
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo) {
        List<PatientDrugWithEsDTO> patientDrugWithEsDTOS = drugManager.findDrugWithEsByPatient(searchDrugReqVo.getSaleName(), searchDrugReqVo.getOrganId(), Arrays.asList(RecipeTypeEnum.RECIPETYPE_WM.getType().toString(), RecipeTypeEnum.RECIPETYPE_CPM.getType().toString()), searchDrugReqVo.getStart(), searchDrugReqVo.getLimit());
        return patientDrugWithEsDTOS;
    }

    @Override
    public Dispensatory getDrugBook(Integer organId, String organDrugCode) {
        Dispensatory dispensatory = drugManager.getDrugBook(organId,organDrugCode);
        return dispensatory;
    }

    @Override
    public List<RecipeRulesDrugcorrelation> getListDrugRules(List<Integer> list, Integer ruleId) {
        return drugManager.getListDrugRules(list,ruleId);
    }
}
