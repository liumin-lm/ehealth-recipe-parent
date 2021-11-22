package recipe.client;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.Recipe;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 科室相关服务
 *
 * @Author liumin
 * @Date 2021/11/19 下午2:26
 * @Description
 */
@Service
public class DepartClient extends BaseClient {

    @Autowired
    private AppointDepartService appointDepartService;

    @Autowired
    private DepartmentService departmentService;


    /**
     * 通过机构和行政科室获取挂号科室
     *
     * @param organId
     * @param depart  处方表depart字段
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Integer organId, Integer depart) {
        return appointDepartService.findByOrganIDAndDepartIDAndCancleFlag(organId, depart);
    }

    /**
     * 通过机构和行政科室获取挂号科室
     *
     * @param recipe
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndDepart(Recipe recipe) {
        AppointDepartDTO appointDepartDTO = new AppointDepartDTO();
        if (recipe == null) {
            return appointDepartDTO;
        }
        if (StringUtils.isNotEmpty(recipe.getAppointDepart())) {
            appointDepartDTO.setAppointDepartCode(recipe.getAppointDepart());
            appointDepartDTO.setAppointDepartName(recipe.getAppointDepartName());
        } else {
            appointDepartDTO = appointDepartService.findByOrganIDAndDepartIDAndCancleFlag(recipe.getClinicOrgan(), recipe.getDepart());
        }
        return appointDepartDTO;
    }

    /**
     * 通过挂号科室编码、机构获取挂号科室
     *
     * @param organId
     * @param appointDepartCode
     * @return
     */
    public AppointDepartDTO getAppointDepartByOrganIdAndAppointDepartCode(Integer organId, String appointDepartCode) {
        return appointDepartService.getByOrganIDAndAppointDepartCode(organId, appointDepartCode);
    }

    /**
     * 通过行政科室id获取行政科室
     *
     * @param depart
     * @return
     */
    public DepartmentDTO getDepartmentByDepart(Integer depart) {
        DepartmentDTO departmentDTO = departmentService.getById(depart);
        return departmentDTO;
    }


}
