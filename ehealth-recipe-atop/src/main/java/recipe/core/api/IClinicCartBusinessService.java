package recipe.core.api;

import recipe.vo.second.ClinicCartVO;

import java.util.List;

public interface IClinicCartBusinessService {

    List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId);

    Integer addClinicCart(ClinicCartVO clinicCartVO);

    Boolean deleteClinicCartById(Integer id);
}
