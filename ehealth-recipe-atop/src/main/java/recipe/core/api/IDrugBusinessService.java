package recipe.core.api;

import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.vo.SearchDrugReqVo;

import java.util.List;

/**
 * @description： 药品service 接口
 * @author： whf
 * @date： 2021-08-23 19:02
 */
public interface IDrugBusinessService {
    /**
     * 患者端搜索药品信息
     * @param searchDrugReqVo
     * @return
     */
    List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVo searchDrugReqVo);
}
