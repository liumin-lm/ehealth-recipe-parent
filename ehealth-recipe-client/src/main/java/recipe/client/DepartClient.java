package recipe.client;

import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
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
     * 获取行政科室
     *
     * @param depart
     * @return
     */
    public DepartmentDTO getDepartmentByDepart(Integer depart) {
        DepartmentDTO departmentDTO = departmentService.getById(depart);
        return departmentDTO;
    }


}
