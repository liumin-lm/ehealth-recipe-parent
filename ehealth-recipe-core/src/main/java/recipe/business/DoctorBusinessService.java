package recipe.business;

import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.DoctorPermissionDTO;
import com.ngari.recipe.entity.DoctorDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.doctor.IDoctorBusinessService;
import recipe.manager.DoctorManager;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.DoctorDefaultVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医生服务类
 * @author fuzi
 */
@Service
public class DoctorBusinessService extends BaseService implements IDoctorBusinessService {

    @Autowired
    private DoctorManager doctorManager;

    @Override
    public Map<Integer, DoctorDTO> findByDoctorIds(List<Integer> doctorIds) {
        return doctorClient.findByDoctorIds(doctorIds);
    }

    @Override
    public List<DoctorDefault> doctorDefaultList(DoctorDefaultVO doctorDefault) {
        //医生默认数据
        List<DoctorDefault> list = doctorManager.doctorDefaultList(doctorDefault.getOrganId(), doctorDefault.getDoctorId(), doctorDefault.getCategory());
        //当前科室的药房
        List<DoctorDefault> doctorDefaultPharmacy = pharmacyManager.appointDepartPharmacy(doctorDefault.getOrganId(), doctorDefault.getDepartId(), doctorDefault.getClinicId(), list);
        //除药房外其他默认数据
        List<DoctorDefault> doctorDefaultList = list.stream().filter(a -> !a.getCategory().equals(1)).collect(Collectors.toList());
        doctorDefaultPharmacy.addAll(doctorDefaultList);
        return doctorDefaultPharmacy;
    }

    @Override
    public void saveDoctorDefault(DoctorDefaultVO doctorDefaultVO) {
        DoctorDefault doctorDefault = ObjectCopyUtils.convert(doctorDefaultVO, DoctorDefault.class);
        if (null == doctorDefault) {
            return;
        }
        doctorManager.saveDoctorDefault(doctorDefault);
    }

    @Override
    public DoctorPermissionDTO doctorRecipePermission(DoctorPermissionDTO doctorPermission) {
        //校验权限类型 true：his权限，false：平台权限
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(doctorPermission.getOrganId(), "doctorRecipePermission", false);
        //平台权限
        if (!drugToHosByEnterprise) {
            return doctorManager.doctorRecipePermission(doctorPermission);
        }
        //his权限
        DoctorPermissionDTO doctorPermissionDTO = doctorManager.doctorHisRecipePermission(doctorPermission);
        if (!doctorPermissionDTO.getResult()) {
            return doctorPermissionDTO;
        }
        return doctorManager.doctorRecipePermission(doctorPermission);
    }
}
