package recipe.business;

import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.vo.SearchDrugReqVo;
import org.springframework.stereotype.Service;
import recipe.core.api.IDrugBusinessService;
import recipe.manager.DrugManeger;
import recipe.manager.OrganDrugListManager;

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
    private DrugManeger drugManeger;

    @Override
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo) {
        List<PatientDrugWithEsDTO>  patientDrugWithEsDTOS = drugManeger.findDrugWithEsByPatient(searchDrugReqVo.getSaleName(),searchDrugReqVo.getOrganId(), Arrays.asList("1","2"),searchDrugReqVo.getStart(),searchDrugReqVo.getLimit());
        return patientDrugWithEsDTOS;
    }
}
