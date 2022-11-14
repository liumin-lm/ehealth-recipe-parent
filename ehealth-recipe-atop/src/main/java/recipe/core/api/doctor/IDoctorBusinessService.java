package recipe.core.api.doctor;

import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.DoctorPermissionDTO;
import com.ngari.recipe.entity.DoctorDefault;
import recipe.vo.doctor.DoctorDefaultVO;

import java.util.List;
import java.util.Map;

/**
 * 医生服务
 * @author fuzi
 */
public interface IDoctorBusinessService {

    /**
     * 获取医生信息
     *
     * @param doctorIds
     * @return
     */
    Map<Integer, DoctorDTO> findByDoctorIds(List<Integer> doctorIds);

    /**
     * 获取医生 默认数据
     * @param doctorDefault
     * @return 医生默认数据
     */
    List<DoctorDefault> doctorDefaultList(DoctorDefaultVO doctorDefault);

    /**
     * 保存医生 默认数据
     *
     * @param doctorDefaultVO
     */
    void saveDoctorDefault(DoctorDefaultVO doctorDefaultVO);

    /**
     * 获取医生权限
     *
     * @param doctorPermission
     */
    DoctorPermissionDTO doctorRecipePermission(DoctorPermissionDTO doctorPermission);
}
